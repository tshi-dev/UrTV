#!/usr/bin/env python3
"""optimize/photos.sh: what it sends to the TV, what it uploads, and how it refuses bad arguments (no TV needed).

    python3 tests/photos_sh_test.py

PY is pointed at a stand-in for the venv's Python: photo_prep.py runs for real (Pillow), optimize/tv.py is replaced by a
script that writes every command to a file and answers like the TV does. Generated pictures only.
"""
import os, shutil, subprocess, sys, tempfile

try:
    from PIL import Image
except ModuleNotFoundError:
    print("SKIP photos.sh: Pillow is not installed (pip install pillow)")
    sys.exit(0)

here = os.path.dirname(os.path.abspath(__file__))
script = os.path.join(here, "..", "optimize", "photos.sh")
tmp = tempfile.mkdtemp()
log = os.path.join(tmp, "sent.txt")
slept = os.path.join(tmp, "slept.txt")   # every `sleep N` the scripts ran
kept = os.path.join(tmp, "pushed")
os.makedirs(kept)
fake = os.path.join(tmp, "fakepy")
nobin = os.path.join(tmp, "bin")
os.makedirs(nobin)
with open(os.path.join(nobin, "sleep"), "w") as f:   # shot.sh waits 34 s for the screensaver
    f.write("#!/bin/sh\necho \"$1\" >> \"$SLEPT\"\nexit 0\n")
os.chmod(os.path.join(nobin, "sleep"), 0o755)
with open(fake, "w") as f:
    f.write(r'''#!/usr/bin/env bash
# $1 = -I, $2 = the script
case "$2" in *photo_prep.py) exec python3 "$@" ;; esac
[ "$2" = -c ] && exit 1
if [ "$3" = --push-b64 ]; then
  echo "PUSH $4 $5" >> "$SENT"; cp "$4" "$KEPT/$(ls "$KEPT" | wc -l).jpg"; echo "pushed $4 -> $5 (1 bytes, md5 ok)"; exit 0
fi
echo "$3" >> "$SENT"
case "$3" in *PHOTOS*) [ -n "${FAKE_SILENT:-}" ] && { echo "Broadcast completed: result=0"; exit 0; } ;; esac
case "$3" in
  "wm size") echo "Physical size: ${FAKE_SCREEN:-1920x1080}" ;;
  *PHOTOS*"op list"*) printf 'Broadcast completed: result=0, data="photos: %s of 10\nmode: %s (x)"\n' "${FAKE_N:-0}" "${FAKE_MODE:-mix}" ;;
  *PHOTOS*"op add"*)
    if [ -n "${FAKE_ADD_FAIL:-}" ] && { [ "$FAKE_ADD_FAIL" = all ] || [[ "$3" != *"--es path"* ]]; }; then
      echo 'Broadcast completed: result=0, data="error: no such file"'
    else
      printf 'Broadcast completed: result=0, data="saved\nphotos: 1 of 10"\n'
    fi ;;
  *PHOTOS*) printf 'Broadcast completed: result=0, data="saved\nphotos: 1 of 10"\n' ;;
esac
''')
os.chmod(fake, 0o755)

failed = 0
def run(*args, n=0, mode="mix", fail=None, screen=None, silent=False):
    open(log, "w").close()
    open(slept, "w").close()
    for f in os.listdir(kept):
        os.remove(os.path.join(kept, f))
    env = {**os.environ, "PY": fake, "SENT": log, "SLEPT": slept, "KEPT": kept, "FAKE_N": str(n), "FAKE_MODE": mode,
           "PATH": nobin + os.pathsep + os.environ["PATH"]}
    env.pop("FAKE_ADD_FAIL", None)
    if silent:
        env["FAKE_SILENT"] = "1"
    if fail:
        env["FAKE_ADD_FAIL"] = fail
    if screen:
        env["FAKE_SCREEN"] = screen
    r = subprocess.run(["bash", script, *args], capture_output=True, text=True, env=env, timeout=120)
    return r.returncode, r.stdout + r.stderr, open(log, encoding="utf-8").read().strip().split("\n") if os.path.getsize(log) else []

def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

A = "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.PHOTOS "
rc, out, sent = run("list", silent=True)
check("a receiver that never ran (no data=): a failure with a !! line, not a blank success", rc != 0 and "!! photos.sh: no answer from the TV's screensaver app" in out, out)
def photo(name, size=(3000, 2000)):
    p = os.path.join(tmp, name)
    im = Image.new("RGB", size, (30, 120, 60))
    ex = im.getexif()
    ex[0x010F] = "SecretCam"
    im.save(p, exif=ex)
    return p

# list, clear, del, mode
rc, out, sent = run("list")
check("list", rc == 0 and sent == [A + "--es op list"] and out.startswith("photos: 0 of 10"), (sent, out))
rc, out, sent = run("clear")
check("clear", rc == 0 and sent == [A + "--es op clear"], sent)
rc, out, sent = run("del", "3")
check("del N", rc == 0 and sent == [A + "--es op del --ei n 3"], sent)
for bad in ("x", "1; reboot", "-1", ""):
    rc, out, sent = run("del", bad) if bad else run("del")
    check(f"del {bad!r}: usage, nothing sent", rc != 0 and "usage" in out and not sent, out + str(sent))
rc, out, sent = run("mode", "only")
check("mode only", rc == 0 and sent == [A + "--es op mode --es mode only"], sent)
rc, out, sent = run("mode", "mix")
check("mode mix", rc == 0 and sent == [A + "--es op mode --es mode mix"], sent)
for bad in ("both", "ONLY", "mix; reboot", ""):
    rc, out, sent = run("mode", bad) if bad else run("mode")
    check(f"mode {bad!r}: usage, nothing sent", rc != 0 and "usage" in out and not sent, out + str(sent))
rc, out, sent = run("bogus")
check("unknown word: usage, nothing sent", rc != 0 and "usage" in out and not sent, out + str(sent))
rc, out, sent = run()
check("no word: usage", rc != 0 and "optimize/photos.sh add" in out and not sent, out)

# add: prepared first, then uploaded, handed to the app with its size, staged file removed
a, b = photo("a.jpg"), photo("b.jpg", (1000, 600))
rc, out, sent = run("add", a, b, n=2, screen="3840x2160")
pushes = [s for s in sent if s.startswith("PUSH")]
adds = [s for s in sent if "op add" in s]
check("add: ok", rc == 0, out)
check("add: counts what the TV has first, asks its screen size", sent[0] == A + "--es op list" and "wm size" in sent, sent[:3])
check("add: each photo uploaded once to the app's staging file, not the original",
      len(pushes) == 2 and all(p.endswith(" /data/local/tmp/urtv-own/in.jpg") for p in pushes)
      and not any(a in p or b in p for p in pushes), pushes)
sizes = [os.path.getsize(os.path.join(kept, f"{k}.jpg")) for k in range(2)]
check("add: the app is told each file's size", [A + f"--es op add --ei size {s}" for s in sizes] == adds[:2]
      or [A + f"--es op add --ei size {s}" for s in sizes] == [x for x in adds if "--es path" not in x], (adds, sizes))
im0, im1 = Image.open(os.path.join(kept, "0.jpg")), Image.open(os.path.join(kept, "1.jpg"))
check("add: what is uploaded is resized (3000 px -> 1920 at most) and has no metadata",
      im0.size == (1920, 1280) and im1.size == (1000, 600) and not im0.getexif() and b"SecretCam" not in open(os.path.join(kept, "0.jpg"), "rb").read(),
      (im0.size, im1.size))
check("add: the shell streams it into the app, then the staging file goes",
      sum("content write --uri content://sa.urtv.screensaver.config/own-in < /data/local/tmp/urtv-own/in.jpg" in s for s in sent) == 2
      and any(s.startswith("rm -f /data/local/tmp/urtv-own/in.jpg") for s in sent), sent)
check("add: ends with the list", "photos: 2 of 10" in out.split("added")[-1], out)

# where `content write` cannot work, the app reads the staged file itself
rc, out, sent = run("add", a, n=0, fail="first")
check("add: falls back to the app reading the staged file", rc == 0 and any("--es path /data/local/tmp/urtv-own/in.jpg" in s for s in sent), (rc, out, sent))
rc, out, sent = run("add", a, n=0, fail="all")
check("add: when both ways fail, says so and exits 1", rc != 0 and "error: no such file" in out, out)

# refusals before anything is uploaded
rc, out, sent = run("add", a, n=9, screen=None)
check("add: no room (9 + 1 fits)", rc == 0, out)
rc, out, sent = run("add", a, b, n=9)
check("add: no room (9 + 2): refused, nothing uploaded", rc != 0 and "room for 1" in out and not [s for s in sent if s.startswith("PUSH")], out)
rc, out, sent = run("add", *[a] * 31)
check("add: 31 files: refused at once (30 is the most any TV keeps)", rc != 0 and "at most 30" in out and not sent, out + str(sent))
rc, out, sent = run("add", *[a] * 11)
check("add: 11 files on a TV that keeps 10 (a weak TV, or an older app: «of 10»): refused by its own limit, nothing uploaded",
      rc != 0 and "room for 10" in out and not [x for x in sent if x.startswith("PUSH")], out + str(sent))
bad = os.path.join(tmp, "notaphoto.jpg")
open(bad, "wb").write(b"hello")
rc, out, sent = run("add", a, bad)
check("add: one bad file stops everything before any upload", rc != 0 and "notaphoto.jpg" in out and not [s for s in sent if s.startswith("PUSH")], out + str(sent))
rc, out, sent = run("add", os.path.join(tmp, "missing.jpg"))
check("add: a missing file is refused, nothing sent", rc != 0 and "is not a file" in out and not sent, out + str(sent))
rc, out, sent = run("add")
check("add without files: usage", rc != 0 and "usage" in out and not sent, out)

# show: only theirs rotate while the screenshot is taken, the owner's mode comes back
rc, out, sent = run("show", n=0)
check("show with no photos: refused", rc != 0 and "no photos" in out, out)
rc, out, sent = run("show", n=3, mode="mix")
modes = [s.split("--es mode ")[1] for s in sent if "op mode" in s]
check("show: sets only, then puts the mode back", rc == 0 and modes == ["only", "mix"], (modes, out))
check("show: screenshots the screensaver", any("--shot" in s or "screencap" in s for s in sent) or "own-photo" in out, out)
check("show: waits 70 s for the page, not 34", "70" in open(slept).read().split() and "34" not in open(slept).read().split(), open(slept).read())
rc, out, sent = run("show", n=3, mode="only")
check("show: already only: mode left alone", rc == 0 and not [s for s in sent if "op mode" in s], sent)
shutil.rmtree(tmp, ignore_errors=True)
sys.exit(1 if failed else 0)
