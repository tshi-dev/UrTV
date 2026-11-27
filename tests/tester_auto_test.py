#!/usr/bin/env python3
"""4.4 beta 4: the automatic tester round (tester/auto/round.sh, lib.py). The parsing is tested on saved answers of a TV, and the whole round on a stand-in TV
(tests/tester_auto/fake.py): the report, the thresholds that turn it red, the previous build's column, the putting back at the end, and that a key is never
sent to a screen that was not read first. Run: python3 tests/tester_auto_test.py"""
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tester/auto"))
import lib  # noqa: E402

failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)[:600]))
    failed += not ok

# ---- the parsers
check("am start -W: TotalTime, and nothing when it said none", lib.start_time("Status: ok\nTotalTime: 523\nWaitTime: 540") == 523 and lib.start_time("Error: x") is None)
check("meminfo: TOTAL PSS (Android 10+), the older TOTAL line, nothing", lib.pss("           TOTAL PSS:   150000    TOTAL RSS: 1") == 150000
      and lib.pss("       TOTAL   98765    1234   5") == 98765 and lib.pss("no process") is None)
sample = "Total frames rendered: 400\nJanky frames: 8 (2.00%)\n90th percentile: 10ms\n99th percentile: 30ms\n"
check("gfxinfo summary lines", lib.jank(sample) == {"frames": 400, "janky_pct": 2.0, "p90_ms": 10, "p99_ms": 30}, lib.jank(sample))
frames = "---PROFILEDATA---\nFlags,IntendedVsync,Vsync,FrameCompleted\n0,1000000000,1000000000,1010000000\n0,2000000000,2000000000,2040000000\n1,3000000000,3000000000,3500000000\n---PROFILEDATA---\n"
j = lib.jank(frames)
check("gfxinfo framestats rows: a flagged row is not a frame, 10 ms fine, 40 ms janky", j["frames"] == 2 and j["janky_pct"] == 50.0 and j["p99_ms"] == 40, j)
check("a crash of ours counts, of another app does not", lib.crashes("x FATAL EXCEPTION: main\nProcess: sa.urtv.home, PID: 1\nFATAL EXCEPTION: m\nProcess: com.other, PID: 2", ["sa.urtv.home"]) == ["sa.urtv.home"])
drop = "2026-11-27 15:00:00 data_app_anr (text)\nProcess: sa.urtv.home\n\n2026-11-27 16:10:00 data_app_anr (text)\nProcess: sa.urtv.home\n\n2026-11-27 16:20:00 data_app_crash (text)\nProcess: com.other\n"
check("dropbox: only our packages, only since the round began", lib.anrs(drop, ["sa.urtv.home"], "2026-11-27 16:00:00") == ["2026-11-27 16:10:00 data_app_anr sa.urtv.home"], lib.anrs(drop, ["sa.urtv.home"], "2026-11-27 16:00:00"))
log = "11-27 16:00:00.000  100  100 I Choreographer: Skipped 150 frames! x\n11-27 16:00:01.000  100  100 I Choreographer: Skipped 5 frames!\n11-27 16:00:02.000  200  200 I Choreographer: Skipped 300 frames!\n11-27 16:00:03.000  100  100 W X: Input dispatching timed out"
fr = lib.frozen(log, ["100"], r"Input dispatching timed out", 120)
check("frozen lines: a big skip of our process, the regex, not a small skip, not another process", len(fr) == 2 and "150 frames" in fr[0] and "timed out" in fr[1], fr)
check("a dream: mCurrentDream or DreamActivity; null is none", lib.dreaming("mCurrentDream=ComponentInfo{a/b}") and lib.dreaming("mResumedActivity: DreamActivity") and not lib.dreaming("mCurrentDream=null"))
check("mute: only the music stream's block counts", lib.muted("- STREAM_MUSIC:\n Muted: true\n- STREAM_ALARM:\n Muted: false") and not lib.muted("- STREAM_MUSIC:\n Muted: false\n- STREAM_ALARM:\n Muted: true"))
xml = '<node text="" focused="true" bounds="[0,0][100,100]"/><node text="الأذان" focused="false" bounds="[10,10][90,50]"/><node text="x" focused="false" bounds="[200,0][300,10]"/>'
check("the focused row's name (a child's text when the row itself has none), and a text on the screen", lib.focused(xml) == "الأذان" and lib.has_text(xml, "الأذان") and not lib.has_text(xml, "غير موجود"))
check("the settings style, the volume, the city, the adhan state of a prayer", lib.settings_style("a=1\nsettingsstyle=couch\n") == "couch" and lib.settings_style("nothing") == "" and lib.music_volume("volume is 7 in range [0..15]") == 7 and lib.city("version=4\ncity=الرياض\nx") == "الرياض"
      and lib.adhan_state("prayer_sound=fajr:silent,dhuhr:|prayer_screen=fajr:on,dhuhr:off", "fajr") == "screen" and lib.adhan_state("prayer_sound=fajr:silent,dhuhr:|prayer_screen=fajr:on,dhuhr:off", "dhuhr") == "sound"
      and lib.adhan_state("nothing", "fajr") == "")

# ---- the report
with tempfile.TemporaryDirectory() as tmp:
    a, b = Path(tmp, "old"), Path(tmp, "new")
    for d, home, jank in ((a, 3000, 5), (b, 3500, 30)):
        d.mkdir()
        (d / "metrics.tsv").write_text(f"cold_start_home_ms\t{home}\tms\njank_home_pct\t{jank}\t%\ncrashes\t0\t\n", encoding="utf-8")
        (d / "steps.tsv").write_text("layout-classic-none\tPASS\tfine\n", encoding="utf-8")
    check("a report over a limit is red, names the metric, and shows the previous build's number and the change", lib.report(str(b), str(a)) == "red"
          and "jank_home_pct = 30" in (b / "report.md").read_text(encoding="utf-8") and "| 3500ms | max 4000 | 3000ms | +500 | ok |" in (b / "report.md").read_text(encoding="utf-8"), (b / "report.md").read_text(encoding="utf-8"))
    check("within every limit is green", lib.report(str(a), None) == "green" and "GREEN" in (a / "report.md").read_text(encoding="utf-8"))
    (a / "steps.tsv").write_text("settings-tv-pray\tFAIL\tmissing\n", encoding="utf-8")
    check("a failed step is red whatever the numbers", lib.report(str(a), None) == "red")

# ---- the round on a stand-in TV
def run(extra_env=None, args=()):
    tmp = tempfile.mkdtemp()
    fake = f"{sys.executable} {ROOT}/tests/tester_auto/fake.py"
    env = dict(os.environ, FAKE_DIR=f"{tmp}/state", ROUND_OUT=f"{tmp}/out", ROUND_FAST="1", ROUND_TV=f"{fake} tv", ROUND_SAVER=f"{fake} saver", ROUND_MUSIC=f"{fake} music",
               ROUND_INSTALL=f"{fake} install", ROUND_STARTSAVER=f"{fake} startsaver", PY=sys.executable)
    env.update(extra_env or {})
    r = subprocess.run(["bash", str(ROOT / "tester/auto/round.sh"), "--build", "t1", *args], cwd=ROOT, env=env, capture_output=True, text=True, timeout=300)
    out = Path(tmp, "out", "t1")
    return r, out, Path(tmp, "state", "log.txt").read_text(encoding="utf-8") if Path(tmp, "state", "log.txt").exists() else ""

r, out, cmds = run()
rep = (out / "report.md").read_text(encoding="utf-8") if (out / "report.md").exists() else ""
steps = [l.split("\t") for l in (out / "steps.tsv").read_text(encoding="utf-8").splitlines()] if (out / "steps.tsv").exists() else []
check("a clean round on the stand-in: exit 0, GREEN, report.md written", r.returncode == 0 and "GREEN" in rep, (r.returncode, r.stdout[-500:], r.stderr[-500:]))
by = {}
for s in steps:
    by[s[1]] = by.get(s[1], 0) + 1
check("every step passed: install, 9 layouts, 30 settings screens (3 styles × 10 sections), saver, 2 stations, 2 displays, 2 font, 20 adhan states, games", by.get("FAIL", 0) == 0 and by.get("PASS", 0) == 1 + 9 + 30 + 1 + 2 + 2 + 2 + 20 + 1 and "SKIP" not in by,
      (by, [s for s in steps if s[1] != "PASS"][:5]))
metrics = {l.split("\t")[0] for l in (out / "metrics.tsv").read_text(encoding="utf-8").splitlines()}
check("the health numbers are in: cold starts, memory, jank, crashes, ANRs, frozen lines", {"cold_start_home_ms", "cold_start_saver_ms", "pss_home_kb", "pss_saver_kb", "jank_home_pct", "jank_home_p99_ms", "crashes", "anrs", "frozen_lines"} <= metrics, metrics)
shots = [p for p in (out / "shots").glob("*.png") if not p.name.endswith("-city.png")]
try:
    import PIL  # noqa: F401
    pil = True
except ImportError:
    pil = False
check("47 screenshots (9 layouts, 30 settings screens, saver, 2 stations, 2 display, 2 font, games), and with Pillow the city boxed copy of each where it is on the screen",
      len(shots) == 47 and (not pil or len(list((out / "shots").glob("*-city.png"))) == 47), (len(shots), len(list((out / "shots").glob("*-city.png")))))
lines = cmds.splitlines()
check("put back at the end: the settings imported, the display size, the volume (7), then Home and a look at the mute", "saver import" in cmds and "settings-before.json --yes" in cmds
      and "wm size reset; wm density reset" in cmds and "cmd media_session volume --stream 3 --set 7" in cmds
      and max(i for i, l in enumerate(lines) if "KEYCODE_HOME" in l) < max(i for i, l in enumerate(lines) if l.startswith("tv dumpsys audio")), lines[-6:])
check("the font was set to 1.3 and put back to what it was (1.0)", "settings put system font_scale 1.3" in cmds and "settings put system font_scale 1.0" in cmds)
check("the build is installed over the installed one with the tag (apk-next)", "install saver tag=apk-next" in cmds and "install launcher tag=apk-next" in cmds)
check("the stations were found by their row's name before OK (the focus walk), and the saver start was asked", cmds.count("KEYCODE_DPAD_CENTER") == 2 and "startsaver" in cmds)
# the second round shows the first one's column
r1b, out1b, _ = run(args=("--prev", str(out)))
rep1b = (out1b / "report.md").read_text(encoding="utf-8") if (out1b / "report.md").exists() else ""
check("--prev: the report has the previous build's numbers beside the new ones", "| cold_start_home_ms | 700ms | max 4000 | 700ms | +0 | ok |" in rep1b, rep1b[:900])
r2, out2, _ = run({"FAKE_JANK": "bad"})
rep2 = (out2 / "report.md").read_text(encoding="utf-8") if (out2 / "report.md").exists() else ""
check("a janky walk turns the round red: exit 1, the metric named in the verdict", r2.returncode == 1 and "RED" in rep2 and "jank_home_pct" in rep2, (r2.returncode, rep2[-300:]))
r3, out3, cmds3 = run({"FAKE_MUTE_ON_RADIO": "1"})
rep3 = (out3 / "report.md").read_text(encoding="utf-8") if (out3 / "report.md").exists() else ""
check("a station that mutes the music is a FAIL that says so, and the mute that remains is reported at the end (never pressed back)", r3.returncode == 1 and "the mute changed" in rep3 and "restore-mute" in rep3 and "KEYCODE_VOLUME_MUTE" not in cmds3, rep3[-500:])
r4, out4, cmds4 = run({"FAKE_FRONT": "none"})
rep4 = (out4 / "report.md").read_text(encoding="utf-8") if (out4 / "report.md").exists() else ""
check("NEVER PRESSES BLIND: when no app of ours comes to the front, no D-pad key is sent (not to the walk, a station list or a settings screen), and the round is red",
      r4.returncode == 1 and "DPAD" not in cmds4 and "no key was sent" in rep4 and "RED" in rep4, (r4.returncode, cmds4.count("DPAD"), rep4[-300:]))


# ---- the CI emulator: the shim for optimize/tv.py, the emulator's limits, the steps left out, the workflow
with tempfile.TemporaryDirectory() as tmp:
    fake_adb = Path(tmp, "adb")
    fake_adb.write_text("#!/usr/bin/env bash\necho \"$@\" >> \"$ADB_LOG\"\ncase \"$1 $2\" in\n  'exec-out screencap') printf 'PNGDATA' ;;\n  'shell echo hi') echo hi ;;\n  *) echo \"out of: $*\" ;;\nesac\n", encoding="utf-8")
    fake_adb.chmod(0o755)
    env = dict(os.environ, PATH=f"{tmp}:{os.environ['PATH']}", ADB_LOG=f"{tmp}/adb.log")
    shim = [sys.executable, str(ROOT / "tester/auto/ci/tv_adb.py")]
    one = subprocess.run(shim + ["echo hi"], env=env, capture_output=True, text=True)
    shot = subprocess.run(shim + ["--shot", f"{tmp}/s.png"], env=env, capture_output=True, text=True)
    batch = subprocess.run(shim + ["--batch"], env=env, input="date\n# a note\nid\n", capture_output=True, text=True)
    tr = subprocess.run(shim + ["--transport"], env=env, capture_output=True, text=True)
    log = Path(tmp, "adb.log").read_text(encoding="utf-8")
    check("the emulator shim: a command goes to `adb shell`, a shot is `adb exec-out screencap -p` written to the file, a batch prints «$ cmd» and its answer, the transport is adb",
          one.stdout.strip() == "hi" and Path(tmp, "s.png").read_bytes() == b"PNGDATA" and "$ date\nout of: shell date" in batch.stdout and "$ id" in batch.stdout and "a note" not in batch.stdout
          and tr.stdout.strip() == "adb" and "shell echo hi" in log and "exec-out screencap -p" in log, (one.stdout, batch.stdout, log))
os.environ["ROUND_THRESHOLDS"] = str(ROOT / "tester/auto/ci/thresholds-emulator.tsv")
check("the emulator's limits are looser for what the machine decides and the same for what the app decides (ROUND_THRESHOLDS)",
      lib.thresholds()["cold_start_home_ms"][0] > 4000 and lib.thresholds()["crashes"][:2] == (0.0, "zero") and lib.thresholds()["frozen_lines"][:2] == (0.0, "zero"))
del os.environ["ROUND_THRESHOLDS"]
check("without it the real TV's limits", lib.thresholds()["cold_start_home_ms"][0] == 4000)
rs, outs, cmds_s = run(args=("--skip", "radio,display"))
steps_s = [l.split("\t") for l in (outs / "steps.tsv").read_text(encoding="utf-8").splitlines()]
check("--skip: the steps left out say so in the report (SKIP), run nothing, and the round is still green",
      rs.returncode == 0 and ["radio", "SKIP"] == steps_s[0][:2] and ["display", "SKIP"] == steps_s[1][:2] and not any(s[0].startswith(("radio-", "display-")) for s in steps_s)
      and "wm size 1280x720" not in cmds_s and "KEYCODE_DPAD_CENTER" not in cmds_s, (rs.returncode, steps_s[:3]))
wf = (ROOT / ".github/workflows/auto-round.yml").read_text(encoding="utf-8")
check("the workflow: a push to work/auto-round or a dispatch, the apps built from the ref, an Android TV emulator, tester/auto/ci/run.sh, published to data-screens in <run>/auto, red when the round is RED",
      "branches: ['work/auto-round-*']" in wf and "--force" not in wf.replace("--force-with-lease", "") and "git push --force" not in wf and "workflow_dispatch:" in wf and "inputs.ref || github.sha" in wf and "reactivecircus/android-emulator-runner@v2" in wf and "target: android-tv" in wf
      and "tester/auto/ci/run.sh" in wf and "--branch data-screens" in wf and '"$run/auto"' in wf and '[ "$verdict" = GREEN ]' in wf and "FONTS_TOKEN" in wf)
run_ci = (ROOT / "tester/auto/ci/run.sh").read_text(encoding="utf-8")
check("run.sh on the emulator: the TV client is the adb shim, the emulator's limits, no network means no radio, and the CI checkout only",
      "cp tester/auto/ci/tv_adb.py optimize/tv.py" in run_ci and "ROUND_THRESHOLDS=tester/auto/ci/thresholds-emulator.tsv" in run_ci and 'SKIP="${SKIP:+$SKIP,}radio"' in run_ci and not (ROOT / "optimize/tv.py").read_text(encoding="utf-8").startswith('#!/usr/bin/env python3\n"""A stand-in'))

# ---- what the script must never do
src = (ROOT / "tester/auto/round.sh").read_text(encoding="utf-8") + (ROOT / "tester/auto/lib.py").read_text(encoding="utf-8")
code = "\n".join(l for l in src.splitlines() if not l.lstrip().startswith("#"))
check("it never touches the family PIN, the setup wizard, the old launcher or the Wi-Fi, never restarts the TV, never uninstalls, never presses the mute key",
      not re.search(r"setupwraith|launcherx|\bpm (uninstall|disable|clear)|svc wifi|wpa|dock\.sh pin|reboot|KEYCODE_(VOLUME_)?MUTE|KEYCODE_POWER|KEYCODE_SLEEP", code), re.findall(r"setupwraith|launcherx|pm uninstall|svc wifi|reboot|KEYCODE_MUTE", code))
check("no address, serial, Wi-Fi name, user name or personal path in it", not re.search(r"\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b|/home/|/Users/|[A-Z0-9]{12,}", code), "")
check("every step has its words in the header and its data file rows (screens.tsv: layouts, cards, sections, styles, prayers)", all(w in (ROOT / "tester/auto/round.sh").read_text(encoding="utf-8") for w in ("install", "layouts", "settings", "saver", "radio", "display", "font", "adhan", "games", "health"))
      and len([l for l in (ROOT / "tester/auto/screens.tsv").read_text(encoding="utf-8").splitlines() if l.startswith(("layout\t", "card\t", "section\t", "style\t", "adhan\t"))]) == 3 + 3 + 10 + 3 + 5)
sys.exit(1 if failed else 0)
