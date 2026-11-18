#!/usr/bin/env python3
"""4.4: «فوق تطبيق آخر» moved to the add-on, so the home offers it once, on the first run of 4.4, to whoever used it (R-274). The rule (OverlayOfferRule:
the kick-off alert on and over other apps, the add-on cannot draw, not kids, no setup running, once per version) runs on a JVM (tests/OverlayOfferRun.java);
the wiring (a thread for the IPC and the disk, the gentle bar, the once-flag, the exclusions), the English words and the changelog line are pinned.

    python3 tests/overlay_offer_test.py
"""
import shutil, subprocess, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


read = lambda p: Path(p).read_text(encoding="utf-8")

if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "OverlayOfferRule.java"), str(ROOT / "tests/OverlayOfferRun.java")], capture_output=True, text=True)
        check("OverlayOfferRule compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.OverlayOfferRun"], capture_output=True, text=True).stdout
            for l in out.splitlines():
                if l.startswith(("PASS ", "FAIL ")): check("rule: " + l.split(" ", 1)[1], l.startswith("PASS "))
            check("every rule case ran", "ALL PASS" in out, out[-300:])
else:
    print("SKIP the Java cases: no javac")

main = read(HOME / "MainActivity.java")
m = main.find("private void checkOverlayOffer()")
body = main[m:main.find("private void overlayAfterOffer()")]
check("the check runs on the home's reads thread, never on the UI thread (settings, add-on and prefs are IPC and disk)",
      "clockReads.execute" in body and "KickoffReceiver.settings(app)" in body and "Extras.canDraw()" in body and "getSharedPreferences" in body.split("clockReads.execute")[1])
check("the condition is the rule's: the kick-off setting on AND the add-on cannot draw", "OverlayOfferRule.due(set[0], set[2], draws, kids, setup, shown)" in body and "Extras.canDraw()" in body)
check("kids mode and a running setup keep it away, in the check and again in the bar",
      "Parental.state(app).kids" in body and "SetupHold.on()" in body
      and "askOverlay = (overlayDue || overlayOnScreen) && !askStock && !askUpdate && !askExtras && !kids && !quiet && !sa.urtv.home.setup.SetupHold.on()" in main)
check("once: marked seen as it first appears, and «لاحقاً» or «شغّلها» mark it too (a versioned flag in the home's prefs)",
      "if (askOverlay && !overlayOnScreen) { overlayOnScreen = true; markOverlayOffered(); }" in main
      and "if (nudgeIsOverlay) { overlayAfterOffer(); return; }" in main
      and "putInt(OverlayOfferRule.KEY, OverlayOfferRule.VERSION)" in main
      and "static final int VERSION = 1" in read(HOME / "OverlayOfferRule.java"))
check("«شغّلها» opens the add-on's own offer for the overlay, behind the TV-settings lock", 'Extras.intent(this, "overlay")' in main and "ParentalPolicy.PILL_TV_SETTINGS" in main.split("nudgeIsOverlay) { launchGuarded")[1][:80])
check("the bar says what is missing and the way (R2), and has English words",
      "«فوق تطبيق آخر» صار في «إضافات تلفزيونك»" in main
      and "«فوق تطبيق آخر» صار في «إضافات تلفزيونك»: شغّلها ليبقى التنبيه فوق التطبيق\t" in read(ROOT / "i18n/en/home.tsv") and "\nشغّلها\t" in read(ROOT / "i18n/en/home.tsv"))
cl = read(ROOT / "CHANGELOG.md")
sec = cl[cl.index("## 4.4"):cl.index("## 4.3.5")]
check("the 4.4 changelog says whoever used «فوق تطبيق آخر» must turn «إضافات تلفزيونك» on again, and where, in Arabic then English",
      "«فوق تطبيق آخر» صار في «إضافات تلفزيونك»" in sec and "لازم يشغّل «إضافات تلفزيونك» من جديد" in sec and "الإعدادات ← المحتوى ← تنبيه بداية المباراة ← فوق تطبيق آخر" in sec
      and "must turn on «إضافات تلفزيونك» again" in sec)

print("ALL PASS" if failed == 0 else f"{failed} FAILED")
raise SystemExit(1 if failed else 0)
