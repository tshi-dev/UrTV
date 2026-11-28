#!/usr/bin/env python3
"""4.4 beta 4, the owner («Do it on our settings and couch setting! Don't miss anything»; docs/research/SETTINGS-FINDABILITY.md): the settings tree walked
in every settings style («أزرار», «كنبة», tvOS). Every row the audit found (docs/research/SETTINGS-AUDIT.md, section A: 139 rows) is still a row, in a
section whose button is on the first screen, so nothing is deeper than section ← row ← its picker; the features that live past a row (the adhan's own
screen in «تلاوة», «الألوان والإضاءة»'s screen, a photo's delete, the hidden apps) are found by the words people use; the three the owner's users could
not find are a short way in.
    python3 tests/settings_findability_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
STYLES = ["buttons", "couch", "tvos"]
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


prefs = (APP / "Prefs.java").read_text(encoding="utf-8")
check("the styles this test walks are every style the settings have (Prefs.SETTINGS_STYLES)",
      re.search(r'SETTINGS_STYLES = \{([^}]*)\}', prefs).group(1).replace('"', "").replace(" ", "").split(",") == STYLES)

# the audit's rows (section A), as the TV labelled them; the renames of the review, and «زر … في الريموت» as one of its rows
audit = (ROOT / "docs/research/SETTINGS-AUDIT.md").read_text(encoding="utf-8")
part = audit[audit.index("## A."):audit.index("## B.")]
RENAMED = {"وضع الألوان": "الألوان والإضاءة", "UrTV النظيف": "\u2066UrTV\u2069 النظيف", "أكمل المشاهدة": "كمّل المشاهدة", "اجعل UrTV الشاشة الرئيسية": "خلّ UrTV هي الشاشة الرئيسية"}   # + the Saudi wording (4.4 beta 4)   # the code isolates the Latin name
labels = []
for n, lab in re.findall(r"^\|\s*(\d+)\s*\|[^|]*\|\s*([^|]+?)\s*\|", part, re.M):
    lab = re.sub(r"\s*…?\s*\(.*\)$", "", lab).strip()   # «صوري (n/max)», «زر يوتيوب في الريموت … (one row per key)»
    labels.append(RENAMED.get(lab, lab))
check("the audit's settings rows are all read (139)", len(labels) == 139, len(labels))
labels = sorted(set(labels))
NEW = ["أضف صوري من الجوال", "احذف صورة", "صوري", "التطبيقات المخفية", "حجم البطاقات", "زوايا البطاقات", "المدينة"]   # the review's own rows, looked up too

# the words people use for what lives past a row, and the row that leads there (the adhan's screen, the colours' screen, the photos, the hidden apps)
WORDS = {
    "اذاني": "أذاني من ملف", "اذان خاص": "أذاني من ملف", "اذان من ملف": "أذاني من ملف", "دعاء": "الأدعية والتذكيرات", "تذكير": "الأدعية والتذكيرات", "صوتياتي": "صوتياتي", "الاقامه": "الأذان",
    "تنبيه عند الاذان": "الأذان", "مستوي صوت الاذان": "الأذان", "اذان الجمعه": "الأذان", "اذان الفجر": "الأذان لكل صلاة", "كتم الاذان": "الأذان لكل صلاة",
    "شفافيه": "الألوان والإضاءة", "لون التمييز": "الألوان والإضاءة", "شكل التركيز": "الألوان والإضاءة", "تعتيم الخلفيه": "الألوان والإضاءة",
    "حذف صوره": "احذف صورة", "اضف صور": "أضف صوري من الجوال", "فلاشه": "أضف من فلاشة أو التنزيلات", "usb": "أضف من فلاشة أو التنزيلات",
    "المخفيه": "التطبيقات المخفية", "رجع تطبيق": "التطبيقات المخفية", "جعفري": "طريقة الحساب", "زوايا": "زوايا البطاقات",
}

if not shutil.which("javac"):
    print("SKIP settings findability: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "SettingsHub.java"), str(APP / "FocusKey.java"), str(ROOT / "tests/SettingsFindabilityRun.java")],
                   check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.SettingsFindabilityRun", *STYLES, "--", *sorted(set(labels) | set(NEW)), "--", *WORDS],
                         check=True, capture_output=True).stdout.decode("utf-8").split("\n")
cats = {}
rows = {}
found = {}
for l in out:
    f = l.split("\t")
    if f[0] == "cat": cats.setdefault(f[1], {})[f[2]] = f[3]
    elif f[0] == "row": rows.setdefault(f[1], {})[f[2]] = (f[3], f[4], int(f[5]))
    elif f[0] == "search": found.setdefault(f[1], {})[f[2]] = [x.split("=")[0] for x in f[3:]]

for st in STYLES:
    check(f"«{st}»: every section is a button of the first screen (none is a screen of buttons, none deeper)",
          cats.get(st) and all(p == "null" for p in cats[st].values()), cats.get(st))
    lost = [k for k, (c, p, r) in rows[st].items() if c == "MISSING"]
    check(f"«{st}»: every row the audit found is still a row of the settings (139, none lost by the new tree)", not lost, lost)
    deep = [k for k, (c, p, r) in rows[st].items() if c != "MISSING" and (p != "null" or c not in cats[st])]
    check(f"«{st}»: each sits in a section of the first screen: section ← row ← its picker, three steps at most", not deep, deep)
    unplaced = [k for k, (c, p, r) in rows[st].items() if c not in ("MISSING", "search") and r < 0]
    check(f"«{st}»: each has its place in its section's order (never dropped at the end)", not unplaced, unplaced)
    miss = {w: found[st][w][:3] for w, want in WORDS.items() if want not in found[st].get(w, [])}
    check(f"«{st}»: what lives past a row (the adhan's screen, the colours' screen, the photos, the hidden apps) is found by the words people use", not miss, miss)

base = rows["buttons"]
check("the same tree in every style: each row in the same section and place in «أزرار», «كنبة» and tvOS",
      all(rows[s] == base for s in STYLES), [s for s in STYLES if rows[s] != base])

# the owner's three: the own adhan, the duas with their own sound, deleting a photo; one section and one row away, in every style
st_src = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
hub = (APP / "SettingsHub.java").read_text(encoding="utf-8")
first = re.findall(r'\{(SEARCH|PRAY|PHOTOS|LOOK|SAVER|APPS|CONTENT|FAMILY|LANG|UPDATE), "([^"]+)"', hub[hub.index("CATEGORIES = {"):hub.index("OLD = {")])
check("the sections in the review's order, the most used first: the prayers, then the photos", [n for _, n in first][:3] == ["بحث في الإعدادات", "الصلاة والأذان", "صوري والخلفيات"], first)
check("deleting a photo: «صوري والخلفيات» ← «احذف صورة» opens the photos' grid at once (it was the 4th choice of a list of modes, ~41 presses)",
      base.get("احذف صورة", ("",))[0] == "photos" and 'row("احذف صورة"' in st_src and "else pickOwnToDelete(); } });" in st_src)
check("adding a photo is said where the photos are: «أضف صوري من الجوال» first in «صوري»; an empty «صوري» says the phone, not only the assistant",
      base.get("أضف صوري من الجوال", ("",))[0] == "photos" and base["أضف صوري من الجوال"][2] < base["صوري"][2]
      and '"لا توجد صور، أضفها من الجوال"' in st_src and "يضيفها مساعدك الذكي" not in st_src)
check("the hidden apps: a row by the name the hide message gives («التطبيقات المخفية»), opening the list at once",
      base.get("التطبيقات المخفية", ("",))[0] == "apps" and "openAppsOrder(false, true); } });" in st_src
      and "«التطبيقات المخفية»" in (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8"))
check("the city is in one place, «اللغة والموقع», and every way in lands there", base.get("المدينة", ("",))[0] == "lang" and "screen = SettingsHub.LANG;" in st_src)
center = (ROOT / "android/launcher/src/main/java/sa/urtv/home/ControlCenter.java").read_text(encoding="utf-8")
check("«الإعدادات» means ours: the control centre's tile that opens the TV's own settings is «إعدادات الجهاز»",
      'tile("tv", S.t("إعدادات الجهاز")' in center and 'tile("tv", S.t("الإعدادات")' not in center)
phone = [(ROOT / f"android/launcher/src/main/assets/phone/{p}.html").read_text(encoding="utf-8") for p in ("adb", "audio", "draw", "photos")]
check("the phone link has one name: the help says the home's «الجوال» button, not «أسهل من الجوال» (a button of the markets page only)",
      all("أسهل من الجوال" not in p and "Easier from the phone" not in p for p in phone)
      and "أسهل من الجوال" not in (ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/AudioActivity.java").read_text(encoding="utf-8"))
check("«كنبة» opens on «الصلاة والأذان» with the focus on its item, and keeps it there when the home's answer about the adhan redraws the rows (the emulator's shot: it jumped to the first row)",
      "if (!classic && !entering && (had == null || !keyed) && page == null && !SettingsHub.SEARCH.equals(screen)) sideFocus = true;" in st_src and "KeyEvent.ACTION_DOWN) keyed = true;" in st_src
      and st_src.index("if (!classic && !entering && (had == null || !keyed) && page == null && !SettingsHub.SEARCH.equals(screen)) sideFocus = true;") < st_src.index("if (sideFocus) {   // the focus stays on"))
city_note = (APP / "Location.java").read_text(encoding="utf-8")
check("the city's note names the section it is in now («اللغة والموقع»), on the TV and on the page", "لتغييرها: الإعدادات ثم اللغة والموقع" in city_note
      and "لتغييرها: الإعدادات ثم اللغة والموقع" in (ROOT / "src/template.html").read_text(encoding="utf-8") and "ثم الموقع\"" not in city_note)
check("no «جديد» marks on rows or sections (the review: badges sparingly; none is needed)", '"جديد"' not in st_src and '"جديد"' not in hub)
check("no «·» in a section's name, line or short line", "·" not in hub[hub.index("CATEGORIES = {"):hub.index("OLD = {")] and "·" not in hub[hub.index("SHORT_LINES"):])
sys.exit(1 if failed else 0)
