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
CONTOUR_JSON = OUT_DIR / 'phrase-contour-layer-v1-narrow-f0.json'
CONTOUR_WAV = OUT_DIR / 'phrase-contour-sine-v1.wav'
HYBRID_WAV = OUT_DIR / 'phrase-contour-hybrid-v1.wav'
MASKED_VOCAL_WAV = OUT_DIR / 'phrase-contour-masked-vocal-v1.wav'
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


def midi_to_hz(midi: np.ndarray) -> np.ndarray:
    return 440.0 * np.power(2.0, (midi - 69.0) / 12.0)


def smooth(values: np.ndarray, window: int) -> np.ndarray:
    if values.size == 0 or window <= 1:
        return values
    window = min(window, values.size)
    if window % 2 == 0:
        window -= 1
    if window <= 1:
        return values
    kernel = np.hanning(window)
    kernel /= np.sum(kernel)
    pad = window // 2
    padded = np.pad(values, (pad, pad), mode='edge')
    return np.convolve(padded, kernel, mode='valid')


def interpolate_contour(points: list[dict], n: int) -> np.ndarray:
    if not points:
        return np.full(n, 63.0, dtype=np.float32)
    xs = np.array([point['time_s'] for point in points], dtype=np.float32)
    ys = np.array([point['midi'] for point in points], dtype=np.float32)
    target = np.linspace(float(xs[0]), float(xs[-1]), n, dtype=np.float32)
    return np.interp(target, xs, ys).astype(np.float32)


def fade_env(n: int, sr: int) -> np.ndarray:
    env = np.ones(n, dtype=np.float32)
    ramp = min(n // 2, int(0.025 * sr))
    if ramp > 1:
        env[:ramp] = np.linspace(0.0, 1.0, ramp, dtype=np.float32)
        env[-ramp:] = np.linspace(1.0, 0.0, ramp, dtype=np.float32)
    return env


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for path in [CONTOUR_JSON, CONTOUR_WAV, HYBRID_WAV, MASKED_VOCAL_WAV]:
        if path.exists():
            raise SystemExit(f'Refusing to overwrite existing artifact: {path}')

    word_layer = json.loads(WORD_LAYER.read_text(encoding='utf-8'))
    f0_frames = json.loads(F0_JSON.read_text(encoding='utf-8'))['frames']
    vocal, sr = read_wav_stereo(VOCAL_WAV)
    if sr != SR:
        raise SystemExit(f'Expected {SR} Hz vocal stem, got {sr}')

    duration = vocal.shape[0] / sr
    mono = np.mean(vocal, axis=1)
    contour_audio = np.zeros(vocal.shape[0], dtype=np.float32)
    mask = np.zeros(vocal.shape[0], dtype=np.float32)
    phrase_records = []
    voiced = [
        frame for frame in f0_frames
        if frame.get('voiced') and frame.get('midi') is not None and float(frame.get('probability', 0.0)) >= 0.10
    ]

    for index, segment in enumerate(word_layer['segments']):
        start = max(0.0, float(segment['start_s']))
        end = min(duration, float(segment['end_s']))
        if end <= start:
            continue
        start_i = max(0, int(round(start * sr)))
        end_i = min(vocal.shape[0], int(round(end * sr)))
        n = end_i - start_i
        if n <= 0:
            continue
        frames = [frame for frame in voiced if start <= float(frame['time']) <= end]
        points = [
            {
                'time_s': float(frame['time']),
                'midi': float(frame['midi']),
                'probability': float(frame.get('probability', 0.0)),
            }
            for frame in frames
        ]
        if not points:
            continue
        raw_midi = np.array([point['midi'] for point in points], dtype=np.float32)
        lo = np.percentile(raw_midi, 10)
        hi = np.percentile(raw_midi, 90)
        # Keep expressive movement, but suppress obvious frame glitches.
        trimmed_points = [point for point in points if lo - 2.0 <= point['midi'] <= hi + 2.0]
        if len(trimmed_points) < 2:
            trimmed_points = points
        interp = interpolate_contour(trimmed_points, n)
        interp = smooth(interp, int(0.08 * sr))
        # Clamp only after smoothing; this preserves contour while avoiding octave slips.
        interp = np.clip(interp, 48.0, 69.0)
        hz = midi_to_hz(interp)
        phase = np.cumsum(2.0 * math.pi * hz / sr).astype(np.float32)
        amp = float(np.sqrt(np.mean(np.square(mono[start_i:end_i]))))
        phrase = np.sin(phase) * fade_env(n, sr) * max(0.035, min(0.11, amp * 1.5))
        contour_audio[start_i:end_i] += phrase.astype(np.float32)
        mask[start_i:end_i] = 1.0
        phrase_records.append({
            'index': index,
            'start_s': start,
            'end_s': end,
            'duration_s': end - start,
            'text': segment['text'],
            'f0_frame_count': len(points),
            'trimmed_f0_frame_count': len(trimmed_points),
            'median_midi': round(float(np.median(raw_midi)), 3),
            'midi_p10': round(float(lo), 3),
            'midi_p90': round(float(hi), 3),
            'midi_min_used': round(float(np.min(interp)), 3),
            'midi_max_used': round(float(np.max(interp)), 3),
        })

    # Soft phrase mask for the real vocal, for alignment listening.
    kernel = np.hanning(int(0.05 * sr) * 2 + 1)
    kernel /= np.sum(kernel)
    smooth_mask = np.clip(np.convolve(mask, kernel, mode='same'), 0.0, 1.0)
    masked_vocal = vocal * smooth_mask[:, None]
    contour_stereo = np.column_stack([contour_audio, contour_audio])
    hybrid = masked_vocal * 0.55 + contour_stereo * 0.8
    write_wav_stereo(CONTOUR_WAV, contour_stereo, sr)
    write_wav_stereo(HYBRID_WAV, hybrid, sr)
    write_wav_stereo(MASKED_VOCAL_WAV, masked_vocal, sr)
    covered = sum(item['duration_s'] for item in phrase_records)
    CONTOUR_JSON.write_text(json.dumps({
        'kind': 'phrase-level continuous f0 contour layer',
        'word_layer': str(WORD_LAYER),
        'f0_source': str(F0_JSON),
        'vocal_source': str(VOCAL_WAV),
        'contour_wav': str(CONTOUR_WAV),
        'hybrid_wav': str(HYBRID_WAV),
        'masked_vocal_wav': str(MASKED_VOCAL_WAV),
        'duration_s': duration,
        'phrase_count': len(phrase_records),
        'covered_phrase_seconds': round(covered, 3),
        'covered_phrase_percent': round(100.0 * covered / duration, 3),
        'method': 'Accepted ASR segments as phrases; narrow C3-A4 pYIN frames; probability>=0.10; trim 10th/90th percentile glitches; interpolate continuous MIDI; 80ms smoothing; sine contour audition.',
        'phrases': phrase_records,
    }, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'contour_json': str(CONTOUR_JSON),
        'contour_wav': str(CONTOUR_WAV),
        'hybrid_wav': str(HYBRID_WAV),
        'masked_vocal_wav': str(MASKED_VOCAL_WAV),
        'phrase_count': len(phrase_records),
        'coverage_percent': round(100.0 * covered / duration, 3),
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
