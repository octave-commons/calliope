# Bundled runtime contract

The distributable skill pack contains a compressed Babashka executable at
`assets/runtime/linux-x64/bb.xz`. The executable is selected from Err's
`foresight-chat-work-linux-x64.tar.gz`; the broader archive remains the
authoritative multi-tool runtime, while the skill carries only the runtime it
actually executes.

## Identity

- Platform: Linux x86_64
- Babashka: `1.13.219`
- Source archive SHA-256:
  `2d1d1bc4b3cb8cc65604bac7d90763b01506842a48499bd190bc298bcbae605c`
- Uncompressed `bb` SHA-256:
  `da8731ca93065423dd96e60f7227b264a5093d9afa502816f1cd18efff947145`
- License: Eclipse Public License 1.0; the pack includes
  `assets/runtime/linux-x64/LICENSE-babashka.txt`.

`scripts/run-sonic-seed.sh` expands the runtime into a content-addressed
temporary directory, verifies the executable before use, and then runs the
Babashka infrastructure adapter. `CALLIOPE_BB` is a development override used
to test the source tree before the binary asset is injected into the release
pack.

The Babashka adapter is byte-compatible with the original NBB adapter for the
same key and options. The historical `calliope.sonic-seed/nbb-1` generator ID is
therefore retained so existing seeds remain reproducible across the runtime
packaging change.

The bundled-runtime guarantee is limited to Linux x86_64. Other platforms must
produce an explicit unsupported-platform error; they must not silently change
the generator or artifact identity.
