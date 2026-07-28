#!/usr/bin/env python3
import json
import math
import wave
from pathlib import Path

import numpy as np


BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
WORD_LAYER = BASE / 'lyrics' / 'word-timing-layer-v2-large-turbo-gated-cleaned.json'
F0_JSON = BASE / 'vocal' / 'vocals.pyin.f0-narrow-c3-a4.json'
VOCAL_WAV = BASE / 'stems' / 'htdemucs' / '存在論的な“反・虚無”の論証として' / 'vocals.wav'
OUT_DIR = BASE / 'lyrics' / 'auditions'
GESTURE_JSON = OUT_DIR / 'gesture-layer-v2-dense-legato.json'
STEP_WAV = OUT_DIR / 'gesture-step-guide-v2-dense-legato.wav'
HYBRID_WAV = OUT_DIR / 'gesture-hybrid-v2-dense-legato.wav'
SR = 44100


def read_wav_stereo(path: Path) -> tuple[np.ndarray, int]:
    with wave.open(str(path), 'rb') as wav:
        channels = wav.getnchannels()
        sr = wav.getframerate()
        frames = wav.getnframes()
        raw = wav.readframes(frames)
        data = np.frombuffer(raw, dtype=np.int16).astype(np.float32) / 32768.0
    data = data.reshape(-1, channels)
    if channels == 1:
        data = np.repeat(data, 2, axis=1)
    return data, sr


def write_wav_stereo(path: Path, data: np.ndarray, sr: int = SR) -> None:
    clipped = np.clip(data, -0.98, 0.98)
    pcm = (clipped * 32767.0).astype(np.int16)
    with wave.open(str(path), 'wb') as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(sr)
        wav.writeframes(pcm.tobytes())


def midi_to_hz(midi: np.ndarray | float) -> np.ndarray | float:
    return 440.0 * np.power(2.0, (midi - 69.0) / 12.0)


def quantize(midi: float) -> float:
    return round(midi * 2.0) / 2.0


def char_density(text: str) -> int:
    japanese = sum(1 for char in text if '\u3040' <= char <= '\u30ff' or '\u4e00' <= char <= '\u9fff')
    latin = sum(1 for char in text if char.isascii() and char.isalpha())
    return max(1, japanese + round(latin / 3))


def choose_event_count(text: str, duration: float, f0_points: int) -> int:
    # v2 intentionally over-segments relative to v1 so missing syllables are audible.
    text_count = char_density(text)
    time_count = max(1, round(duration / 0.20))
    f0_count = max(1, round(f0_points / 4))
    count = max(text_count, time_count, min(text_count + 2, f0_count))
    return max(1, min(24, count))


def local_frames(frames: list[dict], start: float, end: float) -> list[dict]:
    return [frame for frame in frames if start <= float(frame['time']) <= end]


def note_midi(frames: list[dict], fallback: float) -> tuple[float, int]:
    mids = [float(frame['midi']) for frame in frames if frame.get('midi') is not None and float(frame.get('probability', 0.0)) >= 0.10]
    if not mids:
        return fallback, 0
    arr = np.array(mids, dtype=np.float32)
    lo = np.percentile(arr, 20)
    hi = np.percentile(arr, 80)
    trimmed = arr[(arr >= lo - 0.75) & (arr <= hi + 0.75)]
    if trimmed.size == 0:
        trimmed = arr
    return quantize(float(np.median(trimmed))), int(trimmed.size)


def render_tone(buffer: np.ndarray, start_i: int, end_i: int, midi: float, amp: float, reattack: bool) -> None:
    n = end_i - start_i
    if n <= 0:
        return
    hz = float(midi_to_hz(midi))
    t = np.arange(n, dtype=np.float32) / SR
    env = np.ones(n, dtype=np.float32)
    attack = int((0.008 if reattack else 0.0015) * SR)
    release = int(0.006 * SR)
    attack = min(attack, n // 2)
    release = min(release, n // 2)
    if attack > 1:
        env[:attack] = np.linspace(0.0, 1.0, attack, dtype=np.float32)
    if release > 1:
        env[-release:] = np.linspace(1.0, 0.0, release, dtype=np.float32)
    buffer[start_i:end_i] += np.sin(2 * math.pi * hz * t).astype(np.float32) * env * amp


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for path in [GESTURE_JSON, STEP_WAV, HYBRID_WAV]:
        if path.exists():
            raise SystemExit(f'Refusing to overwrite existing artifact: {path}')
    word_layer = json.loads(WORD_LAYER.read_text(encoding='utf-8'))
    f0 = json.loads(F0_JSON.read_text(encoding='utf-8'))['frames']
    f0 = [frame for frame in f0 if frame.get('voiced') and frame.get('midi') is not None]
    vocal, sr = read_wav_stereo(VOCAL_WAV)
    if sr != SR:
        raise SystemExit(f'Expected {SR} Hz vocal stem, got {sr}')
    duration = vocal.shape[0] / sr
    mono = np.mean(vocal, axis=1)
    guide = np.zeros(vocal.shape[0], dtype=np.float32)
    mask = np.zeros(vocal.shape[0], dtype=np.float32)
    events = []
    rests = []
    previous_midi = 63.0
    previous_phrase_end = 0.0
    previous_event_midi: float | None = None

    for segment_index, segment in enumerate(word_layer['segments']):
        phrase_start = max(0.0, float(segment['start_s']))
        phrase_end = min(duration, float(segment['end_s']))
        if phrase_end <= phrase_start:
            continue
        if phrase_start - previous_phrase_end >= 0.16:
            rests.append({
                'start_s': previous_phrase_end,
                'end_s': phrase_start,
                'duration_s': phrase_start - previous_phrase_end,
                'kind': 'hard_pause' if phrase_start - previous_phrase_end >= 0.65 else 'short_rest',
            })
            previous_event_midi = None
        frames = local_frames(f0, phrase_start, phrase_end)
        event_count = choose_event_count(segment['text'], phrase_end - phrase_start, len(frames))
        slot = (phrase_end - phrase_start) / event_count
        phrase_events = []
        for idx in range(event_count):
            start = phrase_start + idx * slot
            end = phrase_start + (idx + 1) * slot
            if idx == event_count - 1:
                end = phrase_end
            window_frames = local_frames(f0, start, end)
            midi, count = note_midi(window_frames, previous_midi)
            midi = max(previous_midi - 3.0, min(previous_midi + 3.0, midi))
            midi = max(48.0, min(69.0, midi))
            previous_midi = midi
            phrase_events.append({
                'segment_index': segment_index,
                'event_index': idx,
                'start_s': start,
                'end_s': end,
                'duration_s': end - start,
                'midi': midi,
                'hz': float(midi_to_hz(midi)),
                'f0_frame_count': count,
                'text': segment['text'],
            })
        for event in phrase_events:
            start_i = max(0, int(round(event['start_s'] * sr)))
            end_i = min(vocal.shape[0], int(round(event['end_s'] * sr)))
            if end_i <= start_i:
                continue
            amp = float(np.sqrt(np.mean(np.square(mono[start_i:end_i]))))
            same_note = previous_event_midi is not None and abs(event['midi'] - previous_event_midi) <= 0.25
            reattack = not same_note
            event['articulation'] = 'legato_same_note' if same_note else 'reattack'
            render_tone(guide, start_i, end_i, event['midi'], max(0.032, min(0.10, amp * 1.35)), reattack)
            mask[start_i:end_i] = 1.0
            previous_event_midi = event['midi']
            events.append(event)
        previous_phrase_end = phrase_end

    kernel = np.hanning(int(0.028 * sr) * 2 + 1)
    kernel /= np.sum(kernel)
    smooth_mask = np.clip(np.convolve(mask, kernel, mode='same'), 0.0, 1.0)
    guide_stereo = np.column_stack([guide, guide])
    # Include enough real vocal to make missing syllable detection easier, but keep guide audible.
    hybrid = vocal * smooth_mask[:, None] * 0.42 + guide_stereo * 0.82
    write_wav_stereo(STEP_WAV, guide_stereo, sr)
    write_wav_stereo(HYBRID_WAV, hybrid, sr)
    event_seconds = sum(event['duration_s'] for event in events)
    GESTURE_JSON.write_text(json.dumps({
        'kind': 'dense syllable-ish gesture layer with legato same-note holds',
        'word_layer': str(WORD_LAYER),
        'f0_source': str(F0_JSON),
        'vocal_source': str(VOCAL_WAV),
        'step_guide_wav': str(STEP_WAV),
        'hybrid_wav': str(HYBRID_WAV),
        'duration_s': duration,
        'event_count': len(events),
        'event_seconds': round(event_seconds, 3),
        'event_coverage_percent': round(100.0 * event_seconds / duration, 3),
        'rest_count': len(rests),
        'method': 'v2 increases syllable density and treats adjacent same-pitch events as legato holds with minimal reattack to avoid repeated pulses.',
        'events': events,
        'rests': rests,
    }, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'gesture_json': str(GESTURE_JSON),
        'step_wav': str(STEP_WAV),
        'hybrid_wav': str(HYBRID_WAV),
        'events': len(events),
        'rests': len(rests),
        'event_coverage_percent': round(100.0 * event_seconds / duration, 3),
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
