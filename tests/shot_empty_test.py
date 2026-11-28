"""Screenshots: an empty capture is taken again, never blamed on Pillow, and the small copy is made without Pillow too (R-370).

Reports showed 0-byte screenshots with «no small copy: Pillow is missing», while Pillow was there: the capture was empty.
tv.py --shot now takes an empty one again (3 tries) and saves nothing if it stays empty; shot.sh says an empty file is
empty; optimize/png_small.py makes the 960 px copy with the standard library when Pillow is missing.

    python3 tests/shot_empty_test.py          (no TV needed)
"""
import io, os, random, shutil, struct, subprocess, sys, tempfile, types, zlib
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
for name in ["adb_shell", "adb_shell.adb_device", "adb_shell.auth", "adb_shell.auth.keygen", "adb_shell.auth.sign_pythonrsa"]:
    sys.modules[name] = types.ModuleType(name)   # tv.py's ADB library is not needed: the TV is a fake
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = object
sys.modules["adb_shell.auth.keygen"].keygen = lambda p: None
sys.modules["adb_shell.auth.sign_pythonrsa"].PythonRSASigner = lambda a, b: None
os.environ["TV_HOST"] = "192.0.2.5"   # a documentation address: nothing is reached
sys.path.insert(0, str(ROOT / "optimize"))
import tv as tvmod, png_small
tvmod.time.sleep = lambda s: None

bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        bad.append(name)

def chunk(k, b):
    return struct.pack(">I", len(b)) + k + b + struct.pack(">I", zlib.crc32(k + b) & 0xffffffff)
def png(w, h, ctype=6, filt=lambda y: y % 5, seed=1):   # every row filter, made by hand: (bytes, the pixels as rows of RGB(A) bytes)
    r = random.Random(seed); bpp = 4 if ctype == 6 else 3
    pix = [bytes(r.getrandbits(8) for _ in range(w * bpp)) for _ in range(h)]
    raw, prior = b"", bytes(w * bpp)
    for y, line in enumerate(pix):
        f = filt(y); enc = bytearray(line)
        for i in range(len(line)):
            a = line[i - bpp] if i >= bpp else 0; b = prior[i]; c = prior[i - bpp] if i >= bpp else 0
            p = a + b - c; pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
            pred = [0, a, b, (a + b) >> 1, a if pa <= pb and pa <= pc else b if pb <= pc else c][f]
            enc[i] = (line[i] - pred) & 255
        raw += bytes([f]) + bytes(enc); prior = line
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, ctype, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""), pix

# png_small reads every row filter exactly, RGBA and RGB
d = Path(tempfile.mkdtemp())
for ctype in (6, 2):
    data, pix = png(37, 11, ctype)
    (d / "a.png").write_bytes(data)
    w, h, bpp, rows = png_small.read_png(str(d / "a.png"))
    check(f"png_small reads colour type {ctype} with all five row filters", (w, h) == (37, 11) and [bytes(r) for r in rows] == pix)
data, pix = png(1920, 20, 6, filt=lambda y: 4)
(d / "big.png").write_bytes(data)
nw, nh = png_small.small(str(d / "big.png"), str(d / "small.png"))
w, h, bpp, rows = png_small.read_png(str(d / "small.png"))
want = (pix[0][0] + pix[0][4] + pix[1][0] + pix[1][4] + 2) >> 2
check("the small copy is 960 wide, RGB, each pixel the mean of its 2 x 2", (nw, nh, w, h, bpp) == (960, 10, 960, 10, 3) and rows[0][0] == want, (nw, nh, rows[0][:3], want))
(d / "empty.png").write_bytes(b"")
r = subprocess.run([sys.executable, "-I", str(ROOT / "optimize/png_small.py"), str(d / "empty.png"), str(d / "x.png")], capture_output=True, text=True)
check("png_small on an empty file: says the file is empty", r.returncode == 1 and "empty" in r.stderr, r.stderr)

# tv.py --shot: an empty capture is taken again
GOOD = png(4, 4)[0]
class FakeTV:
    def __init__(self, answers): self.answers, self.caps = list(answers), 0
    def shell(self, cmd, timeout=120):
        if cmd.startswith("screencap"): self.caps += 1; self.now = self.answers.pop(0) if self.answers else b""
        if cmd.startswith("wc -c"): return f"{len(self.now)}\n"
        return ""
    def call(self, what, remote, local): Path(local).write_bytes(self.now)
out = d / "shot.png"
t = FakeTV([b"", GOOD])
err = io.StringIO(); real = sys.stderr; sys.stderr = err
try:
    tvmod.shot(t, str(out))
finally:
    sys.stderr = real
check("an empty capture, then a good one: taken twice, the PNG saved", t.caps == 2 and out.read_bytes() == GOOD and "came back empty" in err.getvalue(), (t.caps, err.getvalue()))
out.unlink()
t = FakeTV([b"", b"", b""])
try:
    sys.stderr = io.StringIO(); tvmod.shot(t, str(out)); msg = ""
except SystemExit as e:
    msg = str(e.code)
finally:
    sys.stderr = real
check("empty three times: nothing saved, a !! line that says the capture was empty (not Pillow)",
      t.caps == 3 and not out.exists() and msg.startswith("!! The TV gave an empty screenshot 3 times") and "Pillow" not in msg, (t.caps, msg))

# shot.sh: an empty file is said as empty; without Pillow the small copy is still made
def shot_sh(capture, pillow):
    t = Path(tempfile.mkdtemp()); tree = t / "tree"; (tree / "optimize").mkdir(parents=True)
    for f in ("shot.sh", "png_small.py"):
        shutil.copy(ROOT / "optimize" / f, tree / "optimize")
    (t / "cap.png").write_bytes(capture)
    py = t / "py"
    py.write_text(f"""#!/usr/bin/env bash
if [ "$2" = optimize/tv.py ]; then [ "$3" = --shot ] && cp "{t}/cap.png" "$4"; exit 0; fi
case "$*" in *"import PIL"*) [ "{pillow}" = yes ] || exit 1 ;; esac
exec {sys.executable} "$@"
""")
    py.chmod(0o755)
    r = subprocess.run(["bash", str(tree / "optimize/shot.sh"), "now", "x"], env=dict(os.environ, PY=str(py)), capture_output=True, text=True, cwd=tree)
    return r.returncode, r.stdout + r.stderr, tree / "optimize/run/shots"
rc, out_, shots = shot_sh(b"", "yes")
check("shot.sh: an empty file is said as empty, not «Pillow is missing»", rc == 1 and "is empty" in out_ and "Pillow" not in out_, out_)
rc, out_, shots = shot_sh(png(1280, 8)[0], "no")
check("shot.sh without Pillow: the small copy is made anyway", rc == 0 and (shots / "x-small.png").exists() and "x-small.png" in out_
      and png_small.read_png(str(shots / "x-small.png"))[0] == 960, out_)
sys.exit(1 if bad else 0)
