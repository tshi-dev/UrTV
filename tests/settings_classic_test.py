#!/usr/bin/env python3
"""4.4 the owner: the settings have styles («أزرار», the 4.3.2 layout of buttons; «كنبة», a side list; tvOS). 4.4 beta 4 (the owner, the findability
review, docs/research/SETTINGS-FINDABILITY.md): ONE tree of nine sections for all of them; the style changes the layout only (buttons or a side list),
never where a row is. One row registry (SettingsActivity.render builds each row once), one placement (SettingsHub's arrays); every setting is
reachable and found the same in every style, the city is chosen once, and the style switch is in it.   python3 tests/settings_classic_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


hub, st, prefs = ((APP / f).read_text(encoding="utf-8") for f in ("SettingsHub.java", "SettingsActivity.java", "Prefs.java"))
unesc = lambda x: re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m.group(1), 16)), x)
new_rows = [unesc(r) for r, c in re.findall(r'\{"([^"]+)", ([A-Z]+)\}', hub[hub.index("static final String[][] ROWS = {"):hub.index("static final String[][] HEADERS = {")])]
check("one placement for every style: no 4.3.2 tables are left (ROWS_432, ORDER_432, CATEGORIES_432, HEADERS_432, OLD_432), and the lookups ignore the style",
      not re.search(r"(ROWS|ORDER|CATEGORIES|HEADERS|OLD)_432", hub) and "private static String[][] cats() { return CATEGORIES; }" in hub
      and "private static String[][] rows() { return ROWS; }" in hub and "private static String[][] order() { return ORDER; }" in hub)
order = [unesc(x) for h, x in re.findall(r'(H \+ )?"([^"]*)"', hub[hub.index("static final String[][] ORDER = {"):hub.index("};", hub.index("static final String[][] ORDER = {"))]) if not h]
check("…and every row is in its section's order (none left at the end by accident; only the search's own row has no section of rows)",
      all(r in order for r in new_rows if r != "بحث في الإعدادات"), [r for r in new_rows if r not in order and r != "بحث في الإعدادات"])
check("the 4.4 rows sit in their sections: the switch and the look's rows in «الشكل», the horizons in «صوري والخلفيات», the Hijri shift with the language, the caches with the update",
      all(f'{{"{r}", {c}}}' in hub for r, c in (("أشكال جاهزة", "LOOK"), ("شكل شاشة الإعدادات", "LOOK"), ("شكل كنبة", "LOOK"), ("التخطيط", "LOOK"), ("حزم الآفاق", "PHOTOS"), ("الألوان والإضاءة", "LOOK"),
                                               ("فرق التاريخ الهجري", "LANG"), ("مسح الملفات المؤقتة", "UPDATE"))))

if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "SettingsHub.java"), str(ROOT / "tests/SettingsClassicRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.SettingsClassicRun"], check=True,
                             capture_output=True).stdout.decode("utf-8").splitlines()
    r = {}
    for l in out:
        c = l.split("\t")
        r[tuple(c[:3]) if c[1] in ("of", "search") else tuple(c[:2])] = c[-1] if c[1] in ("of", "search") else c[2:]
    same = all(r[("432",) + k[1:]] == v for k, v in r.items() if k[0] == "44")
    check("the buttons' layout and the side list answer the same: the same first screen, the same section for each row, the same search, the same old keys",
          same and len([k for k in r if k[0] == "44"]) > 10, {k: (r.get(("432",) + k[1:]), v) for k, v in r.items() if k[0] == "44" and r.get(("432",) + k[1:]) != v})
    check("the first screen: the search (the wide one, the only button that runs a row) and the nine sections, none is a screen of buttons",
          r[("432", "tiles")] == ["search,pray,photos,look,saver,apps,content,family,lang,update"] and r[("432", "cards")] == [""] and r[("432", "more")] == [""]
          and r[("432", "wide")] == ["true"] and r[("432", "direct")] == ["null", "بحث في الإعدادات"], r)
    check("each row in its one section: the city with the language, the adhan with the prayers, the card beside the clock in the screensaver, the digits with the language, the shows' filters with the matches",
          [r[("432", "of", x)] for x in ("المدينة", "الأذان", "المحتوى", "الأرقام", "التصنيفات")] == ["lang", "pray", "saver", "lang", "content"])
    check("search, in every style, answers with the row's own section: «مدينه», «صوت» (an alias), «كنبه», «واجهه المصنع»",
          r[("432", "search", "مدينه")].startswith("المدينة@lang") and "الأذان@pray" in r[("432", "search", "صوت")]
          and "شكل كنبة@look" in r[("432", "search", "كنبه")] and "@apps" in r[("432", "search", "واجهه المصنع")], {k: v for k, v in r.items() if k[1] == "search"})
    check("an old or a new section key lands somewhere right in every style (4.3.2's «cards» on the screensaver)", r[("432", "canon")] == ["pray", "look", "saver"], r)
else:
    print("SKIP the layouts on a JVM: no javac here")

check("the layout follows the style: buttons unless «كنبة» (SettingsHub.classic, the layout only), set before anything is built; 4.4 beta 2: «شكل شاشة الإعدادات» (tests/settings_one_form_test.py)",
      'classic = !"couch".equals(prefs.settingsStyle());' in st and "SettingsHub.classic(classic);" in st
      and st.index("SettingsHub.classic(classic);") < st.index("UpdateJob.schedule(this);"))
check("4.3.2's own ways: its screens of buttons (hub, tile), Back up a screen, its section title on the side; the side list's only when on",
      "if (tiles) { hub(want, wasIndex); return; }" in st and "private View tile(final String cat, String name, int icon, String summary)" in st
      and "if (classic) {   // 4.3.2: Back goes up a screen" in st and "if (classic) classicHead(tiles); else {" in st and "private void classicSide()" in st)
check("4.4 beta 2: «شكل شاشة الإعدادات» is a row of «الشكل» in every style, and choosing another style opens the settings again in it, on that row",
      'row("شكل شاشة الإعدادات", Prefs.settingsStyleLabel(prefs.settingsStyle())' in st and '.putExtra("want", "شكل شاشة الإعدادات")' in st
      and "finish();" in st[st.index('row("شكل شاشة الإعدادات"'):][:900])
check("one row registry: each row is built once by render (no second copy of a row's code for the old screen)", st.count('row("شكل كنبة"') == 1 and st.count('row("المدينة"') == 1
      and st.count('row("الأذان"') == 1)
tree = (ROOT / "docs/SETTINGS-TREE.md").read_text(encoding="utf-8")
check("docs/SETTINGS-TREE.md has the one tree of every style, and not the old two", "شجرة واحدة لكل أشكال شاشة الإعدادات" in tree and "شاشة ٤.٣.٢" not in tree)
sys.exit(1 if failed else 0)
