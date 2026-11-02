#!/usr/bin/env python3
"""The tester's steps 16 to 20 on the owner's TCL (Android 14, build 174): source checks of the fixes that have no JVM case of their own.
The JVM cases are in their modules' tests (A11yListRun, VersionsRun, AdbClientRun, the install.sh stub, the share page...).

    python3 tests/tester_fixes_test.py
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


# S7: the settings row's second line wraps to two lines instead of «..»
settings = text(APP / "SettingsActivity.java")
row = settings.split("private View row(String label, String value, int icon, final Runnable onClick) {")[1].split("card.setOnFocusChangeListener")[0]
check("S7: a row's value wraps to a second line (max 2), only a third is cut", "val.setMaxLines(2);" in row and "val.setSingleLine(true);" not in row)

# B5/B6: the mute and our accessibility entry carried over our own update
carry = text(HOME / "UpdateCarry.java")
heal = text(HOME / "A11yHeal.java")
upd = text(HOME / "phone/PhoneUpdater.java")
check("B5/B6: the state is recorded right before the files go to the installer", upd.index("UpdateCarry.before(app);") < upd.index("installNext(app);", upd.index("UpdateCarry.before(app);")))
check("B5/B6: recorded with commit() (the process is replaced next), with the build that recorded it", ".commit();" in carry and 'putInt("code", BuildConfig.VERSION_CODE)' in carry
      and 'if (sp.getInt("code", -1) == BuildConfig.VERSION_CODE) return;' in carry)
check("B5/B6: A11yHeal hands the replace and the home's first show (not a boot) to UpdateCarry.after, before its own check",
      'if (!"boot".equals(why))' in heal and "UpdateCarry.after(c, why);" in heal and heal.index("UpdateCarry.after(c, why);") < heal.index("String list = read(c, LIST), sw"))
check("B5: a TV muted before the install is muted again (now and once 20 s later), never unmuted by us",
      "if (wasMuted) {" in carry and "ADJUST_MUTE" in carry and "ADJUST_UNMUTE" not in carry and "AGAIN_MS = 20_000" in carry)
check("B5: the installer itself never touches the sound", "AudioManager" not in upd)
check("B6: only our entries the install lost are added back, only with WRITE_SECURE_SETTINGS; else a one-time notice",
      "A11yList.ownLost(before, now, A11yHeal.TYPE, A11yHeal.SLEEP)" in carry and "A11yList.with(now, lost)" in carry
      and "WRITE_SECURE_SETTINGS" in carry and "Toast.makeText" in carry and "sp.edit().clear().commit();" in carry)
check("B6: the record is used once and only within 30 minutes", "FRESH_MS = 30 * 60_000L" in carry and "if (!fresh(at, System.currentTimeMillis())) return;" in carry)
m = re.search(r"static boolean fresh\(long at, long now\) \{ return (.*?); \}", carry)
fresh = (lambda at, now: eval(m.group(1).replace("&&", "and").replace("FRESH_MS", "1800000"), {}, {"at": at, "now": now})) if m else None
check("B6: fresh(): within the window yes, before it, after it and with no record no", fresh is not None and fresh(1000, 1000 + 60_000)
      and not fresh(1000, 1000 + 1_800_001) and not fresh(0, 5) and not fresh(5000, 1000))

# C10: the done screen's QR code: the packed report, centred in a white card (16 dp), the caption under it, the 8 dp grid
act = text(HOME / "setup/SetupActivity.java")
done = act[act.index("private void buildDone("):act.index("// ---- the buttons")]
qrdp = re.search(r"static final int QR_DP = (\d+);", act)
check("C10: the QR code is a multiple of 8 dp and smaller than the old 300", qrdp is not None and int(qrdp.group(1)) % 8 == 0 and int(qrdp.group(1)) < 300, qrdp and qrdp.group(1))
check("C10: centred, in a white card with 16 dp around it, the caption centred under it", "card.setPadding(dp(16), dp(16), dp(16), dp(16));" in done
      and "white.setColor(0xFFFFFFFF);" in done and done.count("Gravity.CENTER_HORIZONTAL") >= 3 and "Gravity.RIGHT" not in done
      and done.index("body.addView(card, cp);") < done.index("body.addView(caption, tp);"))
margins = [int(x) for x in re.findall(r"(?:topMargin|bottomMargin) = dp\((\d+)\)", done[done.index("QrView qr"):done.index("body.addView(caption, tp);")])]
check("C10: its spacing is on the 8 dp grid", margins and all(m % 8 == 0 for m in margins), margins)
qv = text(HOME / "phone/QrView.java")
check("C10: QrView draws whole pixels per module, centred, with the four-module quiet zone", "Math.floor(side / (float) n)" in qv and "code.size + 8" in qv and "(getWidth() - cell * n) / 2f" in qv)
rep = text(HOME / "setup/SetupReport.java")
check("C10: the link carries the packed report (raw deflate, base64url, «z.»)", 'PAGE + "#" + PACKED + pack(report)' in rep and "new java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION, true)" in rep
      and "getUrlEncoder().withoutPadding()" in rep and 'PACKED = "z."' in rep)

# E17: the setup's done screen in English
sys.path.insert(0, str(ROOT / "tests"))
setup_tsv = dict(l.split("\t", 1) for l in text(ROOT / "i18n/en/setup.tsv").splitlines() if l and not l.startswith("#"))
home_tsv = dict(l.split("\t", 1) for l in text(ROOT / "i18n/en/home.tsv").splitlines() if l and not l.startswith("#"))
fixed = re.findall(r'(?:text|button)\((?:digits\()?S\.t\("([^"]+)"\)', done)
check("E17: the done screen's 13 fixed lines go through S.t (none left as a bare Arabic literal)", len(fixed) == 12 and 'add(text("' not in done and 'button("' not in done, (len(fixed), fixed))
check("E17: every one has its English in i18n/en/setup.tsv (the title in home.tsv)", all(f in setup_tsv for f in fixed) and "إعداد تلفزيوني" in home_tsv
      and 'text(S.t("إعداد تلفزيوني")' in act, [f for f in fixed if f not in setup_tsv])
runner = text(HOME / "setup/SetupRunner.java")
endof = runner[runner.index("static End endOf("):runner.index("A cut connection said")]
ends = re.findall(r'S\.n\("([^"]+)"\)', act[act.index("static final String[] ENDS"):act.index("private void buildDone(")])
fixed_ends = [e for e in re.findall(r'"([^"]*[\u0600-\u06FF][^"]*)"', endof) if "(" not in e or e.endswith(")") or "." in e]
check("E17: the run's fixed end sentences are marked (ENDS) and shown with S.tv", all(e in endof for e in ends) and len(ends) == 4 and "say(S.tv(e.text)" in done
      and all(e in setup_tsv for e in ends))
check("E17: the screen follows «اللغة»: S.wrap, and text starts on the right in Arabic, on the left in English", "super.attachBaseContext(S.wrap(b))" in act and "t.setGravity(Dir.start());" in act)

sys.exit(1 if failed else 0)
