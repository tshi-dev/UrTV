#!/usr/bin/env python3
"""4.4 beta 2, the owner: «محاذاة التطبيقات», the apps row centred by default in every look, or at the screen's right or left; the clock never moves.
The setting, its command, the provider's column, the home's reading of it and the row's gravity.   python3 tests/apps_align_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP, HOME = ROOT / "android/app/src/main/java/sa/urtv/screensaver", ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
prefs, st, hub, recv, prov = (read(APP / f) for f in ("Prefs.java", "SettingsActivity.java", "SettingsHub.java", "SaverReceiver.java", "ConfigProvider.java"))
snap, main, fav = read(HOME / "ConfigSnapshot.java"), read(HOME / "MainActivity.java"), read(HOME / "FavBar.java")
check("three choices, «وسط» first and the default (a TV that updates keeps its centred row)",
      'APPS_ALIGNS = {"center", "right", "left"};' in prefs and 'APPS_ALIGN_NAMES = {"وسط", "يمين", "يسار"};' in prefs and 'sp.getString("appsAlign", "center")' in prefs)
check("a row of «الشكل» (one tree for every settings style, 4.4 beta 4), after «شكل التطبيقات»", 'row("محاذاة التطبيقات", Prefs.appsAlignLabel(prefs.appsAlign())' in st
      and hub.count('"محاذاة التطبيقات"') == 3 and '{"محاذاة التطبيقات", LOOK}' in hub and '"شكل التطبيقات", "محاذاة التطبيقات"' in hub)
check("the agent's command: saver.sh appsalign center|right|left; another word is refused", "appsalign) set1 appsalign" in read(ROOT / "optimize/saver.sh")
      and 'p.setAppsAlign(i.getStringExtra("appsalign"))' in recv and '"error: appsalign is one of center right left"' in recv)
check("the provider gives apps_align; the home reads it (an older screensaver says nothing: centre) and keeps it in every copy",
      '"apps_align"});' in prov and "p.appsAlign()});" in prov and 'cur.getColumnIndex("apps_align")' in snap and snap.count("appsAlign = appsAlign") >= 4
      and 'appsAlign = "center";' in snap)
check("the row's gravity: centre, the screen's right or its left (absolute: the same in Arabic and English), set when the value changes",
      "alignApps(cfg.appsAlign);" in main and "dockRow.setGravity(FavBar.alignGravity(align));" in main
      and "if (\"right\".equals(align)) return 0x05 | 0x10;" in fav and "if (\"left\".equals(align)) return 0x03 | 0x10;" in fav and "return 0x11;" in fav)
check("the clock is not touched by it", "clock" not in main[main.index("    private void alignApps(String align) {"):].split("\n    }\n")[0].lower())
check("docs: SETTINGS.md and FEATURES.md have the row and its command", "`optimize/saver.sh appsalign center`" in read(ROOT / "docs/SETTINGS.md")
      and "`optimize/saver.sh appsalign center`" in read(ROOT / "docs/FEATURES.md"))
sys.exit(1 if failed else 0)
