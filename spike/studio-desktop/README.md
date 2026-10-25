# Isolated desktop profile verification

License: GPL-3.0-or-later. Run from the repository root with a **new, absent**
output directory:

```bash
python3 -I -S spike/studio-desktop/run_probe.py ../desktop-probe-new-run
```

The launcher starts its own Xvfb display (TCP disabled), i3 configuration/socket
and two disposable xmessage clients. It never uses the user's display or socket.
Its `finally` cleanup terminates only the child processes it launched. JDK21,
Clojure CLI, Python3 stdlib, Xvfb, i3, i3-msg, xmessage and coreutils timeout must
be installed. No Docker, display reconfiguration, service restart or user i3
configuration change is involved.

The production desktop controller first previews the two tiled clients. Floating
one reflows the other, so the controller stops after one acknowledged command with
an explicit partial result. A separate explicit simulated-operator fresh preview
and apply completes the profile (six commands total). This is test-driver action,
not an automatic retry by the controller. It verifies exact final rectangles and unchanged focus,
refuses duplicate application without issuing another command, then refuses a
preview after a separate simulated manual geometry change. Every required check
is an assertion; a failed layout or refusal exits nonzero. `result.edn` retains
the actual snapshots, preview, acknowledgements and command operations. Failures
remain in that result/log rather than being presented as successful effects.

The current 2026-10-25 probe04 passed, including the first partial result and
separate fresh-preview action: 1920x1080 private screen, two 960x1080 target rectangles,
six acknowledged layout commands, unchanged focused container, duplicate apply
with zero further commands and manual-change cancellation with zero commands.
This proves production adapter effects against an **isolated real i3 server and
surrogate clients**. It is not a real workstation layout change, actual DAW test,
Calliope studio UI test or human usability acceptance. The ordinary JVM suite
separately tests mocked effects/failure paths. Workstation discovery is read-only.

Historical probe02 predated the after-command user-change fix and completed in one
application. It is retained as historical evidence, not current behavior. Probe03
correctly stopped on reflow but its older success assertion failed; probe04 makes
the conservative behavior and explicit subsequent operator action inspectable.
