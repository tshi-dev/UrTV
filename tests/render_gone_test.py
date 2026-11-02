#!/usr/bin/env python3
"""A WebView whose renderer process dies (F14): every WebView of ours handles onRenderProcessGone, throws the dead view away (never reloads it),
and makes a new one at most twice in ten minutes. RenderGuard runs for real (tests/RenderGuardRun.java, the copy of each app); the hosts are
read from the source (they need Android).  Run: python3 tests/render_gone_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

text = lambda p: p.read_text(encoding="utf-8")
a_guard, h_guard = text(APP / "RenderGuard.java"), text(HOME / "RenderGuard.java")
check("the two apps carry the same RenderGuard", a_guard.split("\n", 1)[1] == h_guard.split("\n", 1)[1])

# every WebView we create has a client that answers a dead renderer
sources = list(APP.glob("*.java")) + list(HOME.glob("*.java")) + list((HOME / "tilawah").glob("*.java"))
makers = [p for p in sources if "new WebView(" in text(p)]
check("the files that make a WebView: ScreenView, HomeCard and «المباريات» (MatchesActivity), no others",
      sorted(p.name for p in makers) == ["HomeCard.java", "MatchesActivity.java", "ScreenView.java"], [p.name for p in makers])
for p in makers:
    s = text(p)
    check(f"{p.name}: onRenderProcessGone is handled and answers true (the app is not taken down with the renderer)",
          "onRenderProcessGone(" in s and re.search(r"onRenderProcessGone\(.*?\{.*?return true;", s, re.S) is not None)
view = text(APP / "ScreenView.java")
check("ScreenView: API 26 callback marked @TargetApi(26) (the app runs from 21)", "@TargetApi(26)" in view)
check("ScreenView: the host is told, or the dead view is released", "gone.onGone(v)" in view and "else release(v)" in view)
check("nothing reloads a dead view", "loadUrl" not in view[view.index("onRenderProcessGone"):view.index("onRenderProcessGone") + 400])
dream, act = text(APP / "ScreenDream.java"), text(APP / "MainActivity.java")
for name, s in (("ScreenDream", dream), ("MainActivity", act)):
    check(f"{name}: records the death, releases the dead view, makes another only if the guard allows and it is still on screen, else finishes",
          "guard.gone(" in s and "ScreenView.release(dead)" in s and "guard.mayCreate(" in s and "finish();" in s and "dead != web" in s)
settings = text(APP / "SettingsActivity.java")
check("SettingsActivity: a preview whose renderer died goes blank and is not made again by itself", "ScreenView.release(dead);" in settings and "web[0] = null; shown[0] = -1;" in settings)
card = text(HOME / "HomeCard.java")
check("HomeCard: the dead view is destroyed, made again only while the home screen is in front and the guard allows",
      "guard.gone(" in card and "if (resumed) refresh();" in card and "guard.mayCreate(" in card and "if (v != web) { v.destroy(); return true; }" in card)
check("HomeCard: resume does not bring a view back while the guard says no", card.index("guard.mayCreate(") < card.index("create(want);"))
matches = text(HOME / "MatchesActivity.java")
check("«المباريات»: a dead renderer takes the page away (destroyed, the activity finishes), never reloaded",
      "if (v == web) web = null;" in matches and "v.destroy();" in matches and "finish();   // the renderer died" in matches
      and "loadUrl" not in matches[matches.index("onRenderProcessGone"):matches.index("onRenderProcessGone") + 300])
check("the only pauseTimers/resumeTimers are HomeCard's and «المباريات»'s (never in front together: each resumes them as it comes, pauses them as it goes; the screensaver app none)",
      sorted(p.name for p in sources if "pauseTimers" in text(p) or "resumeTimers" in text(p)) == ["HomeCard.java", "MatchesActivity.java"]
      and "web.onResume(); web.resumeTimers();" in matches and "web.onPause(); web.pauseTimers();" in matches)

if not shutil.which("javac"):
    print("SKIP render guard: no javac here")
    sys.exit(1 if failed else 0)
for src, cls in ((APP / "RenderGuard.java", "sa.urtv.screensaver.RenderGuard"), (HOME / "RenderGuard.java", "sa.urtv.home.RenderGuard")):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-d", tmp, str(src), str(ROOT / "tests/RenderGuardRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "RenderGuardRun", cls], check=True, capture_output=True).stdout.decode()
    rows = [l.split("\t") for l in out.split("\n") if l]
    for name, res in rows:
        check(f"{cls.split('.')[2]}: {name}", res == "ok")
    check(f"{cls}: every check ran", len(rows) == 8, len(rows))
sys.exit(1 if failed else 0)
