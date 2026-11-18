#!/usr/bin/env python3
"""4.4 the owner: «شكل كنبة» switches the settings screen too. Off (the default) = the 4.3.2 screen exactly as on master (a first screen of big
buttons, «البطاقات» and «المزيد» screens of buttons, Back up a screen); on = the side list. One row registry (SettingsActivity.render builds each row
once), two placements (SettingsHub: the side list's arrays and the *_432 ones); every setting is reachable in both, search works in both, the city
is chosen once (4.3.2's wide «المدينة» button runs the one city row), and the switch is in both.   python3 tests/settings_classic_test.py"""
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
old_rows = [unesc(r) for r, c in re.findall(r'\{"([^"]+)", "([a-z]+)"\}', hub[hub.index("ROWS_432 = {"):hub.index("HEADERS_432 = {")])]
old_order = [unesc(x) for h, x in re.findall(r'(H \+ )?"([^"]*)"', hub[hub.index("ORDER_432 = {"):hub.index("OLD_432 = {")]) if not h and not re.fullmatch(r"[a-z]+", x)]
check("every row of the registry has its place in both layouts (the side list and 4.3.2), no more and no fewer", sorted(new_rows) == sorted(old_rows) and len(set(old_rows)) == len(old_rows),
      (set(new_rows) ^ set(old_rows)))
check("…and every row is in its 4.3.2 section's order (none left at the end by accident)", all(r in old_order for r in old_rows), [r for r in old_rows if r not in old_order])
check("the 4.4 rows sit in their natural 4.3.2 section: the switch and the look's rows in «المظهر والخلفيات», the Hijri shift with the clock, the caches with the update",
      all(f'{{"{r}", "{c}"}}' in hub for r, c in (("أشكال جاهزة", "look"), ("شكل كنبة", "look"), ("التخطيط", "look"), ("حزم الآفاق", "look"), ("وضع الألوان", "look"), ("فرق التاريخ الهجري", "clock"),
                                                 ("مسح الملفات المؤقتة", "update"))) and '{"look", "أشكال جاهزة", "شكل كنبة",' in hub)

if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "SettingsHub.java"), str(ROOT / "tests/SettingsClassicRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.SettingsClassicRun"], check=True,
                             capture_output=True).stdout.decode("utf-8").splitlines()
    r = {}
    for l in out:
        c = l.split("\t")
        r[tuple(c[:3]) if c[1] in ("of", "search") else tuple(c[:2])] = c[-1] if c[1] in ("of", "search") else c[2:]
    check("4.3.2 (off): master's first screen of buttons (the city wide first, then the adhan…, «المزيد»), and the search last",
          r[("432", "tiles")] == ["city,adhan,place,clock,listen,cards,look,control,phone,update,report,more,search"]
          and r[("432", "cards")] == ["saver,home"] and r[("432", "more")] == ["content,apps,shows,parental,family,lang"] and r[("432", "wide")] == ["true"], r)
    check("…the city is chosen once: 4.3.2's «المدينة» button runs the one city row; the search button runs the search", r[("432", "direct")] == ["المدينة", "بحث في الإعدادات"], r[("432", "direct")])
    check("…its rows where 4.3.2 had them: the city in «الموقع والصلاة», the adhan its own button, the card beside the clock in «شاشة التوقف», the digits in «الساعة والأرقام», the shows' filters in «جديد المسلسلات»",
          [r[("432", "of", x)] for x in ("المدينة", "الأذان", "المحتوى", "الأرقام", "التصنيفات")] == ["place", "adhan", "saver", "clock", "shows"])
    check("the side list (on): ten sections and the search, as before", r[("44", "tiles")] == ["search,lang,place,look,apps,buttons,pray,content,saver,family,update"]
          and [r[("44", "of", x)] for x in ("المدينة", "الأذان", "المحتوى", "الأرقام")] == ["place", "pray", "saver", "lang"])
    check("search works in both, each answering with its own section: «مدينه», «صوت» (an alias), «كنبه», «واجهه المصنع»",
          r[("432", "search", "مدينه")].startswith("المدينة@place") and r[("44", "search", "مدينه")].startswith("المدينة@place")
          and "الأذان@adhan" in r[("432", "search", "صوت")] and "الأذان@pray" in r[("44", "search", "صوت")]
          and "شكل كنبة@look" in r[("432", "search", "كنبه")] and "شكل كنبة@look" in r[("44", "search", "كنبه")]
          and "@home" in r[("432", "search", "واجهه المصنع")] and "@apps" in r[("44", "search", "واجهه المصنع")], {k: v for k, v in r.items() if k[1] == "search"})
    check("an old or a new section key lands somewhere right in either layout", r[("432", "canon")] == ["place", "look", "cards"] and r[("44", "canon")] == ["pray", "look", "saver"], r)
else:
    print("SKIP the layouts on a JVM: no javac here")

check("the screen follows the switch: off = 4.3.2 (SettingsHub.classic), set before anything is built; 4.5: only with Prefs.ONE_FORM off (tests/settings_one_form_test.py)",
      "classic = !Prefs.ONE_FORM && !prefs.couchOn();   // 4.5" in st and "SettingsHub.classic(classic);" in st
      and st.index("SettingsHub.classic(classic);") < st.index("UpdateJob.schedule(this);"))
check("4.3.2's own ways: its screens of buttons (hub, tile), Back up a screen, its section title on the side; the side list's only when on",
      "if (tiles) { hub(want, wasIndex); return; }" in st and "private View tile(final String cat, String name, int icon, String summary)" in st
      and "if (classic) {   // 4.3.2: Back goes up a screen" in st and "if (classic) classicHead(tiles); else {" in st and "private void classicSide()" in st)
check("the switch is in both (its row is the registry's, in «المظهر» and in «المظهر والخلفيات»), and flipping it opens the settings again in the other layout, on it",
      'row("شكل كنبة", prefs.couchLabel()' in st and "flipCouch()" in st and '.putExtra("want", "شكل كنبة")' in st and "finish();" in st[st.index("private void flipCouch()"):][:600])
check("one row registry: each row is built once by render (no second copy of a row's code for the old screen)", st.count('row("شكل كنبة"') == 1 and st.count('row("المدينة"') == 1
      and st.count('row("الأذان"') == 1)
tree = (ROOT / "docs/SETTINGS-TREE.md").read_text(encoding="utf-8")
check("docs/SETTINGS-TREE.md has both trees", "«شكل كنبة» مفعّل: الشاشة الجديدة" in tree and "«شكل كنبة» متوقف (الأصل): شاشة ٤.٣.٢" in tree)
sys.exit(1 if failed else 0)
