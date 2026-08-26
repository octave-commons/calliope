#!/usr/bin/env python3
import json
import math
import wave
from pathlib import Path

import numpy as np


BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
WORD_LAYER = BASE / 'lyrics' / 'word-timing-layer-v2-large-turbo-gated-cleaned.json'
F0_JSON = BASE / 'vocal' / 'vocals.pyin.f0.json'
VOCAL_WAV = BASE / 'stems' / 'htdemucs' / '存在論的な“反・虚無”の論証として' / 'vocals.wav'
OUT_DIR = BASE / 'lyrics' / 'auditions'
FEATURES_JSON = OUT_DIR / 'word-f0-layer-v1-large-turbo-cleaned.json'
MASKED_WAV = OUT_DIR / 'vocals-accepted-word-mask-v1.wav'
GUIDE_WAV = OUT_DIR / 'word-f0-sine-guide-v1.wav'
HYBRID_WAV = OUT_DIR / 'word-timing-hybrid-audition-v1.wav'
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


def midi_to_hz(midi: float) -> float:
    return 440.0 * (2.0 ** ((midi - 69.0) / 12.0))


def smooth_mask(mask: np.ndarray, ramp_samples: int = 441) -> np.ndarray:
    if ramp_samples <= 1:
        return mask
    kernel = np.hanning(ramp_samples * 2 + 1)
    kernel /= np.sum(kernel)
    return np.clip(np.convolve(mask, kernel, mode='same'), 0.0, 1.0)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for path in [FEATURES_JSON, MASKED_WAV, GUIDE_WAV, HYBRID_WAV]:
        if path.exists():
            raise SystemExit(f'Refusing to overwrite existing artifact: {path}')

    word_layer = json.loads(WORD_LAYER.read_text(encoding='utf-8'))
    f0_frames = json.loads(F0_JSON.read_text(encoding='utf-8'))['frames']
    vocal, sr = read_wav_stereo(VOCAL_WAV)
    if sr != SR:
        raise SystemExit(f'Expected {SR} Hz vocal stem, got {sr}')

    duration = vocal.shape[0] / sr
    mono = np.mean(vocal, axis=1)
    mask = np.zeros(vocal.shape[0], dtype=np.float32)
    guide = np.zeros(vocal.shape[0], dtype=np.float32)
    features = []

    voiced_frames = [frame for frame in f0_frames if frame.get('voiced') and frame.get('midi') is not None]

    for index, word in enumerate(word_layer['words']):
        start = max(0.0, float(word['start_s']))
        end = min(duration, float(word['end_s']))
        if end <= start:
            continue
        start_i = max(0, int(round(start * sr)))
        end_i = min(vocal.shape[0], int(round(end * sr)))
        mask[start_i:end_i] = 1.0
        mids = [float(frame['midi']) for frame in voiced_frames if start <= float(frame['time']) <= end and float(frame.get('probability', 0.0)) >= 0.05]
        probs = [float(frame.get('probability', 0.0)) for frame in voiced_frames if start <= float(frame['time']) <= end and frame.get('midi') is not None]
        if mids:
            median_midi = float(np.median(np.array(mids, dtype=np.float32)))
            mean_midi = float(np.mean(np.array(mids, dtype=np.float32)))
            midi_min = float(np.min(np.array(mids, dtype=np.float32)))
            midi_max = float(np.max(np.array(mids, dtype=np.float32)))
            confidence = float(np.mean(np.array(probs, dtype=np.float32))) if probs else 0.0
        else:
            median_midi = 63.0
            mean_midi = 63.0
            midi_min = 63.0
            midi_max = 63.0
            confidence = 0.0

        amp = float(np.sqrt(np.mean(np.square(mono[start_i:end_i])))) if end_i > start_i else 0.0
        hz = midi_to_hz(median_midi)
        n = end_i - start_i
        if n > 0:
            t = np.arange(n, dtype=np.float32) / sr
            env = np.ones(n, dtype=np.float32)
            ramp = min(n // 2, int(0.012 * sr))
            if ramp > 1:
                env[:ramp] = np.linspace(0, 1, ramp)
                env[-ramp:] = np.linspace(1, 0, ramp)
            guide[start_i:end_i] += np.sin(2 * math.pi * hz * t).astype(np.float32) * env * max(0.035, min(0.12, amp * 1.8))

        features.append({
            'index': index,
            'word': word['word'],
            'start_s': start,
            'end_s': end,
            'duration_s': end - start,
            'median_midi': round(median_midi, 3),
            'mean_midi': round(mean_midi, 3),
            'midi_min': round(midi_min, 3),
            'midi_max': round(midi_max, 3),
            'median_hz': round(hz, 3),
            'f0_frame_count': len(mids),
            'f0_confidence': round(confidence, 4),
            'rms': round(amp, 6),
            'source_probability': word.get('probability'),
        })

    smoothed_mask = smooth_mask(mask, ramp_samples=int(0.025 * sr))
    masked = vocal * smoothed_mask[:, None]
    guide_stereo = np.column_stack([guide, guide])
    hybrid = masked * 0.85 + guide_stereo * 0.45

    write_wav_stereo(MASKED_WAV, masked, sr)
    write_wav_stereo(GUIDE_WAV, guide_stereo, sr)
    write_wav_stereo(HYBRID_WAV, hybrid, sr)

    total_word_s = sum(item['duration_s'] for item in features)
    result = {
        'generated_at': None,
        'kind': 'word-constrained f0/tone layer and audition renders',
        'word_layer': str(WORD_LAYER),
        'f0_source': str(F0_JSON),
        'vocal_source': str(VOCAL_WAV),
        'masked_vocal_wav': str(MASKED_WAV),
        'sine_guide_wav': str(GUIDE_WAV),
        'hybrid_audition_wav': str(HYBRID_WAV),
        'duration_s': duration,
        'word_count': len(features),
        'accepted_word_seconds': round(total_word_s, 3),
        'accepted_word_coverage_percent': round(100.0 * total_word_s / duration, 3),
        'notes': 'Masked vocal lets us hear accepted ASR windows against real voice. Sine guide lets us hear extracted per-word f0 timing without UTAU.',
        'words': features,
    }
    FEATURES_JSON.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'features': str(FEATURES_JSON),
        'masked': str(MASKED_WAV),
        'guide': str(GUIDE_WAV),
        'hybrid': str(HYBRID_WAV),
        'word_count': len(features),
        'coverage_percent': result['accepted_word_coverage_percent'],
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
