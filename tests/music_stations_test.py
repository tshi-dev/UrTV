#!/usr/bin/env python3
"""`optimize/music.sh stations` lists the stations of the INSTALLED app (`radio N` counts in them: 13 on build 629, the tester's older checkout had 11 and
skipped Egypt and Khayyat), and this folder's copy only when the TV does not answer. Run with a stand-in for tv.py: python3 tests/music_stations_test.py"""
import os, stat, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

STAND_IN = """#!/usr/bin/env bash
# a stand-in for optimize/venv/bin/python: tv.py's calls answer from $FAKE_APK, everything else is the real python
if [ "$1" = "-I" ] && [ "$2" = "optimize/tv.py" ]; then
  case "$3" in
    "pm path sa.urtv.home") [ -n "${FAKE_NO_TV:-}" ] && exit 1; echo "package:/data/app/x/base.apk"; exit 0 ;;
    unzip*) [ -n "${FAKE_NO_TV:-}" ] && exit 1; printf '%s' "$FAKE_STATIONS"; exit 0 ;;
  esac
  exit 1
fi
exec python3 "$@"
"""
with tempfile.TemporaryDirectory() as tmp:
    stub = Path(tmp, "py"); stub.write_text(STAND_IN); stub.chmod(stub.stat().st_mode | stat.S_IEXEC)
    env = dict(os.environ, PY=str(stub), FAKE_STATIONS='{"stations": [{"name": "A"}, {"name": "B"}, {"name": "C"}]}')
    run = lambda e: subprocess.run(["bash", "optimize/music.sh", "stations"], cwd=ROOT, env=e, capture_output=True, text=True, timeout=60)
    r = run(env)
    check("the TV's own list is printed (3 stations, numbered from 0)", r.stdout.splitlines() == ["0\tA", "1\tB", "2\tC"], (r.stdout, r.stderr[-200:]))
    r = run(dict(env, FAKE_NO_TV="1"))
    n = len(r.stdout.splitlines())
    check("with no answer from the TV, this folder's copy (every station of stations.json, Egypt and Khayyat among them)", n >= 13 and "مصر" in r.stdout and "خياط" in r.stdout, (n, r.stderr[-200:]))
    r = run(dict(env, FAKE_STATIONS="not json"))
    check("an answer that is not the list falls back to this folder's copy", len(r.stdout.splitlines()) >= 13, r.stdout[:80])
sv = (ROOT / "optimize/start-saver.sh").read_text(encoding="utf-8")
check("start-saver.sh asks every 2 s until the dream runs (up to 60 s, then 20 s after Android 14's own way), never a fixed 6 s (a dream that starts at 8 s was «not started»)",
      "wait_dream()" in sv and 'wait_dream "${SAVER_WAIT_S:-60}"' in sv and 'wait_dream "${SAVER_WAIT2_S:-20}"' in sv and 'sleep "${SAVER_WAIT_S:-6}"' not in sv and "SAVER_POLL_S:-2" in sv)
sys.exit(1 if failed else 0)
