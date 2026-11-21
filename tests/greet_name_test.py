"""«اسمك في التحية» (greetname): a name after the home's greeting, from the command and the TV's settings to the home.
What the greeting then says: tests/greeting_test.py (GreetingText.withName on tests/greeting_cases.tsv).
Run: python3 tests/greet_name_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    if not ok:
        failed += 1


def text(p):
    return Path(p).read_text(encoding="utf-8")


prefs, rcv, prov, st = text(SAVER / "Prefs.java"), text(SAVER / "SaverReceiver.java"), text(SAVER / "ConfigProvider.java"), text(SAVER / "SettingsActivity.java")
check("Prefs: none by default, one line, at most 20 characters, the same limit as the home's",
      'sp.getString("greetName", "")' in prefs and "static final int GREET_NAME_MAX = 20;" in prefs
      and "static final int NAME_MAX = 20;" in text(HOME / "GreetingText.java") and "if (greetNameOk(n)) sp.edit().putString(\"greetName\", n)" in prefs)
check("saver: a name too long is refused before anything is set; then set, and `show` says it",
      'if (greetName != null && !Prefs.greetNameOk(greetName)) return "error: greetname is at most "' in rcv
      and "if (greetName != null) p.setGreetName(greetName);" in rcv and '"\\ngreetname: " + p.greetName()' in rcv
      and rcv.index("error: greetname is at most") < rcv.index("if (idle != null) {   // first"))
check("the provider gives the home greet_name, last of the columns (an older home reads the others as before)",
      '"city_chosen", "greet_name"});' in prov and 'p.cityChosen() ? "1" : "0", p.greetName()});' in prov)
snap, greet = text(HOME / "ConfigSnapshot.java"), text(HOME / "Greeting.java")
check("the home reads it (an older screensaver says nothing: no name) and keeps it in the old copies too",
      'String greetName = "";' in snap and 'cur.getColumnIndex("greet_name")' in snap and "made.greetName = greetName;" in snap
      and snap.count("c.greetName = greetName;") == 2)
check("both greetings take the name: with «التحية والطقس» off (the plain one) and on (by the prayer times)",
      greet.count("GreetingText.withName(") == 2 and greet.count(", snap.greetName)") == 2)
check("settings: «اسمك في التحية» under «التحية والطقس», typed on the TV, the field stops at the limit",
      st.index('row("التحية والطقس"') < st.index('row("اسمك في التحية"') and "private void askGreetName()" in st
      and "new android.text.InputFilter.LengthFilter(Prefs.GREET_NAME_MAX)" in st and "prefs.setGreetName(in.getText().toString()); render();" in st)
words, hub, tsv = text(SAVER / "Words.java"), text(SAVER / "SettingsHub.java"), text(ROOT / "i18n/en/saver.tsv")
check("English: the row's words have their entries, and the row has its place in the settings' map",
      all(f'S.n("{w}")' in words and f"\n{w}\t" in tsv for w in ("اسمك في التحية", "بدون · «مساء الخير» وحدها", "اسمك في التحية (فاضي = بدون)", "مثال: أبو محمد"))
      and '{"اسمك في التحية", HOME}' in hub)
sh = text(ROOT / "optimize/saver.sh")
check("saver.sh: greetname NAME ('' = none), in the usage", "  greetname) [ $# -ge 2 ]" in sh and 'set1 greetname "$2"' in sh
      and "optimize/saver.sh greetname " in sh.split("set -euo")[0])
check("the export never carries it: the person's own", "greetname" not in text(ROOT / "optimize/settings_move.py") and "greet_name" not in text(ROOT / "optimize/settings_move.py"))
check("docs/SETTINGS.md says the command and where it is on the TV", "`optimize/saver.sh greetname 'أبو محمد'`" in text(ROOT / "docs/SETTINGS.md"))
sys.exit(1 if failed else 0)
