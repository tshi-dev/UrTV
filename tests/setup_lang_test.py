#!/usr/bin/env python3
"""Arabic is the default everywhere, and «العربية | English» on the installer's start screen (owner, build 179 follow-up):
a TV whose system language is English still starts in Arabic; the switch saves lang for the whole system like `saver.sh lang en`.
python3 tests/setup_lang_test.py"""
import re, shutil, subprocess, sys, tempfile
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


if shutil.which("javac"):
    tmp = tempfile.mkdtemp()
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "Tr.java"), str(ROOT / "tests/SetupLangRun.java")], capture_output=True, text=True)
    check("it compiles", r.returncode == 0, r.stderr)
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SetupLangRun"], capture_output=True, text=True, timeout=60).stdout
        for line in out.splitlines():
            if line.startswith(("PASS ", "FAIL ")):
                print(line)
                failed += line.startswith("FAIL ")
        check("every language case ran", "ALL PASS" in out, out[-200:])
else:
    print("SKIP the JVM cases: no javac")

# nothing reads the TV's own language to pick ours
code = [p for m in ("app", "launcher") for p in (ROOT / f"android/{m}/src/main/java").rglob("*.java")]
reads = [p.name for p in code if re.search(r"Locale\.getDefault\(|getConfiguration\(\)\.locale|getLocales\(\)|getLanguage\(\)", text(p))]
check("no source reads the TV's system language (Arabic is the default of the installer and of the system)", not reads, reads)
check("a fresh install: the kept language is Arabic and S reads «ar» when nothing was saved",
      'getString("lang", Tr.AR)' in text(HOME / "S.java") and 'sp.getString("lang", Tr.AR)' in text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java"))
check("no values-en folder: the TV's own language never picks English strings", not [p for m in ("app", "launcher") for p in (ROOT / f"android/{m}/src/main/res").glob("values-*")])
check("the installer run does not set a language (the person's choice, or the default)", "setLang" not in text(HOME / "setup/SetupEngine.java") and "saver.sh lang" not in text(HOME / "setup/SetupEngine.java"))

act = text(HOME / "setup/SetupActivity.java")
start = act[act.index("private void buildStart"):act.index("private View langToggle")]
tog = act[act.index("private View langToggle"):act.index("private void buildPlan")]
check("the switch is the first row of the start screen", start.index("add(langToggle(), dp(12));") < start.index("add(text(S.t(") and start.count("langToggle()") == 1)
check("«العربية» and «English», each in its own language, Arabic first", '{SetupLang.AR, "العربية"}, {SetupLang.EN, "English"}' in tog)
check("the one in use is the language now (Arabic until the person says else), marked as every on/off is (triangles, green words)",
      "code.equals(S.lang())" in tog and "ic_on" in tog and "OnOff.green" in tog)
check("laid out by Dir, 24 px apart, buttons 160 dp wide, as tall as their text needs plus 24 px above and below, the buttons' 14 radius (no pills)", "row.setLayoutDirection(Dir.layout());" in tog and "p.setMarginStart(dp(12))" in tog
      and "dp(160), -2" in tog and "b.setPadding(dp(16), dp(12), dp(16), dp(12));" in tog and "panel(this, look, 14)" in act and "setCornerRadius" not in tog)
check("both are focusable by the D-pad (the button style) and a press chooses", "SetupLang.choose(SetupActivity.this, code)" in tog and "button(opts[i][1])" in tog)
sl = text(HOME / "setup/SetupLang.java")
check("choosing keeps the language here, tells the screensaver (phone_set lang, as saver.sh lang does) and builds the screen again",
      "S.set(a, v);" in sl and "save(a, v);" in sl and "a.recreate();" in sl and '"phone_set"' in sl and 'extras.putString("lang", Tr.norm(lang))' in sl
      and sl.index("S.set(a, v);") < sl.index("save(a, v);") < sl.index("a.recreate();"))
check("choosing the language already in use does nothing", "if (v.equals(S.lang())) return;" in sl)
prov = text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/ConfigProvider.java")
rec = text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SaverReceiver.java")
check("the screensaver's side: phone_set takes lang and SaverReceiver.set saves it with Prefs.setLang (the same call as `saver.sh lang`)",
      '"idle", "lang", "favbar"}' in prov and "if (lang != null) p.setLang(lang);" in rec)
check("S.set keeps the choice for the next start, with only ar or en", "public static void set(Context c, String v)" in text(HOME / "S.java") and "lang = Tr.norm(v);" in text(HOME / "S.java"))
words = text(ROOT / "i18n/en/setup.tsv")
check("the labels are the same in both languages, so they need no table entry (never translated)", "العربية\t" not in words and "English\t" not in words)
check("build 180: after OK on the switch the screen is built again and the focus returns to the switch (not to «جهّز كل شي», where a second OK would start the run)",
      "if (!code.equals(S.lang())) refocusLang = code;" in tog and "static String refocusLang;" in act and "final TextView first = again != null ? again : start;" in act and "refocusLang = null;" in act)
sys.exit(1 if failed else 0)
