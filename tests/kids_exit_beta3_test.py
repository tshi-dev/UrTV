#!/usr/bin/env python3
"""4.4 beta 3 (a user on X): kids mode had no way out he could see, and the QR codes opened «لا يمكن الوصول إلى هذا الموقع» in the phone's
browser while the same address typed worked. Kids mode now shows «الخروج من وضع الأطفال» in the home's row (where «مركز التحكم» was, always
shown, opening «الرقابة الأبوية», which asks the PIN when there is one), and the phone page has the same button while kids mode is on (it opens
that screen on the TV; the PIN is asked there). Under every QR: what to do when the camera's link does not open.
    python3 tests/kids_exit_beta3_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
L = ROOT / "android/launcher/src/main"
failed = 0


def check(name, ok):
    global failed
    print(("PASS " if ok else "FAIL ") + name)
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
main, schema = read(L / "java/sa/urtv/home/MainActivity.java"), read(L / "java/sa/urtv/home/phone/Schema.java")
ar, en = read(L / "assets/phone/index.html"), read(L / "assets/phone/index.en.html")
phone_act = read(L / "java/sa/urtv/home/PhoneActivity.java")
quiz = read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/QuizActivity.java")
game = read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/GameActivity.java")
home_tsv, saver_tsv = read(ROOT / "i18n/en/home.tsv"), read(ROOT / "i18n/en/saver.tsv")

ex = main[main.index("    private void applyKidsExit() {"):]
ex = ex[:ex.index("\n    }\n") + 6]
check("the home: kids mode shows «الخروج من وضع الأطفال» in the row while it is on and only then, its OK opens «الرقابة الأبوية» (the PIN asked there)",
      'kidsExitPill = pill(S.t(KIDS_LEAVE), R.drawable.ic_lock);' in main and "kidsExitPill.setOnClickListener(v -> openParental());" in main
      and "boolean want = Parental.state(this).kids" in ex and "pillRow.addView(kidsExitPill" in ex and "pillRow.removeView(kidsExitPill);" in ex)
check("…put in or taken out every time the control centre's place is (a return to the home, the minute tick), before that pill's own early return",
      main.index("applyKidsExit();") < main.index("boolean want = centerWanted(), has = centerPill.getParent() == pillRow;"))
check("…always shown, never behind the row's closed state (one of its main buttons)", "right.mains(centerPill, phonePill, kidsExitPill);" in main)
check("the phone: «@parental» is a tool the page may ask for, the one the home opens even in kids mode, «الرقابة الأبوية» on the TV",
      '"@parental"}' in schema and '!"@parental".equals(t) && Parental.state(this).kids' in main and 'case "@parental": openParental(); break;' in main)
for name, page, words in (("Arabic", ar, "الخروج من وضع الأطفال"), ("English", en, "Leave kids mode")):
    check(f"the phone page ({name}): the button while kids mode is on and only then, asking for «@parental»",
          f'<button id="kidsOut" class="wide hidden">{words}</button>' in page and "$('kidsOut').classList.toggle('hidden', !fam.kids);" in page
          and "$('kidsOut').onclick = function () { launch('@parental'); };" in page)
check("the words have their English", "الخروج من وضع الأطفال\t" in home_tsv and "ما فتح؟ افتح متصفح الجوال واكتب العنوان بنفسك\t" in home_tsv
      and "امسح الرمز بكاميرا الجوال. ما فتح؟ اكتب العنوان في متصفح الجوال\t" in saver_tsv)
check("the QRs: a full http:// address (the phone's and the games'), and under each what to do when the camera's link does not open",
      'url = "http://" + lan.getHostAddress() + ":" + port + "/" + code;' in quiz and "ما فتح؟ افتح متصفح الجوال واكتب العنوان بنفسك" in phone_act
      and "امسح الرمز بكاميرا الجوال. ما فتح؟ اكتب العنوان في متصفح الجوال" in quiz and "امسح الرمز بكاميرا الجوال. ما فتح؟ اكتب العنوان في متصفح الجوال" in game
      and "امسح الرمز بكاميرا الجوال، أو افتح العنوان" not in quiz + game)
sys.exit(1 if failed else 0)
