#!/usr/bin/env python3
"""Assemble render-unit OpenUTAU manifests into a full timeline WAV.

The manifest must contain render_units with:
- planned_raw_wav_path / planned_aligned_wav_path for rendered units;
- kind: silence for silent spans;
- trim_start_seconds and assembly_duration_seconds;
- target_duration_seconds.
"""
import argparse
import json
import subprocess
from pathlib import Path


def run(cmd: list[str]) -> None:
    print('+', ' '.join(cmd))
    subprocess.run(cmd, check=True)


def ffprobe_duration(path: Path) -> str:
    return subprocess.check_output([
        'ffprobe', '-v', 'error', '-show_entries', 'format=duration',
        '-of', 'default=nw=1:nk=1', str(path)
    ], text=True).strip()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--manifest', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--force', action='store_true')
    args = parser.parse_args()

    manifest = json.loads(args.manifest.read_text(encoding='utf-8'))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists() and not args.force:
        raise SystemExit(f'Output exists; pass --force to overwrite: {args.output}')

    concat_path = args.output.with_suffix('.concat.txt')
    concat_lines: list[str] = []
    for unit in manifest['render_units']:
        aligned = Path(unit['planned_aligned_wav_path'])
        aligned.parent.mkdir(parents=True, exist_ok=True)
        duration = float(unit['assembly_duration_seconds'])
        if unit.get('kind') == 'silence':
            run([
                'ffmpeg', '-y' if args.force else '-n', '-v', 'error',
                '-f', 'lavfi', '-i', 'anullsrc=r=44100:cl=stereo',
                '-t', f'{duration:.9f}', str(aligned),
            ])
        else:
            raw = Path(unit['planned_raw_wav_path'])
            if not raw.exists():
                raise SystemExit(f'Missing rendered raw WAV for {unit["id"]}: {raw}')
            trim_start = float(unit['trim_start_seconds'])
            run([
                'ffmpeg', '-y' if args.force else '-n', '-v', 'error',
                '-ss', f'{trim_start:.9f}', '-i', str(raw),
                '-af', f'apad,atrim=0:{duration:.9f}',
                '-ar', '44100', '-ac', '2', str(aligned),
            ])
        print(f'{unit["id"]}: {ffprobe_duration(aligned)}s -> {aligned}')
        concat_lines.append(f"file '{aligned}'\n")

    concat_path.write_text(''.join(concat_lines), encoding='utf-8')
    run([
        'ffmpeg', '-y' if args.force else '-n', '-v', 'error',
        '-f', 'concat', '-safe', '0', '-i', str(concat_path),
        '-c', 'copy', str(args.output),
    ])
    print(f'full: {ffprobe_duration(args.output)}s -> {args.output}')


if __name__ == '__main__':
    main()
