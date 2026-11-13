#!/usr/bin/env python3
"""4.3: «خلّ تلفزيونك يفتح على UrTV»: while the home app is not the TV's Home (a Downloader install that was only opened), one card at the top opens «إعداد تلفزيوني»;
«لاحقاً» hides it for 7 days, then 30, then for good; never once we are the Home or in kids mode. The rules (HomeNudgeRule) run in Java; the wiring is pinned.

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
      "buildHomeNudge(root);" in main and "updateHomeNudge(true);" in main and "HomeNudgeRule.show(isDefaultHome(), kids, np.getLong(\"later_until\", 0), nowMs)" in main
      and "sa.urtv.home.setup.SetupActivity.class" in main.split("private void buildHomeNudge", 1)[1][:2500] and "HomeNudgeRule.laterUntil(System.currentTimeMillis(), presses)" in main)
check("its words are in the English table, and the bar is ONE short line in the top-centre slot (it never covers a card: the first version was a two-line card with a button under it)",
      all(w in tsv for w in ("خلّ تلفزيونك يفتح على UrTV\t", "لاحقاً\t")) and "lp(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dp(30), 0, 0));" in main.split("private void buildHomeNudge", 1)[1][:3500]
      and "nudgeCard.setSingleLine(true);" in main and "nudge.setOrientation(LinearLayout.HORIZONTAL);" in main and "dp(48)" not in main.split("private void buildHomeNudge", 1)[1][:2000])
wire = main.split("private void wireNudge()", 1)[1].split("private void updateHomeNudge", 1)[0]
check("reachable with the remote (hotfix 4.3): UP from every top-row button reaches the bar while it shows (the buttons' own UP was themselves), DOWN goes back, the two buttons are side by side in the screen's direction (RTL and LTR)",
      "for (View p : pills) p.setNextFocusUpId(on ? nudgeCard.getId() : p.getId());" in wire and "nudgeCard.setNextFocusDownId(pill0);" in wire and "nudgeLater.setNextFocusDownId(pill0);" in wire
      and "boolean rtl = Dir.rtl();" in wire and "nudgeCard.setNextFocusRightId(rtl ? nudgeCard.getId() : nudgeLater.getId());" in wire and "nudgeLater.setNextFocusLeftId(rtl ? nudgeLater.getId() : nudgeCard.getId());" in wire
      and main.count("wireNudge();") >= 3 and main.index("wireNudge();   // the bar above the top row") < main.index("int topId = card != null"))
check("it takes the focus the first time it shows, after the home has laid out (posted), once per run; OK opens «إعداد تلفزيوني», «لاحقاً» and Back on the bar snooze it",
      "nudge.post(() -> { if (nudge.getVisibility() == View.VISIBLE && !nudgeFocused) { nudgeFocused = true; nudgeCard.requestFocus(); } });" in main and "nudgeLater.setOnClickListener(v -> snoozeNudge());" in main
      and "if (nudge != null && nudge.getVisibility() == View.VISIBLE && (nudgeCard.hasFocus() || nudgeLater.hasFocus())) { snoozeNudge(); return; }" in main
      and 'putInt(k + "later_count", presses + 1)' in main)
# the bar is new in 4.3 (4.2 had none): in 4.3 the top-row buttons' UP was themselves and nothing wired the bar; its one focus request came before the home's own
check("a debug shot starts it without a setup (DebugNudgeShot, declared in the debug manifest only)",
      (ROOT / "android/launcher/src/debug/java/sa/urtv/home/DebugNudgeShot.java").is_file() and "DebugNudgeShot" in (ROOT / "android/launcher/src/debug/AndroidManifest.xml").read_text(encoding="utf-8")
      and "DebugNudgeShot" not in (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
saver = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
check("settings: «اجعل UrTV الشاشة الرئيسية» opens the launcher's «إعداد تلفزيوني» when we are not the Home and says «✓ UrTV هو الشاشة الرئيسية» when we are; the manifest lets the app see the Home resolver",
      'row("اجعل UrTV الشاشة الرئيسية", isHome ? "✓ UrTV هو الشاشة الرئيسية"' in saver and '"sa.urtv.home.setup.SetupActivity"' in saver and "if (!isHome) openSetup();" in saver
      and "android.intent.category.HOME" in (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
# ---- 4.3.1 the city question: the same bar, one at a time
tail = main.split("private void updateHomeNudge", 1)[1][:2600]
cc = (HOME / "ControlCenter.java").read_text(encoding="utf-8")
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "HomeNudgeRule.java"), str(HOME / "CityAskRule.java"), str(HOME / "VersionMismatchRule.java"), str(ROOT / "tests/CityAskRun.java")], capture_output=True, text=True)
        check("CityAskRule compiles", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.CityAskRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
            check("every city rule ran", "ALL PASS" in out, out[-200:])
check("the question is the nudge's bar: due only while no city was chosen (the screensaver's city_chosen), never together with the nudge (the city first), its own snooze count, OK opens the chooser",
      "CityAskRule.show(isInstalled(SAVER_PKG), cityChosenNow, java.util.TimeZone.getDefault().getID(), kids" in tail and "boolean show = askUpdate || askCity || HomeNudgeRule.show(" in tail
      and "nudgeIsCity = askCity;" in tail and 'String k = nudgeIsUpdate ? "ver_" : nudgeIsCity ? "city_" : "";' in main and "if (nudgeIsCity) { openCity(); return; }" in main
      and "cfg.cityChosen != cityChosenNow" in main)
check("the home asks the screensaver (city_chosen column; an older one says nothing = chosen, nobody is asked), and its words are in the English table",
      '"city_chosen"' in (ROOT / "android/app/src/main/java/sa/urtv/screensaver/ConfigProvider.java").read_text(encoding="utf-8") and "boolean cityChosen = true;" in (HOME / "ConfigSnapshot.java").read_text(encoding="utf-8")
      and all(w in tsv for w in ("وين أنت؟ اختر مدينتك عشان الطقس والصلاة\t", "اختر المدينة\t", "للطقس والصلاة\t")))
check("the screensaver app opens the chooser on the extra (onCreate and onNewIntent), and the settings' FIRST button is «المدينة», wide, with the current city",
      'getBooleanExtra("pickCity", false)' in saver and "onNewIntent" in saver and "pickCity();" in saver.split("onNewIntent", 1)[1][:300]
      and (SAVER_HUB := (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsHub.java").read_text(encoding="utf-8")).split("CATEGORIES = {", 1)[1].lstrip().startswith("{CITY, \"المدينة\"")
      and "case SettingsHub.CITY: return Words.city(prefs.city());" in saver)
sys.exit(1 if failed else 0)
