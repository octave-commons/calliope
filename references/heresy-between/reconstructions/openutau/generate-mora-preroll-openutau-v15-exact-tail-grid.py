#!/usr/bin/env python3
"""Generate v15 exact-tail-grid OpenUTAU render units.

v15 keeps v14 but reproduces the exact integer tick grid from the phrase probe
for the 世界 render unit. The difference is small (1-4 ticks) but local STT was
sensitive to the overlap/gap drift from source-time rounding.
"""
import datetime as dt
import json
import re
from pathlib import Path

import pykakasi

BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
OUT_BASE = BASE / 'reconstructions' / 'openutau'
OUT_DIR = OUT_BASE / 'v15-exact-tail-grid-chunks'
HYBRID_PATH = BASE / 'lyrics' / 'hybrid-transcript-layer-v1-large-with-medium-tail.json'
GESTURE_PATH = BASE / 'lyrics' / 'auditions' / 'gesture-layer-v3-hybrid-safe.json'
MANIFEST_PATH = OUT_BASE / 'heresy-between-exact-tail-grid-openutau-v15.json'
TICKS_PER_SECOND = 800
PREROLL_SECONDS = 0.5
TARGET_DURATION = 303.32
SMALL_TSU_TICKS = 80

# Render units are assembled in this exact order. render_start/render_end select
# notes and render context; assembly_start/assembly_end select the audible span.
# render_tail_seconds extends the voice part after render_end so short phrases do
# not get clipped by OpenUTAU/WORLDLINE-R before ffmpeg trimming.
RENDER_UNITS = [
    {
        'id': 'unit-01-opening',
        'render_start': 0.0,
        'render_end': 55.26,
        'assembly_start': 0.0,
        'assembly_end': 55.26,
        'render_tail_seconds': 0.5,
        'policy': 'v10-opening-small-tsu-preserved',
    },
    {
        'id': 'unit-02a-timeline-with-context',
        'render_start': 55.26,
        'render_end': 69.22,
        'assembly_start': 55.26,
        'assembly_end': 59.44,
        'render_tail_seconds': 0.5,
        'policy': 'timeline-segment-with-following-context',
    },
    {
        'id': 'unit-02b-gap',
        'render_start': 59.44,
        'render_end': 60.28,
        'assembly_start': 59.44,
        'assembly_end': 60.28,
        'kind': 'silence',
        'policy': 'timeline-gap-silence',
    },
    {
        'id': 'unit-02c-tsuyogatte-scroll',
        'render_start': 60.28,
        'render_end': 64.82,
        'assembly_start': 60.28,
        'assembly_end': 64.82,
        'render_tail_seconds': 0.5,
        'policy': 'literal-small-tsu-and-long-vowel-source-text-segment-11',
    },
    {
        'id': 'unit-02d-uchikake',
        'render_start': 60.28,
        'render_end': 69.22,
        'assembly_start': 64.82,
        'assembly_end': 69.22,
        'render_tail_seconds': 0.5,
        'policy': 'tail-from-segment-11-12-context',
    },
    {
        'id': 'unit-03-middle-pre-sekai',
        'render_start': 69.22,
        'render_end': 103.64,
        'assembly_start': 69.22,
        'assembly_end': 103.64,
        'render_tail_seconds': 0.5,
        'policy': 'v10-microphrase-middle',
    },
    {
        'id': 'unit-04-sekai-phrase-with-custom-tail',
        'render_start': 103.64,
        'render_end': 113.86,
        'assembly_start': 103.64,
        'assembly_end': 113.86,
        'render_tail_seconds': 2.0,
        'policy': 'phrase-isolated-render-with-custom-soredakedeii-tail-exact-grid-fixes-sekai',
        'exact_grid_segments': [21, 22],
        'tail_context_lyrics': ['そ', 'れ', 'だ', 'け', 'で', 'い', 'い'],
        'tail_context_duration_ticks': 209,
    },
    {
        'id': 'unit-05-middle-post-sekai',
        'render_start': 113.86,
        'render_end': 173.44,
        'assembly_start': 113.86,
        'assembly_end': 173.44,
        'render_tail_seconds': 0.5,
        'policy': 'v10-microphrase-middle-tail',
    },
    {
        'id': 'unit-06-sorede',
        'render_start': 173.44,
        'render_end': 243.35999989509583,
        'assembly_start': 173.44,
        'assembly_end': 243.35999989509583,
        'render_tail_seconds': 0.5,
        'policy': 'v10-sorede-and-long-gap',
    },
    {
        'id': 'unit-07-ending',
        'render_start': 243.35999989509583,
        'render_end': 303.32,
        'assembly_start': 243.35999989509583,
        'assembly_end': 303.32,
        'render_tail_seconds': 0.5,
        'policy': 'v10-ending',
    },
]

# Use raw transcript text where the reference matcher removed important phonetic
# material. Segment 11 preserves スクロール and っ; other segments keep the
# best-reference correction layer.
READING_SOURCE_OVERRIDES = {
    11: 'text',
}

SMALL_KANA = set('ゃゅょぁぃぅぇぉャュョァィゥェォ')
VOWELS = {
    'あ': 'あ', 'か': 'あ', 'が': 'あ', 'さ': 'あ', 'ざ': 'あ', 'た': 'あ', 'だ': 'あ', 'な': 'あ', 'は': 'あ', 'ば': 'あ', 'ぱ': 'あ', 'ま': 'あ', 'や': 'あ', 'ら': 'あ', 'わ': 'あ',
    'い': 'い', 'き': 'い', 'ぎ': 'い', 'し': 'い', 'じ': 'い', 'ち': 'い', 'ぢ': 'い', 'に': 'い', 'ひ': 'い', 'び': 'い', 'ぴ': 'い', 'み': 'い', 'り': 'い',
    'う': 'う', 'く': 'う', 'ぐ': 'う', 'す': 'う', 'ず': 'う', 'つ': 'う', 'づ': 'う', 'ぬ': 'う', 'ふ': 'う', 'ぶ': 'う', 'ぷ': 'う', 'む': 'う', 'ゆ': 'う', 'る': 'う', 'う゛': 'う',
    'え': 'え', 'け': 'え', 'げ': 'え', 'せ': 'え', 'ぜ': 'え', 'て': 'え', 'で': 'え', 'ね': 'え', 'へ': 'え', 'べ': 'え', 'ぺ': 'え', 'め': 'え', 'れ': 'え',
    'お': 'お', 'こ': 'お', 'ご': 'お', 'そ': 'お', 'ぞ': 'お', 'と': 'お', 'ど': 'お', 'の': 'お', 'ほ': 'お', 'ぼ': 'お', 'ぽ': 'お', 'も': 'お', 'よ': 'お', 'ろ': 'お', 'を': 'お',
}
KANA_POOL = [{'lyric': c, 'kind': 'mora'} for c in 'あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわん']


def to_hiragana(text: str, kakasi) -> str:
    converted = ''.join(item.get('hira') or item.get('kana') or item.get('orig') or '' for item in kakasi.convert(text))
    converted = converted.replace('ゔ', 'う').replace('ヴ', 'う')
    converted = re.sub(r'[\s「」『』（）()\[\]【】.,，。!！?？:：;；"“”‘’\-—–・/\\]', '', converted)
    return converted


def mora_tokens_from_hiragana(text: str) -> list[dict]:
    tokens: list[dict] = []
    for char in text:
        if char in ('っ', 'ッ'):
            tokens.append({'lyric': 'っ', 'kind': 'small_tsu'})
            continue
        if char in SMALL_KANA and tokens and tokens[-1]['kind'] == 'mora':
            tokens[-1]['lyric'] += char
            continue
        if char == 'ー':
            if tokens:
                prev = tokens[-1]['lyric']
                tokens.append({'lyric': VOWELS.get(prev[-1], 'あ'), 'kind': 'long_vowel'})
            continue
        if '\u3040' <= char <= '\u309f':
            tokens.append({'lyric': char, 'kind': 'mora'})
    return tokens or KANA_POOL[:]


def reading_for_segment(segment_index: int, segment: dict, kakasi) -> tuple[str, list[dict], str]:
    source_key = READING_SOURCE_OVERRIDES.get(segment_index)
    if source_key:
        source = segment.get(source_key) or segment.get('best_reference_match') or segment.get('text') or ''
    else:
        source = segment.get('best_reference_match') or segment.get('text') or ''
    hira = to_hiragana(str(source), kakasi)
    return hira, mora_tokens_from_hiragana(hira), source_key or 'best_reference_match_or_text'


def allocate_token_spans(tokens: list[dict], start_s: float, end_s: float) -> list[dict]:
    duration_s = end_s - start_s
    small_count = sum(1 for token in tokens if token['kind'] == 'small_tsu')
    normal_count = len(tokens) - small_count
    small_s = SMALL_TSU_TICKS / TICKS_PER_SECOND
    if small_count and normal_count and small_count * small_s > duration_s * 0.25:
        small_s = (duration_s * 0.25) / small_count
    normal_s = (duration_s - small_count * small_s) / max(1, normal_count)
    cursor = start_s
    spans = []
    for idx, token in enumerate(tokens):
        token_duration = small_s if token['kind'] == 'small_tsu' else normal_s
        token_end = end_s if idx == len(tokens) - 1 else cursor + token_duration
        spans.append({**token, 'source_start_s': cursor, 'source_end_s': token_end, 'duration_s': token_end - cursor})
        cursor = token_end
    return spans


def pitch_for_interval(events: list[dict], start_s: float, end_s: float, fallback: int) -> int:
    mids = [float(e.get('midi') or fallback) for e in events if float(e['start_s']) < end_s and float(e['end_s']) > start_s]
    if not mids:
        return fallback
    mids.sort()
    return max(48, min(72, round(mids[len(mids) // 2])))


def note_yaml(note: dict) -> str:
    return f"""  - position: {note['position']}
    duration: {note['duration']}
    tone: {note['tone']}
    lyric: {note['lyric']}
    pitch:
      data:
      - x: -40
        y: 0
        shape: io
      - x: 40
        y: 0
        shape: io
      snap_first: true
    vibrato:
      length: 0
      period: 175
      depth: {10 if note['duration'] >= 320 else 0}
      in: 15
      out: 15
      shift: 0
      drift: 0
      vol_link: 0
    phoneme_expressions: []
    phoneme_overrides: []"""


def ustx_text(unit: dict, notes: list[dict]) -> str:
    render_span = unit['render_end'] - unit['render_start']
    tail = unit.get('render_tail_seconds', 0.0)
    duration = round((render_span + PREROLL_SECONDS + tail) * TICKS_PER_SECOND)
    notes_text = '\n'.join(note_yaml(note) for note in notes)
    return f"""name: heresy-between-exact-tail-grid-openutau-v15-{unit['id']}
comment: 'v15 exact-tail-grid reconstruction: generated phrase-isolated units with exact assembly trims and render-tail policy.'
output_dir: Export
cache_dir: UCache
ustx_version: '0.6'
resolution: 480
key: 0
time_signatures:
- bar_position: 0
  beat_per_bar: 4
  beat_unit: 4
tempos:
- position: 0
  bpm: 100
tracks:
- singer: 波音リツ連続音Ver1.5.1
  phonemizer: OpenUtau.Plugin.Builtin.JapaneseVCVPhonemizer
  renderer_settings:
    renderer: WORLDLINE-R
  track_name: Ritsu exact-tail-grid vocal v15 {unit['id']}
  track_color: Blue
  mute: false
  solo: false
  volume: 0
  pan: 0
  voice_color_names:
  - ''
voice_parts:
- duration: {duration}
  name: Ritsu exact-tail-grid vocal v15 {unit['id']}
  track_no: 0
  position: 0
  notes:
{notes_text}
"""


def main() -> None:
    if MANIFEST_PATH.exists():
        raise SystemExit(f'Refusing to overwrite existing manifest: {MANIFEST_PATH}')
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    kakasi = pykakasi.kakasi()
    hybrid = json.loads(HYBRID_PATH.read_text(encoding='utf-8'))
    gesture = json.loads(GESTURE_PATH.read_text(encoding='utf-8'))
    gesture_events = gesture['events']
    segment_readings = []
    segment_notes = []
    fallback_tone = 60
    for segment_index, segment in enumerate(hybrid['segments']):
        seg_start = float(segment['start_s'])
        seg_end = float(segment['end_s'])
        hira, tokens, source_policy = reading_for_segment(segment_index, segment, kakasi)
        timed_tokens = allocate_token_spans(tokens, seg_start, seg_end)
        segment_readings.append({
            'segment_index': segment_index,
            'start_s': seg_start,
            'end_s': seg_end,
            'text': segment.get('text'),
            'best_reference_match': segment.get('best_reference_match'),
            'source_policy': source_policy,
            'reading_hiragana': hira,
            'tokens': tokens,
            'token_count': len(tokens),
            'small_tsu_count': sum(1 for token in tokens if token['kind'] == 'small_tsu'),
        })
        for i, token in enumerate(timed_tokens):
            n_start = token['source_start_s']
            n_end = token['source_end_s']
            tone = pitch_for_interval(gesture_events, n_start, n_end, fallback_tone)
            if 10 <= segment_index <= 36:
                tone = 60
            fallback_tone = tone
            segment_notes.append({
                'source_segment_index': segment_index,
                'source_token_index': i,
                'source_start_s': n_start,
                'source_end_s': n_end,
                'duration_s': n_end - n_start,
                'tone': tone,
                'lyric': token['lyric'],
                'token_kind': token['kind'],
            })

    manifest_units = []
    preroll_ticks = round(PREROLL_SECONDS * TICKS_PER_SECOND)
    for unit in RENDER_UNITS:
        assembly_duration = unit['assembly_end'] - unit['assembly_start']
        if unit.get('kind') == 'silence':
            manifest_units.append({
                **unit,
                'ustx_path': None,
                'planned_raw_wav_path': None,
                'planned_aligned_wav_path': str(OUT_DIR / f"heresy-between-exact-tail-grid-openutau-v15-{unit['id']}.wav"),
                'assembly_duration_seconds': assembly_duration,
                'trim_start_seconds': 0.0,
                'note_count': 0,
                'first_lyrics': [],
            })
            continue
        notes = []
        for note in segment_notes:
            if not (unit['render_start'] <= note['source_start_s'] < unit['render_end']):
                continue
            duration_ticks = max(40 if note['token_kind'] == 'small_tsu' else 96, round(note['duration_s'] * TICKS_PER_SECOND))
            notes.append({
                **note,
                'position': preroll_ticks + round((note['source_start_s'] - unit['render_start']) * TICKS_PER_SECOND),
                'duration': duration_ticks,
            })
        if unit.get('exact_grid_segments'):
            notes = []
            cursor = preroll_ticks
            for seg_idx in unit['exact_grid_segments']:
                seg = segment_readings[int(seg_idx)]
                seg_ticks = round((float(seg['end_s']) - float(seg['start_s'])) * TICKS_PER_SECOND)
                token_duration = round(seg_ticks / max(1, len(seg['tokens'])))
                for token_idx, token in enumerate(seg['tokens']):
                    notes.append({
                        'position': cursor,
                        'duration': token_duration,
                        'tone': 60 if 10 <= int(seg_idx) <= 36 else fallback_tone,
                        'lyric': token['lyric'],
                        'source_segment_index': seg_idx,
                        'source_token_index': token_idx,
                        'source_start_s': None,
                        'source_end_s': None,
                        'duration_s': token_duration / TICKS_PER_SECOND,
                        'token_kind': token['kind'],
                    })
                    cursor += token_duration
            if unit.get('tail_context_lyrics'):
                tail_duration = int(unit.get('tail_context_duration_ticks', 209))
                tail_tone = int(unit.get('tail_context_tone', 60))
                for lyric in unit['tail_context_lyrics']:
                    notes.append({
                        'position': cursor,
                        'duration': tail_duration,
                        'tone': tail_tone,
                        'lyric': lyric,
                        'source_segment_index': 'tail_context',
                        'source_token_index': None,
                        'source_start_s': None,
                        'source_end_s': None,
                        'duration_s': tail_duration / TICKS_PER_SECOND,
                        'token_kind': 'tail_context',
                    })
                    cursor += tail_duration

        if unit.get('tail_context_lyrics') and not unit.get('exact_grid_segments'):
            cursor = max((int(n['position']) + int(n['duration']) for n in notes), default=preroll_ticks)
            tail_duration = int(unit.get('tail_context_duration_ticks', 209))
            tail_tone = int(unit.get('tail_context_tone', 60))
            for lyric in unit['tail_context_lyrics']:
                notes.append({
                    'position': cursor,
                    'duration': tail_duration,
                    'tone': tail_tone,
                    'lyric': lyric,
                    'source_segment_index': 'tail_context',
                    'source_token_index': None,
                    'source_start_s': None,
                    'source_end_s': None,
                    'duration_s': tail_duration / TICKS_PER_SECOND,
                    'token_kind': 'tail_context',
                })
                cursor += tail_duration

        ustx_path = OUT_DIR / f"heresy-between-exact-tail-grid-openutau-v15-{unit['id']}.ustx"
        if ustx_path.exists():
            raise SystemExit(f'Refusing to overwrite existing render unit: {ustx_path}')
        ustx_path.write_text(ustx_text(unit, notes), encoding='utf-8')
        trim_start = PREROLL_SECONDS + (unit['assembly_start'] - unit['render_start'])
        manifest_units.append({
            **unit,
            'ustx_path': str(ustx_path),
            'planned_raw_wav_path': str(ustx_path).replace('.ustx', '-raw.wav'),
            'planned_aligned_wav_path': str(ustx_path).replace('.ustx', '.wav'),
            'assembly_duration_seconds': assembly_duration,
            'trim_start_seconds': trim_start,
            'note_count': len(notes),
            'first_lyrics': [n['lyric'] for n in notes[:40]],
        })

    manifest = {
        'generated_at': dt.datetime.now(dt.UTC).isoformat(),
        'kind': 'exact-tail-grid OpenUTAU v15 full timeline',
        'source_hybrid_transcript_layer': str(HYBRID_PATH),
        'source_gesture_layer': str(GESTURE_PATH),
        'target_duration_seconds': TARGET_DURATION,
        'singer': '波音リツ連続音Ver1.5.1',
        'phonemizer': 'OpenUtau.Plugin.Builtin.JapaneseVCVPhonemizer',
        'renderer': 'WORLDLINE-R',
        'preroll_seconds': PREROLL_SECONDS,
        'small_tsu_ticks': SMALL_TSU_TICKS,
        'reading_source_overrides': READING_SOURCE_OVERRIDES,
        'total_note_count_available': len(segment_notes),
        'render_units': manifest_units,
        'segment_readings': segment_readings,
        'note_strategy': 'v15 promotes audited phrase probes into generated render units with exact integer tick grids and custom phrase-tail context: exact assembly spans, optional render context, render-tail extension for short phrases, small-tsu preservation, long-vowel preservation for segment 11, and stable pitch for segments 10-36.',
    }
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'manifest_path': str(MANIFEST_PATH),
        'out_dir': str(OUT_DIR),
        'render_units': [{'id': u['id'], 'assembly': [u['assembly_start'], u['assembly_end']], 'trim_start_seconds': u['trim_start_seconds'], 'note_count': u['note_count'], 'first_lyrics': u['first_lyrics'][:24]} for u in manifest_units],
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
