#!/usr/bin/env python3
"""Step 41 (build 209): while the home app replaces itself, the app used before (Apple TV) came to the front for about 5 s. Now the screensaver
app's UpdateCover (a package the install does not touch) is opened right under the installer's confirm screen, in one startActivities call from
the update screen; it presses Home once the home app is replaced, closes when the home app says declined or failed, and never stays past its
limit. CoverRule runs for real (tests/UpdateCoverRun.java); the wiring is read from the sources.   python3 tests/update_cover_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

cover = (APP / "UpdateCover.java").read_text(encoding="utf-8")
upd = (ROOT / "android/launcher/src/main/java/sa/urtv/home/phone/PhoneUpdater.java").read_text(encoding="utf-8")
app_mf = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
home_mf = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

m = re.search(r'<activity\s+android:name="\.UpdateCover"(.*?)/>', app_mf, re.S)
check("the cover is exported only behind a signature permission, its own task, out of recents",
      m and 'android:permission="sa.urtv.screensaver.PHONE_SETTINGS"' in m.group(1) and 'android:launchMode="singleTask"' in m.group(1)
      and 'android:excludeFromRecents="true"' in m.group(1)
      and re.search(r'<permission\s+android:name="sa.urtv.screensaver.PHONE_SETTINGS"\s+android:protectionLevel="signature"', app_mf), m and m.group(1))
check("the home app holds that permission already (no new one)", '<uses-permission android:name="sa.urtv.screensaver.PHONE_SETTINGS" />' in home_mf and "UPDATE_COVER" not in home_mf + app_mf)
check("only over our own replace, the cover under the confirm screen in one call; without it (an older screensaver app) the confirm screen alone",
      "if (UpdateOrder.replacesUs(installing) && coverThere(c))" in upd and "c.startActivities(new Intent[] {cover(), screen});" in upd
      and upd.index("c.startActivities(new Intent[] {cover(), screen});") < upd.index("c.startActivity(screen);"))
fail_body = upd.split("private static void fail(")[1][:200]
declined = upd.split("if (UpdateFlow.isDeclined(why)) {")[1][:120]
check("a failed or declined install closes the cover (the update screen under it says why)", "closeCover(c);" in fail_body and "closeCover(c);" in declined)
check("the close is sent to the screensaver app only, and the cover takes it only from a holder of the permission",
      "new Intent(COVER_CLOSE).setPackage(COVER_PKG)" in upd and "InternalReceivers.registerFromHome(this, close, cf, PERMISSION)" in cover)
check("the cover listens for the home app's replace, presses Home from the front, and has a limit and Back",
      "Intent.ACTION_PACKAGE_REPLACED" in cover and 'addDataScheme("package")' in cover and "Intent.CATEGORY_HOME" in cover
      and "LIMIT_MS = 120_000" in cover and "handler.postDelayed(limit, LIMIT_MS)" in cover and "KeyEvent.KEYCODE_BACK" in cover)
check("its line has its English", "يكمّل التحديث… ترجع الشاشة الرئيسية بعد لحظات\t" in (ROOT / "i18n/en/saver.tsv").read_text(encoding="utf-8")
      and 'S.t("يكمّل التحديث… ترجع الشاشة الرئيسية بعد لحظات")' in cover)

if not shutil.which("javac"):
    print("SKIP the cover's rules: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "CoverRule.java"), str(ROOT / "tests/UpdateCoverRun.java")], capture_output=True, text=True)
        check("CoverRule compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.UpdateCoverRun"], capture_output=True, text=True).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            check("CoverRule: every case", len(lines) == 8 and all(v == "ok" for _, v in lines), out)
sys.exit(1 if failed else 0)
