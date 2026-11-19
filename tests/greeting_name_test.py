#!/usr/bin/env python3
"""«اسمك في التحية» (based on PR #45 by AlaaAlmasri): a name after the home's greeting. Off by default (no name = the greeting as it was). The
wiring: the setting (Prefs), the provider's column, the home's snapshot, Greeting (both branches), the settings row, the agent's command and the docs.
Run: python3 tests/greeting_name_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
t = lambda p: (ROOT / p).read_text(encoding="utf-8")
failed = 0
def check(name, ok):
    global failed
    print(("PASS " if ok else "FAIL ") + name)
    failed += not ok

app = "android/app/src/main/java/sa/urtv/screensaver/"
home = "android/launcher/src/main/java/sa/urtv/home/"
prefs, prov, rcv = t(app + "Prefs.java"), t(app + "ConfigProvider.java"), t(app + "SaverReceiver.java")
snap, greet, gt = t(home + "ConfigSnapshot.java"), t(home + "Greeting.java"), t(home + "GreetingText.java")
check("the name is empty by default and kept clean: folded spaces, no control or bidi characters, at most 30",
      'sp.getString("greeting_name", "")' in prefs and "NAME_MAX = 30" in prefs and "static String cleanName(" in prefs and "n.substring(0, NAME_MAX)" in prefs)
check("the provider gives it as «greeting_name» and the home reads it (none for an older screensaver)",
      '"greeting_name"' in prov and "p.greetingName()" in prov and 'getColumnIndex("greeting_name")' in snap and 'String greetingName = "";' in snap)
check("every snapshot copy keeps it (withFragment, asStale, and 4.5 withLook)", snap.count("c.greetingName = greetingName;") == 3)
check("the greeting gets the name in both branches (weather on and off), through GreetingText.withName",
      greet.count("GreetingText.withName(") == 2 and greet.count("snap.greetingName") == 2 and "static String withName(" in gt and "\\u2068" in gt)
check("the settings row opens a dialog, and «بدون اسم» clears it", 'row("اسمك في التحية"' in t(app + "SettingsActivity.java") and "private void askGreetingName()" in t(app + "SettingsActivity.java"))
check("the agent: saver.sh greetingname NAME|off, the receiver refuses an empty or too long name, show says it",
      "greetingname)" in t("optimize/saver.sh") and 'getStringExtra("greetingname")' in rcv and '"\\ngreetingname: "' in rcv and "Prefs.NAME_MAX" in rcv)
check("documented: SETTINGS.md and the CHANGELOG thank the PR", "| اسمك في التحية |" in t("docs/SETTINGS.md") and "AlaaAlmasri (#45)" in t("CHANGELOG.md"))
check("tests/run.sh runs this test", "tests/greeting_name_test.py" in t("tests/run.sh"))
print("\n" + (f"{failed} failed" if failed else "greeting name: all passed"))
sys.exit(1 if failed else 0)
