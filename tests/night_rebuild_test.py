#!/usr/bin/env python3
"""4.4 beta 3 (a user: «after about a week it gets very heavy»): the home card's page is let go once a day at night while the home is not in front, and
built again at the next return (NightRebuild, HomeCard); never while in use. The page lets go of what only grew: LIVE_PATCH entries older than 3 days,
and the prayer times kept for an old place or method (ptimes: keys).   Run: python3 tests/night_rebuild_test.py"""
import json, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

card = (HOME / "HomeCard.java").read_text(encoding="utf-8")
rule = (HOME / "NightRebuild.java").read_text(encoding="utf-8")
check("NightRebuild has no Android in it", "import android" not in rule and "android." not in rule.split("final class NightRebuild")[1])
pause = card.split("    void pause() {")[1].split("\n    }\n")[0]
refresh = card.split("    void refresh() {")[1].split("\n    }\n")[0]
night = card.split("private final Runnable night = new Runnable() {")[1].split("\n    };\n")[0]
check("the check starts only when the home leaves (pause), and a return stops it before anything else (refresh)",
      "idle.post(night);" in pause and refresh.index("idle.removeCallbacks(night);") < refresh.index("new Thread("))
check("it never acts while the home is in front (resumed), and asks NightRebuild with the page's age (the TV's sleep counted) and the hour",
      "if (resumed || web == null) return;" in night and "NightRebuild.due(false, android.os.SystemClock.elapsedRealtime() - builtAt," in night
      and "builtAt = android.os.SystemClock.elapsedRealtime();" in card.split("private boolean create(String u) {")[1])
check("letting go is the card's own destroy (the next return builds the page again), said with one fixed log word",
      'android.util.Log.i("UrTVCard", "card rebuild=nightly");' in night and "destroy();" in night and "idle.postDelayed(this, NightRebuild.CHECK_MS);" in night)
check("destroy stops the check", "idle.removeCallbacks(night);" in card.split("    void destroy() {")[1].split("\n    }\n")[0])

H = 3600_000
CASES = [((True, 30 * H, 3), "false", "the home in front: never, whatever the age and hour"),
         ((False, 21 * H, 3), "true", "away, a day old, at 3 at night: let go"),
         ((False, 21 * H, 1), "true", "... from 1:00"),
         ((False, 21 * H, 6), "false", "... not from 6:00"),
         ((False, 21 * H, 15), "false", "away, a day old, in the afternoon: kept"),
         ((False, 10 * H, 3), "false", "away at night but built this evening: kept"),
         ((False, 49 * H, 15), "true", "two days old and away: any hour"),
         ((False, -1, 3), "false", "a clock that went back: kept")]
if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    run = Path(tmp) / "NightRebuildRun.java"
    lines = "\n".join("        System.out.println(NightRebuild.due(%s, %dL, %d));" % ("true" if a else "false", b, c) for (a, b, c), _, _ in CASES)
    run.write_text("package sa.urtv.home;\npublic final class NightRebuildRun {\n    public static void main(String[] x) {\n" + lines + "\n    }\n}\n", encoding="utf-8")
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "NightRebuild.java"), str(run)], capture_output=True, text=True)
    check("NightRebuild.java compiles", r.returncode == 0, r.stderr[-400:])
    if r.returncode == 0:
        got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.NightRebuildRun"], capture_output=True, text=True).stdout.split()
        for (case, want, name), g in zip(CASES, got + [""] * len(CASES)):
            check(name, g == want, g)
else:
    print("SKIP NightRebuild cases: no javac here")

page = (ROOT / "src/template.html").read_text(encoding="utf-8")
fn = re.search(r"const LIVE_PATCH_KEEP_MS = [^\n]+\nfunction pruneLivePatch\(now\)\{[^\n]+\n", page)
check("the page lets go of LIVE_PATCH entries older than 3 days, before each round of live lookups",
      fn is not None and "LIVE_PATCH_KEEP_MS = 3 * 86400000" in fn.group(0) and "  pruneLivePatch(now);\n  const jobs = [" in page)
lp = page.split("const PT = 'ptimes:'")[1].split("\n}\n")[0]
check("today's prayer times are kept for the place and method in use only (an old place's or method's ptimes: key goes)",
      "localStorage.setItem(PT, JSON.stringify({d: want, t}));" in lp and "k.startsWith('ptimes:') && k !== PT) localStorage.removeItem(k);" in lp
      and lp.index("localStorage.setItem(PT") < lp.index("k.startsWith('ptimes:')"))
node = shutil.which("node")
if node and fn:
    js = "let LIVE_PATCH = {a:{at:0}, b:{at:Date.now()}, c:null, d:{at:Date.now() - 2*86400000}, e:{}};\n" + fn.group(0) + "pruneLivePatch(Date.now()); console.log(JSON.stringify(Object.keys(LIVE_PATCH).sort()));"
    out = subprocess.run([node, "-e", js], capture_output=True, text=True).stdout.strip()
    check("pruneLivePatch keeps the recent patches only (an old, empty or broken one goes)", out == '["b","d"]', out)
check("tests/run.sh runs this test", "tests/night_rebuild_test.py" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
