"""GPL-3.0-or-later. Fixed Linux descriptor adapter; stdin is bounded config.

O_PATH first opens only metadata. After fstat proves a regular inode, /proc/self/fd
reopens that exact retained inode for reading. No check-then-open leaf race can
redirect a payload read to a link, FIFO, replaced root, or sibling path.
No network, model, recording-device or canonical-storage operation exists here.
"""
import base64
import hashlib
import json
import os
import stat
import sys

MAX_BYTES = 8 * 1024 * 1024


class Refusal(Exception):
    def __init__(self, code, message):
        self.code, self.message = code, message


def reject(code, message):
    raise Refusal(code, message)


def leaf(name):
    return (isinstance(name, str) and bool(name.strip()) and name not in (".", "..")
            and not any(c in name for c in ("/", "\\", ":", "\0")))


def identity(s):
    # Unlink/relink can change ctime without changing this retained inode's bytes.
    # Canonical identity is the verified payload hash; pathname is only ingress.
    return s.st_dev, s.st_ino, s.st_size, s.st_mtime_ns


def run(config):
    if not sys.platform.startswith("linux") or not hasattr(os, "O_PATH"):
        reject("capture/confinement-unavailable", "Linux O_PATH and procfs are required")
    sources = config["sources"]
    if len(sources) != 2 or not all(leaf(s["filename"]) for s in sources):
        reject("capture/unsafe-source", "Expected two direct regular-file leaves")
    fds = []
    try:
        root = os.open(config["root"], os.O_PATH | os.O_DIRECTORY | os.O_NOFOLLOW | os.O_CLOEXEC)
        fds.append(root)
        s = os.fstat(root)
        if (s.st_dev, s.st_ino) != (config["root-dev"], config["root-ino"]):
            reject("capture/staging-changed", "Granted staging directory identity changed")
        entries = []
        # Inspect both entries before any read-capable source descriptor opens.
        for source in sources:
            fd = os.open(source["filename"], os.O_PATH | os.O_NOFOLLOW | os.O_CLOEXEC, dir_fd=root)
            fds.append(fd)
            attrs = os.fstat(fd)
            if not stat.S_ISREG(attrs.st_mode) or not 0 < attrs.st_size <= MAX_BYTES:
                reject("capture/unsafe-source", "Source must be a bounded regular file")
            entries.append((source, fd, attrs))
        result = []
        for source, metadata_fd, before in entries:
            read_fd = os.open("/proc/self/fd/" + str(metadata_fd), os.O_RDONLY | os.O_NONBLOCK | os.O_CLOEXEC)
            fds.append(read_fd)
            if identity(os.fstat(read_fd)) != identity(before):
                reject("capture/source-changed", "Source changed before snapshot")
            chunks, size = [], 0
            while True:
                chunk = os.read(read_fd, min(65536, MAX_BYTES + 1 - size))
                if not chunk:
                    break
                chunks.append(chunk)
                size += len(chunk)
                if size > MAX_BYTES:
                    reject("capture/unsafe-source", "Source exceeds bounded ingress size")
            payload = b"".join(chunks)
            if identity(os.fstat(read_fd)) != identity(before) or len(payload) != before.st_size:
                reject("capture/source-changed", "Source changed during snapshot")
            digest = hashlib.sha256(payload).hexdigest()
            if digest != source["sha256"]:
                reject("capture/hash-mismatch", "Source bytes do not match the expected SHA-256")
            result.append({"base64": base64.b64encode(payload).decode("ascii"), "sha256": digest})
        return {"ok": True, "sources": result}
    finally:
        for fd in reversed(fds):
            os.close(fd)


def main():
    try:
        line = sys.stdin.buffer.read(65537)
        if len(line) > 65536:
            reject("command/invalid", "Ingress request exceeds bound")
        response = run(json.loads(line))
    except Refusal as e:
        response = {"ok": False, "code": e.code, "message": e.message}
    except (OSError, KeyError, ValueError, TypeError):
        response = {"ok": False, "code": "capture/source-unavailable", "message": "Confined source snapshot unavailable"}
    print(json.dumps(response, separators=(",", ":")))


if __name__ == "__main__":
    main()
