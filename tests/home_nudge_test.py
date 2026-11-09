#!/usr/bin/env python3
"""4.3: «خلّ تلفزيونك يفتح على UrTV»: while the home app is not the TV's Home (a Downloader install that was only opened), one card at the top opens «إعداد تلفزيوني»;
«لاحقاً» hides it for 3 days; never once we are the Home or in kids mode. The rules (HomeNudgeRule) run in Java; the wiring is pinned.

    python3 tests/home_nudge_test.py
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "HomeNudgeRule.java"), str(ROOT / "tests/HomeNudgeRun.java")], capture_output=True, text=True)
        check("it compiles", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.HomeNudgeRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
            check("every rule ran", "ALL PASS" in out, out[-200:])
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
tsv = (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8")
check("the card is built, shown from onResume by HomeNudgeRule (isDefaultHome, kids, «لاحقاً»), opens SetupActivity behind its lock, and «لاحقاً» stores 3 days",
      "buildHomeNudge(root);" in main and "updateHomeNudge(true);" in main and "HomeNudgeRule.show(isDefaultHome(), Parental.state(this).kids" in main
      and "sa.urtv.home.setup.SetupActivity.class" in main.split("private void buildHomeNudge", 1)[1][:2500] and "HomeNudgeRule.laterUntil(System.currentTimeMillis())" in main)
check("its words are in the English table, and the card sits inside the safe area (top 30 dp, centred)",
      all(w in tsv for w in ("خلّ تلفزيونك يفتح على UrTV\t", "اضغط هنا وكمّل إعداد تلفزيوني (دقيقتين)\t", "لاحقاً\t")) and "lp(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dp(30), 0, 0));\n    }\n\n    /** as the home comes" in main)
check("a debug shot starts it without a setup (DebugNudgeShot, declared in the debug manifest only)",
      (ROOT / "android/launcher/src/debug/java/sa/urtv/home/DebugNudgeShot.java").is_file() and "DebugNudgeShot" in (ROOT / "android/launcher/src/debug/AndroidManifest.xml").read_text(encoding="utf-8")
      and "DebugNudgeShot" not in (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
saver = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
check("settings: «اجعل UrTV الشاشة الرئيسية» opens the launcher's «إعداد تلفزيوني» when we are not the Home and says «✓ UrTV هو الشاشة الرئيسية» when we are; the manifest lets the app see the Home resolver",
      'row("اجعل UrTV الشاشة الرئيسية", isHome ? "✓ UrTV هو الشاشة الرئيسية"' in saver and '"sa.urtv.home.setup.SetupActivity"' in saver and "if (!isHome) openSetup();" in saver
      and "android.intent.category.HOME" in (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
