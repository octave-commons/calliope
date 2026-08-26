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
FEATURES_JSON = OUT_DIR / 'word-f0-layer-v2-narrow-smoothed.json'
GUIDE_WAV = OUT_DIR / 'word-f0-sine-guide-v2-narrow-smoothed.wav'
HYBRID_WAV = OUT_DIR / 'word-timing-hybrid-audition-v2-narrow-smoothed.wav'
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


def quantize_midi(midi: float) -> float:
    # Keep quarter-tone detail, but avoid jittery exact f0 medians.
    return round(midi * 2.0) / 2.0


def segment_id_for_word(word: dict, segments: list[dict]) -> int | None:
    start = float(word['start_s'])
    end = float(word['end_s'])
    mid = (start + end) * 0.5
    for idx, segment in enumerate(segments):
        if float(segment['start_s']) - 0.05 <= mid <= float(segment['end_s']) + 0.05:
            return idx
    return None


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for path in [FEATURES_JSON, GUIDE_WAV, HYBRID_WAV]:
        if path.exists():
            raise SystemExit(f'Refusing to overwrite existing artifact: {path}')

    word_layer = json.loads(WORD_LAYER.read_text(encoding='utf-8'))
    f0_frames = json.loads(F0_JSON.read_text(encoding='utf-8'))['frames']
    vocal, sr = read_wav_stereo(VOCAL_WAV)
    if sr != SR:
        raise SystemExit(f'Expected {SR} Hz vocal stem, got {sr}')

    duration = vocal.shape[0] / sr
    mono = np.mean(vocal, axis=1)
    guide = np.zeros(vocal.shape[0], dtype=np.float32)
    features = []
    segments = word_layer['segments']
    words = word_layer['words']
    voiced_frames = [frame for frame in f0_frames if frame.get('voiced') and frame.get('midi') is not None and float(frame.get('probability', 0.0)) >= 0.12]

    segment_medians: dict[int, float] = {}
    for idx, segment in enumerate(segments):
        mids = [float(frame['midi']) for frame in voiced_frames if float(segment['start_s']) <= float(frame['time']) <= float(segment['end_s'])]
        if mids:
            segment_medians[idx] = quantize_midi(float(np.median(np.array(mids, dtype=np.float32))))

    previous_midi = 63.0
    for index, word in enumerate(words):
        start = max(0.0, float(word['start_s']))
        end = min(duration, float(word['end_s']))
        if end <= start:
            continue
        start_i = max(0, int(round(start * sr)))
        end_i = min(vocal.shape[0], int(round(end * sr)))
        seg_id = segment_id_for_word(word, segments)
        local_mids = [float(frame['midi']) for frame in voiced_frames if start <= float(frame['time']) <= end]
        if local_mids:
            local = quantize_midi(float(np.median(np.array(local_mids, dtype=np.float32))))
        elif seg_id is not None and seg_id in segment_medians:
            local = segment_medians[seg_id]
        else:
            local = previous_midi

        segment_base = segment_medians.get(seg_id, local) if seg_id is not None else local
        raw_midi = 0.65 * local + 0.35 * segment_base
        # Limit word-to-word jumps; real sung Japanese here is mostly spoken/stepwise.
        midi = max(previous_midi - 3.0, min(previous_midi + 3.0, raw_midi))
        midi = max(48.0, min(69.0, quantize_midi(midi)))
        previous_midi = midi
        hz = midi_to_hz(midi)
        amp = float(np.sqrt(np.mean(np.square(mono[start_i:end_i])))) if end_i > start_i else 0.0
        n = end_i - start_i
        if n > 0:
            t = np.arange(n, dtype=np.float32) / sr
            env = np.ones(n, dtype=np.float32)
            ramp = min(n // 2, int(0.018 * sr))
            if ramp > 1:
                env[:ramp] = np.linspace(0, 1, ramp)
                env[-ramp:] = np.linspace(1, 0, ramp)
            guide[start_i:end_i] += np.sin(2 * math.pi * hz * t).astype(np.float32) * env * max(0.035, min(0.105, amp * 1.45))
        features.append({
            'index': index,
            'word': word['word'],
            'start_s': start,
            'end_s': end,
            'duration_s': end - start,
            'segment_id': seg_id,
            'note_midi': round(midi, 3),
            'note_hz': round(hz, 3),
            'local_f0_frame_count': len(local_mids),
            'segment_median_midi': segment_medians.get(seg_id) if seg_id is not None else None,
            'rms': round(amp, 6),
            'source_probability': word.get('probability'),
        })

    guide_stereo = np.column_stack([guide, guide])
    hybrid = vocal * 0.35 + guide_stereo * 0.75
    write_wav_stereo(GUIDE_WAV, guide_stereo, sr)
    write_wav_stereo(HYBRID_WAV, hybrid, sr)
    total_word_s = sum(item['duration_s'] for item in features)
    result = {
        'kind': 'word-constrained narrow-smoothed f0/tone layer and audition renders',
        'word_layer': str(WORD_LAYER),
        'f0_source': str(F0_JSON),
        'vocal_source': str(VOCAL_WAV),
        'sine_guide_wav': str(GUIDE_WAV),
        'hybrid_audition_wav': str(HYBRID_WAV),
        'duration_s': duration,
        'word_count': len(features),
        'accepted_word_seconds': round(total_word_s, 3),
        'accepted_word_coverage_percent': round(100.0 * total_word_s / duration, 3),
        'smoothing': 'C3-A4 pYIN, probability>=0.12, segment median blend, quarter-tone quantization, max 3 semitone word-to-word jump.',
        'words': features,
    }
    FEATURES_JSON.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'features': str(FEATURES_JSON),
        'guide': str(GUIDE_WAV),
        'hybrid': str(HYBRID_WAV),
        'word_count': len(features),
        'coverage_percent': result['accepted_word_coverage_percent'],
    }, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
