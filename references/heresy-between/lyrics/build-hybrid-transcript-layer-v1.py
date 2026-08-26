#!/usr/bin/env python3
import json
import re
import unicodedata
from difflib import SequenceMatcher
from pathlib import Path


BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
LYRICS_PATH = Path('/home/err/devel/Music/heresy_between/存在論的な“反・虚無”の論証として/存在論的な“反・虚無”の論証として.txt')
LARGE_PATH = BASE / 'lyrics/alignment.faster-whisper-large-v3-turbo-gated-cpu-int8-20260514.json'
MEDIUM_PATH = BASE / 'lyrics/alignment.knoxx-whisper-medium-vocals-denoise-norm-20260514.json'
OUT_JSON = BASE / 'lyrics/hybrid-transcript-layer-v1-large-with-medium-tail.json'
OUT_MD = BASE / 'lyrics/hybrid-transcript-layer-v1-summary.md'

PUNCT_RE = re.compile(r'[\s\u3000「」『』（）()\[\]【】*＊"“”\'’`、。，．・…—ー\-:：;；!！?？/／]+')
META_PATTERNS = [
    re.compile(pattern, re.I)
    for pattern in [
        r'HIP HIP', r'ご視聴ありがとうございました', r'スタイルプロンプト', r'World-?Scale',
        r'ゲルケゴール', r'ロールズ', r'必要なら', r'作れるよ', r'Ah ah ah', r'thank you',
        r'チャンネル登録', r'字幕', r'プロンプト', r'so happy', r'chant, chant', r'タタタタタ'
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
        'メッセージ': 'めっせーじ', 'メセージ': 'めっせーじ', '一行': 'いちぎょう', '一個': 'いっこ',
        '一興': 'いっきょう', '意味': 'いみ', '元気': 'げんき', '明日': 'あした', '世界': 'せかい',
        '夜': 'よる', '僕': 'ぼく', '胸': 'むね', '責任': 'せきにん', '送信': 'そうしん',
        '沈黙': 'ちんもく', '決意': 'けつい', '肯定': 'こうてい', '十分': 'じゅうぶん',
    }
    for source, target in replacements.items():
        value = value.replace(source, target)
    return PUNCT_RE.sub('', value)


def ngrams(text: str, size: int = 2) -> set[str]:
    if len(text) < size:
        return {text} if text else set()
    return {text[index:index + size] for index in range(len(text) - size + 1)}


def support_score(text: str, references: list[str], joined: str) -> tuple[float, str]:
    norm = normalize(text)
    if not norm:
        return 0.0, ''
    if norm in joined:
        return 1.0, norm
    text_ngrams = ngrams(norm)
    candidates = list(references)
    candidates.extend(references[index] + references[index + 1] for index in range(len(references) - 1))
    best = (0.0, '')
    for candidate in candidates:
        ratio = SequenceMatcher(None, norm, candidate).ratio()
        candidate_ngrams = ngrams(candidate)
        jaccard = len(text_ngrams & candidate_ngrams) / len(text_ngrams | candidate_ngrams) if (text_ngrams or candidate_ngrams) else 0.0
        score = max(ratio, 0.35 * ratio + 0.65 * jaccard)
        if score > best[0]:
            best = (score, candidate)
    return best


def is_hallucination(text: str, score: float) -> bool:
    if any(pattern.search(text) for pattern in META_PATTERNS):
        return True
    norm = normalize(text)
    if REPEAT_CHAR_RE.search(norm):
        return True
    return score < 0.38 and len(norm) >= 12


def classify_segment(segment: dict, refs: list[str], joined: str, source: str) -> dict:
    text = str(segment.get('text', '')).strip()
    start = float(segment.get('start_s', 0) or 0)
    end = float(segment.get('end_s', 0) or 0)
    score, match = support_score(text, refs, joined)
    if score >= 0.72:
        status = 'supported'
    elif score >= 0.50:
        status = 'partial'
    else:
        status = 'unsupported'
    if is_hallucination(text, score):
        status = 'hallucination'
    return {
        'source': source,
        'start_s': start,
        'end_s': end,
        'duration_s': max(0.0, end - start),
        'text': text,
        'support_score': round(score, 3),
        'status': status,
        'best_reference_match': match[:100],
    }


def overlap(a: dict, b: dict) -> float:
    return max(0.0, min(a['end_s'], b['end_s']) - max(a['start_s'], b['start_s']))


def load_segments(path: Path, refs: list[str], joined: str, source: str) -> list[dict]:
    data = json.loads(path.read_text(encoding='utf-8'))
    return [classify_segment(segment, refs, joined, source) for segment in data.get('segments', []) if str(segment.get('text', '')).strip()]


def summarize(segments: list[dict]) -> dict:
    total_duration = sum(segment['duration_s'] for segment in segments) or 1.0
    total_segments = len(segments) or 1
    halluc = [segment for segment in segments if segment['status'] == 'hallucination']
    unsupported = [segment for segment in segments if segment['status'] in {'unsupported', 'hallucination'}]
    return {
        'segments': len(segments),
        'supported': sum(1 for s in segments if s['status'] == 'supported'),
        'partial': sum(1 for s in segments if s['status'] == 'partial'),
        'unsupported': sum(1 for s in segments if s['status'] == 'unsupported'),
        'hallucination': len(halluc),
        'duration_s': round(total_duration, 3),
        'unsupported_duration_s': round(sum(s['duration_s'] for s in unsupported), 3),
        'hallucination_duration_s': round(sum(s['duration_s'] for s in halluc), 3),
        'unsupported_segment_rate': round(len(unsupported) / total_segments, 4),
        'hallucination_segment_rate': round(len(halluc) / total_segments, 4),
        'unsupported_duration_rate': round(sum(s['duration_s'] for s in unsupported) / total_duration, 4),
        'hallucination_duration_rate': round(sum(s['duration_s'] for s in halluc) / total_duration, 4),
    }


def main() -> None:
    if OUT_JSON.exists() or OUT_MD.exists():
        raise SystemExit('Refusing to overwrite existing hybrid transcript artifacts')
    refs = [normalize(line) for line in lyric_reference_lines() if normalize(line)]
    joined = ''.join(refs)
    large = load_segments(LARGE_PATH, refs, joined, 'large_v3_turbo_gated_cpu_int8')
    medium = load_segments(MEDIUM_PATH, refs, joined, 'medium_denoise_norm')
    selected = []
    rejected = []

    for segment in large:
        if segment['status'] in {'supported', 'partial'}:
            selected.append({**segment, 'selection_reason': 'large_supported_or_partial'})
        else:
            replacement_candidates = [
                candidate for candidate in medium
                if overlap(segment, candidate) >= min(segment['duration_s'], candidate['duration_s']) * 0.25
                and candidate['status'] in {'supported', 'partial'}
            ]
            if replacement_candidates:
                replacement = max(replacement_candidates, key=lambda item: (overlap(segment, item), item['support_score']))
                selected.append({**replacement, 'selection_reason': f"medium_replaces_large_{segment['status']}", 'replaces_large_text': segment['text']})
                rejected.append({**segment, 'rejection_reason': 'replaced_by_medium'})
            else:
                rejected.append({**segment, 'rejection_reason': 'large_unsupported_no_medium_replacement'})

    # Add medium supported segments that cover holes not covered by selected segments.
    for candidate in medium:
        if candidate['status'] not in {'supported', 'partial'}:
            continue
        max_overlap = max((overlap(candidate, segment) for segment in selected), default=0.0)
        if candidate['duration_s'] > 0 and max_overlap / candidate['duration_s'] < 0.35:
            selected.append({**candidate, 'selection_reason': 'medium_fills_uncovered_hole'})

    selected.sort(key=lambda item: (item['start_s'], item['end_s']))
    # Deduplicate near-identical overlaps, preferring higher support and large source.
    deduped = []
    for segment in selected:
        conflict_index = None
        for idx, existing in enumerate(deduped):
            if overlap(segment, existing) >= min(segment['duration_s'], existing['duration_s']) * 0.75:
                conflict_index = idx
                break
        if conflict_index is None:
            deduped.append(segment)
        else:
            existing = deduped[conflict_index]
            segment_rank = (segment['support_score'], 1 if segment['source'].startswith('large') else 0)
            existing_rank = (existing['support_score'], 1 if existing['source'].startswith('large') else 0)
            if segment_rank > existing_rank:
                rejected.append({**existing, 'rejection_reason': 'deduped_lower_confidence'})
                deduped[conflict_index] = segment
            else:
                rejected.append({**segment, 'rejection_reason': 'deduped_lower_confidence'})
    selected = sorted(deduped, key=lambda item: (item['start_s'], item['end_s']))

    report = {
        'kind': 'hybrid transcript layer v1',
        'reference_policy': 'Lyrics are content dictionary only; timings come only from ASR segments.',
        'large_source': str(LARGE_PATH),
        'medium_source': str(MEDIUM_PATH),
        'selection_policy': 'Use large supported/partial segments; replace large unsupported/hallucinated spans with overlapping medium supported/partial segments; add medium supported segments in uncovered holes; dedupe overlaps.',
        'summary': summarize(selected),
        'large_summary': summarize(large),
        'medium_summary': summarize(medium),
        'segments': selected,
        'rejected_segments': rejected,
    }
    OUT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    lines = [
        '# Hybrid Transcript Layer v1',
        '',
        'Lyrics are content dictionary only; timings come only from ASR segments.',
        '',
        f"- Output JSON: `{OUT_JSON}`",
        f"- Large source: `{LARGE_PATH}`",
        f"- Medium source: `{MEDIUM_PATH}`",
        '',
        '## Summary',
        '',
        '| Layer | Segments | Supported | Partial | Unsupported | Hallucinated | Unsupported duration | Hallucination duration |',
        '| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |',
    ]
    for name, summary in [('hybrid', report['summary']), ('large', report['large_summary']), ('medium', report['medium_summary'])]:
        lines.append(
            f"| {name} | {summary['segments']} | {summary['supported']} | {summary['partial']} | {summary['unsupported']} | "
            f"{summary['hallucination']} | {summary['unsupported_duration_rate']:.2%} | {summary['hallucination_duration_rate']:.2%} |"
        )
    lines.extend(['', '## Selected Segments', ''])
    for idx, segment in enumerate(selected):
        lines.append(f"- {idx:02d} `{segment['source']}` `{segment['status']}` {segment['start_s']:.2f}-{segment['end_s']:.2f}s score `{segment['support_score']}`: {segment['text']}")
    OUT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(json.dumps({'json': str(OUT_JSON), 'markdown': str(OUT_MD), 'summary': report['summary']}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
