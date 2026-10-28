#!/usr/bin/env python3
"""The settings start small (the owner's ask): a first screen of categories (SettingsHub), each tile opening its rows. Every row of
SettingsActivity is found in the source and must be named in SettingsHub.ROWS (a new row with no category fails here), every header in
SettingsHub.HEADERS; the map has no stale names; the categories, the D-pad on the tiles (two to a row, from the right), Back to the
tile, and the wiring of render are checked from the source and, for SettingsHub and FocusKey, run for real (tests/SettingsHubRun.java).
Needs javac for the JVM part.   Run: python3 tests/settings_hub_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
hub = (APP / "SettingsHub.java").read_text(encoding="utf-8")
render = st[st.index("    private void render() {"):]
render = render[:render.index("\n    }\n")]

# ---- every row and header of the settings, in source order, with the header it sits under
calls, header = [], ""
for m in re.finditer(r'\bheader\("([^"]+)"\)|\brow\(("(?:[^"\\]|\\.)*"|HOME_CARD_NAMES\[k\]|PrayerAdj\.label\()', st):
    if m.group(1):
        header = m.group(1)
        calls.append(("header", header, ""))
    elif m.group(2) == "HOME_CARD_NAMES[k]":
        names = re.search(r'HOME_CARD_NAMES = \{([^}]*)\}', st).group(1)
        for n in re.findall(r'"([^"]+)"', names): calls.append(("row", n, header))
    elif m.group(2) == "PrayerAdj.label(":
        pass   # adjRow: the prayers' own rows, by their names and minutes; checked below with the header they are made under
    else:
        calls.append(("row", m.group(2)[1:-1], header))
rows = [c for c in calls if c[0] == "row"]
heads = [c for c in calls if c[0] == "header"]
check("the source has the settings' rows and headers (the test reads what it should)", len(rows) >= 45 and len(heads) >= 9, (len(rows), len(heads)))
check("no row is made outside render, adjRow and the home cards' loop (so the scan above sees them all)",
      len(re.findall(r"(?<!View )\brow\((?!\))", st)) == len(re.findall(r'\brow\(("|HOME_CARD_NAMES\[k\]|PrayerAdj\.label\()', st)), "a row( call with another first argument")

cat_names = dict(re.findall(r'\{(PLACE|ADHAN|SAVER|HOME|CONTENT|APPS|FAMILY|PHONE), "([^"]+)"\}', hub))
rows_block = hub[hub.index("static final String[][] ROWS"):hub.index("static final String[][] HEADERS")]
mapped = re.findall(r'\{"([^"]+)", (PLACE|ADHAN|SAVER|HOME|CONTENT|APPS|FAMILY|PHONE)\}', rows_block)

if not shutil.which("javac"):
    print("SKIP settings hub: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    feed = Path(tmp) / "feed.tsv"
    probe = [("row", "صف جديد بلا قسم", "التوقيت")]   # the test's own probe: a row nobody named must come out MISSING
    feed.write_text("\n".join("\t".join(c) for c in calls + probe) + "\n", encoding="utf-8")
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "SettingsHub.java"), str(APP / "FocusKey.java"), str(ROOT / "tests/SettingsHubRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.SettingsHubRun", str(feed)],
                         check=True, capture_output=True).stdout.decode("utf-8").split("\n")
lines = out[:len(calls) + 1]
rest = out[len(calls) + 1:]
row_out = [l.split("\t") for c, l in zip(calls + probe, lines) if c[0] == "row"]
head_out = [l.split("\t") for c, l in zip(calls, lines) if c[0] == "header"]
missing = [r[0] for r in row_out[:-1] if r[1] == "MISSING"]
check("every row of the settings has its category in SettingsHub.ROWS (a new row with none fails here)", not missing, missing)
check("…and the test itself catches a row nobody named", row_out[-1][1] == "MISSING", row_out[-1])
check("every header has its category in SettingsHub.HEADERS", all(h[1] != "MISSING" for h in head_out), [h for h in head_out if h[1] == "MISSING"])
found = {r[0] for r in row_out[:-1]}
check("SettingsHub.ROWS names no row that is gone (no stale names)", all(k in found for k, _ in mapped), [k for k, _ in mapped if k not in found])
check("each row is named once in SettingsHub.ROWS", len({k for k, _ in mapped}) == len(mapped), [k for k, _ in mapped])
by = {}
for r in row_out[:-1]: by.setdefault(r[1], []).append(r[0])

# ---- the categories
cats = [l.split("\t") for l in rest if l.startswith("category ")]
check("6 to 8 categories, each with a name, in order", 6 <= len(cats) <= 8 and [int(c[2]) for c in cats] == list(range(len(cats))) and all(c[1] for c in cats), cats)
check("every category has rows of its own (no empty tile)", all(by.get(c[0].split(" ")[1]) for c in cats), {c[0]: len(by.get(c[0].split(" ")[1], [])) for c in cats})
want = {"الأذان": "adhan", "تنبيه قبل الأذان": "adhan", "أسماء المصادر": "apps", "ترتيب التطبيقات المفضلة": "apps", "تعديل المفضلة": "apps",
        "تحديث تلفزيوني": "phone", "تحكم من الجوال": "phone", "حماية العائلة": "family", "الدولة": "place", "المدينة": "place",
        "طريقة الحساب": "place", "زر تلاوة وأذان": "home", "البطاقة الأولى": "home", "معاينة الشاشة": "saver", "بطاقة ثانية": "saver"}
got = {r[0]: r[1] for r in row_out[:-1]}
check("the adhan rows in «الأذان», the inputs and favourites in «التطبيقات والمفضلة», «تحديث تلفزيوني» in «الجوال والتحديث» (two steps)",
      all(got.get(k) == v for k, v in want.items()), {k: got.get(k) for k, v in want.items() if got.get(k) != v})
adj = st[st.index("private void adjRow(final int k)"):].split("\n    }\n")[0]
check("the prayers' own minute rows (adjRow) are made in «الموقع», so they go with its category («الموقع والصلاة»)",
      "row(PrayerAdj.label(" in adj and render.index('header("الموقع")') < render.index("adjRow(k)") < render.index('header("المباريات")')
      and 'if (on(SettingsHub.PLACE)) line(Location.HELP' in render)
check("a row not named goes with its header's category at run time (none is ever lost), else «شاشة التوقف»",
      row_out[-1][2] == "saver" and all(r[2] == r[1] for r in row_out[:-1]), [r for r in row_out[:-1] if r[2] != r[1]])

# ---- the D-pad on the tiles: two to a row from the right; nothing wraps
nxt = {tuple(map(int, re.match(r"next (\d+) (\d+) (\d+) = (\d+)", l).groups()[:3])): int(l.split(" = ")[1]) for l in rest if l.startswith("next ")}
n = len(cats)
ok = all(nxt[(i, n, 3)] == (i - 1 if i % 2 else i) and nxt[(i, n, 2)] == (i + 1 if i % 2 == 0 and i + 1 < n else i) for i in range(n))
check("RTL: on the tiles RIGHT goes to the right one of the pair (the first), LEFT to the left one; the edges stay", ok,
      {i: (nxt[(i, n, 2)], nxt[(i, n, 3)]) for i in range(n)})
check("UP and DOWN go a row up and down, the top and bottom rows stay", all(nxt[(i, n, 0)] == (i - 2 if i >= 2 else i) and nxt[(i, n, 1)] == (i + 2 if i + 2 < n else i) for i in range(n)))
check("an odd last tile, alone on its row: DOWN and LEFT stay on it", nxt[(6, 7, 1)] == 6 and nxt[(6, 7, 2)] == 6)
hubfn = st[st.index("    private void hub(String want, int wasIndex) {"):].split("\n    }\n")[0]
check("the tiles are wired with SettingsHub.next for all four keys, in rows laid out right to left",
      all(f"t.setNextFocus{d}Id(tiles.get(SettingsHub.next(i, tiles.size(), {k})).getId());" in hubfn for d, k in (("Up", 0), ("Down", 1), ("Left", 2), ("Right", 3)))
      and "line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);" in hubfn)
check("a tile has an icon for each category (HUB_ICONS in the order of SettingsHub.CATEGORIES)",
      len(re.findall(r"R\.drawable\.ic_\w+", st[st.index("HUB_ICONS = {"):st.index("};", st.index("HUB_ICONS = {"))])) == n)
summ = st[st.index("private String hubSummary(String cat)"):].split("\n    }\n")[0]
check("each tile says what is set in it now (a summary for every category)", all(f"case SettingsHub.{k.upper()}:" in summ for k in (c[0].split(" ")[1] for c in cats)))

# ---- the screens: Back to the tile, the focus kept on a row, nothing drawn outside its category
back = st[st.index("public void onBackPressed()"):].split("\n    }\n")[0]
check("Back on a category goes back to the categories with the focus on its tile; on the categories it leaves the settings",
      "if (screen == null) { super.onBackPressed(); return; }" in back and "hubAt = screen;" in back and "open(null);" in back)
check("each category's screen comes back on its row last focused (the first row the first time); the hub on the tile last opened",
      "lastRow.put(screen, (String) f.getTag());" in back and back.index("lastRow.put(") < back.index("open(null);")
      and ": lastRow.get(screen))" in render)
side = st[st.index("private void sidePane()"):].split("\n    }\n")[0]
check("a category's screen: its title and its line on the right (the side pane, first in the RTL panes), the rows on the left; no side on the first screen",
      "side.setVisibility(screen == null ? View.GONE : View.VISIBLE);" in side and "SettingsHub.name(screen)" in side and "SettingsHub.about(screen)" in side
      and st.index("panes.addView(side, sideLp);") < st.index("panes.addView(scroll,") and "panes.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);" in st
      and "if (screen == null) list.addView(head);" in render)
abouts = re.findall(r'\{(?:PLACE|ADHAN|SAVER|HOME|CONTENT|APPS|FAMILY|PHONE), "[^"]+", "([^"]*)"\}', hub)
check("every category has its line for its screen", len(abouts) == n and all(len(a) > 20 for a in abouts), abouts)
check("«الأرقام» (digits) is in a tile, «شاشة التوقف»", got.get("الأرقام") == "saver")
check("a tile opens its category from the top (OK on it)", "card.setOnClickListener(x -> open(cat));" in st and "entering = true;" in st[st.index("private void open(String category)"):].split("\n    }\n")[0])
check("the first screen is the categories, built before any row", render.index("if (screen == null) { hub(want, wasIndex); return; }") < render.index('row("معاينة الشاشة"'))
rowfn = st[st.index("private View row(String label, String value, int icon, final Runnable onClick)"):].split("\n    }\n")[0]
check("a row is added only on its own category's screen (made on every screen, so its code runs as before)",
      "final String cat = SettingsHub.of(FocusKey.key(label), groupCat);" in rowfn and "if (!on(cat)) return card;" in rowfn
      and rowfn.index("if (!on(cat)) return card;") < rowfn.index("list.addView(card, lp);"))
check("a header is drawn only on its category's screen, above the first of its rows there",
      "pendingHeader = on(groupCat) ? text : null;" in st and "if (pendingHeader != null && cat.equals(groupCat)) { drawHeader(pendingHeader); pendingHeader = null; }" in rowfn)
check("the version lines show under «تحديث الشاشة الآن» only where that row is", "if (last.getParent() == list) note(last, Updater.versionLines(this));" in render)
check("the tiles and the rows are found again by their tag after a rebuild (the focus stays on the tile, as on a row)",
      'card.setTag("hub:" + cat);' in st and "if (all.contains(v)) return v;" in st)
for fn_ in ("openAdhanSettings()", "openDockSettings()", "openInputNames()", "openDockEdit()", "openUpdate()", "openPhoneControl()", "openFamilyDns()", "pickCity()", "updateNow()"):
    check(f"the row's own action is kept: {fn_}", fn_ in render)

# ---- TV6 row 21: «تحكم من الجوال» must be findable (a tester on 3.145 did not find it and phone control stayed on)
cat_keys = {x[0].split(" ")[1] for x in cats}
check("every row of SettingsActivity is in a category that has a tile (nothing hides a row, whatever is installed)",
      all(c in cat_keys for c in got.values()), {k: c for k, c in got.items() if c not in cat_keys})
# every tile is reachable from the first one with the D-pad alone (SettingsHub.next), so the «الجوال والتحديث» tile is
seen, todo = {0}, [0]
while todo:
    i = todo.pop()
    for d in range(4):
        j = nxt.get((i, n, d), i)
        if j not in seen: seen.add(j); todo.append(j)
check("every category tile, «الجوال والتحديث» included, is reachable from the first tile with the D-pad", seen == set(range(n)), sorted(set(range(n)) - seen))
phone_idx = [i for i, c in enumerate(cats) if c[0].split(" ")[1] == "phone"]
check("the «الجوال والتحديث» tile exists once and holds «تحكم من الجوال»", len(phone_idx) == 1 and got.get("تحكم من الجوال") == "phone")
first_phone = [c[1] for c in calls if c[0] == "row" and got.get(c[1]) == "phone"]
check("«تحكم من الجوال» is the FIRST row of its category screen", first_phone and first_phone[0] == "تحكم من الجوال", first_phone)
check("its line does not claim it is off (this app cannot see the home's switch): the screen it opens shows the state",
      'row("تحكم من الجوال", "متوقف' not in st and 'row("تحكم من الجوال", "تشغيله وإيقافه' in st)
check("its tile's line names it, so the tile is the place people look", '"التحكم من الجوال · "' in summ)
check("opening it never fails silently: no home installed says so, and the target is exported in the home's manifest",
      "ثبّت الواجهة الرئيسية" in st[st.index("private void openPhoneControl()"):].split("\n    }\n")[0]
      and re.search(r'<activity\s+android:name="\.PhoneActivity"\s+android:exported="true"', (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")) is not None)
# ---- the TV test of 3.145: after going down and up again the header stayed hidden; once, two screens were drawn over each other
top = st[st.index("private void topWhenFirst(View v)"):].split("\n    }\n")[0]
check("the first row (on the categories, the first line of tiles) brings the page back to its very top, after the scroll view's own move",
      "if (at == 0 || (screen == null && at == 1)) scroll.post(() -> scroll.smoothScrollTo(0, 0));" in top
      and "if (has) topWhenFirst(v);" in rowfn and "card.setOnFocusChangeListener((x, has) -> { if (has) topWhenFirst(x); });" in st)
check("a screen just opened is at its top at once (never drawn at the old one's place first)",
      "scroll.scrollTo(0, 0);" in st[st.index("private void open(String category)"):].split("\n    }\n")[0])
check("the settings have no window animation in or out, so no two screens are ever drawn over each other",
      "getWindow().setWindowAnimations(0);" in st and "overridePendingTransition(0, 0);" in st[st.index("public void finish()"):].split("\n    }\n")[0])
dock = (ROOT / "android/launcher/src/main/java/sa/urtv/home/DockActivity.java").read_text(encoding="utf-8")
check("«ترتيب المفضلة»: the first app brings the list back to its top, the title over it",
      "View first = cards.isEmpty() ? reset : cards.get(0);" in dock and "if (has) scroll.post(() -> scroll.smoothScrollTo(0, 0));" in dock
      and "if (was != null) was.onFocusChange(v, has);" in dock)
# the TV test of 3.147: in «الموقع والصلاة», after 5 down and 5 up, the list moved from the left half to the right half and back
check("the settings' side pane and rows have fixed widths (no weights), worked out once; the rows' width changes only between the categories and a category",
      "new LinearLayout.LayoutParams(sideWidth, -1)" in st and "new LinearLayout.LayoutParams(width - sideWidth - dp(36), -1)" in st
      and "0.36f);" not in st and "0.64f));" not in st and "int rowsWidth = screen == null ? pageWidth : pageWidth - sideWidth - dp(36);" in st)
check("…and the scroll goes up and down only: nothing can move the page sideways", "@Override public void scrollTo(int x, int y) { super.scrollTo(0, y); }" in st
      and "scroll = new UpDownScroll(this);" in st)
sys.exit(1 if failed else 0)
