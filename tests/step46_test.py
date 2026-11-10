#!/usr/bin/env python3
"""The tester's step 46 findings (build 214, a real TCL), the ones that are pinned in the source: «تطبيقات مقترحة» inside the safe area on a 960 dp TV, «المظهر والخلفيات»'s
«رجوع» inside the scroll whose viewport is the safe area, the leagues picker (one dialog, a loading dialog with «إلغاء», the copy at once, short connect timeouts, «حفظ» reachable
from any row), and an empty day of the matches saying its own day. (The control center, the tray and the screensaver's picture have their pins in control_center_test and
home_snapshot_test; the page's idle cost in lowram_test.)"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def src(p): return (ROOT / p).read_text(encoding="utf-8")

sug = src("android/launcher/src/main/java/sa/urtv/home/SuggestedAppsActivity.java")
check("«تطبيقات مقترحة»: the column is 1000 dp at most and always inside 90 % of the screen's width (a 960 dp TV is narrower than 1000)",
      "Math.min(dp(1000), Math.round(getResources().getDisplayMetrics().widthPixels * 0.9f))" in sug and "new FrameLayout.LayoutParams(wide, -2, Gravity.CENTER_HORIZONTAL)" in sug and "LayoutParams(dp(1000)" not in sug)

app = src("android/app/src/main/java/sa/urtv/screensaver/AppearanceActivity.java")
check("«المظهر والخلفيات»: the scroll's viewport has the safe area as its margins and «رجوع» is the last row of what scrolls, so it is cut at 95 % of the height until the focus brings it in, never drawn out to the edge",
      "lp.topMargin = safe;" in app and "lp.bottomMargin = safe;" in app and "root.addView(scroll, lp);" in app and "col.addView(back, line(dp(8)));" in app and "scroll.addView(col, new FrameLayout.LayoutParams(-1, -2));" in app)

st = src("android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java")
check("leagues picker: one at a time (a second press does nothing while one is loading or open), the dialogs release it when they go",
      "private boolean catalogueBusy;" in st and "if (catalogueBusy) return;" in st and st.count("catalogueBusy = false") >= 4 and "catalogueBusy = nextList[0]" in st)
check("leagues picker: the copy of the last time opens at once and is refreshed quietly; with none, a loading dialog with «إلغاء» (not a toast); through Net: IPv4 first, 4 s per address, 12 s in all (the 54 s was 4 addresses x 12 s)",
      "new Thread(() -> fetchCatalogue(cache), \"catalogue-refresh\").start();" in st and 'setMessage("جارٍ تنزيل قائمة الدوريات…")' in st and "Net.get(CatalogueLeagues.URL, null, 15000, 12000, Bounded.JSON_MAX)" in st
      and 'toast("جارٍ التنزيل…");' not in st)
check("leagues picker: «حفظ» is reachable from any row of a long list (LEFT or RIGHT), as DOWN on the last row; Back still saves",
      "code == android.view.KeyEvent.KEYCODE_DPAD_LEFT || code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) { save.requestFocus(); return true; }" in st and "setOnCancelListener(d -> onSave.run())" in st)

page = src("src/template.html")
check("matches: an empty day says its own day (اليوم، غدًا، أمس, else plainly «لا مباريات»), not «لا مباريات اليوم» for all",
      "function fbNone(day)" in page and "FB_W.noneTomorrow" in page and "MATCHES_LOADED ? fbNone(MP.day) : matchesEmptyText()" in page and "noneDay:'لا مباريات'" in page)
sys.exit(1 if failed else 0)
