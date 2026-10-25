"""GPL-3.0-or-later. Real descriptor races, no audio/device claims."""
import base64
import hashlib
import os
from pathlib import Path
import runpy
import signal
import tempfile
import unittest
from unittest.mock import patch

helper = runpy.run_path(str(Path(__file__).resolve().parents[3] / "resources/studio/secure_ingress.py"))
run = helper["run"]
Refusal = helper["Refusal"]


class DescriptorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / "staging"
        self.root.mkdir()
        self.original = [b"original audio bytes", b"original midi bytes"]
        for name, data in zip(["a.wav", "b.mid"], self.original):
            (self.root / name).write_bytes(data)
        attrs = self.root.stat()
        self.config = {"root": str(self.root), "root-dev": attrs.st_dev, "root-ino": attrs.st_ino,
                       "sources": [{"filename": name, "sha256": hashlib.sha256(data).hexdigest()}
                                   for name, data in zip(["a.wav", "b.mid"], self.original)]}

    def test_replacement_after_metadata_open_reads_only_original_retained_inode(self):
        outside = Path(self.temp.name) / "outside"
        outside.write_bytes(b"outside must never be read")
        original_open = os.open
        replaced = False

        def racing_open(path, flags, *args, **kwargs):
            nonlocal replaced
            if str(path).startswith("/proc/self/fd/") and not replaced:
                (self.root / "a.wav").unlink()
                (self.root / "a.wav").symlink_to(outside)
                replaced = True
            return original_open(path, flags, *args, **kwargs)

        with patch("os.open", side_effect=racing_open):
            result = run(self.config)
        self.assertTrue(replaced)
        self.assertEqual(self.original, [base64.b64decode(s["base64"]) for s in result["sources"]])

    def test_fifo_directory_and_symlink_refuse_without_read_capable_source_open(self):
        for kind in ["fifo", "directory", "link"]:
            with self.subTest(kind=kind):
                target = self.root / "a.wav"
                if target.is_dir():
                    target.rmdir()
                else:
                    target.unlink()
                if kind == "fifo":
                    os.mkfifo(target)
                elif kind == "directory":
                    target.mkdir()
                else:
                    target.symlink_to(self.root / "b.mid")
                original_open = os.open
                reads = []

                def observe(path, flags, *args, **kwargs):
                    if not flags & os.O_PATH:
                        reads.append(path)
                    return original_open(path, flags, *args, **kwargs)

                with patch("os.open", side_effect=observe):
                    with self.assertRaises(Refusal) as raised:
                        run(self.config)
                self.assertEqual("capture/unsafe-source", raised.exception.code)
                self.assertEqual([], reads)

    def test_replaced_root_refuses_before_source_payload_open(self):
        self.root.rename(self.root.with_name("old-staging"))
        self.root.mkdir()
        with self.assertRaises(Refusal) as raised:
            run(self.config)
        self.assertEqual("capture/staging-changed", raised.exception.code)

    def test_hash_mismatch_is_non_success(self):
        (self.root / "a.wav").write_bytes(b"changed source")
        with self.assertRaises(Refusal) as raised:
            run(self.config)
        self.assertEqual("capture/hash-mismatch", raised.exception.code)


if __name__ == "__main__":
    signal.alarm(10)
    unittest.main()
