#!/usr/bin/env python3
"""«تحديث تلفزيوني» in the 4.2 language (owner, 4.3): a card on a solid surface inside the safe area, a quiet four-step line (no pills), a calm progress bar with the percent
and the MB, one 56 dp button; the same behaviour (the same states and buttons), read-only counters from PhoneUpdater, the picture hook in debug builds only.

    python3 tests/update_ui_test.py
"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


ua, pu = text(HOME / "UpdateActivity.java"), text(HOME / "phone/PhoneUpdater.java")
check("the card: a solid surface of the look's dialog fill, 760 dp at most and inside the 5 % safe area plus 16 dp",
      "GlassPaint.fill(this, GlassStyle.over(look.dialog, look.base), 22, look.line)" in ua and "Math.min(dp(760)" in ua and "Math.round(h * 0.05f) + dp(16)" in ua)
check("the steps «تنزيل · تحقق · تثبيت · تم» are a line and four dots (a custom view), laid out the way the language reads, with no pill",
      'S.t("تنزيل"), S.t("تحقق"), S.t("تثبيت"), S.t("تم")' in ua and "private final class Stepper extends View" in ua and "boolean rtl = Dir.rtl();" in ua and "setBackground" not in ua.split("class Stepper")[1].split("// ----")[0])
check("the bar: an 8 dp track with the share in the accent colour, «٤٥٪ · ١٢٫٦ من ٢٨٫٠ م.ب» under it; nothing when the size is unknown",
      "new CalmBar(this, look.line, GOLD)" in ua and 'S.t("%s من %s م.ب"' in ua and "if (total <= 0) return;" in ua)
check("the one button of a state is 56 dp high, full width, 24 dp below what it answers", "new LinearLayout.LayoutParams(-1, dp(56))" in ua and "lp.topMargin = dp(24);" in ua)
check("every state and button is still there (checking, downloading, uptodate, found/available, waiting_owner, done, error with its reasons, the record of an earlier update)",
      all(w in ua for w in ('case "checking":', 'case "downloading"', 'case "uptodate":', 'case "found":', 'case "available":', 'case "waiting_owner":', 'case "done":', 'case "error":',
                            'case "offline":', 'case "need_permission":', 'case "declined":', 'case "failed":', "renderRecord()", "doneButton()", "checkButton()", "aiMessage()")))
check("PhoneUpdater only counts bytes for the bar (read-only: progress(), told at most four times a second); no decision changed",
      "public static long[] progress()" in pu and "if (now - dlAt < 250) return;" in pu and "dlDone += n;" in pu and "verifying = true;" in pu and "verifying = false;" in pu)
check("the picture hook is for debug builds: the extra is read only when BuildConfig.DEBUG, the receiver lives in src/debug",
      "if (BuildConfig.DEBUG) preview = getIntent().getStringExtra(\"preview\");" in ua and "extends Activity" in text(ROOT / "android/launcher/src/debug/java/sa/urtv/home/DebugUpdateShot.java")
      and "DebugUpdateShot" not in text(ROOT / "android/launcher/src/main/AndroidManifest.xml"))
wf = text(ROOT / ".github/workflows/update-shots.yml")
sh = text(ROOT / ".github/screens/update_shots.sh")
check("a picture is taken only while UpdateActivity is the resumed activity (else the script fails), and only a successful run commits, and only all 72 pictures",
      'grep -q "$expect"' in sh and "topResumedActivity" in sh and sh.count("exit 1") >= 2 and "sa.urtv.home/.DebugUpdateShot" in sh and "sa.urtv.home/.setup.DebugSetupShot" in sh
      and "if: success() && contains(join(github.event.commits.*.message, ' '), '[shots]')" in wf and "= 72" in wf)
check("the pictures workflow: this branch and master with the hook, glass and sand, Arabic and English, committed on «[shots]»",
      "branches: ['work/update-ui', 'work/fix-182']" in wf and "update_before_patch.py" in wf and "contains(join(github.event.commits.*.message, ' '), '[shots]')" in wf
      and "for look in glass sand" in text(ROOT / ".github/screens/update_shots.sh") and "for lang in ar en" in text(ROOT / ".github/screens/update_shots.sh"))

# the setup screen in the same language (4.3)
sa_ = text(HOME / "setup/SetupActivity.java")
check("setup: one card on a solid surface in the safe area (880 dp at most), title 28, the steps a vertical rail (done tick, now ring with its line under it, next hollow), «٧ من ١٥» with the calm bar",
      "GlassPaint.fill(this, sa.urtv.home.GlassStyle.over(look.dialog, look.base), 22, look.line)" in sa_ and "Math.min(dp(880)" in sa_ and "new SetupProgressView(this, look" in sa_
      and 'S.t("%s من %s خطوة"' in sa_ and "new CalmBar(c, look.line, look.accent)" in (HOME / "setup/SetupProgressView.java").read_text(encoding="utf-8") and "Math.max(dp(880), screenW" in sa_)
check("setup: the restart notice is said before the TV restarts (the measuring steps, on a TV that survives it), one primary button of 56 dp and the other choices quiet",
      'S.t("سيعيد التلفزيون التشغيل الآن، وسيكمل بنفسه.")' in sa_ and "pv.restarts" in sa_ and "t.setMinHeight(dp(56));" in sa_ and sa_.count("quiet(S.t(") >= 4)
check("setup: the behaviour is the runner's, untouched: the same go/confirm/cancel calls", all(w in sa_ for w in ("SetupRunner.get().confirm(new java.util.ArrayList<>(picks))", "SetupRunner.get().cancel()", "go(false)", "go(true)", "dismissed = e;")))
dbg = text(ROOT / "android/launcher/src/debug/java/sa/urtv/home/setup/DebugSetupShot.java")
check("setup: the picture hook is debug only (src/debug), and sets the runner's fields, nothing runs", "SetupRunner.class.getDeclaredField(name)" in dbg and "DebugSetupShot" not in text(ROOT / "android/launcher/src/main/AndroidManifest.xml"))
check("setup: «تم» on the done screen opens the home and closes the screen (build 208: nothing happened, the screen stayed in front)",
      "dismissed = e;\n                // «تم» ends the setup" in sa_ and "sa.urtv.home.MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));\n                finish();" in sa_)
sys.exit(1 if failed else 0)
