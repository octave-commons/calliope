#!/usr/bin/env python3
"""Generate v10 microphrase OpenUTAU chunks.

v10 keeps v9's audited hybrid pitch policy, but fixes the next layer of lyric
articulation bugs:

- split the mid-song failure zones into smaller segment-safe microchunks so a
  large chunk render cannot smear early phrase recognition;
- preserve Japanese small-tsu as a short note instead of dropping it;
- preserve long vowels from source text where the reference matcher collapsed
  them (notably スクロール in segment 11).
"""
import datetime as dt
import json
import re
from pathlib import Path

import pykakasi

BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
OUT_BASE = BASE / 'reconstructions' / 'openutau'
OUT_DIR = OUT_BASE / 'v10-microphrase-chunks'
HYBRID_PATH = BASE / 'lyrics' / 'hybrid-transcript-layer-v1-large-with-medium-tail.json'
GESTURE_PATH = BASE / 'lyrics' / 'auditions' / 'gesture-layer-v3-hybrid-safe.json'
MANIFEST_PATH = OUT_BASE / 'heresy-between-microphrase-openutau-v10.json'
TICKS_PER_SECOND = 800
PREROLL_SECONDS = 0.5
TARGET_DURATION = 303.32
SMALL_TSU_TICKS = 80

# Segment-safe chunks. The additional split points isolate the phrases whose
# first lines passed in phrase probes but failed when embedded in larger chunks.
CHUNKS = [
    {'id': 'chunk-01', 'start': 0.0, 'end': 55.26},
    {'id': 'chunk-02a', 'start': 55.26, 'end': 69.22},
    {'id': 'chunk-02b', 'start': 69.22, 'end': 103.64},
    {'id': 'chunk-03a', 'start': 103.64, 'end': 113.86},
    {'id': 'chunk-03b', 'start': 113.86, 'end': 173.44},
    {'id': 'chunk-04', 'start': 173.44, 'end': 243.35999989509583},
    {'id': 'chunk-05', 'start': 243.35999989509583, 'end': 303.32},
]

# Use the raw transcript text for segments where the reference matcher dropped
# critical phonetic material. Keep best_reference_match elsewhere because it
# corrected several ASR mistakes (e.g. 来なくても vs 動かなくても).
READING_SOURCE_OVERRIDES = {
    11: 'text',  # 強がってスクロールして心だけ取り残す
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
    # Never let short small-tsu notes consume too much of a short segment.
    if small_count and normal_count and small_count * small_s > duration_s * 0.25:
        small_s = (duration_s * 0.25) / small_count
    normal_s = (duration_s - small_count * small_s) / max(1, normal_count)
    cursor = start_s
    spans = []
    for idx, token in enumerate(tokens):
        token_duration = small_s if token['kind'] == 'small_tsu' else normal_s
        if idx == len(tokens) - 1:
            token_end = end_s
        else:
            token_end = cursor + token_duration
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


def ustx_text(chunk: dict, notes: list[dict]) -> str:
    duration = round((chunk['end'] - chunk['start'] + PREROLL_SECONDS) * TICKS_PER_SECOND)
    notes_text = '\n'.join(note_yaml(note) for note in notes)
    return f"""name: heresy-between-microphrase-openutau-v10-{chunk['id']}
comment: 'v10 microphrase reconstruction: one note per mora, literal short small-tsu notes, segment-safe microchunks, audited hybrid pitch.'
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
  track_name: Ritsu microphrase vocal v10 {chunk['id']}
  track_color: Blue
  mute: false
  solo: false
  volume: 0
  pan: 0
  voice_color_names:
  - ''
voice_parts:
- duration: {duration}
  name: Ritsu microphrase vocal v10 {chunk['id']}
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
    chunks = []
    preroll_ticks = round(PREROLL_SECONDS * TICKS_PER_SECOND)
    for chunk in CHUNKS:
        notes = []
        for note in segment_notes:
            if not (chunk['start'] <= note['source_start_s'] < chunk['end']):
                continue
            duration_ticks = max(40 if note['token_kind'] == 'small_tsu' else 96, round(note['duration_s'] * TICKS_PER_SECOND))
            notes.append({
                **note,
                'position': preroll_ticks + round((note['source_start_s'] - chunk['start']) * TICKS_PER_SECOND),
                'duration': duration_ticks,
            })
        ustx_path = OUT_DIR / f"heresy-between-microphrase-openutau-v10-{chunk['id']}.ustx"
        if ustx_path.exists():
            raise SystemExit(f'Refusing to overwrite existing chunk: {ustx_path}')
        ustx_path.write_text(ustx_text(chunk, notes), encoding='utf-8')
        chunks.append({
            **chunk,
            'ustx_path': str(ustx_path),
            'planned_raw_wav_path': str(ustx_path).replace('.ustx', '-raw.wav'),
            'planned_aligned_wav_path': str(ustx_path).replace('.ustx', '.wav'),
            'assembly_offset_seconds': chunk['start'] - PREROLL_SECONDS,
            'note_count': len(notes),
            'first_lyrics': [n['lyric'] for n in notes[:40]],
        })
    manifest = {
        'generated_at': dt.datetime.now(dt.UTC).isoformat(),
        'kind': 'microphrase OpenUTAU v10 full chunks',
        'source_hybrid_transcript_layer': str(HYBRID_PATH),
        'source_gesture_layer': str(GESTURE_PATH),
        'target_duration_seconds': TARGET_DURATION,
        'singer': '波音リツ連続音Ver1.5.1',
        'phonemizer': 'OpenUtau.Plugin.Builtin.JapaneseVCVPhonemizer',
        'renderer': 'WORLDLINE-R',
        'preroll_seconds': PREROLL_SECONDS,
        'small_tsu_ticks': SMALL_TSU_TICKS,
        'reading_source_overrides': READING_SOURCE_OVERRIDES,
        'total_note_count': len(segment_notes),
        'chunks': chunks,
        'segment_readings': segment_readings,
        'note_strategy': 'v10 keeps v9 audited hybrid pitch, preserves small tsu as short notes, preserves long vowels for overridden source-text segments, and uses smaller segment-safe chunks around the previously degraded phrase zones.',
    }
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'manifest_path': str(MANIFEST_PATH),
        'out_dir': str(OUT_DIR),
        'total_note_count': len(segment_notes),
        'chunks': [{'id': c['id'], 'start': c['start'], 'end': c['end'], 'note_count': c['note_count'], 'first_lyrics': c['first_lyrics'][:32]} for c in chunks],
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
