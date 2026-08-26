#!/usr/bin/env python3
import argparse
import base64
import json
import subprocess
import textwrap
import urllib.error
import urllib.request
from pathlib import Path


OLLAMA_URL = 'http://192.168.12.68:11434/v1/chat/completions'
MODEL = 'gemma4:e4b'
BASE = Path('/home/err/devel/Music/fork-tales/references/heresy-between')
OUT_DIR = BASE / 'diagnostics' / 'gemma-audio'


def run_ffmpeg(src: Path, out_dir: Path, label: str, chunk_seconds: float) -> list[dict]:
    out_dir.mkdir(parents=True, exist_ok=True)
    duration = probe_duration(src)
    chunks = []
    index = 0
    start = 0.0
    while start < duration - 0.01:
        length = min(chunk_seconds, duration - start)
        index += 1
        dest = out_dir / f'{label}-chunk-{index:02d}-{start:06.2f}-{start + length:06.2f}.wav'
        if not dest.exists():
            subprocess.run([
                'ffmpeg', '-y', '-hide_banner', '-nostats',
                '-ss', f'{start:.3f}', '-t', f'{length:.3f}', '-i', str(src),
                '-ac', '1', '-ar', '16000', '-c:a', 'pcm_s16le', str(dest),
            ], check=True)
        chunks.append({'label': label, 'index': index, 'start_s': round(start, 3), 'end_s': round(start + length, 3), 'path': dest})
        start += chunk_seconds
    return chunks


def probe_duration(path: Path) -> float:
    result = subprocess.run([
        'ffprobe', '-v', 'error', '-show_entries', 'format=duration',
        '-of', 'default=nw=1:nk=1', str(path),
    ], check=True, capture_output=True, text=True)
    return float(result.stdout.strip())


def audio_part(path: Path) -> dict:
    return {
        'type': 'input_audio',
        'input_audio': {
            'data': base64.b64encode(path.read_bytes()).decode(),
            'format': 'wav',
        },
    }


def text_part(text: str) -> dict:
    return {'type': 'text', 'text': text}


def read_optional(path: Path | None, limit: int = 12000) -> str:
    if path is None:
        return ''
    text = path.read_text(encoding='utf-8', errors='replace')
    if len(text) > limit:
        return text[:limit] + '\n...[truncated]'
    return text


def build_serial_parts(original_chunks: list[dict], candidate_chunks: list[dict], args: argparse.Namespace) -> list[dict]:
    parts = []
    parts.append(text_part(textwrap.dedent(f'''
        What follows are audio chunks for a vocal reconstruction audit.
        All audio chunks appear BEFORE the final instruction, because this model requires multimodal inputs before the question.
        Task domain: Japanese sung/spoken vocal reconstruction from a Suno-like original into OpenUTAU/Ritsu.
        Primary audit dimensions: lyric correctness, rhythm/meter, pitch contour, delivery/timbre, and where reconstruction diverges.
        The original lyrics/prompt are reference context only; actual timing and performance are in the audio.
    ''').strip()))
    if args.lyrics:
        parts.append(text_part('Reference lyrics/style prompt follows:\n' + read_optional(args.lyrics, limit=16000)))
    if args.manifest:
        parts.append(text_part('Candidate reconstruction manifest/project context follows:\n' + read_optional(args.manifest, limit=16000)))

    parts.append(text_part(f'BEGIN ORIGINAL AUDIO: {args.original_label}'))
    for chunk in original_chunks:
        parts.append(text_part(f'Original chunk {chunk["index"]}: {chunk["start_s"]:.2f}s to {chunk["end_s"]:.2f}s'))
        parts.append(audio_part(chunk['path']))

    parts.append(text_part(f'BEGIN CANDIDATE AUDIO: {args.candidate_label}'))
    for chunk in candidate_chunks:
        parts.append(text_part(f'Candidate chunk {chunk["index"]}: {chunk["start_s"]:.2f}s to {chunk["end_s"]:.2f}s'))
        parts.append(audio_part(chunk['path']))
    return parts


def build_interleaved_parts(original_chunks: list[dict], candidate_chunks: list[dict], args: argparse.Namespace) -> list[dict]:
    parts = []
    parts.append(text_part(textwrap.dedent(f'''
        What follows are interleaved A/B audio chunks for a vocal reconstruction audit.
        For each time window, A is the original isolated vocal and B is the OpenUTAU candidate.
        All audio chunks appear BEFORE the final instruction.
    ''').strip()))
    if args.lyrics:
        parts.append(text_part('Reference lyrics/style prompt follows:\n' + read_optional(args.lyrics, limit=16000)))
    if args.manifest:
        parts.append(text_part('Candidate reconstruction manifest/project context follows:\n' + read_optional(args.manifest, limit=16000)))
    for original, candidate in zip(original_chunks, candidate_chunks):
        parts.append(text_part(f'Window {original["index"]}: {original["start_s"]:.2f}s to {original["end_s"]:.2f}s, A original'))
        parts.append(audio_part(original['path']))
        parts.append(text_part(f'Window {candidate["index"]}: {candidate["start_s"]:.2f}s to {candidate["end_s"]:.2f}s, B candidate'))
        parts.append(audio_part(candidate['path']))
    return parts


def final_instruction(mode: str) -> str:
    if mode == 'transcribe-original':
        return textwrap.dedent('''
            Final instruction: transcribe the ORIGINAL audio as carefully as possible.
            Return JSON only with fields:
            - summary
            - sections: [{start_s,end_s,heard_japanese,heard_kana_or_romaji,confidence,notes}]
            - opening_15s_best_guess
            - uncertain_regions
            - reconstruction_advice_for_openutau
        ''').strip()
    return textwrap.dedent('''
        Final instruction: compare the candidate OpenUTAU vocal to the original vocal.
        Be blunt and specific. Return JSON only with fields:
        - overall_verdict
        - lyric_accuracy: {score_0_to_10, major_mismatches, opening_15s_comparison}
        - rhythm_meter: {score_0_to_10, where_candidate_is_early_late_or_wrong_density}
        - pitch_contour: {score_0_to_10, where_pitch_is_wrong}
        - delivery_timbre: {score_0_to_10, differences}
        - highest_priority_fix
        - phrase_level_notes: [{start_s,end_s,original_heard,candidate_heard,problem,fix_hint}]
        - should_next_iteration_focus_on: one of [lyrics, rhythm, pitch, delivery, coverage]
    ''').strip()


def call_gemma(parts: list[dict], mode: str, args: argparse.Namespace) -> dict:
    parts.append(text_part(final_instruction(mode)))
    payload = {
        'model': args.model,
        'stream': False,
        'messages': [{'role': 'user', 'content': parts}],
        # Ollama's OpenAI-compatible endpoint may ignore nested options for
        # model context, so also send important runtime knobs top-level.
        'num_ctx': args.num_ctx,
        'temperature': args.temperature,
        'options': {
            'num_ctx': args.num_ctx,
            'temperature': args.temperature,
        },
    }
    if args.thinking_budget is not None:
        payload['thinking_budget'] = args.thinking_budget
        payload['options']['thinking_budget'] = args.thinking_budget
    request = urllib.request.Request(OLLAMA_URL, data=json.dumps(payload).encode(), headers={'Content-Type': 'application/json'})
    try:
        raw = urllib.request.urlopen(request, timeout=args.timeout).read().decode()
    except urllib.error.HTTPError as error:
        body = error.read().decode(errors='replace')
        raise SystemExit(f'Ollama HTTP {error.code}: {body}') from error
    return {'request_summary': {'model': args.model, 'mode': mode, 'layout': args.layout}, 'raw_response': json.loads(raw)}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--original', type=Path, required=True)
    parser.add_argument('--candidate', type=Path)
    parser.add_argument('--lyrics', type=Path)
    parser.add_argument('--manifest', type=Path)
    parser.add_argument('--mode', choices=['transcribe-original', 'compare'], default='compare')
    parser.add_argument('--layout', choices=['serial', 'interleaved'], default='interleaved')
    parser.add_argument('--chunk-seconds', type=float, default=30.0)
    parser.add_argument('--model', default=MODEL)
    parser.add_argument('--num-ctx', type=int, default=131072)
    parser.add_argument('--thinking-budget', type=int)
    parser.add_argument('--temperature', type=float, default=0.0)
    parser.add_argument('--timeout', type=int, default=1800)
    parser.add_argument('--original-label', default='original-isolated-vocal')
    parser.add_argument('--candidate-label', default='openutau-candidate')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()

    work = OUT_DIR / 'audit-chunks' / args.output.stem
    original_chunks = run_ffmpeg(args.original, work / 'original', args.original_label, args.chunk_seconds)
    candidate_chunks = []
    if args.candidate:
        candidate_chunks = run_ffmpeg(args.candidate, work / 'candidate', args.candidate_label, args.chunk_seconds)
    if args.mode == 'compare' and not candidate_chunks:
        raise SystemExit('--candidate is required for compare mode')

    if args.mode == 'transcribe-original':
        parts = build_serial_parts(original_chunks, [], args)
    elif args.layout == 'serial':
        parts = build_serial_parts(original_chunks, candidate_chunks, args)
    else:
        parts = build_interleaved_parts(original_chunks, candidate_chunks, args)

    result = call_gemma(parts, args.mode, args)
    result['chunks'] = {
        'original': [{**chunk, 'path': str(chunk['path'])} for chunk in original_chunks],
        'candidate': [{**chunk, 'path': str(chunk['path'])} for chunk in candidate_chunks],
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    message = result['raw_response']['choices'][0]['message']
    print(message.get('content', ''))
    print(f'\nSaved: {args.output}')


if __name__ == '__main__':
    main()
