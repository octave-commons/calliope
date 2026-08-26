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
GESTURE_JSON = OUT_DIR / 'gesture-layer-v1-syllable-notes.json'
STEP_WAV = OUT_DIR / 'gesture-step-guide-v1.wav'
HYBRID_WAV = OUT_DIR / 'gesture-hybrid-v1.wav'
BREATH_MASK_WAV = OUT_DIR / 'gesture-rest-breath-mask-v1.wav'
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


def kana_like_count(text: str) -> int:
    # ASR text is imperfect, but Japanese character count gives a useful syllable-ish density.
    count = sum(1 for char in text if '\u3040' <= char <= '\u30ff' or '\u4e00' <= char <= '\u9fff')
    return max(1, count)


def choose_event_count(text: str, duration: float, f0_points: int) -> int:
    text_density = kana_like_count(text)
    duration_density = max(1, round(duration / 0.32))
    count = min(text_density, duration_density)
    if f0_points < 5:
        count = min(count, 2)
    return max(1, min(14, count))


def local_frames(frames: list[dict], start: float, end: float) -> list[dict]:
    return [frame for frame in frames if start <= float(frame['time']) <= end]


def note_midi_for_window(frames: list[dict], fallback: float) -> tuple[float, int, float]:
    mids = [float(frame['midi']) for frame in frames if frame.get('midi') is not None and float(frame.get('probability', 0.0)) >= 0.12]
    probs = [float(frame.get('probability', 0.0)) for frame in frames if frame.get('midi') is not None]
    if not mids:
        return fallback, 0, 0.0
    arr = np.array(mids, dtype=np.float32)
    lo = np.percentile(arr, 15)
    hi = np.percentile(arr, 85)
    trimmed = arr[(arr >= lo - 1.0) & (arr <= hi + 1.0)]
    if trimmed.size == 0:
        trimmed = arr
    confidence = float(np.mean(np.array(probs, dtype=np.float32))) if probs else 0.0
    return quantize(float(np.median(trimmed))), int(trimmed.size), confidence


def fade(n: int, attack: int, release: int) -> np.ndarray:
    env = np.ones(n, dtype=np.float32)
    attack = min(attack, n // 2)
    release = min(release, n // 2)
    if attack > 1:
        env[:attack] = np.linspace(0.0, 1.0, attack, dtype=np.float32)
    if release > 1:
        env[-release:] = np.linspace(1.0, 0.0, release, dtype=np.float32)
    return env


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for path in [GESTURE_JSON, STEP_WAV, HYBRID_WAV, BREATH_MASK_WAV]:
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
    breath_mask = np.zeros(vocal.shape[0], dtype=np.float32)
    events = []
    rests = []
    previous_midi = 63.0
    previous_phrase_end = 0.0

    for segment_index, segment in enumerate(word_layer['segments']):
        phrase_start = max(0.0, float(segment['start_s']))
        phrase_end = min(duration, float(segment['end_s']))
        if phrase_end <= phrase_start:
            continue
        if phrase_start - previous_phrase_end >= 0.18:
            rests.append({
                'start_s': previous_phrase_end,
                'end_s': phrase_start,
                'duration_s': phrase_start - previous_phrase_end,
                'kind': 'hard_pause' if phrase_start - previous_phrase_end >= 0.65 else 'short_rest',
            })
        phrase_frames = local_frames(f0, phrase_start, phrase_end)
        event_count = choose_event_count(segment['text'], phrase_end - phrase_start, len(phrase_frames))
        gap = min(0.045, (phrase_end - phrase_start) / max(1, event_count) * 0.12)
        slot = (phrase_end - phrase_start - gap * max(0, event_count - 1)) / event_count
        phrase_midis = []
        phrase_events = []
        for idx in range(event_count):
            start = phrase_start + idx * (slot + gap)
            end = start + slot
            if idx == event_count - 1:
                # Preserve phrase tails. Earlier guides often clipped them too hard.
                end = phrase_end
            frames = local_frames(f0, start, end)
            midi, frame_count, confidence = note_midi_for_window(frames, previous_midi)
            midi = max(previous_midi - 4.0, min(previous_midi + 4.0, midi))
            midi = max(48.0, min(69.0, midi))
            phrase_midis.append(midi)
            previous_midi = midi
            phrase_events.append({
                'segment_index': segment_index,
                'event_index': idx,
                'start_s': start,
                'end_s': end,
                'duration_s': end - start,
                'midi': midi,
                'hz': float(midi_to_hz(midi)),
                'f0_frame_count': frame_count,
                'f0_confidence': confidence,
                'text': segment['text'],
            })
        # Classify gesture shape at phrase level.
        if len(phrase_midis) >= 3 and max(phrase_midis) - min(phrase_midis) <= 1.0:
            gesture = 'stable_syllabic'
        elif len(phrase_midis) >= 3 and all(b >= a - 0.5 for a, b in zip(phrase_midis, phrase_midis[1:])):
            gesture = 'rising_phrase'
        elif len(phrase_midis) >= 3 and all(b <= a + 0.5 for a, b in zip(phrase_midis, phrase_midis[1:])):
            gesture = 'falling_phrase'
        else:
            gesture = 'stepped_phrase'
        for event in phrase_events:
            event['gesture'] = gesture
            events.append(event)
            start_i = max(0, int(round(event['start_s'] * sr)))
            end_i = min(vocal.shape[0], int(round(event['end_s'] * sr)))
            if end_i <= start_i:
                continue
            n = end_i - start_i
            amp = float(np.sqrt(np.mean(np.square(mono[start_i:end_i])))) if n > 0 else 0.0
            # Mostly hard syllable notes, with tiny transition ramps. No fake trills.
            hz = float(event['hz'])
            t = np.arange(n, dtype=np.float32) / sr
            env = fade(n, int(0.01 * sr), int(0.018 * sr))
            guide[start_i:end_i] += np.sin(2 * math.pi * hz * t).astype(np.float32) * env * max(0.035, min(0.105, amp * 1.45))
            mask[start_i:end_i] = 1.0
        previous_phrase_end = phrase_end

    # Breath/rest layer: retain quiet original vocal in explicit pauses so silence grammar is audible.
    for rest in rests:
        start_i = max(0, int(round(rest['start_s'] * sr)))
        end_i = min(vocal.shape[0], int(round(rest['end_s'] * sr)))
        if end_i <= start_i:
            continue
        breath_mask[start_i:end_i] = 1.0 if rest['kind'] == 'hard_pause' else 0.55

    kernel = np.hanning(int(0.035 * sr) * 2 + 1)
    kernel /= np.sum(kernel)
    smooth_mask = np.clip(np.convolve(mask, kernel, mode='same'), 0.0, 1.0)
    smooth_breath = np.clip(np.convolve(breath_mask, kernel, mode='same'), 0.0, 1.0)
    guide_stereo = np.column_stack([guide, guide])
    masked_real = vocal * smooth_mask[:, None]
    rest_breath = vocal * smooth_breath[:, None] * 0.55
    hybrid = masked_real * 0.45 + rest_breath + guide_stereo * 0.75
    write_wav_stereo(STEP_WAV, guide_stereo, sr)
    write_wav_stereo(HYBRID_WAV, hybrid, sr)
    write_wav_stereo(BREATH_MASK_WAV, rest_breath, sr)

    event_seconds = sum(event['duration_s'] for event in events)
    rest_seconds = sum(rest['duration_s'] for rest in rests)
    GESTURE_JSON.write_text(json.dumps({
        'kind': 'syllable-ish gesture layer with explicit rests and phrase tails',
        'word_layer': str(WORD_LAYER),
        'f0_source': str(F0_JSON),
        'vocal_source': str(VOCAL_WAV),
        'step_guide_wav': str(STEP_WAV),
        'hybrid_wav': str(HYBRID_WAV),
        'rest_breath_mask_wav': str(BREATH_MASK_WAV),
        'duration_s': duration,
        'event_count': len(events),
        'event_seconds': round(event_seconds, 3),
        'event_coverage_percent': round(100.0 * event_seconds / duration, 3),
        'rest_count': len(rests),
        'rest_seconds': round(rest_seconds, 3),
        'method': 'Accepted ASR segments split into syllable-ish events by text/duration density; narrow C3-A4 f0 median per event; phrase tails extended; explicit rest/breath mask from inter-phrase gaps; no continuous trill rendering.',
        'events': events,
        'rests': rests,
    }, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'gesture_json': str(GESTURE_JSON),
        'step_wav': str(STEP_WAV),
        'hybrid_wav': str(HYBRID_WAV),
        'rest_breath_wav': str(BREATH_MASK_WAV),
        'events': len(events),
        'rests': len(rests),
        'event_coverage_percent': round(100.0 * event_seconds / duration, 3),
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
