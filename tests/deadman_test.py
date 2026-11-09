"""optimize/deadman.sh, the safety switch that runs on the TV, run here with stub pm/cmd/am/input/sleep.

    python3 tests/deadman_test.py

Its marker carries a nonce (an older switch's marker cannot disarm it), and it writes its pid and a heartbeat while it waits.
"""
import os, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
bad = []


def run(nonce, markers, limit=3, late=""):
    t = Path(tempfile.mkdtemp()); (t / "bin").mkdir(); (t / "d").mkdir()
    for name in ("pm", "cmd", "am", "input"):
        (t / "bin" / name).write_text('#!/bin/sh\necho "$0 $*" | sed "s|.*/||" >> "$T/fired"\n')
    # sleep records the heartbeat file it can see, so a heartbeat is proven to be written while the switch waits
    (t / "bin" / "sleep").write_text('#!/bin/sh\nfor f in "$HOME_DIR"/home_hb.* "$HOME_DIR"/home_pid.*; do [ -f "$f" ] && echo "${f##*/}" >> "$T/seen"; done\n')
    if late:   # the marker lands during the last second of the wait
        (t / "bin" / "sleep").write_text('#!/bin/sh\necho x >> "$T/n"; [ "$(wc -l < "$T/n")" -ge %d ] && : > "$HOME_DIR/%s"; exit 0\n' % (limit, late))
    for f in (t / "bin").iterdir(): f.chmod(0o755)
    for m in markers: (t / "d" / m).write_text("")
    env = dict(os.environ, T=str(t), HOME_DIR=str(t / "d"), PATH=f"{t / 'bin'}:{os.environ['PATH']}")
    args = ["sh", str(ROOT / "optimize" / "deadman.sh"), "stock.pkg/.Home", str(limit)] + ([nonce] if nonce else [])
    r = subprocess.run(args, env=env, capture_output=True, text=True)
    fired = (t / "fired").read_text() if (t / "fired").exists() else ""
    seen = (t / "seen").read_text().split() if (t / "seen").exists() else []
    left = sorted(p.name for p in (t / "d").iterdir())
    return r.returncode, fired, seen, left


def check(name, ok, detail=""):
    print(("ok: " if ok else "WRONG: ") + name + ("" if ok else f" {detail}"))
    if not ok: bad.append(name)


rc, fired, seen, left = run("n2", ["home_ok.n2"])
check("its own marker: it ends without touching the home", rc == 0 and not fired, fired)
rc, fired, seen, left = run("n2", ["home_ok.n1", "home_ok"])
check("the marker of an older switch (and the old plain marker) does not disarm it: the stock home comes back",
      "pm enable stock.pkg" in fired and "pm enable com.google.android.tungsten.setupwraith" in fired
      and "cmd role add-role-holder --user 0 android.app.role.HOME stock.pkg" in fired and "input keyevent KEYCODE_HOME" in fired, fired)
rc, fired, seen, left = run("n2", [], limit=12)
check("it writes its pid and a heartbeat while it waits", "home_pid.n2" in seen and "home_hb.n2" in seen, seen)
check("and removes them when it ends", not [f for f in left if f.startswith(("home_pid", "home_hb"))], left)
rc, fired, seen, left = run("n3", [], limit=4, late="home_ok.n3")
check("its marker lands in the last second of the wait: looked at again, the home is not touched (4.3 S10)", rc == 0 and not fired, fired)
rc, fired, seen, left = run("", ["home_ok"])
check("without a nonce it refuses to start: an old plain home_ok can never disarm a switch", rc == 2 and not fired, f"{rc} {fired}")
sys.exit(1 if bad else 0)
