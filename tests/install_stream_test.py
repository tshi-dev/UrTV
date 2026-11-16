"""4.3.3: tv.py --install-stream: the APK streamed to `cmd package install -S` (as `adb install` does), with ADB's flow control, through a fake
connection; and install.sh taking it only on a clear answer, else uploading the file as before. No TV needed.

    python3 tests/install_stream_test.py
"""
import contextlib, io, os, random, subprocess, shutil, sys, tempfile, types
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
bad = 0
def check(name, ok, detail=""):
    global bad
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}")); bad += not ok

# ---- a fake adb_shell (only what tv.py touches) and a fake TV that takes an exec: stream
class Msg:
    def __init__(self, cmd, a0, a1, data=b""): self.cmd, self.a0, self.a1, self.data = cmd, a0, a1, data
const = types.SimpleNamespace(OKAY=b"OKAY", WRTE=b"WRTE", CLSE=b"CLSE")
class Info:
    def __init__(self): self.local_id, self.remote_id = 7, 99
class IO:
    def __init__(self, tv): self.tv = tv
    def send(self, m, info): self.tv.got(m)
    def read(self, expected, info): return self.tv.next()
class Dev:
    def __init__(self, *a, **k):
        self._maxdata = 4096; self.opened = None; self.body = bytearray(); self.q = []; self.pieces = 0; self.max_piece = 0
        self._io_manager = IO(self); self.mode = MODE["mode"]; self.sdk = MODE["sdk"]
    def connect(self, **k): pass
    def close(self): pass
    def shell(self, cmd, read_timeout_s=0): return self.sdk + "\n" if cmd == "getprop ro.build.version.sdk" else ""
    def _open(self, dest, *a): self.opened = dest; return Info()
    def got(self, m):
        if m.cmd == b"WRTE":
            assert not self.q or MODE["mode"] == "chatty", "a piece was sent before the last one was acknowledged"
            self.body += m.data; self.pieces += 1; self.max_piece = max(self.max_piece, len(m.data))
            if self.mode == "close" and self.pieces == 3: self.q.append((b"CLSE", 0, 0, b"")); return
            if self.mode == "chatty" and self.pieces == 2: self.q.append((b"WRTE", 0, 0, b"progress "))
            self.q.append((b"OKAY", 0, 0, b""))
        LOG.append(m.cmd)
    def next(self): return self.q.pop(0)
    def _okay(self, info): LOG.append(b"OKAY-sent")
    def _read_until_close(self, info):
        want = int(self.opened.split(b"-S ")[1].split()[0])
        yield b"Success\n" if len(self.body) == want else b"Failure [short]\n"
MODE = {"mode": "", "sdk": "30"}; LOG = []
for name in ["adb_shell", "adb_shell.adb_device", "adb_shell.auth", "adb_shell.auth.keygen", "adb_shell.auth.sign_pythonrsa", "adb_shell.constants", "adb_shell.adb_message"]:
    sys.modules[name] = types.ModuleType(name)
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = Dev
sys.modules["adb_shell.auth.keygen"].keygen = lambda p: None
sys.modules["adb_shell.auth.sign_pythonrsa"].PythonRSASigner = lambda a, b: None
sys.modules["adb_shell"].constants = const
for k in ("OKAY", "WRTE", "CLSE"): setattr(sys.modules["adb_shell.constants"], k, getattr(const, k))
sys.modules["adb_shell.adb_message"].AdbMessage = Msg
os.environ.update(TV_HOST="10.0.0.9", TV_PORT="5555", URTV_HOME=tempfile.mkdtemp())
sys.path.insert(0, str(ROOT / "optimize"))
import tv
tv.time.sleep = lambda s: None
tv.signer = lambda: None

apk = Path(tempfile.mkdtemp()) / "x.apk"
data = bytes(random.Random(5).randrange(256) for _ in range(50_000)); apk.write_bytes(data)
def go(mode="", sdk="30", opts=("-r",)):
    MODE.update(mode=mode, sdk=sdk); LOG.clear()
    t = tv.TV(); err = io.StringIO()
    with contextlib.redirect_stderr(err):
        out = tv.install_stream(t, str(apk), list(opts))
    return out, t, err.getvalue()

out, t, _ = go()
d = t.dev
check("the stream: exec:cmd package install -S <size> -r, every byte in order, pieces no bigger than the TV's message size, each one after its OKAY",
      out.strip() == "Success" and d.opened == b"exec:cmd package install -S 50000 -r" and bytes(d.body) == data and d.max_piece <= 4096 and d.pieces == 13, (out, d.opened, d.pieces))
out, t, _ = go("chatty")
check("the TV says something while the file goes: it is acknowledged and kept, the stream goes on", "progress" in out and "Success" in out and b"OKAY-sent" in LOG, out)
out, t, err = go("close")
check("the TV closes the stream early: no answer (None), said, the connection let go: the caller uploads the file instead", out is None and "uploading the file instead" in err and t.dev is None, (out, err))
out, t, _ = go(sdk="23")
check("Android 6 and older (no `cmd package install -S`): not tried at all", out is None and t.dev.opened is None if t.dev else out is None)

class Cli:   # the adb program (Wireless debugging): exec-in with the file as standard input
    def __init__(self): self.args = None
    def install_stream(self, command, local, timeout): self.args = (command, Path(local).read_bytes() == data); return "Success\n"
    def shell(self, cmd, read_timeout_s=0): return "33\n"
    def connect(self, **k): pass
    def close(self): pass
t = tv.TV(); t.dev = Cli(); cli = t.dev
check("Wireless debugging: the same command through the adb program", tv.install_stream(t, str(apk), ["-r"]) == "Success\n" and cli.args == ("cmd package install -S 50000 -r", True), cli.args)

import wireless
calls = []
def fake_run(argv, **kw): calls.append((argv, kw.get("stdin").read() if kw.get("stdin") else None)); return types.SimpleNamespace(stdout=b"Success\n", stderr=b"", returncode=0)
c = wireless.AdbCli.__new__(wireless.AdbCli); c.host, c.state, c.timeout, c.run, c.adb = "10.0.0.9", {"port": 41000}, 60, fake_run, "adb"
check("wireless.AdbCli.install_stream: adb -s <tv> exec-in <command>, the APK as its standard input", c.install_stream("cmd package install -S 50000 -r", str(apk), 60) == "Success\n"
      and calls[0][0] == ["adb", "-s", "10.0.0.9:41000", "exec-in", "cmd package install -S 50000 -r"] and calls[0][1] == data, calls)

# ---- install.sh: the stream is taken only on a clear answer; else the file goes up and is installed as before
src = (ROOT / "optimize/install.sh").read_text(encoding="utf-8")
body = src[src.index("stream_install() {"):src.index("\n}\n", src.index("stream_install() {")) + 3]
def stream_sh(answer, rc=0, listed=True, env=None):
    t = Path(tempfile.mkdtemp())
    (t / "py").write_text(f'''#!/usr/bin/env bash
shift 2
case "$1" in --install-stream) echo "$1 $2 $3" >> "{t}/log"; printf '%s\\n' "$ANSWER"; exit $RC ;; "pm list packages"*) [ "$LISTED" = yes ] && echo "package:${{1##* }}" ;; esac
''')
    (t / "py").chmod(0o755)
    e = dict(os.environ, PY=str(t / "py"), ANSWER=answer, RC=str(rc), LISTED="yes" if listed else "no", **(env or {}))
    r = subprocess.run(["bash", "-c", "sleep() { :; }\n" + body + '\nstream_install urtv-home.apk sa.urtv.home'], env=e, capture_output=True, text=True)
    log = (t / "log").read_text() if (t / "log").exists() else ""
    shutil.rmtree(t)
    return r.stdout, r.stderr, log
o, e, l = stream_sh("Success")
check("install.sh: a streamed Success with the app still there is the answer (no upload)", o.strip() == "Success" and "--install-stream optimize/urtv-home.apk -r" in l and "by streaming" in e, (o, e, l))
for ans, rc, listed, why in (("", 3, True, "the stream is not possible here (exit 3)"), ("Failure [INSTALL_FAILED_VERIFICATION_FAILURE: Install not allowed]", 1, True, "Play Protect refused it"),
                             ("", 1, True, "no answer"), ("Success", 0, False, "Success but the TV removed the app again")):
    o, e, l = stream_sh(ans, rc, listed)
    check(f"install.sh: {why}: nothing taken, the file is uploaded as before", o.strip() == "", (o, e))
o, e, l = stream_sh("Failure [INSTALL_FAILED_VERSION_DOWNGRADE: Downgrade detected]", 1)
check("install.sh: a newer build already on the TV is a clear answer (the caller keeps it, never a downgrade)", "DOWNGRADE" in o, o)
o, e, l = stream_sh("Success", env={"URTV_STREAM": "0"})
check("install.sh: URTV_STREAM=0: not tried", o.strip() == "" and l == "", (o, l))
check("install.sh: push_install and from_github try the stream first, then the file way unchanged",
      src.count('out=$(stream_install "$1" "$2")') == 1 and src.count('out=$(stream_install "$name" "$2")') == 1 and '[ "${try:-}" = streamed ] || for try in 1 2; do' in src)
sys.exit(1 if bad else 0)
