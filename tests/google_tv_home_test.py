#!/usr/bin/env python3
"""Google TV (user reports: TCL G10/G08, Changhong Android 14, Sony BRAVIA VH21, Xiaomi MiTV-AFMU0/1): since 4.3 the home switch fails there, the Home
key goes to launcherx whatever the role says, and the add-on's watch is how Home opens ours. The in-TV setup's report says so in one line, the
computer path has `dock.sh home-watch on|off|status` (the home's HOME_WANTED), and the agent notes say it. Read from the source.
Run: python3 tests/google_tv_home_test.py"""
import subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def read(p): return (ROOT / p).read_text(encoding="utf-8")
rep = read("android/launcher/src/main/java/sa/urtv/home/setup/SetupReport.java")
dock, settings, prompt, tvs = read("optimize/dock.sh"), read("docs/SETTINGS.md"), read("docs/SETUP-PROMPT.md"), read("docs/TVS.md")
MODELS = ("TCL G10/G08", "Changhong", "Sony BRAVIA VH21", "Xiaomi MiTV-AFMU0/1")

gl = rep.split("static String googleTvLine(boolean watch) {")[1].split("\n    }\n")[0]
check("the report's Google TV line: bound says the add-on path is how it works", "Google TV: Home works through the add-on's watch" in gl and "This is how it works on Google TV" in gl)
check("…not bound says the add-on is the only way, and how to turn it on", "is NOT bound" in gl and "«جهّز كل شي»" in gl)
check("…both say never turn launcherx off, and neither is a !! guard", gl.count("never turn launcherx off") == 2 and "!!" not in gl)
check("the line is added only on a Google TV (launcherx installed), after homeHow", "out.add(homeHow());\n        if (installed != null && installed.contains(RescuePlan.LAUNCHERX)) out.add(googleTvLine(watchBound));" in rep)
check("homeHow keeps what it read of the watch for it", "watchBound = watch;" in rep.split("private String homeHow()")[1].split("\n    }\n")[0])
check("dock.sh home-watch sends HOME_WANTED with on|off|status only", 'on|off|status) send HOME_WANTED "--es mode $2" ;;' in dock and 'home-watch:*) echo "usage: optimize/dock.sh home-watch on|off|status"; exit 1 ;;' in dock)
out = subprocess.run(["bash", str(ROOT / "optimize/dock.sh"), "home-watch", "maybe"], capture_output=True, text=True, cwd=ROOT)
check("a wrong word is refused before the TV is asked", out.returncode == 1 and "usage: optimize/dock.sh home-watch" in out.stdout, out.stdout + out.stderr)
for name, doc in (("SETTINGS.md", settings), ("SETUP-PROMPT.md", prompt), ("TVS.md", tvs)):
    check(name + " names the reported models and the add-on path", all(m in doc for m in MODELS) and "home-watch on" in doc)
check("the agent notes say the add-on path is how Home works on Google TV", "The add-on path is how Home works on Google\n  TV" in prompt and "**the add-on path is how it works on Google TV:**" in settings)
check("the «Home switch skipped» row points a Google TV to the add-on path", "**On a Google TV** (the stock home is `launcherx`) this is expected since 4.3: take the add-on path" in prompt)
sys.exit(1 if failed else 0)
