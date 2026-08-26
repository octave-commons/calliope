#!/usr/bin/env python3
import argparse
import json
import time
from pathlib import Path

from faster_whisper import WhisperModel


def segment_to_dict(segment):
    words = []
    for word in segment.words or []:
        words.append(
            {
                "start_s": float(word.start),
                "end_s": float(word.end),
                "word": word.word.strip(),
                "probability": float(word.probability),
            }
        )
    return {
        "id": int(segment.id),
        "seek": int(segment.seek),
        "start_s": float(segment.start),
        "end_s": float(segment.end),
        "text": segment.text.strip(),
        "tokens": list(segment.tokens),
        "avg_logprob": float(segment.avg_logprob),
        "compression_ratio": float(segment.compression_ratio),
        "no_speech_prob": float(segment.no_speech_prob),
        "words": words,
    }


def main():
    parser = argparse.ArgumentParser(description="Batch faster-whisper transcription for Fork Tales vocal stems")
    parser.add_argument("--audio", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--model", default="large-v3-turbo")
    parser.add_argument("--device", default="cuda")
    parser.add_argument("--compute-type", default="float16")
    parser.add_argument("--language", default="ja")
    parser.add_argument("--beam-size", type=int, default=5)
    parser.add_argument("--vad-filter", action="store_true")
    args = parser.parse_args()

    audio_path = Path(args.audio)
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    if out_path.exists():
        raise SystemExit(f"Refusing to overwrite existing artifact: {out_path}")

    start = time.time()
    model = WhisperModel(args.model, device=args.device, compute_type=args.compute_type)
    segments_iter, info = model.transcribe(
        str(audio_path),
        language=args.language,
        beam_size=args.beam_size,
        word_timestamps=True,
        vad_filter=args.vad_filter,
        condition_on_previous_text=False,
        temperature=0.0,
        compression_ratio_threshold=2.4,
        log_prob_threshold=-1.0,
        no_speech_threshold=0.6,
    )
    segments = [segment_to_dict(segment) for segment in segments_iter]
    elapsed = time.time() - start
    words = [word for segment in segments for word in segment["words"]]
    duration = float(info.duration)
    result = {
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "kind": "faster-whisper batch transcription",
        "audio_path": str(audio_path),
        "model": args.model,
        "device": args.device,
        "compute_type": args.compute_type,
        "language": args.language,
        "beam_size": args.beam_size,
        "vad_filter": args.vad_filter,
        "duration_s": duration,
        "elapsed_s": elapsed,
        "rtf": elapsed / duration if duration else None,
        "detected_language": info.language,
        "language_probability": float(info.language_probability),
        "segments_count": len(segments),
        "words_count": len(words),
        "text": " ".join(segment["text"] for segment in segments if segment["text"]).strip(),
        "segments": segments,
        "words": words,
    }
    out_path.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"out": str(out_path), "segments": len(segments), "words": len(words), "rtf": result["rtf"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
