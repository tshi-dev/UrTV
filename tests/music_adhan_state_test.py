#!/usr/bin/env python3
"""`optimize/music.sh adhan state <prayer> <both|sound|screen|none>` (4.4 beta 4) and `adhan screen <prayer> on|off` really send their broadcasts, and `adhan state` alone is still
the status. The first emulator round found the per-prayer command printed the usage line: the adhan's `state)` word (the status) came first in the case and the per-prayer one
under the same word was never reached. Run with a stand-in for tv.py: python3 tests/music_adhan_state_test.py"""
import os, stat, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

STAND_IN = """#!/usr/bin/env bash
# a stand-in for optimize/venv/bin/python: tv.py's calls are logged and answered, everything else is the real python
if [ "$1" = "-I" ] && [ "$2" = "optimize/tv.py" ]; then
  printf '%s\\n' "$3" >> "$FAKE_LOG"
  case "$3" in
    *GET_PLAYER*) echo 'Broadcast completed: result=-1, data="playing=0|source=none|adhan=off|voice=1|fajr_voice=10|prayer_sound=fajr:,dhuhr:silent|prayer_screen=fajr:on,dhuhr:off"' ;;
    *PRAYER_STATE*|*PRAYER_SCREEN*) [ -n "${FAKE_OLD:-}" ] && echo 'Broadcast completed: result=0' || echo 'Broadcast completed: result=-1, data="saved"' ;;
    *) echo "" ;;
  esac
  exit 0
fi
exec python3 "$@"
"""
with tempfile.TemporaryDirectory() as tmp:
    stub = Path(tmp, "py"); stub.write_text(STAND_IN); stub.chmod(stub.stat().st_mode | stat.S_IEXEC)
    log = Path(tmp, "log.txt")
    def run(*args, **env):
        log.write_text("")
        r = subprocess.run(["bash", "optimize/music.sh", *args], cwd=ROOT, env=dict(os.environ, PY=str(stub), FAKE_LOG=str(log), **env), capture_output=True, text=True, timeout=60)
        return r, log.read_text()
    r, sent = run("adhan", "state", "fajr", "sound")
    check("adhan state fajr sound: says so, sends PRAYER_STATE prayer 0 state 1 to the home", r.returncode == 0 and r.stdout.strip() == "fajr: sound." and "PRAYER_STATE --ei prayer 0 --ei state 1" in sent, (r.returncode, r.stdout, r.stderr[-200:], sent))
    for word, n in (("both", 0), ("sound", 1), ("screen", 2), ("none", 3)):
        r, sent = run("adhan", "state", "isha", word)
        check(f"adhan state isha {word}: state {n}, Isha is prayer 5", r.returncode == 0 and f"PRAYER_STATE --ei prayer 5 --ei state {n}" in sent, (r.stdout, sent))
    r, sent = run("adhan", "state", "maghrib", "loud")
    check("a state that is not one of the four is the usage line, nothing sent", r.returncode != 0 and "PRAYER_STATE" not in sent and "usage" in (r.stdout + r.stderr).lower(), (r.returncode, sent))
    r, sent = run("adhan", "state", "friday", "both")
    check("a prayer that is not one of the five is the usage line, nothing sent", r.returncode != 0 and "PRAYER_STATE" not in sent, (r.returncode, sent))
    r, sent = run("adhan", "state", "asr", "none", FAKE_OLD="1")
    check("a home that does not know the command (no «saved») is refused with a line, exit 1", r.returncode == 1 and "Refused" in r.stderr, (r.returncode, r.stderr))
    r, sent = run("adhan", "screen", "maghrib", "off")
    check("adhan screen maghrib off: PRAYER_SCREEN prayer 4, on false", r.returncode == 0 and "PRAYER_SCREEN --ei prayer 4 --ez on false" in sent, (r.returncode, r.stdout, r.stderr[-200:], sent))
    r, sent = run("adhan", "state")
    check("adhan state alone is still the status (prayer_sound and prayer_screen lines, the own-recording line)", r.returncode == 0 and "prayer_screen=fajr:on,dhuhr:off" in r.stdout and "GET_PLAYER" in sent and "PRAYER_STATE" not in sent, (r.stdout, sent))
    r, sent = run("adhan")
    check("adhan with no word is the status too", r.returncode == 0 and "prayer_sound=" in r.stdout, r.stdout)
sys.exit(1 if failed else 0)
