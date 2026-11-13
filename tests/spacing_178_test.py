#!/usr/bin/env python3
"""Alignment of build 178 (tester step 22): the numbers pinned. The tester's TV runs 1080p at density 2, so 1 dp = 2 px: a gap is on the 8 px scale
when its dp value is a multiple of 4 (16 px = 8 dp, 24 px = 12 dp), and nothing may end below the safe area (y 1026 of 1080: 54 px from the bottom, 5 %).
Also: the matches and markets pages can be reached by keys.   python3 tests/spacing_178_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
RES = ROOT / "android/launcher/src/main/res"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


settings = text(APP / "SettingsActivity.java")
# footer hints: inside y <= 1026 (5 % of the screen's height left under the box; the text is never lower than its box)
check("settings pages: the footer hint box ends 5 % above the screen's bottom (glyphs were at y 1037)",
      "hintLp.bottomMargin = Math.round(getResources().getDisplayMetrics().heightPixels * 0.05f);" in settings and "column.addView(hint, hintLp);" in settings)
for f in ("activity_main", "activity_adhan", "activity_player", "activity_surah"):
    x = text(RES / "layout" / (f + ".xml"))
    m = re.search(r'android:paddingBottom="(\d+)dp">', x)
    check(f"Tilawah {f}: the footer hint ends at y <= 1026 (bottom padding >= 27 dp = 54 px at 1080p)", m is not None and int(m.group(1)) >= 27, m and m.group(1))
for f in ("activity_main", "activity_adhan"):
    x = text(RES / "layout" / (f + ".xml"))
    m = re.search(r'android:dividerHeight="(\d+)dp"', x)
    check(f"Tilawah {f}: the rows are 16 px apart (8 dp)", m is not None and m.group(1) == "8", m and m.group(1))

# gaps on the 8 px scale (dp multiples of 4)
fav = text(HOME / "FavBar.java")
check("favourites tray: tiles 24 px apart (FavBar.GAP)", re.search(r"GAP = 24,", fav) is not None)
main = text(HOME / "MainActivity.java")
cell = main[main.index("private LinearLayout.LayoutParams rowCell("):main.index("After «إضافة إلى المفضلة»")]
check("home rows of cards: 6 + 6 dp = 24 px apart, like the tray", "dp(6)" in cell and "dp(10)" not in cell)
check("dockCell keeps FavBar.GAP in two halves (24 px)", "p.setMarginStart(i == 0 ? 0 : bar(FavBar.GAP) / 2);" in main
      and "p.setMarginEnd(i == n - 1 ? 0 : bar(FavBar.GAP) - bar(FavBar.GAP) / 2);" in main)
cc = text(HOME / "ControlCenter.java")
check("Control Center: the side panel inside the 5 % safe area (x from 96, y 54 to 1026 at 1080p), 560 px wide; every margin and size a multiple of 4 dp",
      "SAFE = 0.05f" in cc and "PANEL_W = 560f / 1920f" in cc and "panelH = H - 2 * top" in cc and "Math.round(H * SAFE)" in cc
      and all(int(n) % 4 == 0 for n in re.findall(r"dp\((\d+)\)", cc) if int(n) not in (1, 2, 3, 6, 24, 16, 8, 12)) and "dp(10)" not in cc and "dp(20)" not in cc)
pin = text(HOME / "PinPrompt.java")
gaps = [int(n) for n in re.findall(r"(?:topMargin|leftMargin|bottomMargin) = dp\(a, (\d+)\)", pin)]
check("PIN pad: every margin is a multiple of 4 dp (keys, rows, «إلغاء» 24 px apart)", gaps and all(n % 4 == 0 for n in gaps) and gaps.count(12) >= 5, gaps)
check("PIN pad: no 20 dp or 10 dp gap left", not re.search(r"(?:topMargin|leftMargin|bottomMargin) = dp\(a, (?:10|20)\)", pin))
par = text(HOME / "ParentalActivity.java")
check("parental «off» card: its buttons 24 px apart", "p.setMarginStart(dp(12));" in par and "p.setMarginStart(dp(10));" not in par)
check("settings list: rows 16 px apart (8 dp)", "lp.bottomMargin = dp(8);\n        if (!on(cat)) return card;" in settings)
setup = text(HOME / "setup/SetupActivity.java")
sg = {int(n) for n in re.findall(r"(?:add\([^\n]*,\s|right\()dp\((\d+)\)\)", setup)}
check("setup screens: every gap is 8, 16, 24 or 32 px (4 to 16 dp, multiples of 4 dp)", sg and all(n % 4 == 0 for n in sg), sorted(sg))
phone = text(HOME / "PhoneActivity.java")
m = re.search(r"col\.setPadding\(0, dp\((\d+)\), 0, dp\(\d+\)\);", phone)
check("«تحكم من الجوال»: the title starts at y >= 54 (28 dp = 56 px)", m is not None and int(m.group(1)) >= 27, m and m.group(1))
check("the phone's allow/deny buttons: 24 px apart", "p.setMarginEnd(dp(12));" in phone)

# the month line is shown whole
prefs = text(APP / "Prefs.java")
line = re.search(r"static String monthsLabel\(String v\) \{(.*?)\}", prefs).group(1)
check("the month line has no «…» (a sample of three months, shown whole)", "…" not in line and "يناير، فبراير، مارس" in line and "كانون الثاني، شباط، آذار" in line, line)
tsv = text(ROOT / "i18n/en/saver.tsv")
check("…and neither has its English", "Standard (January, February, March)" in tsv and "Levantine (Kanun al-Thani, Shubat, Adhar)" in tsv and "February…" not in tsv)

# the matches and markets pages by keys (docs/SETTINGS.md has the path)
hub = text(APP / "SettingsHub.java")
cats = re.findall(r'^\s*\{([A-Z]+), "', hub[hub.index("CATEGORIES = {"):], re.M)
check("hub order: CITY wide (4.3.1), then two to a row from ADHAN; «المزيد» is the 11th tile after it", cats[:12] == ["CITY", "ADHAN", "PLACE", "CLOCK", "LISTEN", "CARDS", "LOOK", "CONTROL", "PHONE", "UPDATE", "REPORT", "MORE"], cats[:11])
check("«المباريات والأسواق» is the first button of «المزيد»", [c for c in cats if c in ("CONTENT", "APPS", "SHOWS", "PARENTAL", "FAMILY", "LANG")][0] == "CONTENT")
rows = re.findall(r'^\s*row\("([^"]+)"', settings[settings.index("private void render()"):], re.M)
content = ["مباريات اليوم", "الفرق المتابعة (", "الدوريات كاملة", "رياضات أخرى", "صفحة الأسواق"]
order = [r for r in rows if any(r.startswith(c) for c in content)]
check("«مباريات اليوم» is the first row of «المباريات والأسواق» and «صفحة الأسواق» the fifth (four ▼ from the first)", [r.split(" (")[0] for r in order][:5] == ["مباريات اليوم", "الفرق المتابعة", "الدوريات كاملة", "رياضات أخرى", "صفحة الأسواق"], order)
check("both rows open the launcher's pages, and those are exported", 'setClassName("sa.urtv.home", "sa.urtv.home.MatchesActivity")' in settings and 'setClassName("sa.urtv.home", "sa.urtv.home.MarketsActivity")' in settings
      and re.search(r'android:name="\.MatchesActivity"\s+android:exported="true"', text(ROOT / "android/launcher/src/main/AndroidManifest.xml")) is not None
      and re.search(r'android:name="\.MarketsActivity"\s+android:exported="true"', text(ROOT / "android/launcher/src/main/AndroidManifest.xml")) is not None)
check("from the home: OK on the stocks card opens the markets page, OK on the games widget the matches page",
      "launch(new Intent(MainActivity.this, MarketsActivity.class));" in main and "new Intent(MainActivity.this, MatchesActivity.class)" in main)
doc = text(ROOT / "docs/SETTINGS.md")
check("docs/SETTINGS.md has the key path", "المباريات والأسواق" in doc and "▼ ×5" in doc and "▼ ×4" in doc)
sys.exit(1 if failed else 0)
