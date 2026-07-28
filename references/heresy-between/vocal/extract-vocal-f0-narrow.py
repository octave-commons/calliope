#!/usr/bin/env python3
import json
from pathlib import Path

import librosa
import numpy as np
import soundfile as sf


BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
VOCAL_WAV = BASE / 'stems' / 'htdemucs' / '存在論的な“反・虚無”の論証として' / 'vocals.wav'
OUT = BASE / 'vocal' / 'vocals.pyin.f0-narrow-c3-a4.json'


def main() -> None:
    if OUT.exists():
        raise SystemExit(f'Refusing to overwrite existing artifact: {OUT}')
    audio, sr = sf.read(VOCAL_WAV, always_2d=True)
    mono = np.mean(audio, axis=1)
    target_sr = 22050
    if sr != target_sr:
        mono = librosa.resample(mono.astype(np.float32), orig_sr=sr, target_sr=target_sr)
        sr = target_sr
    hop_length = 512
    frame_length = 2048
    f0, voiced_flag, voiced_prob = librosa.pyin(
        mono,
        fmin=float(librosa.note_to_hz('C3')),
        fmax=float(librosa.note_to_hz('A4')),
        sr=sr,
        frame_length=frame_length,
        hop_length=hop_length,
        fill_na=np.nan,
    )
    times = librosa.frames_to_time(np.arange(len(f0)), sr=sr, hop_length=hop_length)
    frames = []
    for time_s, hz, voiced, prob in zip(times, f0, voiced_flag, voiced_prob):
        if np.isnan(hz):
            hz_value = None
            midi_value = None
        else:
            hz_value = round(float(hz), 3)
            midi_value = round(float(librosa.hz_to_midi(hz)), 3)
        frames.append({
            'time': round(float(time_s), 6),
            'hz': hz_value,
            'midi': midi_value,
            'voiced': bool(voiced),
            'probability': round(float(prob), 6),
        })
    voiced_count = sum(1 for frame in frames if frame['voiced'] and frame['midi'] is not None)
    OUT.write_text(json.dumps({
        'source': str(VOCAL_WAV),
        'algorithm': 'librosa.pyin narrow vocal range',
        'sr': sr,
        'hop_length': hop_length,
        'frame_length': frame_length,
        'fmin': 'C3',
        'fmax': 'A4',
        'frame_count': len(frames),
        'voiced_frame_count': voiced_count,
        'frames': frames,
    }, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'out': str(OUT), 'frames': len(frames), 'voiced': voiced_count}, ensure_ascii=False))


if __name__ == '__main__':
    main()
