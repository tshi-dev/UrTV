#!/usr/bin/env python3
"""R-454 (a report: «the screen froze after a day of Quran»). Two halves.
The soak: .github/workflows/soak.yml (manual only) runs tester/soak/soak.sh on an Android TV emulator for hours of radio and recitation, and
tester/soak/judge.py fails it on growth; here the judge is fed made-up series (flat, a slow leak, a thread leak, a restart, an ANR, too short).
The fix: the media session got new metadata and a new playback state every second of play (about 86,000 calls a day into the system, each waking
every controller), and the black screen set its window flag every second; now only what changed is sent (SessionPush, run on a plain JVM).

    python3 tests/soak_test.py
"""
import importlib.util, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TIL = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

spec = importlib.util.spec_from_file_location("judge", ROOT / "tester/soak/judge.py")
j = importlib.util.module_from_spec(spec); spec.loader.exec_module(j)
def series(hours=4, pss=lambda m: 180_000, thr=lambda m: 60, pid=lambda m: "1234", anr=lambda m: 0):
    return "\n".join(f"{m}\t{pid(m)}\t{pss(m)}\t{thr(m)}\t900000\t{anr(m)}\tradio" for m in range(0, hours * 60, 5))
def verdict(text, **kw): return j.judge(j.parse(text), **kw)
red, out = verdict(series(pss=lambda m: 180_000 + (m % 15) * 900))
check("flat memory with noise and steady threads: GREEN", not red, out)
red, out = verdict(series(pss=lambda m: 180_000 + m * 300))   # 18 MB an hour
check("a slow memory leak (18 MB an hour): RED", red and any(l.startswith("FAIL the home's PSS") for l in out), out)
red, out = verdict(series(thr=lambda m: 60 + m // 10))   # a thread every 10 minutes
check("a thread leak (one more every 10 min): RED", red and any(l.startswith("FAIL the home's threads") for l in out), out)
red, out = verdict(series(pid=lambda m: "1234" if m < 120 else "5678"))
check("the home's process restarted (a crash, a low-memory kill): RED", red and any(l.startswith("FAIL the home's process") for l in out), out)
red, out = verdict(series(pid=lambda m: "-" if m == 100 else "1234"))
check("the home's process missing at one sample: RED", red, out)
red, out = verdict(series(anr=lambda m: 1 if m > 200 else 0))
check("an ANR logged: RED", red and any(l.startswith("FAIL no ANR") for l in out), out)
red, out = verdict(series(hours=1))
check("too short to judge (fewer than 12 samples, an hour, after the warm-up): RED", red and any("enough samples" in l and l.startswith("FAIL") for l in out), out)
red, out = verdict(series(pss=lambda m: 150_000 + min(m, 25) * 2000))   # grows only while warming up
check("growth only in the warm-up (caches filling): GREEN", not red, out)
red, out = verdict("# header\n" + series().replace("\t900000\t", "\t-\t"))
check("a value the emulator did not give («-») is skipped, not a failure", not red, out)

wf = (ROOT / ".github/workflows/soak.yml").read_text(encoding="utf-8")
on = wf.split("\non:\n")[1].split("\npermissions:")[0]
check("the soak runs on manual dispatch only (never on a push, a schedule or a pull request)",
      "workflow_dispatch:" in on and not re.search(r"^\s{2}(push|schedule|pull_request|pull_request_target):", on, re.M), on)
check("the soak runs tester/soak/soak.sh on an Android TV emulator and keeps its samples", "target: android-tv" in wf and "bash tester/soak/soak.sh" in wf
      and "path: soak-out" in wf and "timeout-minutes: 350" in wf)
check("the hours are checked (2 to 5: the judge needs an hour after the warm-up) before they reach a script", 'case "$HOURS" in 2|3|4|5) ;; *)' in wf and wf.index("case \"$HOURS\"") < wf.index("bash tester/soak/soak.sh"))
check("the soak leaves auto-round.yml alone (its own workflow)", "soak" not in (ROOT / ".github/workflows/auto-round.yml").read_text(encoding="utf-8"))
sh = (ROOT / "tester/soak/soak.sh").read_text(encoding="utf-8")
check("the soak samples every 5 minutes: the home's pid, PSS and threads, system_server's PSS, the ANRs, what plays; and the judge has the last word",
      "sleep 300" in sh and "pidof sa.urtv.home" in sh and "/proc/$pid/task" in sh and "pss system_server" in sh and "am_anr" in sh
      and 'python3 -I tester/soak/judge.py "$OUT/samples.tsv"' in sh and 'exit "${PIPESTATUS[0]}"' in sh)
check("the radio and the recitation in turns (PLAY_RADIO and PLAY_SURAH, MusicReceiver)", "sa.urtv.home.PLAY_RADIO" in sh and "sa.urtv.home.PLAY_SURAH" in sh)

svc = (TIL / "MusicService.java").read_text(encoding="utf-8")
up = svc[svc.index("    private void updateSession() {"):]
up = up[:up.index("\n    }\n")]
check("the session's metadata is sent only when it changed (SessionPush.metaChanged)",
      "if (push.metaChanged(title, artist, album, metaDuration)) session.setMetadata(meta.build());" in up and up.count("session.setMetadata(") == 1)
check("the playback state only when the state, the actions or the speed changed, or the position drifted (SessionPush.stateDue)",
      "if (push.stateDue(state, actions, speed, shownPos, SystemClock.elapsedRealtime()))" in up and up.count("session.setPlaybackState(") == 1)
check("setActive(true) once per session, and a new session sends everything again", "if (push.activate()) session.setActive(true);" in up
      and "session = new MediaSession(this, \"urtv-tilawah\");\n        push.reset();" in svc)
blk = (TIL / "BlackScreenActivity.java").read_text(encoding="utf-8")
keep = blk[blk.index("    private void keepOn(boolean on) {"):]
check("the black screen changes its keep-on flag only when it changes (not a relayout every second)",
      "if (has == on) return;" in keep and keep.index("if (has == on) return;") < keep.index("addFlags("))

if not shutil.which("javac"):
    print("SKIP the JVM part: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(TIL / "SessionPush.java"), str(ROOT / "tests/SessionPushRun.java")], capture_output=True, text=True)
    check("SessionPush compiles on a plain JVM", r.returncode == 0, r.stderr)
    if r.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.SessionPushRun"], capture_output=True, text=True)
        print(r.stdout, end="")
        failed += r.returncode != 0 or r.stdout.count("PASS ") != 7
sys.exit(1 if failed else 0)
