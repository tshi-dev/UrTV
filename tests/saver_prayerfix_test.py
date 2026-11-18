#!/usr/bin/env python3
"""4.4 review F2: the prayer correction read by the first background load of a process reaches a dream that was started without it: when the thread of
prayerFixKnown finds another copy and no screen asked to redraw (changed == null) it sends the settings-changed broadcast; the dream rebuilds only when its address differs,
and a repeat read finds the same copy (ContentFile.Kept loads again only when the file moved), so no loop."""
import re, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
prefs = (APP / "Prefs.java").read_text(encoding="utf-8")
k = prefs.split("int[] prayerFixKnown(", 1)[1].split("java.text.SimpleDateFormat", 1)[0]
check("F2: a changed copy with no redraw callback sends Prefs.CHANGED to our own package",
      re.search(r"if \(PRAYER_FIX\.value\(\) != before\) \{\s*if \(changed != null\)[^\n]*\n[^\n]*\n\s*else app\.sendBroadcast\(new android\.content\.Intent\(CHANGED\)\.setPackage\(app\.getPackageName\(\)\)\);", k) is not None, k)
check("F2: only when the copy differs from the one read before the load", "PRAYER_FIX.value() != before" in k and "Object before = PRAYER_FIX.value();" in k)
check("F2: the broadcast is sent from the background thread, not the UI thread", k.index("new Thread(") < k.index("sendBroadcast") < k.index('"prayer-fix"'))
load = (APP / "ContentFile.java").read_text(encoding="utf-8").split("synchronized void load(", 1)[1].split("}\n    }", 1)[0]
check("F2: no loop: Kept.load keeps the value when the file's time and size did not move", "if (t == at && n == size) return;" in load)
check("F2: the receivers (dream, preview) rebuild only when the address differs",
      "screenUrl(c, false).equals(shownUrl)) return;" in (APP / "ScreenDream.java").read_text(encoding="utf-8")
      and "screenUrl(this, false).equals(shownUrl)) return;" in (APP / "MainActivity.java").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
