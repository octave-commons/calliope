#!/usr/bin/env python3
import json
import math
import wave
from pathlib import Path

import numpy as np


BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
HYBRID = BASE / 'lyrics' / 'hybrid-transcript-layer-v1-large-with-medium-tail.json'
F0_JSON = BASE / 'vocal' / 'vocals.pyin.f0-narrow-c3-a4.json'
VOCAL_WAV = BASE / 'stems' / 'htdemucs' / '存在論的な“反・虚無”の論証として' / 'vocals.wav'
OUT_DIR = BASE / 'lyrics' / 'auditions'
GESTURE_JSON = OUT_DIR / 'gesture-layer-v3-hybrid-safe.json'
STEP_WAV = OUT_DIR / 'gesture-step-guide-v3-hybrid-safe.wav'
HYBRID_WAV = OUT_DIR / 'gesture-hybrid-v3-hybrid-safe.wav'
REST_WAV = OUT_DIR / 'gesture-rest-mask-v3-hybrid-safe.wav'
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


def midi_to_hz(midi: float) -> float:
    return 440.0 * (2.0 ** ((midi - 69.0) / 12.0))


def quantize(midi: float) -> float:
    return round(midi * 2.0) / 2.0


def char_density(text: str) -> int:
    japanese = sum(1 for char in text if '\u3040' <= char <= '\u30ff' or '\u4e00' <= char <= '\u9fff')
    latin = sum(1 for char in text if char.isascii() and char.isalpha())
    return max(1, japanese + round(latin / 3))


def event_count(text: str, duration: float, f0_count: int) -> int:
    return max(1, min(24, max(char_density(text), round(duration / 0.22), round(f0_count / 5))))


def frames_between(frames: list[dict], start: float, end: float) -> list[dict]:
    return [frame for frame in frames if start <= float(frame['time']) <= end]


def pitch_for(frames: list[dict], fallback: float) -> tuple[float, int]:
    mids = [float(frame['midi']) for frame in frames if frame.get('midi') is not None and float(frame.get('probability', 0)) >= 0.1]
    if not mids:
        return fallback, 0
    arr = np.array(mids, dtype=np.float32)
    lo = np.percentile(arr, 20)
    hi = np.percentile(arr, 80)
    trimmed = arr[(arr >= lo - 0.75) & (arr <= hi + 0.75)]
    if trimmed.size == 0:
        trimmed = arr
    return quantize(float(np.median(trimmed))), int(trimmed.size)


def render_note(buffer: np.ndarray, start_i: int, end_i: int, midi: float, amp: float, reattack: bool) -> None:
    n = end_i - start_i
    if n <= 0:
        return
    hz = midi_to_hz(midi)
    t = np.arange(n, dtype=np.float32) / SR
    env = np.ones(n, dtype=np.float32)
    attack = min(n // 2, int((0.008 if reattack else 0.0015) * SR))
    release = min(n // 2, int(0.006 * SR))
    if attack > 1:
        env[:attack] = np.linspace(0, 1, attack, dtype=np.float32)
    if release > 1:
        env[-release:] = np.linspace(1, 0, release, dtype=np.float32)
    buffer[start_i:end_i] += np.sin(2 * math.pi * hz * t).astype(np.float32) * env * amp


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for path in [GESTURE_JSON, STEP_WAV, HYBRID_WAV, REST_WAV]:
        if path.exists():
            raise SystemExit(f'Refusing to overwrite existing artifact: {path}')
    layer = json.loads(HYBRID.read_text(encoding='utf-8'))
    f0 = json.loads(F0_JSON.read_text(encoding='utf-8'))['frames']
    f0 = [frame for frame in f0 if frame.get('voiced') and frame.get('midi') is not None]
    vocal, sr = read_wav(VOCAL_WAV)
    if sr != SR:
        raise SystemExit(f'Expected {SR}, got {sr}')
    duration = vocal.shape[0] / sr
    mono = np.mean(vocal, axis=1)
    guide = np.zeros(vocal.shape[0], dtype=np.float32)
    mask = np.zeros(vocal.shape[0], dtype=np.float32)
    rest_mask = np.zeros(vocal.shape[0], dtype=np.float32)
    events = []
    rests = []
    prev_end = 0.0
    prev_midi = 63.0
    prev_event_midi = None
    for segment_index, segment in enumerate(layer['segments']):
        start = max(0.0, float(segment['start_s']))
        end = min(duration, float(segment['end_s']))
        if end <= start:
            continue
        if start - prev_end >= 0.16:
            rests.append({'start_s': prev_end, 'end_s': start, 'duration_s': start - prev_end, 'kind': 'unresolved_or_rest'})
            rest_mask[int(prev_end * sr):int(start * sr)] = 1.0
            prev_event_midi = None
        phrase_frames = frames_between(f0, start, end)
        count = event_count(segment['text'], end - start, len(phrase_frames))
        slot = (end - start) / count
        for idx in range(count):
            event_start = start + idx * slot
            event_end = end if idx == count - 1 else start + (idx + 1) * slot
            local = frames_between(f0, event_start, event_end)
            midi, f0_count = pitch_for(local, prev_midi)
            midi = max(prev_midi - 3.0, min(prev_midi + 3.0, midi))
            midi = max(48.0, min(69.0, midi))
            s_i = max(0, int(round(event_start * sr)))
            e_i = min(vocal.shape[0], int(round(event_end * sr)))
            amp = float(np.sqrt(np.mean(np.square(mono[s_i:e_i])))) if e_i > s_i else 0.0
            same = prev_event_midi is not None and abs(midi - prev_event_midi) <= 0.25
            render_note(guide, s_i, e_i, midi, max(0.032, min(0.10, amp * 1.35)), not same)
            mask[s_i:e_i] = 1.0
            events.append({
                'segment_index': segment_index,
                'event_index': idx,
                'start_s': event_start,
                'end_s': event_end,
                'duration_s': event_end - event_start,
                'midi': midi,
                'hz': midi_to_hz(midi),
                'f0_frame_count': f0_count,
                'text': segment['text'],
                'source': segment['source'],
                'support_score': segment['support_score'],
                'status': segment['status'],
                'articulation': 'legato_same_note' if same else 'reattack',
            })
            prev_midi = midi
            prev_event_midi = midi
        prev_end = end
    if prev_end < duration:
        rests.append({'start_s': prev_end, 'end_s': duration, 'duration_s': duration - prev_end, 'kind': 'unresolved_or_rest'})
        rest_mask[int(prev_end * sr):] = 1.0
    kernel = np.hanning(int(0.028 * sr) * 2 + 1)
    kernel /= np.sum(kernel)
    smooth_mask = np.clip(np.convolve(mask, kernel, mode='same'), 0, 1)
    smooth_rest = np.clip(np.convolve(rest_mask, kernel, mode='same'), 0, 1)
    guide_stereo = np.column_stack([guide, guide])
    rest_audio = vocal * smooth_rest[:, None] * 0.25
    hybrid_audio = vocal * smooth_mask[:, None] * 0.35 + rest_audio + guide_stereo * 0.82
    write_wav(STEP_WAV, guide_stereo, sr)
    write_wav(HYBRID_WAV, hybrid_audio, sr)
    write_wav(REST_WAV, rest_audio, sr)
    event_seconds = sum(event['duration_s'] for event in events)
    GESTURE_JSON.write_text(json.dumps({
        'kind': 'hybrid-safe gesture layer v3',
        'hybrid_transcript_layer': str(HYBRID),
        'f0_source': str(F0_JSON),
        'vocal_source': str(VOCAL_WAV),
        'step_guide_wav': str(STEP_WAV),
        'hybrid_wav': str(HYBRID_WAV),
        'rest_mask_wav': str(REST_WAV),
        'duration_s': duration,
        'event_count': len(events),
        'event_seconds': round(event_seconds, 3),
        'event_coverage_percent': round(100 * event_seconds / duration, 3),
        'rest_count': len(rests),
        'rest_seconds': round(sum(rest['duration_s'] for rest in rests), 3),
        'method': 'Dense/legato gesture rendering from hallucination-safe hybrid transcript segments. Excluded spans become unresolved/rest mask, not guessed lyrics.',
        'events': events,
        'rests': rests,
    }, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'gesture_json': str(GESTURE_JSON), 'step_wav': str(STEP_WAV), 'hybrid_wav': str(HYBRID_WAV), 'events': len(events), 'rests': len(rests), 'event_coverage_percent': round(100 * event_seconds / duration, 3)}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
