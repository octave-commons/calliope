#!/usr/bin/env python3
import json
import re
import unicodedata
from difflib import SequenceMatcher
from pathlib import Path


BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
LYRICS_PATH = Path('/home/err/devel/Music/heresy_between/存在論的な“反・虚無”の論証として/存在論的な“反・虚無”の論証として.txt')
TRANSCRIPTS = {
    'large_v3_turbo_gated_cpu_int8': BASE / 'lyrics/alignment.faster-whisper-large-v3-turbo-gated-cpu-int8-20260514.json',
    'medium_gated': BASE / 'lyrics/alignment.knoxx-whisper-medium-vocals-gated-20260514.json',
    'medium_denoise_norm': BASE / 'lyrics/alignment.knoxx-whisper-medium-vocals-denoise-norm-20260514.json',
    'medium_compressed': BASE / 'lyrics/alignment.knoxx-whisper-medium-vocals-compressed-20260514.json',
    'medium_raw': BASE / 'lyrics/alignment.knoxx-whisper-medium-vocals-20260514.json',
}
OUT_JSON = BASE / 'lyrics/transcript-hallucination-metrics-v1.json'
OUT_MD = BASE / 'lyrics/transcript-hallucination-metrics-v1.md'

PUNCT_RE = re.compile(r'[\s\u3000「」『』（）()\[\]【】*＊"“”\'’`、。，．・…—ー\-:：;；!！?？/／]+')
META_PATTERNS = [
    re.compile(pattern, re.I)
    for pattern in [
        r'HIP HIP',
        r'ご視聴ありがとうございました',
        r'スタイルプロンプト',
        r'World-?Scale',
        r'ゲルケゴール',
        r'ロールズ',
        r'必要なら',
        r'作れるよ',
        r'Ah ah ah',
        r'thank you',
        r'チャンネル登録',
        r'字幕',
        r'プロンプト',
        r'so happy',
    ]
]
REPEAT_CHAR_RE = re.compile(r'(.)(?:\1){10,}')


def lyric_reference_lines() -> list[str]:
    raw = LYRICS_PATH.read_text(encoding='utf-8')
    marker = '## Lyrics（存在論的な“反・虚無”の論証として）'
    section = raw[raw.index(marker) + len(marker):]
    lines = []
    for line in section.splitlines():
        line = line.strip()
        if not line:
            continue
        if line.startswith('**[') and line.endswith(']**'):
            continue
        if line.startswith('---'):
            break
        lines.append(line)
    return lines


def normalize(text: str) -> str:
    value = unicodedata.normalize('NFKC', str(text)).lower()
    replacements = {
        'メッセージ': 'めっせーじ',
        'メセージ': 'めっせーじ',
        '一行': 'いちぎょう',
        '一個': 'いっこ',
        '一興': 'いっきょう',
        '意味': 'いみ',
        '元気': 'げんき',
        '明日': 'あした',
        '世界': 'せかい',
        '夜': 'よる',
        '僕': 'ぼく',
        '胸': 'むね',
        '責任': 'せきにん',
        '送信': 'そうしん',
        '沈黙': 'ちんもく',
        '決意': 'けつい',
        '肯定': 'こうてい',
        '十分': 'じゅうぶん',
    }
    for source, target in replacements.items():
        value = value.replace(source, target)
    return PUNCT_RE.sub('', value)


def ngrams(text: str, size: int = 2) -> set[str]:
    if len(text) < size:
        return {text} if text else set()
    return {text[index:index + size] for index in range(len(text) - size + 1)}


def support_score(segment: str, references: list[str], reference_joined: str) -> tuple[float, str]:
    segment_norm = normalize(segment)
    if not segment_norm:
        return 0.0, ''
    if segment_norm in reference_joined:
        return 1.0, segment_norm
    segment_ngrams = ngrams(segment_norm)
    candidates = list(references)
    candidates.extend(references[index] + references[index + 1] for index in range(len(references) - 1))
    best_score = 0.0
    best_match = ''
    for candidate in candidates:
        if not candidate:
            continue
        ratio = SequenceMatcher(None, segment_norm, candidate).ratio()
        candidate_ngrams = ngrams(candidate)
        jaccard = (
            len(segment_ngrams & candidate_ngrams) / len(segment_ngrams | candidate_ngrams)
            if segment_ngrams or candidate_ngrams
            else 0.0
        )
        score = max(ratio, 0.35 * ratio + 0.65 * jaccard)
        if score > best_score:
            best_score = score
            best_match = candidate
    return best_score, best_match


def is_obvious_hallucination(text: str, score: float) -> bool:
    if any(pattern.search(text) for pattern in META_PATTERNS):
        return True
    normalized = normalize(text)
    if REPEAT_CHAR_RE.search(normalized):
        return True
    return score < 0.38 and len(normalized) >= 12


def classify_segments(path: Path, references: list[str], reference_joined: str) -> dict:
    data = json.loads(path.read_text(encoding='utf-8'))
    entries = []
    totals = {
        'segments': 0,
        'supported': 0,
        'partial': 0,
        'unsupported': 0,
        'hallucination': 0,
        'duration_s': 0.0,
        'unsupported_duration_s': 0.0,
        'hallucination_duration_s': 0.0,
        'chars': 0,
        'unsupported_chars': 0,
        'hallucination_chars': 0,
    }
    for index, segment in enumerate(data.get('segments', [])):
        text = str(segment.get('text', '')).strip()
        if not text:
            continue
        start = float(segment.get('start_s', 0) or 0)
        end = float(segment.get('end_s', 0) or 0)
        duration = max(0.0, end - start)
        score, match = support_score(text, references, reference_joined)
        if score >= 0.72:
            status = 'supported'
        elif score >= 0.50:
            status = 'partial'
        else:
            status = 'unsupported'
        if is_obvious_hallucination(text, score):
            status = 'hallucination'
        chars = len(normalize(text))
        totals['segments'] += 1
        totals[status] += 1
        totals['duration_s'] += duration
        totals['chars'] += chars
        if status in {'unsupported', 'hallucination'}:
            totals['unsupported_duration_s'] += duration
            totals['unsupported_chars'] += chars
        if status == 'hallucination':
            totals['hallucination_duration_s'] += duration
            totals['hallucination_chars'] += chars
        entries.append({
            'index': index,
            'start_s': start,
            'end_s': end,
            'duration_s': duration,
            'text': text,
            'support_score': round(score, 3),
            'status': status,
            'best_reference_match': match[:100],
        })
    duration = totals['duration_s'] or 1.0
    chars = totals['chars'] or 1
    totals['unsupported_segment_rate'] = round((totals['unsupported'] + totals['hallucination']) / totals['segments'], 4) if totals['segments'] else None
    totals['hallucination_segment_rate'] = round(totals['hallucination'] / totals['segments'], 4) if totals['segments'] else None
    totals['unsupported_duration_rate'] = round(totals['unsupported_duration_s'] / duration, 4)
    totals['hallucination_duration_rate'] = round(totals['hallucination_duration_s'] / duration, 4)
    totals['unsupported_char_rate'] = round(totals['unsupported_chars'] / chars, 4)
    totals['hallucination_char_rate'] = round(totals['hallucination_chars'] / chars, 4)
    return {'path': str(path), 'totals': totals, 'entries': entries}


def main() -> None:
    if OUT_JSON.exists() or OUT_MD.exists():
        raise SystemExit('Refusing to overwrite existing hallucination metrics artifacts')
    references = [normalize(line) for line in lyric_reference_lines() if normalize(line)]
    reference_joined = ''.join(references)
    results = {
        name: classify_segments(path, references, reference_joined)
        for name, path in TRANSCRIPTS.items()
    }
    best = min(results, key=lambda name: (results[name]['totals']['hallucination_duration_rate'], results[name]['totals']['unsupported_duration_rate']))
    report = {
        'kind': 'transcript unsupported and hallucinated content metrics',
        'reference_lyrics_path': str(LYRICS_PATH),
        'method': 'Content-only transcript segment support against lyric reference. supported>=0.72, partial>=0.50, unsupported<0.50; known meta/repetition patterns counted as hallucination. No timing inferred from lyrics.',
        'best_by_lowest_hallucination_then_unsupported_duration': best,
        'results': results,
    }
    OUT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    lines = [
        '# Transcript Hallucination Metrics v1',
        '',
        'Lyrics are content reference only; timing is not inferred from lyrics.',
        '',
        '| Transcript | Segments | Supported | Partial | Unsupported | Hallucinated | Unsupported duration | Hallucination duration | Unsupported chars | Hallucination chars |',
        '| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |',
    ]
    for name, result in results.items():
        total = result['totals']
        lines.append(
            f"| {name} | {total['segments']} | {total['supported']} | {total['partial']} | "
            f"{total['unsupported']} | {total['hallucination']} | {total['unsupported_duration_rate']:.2%} | "
            f"{total['hallucination_duration_rate']:.2%} | {total['unsupported_char_rate']:.2%} | "
            f"{total['hallucination_char_rate']:.2%} |"
        )
    lines.extend(['', f'Best by lowest hallucination duration then unsupported duration: `{best}`.', ''])
    lines.append('## Hallucinated/Unsupported Segments For Best Transcript')
    for entry in results[best]['entries']:
        if entry['status'] in {'unsupported', 'hallucination'}:
            lines.append(
                f"- `{entry['status']}` {entry['start_s']:.2f}-{entry['end_s']:.2f}s "
                f"score `{entry['support_score']}`: {entry['text']}"
            )
    OUT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(json.dumps({
        'json': str(OUT_JSON),
        'markdown': str(OUT_MD),
        'best': best,
        'summary': {name: result['totals'] for name, result in results.items()},
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
