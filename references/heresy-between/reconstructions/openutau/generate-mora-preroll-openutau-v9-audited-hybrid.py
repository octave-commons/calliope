#!/usr/bin/env python3
"""Generate v9 audited-hybrid OpenUTAU chunks.

v9 keeps segment-safe chunking and uses audited stable pitch only for mid-song segments where source-pitch renders failed word recognition.
"""
import datetime as dt
import json
import re
from pathlib import Path

import pykakasi

BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
OUT_BASE = BASE / 'reconstructions' / 'openutau'
OUT_DIR = OUT_BASE / 'v9-audited-hybrid-chunks'
HYBRID_PATH = BASE / 'lyrics' / 'hybrid-transcript-layer-v1-large-with-medium-tail.json'
GESTURE_PATH = BASE / 'lyrics' / 'auditions' / 'gesture-layer-v3-hybrid-safe.json'
MANIFEST_PATH = OUT_BASE / 'heresy-between-audited-hybrid-openutau-v9.json'
TICKS_PER_SECOND = 800
PREROLL_SECONDS = 0.5
TARGET_DURATION = 303.32
# Safe boundaries picked from transcript segment edges near the old v6 chunks.
CHUNKS = [
    {'id': 'chunk-01', 'start': 0.0, 'end': 55.26},
    {'id': 'chunk-02', 'start': 55.26, 'end': 103.64},
    {'id': 'chunk-03', 'start': 103.64, 'end': 173.44},
    {'id': 'chunk-04', 'start': 173.44, 'end': 243.35999989509583},
    {'id': 'chunk-05', 'start': 243.35999989509583, 'end': 303.32},
]
SMALL_KANA = set('ゃゅょぁぃぅぇぉャュョァィゥェォ')
VOWELS = {
    'あ': 'あ', 'か': 'あ', 'が': 'あ', 'さ': 'あ', 'ざ': 'あ', 'た': 'あ', 'だ': 'あ', 'な': 'あ', 'は': 'あ', 'ば': 'あ', 'ぱ': 'あ', 'ま': 'あ', 'や': 'あ', 'ら': 'あ', 'わ': 'あ',
    'い': 'い', 'き': 'い', 'ぎ': 'い', 'し': 'い', 'じ': 'い', 'ち': 'い', 'ぢ': 'い', 'に': 'い', 'ひ': 'い', 'び': 'い', 'ぴ': 'い', 'み': 'い', 'り': 'い',
    'う': 'う', 'く': 'う', 'ぐ': 'う', 'す': 'う', 'ず': 'う', 'つ': 'う', 'づ': 'う', 'ぬ': 'う', 'ふ': 'う', 'ぶ': 'う', 'ぷ': 'う', 'む': 'う', 'ゆ': 'う', 'る': 'う', 'う゛': 'う',
    'え': 'え', 'け': 'え', 'げ': 'え', 'せ': 'え', 'ぜ': 'え', 'て': 'え', 'で': 'え', 'ね': 'え', 'へ': 'え', 'べ': 'え', 'ぺ': 'え', 'め': 'え', 'れ': 'え',
    'お': 'お', 'こ': 'お', 'ご': 'お', 'そ': 'お', 'ぞ': 'お', 'と': 'お', 'ど': 'お', 'の': 'お', 'ほ': 'お', 'ぼ': 'お', 'ぽ': 'お', 'も': 'お', 'よ': 'お', 'ろ': 'お', 'を': 'お',
}
KANA_POOL = list('あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわん')


def to_hiragana(text: str, kakasi) -> str:
    converted = ''.join(item.get('hira') or item.get('kana') or item.get('orig') or '' for item in kakasi.convert(text))
    converted = converted.replace('ゔ', 'う').replace('ヴ', 'う')
    converted = re.sub(r'[\s「」『』（）()\[\]【】.,，。!！?？:：;；"“”‘’\-—–・/\\]', '', converted)
    return converted


def normalize_long_mark(units: list[str]) -> list[str]:
    out: list[str] = []
    for unit in units:
        if unit == 'ー':
            if out:
                out.append(VOWELS.get(out[-1][-1], 'あ'))
            continue
        out.append(unit)
    return out


def morae_from_hiragana(text: str) -> list[str]:
    units: list[str] = []
    for char in text:
        if char in ('っ', 'ッ'):
            continue
        if char in SMALL_KANA and units:
            units[-1] += char
            continue
        if '\u3040' <= char <= '\u309f' or char == 'ー':
            units.append(char)
    return normalize_long_mark(units)


def reading_for_segment(segment: dict, kakasi) -> tuple[str, list[str]]:
    source = segment.get('best_reference_match') or segment.get('text') or ''
    hira = to_hiragana(str(source), kakasi)
    return hira, morae_from_hiragana(hira) or KANA_POOL[:]


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
    return f"""name: heresy-between-audited-hybrid-openutau-v9-{chunk['id']}
comment: 'v9 audited-hybrid reconstruction: one note per mora, segment-safe chunk edges, source pitch except stable-pitch mid-song clarity zones.'
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
  track_name: Ritsu audited-hybrid vocal v9 {chunk['id']}
  track_color: Blue
  mute: false
  solo: false
  volume: 0
  pan: 0
  voice_color_names:
  - ''
voice_parts:
- duration: {duration}
  name: Ritsu audited-hybrid vocal v9 {chunk['id']}
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
        hira, morae = reading_for_segment(segment, kakasi)
        duration = seg_end - seg_start
        mora_duration = duration / len(morae)
        segment_readings.append({
            'segment_index': segment_index,
            'start_s': seg_start,
            'end_s': seg_end,
            'text': segment.get('text'),
            'best_reference_match': segment.get('best_reference_match'),
            'reading_hiragana': hira,
            'morae': morae,
            'mora_count': len(morae),
        })
        for i, lyric in enumerate(morae):
            n_start = seg_start + i * mora_duration
            n_end = seg_start + (i + 1) * mora_duration
            tone = pitch_for_interval(gesture_events, n_start, n_end, fallback_tone)
            if 10 <= segment_index <= 36:
                tone = 60
            fallback_tone = tone
            segment_notes.append({
                'source_segment_index': segment_index,
                'source_mora_index': i,
                'source_start_s': n_start,
                'source_end_s': n_end,
                'duration_s': n_end - n_start,
                'tone': tone,
                'lyric': lyric,
            })
    chunks = []
    preroll_ticks = round(PREROLL_SECONDS * TICKS_PER_SECOND)
    for chunk in CHUNKS:
        notes = []
        for note in segment_notes:
            if not (chunk['start'] <= note['source_start_s'] < chunk['end']):
                continue
            notes.append({
                **note,
                'position': preroll_ticks + round((note['source_start_s'] - chunk['start']) * TICKS_PER_SECOND),
                'duration': max(96, round(note['duration_s'] * TICKS_PER_SECOND)),
            })
        ustx_path = OUT_DIR / f"heresy-between-audited-hybrid-openutau-v9-{chunk['id']}.ustx"
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
            'first_lyrics': [n['lyric'] for n in notes[:32]],
        })
    manifest = {
        'generated_at': dt.datetime.now(dt.UTC).isoformat(),
        'kind': 'audited-hybrid OpenUTAU v9 full chunks',
        'source_hybrid_transcript_layer': str(HYBRID_PATH),
        'source_gesture_layer': str(GESTURE_PATH),
        'target_duration_seconds': TARGET_DURATION,
        'singer': '波音リツ連続音Ver1.5.1',
        'phonemizer': 'OpenUtau.Plugin.Builtin.JapaneseVCVPhonemizer',
        'renderer': 'WORLDLINE-R',
        'preroll_seconds': PREROLL_SECONDS,
        'total_note_count': len(segment_notes),
        'chunks': chunks,
        'segment_readings': segment_readings,
        'note_strategy': 'One sung note per kana mora. Chunk boundaries are transcript segment edges so no chunk starts mid-phrase. Segments 10-36 use stable pitch after v7 ASR/Gemma failed and v8 stable-pitch improved recognition; other segments keep source pitch. Extra pitch events do not create extra lyrics.',
    }
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'manifest_path': str(MANIFEST_PATH),
        'out_dir': str(OUT_DIR),
        'total_note_count': len(segment_notes),
        'chunks': [{'id': c['id'], 'start': c['start'], 'end': c['end'], 'note_count': c['note_count'], 'first_lyrics': c['first_lyrics'][:24]} for c in chunks],
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
