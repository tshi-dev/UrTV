#!/usr/bin/env python3
"""4.4 (the owner, R-470): a TV set up from now on opens the settings in the side list («كنبة»); an update never switches an installed TV.
Prefs.settingsStyle fixes the style the first time it is read: «كنبة» only on a fresh install (FreshInstall: never updated over an older
version), and BootReceiver reads it at the first boot (BootFirst) so a TV whose app updated itself before the settings were opened still counts as new.
Run: python3 tests/side_new_setups_test.py"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


prefs = (APP / "Prefs.java").read_text(encoding="utf-8")
fresh = (APP / "FreshInstall.java").read_text(encoding="utf-8")
boot = (APP / "BootReceiver.java").read_text(encoding="utf-8")
first = (APP / "BootFirst.java").read_text(encoding="utf-8")
idle = (APP / "IdleKeep.java").read_text(encoding="utf-8")
style = prefs.split("String settingsStyle() {", 1)[1].split("\n    }\n", 1)[0]
check("the owner's switch is on: new setups get the side list", "static final boolean SIDE_FOR_NEW_SETUPS = true;" in prefs)
check("only when nothing is stored yet, and only for a fresh install (an update keeps «أزرار» unless it showed «كنبة»)",
      'String v = sp.getString("settingsStyle", null);' in style and "if (v == null) {" in style
      and "SIDE_FOR_NEW_SETUPS && FreshInstall.is(ctx)" in style and '? "couch" : "buttons"' in style and 'putString("settingsStyle", v)' in style)
check("a fresh install is one never updated (first install time = last update time); unknown counts as not fresh",
      "return i.firstInstallTime == i.lastUpdateTime;" in fresh and re.search(r"catch \(Exception e\) \{\s*return false;", fresh) is not None)
check("the person's own choice is kept as theirs", 'void setSettingsStyle(String v)' in prefs and '.putString("settingsStyle", v)' in prefs)
check("the style is fixed at a boot (never at an update: MY_PACKAGE_REPLACED is not a boot action), on a thread",
      "p.settingsStyle();" in first and "BootFirst.atBoot(app);" in boot and boot.index("IdleKeep.bootAction(i.getAction())") < boot.index("BootFirst.atBoot(app);")
      and "new Thread(" in boot and "MY_PACKAGE_REPLACED" not in idle.split("static boolean bootAction(", 1)[1].split("}", 1)[0])
sys.exit(1 if failed else 0)
