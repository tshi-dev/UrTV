#!/usr/bin/env python3
"""The settings start small (the owner's ask, notes/settings-proto v2): a first screen of buttons (SettingsHub), «الأذان» wide on top; a button opens
its rows, a screen of more buttons («البطاقات», «المزيد»), or does one thing at once (DIRECT). Every row of
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

cat_names = dict(re.findall(r'\{(ADHAN|PLACE|CLOCK|LISTEN|CARDS|LOOK|CONTROL|PHONE|UPDATE|REPORT|MORE|SAVER|HOME|CONTENT|APPS|SHOWS|FAMILY|LANG), "([^"]+)"', hub))
rows_block = hub[hub.index("static final String[][] ROWS"):hub.index("static final String[][] HEADERS")]
mapped = re.findall(r'\{"([^"]+)", (ADHAN|PLACE|CLOCK|LISTEN|CARDS|LOOK|CONTROL|PHONE|UPDATE|REPORT|MORE|SAVER|HOME|CONTENT|APPS|SHOWS|FAMILY|LANG)\}', rows_block)

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

# ---- the categories (notes/settings-proto, v2 as the owner approved it): the first screen's buttons, «البطاقات» and «المزيد»
cats = [l.split("\t") for l in rest if l.startswith("category ")]
C = {c[0].split(" ")[1]: {"name": c[1], "index": int(c[2]), "parent": c[3], "direct": c[4], "tiles": c[5] == "true"} for c in cats}
first = sorted([k for k, v in C.items() if v["parent"] == "-"], key=lambda k: C[k]["index"])
check("the first screen: «الأذان» first (the wide one), then the buttons of the prototype in its order",
      [C[k]["name"] for k in first] == ["الأذان", "الموقع والصلاة", "الساعة والأرقام", "تلاوة وأذان", "البطاقات", "المظهر", "مركز التحكم",
                                       "التحكم من الجوال", "تحديث تلفزيوني", "أرسل تقرير", "المزيد"], [C[k]["name"] for k in first])
check("«البطاقات» holds «شاشة التوقف» and «الشاشة الرئيسية»; «المزيد» the matches and markets, the apps, the shows, «حماية العائلة» (and «اللغة» once LANG_TILE is on)",
      sorted((C[k]["index"], C[k]["name"]) for k in C if C[k]["parent"] == "cards") == [(0, "شاشة التوقف"), (1, "الشاشة الرئيسية")]
      and [C[k]["name"] for k in sorted((k for k in C if C[k]["parent"] == "more" and C[k]["index"] >= 0), key=lambda k: C[k]["index"])]
      == ["المباريات والأسواق", "التطبيقات والمفضلة", "جديد المسلسلات", "حماية العائلة"] + (["اللغة"] if "static final boolean LANG_TILE = true;" in hub else []),
      {k: (v["parent"], v["index"]) for k, v in C.items()})
check("only the first screen, «البطاقات» and «المزيد» are screens of buttons, and nothing is deeper than that",
      {k for k, v in C.items() if v["tiles"]} == {"cards", "more"} and all(C[v["parent"]]["parent"] == "-" for v in C.values() if v["parent"] != "-"))
direct = {k: v["direct"] for k, v in C.items() if v["direct"] != "-"}
check("the buttons that do one thing at once, each by its one row: «الأذان», «المظهر», «مركز التحكم», «التحكم من الجوال», «أرسل تقرير», «حماية العائلة», «اللغة»",
      direct == {"adhan": "الأذان", "look": "المظهر", "control": "مركز التحكم", "phone": "تحكم من الجوال", "report": "أرسل تقرير", "family": "حماية العائلة",
                 "lang": "اللغة"}
      and all(by.get(k) == [r] for k, r in direct.items()), {k: (r, by.get(k)) for k, r in direct.items()})
check("every other category has rows of its own (no empty screen), a screen of buttons none",
      all(by.get(k) for k, v in C.items() if not v["tiles"] and k not in direct) and not any(by.get(k) for k, v in C.items() if v["tiles"]),
      {k: len(by.get(k, [])) for k in C})
want = {"الأذان": "adhan", "تنبيه قبل الأذان": "saver", "أسماء المصادر": "apps", "ترتيب التطبيقات المفضلة": "apps", "تعديل المفضلة": "apps",
        "تحديث تلفزيوني": "update", "تحديث صفحة شاشة التوقف": "update", "التحديث التلقائي للصفحة": "update", "تحكم من الجوال": "phone",
        "حماية العائلة": "family", "الدولة": "place", "المدينة": "place", "طريقة الحساب": "place", "زر تلاوة وأذان": "listen",
        "افتح تلاوة وأذان": "listen", "البطاقة الأولى": "home", "معاينة الشاشة": "saver", "بطاقة ثانية": "saver", "نظام الساعة": "clock",
        "الأرقام": "clock", "أسماء الأشهر الميلادية": "clock", "حجم الساعة": "saver", "المظهر": "look", "التصنيفات": "shows", "قائمة الأسهم": "content"}
got = {r[0]: r[1] for r in row_out[:-1]}
check("each row in its place: the clock and digits in «الساعة والأرقام», the updates together, the button's switch in «تلاوة وأذان»…",
      all(got.get(k) == v for k, v in want.items()), {k: got.get(k) for k, v in want.items() if got.get(k) != v})
adj = st[st.index("private void adjRow(final int k)"):].split("\n    }\n")[0]
check("the prayers' own minute rows (adjRow) are made in «الموقع», so they go with its category («الموقع والصلاة»)",
      "row(PrayerAdj.label(" in adj and render.index('header("الموقع")') < render.index("adjRow(k)") < render.index('header("المباريات")')
      and 'if (on(SettingsHub.PLACE)) line(Location.HELP' in render)
check("a row not named goes with its header's category at run time (none is ever lost), else «شاشة التوقف»",
      row_out[-1][2] == "saver" and all(r[2] == r[1] for r in row_out[:-1]), [r for r in row_out[:-1] if r[2] != r[1]])

# ---- the D-pad on a screen of buttons: two to a row from the right, the first screen's first one wide; nothing wraps
scr = {l.split(" ")[1]: (int(l.split(" ")[2]), l.split(" ")[3] == "true") for l in rest if l.startswith("screen ")}
nxt = {(m.group(1), int(m.group(2)), int(m.group(3)), int(m.group(4))): int(m.group(5))
       for m in (re.match(r"next (\S+) (\d+) (\d+) (\d+) = (\d+)", l) for l in rest if l.startswith("next "))}
for name, (n, wide) in scr.items():
    off = 1 if wide else 0   # the pairs start after the wide one
    pair = lambda i: (i - off) // 2
    col = lambda i: (i - off) % 2
    ok = all(nxt[(name, i, n, 3)] == (i - 1 if col(i) == 1 else i) and nxt[(name, i, n, 2)] == (i + 1 if col(i) == 0 and i + 1 < n else i)
             for i in range(off, n))
    check(f"RTL on «{name}»: RIGHT goes to the right one of the pair (the first), LEFT to the left one; the edges stay", ok,
          {i: (nxt[(name, i, n, 2)], nxt[(name, i, n, 3)]) for i in range(n)})
    ok = all(nxt[(name, i, n, 0)] == (i - 2 if pair(i) >= 1 else (0 if wide else i)) and nxt[(name, i, n, 1)] == (i + 2 if i + 2 < n else i) for i in range(off, n))
    check(f"UP and DOWN on «{name}» go a row up and down; the top row stays (or, under the wide one, goes up to it), the bottom row stays", ok,
          {i: (nxt[(name, i, n, 0)], nxt[(name, i, n, 1)]) for i in range(n)})
    seen, todo = {0}, [0]
    while todo:
        i = todo.pop()
        for d in range(4):
            j = nxt.get((name, i, n, d), i)
            if j not in seen: seen.add(j); todo.append(j)
    check(f"every button of «{name}» is reachable from the first with the D-pad", seen == set(range(n)), sorted(set(range(n)) - seen))
n0, w0 = scr["-"]
check("the wide «الأذان»: DOWN goes to the right one under it, the other keys stay on it", w0 and nxt[("-", 0, n0, 1)] == 1
      and all(nxt[("-", 0, n0, d)] == 0 for d in (0, 2, 3)))
check("an odd last button, alone on its row: DOWN and LEFT stay on it", nxt[("-", 6, 7, 1)] == 6 and nxt[("-", 6, 7, 2)] == 6)
hubfn = st[st.index("    private void hub(String want, int wasIndex) {"):].split("\n    }\n")[0]
check("the buttons are wired with SettingsHub.next for all four keys (the wide one known), in rows laid out right to left (Dir: left to right in English, the sides swapped)",
      all(f"t.setNextFocus{d}Id(tiles.get(SettingsHub.next(i, tiles.size(), {k}, wide)).getId());" in hubfn for d, k in (("Up", 0), ("Down", 1), ("Left", "left"), ("Right", "right")))
      and "final int left = Dir.rtl() ? 2 : 3, right = Dir.rtl() ? 3 : 2;" in hubfn
      and "line.setLayoutDirection(Dir.layout());" in hubfn and "final boolean wide = SettingsHub.wide(screen);" in hubfn
      and "boolean alone = wide && i == 0;" in hubfn)
icons = st[st.index("private static int hubIcon(String cat)"):].split("\n    }\n")[0]
summ = st[st.index("private String hubSummary(String cat)"):].split("\n    }\n")[0]
check("every button has its icon and says what is set in it now (a case for every category)",
      all(f"case SettingsHub.{k.upper()}:" in icons and f"case SettingsHub.{k.upper()}:" in summ for k in C), [k for k in C if f"case SettingsHub.{k.upper()}:" not in summ])
check("«الأذان» shows the home's switch, read from the home (NowPlayingProvider .../adhan_on), and says nothing of it when the home cannot say",
      "Boolean on = homeAdhanOn();" in summ and 'content://sa.urtv.home.nowplaying/adhan_on' in st and "OnOff.ON_WORD : OnOff.OFF_WORD" in summ)
tilefn = st[st.index("private View tile(final String cat, String name, int icon, String summary)"):].split("\n    }\n")[0]
check("OK on a button: a DIRECT one runs its row's own action, any other opens its screen (and is the one focused on the way back)",
      "String row = SettingsHub.direct(cat);" in tilefn and "Runnable act = row == null ? null : actions.get(row);" in tilefn
      and "if (act != null) { act.run(); return; }" in tilefn and "hubAt = cat;\n            open(cat);" in tilefn)
check("the screens of buttons are built after the rows (so each row's action is known), and those rows are never added there",
      render.index("actions.clear();") < render.index('row("معاينة الشاشة"') < render.index("if (tiles) { hub(want, wasIndex); return; }")
      and "if (onClick != null) actions.put(FocusKey.key(label), onClick);" in st)

# ---- the screens: Back to the tile, the focus kept on a row, nothing drawn outside its category
back = st[st.index("public void onBackPressed()"):].split("\n    }\n")[0]
check("Back on a category goes back to the screen its button is on, the focus on that button; on the first screen it leaves the settings",
      "if (screen == null) { super.onBackPressed(); return; }" in back and "hubAt = screen;" in back and "open(SettingsHub.parent(screen));" in back)
check("each category's screen comes back on its row last focused (the first row the first time); the hub on the tile last opened",
      "lastRow.put(screen, (String) f.getTag());" in back and back.index("lastRow.put(") < back.index("open(SettingsHub.parent(screen));")
      and ": lastRow.get(screen))" in render)
side = st[st.index("private void sidePane()"):].split("\n    }\n")[0]
check("a category's screen: its title and its line on the right (the side pane, first in the RTL panes), the rows on the left; no side on the first screen",
      "side.setVisibility(tiles ? View.GONE : View.VISIBLE);" in side and "SettingsHub.name(screen)" in side and "SettingsHub.about(screen)" in side
      and st.index("panes.addView(side, sideLp);") < st.index("panes.addView(scroll,") and "panes.setLayoutDirection(Dir.layout());" in st
      and "if (tiles) list.addView(head);" in render)
abouts = dict(re.findall(r'\{(ADHAN|PLACE|CLOCK|LISTEN|CARDS|LOOK|CONTROL|PHONE|UPDATE|REPORT|MORE|SAVER|HOME|CONTENT|APPS|SHOWS|FAMILY|LANG), "[^"]+", "([^"]*)"', hub))
check("every category with a screen of its own has its line for it (a DIRECT one has none)",
      all(len(abouts[k.upper()]) > 20 for k in C if k not in direct) and all(abouts[k.upper()] == "" for k in direct), abouts)
check("a button opens its category from the top", "entering = true;" in st[st.index("private void open(String category)"):].split("\n    }\n")[0])
rowfn = st[st.index("private View row(String label, String value, int icon, final Runnable onClick)"):].split("\n    }\n")[0]
check("a row is added only on its own category's screen (made on every screen, so its code runs as before)",
      "final String cat = SettingsHub.of(FocusKey.key(label), groupCat);" in rowfn and "if (!on(cat)) return card;" in rowfn
      and rowfn.index("if (!on(cat)) return card;") < rowfn.index("list.addView(card, lp);"))
check("a header is drawn only on its category's screen, above the first of its rows there",
      "pendingHeader = on(groupCat) ? text : null;" in st and "if (pendingHeader != null && cat.equals(groupCat)) { drawHeader(pendingHeader); pendingHeader = null; }" in rowfn)
check("the version lines show under «تحديث صفحة شاشة التوقف» only where that row is", "if (last.getParent() == list) note(last, Updater.versionLines(this));" in render)
check("the tiles and the rows are found again by their tag after a rebuild (the focus stays on the tile, as on a row)",
      'card.setTag("hub:" + cat);' in st and "if (all.contains(v)) return v;" in st)
for fn_ in ("openAdhanSettings()", "openDockSettings()", "openInputNames()", "openDockEdit()", "openUpdate()", "openPhoneControl()", "openFamilyDns()",
            "pickCity()", "updateNow()", 'openHome("center")', 'openHome("listen")', "openReport()", "pickLook()"):
    check(f"the row's own action is kept: {fn_}", fn_ in render)

# ---- TV6 row 21: «تحكم من الجوال» must be findable (a tester on 3.145 did not find it and phone control stayed on): now its own button
cat_keys = set(C)
check("every row of SettingsActivity is in a category that has a button (nothing hides a row, whatever is installed)",
      all(c in cat_keys for c in got.values()), {k: c for k, c in got.items() if c not in cat_keys})
check("«التحكم من الجوال» is a button of the first screen, and OK on it opens the home's screen at once («تحكم من الجوال»)",
      C["phone"]["parent"] == "-" and direct.get("phone") == "تحكم من الجوال" and got.get("تحكم من الجوال") == "phone")
check("its line does not claim it is off (this app cannot see the home's switch): the screen it opens shows the state",
      'row("تحكم من الجوال", "متوقف' not in st and 'row("تحكم من الجوال", "تشغيله وإيقافه' in st and 'case SettingsHub.PHONE: return "تشغيله وإيقافه' in summ)
check("opening it never fails silently: no home installed says so, and the target is exported in the home's manifest",
      "ثبّت الواجهة الرئيسية" in st[st.index("private void openPhoneControl()"):].split("\n    }\n")[0]
      and re.search(r'<activity\s+android:name="\.PhoneActivity"\s+android:exported="true"', (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")) is not None)
check("«أرسل تقرير» opens the home's ReportActivity (exported), and says so when the home is missing or older",
      re.search(r'<activity\s+android:name="\.ReportActivity"\s+android:exported="true"', (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")) is not None
      and 'setClassName("sa.urtv.home", "sa.urtv.home.ReportActivity")' in st and "حدّث الواجهة الرئيسية" in st[st.index("private void openReport()"):].split("\n    }\n")[0])
# ---- the TV test of 3.145: after going down and up again the header stayed hidden; once, two screens were drawn over each other
top = st[st.index("private void topWhenFirst(View v)"):].split("\n    }\n")[0]
check("the first row (on a screen of buttons, its first line) brings the page back to its very top, after the scroll view's own move",
      "if (at == 0 || (pairs && at == 1)) scroll.post(() -> scroll.smoothScrollTo(0, 0));" in top and "boolean pairs = SettingsHub.hasTiles(screen) && !SettingsHub.wide(screen);" in top
      and "if (has) topWhenFirst(v);" in rowfn and "card.setOnFocusChangeListener((x, has) -> { paintState(v, mark, shown, state, has); if (has) topWhenFirst(x); });" in st)
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
      and "0.36f);" not in st and "0.64f));" not in st and "int rowsWidth = tiles ? pageWidth : pageWidth - sideWidth - dp(36);" in st)
check("…and the scroll goes up and down only: nothing can move the page sideways", "@Override public void scrollTo(int x, int y) { super.scrollTo(0, y); }" in st
      and "scroll = new UpDownScroll(this);" in st)
sys.exit(1 if failed else 0)
