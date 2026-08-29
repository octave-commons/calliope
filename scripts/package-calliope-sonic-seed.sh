#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 2 ]]; then
  printf 'usage: %s RUNTIME_ARCHIVE OUTPUT_ZIP\n' "$0" >&2
  exit 64
fi

runtime_archive="$(realpath -- "$1")"
output_zip="$(realpath -m -- "$2")"
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "$script_dir/.." && pwd)"
skill_source="$repo_root/skills/calliope-sonic-seed"
expected_archive_sha="2d1d1bc4b3cb8cc65604bac7d90763b01506842a48499bd190bc298bcbae605c"
expected_bb_sha="da8731ca93065423dd96e60f7227b264a5093d9afa502816f1cd18efff947145"
bb_member="foresight-chat-work-linux-x64/bin/bb"
max_zip_bytes=$((50 * 1024 * 1024))
max_file_bytes=$((25 * 1024 * 1024))

for command_name in realpath sha256sum tar xz zip unzip; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    printf 'required command is unavailable: %s\n' "$command_name" >&2
    exit 69
  fi
done

archive_sha="$(sha256sum "$runtime_archive" | awk '{print $1}')"
if [[ "$archive_sha" != "$expected_archive_sha" ]]; then
  printf 'runtime archive SHA-256 mismatch: %s\n' "$archive_sha" >&2
  exit 65
fi

work_dir="$(mktemp -d /tmp/calliope-skill-pack.XXXXXX)"
trap 'rm -rf -- "$work_dir"' EXIT
stage="$work_dir/stage"
mkdir -p -- "$stage"
cp -a -- "$skill_source/." "$stage/"

runtime_dir="$stage/assets/runtime/linux-x64"
mkdir -p -- "$runtime_dir"
bb_file="$work_dir/bb"
tar -xOzf "$runtime_archive" "$bb_member" > "$bb_file"
bb_sha="$(sha256sum "$bb_file" | awk '{print $1}')"
if [[ "$bb_sha" != "$expected_bb_sha" ]]; then
  printf 'extracted Babashka SHA-256 mismatch: %s\n' "$bb_sha" >&2
  exit 65
fi

xz -9 --threads=0 --stdout "$bb_file" > "$runtime_dir/bb.xz"
expanded_sha="$(xz -dc -- "$runtime_dir/bb.xz" | sha256sum | awk '{print $1}')"
if [[ "$expanded_sha" != "$expected_bb_sha" ]]; then
  printf 'compressed runtime round-trip mismatch: %s\n' "$expanded_sha" >&2
  exit 65
fi

chmod 0755 -- "$stage/scripts/run-sonic-seed.sh"
oversized="$(find "$stage" -type f -size "+${max_file_bytes}c" -print -quit)"
if [[ -n "$oversized" ]]; then
  printf 'skill member exceeds 25 MiB: %s\n' "$oversized" >&2
  exit 65
fi

file_count="$(find "$stage" -type f | wc -l | tr -d ' ')"
if (( file_count > 500 )); then
  printf 'skill contains too many files: %s\n' "$file_count" >&2
  exit 65
fi

mkdir -p -- "$(dirname -- "$output_zip")"
(
  cd -- "$stage"
  zip -X -9 -q -r "$output_zip" .
)

zip_bytes="$(stat -c '%s' "$output_zip")"
if (( zip_bytes > max_zip_bytes )); then
  printf 'skill ZIP exceeds 50 MiB: %s bytes\n' "$zip_bytes" >&2
  exit 65
fi

unzip -tq "$output_zip" >/dev/null
printf 'created %s (%s files, %s bytes)\n' "$output_zip" "$file_count" "$zip_bytes"
