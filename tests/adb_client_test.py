"""The in-app ADB client (android/launcher/.../setup/AdbClient.java, adapted from Free My TV, MIT): the protocol framing, the real RSA signature and
public-key format, the «Allow USB debugging?» flow (accepted, denied, Android 8 to 10), exit codes and refused commands, against a fake adbd on a
plain JVM. No Android, no TV needed.

    python3 tests/adb_client_test.py
"""
import subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

src = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/AdbClient.java").read_text(encoding="utf-8")
check("the file keeps Free My TV's MIT credit header", "simbasjungles/free-my-tv" in src and "Copyright (c) 2026 Free My TV contributors" in src)
check("no Android classes in the protocol client (it runs on a JVM)", "import android." not in src)
notices = (ROOT / "THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
check("THIRD_PARTY_NOTICES.md credits Free My TV and carries its licence", "Free My TV" in notices and "simbasjungles/free-my-tv" in notices and "Copyright (c) 2026 Free My TV contributors" in notices)

import shutil
if not shutil.which("javac"):
    print("SKIP adb client: no javac on this machine")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/AdbClient.java"),
                        str(ROOT / "tests/AdbClientRun.java")], capture_output=True, text=True)
    check("it compiles", r.returncode == 0, r.stderr)
    if r.returncode:
        sys.exit(1)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.AdbClientRun"], capture_output=True, text=True, timeout=180).stdout.splitlines()
    got = {l.split()[0]: l for l in out}
    check("framing: command, args, length, checksum and the XOR magic of one message", got.get("frame") == "frame true 1 0 9 true true 33", got.get("frame"))
    check("the signed block: 00 01 FF… 00, the SHA-1 prefix, then the 20-byte token", got.get("padded") == "padded true", got.get("padded"))
    check("the public key in adbd's format: 64 words, n0inv, n, R², exponent 65537, the name", got.get("blob") == "blob true true true", got.get("blob"))
    check("an unknown key accepted on the TV: the prompt is announced once, the command runs, its code is read, a failing one says 3",
          got.get("accept") == "accept true 1 0 [out(echo hello)] 3 [out(echo plain)]", got.get("accept"))
    check("no exit marker, or a garbled one, is code -1 (the TV did not say), never 0", got.get("noMarker") == "noMarker -1 -1", got.get("noMarker"))
    check("a command that prints more than 2 MB is an error, not memory filled", got.get("huge") == "huge the command printed too much", got.get("huge"))
    check("the key is known now: a second connection is not asked again", got.get("known") == "known false 1 true", got.get("known"))
    check("the person says no: an error that says the permission was not accepted", (got.get("deny") or "").startswith("deny debugging permission was not accepted"), got.get("deny"))
    check("debugging off (nothing listens): an error that says so", got.get("closed") == "closed true", got.get("closed"))
    check("a command the TV refuses is an error, not an empty answer", (got.get("refused") or "").startswith("refused ADB refused the command"), got.get("refused"))
    check("exec: an APK streamed to `cmd package install -S`: every byte arrives, in pieces of the TV's message size (4096 here), the answer is read, the connection goes on",
          got.get("exec") == "exec Success true 4096 4096 true 0", got.get("exec"))
    check("exec: the TV closes the stream half way: an error that carries what it said", got.get("execEarly") == "execEarly true", got.get("execEarly"))
    check("exec: a file shorter than its size is an error, never a half install", got.get("execShort") == "execShort the file ended before its size", got.get("execShort"))
    check("Android 8 to 10 (the first connection hangs): a new connection that only signs gets in, and the command runs",
          got.get("legacy") == "legacy true true 0 true", got.get("legacy"))
sys.exit(1 if failed else 0)
