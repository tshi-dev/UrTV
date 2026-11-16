#!/usr/bin/env python3
"""4.3: the home offers «تحديث تلفزيوني» on the TV when the screensaver app is older than the home, or a newer build is out: the rules (UpdateNoticeRule) run in
Java, and the wiring (a pill in the top row, the release read off the main thread at most every 12 hours, nothing installed from the home).

    python3 tests/update_notice_test.py
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
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "UpdateNoticeRule.java"), str(ROOT / "tests/UpdateNoticeRun.java")], capture_output=True, text=True)
        check("it compiles", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.UpdateNoticeRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
            check("every rule ran", "ALL PASS" in out, out[-200:])
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
un = (HOME / "UpdateNotice.java").read_text(encoding="utf-8")
check("a pill «تحديث جديد» opens the update screen (behind its lock) and is in the row only while UpdateNotice says so, never in kids mode",
      'new Intent(this, UpdateActivity.class)' in main and "UpdateNotice.wanted(this)" in main and "!Parental.state(this).kids && UpdateNotice.wanted(this)" in main)
check("the release of the person's channel (apk-latest, or apk-next for a tester) is read off the main thread, at most every 6 hours, only its first 64 bytes, never during playback or the adhan; nothing is installed from the notice",
      "new Thread(" in un and "Net.get(UpdateRules.baseFor(ch) + \"version.txt\", null, 10000, 12000, 64)" in un and "UpdateChannel.read(c)" in un
      and "MusicService.carryOurPlaying() || sa.urtv.home.tilawah.NowPlaying.adhanLive()" in un and "UpdateNoticeRule.due(" in un and "PackageInstaller" not in un and "ACTION_VIEW" not in un)
check("4.4: all three apps are compared (the home, the screensaver app, «إضافات تلفزيونك» where it is installed); a missing screensaver keeps «ثبّت الجزء الثاني»",
      "UpdateNoticeRule.behind(code(c, c.getPackageName()), code(c, SAVER), code(c, ExtrasWords.PACKAGE), newest(c))" in un and "k < 0 ? 0 : k" in un)
check("4.4: the one-time card «في تحديث لـ …» on the home's bar: OK opens «تحديث تلفزيوني» behind its lock, never in kids mode or over playback or the adhan, marked shown as it appears, «لاحقاً» hides it",
      'S.t("في تحديث لـ %s", names(b))' in un and "UpdateNotice.card(this, updateCardOnScreen, nowMs)" in main and "kids || quiet ? null :" in main
      and "UpdateNotice.shown(this, card.target, nowMs)" in main and "if (nudgeIsUpdate) { launchGuarded(ParentalPolicy.PILL_UPDATES, new Intent(this, UpdateActivity.class)" in main
      and "if (nudgeIsUpdate) { updateCardOnScreen = -1; updateHomeNudge(false); return; }" in main)
check("4.4: a good read of the release updates the pill and the card at once", "if (resumed) updateHomeNudge(false);" in main)
sys.exit(1 if failed else 0)
