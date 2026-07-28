#!/usr/bin/env python3
import json
import re
from pathlib import Path

import pykakasi


BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
OUT_BASE = BASE / 'reconstructions' / 'openutau'
CHUNKS_DIR = OUT_BASE / 'v5-reading-aware-chunks'
GESTURE_PATH = BASE / 'lyrics' / 'auditions' / 'gesture-layer-v3-hybrid-safe.json'
HYBRID_PATH = BASE / 'lyrics' / 'hybrid-transcript-layer-v1-large-with-medium-tail.json'
MANIFEST_PATH = OUT_BASE / 'heresy-between-reading-aware-openutau-v5.json'
TICKS_PER_SECOND = 800
TARGET_DURATION = 303.32
CHUNKS = [
    {'id': 'chunk-01', 'start': 0, 'end': 58},
    {'id': 'chunk-02', 'start': 58, 'end': 106},
    {'id': 'chunk-03', 'start': 106, 'end': 174},
    {'id': 'chunk-04', 'start': 174, 'end': 244},
    {'id': 'chunk-05', 'start': 244, 'end': 303.32},
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


def hira_converter():
    kakasi = pykakasi.kakasi()
    return kakasi


def to_hiragana(text: str, kakasi) -> str:
    converted = ''.join(item.get('hira') or item.get('kana') or item.get('orig') or '' for item in kakasi.convert(text))
    converted = converted.replace('ゔ', 'う').replace('ヴ', 'う')
    converted = re.sub(r'[\s「」『』（）()\[\]【】.,，。!！?？:：;；"“”‘’\-—–・/\\]', '', converted)
    return converted


def normalize_long_mark(units: list[str]) -> list[str]:
    normalized: list[str] = []
    for unit in units:
        if unit == 'ー':
            if normalized:
                normalized.append(VOWELS.get(normalized[-1][-1], 'あ'))
            continue
        normalized.append(unit)
    return normalized


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
    morae = morae_from_hiragana(hira)
    if not morae:
        morae = KANA_POOL[:]
    return hira, morae


def lyric_for_event(event: dict, segment_morae: dict[int, list[str]]) -> str:
    segment_index = int(event['segment_index'])
    event_index = int(event['event_index'])
    morae = segment_morae.get(segment_index) or KANA_POOL
    return morae[event_index % len(morae)]


def yaml_notes(notes: list[dict]) -> str:
    return '\n'.join(f"""  - position: {note['position']}
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
      depth: {12 if note['duration'] >= 480 else 0}
      in: 15
      out: 15
      shift: 0
      drift: 0
      vol_link: 0
    phoneme_expressions: []
    phoneme_overrides: []""" for note in notes)


def ustx_for_chunk(chunk: dict, notes: list[dict]) -> str:
    duration = round((chunk['end'] - chunk['start']) * TICKS_PER_SECOND)
    return f"""name: heresy-between-reading-aware-openutau-v5-{chunk['id']}
comment: 'Reading-aware OpenUTAU audition v5. Uses hybrid-safe timing with explicit kana readings from best_reference_match/pykakasi.'
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
  track_name: Ritsu reading-aware vocal v5 {chunk['id']}
  track_color: Blue
  mute: false
  solo: false
  volume: 0
  pan: 0
  voice_color_names:
  - ''
voice_parts:
- duration: {duration}
  name: Ritsu reading-aware vocal v5 {chunk['id']}
  track_no: 0
  position: 0
  notes:
{yaml_notes(notes)}
"""


def main() -> None:
    if MANIFEST_PATH.exists():
        raise SystemExit(f'Refusing to overwrite existing artifact: {MANIFEST_PATH}')
    CHUNKS_DIR.mkdir(parents=True, exist_ok=True)
    gesture = json.loads(GESTURE_PATH.read_text(encoding='utf-8'))
    hybrid = json.loads(HYBRID_PATH.read_text(encoding='utf-8'))
    kakasi = hira_converter()
    segment_readings: dict[int, dict] = {}
    segment_morae: dict[int, list[str]] = {}
    for index, segment in enumerate(hybrid['segments']):
        hira, morae = reading_for_segment(segment, kakasi)
        segment_readings[index] = {
            'text': segment.get('text'),
            'best_reference_match': segment.get('best_reference_match'),
            'reading_hiragana': hira,
            'morae': morae,
            'mora_count': len(morae),
        }
        segment_morae[index] = morae

    records = []
    for chunk in CHUNKS:
        chunk_start_tick = round(chunk['start'] * TICKS_PER_SECOND)
        notes = []
        for event in gesture['events']:
            if not (chunk['start'] <= float(event['start_s']) < chunk['end']):
                continue
            start_tick = max(0, round(float(event['start_s']) * TICKS_PER_SECOND) - chunk_start_tick)
            duration_ticks = max(64, round((float(event['end_s']) - float(event['start_s'])) * TICKS_PER_SECOND))
            notes.append({
                'position': start_tick,
                'duration': duration_ticks,
                'tone': max(48, min(72, round(float(event['midi'])))),
                'lyric': lyric_for_event(event, segment_morae),
                'source_text': event.get('text'),
                'source_start_s': event.get('start_s'),
                'source_end_s': event.get('end_s'),
                'segment_index': event.get('segment_index'),
                'event_index': event.get('event_index'),
                'articulation': event.get('articulation'),
            })
        notes.sort(key=lambda note: note['position'])
        ustx_path = CHUNKS_DIR / f"heresy-between-reading-aware-openutau-v5-{chunk['id']}.ustx"
        if ustx_path.exists():
            raise SystemExit(f'Refusing to overwrite existing artifact: {ustx_path}')
        ustx_path.write_text(ustx_for_chunk(chunk, notes), encoding='utf-8')
        records.append({
            **chunk,
            'duration_seconds': round(chunk['end'] - chunk['start'], 3),
            'ustx_path': str(ustx_path),
            'planned_wav_path': str(ustx_path).replace('.ustx', '.wav'),
            'note_count': len(notes),
            'notes': notes,
        })

    note_seconds = sum(note['duration'] / TICKS_PER_SECOND for record in records for note in record['notes'])
    manifest = {
        'generated_at': __import__('datetime').datetime.now(__import__('datetime').UTC).isoformat(),
        'kind': 'Reading-aware hybrid-safe OpenUTAU audition v5',
        'source_gesture_layer': str(GESTURE_PATH),
        'source_hybrid_transcript_layer': str(HYBRID_PATH),
        'source_f0': gesture.get('f0_source'),
        'target_duration_seconds': TARGET_DURATION,
        'singer': '波音リツ連続音Ver1.5.1',
        'phonemizer': 'OpenUtau.Plugin.Builtin.JapaneseVCVPhonemizer',
        'renderer': 'WORLDLINE-R',
        'total_note_count': sum(record['note_count'] for record in records),
        'total_note_seconds': round(note_seconds, 3),
        'note_coverage_percent': round(100 * note_seconds / TARGET_DURATION, 3),
        'chunks': [{key: value for key, value in record.items() if key != 'notes'} for record in records],
        'segment_readings': segment_readings,
        'note_strategy': 'Use hybrid-safe ASR timing and f0, but assign explicit hiragana mora lyrics from best_reference_match where available, converted by pykakasi. This corrects the v3/v4 kanji-skipping failure where 元気 became missing from the sung lyrics.',
    }
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'manifest_path': str(MANIFEST_PATH),
        'total_note_count': manifest['total_note_count'],
        'note_coverage_percent': manifest['note_coverage_percent'],
        'opening_reading': segment_readings[0],
        'chunks': [{'id': record['id'], 'note_count': record['note_count'], 'ustx': record['ustx_path']} for record in records],
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
