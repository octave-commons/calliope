"""GPL-3.0-or-later. Isolated Xvfb/i3 effects; never uses the user's display/socket."""
import json
import os
from pathlib import Path
import select
import subprocess
import sys
import time


def main():
    root = Path(sys.argv[1]).resolve()
    root.mkdir(mode=0o700, parents=True, exist_ok=False)
    (root / ".exclusive-studio-desktop-probe").write_text("Private disposable Xvfb/i3 only\n")
    runtime = root / "runtime"
    runtime.mkdir(mode=0o700)
    socket = root / "i3.sock"
    config = root / "i3.conf"
    config.write_text("font pango:monospace 8\nipc-socket " + str(socket) + "\n")
    children = []
    with (root / "process.log").open("w") as log:
        try:
            r, w = os.pipe()
            xvfb = subprocess.Popen(["/usr/bin/Xvfb", "-displayfd", str(w), "-screen", "0", "1920x1080x24", "-nolisten", "tcp"],
                                    pass_fds=(w,), stdout=log, stderr=log)
            children.append(xvfb)
            os.close(w)
            try:
                if not select.select([r], [], [], 10)[0]:
                    raise RuntimeError("Private Xvfb did not announce a display")
                number = os.read(r, 64).decode("ascii").strip()
                if not number.isdigit():
                    raise RuntimeError("Invalid private display number")
            finally:
                os.close(r)
            env = dict(os.environ, DISPLAY=":" + number, XDG_RUNTIME_DIR=str(runtime))
            env.pop("DBUS_SESSION_BUS_ADDRESS", None)
            env.pop("XAUTHORITY", None)
            i3 = subprocess.Popen(["/usr/bin/i3", "-a", "-c", str(config)], env=env, stdout=log, stderr=log)
            children.append(i3)
            deadline = time.monotonic() + 10
            while not socket.exists():
                if i3.poll() is not None or time.monotonic() > deadline:
                    raise RuntimeError("Private i3 failed to create its socket")
                time.sleep(0.05)
            for name in ["CalliopeDesktopProbeStudio", "CalliopeDesktopProbeDAW"]:
                children.append(subprocess.Popen(["/usr/bin/xmessage", "-name", name, "-geometry", "800x600", name],
                                                 env=env, stdout=log, stderr=log))

            def collect(node, found):
                name = node.get("window_properties", {}).get("instance")
                if name in ("CalliopeDesktopProbeStudio", "CalliopeDesktopProbeDAW"):
                    found[name] = node["id"]
                for child in node.get("nodes", []) + node.get("floating_nodes", []):
                    collect(child, found)

            ids = {}
            deadline = time.monotonic() + 10
            while len(ids) != 2:
                tree = subprocess.run(["/usr/bin/i3-msg", "-s", str(socket), "-t", "get_tree", "-r"],
                                      env=env, check=True, capture_output=True, text=True, timeout=3)
                collect(json.loads(tree.stdout), ids)
                if time.monotonic() > deadline:
                    raise RuntimeError("Private test clients did not become visible")
                time.sleep(0.05)
            command = ["clojure", "-M", "spike/studio-desktop/probe.clj", str(socket),
                       str(ids["CalliopeDesktopProbeStudio"]), str(ids["CalliopeDesktopProbeDAW"]), str(root / "result.edn")]
            result = subprocess.run(command, env=env, text=True, capture_output=True, timeout=40)
            (root / "probe.log").write_text(result.stdout + result.stderr)
            print(result.stdout + result.stderr, end="")
            return result.returncode
        finally:
            for process in reversed(children):
                if process.poll() is None:
                    process.terminate()
                    try:
                        process.wait(timeout=3)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait(timeout=3)


if __name__ == "__main__":
    sys.exit(main())
