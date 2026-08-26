#!/usr/bin/env python3
import json
import wave
from pathlib import Path

import numpy as np


BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
ORIGINAL_VOCAL = BASE / 'stems' / 'htdemucs' / '存在論的な“反・虚無”の論証として' / 'vocals.wav'
UTAU_VOCAL = BASE / 'reconstructions' / 'openutau' / 'heresy-between-gesture-openutau-v3.wav'
WORD_LAYER = BASE / 'lyrics' / 'word-timing-layer-v2-large-turbo-gated-cleaned.json'
OUT_DIR = BASE / 'reconstructions' / 'openutau' / 'diagnostics-v3'
SPLIT_WAV = OUT_DIR / 'original-left-openutau-right-v3.wav'
AB_WAV = OUT_DIR / 'phrase-ab-original-then-openutau-v3.wav'
MANIFEST = OUT_DIR / 'phrase-comparison-v3.json'
SR = 44100


def read_wav(path: Path) -> tuple[np.ndarray, int]:
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


def write_wav(path: Path, data: np.ndarray, sr: int = SR) -> None:
    clipped = np.clip(data, -0.98, 0.98)
    pcm = (clipped * 32767).astype(np.int16)
    with wave.open(str(path), 'wb') as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(sr)
        wav.writeframes(pcm.tobytes())


def rms(audio: np.ndarray) -> float:
    if audio.size == 0:
        return 0.0
    return float(np.sqrt(np.mean(np.square(audio))))


def segment_audio(audio: np.ndarray, start: float, end: float) -> np.ndarray:
    start_i = max(0, int(round(start * SR)))
    end_i = min(audio.shape[0], int(round(end * SR)))
    if end_i <= start_i:
        return np.zeros((0, 2), dtype=np.float32)
    return audio[start_i:end_i]


def pad_seconds(seconds: float) -> np.ndarray:
    return np.zeros((int(round(seconds * SR)), 2), dtype=np.float32)


def tone_marker(freq: float = 880.0, seconds: float = 0.08, gain: float = 0.07) -> np.ndarray:
    n = int(round(seconds * SR))
    t = np.arange(n, dtype=np.float32) / SR
    wave_data = np.sin(2 * np.pi * freq * t).astype(np.float32) * gain
    env = np.hanning(n).astype(np.float32)
    return np.column_stack([wave_data * env, wave_data * env])


def match_level(src: np.ndarray, target_rms: float) -> np.ndarray:
    current = rms(src)
    if current <= 1e-6:
        return src
    gain = min(4.0, target_rms / current)
    return src * gain


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for path in [SPLIT_WAV, AB_WAV, MANIFEST]:
        if path.exists():
            raise SystemExit(f'Refusing to overwrite existing artifact: {path}')
    original, sr1 = read_wav(ORIGINAL_VOCAL)
    utau, sr2 = read_wav(UTAU_VOCAL)
    if sr1 != SR or sr2 != SR:
        raise SystemExit(f'Expected {SR} Hz, got {sr1} and {sr2}')
    n = min(original.shape[0], utau.shape[0])
    original = original[:n]
    utau = utau[:n]

    # Split-stereo global comparison: original on left, OpenUTAU on right.
    original_mono = np.mean(original, axis=1)
    utau_mono = np.mean(utau, axis=1)
    utau_mono = match_level(utau_mono, rms(original_mono))
    split = np.column_stack([original_mono * 0.9, utau_mono * 0.9])
    write_wav(SPLIT_WAV, split, SR)

    layer = json.loads(WORD_LAYER.read_text(encoding='utf-8'))
    records = []
    ab_parts = []
    for idx, segment in enumerate(layer['segments']):
        start = max(0.0, float(segment['start_s']) - 0.12)
        end = min(n / SR, float(segment['end_s']) + 0.18)
        orig_seg = segment_audio(original, start, end)
        utau_seg = segment_audio(utau, start, end)
        if orig_seg.size == 0 or utau_seg.size == 0:
            continue
        target = rms(orig_seg)
        utau_matched = match_level(utau_seg, target)
        ab_parts.extend([
            tone_marker(660.0, 0.06),
            orig_seg * 0.95,
            pad_seconds(0.18),
            tone_marker(990.0, 0.06),
            utau_matched * 0.95,
            pad_seconds(0.45),
        ])
        # Simple diagnostics: same-window RMS and normalized correlation of mono envelopes.
        o = np.mean(orig_seg, axis=1)
        u = np.mean(utau_matched, axis=1)
        length = min(o.shape[0], u.shape[0])
        corr = 0.0
        if length > 10 and np.std(o[:length]) > 1e-6 and np.std(u[:length]) > 1e-6:
            corr = float(np.corrcoef(o[:length], u[:length])[0, 1])
        records.append({
            'index': idx,
            'start_s': start,
            'end_s': end,
            'duration_s': end - start,
            'text': segment['text'],
            'original_rms': round(target, 6),
            'utau_rms_before_match': round(rms(utau_seg), 6),
            'mono_correlation_after_level_match': round(corr, 4),
            'ab_order': 'marker_low/original, marker_high/openutau, pause',
        })

    ab = np.concatenate(ab_parts, axis=0) if ab_parts else np.zeros((1, 2), dtype=np.float32)
    write_wav(AB_WAV, ab, SR)
    MANIFEST.write_text(json.dumps({
        'kind': 'OpenUTAU v3 phrase comparison diagnostics',
        'original_vocal': str(ORIGINAL_VOCAL),
        'openutau_vocal': str(UTAU_VOCAL),
        'word_layer': str(WORD_LAYER),
        'split_stereo_wav': str(SPLIT_WAV),
        'phrase_ab_wav': str(AB_WAV),
        'phrase_count': len(records),
        'instructions': [
            'Split stereo: original isolated vocal is left, OpenUTAU v3 is right.',
            'Phrase A/B: low beep then original phrase, high beep then OpenUTAU phrase, short pause.',
            'Use this to mark phrases that are tight vs sliding/off.'
        ],
        'phrases': records,
    }, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'split': str(SPLIT_WAV),
        'ab': str(AB_WAV),
        'manifest': str(MANIFEST),
        'phrases': len(records),
        'ab_duration_s': ab.shape[0] / SR,
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
