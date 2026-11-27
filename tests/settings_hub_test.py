#!/usr/bin/env python3
"""The settings (4.3.2, the owner's ask: every option in the one section it belongs to; 4.3.3: ten sections, docs/mockups/SETTINGS-MAP.md): a first
screen of buttons (SettingsHub), the search wide on top; a button opens its section's rows (nothing deeper), or does one thing at once (DIRECT: the search). Every row of
SettingsActivity is found in the source and must be named in SettingsHub.ROWS (a new row with no category fails here), every header in
SettingsHub.HEADERS; the map has no stale names; the categories, the D-pad on the tiles (two to a row, from the right), Back to the
tile, and the wiring of render are checked from the source and, for SettingsHub and FocusKey, run for real (tests/SettingsHubRun.java).
Needs javac for the JVM part.   Run: python3 tests/settings_hub_test.py"""
import re, shutil, subprocess, sys, tempfile
unesc = lambda t: re.sub(r'\\u([0-9a-fA-F]{4})', lambda m: chr(int(m.group(1), 16)), t)   # a row named with isolates (\u2066UrTV\u2069 ...) as javac reads it
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
for m in re.finditer(r'\bheader\("([^"]+)"\)|\brow\(("(?:[^"\\]|\\.)*"|HOME_CARD_NAMES\[k\]|PrayerAdj\.label\(|k\.title)', st):
    if m.group(1):
        header = m.group(1)
        calls.append(("header", header, ""))
    elif m.group(2) == "HOME_CARD_NAMES[k]":
        names = re.search(r'HOME_CARD_NAMES = \{([^}]*)\}', st).group(1)
        for n in re.findall(r'"([^"]+)"', names): calls.append(("row", unesc(n), header))
    elif m.group(2) == "k.title":   # «أزرار الريموت»: one row for each app button this TV's remote has (RemoteButtons; every brand's, for the map)
        rb = (APP / "RemoteButtons.java").read_text(encoding="utf-8")
        for n in re.findall(r'b\("[a-z]+", "([^"]+)"', rb): calls.append(("row", unesc(n), header))
    elif m.group(2) == "PrayerAdj.label(":
        pass   # adjRow: the prayers' own rows, by their names and minutes; checked below with the header they are made under
    else:
        calls.append(("row", unesc(m.group(2)[1:-1]), header))
rows = [c for c in calls if c[0] == "row"]
heads = [c for c in calls if c[0] == "header"]
check("the source has the settings' rows and headers (the test reads what it should)", len(rows) >= 45 and len(heads) >= 9, (len(rows), len(heads)))
check("no row is made outside render, adjRow and the home cards' loop (so the scan above sees them all)",
      len(re.findall(r"(?<!View )\brow\((?!\))", st)) == len(re.findall(r'\brow\(("|HOME_CARD_NAMES\[k\]|PrayerAdj\.label\(|k\.title)', st)), "a row( call with another first argument")

catblock = hub[hub.index("static final String[][] CATEGORIES"):hub.index("static final String[][] OLD")]
cat_names = dict(re.findall(r'\{(SEARCH|LANG|PLACE|LOOK|APPS|BUTTONS|PRAY|CONTENT|SAVER|FAMILY|UPDATE|PHOTOS), "([^"]+)"', hub[hub.index("static final String[][] CATEGORIES"):hub.index("static final String[][] OLD")]))
rows_block = hub[hub.index("static final String[][] ROWS"):hub.index("static final String[][] HEADERS")]
mapped = [(unesc(a), b) for a, b in re.findall(r'\{"([^"]+)", (SEARCH|LANG|PLACE|LOOK|APPS|BUTTONS|PRAY|CONTENT|SAVER|FAMILY|UPDATE|PHOTOS)\}', rows_block)]

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
check("the first screen: the search first (the wide one), then the nine sections of the findability review, the most used first (4.4 beta 4, the owner)",
      [C[k]["name"] for k in first] == ["بحث في الإعدادات", "الصلاة والأذان", "صوري والخلفيات", "الشكل", "شاشة التوقف", "التطبيقات والأزرار",
                                       "المباريات والأسواق", "العائلة والجوال", "اللغة والموقع", "حول والتحديث"], [C[k]["name"] for k in first])
check("nothing is deeper than the first screen: every category is a button of it and none is a screen of buttons",
      len(first) == len(C) and not any(v["tiles"] for v in C.values()))
direct = {k: v["direct"] for k, v in C.items() if v["direct"] != "-"}
check("the one button that does one thing at once is the search; no city shortcut, no second city row (4.3.3: the city is chosen in one place)",
      direct == {"search": "بحث في الإعدادات"} and by.get("search") == ["بحث في الإعدادات"] and "city" not in by
      and by.get("lang", []).count("المدينة") == 1 and 'row("تغيير المدينة"' not in st and by.get("pray", []).count("الأذان") == 1, {k: (r, by.get(k)) for k, r in direct.items()})
check("every other category has rows of its own (no empty screen), a screen of buttons none",
      all(by.get(k) for k in C if k not in direct),
      {k: len(by.get(k, [])) for k in C})
want = {"الأذان": "pray", "تنبيه قبل الأذان": "pray", "أسماء المصادر": "apps", "ترتيب التطبيقات المفضلة": "apps", "تعديل المفضلة": "apps",
        "تحديث تلفزيوني": "update", "تحديث صفحة شاشة التوقف": "update", "التحديث التلقائي للصفحة": "update", "أرسل تقرير": "update", "الجوالات المسموح لها": "family",
        "مركز التحكم": "apps", "الرقابة الأبوية": "family", "تصفية المواقع": "family", "الدولة": "lang", "المدينة": "lang", "اللغة": "lang", "نظام الساعة": "lang",
        "التحية والطقس": "lang", "الرطوبة والرياح": "lang",
        "طريقة الحساب": "pray", "تعديل وقت الصلاة": "pray", "زر تلاوة وأذان": "apps", "افتح تلاوة وأذان": "pray", "البطاقة الأولى": "look", "معاينة الشاشة": "saver",
        "بطاقة ثانية": "saver", "بدء شاشة التوقف بعد": "saver", "الأرقام": "lang", "أسماء الأشهر الميلادية": "lang", "إظهار الثواني": "look", "حجم الساعة": "saver",
        "الألوان والإضاءة": "look", "نص أوضح": "look", "توهّج لون التطبيق": "look", "تعتيم الليل": "look", "صور شاشة التوقف": "photos", "صوري": "photos", "سرعة ألبوم صوري": "photos",
        "أضف صوري من الجوال": "photos", "احذف صورة": "photos", "التطبيقات المخفية": "apps", "حجم البطاقات": "look", "زوايا البطاقات": "look",
        "الرجوع يفتح شاشة التوقف": "saver", "رجوع مرتين يفتح شاشة التوقف": "saver", "الوضع الخفيف": "update",
        "التصنيفات": "content", "قائمة الأسهم": "content", "مباريات اليوم": "content", "زر المباريات": "apps"}
got = {r[0]: r[1] for r in row_out[:-1]}
check("each row in its one section (4.4 beta 4, the findability review): the prayers together, the photos with their add and delete, the city with the language, the buttons with the apps, the Back rows with the screensaver…",
      all(got.get(k) == v for k, v in want.items()), {k: got.get(k) for k, v in want.items() if got.get(k) != v})
adj = st[st.index("private void adjRow(final int k)"):].split("\n    }\n")[0]
check("the prayers' own minute rows (adjRow) are made in «الموقع», so they go with its category («الصلاة والأذان والتلاوة»), after «تعديل وقت الصلاة»",
      "row(PrayerAdj.label(" in adj and render.index('header("الموقع")') < render.index("adjRow(k)") < render.index('header("المباريات")')
      and 'if (on(SettingsHub.LANG) || on(SettingsHub.PRAY)) line(Location.HELP' in render and "SettingsHub.rank(screen, (String) v.getTag())" in st and "double at = r >= 0 ? r : last + 0.5;" in st)
check("a row not named goes with its header's category at run time (none is ever lost), else «شاشة التوقف»",
      row_out[-1][2] == "photos" and all(r[2] == r[1] for r in row_out[:-1]), [r for r in row_out[:-1] if r[2] != r[1]])

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
icons = st[st.index("private static int hubIcon(String cat)"):].split("\n    }\n")[0]
summ = st[st.index("private String hubSummary(String cat)"):].split("\n    }\n")[0]
check("every button has its icon and says what is set in it now (a case for every category)",
      all(f"case SettingsHub.{k.upper()}:" in icons and f"case SettingsHub.{k.upper()}:" in summ for k in C), [k for k in C if f"case SettingsHub.{k.upper()}:" not in summ])
check("«الأذان» shows the home's switch, read from the home (NowPlayingProvider .../adhan_on) off the UI thread (the last answer is kept), and says nothing of it when the home cannot say",
      "Boolean on = homeAdhanSeen();" in summ and "private Boolean homeAdhanSeen()" in st and "new Thread(() -> {" in st.split("private Boolean homeAdhanSeen()")[1].split("private Boolean homeAdhanOn()")[0] and 'content://sa.urtv.home.nowplaying/adhan_on' in st and "OnOff.ON_WORD : OnOff.OFF_WORD" in summ)
sidefn = st[st.index("    private View sideItem(final String cat) {"):].split("\n    }\n")[0]
sidepane = st[st.index("    private void sidePane() {"):].split("\n    }\n")[0]
check("4.3.3 the side list (the mockup): «الإعدادات», then the search and the ten sections in SettingsHub's order, built once, the section shown marked",
      "for (String cat : SettingsHub.tiles(null)) {" in sidepane and "if (sideItems.isEmpty()) {" in sidepane and 'findViewWithTag("bar")' in sidepane
      and 't.setText(conv("الإعدادات"));' in sidepane)
check("moving the focus along the list shows each section's rows once the focus rests on it (SIDE_MS: holding the arrow builds none on the way) and keeps the focus on the list; OK goes into the rows (the one last focused); the search item opens the search",
      "if (has && !SettingsHub.SEARCH.equals(cat) && !cat.equals(screen)) {" in sidefn and "sideFocus = true;\n                    open(cat);" in sidefn
      and "side.removeCallbacks(sideOpen);" in sidefn and "side.postDelayed(sideOpen, SIDE_MS);" in sidefn and "if (!x.hasFocus() || cat.equals(screen)) return;" in sidefn
      and "if (SettingsHub.SEARCH.equals(cat)) { searchSettings(); return; }" in sidefn and "restoreFocus(lastRow.get(screen), 0, scroll.getScrollY());" in sidefn
      and "if (sideFocus) {" in render and "item.post(item::requestFocus);" in render)
check("a row's key toward the list goes to its own section's item, never to the one beside it",
      "View item = sideItems.get(screen);" in st and "if (Dir.rtl()) card.setNextFocusRightId(item.getId()); else card.setNextFocusLeftId(item.getId());" in st)
check("the settings open on the first section with the focus on its item (a link or the city's way in opens its own section and row)",
      "if (screen == null && !classic) { screen = SettingsHub.PRAY; sideFocus = true; }" in st and st.index("if (askCity) cityRow();") < st.index("if (screen == null && !classic) { screen = SettingsHub.PRAY; sideFocus = true; }"))
check("the pane's head: the section's name, its line, and what is set in it now; no «•» in the hint",
      "title.setText(conv(SettingsHub.name(screen)));" in render and "line(SettingsHub.about(screen), look.sub, 0);" in render and "String now = hubSummary(screen);" in render
      and 'hint.setText(Words.of("اختيار يفتح، ورجوع يرجّعك خطوة"));' in render)

# ---- the screens: Back to the tile, the focus kept on a row, nothing drawn outside its category
back = st[st.index("public void onBackPressed()"):].split("\n    }\n")[0]
check("Back in the rows goes to their section's item in the side list (the row kept for the way back in); Back on the list leaves the settings",
      "if (f == null || screen == null || sideItems.get(screen) == null) { super.onBackPressed(); return; }" in back
      and "lastRow.put(screen, (String) f.getTag());" in back and back.index("lastRow.put(") < back.index("sideItems.get(screen).requestFocus();")
      and ": entering ? (tiles ? (hubAt != null ? \"hub:\" + hubAt : null) : lastRow.get(screen)) :" in render)
side = st[st.index("private void sidePane()"):].split("\n    }\n")[0]
check("the side list on the right (first in the RTL panes, in a scroll view for small screens), the rows on the left",
      st.index("panes.addView(sideScroll, sideLp);") < st.index("panes.addView(scroll,") and "panes.setLayoutDirection(Dir.layout());" in st
      and "sideScroll.addView(side, new FrameLayout.LayoutParams(-1, -2));" in st)
abouts = dict(re.findall(r'\{(SEARCH|LANG|PLACE|LOOK|APPS|BUTTONS|PRAY|CONTENT|SAVER|FAMILY|UPDATE|PHOTOS), "[^"]+", "([^"]*)"', hub[hub.index("static final String[][] CATEGORIES"):hub.index("static final String[][] OLD")]))
check("every category with a screen of its own has its line for it (a DIRECT one has none)",
      all(len(abouts[k.upper()]) > 20 for k in C if k not in direct) and all(abouts[k.upper()] == "" for k in direct), abouts)
check("a button opens its category from the top", "entering = true;" in st[st.index("private void open(String category)"):].split("\n    }\n")[0])
rowfn = st[st.index("private View row(String label, String value, int icon, final Runnable onClick)"):].split("\n    }\n")[0]
check("a row is added only on its own category's screen (made on every screen, so its code runs as before)",
      "final String cat = SettingsHub.of(FocusKey.key(label), groupCat);" in rowfn and "if (!on(cat)) return card;" in rowfn
      and rowfn.index("if (!on(cat)) return card;") < rowfn.index("list.addView(card, lp);"))
check("a header is drawn by arrange, from SettingsHub.ORDER, and only above a row; a row made in the code is added at once and put in its place after the build",
      "if (!row) continue;" in st and "headerView(h)" in st and "SettingsHub.headers(screen)" in st and "pendingHeader" not in st and "arrange();" in render
      and render.index('row("معاينة الشاشة"') < render.index("arrange();") < render.index("restoreFocus(want, wasIndex, scrollY);"))
check("the version lines show under «تحديث صفحة شاشة التوقف» only where that row is", "if (last.getParent() == list) { TextView ver = note(last, Updater.versionLines(this));" in render)
check("the rows are found again by their tag after a rebuild (the focus stays on the row)", "if (all.contains(v)) return v;" in st)
for fn_ in ("openAdhanSettings()", "openDockSettings()", "openInputNames()", "openDockEdit()", "openUpdate()", "openPhoneControl()", "openFamilyDns()",
            "pickCity()", "updateNow()", 'openHome("center")', 'openHome("listen")', "openReport()", "AppearanceActivity.class"):
    check(f"the row's own action is kept: {fn_}", fn_ in render)

# ---- TV6 row 21: «تحكم من الجوال» must be findable (a tester on 3.145 did not find it and phone control stayed on): a row of «التحكم والجوال», and the search finds it by «جوال»
check("phone control (4.4 «الجوالات المسموح لها», the owner's name) is a row of «العائلة والأمان», and the search finds it by «جوال», «ريموت» and its old name",
      got.get("الجوالات المسموح لها") == "family" and 'row("الجوالات المسموح لها"' in st and '{"جوال", "الجوالات المسموح لها"}' in hub and '{"ريموت", "الجوالات المسموح لها"}' in hub
      and '{"تحكم من الجوال", "الجوالات المسموح لها"}' in hub and '{FAMILY, "الرقابة الأبوية", "تصفية المواقع", "الجوالات المسموح لها"}' in hub)
check("its line does not claim it is off (this app cannot see the home's switch): the screen it opens shows the state",
      'row("الجوالات المسموح لها", "متوقف' not in st and 'row("الجوالات المسموح لها", "التحكم من الجوال: تشغيله وإيقافه' in st)
check("opening it never fails silently: no home installed says so, and the target is exported in the home's manifest",
      "ثبّت الواجهة الرئيسية" in st[st.index("private void openPhoneControl()"):].split("\n    }\n")[0]
      and re.search(r'<activity\s+android:name="\.PhoneActivity"\s+android:exported="true"', (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")) is not None)
check("«أرسل تقرير» opens the home's ReportActivity (exported), and says so when the home is missing or older",
      re.search(r'<activity\s+android:name="\.ReportActivity"\s+android:exported="true"', (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")) is not None
      and 'setClassName("sa.urtv.home", "sa.urtv.home.ReportActivity")' in st and "حدّث الواجهة الرئيسية" in st[st.index("private void openReport()"):].split("\n    }\n")[0])
# ---- the TV test of 3.145: after going down and up again the header stayed hidden; once, two screens were drawn over each other
top = st[st.index("private void topWhenFirst(View v)"):].split("\n    }\n")[0]
check("the first row brings the page back to its very top (the section's name and line show again), after the scroll view's own move",
      "if (at == 0 || (pairs && at == 1)) scroll.post(() -> scroll.smoothScrollTo(0, 0));" in top and "boolean pairs = classic && SettingsHub.hasTiles(screen)" in top and "if (has) topWhenFirst(v);" in rowfn)
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
      "new LinearLayout.LayoutParams(sideWidth, -1)" in st and "new LinearLayout.LayoutParams(width - sideWidth - gapPx + (cs != null ? cs.px(CouchStyle.PAD) : 0), -1)" in st
      and "0.36f);" not in st and "0.64f));" not in st and "int rowsWidth = pageWidth - sideWidth - gapPx + (cs != null ? cs.px(CouchStyle.PAD) : 0);" in st)
check("…and the scroll goes up and down only: nothing can move the page sideways", "@Override public void scrollTo(int x, int y) { super.scrollTo(0, y); }" in st
      and "scroll = new UpDownScroll(this);" in st)
check("beta 4 «الأذان لكل صلاة»: in the hub right after «الأذان» in «الصلاة والأذان» (both orders), found by its words, and its value is the home's summary (not a fixed line)",
      by.get("pray", [])[by["pray"].index("الأذان") + 1] == "الأذان لكل صلاة" and '{"الأذان لكل صلاة", PRAY}' in hub
      and all(f'{{"{w}", "الأذان لكل صلاة"}}' in hub for w in ("اذان الفجر", "كتم الاذان", "الاذان في الشاشه")) and 'row("الأذان لكل صلاة", prayersValue()' in st and '"content://sa.urtv.home.nowplaying/adhan_prayers"' in st)
sys.exit(1 if failed else 0)
