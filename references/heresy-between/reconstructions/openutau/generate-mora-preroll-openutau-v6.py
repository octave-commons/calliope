#!/usr/bin/env python3
"""Generate a v6 OpenUTAU probe that prioritizes word intelligibility.

Fixes tested here:
- one sung note per kana mora instead of cycling lyrics across pitch events;
- no repeated phrase text when pitch events outnumber morae;
- render pre-roll so the first consonant is not clipped at chunk time zero.
"""
import json
import re
from pathlib import Path

import pykakasi

BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
OUT_BASE = BASE / 'reconstructions' / 'openutau'
OUT_DIR = OUT_BASE / 'v6-mora-preroll-opening'
HYBRID_PATH = BASE / 'lyrics' / 'hybrid-transcript-layer-v1-large-with-medium-tail.json'
GESTURE_PATH = BASE / 'lyrics' / 'auditions' / 'gesture-layer-v3-hybrid-safe.json'
USTX_PATH = OUT_DIR / 'heresy-between-mora-preroll-openutau-v6-opening.ustx'
RAW_WAV_PATH = OUT_DIR / 'heresy-between-mora-preroll-openutau-v6-opening-raw.wav'
TRIMMED_WAV_PATH = OUT_DIR / 'heresy-between-mora-preroll-openutau-v6-opening.wav'
MANIFEST_PATH = OUT_DIR / 'heresy-between-mora-preroll-openutau-v6-opening.json'
TICKS_PER_SECOND = 800
PREROLL_SECONDS = 0.5
START_SECONDS = 0.0
END_SECONDS = 10.24

SMALL_KANA = set('ゃゅょぁぃぅぇぉャュョァィゥェォ')
VOWELS = {
    'あ': 'あ', 'か': 'あ', 'が': 'あ', 'さ': 'あ', 'ざ': 'あ', 'た': 'あ', 'だ': 'あ', 'な': 'あ', 'は': 'あ', 'ば': 'あ', 'ぱ': 'あ', 'ま': 'あ', 'や': 'あ', 'ら': 'あ', 'わ': 'あ',
    'い': 'い', 'き': 'い', 'ぎ': 'い', 'し': 'い', 'じ': 'い', 'ち': 'い', 'ぢ': 'い', 'に': 'い', 'ひ': 'い', 'び': 'い', 'ぴ': 'い', 'み': 'い', 'り': 'い',
    'う': 'う', 'く': 'う', 'ぐ': 'う', 'す': 'う', 'ず': 'う', 'つ': 'う', 'づ': 'う', 'ぬ': 'う', 'ふ': 'う', 'ぶ': 'う', 'ぷ': 'う', 'む': 'う', 'ゆ': 'う', 'る': 'う', 'う゛': 'う',
    'え': 'え', 'け': 'え', 'げ': 'え', 'せ': 'え', 'ぜ': 'え', 'て': 'え', 'で': 'え', 'ね': 'え', 'へ': 'え', 'べ': 'え', 'ぺ': 'え', 'め': 'え', 'れ': 'え',
    'お': 'お', 'こ': 'お', 'ご': 'お', 'そ': 'お', 'ぞ': 'お', 'と': 'お', 'ど': 'お', 'の': 'お', 'ほ': 'お', 'ぼ': 'ぽ', 'ぽ': 'お', 'も': 'お', 'よ': 'お', 'ろ': 'お', 'を': 'お',
}
KANA_POOL = list('あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわん')


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
        # Ritsu VCV has no obvious small-tsu alias; keep timing by assigning
        # the following consonant, but do not create an unsingable っ note.
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
    morae = morae_from_hiragana(hira) or KANA_POOL[:]
    return hira, morae


def pitch_for_interval(events: list[dict], start_s: float, end_s: float, fallback: int) -> int:
    mids = [float(e.get('midi') or fallback) for e in events if float(e['start_s']) < end_s and float(e['end_s']) > start_s]
    if not mids:
        return fallback
    mids.sort()
    return max(48, min(72, round(mids[len(mids) // 2])))


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
      depth: {10 if note['duration'] >= 320 else 0}
      in: 15
      out: 15
      shift: 0
      drift: 0
      vol_link: 0
    phoneme_expressions: []
    phoneme_overrides: []""" for note in notes)


def ustx_text(notes: list[dict]) -> str:
    duration = round((END_SECONDS - START_SECONDS + PREROLL_SECONDS) * TICKS_PER_SECOND)
    return f"""name: heresy-between-mora-preroll-openutau-v6-opening
comment: 'v6 opening probe: one note per mora, no lyric cycling, 0.5s render pre-roll for first consonants.'
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
  track_name: Ritsu mora-preroll vocal v6 opening
  track_color: Blue
  mute: false
  solo: false
  volume: 0
  pan: 0
  voice_color_names:
  - ''
voice_parts:
- duration: {duration}
  name: Ritsu mora-preroll vocal v6 opening
  track_no: 0
  position: 0
  notes:
{yaml_notes(notes)}
"""


def main() -> None:
    if MANIFEST_PATH.exists() or USTX_PATH.exists():
        raise SystemExit(f'Refusing to overwrite existing v6 opening artifacts in {OUT_DIR}')
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    kakasi = pykakasi.kakasi()
    hybrid = json.loads(HYBRID_PATH.read_text(encoding='utf-8'))
    gesture = json.loads(GESTURE_PATH.read_text(encoding='utf-8'))
    gesture_events = gesture['events']
    notes = []
    readings = []
    fallback_tone = 60
    preroll_ticks = round(PREROLL_SECONDS * TICKS_PER_SECOND)
    for segment_index, segment in enumerate(hybrid['segments']):
        seg_start = float(segment['start_s'])
        seg_end = float(segment['end_s'])
        if seg_start >= END_SECONDS or seg_end <= START_SECONDS:
            continue
        hira, morae = reading_for_segment(segment, kakasi)
        duration = seg_end - seg_start
        mora_duration = duration / len(morae)
        readings.append({
            'segment_index': segment_index,
            'start_s': seg_start,
            'end_s': seg_end,
            'text': segment.get('text'),
            'best_reference_match': segment.get('best_reference_match'),
            'reading_hiragana': hira,
            'morae': morae,
            'mora_count': len(morae),
            'note_policy': 'exactly one sung note per mora; no cyclic lyric reuse',
        })
        for i, lyric in enumerate(morae):
            n_start = seg_start + i * mora_duration
            n_end = seg_start + (i + 1) * mora_duration
            tone = pitch_for_interval(gesture_events, n_start, n_end, fallback_tone)
            fallback_tone = tone
            notes.append({
                'position': preroll_ticks + round((n_start - START_SECONDS) * TICKS_PER_SECOND),
                'duration': max(96, round((n_end - n_start) * TICKS_PER_SECOND)),
                'tone': tone,
                'lyric': lyric,
                'source_segment_index': segment_index,
                'source_start_s': n_start,
                'source_end_s': n_end,
            })
    USTX_PATH.write_text(ustx_text(notes), encoding='utf-8')
    manifest = {
        'kind': 'mora-preroll OpenUTAU v6 opening probe',
        'ustx_path': str(USTX_PATH),
        'raw_wav_path': str(RAW_WAV_PATH),
        'trimmed_wav_path': str(TRIMMED_WAV_PATH),
        'source_hybrid_transcript_layer': str(HYBRID_PATH),
        'source_gesture_layer': str(GESTURE_PATH),
        'start_seconds': START_SECONDS,
        'end_seconds': END_SECONDS,
        'preroll_seconds': PREROLL_SECONDS,
        'note_count': len(notes),
        'readings': readings,
        'notes': notes,
        'hypothesis': 'v5 sounded wrong because it cycled lyrics when pitch events outnumbered morae and clipped first-note preutterance at chunk start. v6 sings each mora once and renders with pre-roll.',
    }
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'ustx_path': str(USTX_PATH),
        'manifest_path': str(MANIFEST_PATH),
        'note_count': len(notes),
        'opening_morae': readings[0]['morae'] if readings else [],
        'raw_wav_path': str(RAW_WAV_PATH),
        'trimmed_wav_path': str(TRIMMED_WAV_PATH),
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
