#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
skill_root="$(cd -- "$script_dir/.." && pwd)"
bb_sha256="da8731ca93065423dd96e60f7227b264a5093d9afa502816f1cd18efff947145"

if [[ -n "${CALLIOPE_BB:-}" ]]; then
  bb="$CALLIOPE_BB"
else
  if [[ "$(uname -s)" != "Linux" || "$(uname -m)" != "x86_64" ]]; then
    printf '%s\n' "Bundled sonic-seed runtime supports Linux x86_64 only." >&2
    exit 78
  fi

  runtime_xz="$skill_root/assets/runtime/linux-x64/bb.xz"
  if [[ ! -f "$runtime_xz" ]]; then
    printf '%s\n' "Bundled runtime asset is missing: $runtime_xz" >&2
    exit 66
  fi

  for command_name in xz sha256sum; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
      printf '%s\n' "Required bootstrap command is unavailable: $command_name" >&2
      exit 69
    fi
  done

  cache_root="${TMPDIR:-/tmp}/calliope-sonic-seed-runtime/$bb_sha256"
  bb="$cache_root/bb"
  mkdir -p -- "$cache_root"

  installed_sha=""
  if [[ -f "$bb" ]]; then
    installed_sha="$(sha256sum "$bb" | awk '{print $1}')"
  fi

  if [[ "$installed_sha" != "$bb_sha256" ]]; then
    temporary_bb="$(mktemp "$cache_root/.bb.XXXXXX")"
    trap 'rm -f -- "${temporary_bb:-}"' EXIT
    xz -dc -- "$runtime_xz" > "$temporary_bb"
    extracted_sha="$(sha256sum "$temporary_bb" | awk '{print $1}')"
    if [[ "$extracted_sha" != "$bb_sha256" ]]; then
      printf '%s\n' "Bundled runtime failed its SHA-256 check." >&2
      exit 65
    fi
    chmod 0755 -- "$temporary_bb"
    mv -f -- "$temporary_bb" "$bb"
    trap - EXIT
  fi
fi

exec "$bb" --classpath "$skill_root/scripts" \
  "$skill_root/scripts/calliope/sonic_seed/infra.bb" "$@"
