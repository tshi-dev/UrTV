#!/usr/bin/env python3
"""docs/SETTINGS-TREE.md, made from the code (never by hand): every setting of the TV as an indented tree with its full path, read from SettingsHub.java
(the sections, their headers and rows, in the order the person meets them), with each row's choices and default from Prefs.java, its command from
docs/SETTINGS.md (the agent's way), and where the phone page (phone/Schema.java «set») and the control centre (ControlCenter.ROWS) reach the same row.

    python3 .github/tools/settings_tree.py            writes docs/SETTINGS-TREE.md
    python3 .github/tools/settings_tree.py --check    exit 1 when the file is not what the code says now (tests/settings_tree_test.py)
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
OUT = ROOT / "docs/SETTINGS-TREE.md"

# a row's choices: (Prefs constant of [value, name] pairs) or (values constant, names constant), and the Prefs getter its default is read from
CHOICES = {
    "الأرقام": ("DIGITS", None, "digits"),
    "أشكال جاهزة": ("PRESETS", "PRESET_NAMES", "preset"),
    "شكل كنبة": (None, None, "couchOn"),
    "التخطيط": ("HOME_LAYOUTS", "HOME_LAYOUT_NAMES", "homeLayout"),
    "الخلفية": ("HOME_BGS", "HOME_BG_NAMES", "homeBg"),
    "الساعة": ("HOME_CLOCKS", "HOME_CLOCK_NAMES", "homeClock"),
    "شكل التطبيقات": ("HOME_APPS", "HOME_APPS_NAMES", "appsStyle"),
    "حجم شريط tvOS": ("GLASS_SIZES", None, "glassSize"),
    "صفوف شريط tvOS": ("GLASS_ROWS", "GLASS_ROWS_NAMES", "glassRows"),
    "حزم الآفاق": ("SCENE_PACKS", None, None),
    "بقاء كل أفق": ("SCENE_STAYS", "SCENE_STAY_NAMES", "sceneStay"),
    "الأفق يتبع الوقت": (None, None, "sceneByTime"),
    "الأفق يتبع الطقس": (None, None, "sceneByWeather"),
    "شكل البطاقة": ("CARD_STYLES", None, "cardStyle"),
    "حجم البطاقات": ("CARD_SIZES", "CARD_SIZE_NAMES", "cardSize"),
    "زوايا البطاقات": ("CARD_CORNERS", "CARD_CORNER_NAMES", "cardCorner"),
    "حجم الساعة": ("CLOCK_SIZES", None, "clockSize"),
    "تصميم شاشة التوقف": ("SAVER_DESIGNS", None, "design"),
    "الشريط في الوسط": ("SAVER_BARS", None, "bar"),
    "مكان الشريط": ("BAR_PLACES", None, "barAt"),
    "مكان الطقس": ("WX_PLACES", None, "wxAt"),
    "عرض الأسواق": ("MK_STYLES", None, "mkStyle"),
    "تغيّر الأسهم": ("STK_CHG", None, "stkChg"),
    "الخانة اليسرى": ("SLOTS", None, "slot"),
    "المناسبات الوطنية": ("NATIONAL", None, "national"),
    "تطبيقات في الشريط": ("FAV_BARS", None, "favBar"),
}
# the phone page's «set» keys (phone/Schema.java) and the row each one changes; None: not a row of the settings (a one-off action)
PHONE = {"city": "المدينة", "home_cards": "البطاقة الأولى", "adhan_on": "الأذان", "adhan_short": "الأذان", "adhan_stopkey": "الأذان", "adhan_voice": "الأذان",
         "adhan_fajr_voice": "الأذان", "adhan_volume": "الأذان", "adhan_prayers": "الأذان لكل صلاة", "sleep_minutes": None, "saver_idle": "بدء شاشة التوقف بعد", "lang": "اللغة",
         "fav_bar": "تطبيقات في الشريط"}
# the control centre's rows (ControlCenter.ROWS) that open a setting's row; the others are actions or screens of their own
CONTROL = {"net": None, "sound": None, "speed": None, "ours": None, "games": None, "city": "المدينة", "tv": None, "apps": None, "saver": None, "adhan": None, "black": None}
YES, NO = "مفعّل", "متوقف"
# a row's command where docs/SETTINGS.md names its place another way: an example each (the subcommand must be a case of optimize/saver.sh: checked)
MORE_CMDS = {
    "شكل كنبة": ["optimize/saver.sh couch on"],
    "نظام الساعة": ["optimize/saver.sh clock24 on"], "الدولة": ["optimize/saver.sh city '<name>' <lat> <lon> <time zone> SA"],
    "الألوان والإضاءة": ["optimize/saver.sh look navy", "optimize/saver.sh appearance auto"], "إظهار الثواني": ["optimize/saver.sh seconds on"],
    "البطاقة الأولى": ["optimize/saver.sh home-card prayers,hourly,verse"], "البطاقة الثانية": ["optimize/saver.sh home-card prayers,hourly,verse"],
    "البطاقة الثالثة": ["optimize/saver.sh home-card prayers,hourly,verse"],
    "سرعة ألبوم صوري": ["optimize/saver.sh albummode ten"], "شكل صورة ألبوم صوري": ["optimize/saver.sh albumfit fill"], "حجم ألبوم صوري": ["optimize/saver.sh albumsize tall"],
    "عنوان ألبوم صوري": ["optimize/saver.sh albumtitle off"], "تاريخ الصورة في ألبوم صوري": ["optimize/saver.sh albumdate off"],
    "الخروج من تلفزيونك": ["optimize/saver.sh backexit hold"], "تطبيقات في الشريط": ["optimize/saver.sh favbar 7"],
    "زر مركز التحكم": ["optimize/saver.sh pillshidden center"], "زر الجوال": ["optimize/saver.sh pillshidden phone"], "زر المصادر": ["optimize/saver.sh pillshidden inputs"], "زر الأسواق": ["optimize/saver.sh pillshidden markets"],
    "زر المباريات": ["optimize/saver.sh pillshidden matches"], "تعديل وقت الصلاة": ["optimize/saver.sh prayeradj fajr +2 maghrib -1"],
    "الفرق المتابعة": ["optimize/saver.sh teams 136013:Al-Hilal"], "الدوريات كاملة": ["optimize/saver.sh leagues 'Saudi-Arabian Pro League'"],
    "قائمة الأسهم": ["optimize/saver.sh stocks 2222 1120 AAPL"], "تغيّر الأسهم": ["optimize/saver.sh stkchg amount"], "التصنيفات": ["optimize/saver.sh genres Drama,Comedy"],
    "اللغات": ["optimize/saver.sh langs Arabic,English"], "تصفية المواقع": ["optimize/dock.sh familydns on"], "قفل التطبيقات": ["optimize/dock.sh pin lock com.netflix.ninja"], "أقل تقييم": ["optimize/saver.sh minrating 7"], "تحديث صفحة شاشة التوقف": ["optimize/saver.sh update"],
}
# «أزرار الريموت»: one row per app button a remote has (RemoteButtons), each set with `optimize/dock.sh key <button> <package>`
for _id, _title in re.findall(r'b\("([a-z]+)", "([^"]+)"', (ROOT / "android/app/src/main/java/sa/urtv/screensaver/RemoteButtons.java").read_text(encoding="utf-8")):
    MORE_CMDS[_title] = [f"optimize/dock.sh key {_id} org.smarttube.stable", f"optimize/dock.sh key {_id} default"]
# rows that are an action or open a screen of their own, not a value to set (nothing for a command to set)
ACTIONS = {"أضف صوري من الجوال", "احذف صورة", "التطبيقات المخفية", "المظهر على الرئيسية", "اجعل UrTV الشاشة الرئيسية", "جرّب التنبيه", "تخلّص من واجهة المصنع", "رجّع واجهة المصنع", "قبل ما تحذف التطبيق…", "تفاصيل التطبيقات", "افتح تلاوة وأذان",
           "مباريات اليوم", "افتح صفحة الأسواق", "معاينة الشاشة", "تحديث تلفزيوني", "إعداد تلفزيوني", "مسح الملفات المؤقتة", "أرسل تقرير", "أرسل ملاحظة عن النسخة التجريبية", "مركز التحكم", "بحث في الإعدادات",
           "تعديل المفضلة", "ترتيب التطبيقات المفضلة", "ترتيب كل التطبيقات", "أسماء المصادر"}
# rows the code gives no command (no saver.sh case, no receiver extra): set from the TV only
TV_ONLY = {"مسلسلات مختارة", "التحديث التلقائي للصفحة"}
# what a row's choices cannot say by themselves
NOTES = {"شكل كنبة": "تشغيله يضبط مرة وحدة: التخطيط كنبة، الخلفية الآفاق، الساعة زجاجية، شكل التطبيقات أيقونات، ويحفظ اختياراتك؛ وإيقافه يرجّعها كما كانت. وتغيير أي واحد منها وهو مفعّل يبقيه مفعّلًا ويقول «مخصّص»"}


def read(p):
    return p.read_text(encoding="utf-8")


def strings(s):
    """the string literals of a piece of Java, its \\uXXXX escapes decoded"""
    return [re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m.group(1), 16)), x) for x in re.findall(r'"((?:[^"\\]|\\.)*)"', s)]


def array(prefs, name):
    """a Prefs String[] or String[][] constant: a list of strings, or of rows"""
    m = re.search(r"static final String\[\](\[\])? " + name + r" = (\{.*?\});", prefs, re.S)
    if not m:
        sys.exit(f"settings_tree: Prefs has no {name}")
    body = m.group(2)
    if m.group(1):
        return [strings(r) for r in re.findall(r"\{([^{}]*)\}", body)]
    return strings(body)


def default(prefs, getter):
    """the default a Prefs getter returns when nothing is stored: the last literal of its fallback, a constant's first entry, or a boolean"""
    m = re.search(r"\b(?:String|boolean) " + getter + r"\(\) \{(.*?)\n    \}", prefs, re.S) or re.search(r"\b(?:String|boolean) " + getter + r"\(\) \{([^\n]*)\}", prefs)
    if not m:
        return None
    body = m.group(1)
    g = re.search(r'sp\.get(String|Boolean)\("[^"]+",\s*', body)
    if not g:
        return None
    rest, depth, arg = body[g.end():], 0, ""
    for ch in rest:
        if ch == "(":
            depth += 1
        elif ch == ")":
            if depth == 0:
                break
            depth -= 1
        arg += ch
    if g.group(1) == "Boolean":
        return arg.strip() == "true"
    lit = strings(arg)
    if lit:
        return lit[-1]
    c = re.match(r"\s*([A-Z_]+)\[0\]", arg)
    return array(prefs, c.group(1))[0] if c else None


# a stored value that means one of the choices (4.4 «شكل التطبيقات»: "frame" and "zoom", its first values, are the tiles' way: Prefs.appsWay)
STORED_AS = {("شكل التطبيقات", "frame"): "tiles", ("شكل التطبيقات", "zoom"): "tiles"}


def choices(prefs, row):
    if row not in CHOICES:
        return None
    a, b, getter = CHOICES[row]
    if a is None:
        d = default(prefs, getter)
        return [YES, NO], (YES if d else NO)
    rows = array(prefs, a)
    if b:
        pairs = list(zip(rows, array(prefs, b)))
    elif rows and isinstance(rows[0], list):
        pairs = [(r[0], r[1]) for r in rows]
    else:
        pairs = [(v, v) for v in rows]
    d = default(prefs, getter) if getter else pairs[0][0]
    d = STORED_AS.get((row, d), d)
    names = [n for _, n in pairs]
    dn = next((n for v, n in pairs if v == d), None)
    return names, dn


def commands():
    """row name -> ([the agent's commands, each once, in the order docs/SETTINGS.md gives them], how): how is "tv" when its line says the row has
    no command on purpose (the TV only), else "" (the «where» column names the row; the commands are the last column's)"""
    out = {}
    for line in read(ROOT / "docs/SETTINGS.md").splitlines():
        cells = [c.strip() for c in re.split(r"\s\|\s", line.strip().strip("|"))]   # a command's own «on|off» has no spaces round its bar
        if len(cells) < 3 or not line.startswith("|"):
            continue
        for where in (re.sub(r"\s*\([^)]*\)", "", w.split("←")[-1]).replace("\u2066", "").replace("\u2069", "").strip() for w in cells[1].split("،")):
            if not where:
                continue
            cmds, how = out.setdefault(where, ([], ""))
            for c, note in re.findall(r"`(optimize/[a-z_.]+\.(?:sh|py) [^`]*)`(?: \(([^()]{1,140})\))?", cells[-1]):
                c = c + (f"  ({note})" if note else "")
                if re.match(r"optimize/saver\.sh (show|export|import|cities|on|off)\b", c):   # not this row's own setting
                    continue
                if c not in cmds:
                    cmds.append(c)
            if re.search(r"no command|on the TV only|There is no command", cells[-1]):
                out[where] = (cmds, "tv")
    return out


def usages():
    """saver.sh's subcommand -> the values its usage line accepts ("plain|glass"), read from optimize/saver.sh itself"""
    out = {}
    for m in re.finditer(r"^  ([a-z0-9-]+)\)\s.*?usage: optimize/saver\.sh \1 ([^}\"]+)", read(ROOT / "optimize/saver.sh"), re.M):
        out[m.group(1)] = m.group(2).strip()
    return out


SCOPE_WORDS = {"home": "الرئيسية", "saver": "شاشة التوقف", "both": "الرئيسية وشاشة التوقف", "tv": "كل التلفزيون"}


def scopes():
    """row name -> where it shows (SettingsHub.SCOPES: home, saver, both, tv)"""
    src = read(APP / "SettingsHub.java")
    m = re.search(r"static final String\[\]\[\] SCOPES = \{(.*?)\n    \};", src, re.S)
    if not m:
        sys.exit("settings_tree: SettingsHub has no SCOPES")
    return {strings('"' + r + '"')[0]: k for r, k in re.findall(r'\{"([^"]+)", "(home|saver|both|tv)"\}', m.group(1))}


def hub():
    """[(category name, [(level, text, is_header)])] in the first screen's order; a row of ROWS that ORDER does not name goes at its section's end"""
    src = read(APP / "SettingsHub.java")
    keys = dict(re.findall(r'\b([A-Z]+) = "([a-z]+)"', src[:src.index("static final String[][] CATEGORIES")]))
    cats = re.findall(r'\{([A-Z]+), "([^"]+)", "[^"]*", null, "[^"]*"\}', src)
    order = {}
    for m in re.finditer(r"\{([A-Z]+), ((?:H \+ )?\"[^}]*)\}", src[src.index("static final String[][] ORDER"):src.index("static int place(") if "static int place(" in src else None]):
        items = re.findall(r'(H \+ )?"([^"]*)"', m.group(2))
        order[keys[m.group(1)]] = [(bool(h), strings('"' + t + '"')[0]) for h, t in items]
    rows = [(strings('"' + r + '"')[0], c) for r, c in re.findall(r'\{"([^"]+)", ([A-Z]+)\}', src[src.index("static final String[][] ROWS"):src.index("static final String[][] HEADERS")])]
    out = []
    for k, name in cats:
        key = keys[k]
        if key == "search":
            continue
        items = list(order.get(key, []))
        named = {t for h, t in items if not h}
        items += [(False, r) for r, c in rows if keys.get(c) == key and r not in named]
        out.append((name, items))
    return out


def build():
    prefs = read(APP / "Prefs.java")
    cmds, uses, scope = commands(), usages(), scopes()
    cases = set(re.findall(r"^  ([a-z0-9-]+)\)", read(ROOT / "optimize/saver.sh"), re.M))
    bad = [c for v in MORE_CMDS.values() for c in v if c.startswith("optimize/saver.sh ") and c.split()[1] not in cases]
    if bad:
        sys.exit("settings_tree: no such saver.sh command: " + ", ".join(bad))
    schema = read(ROOT / "android/launcher/src/main/java/sa/urtv/home/phone/Schema.java")
    phone_keys = re.findall(r'case "([a-z_]+)":', schema[schema.index("private static Clean set("):])
    control_rows = strings(re.search(r"static final String\[\] ROWS = (\{[^}]*\})", read(ROOT / "android/launcher/src/main/java/sa/urtv/home/ControlCenter.java")).group(1))
    unknown = [k for k in phone_keys if k not in PHONE] + [r for r in control_rows if r not in CONTROL]
    if unknown:
        sys.exit("settings_tree: say which row these reach (PHONE / CONTROL): " + ", ".join(unknown))
    by_phone, by_control = {}, {}
    for k in phone_keys:
        if PHONE[k]:
            by_phone.setdefault(PHONE[k], []).append(k)
    for r in control_rows:
        if CONTROL[r]:
            by_control.setdefault(CONTROL[r], []).append(r)
    lines = ["الإعدادات"]
    def section(top, items, lines):
        depth = top + 1
        for header, text in items:
            if header:
                lines.append("—" * (top + 1) + " " + text)
                depth = top + 2
                continue
            row(text, depth, lines)
    def row(text, depth, lines):
        if True:
                c = choices(prefs, text)
                lines.append("—" * depth + " " + text + (f" [{SCOPE_WORDS[scope[text]]}]" if text in scope else ""))
                pad = " " * (depth + 1)
                if c:
                    names, d = c
                    lines.append(pad + "الخيارات: " + " / ".join(names))
                    if d:
                        lines.append(pad + "الأصل: " + d)
                if text in NOTES:
                    lines.append(pad + NOTES[text])
                cl, how = cmds.get(text.replace("\u2066", "").replace("\u2069", ""), ([], ""))
                cl = cl + [c for c in MORE_CMDS.get(text, []) if c not in cl]
                for cmd in cl:
                    lines.append(pad + "مثال للوكيل: " + cmd)
                    sub = cmd.split()[1] if cmd.startswith("optimize/saver.sh ") and len(cmd.split()) > 1 else None   # the values its usage line gives
                    if sub in uses and "<" not in cmd:
                        lines.append(pad + "  القيم المقبولة: " + uses[sub])
                if not cl:
                    lines.append(pad + ("إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)" if text in ACTIONS
                                        else "من التلفزيون فقط (بلا أمر، عن قصد)" if how == "tv" or text in TV_ONLY else "لا أمر موثّق بعد"))
                if text in by_phone:
                    lines.append(pad + "صفحة الجوال: " + ", ".join(by_phone[text]))
                if text in by_control:
                    lines.append(pad + "مركز التحكم: " + ", ".join(by_control[text]))

    lines.append("— بحث في الإعدادات")   # the first button (the wide one) and the side list's first item: a word finds a row in any section
    for cat, items in hub():
        lines.append("— " + cat)
        section(1, items, lines)
    head = ("# شجرة الإعدادات\n\n"
            "كل إعداد في التلفزيون بمكانه الكامل، مولّدة من الكود (`.github/tools/settings_tree.py`: SettingsHub وPrefs وdocs/SETTINGS.md وصفحة الجوال ومركز التحكم).\n"
            "لا تعدّلها باليد: شغّل الأداة بعد أي تغيير في الإعدادات، و`tests/settings_tree_test.py` يفشل إذا صارت قديمة.\n\n"
            "بين القوسين أين يظهر الإعداد. «الأصل» القيمة قبل أي تغيير، و«مثال للوكيل» أمر واحد كمثال وتحته القيم اللي يقبلها.\n\n"
            "Every setting of the TV with its full path, generated from the code; do not edit by hand. [ ] says where it shows (the home, the screensaver,\n"
            "both, or the whole TV); «الأصل» is the default, «مثال للوكيل» one example of the agent's command with the values it accepts under it; «صفحة الجوال»\n"
            "the phone page's key, «مركز التحكم» the control centre's row that opens it; «من التلفزيون فقط» a row with no command on purpose.\n\n")
    return (head + "## شجرة واحدة لكل أشكال شاشة الإعدادات (أزرار، كنبة، tvOS)\n\n"
            + "٤.٤ التجريبية ٤ (مراجعة سهولة الوصول، docs/research/SETTINGS-FINDABILITY.md): الأقسام التسعة نفسها في كل شكل؛ «أزرار» وtvOS تعرضها أزرارًا في أول شاشة،\n"
            + "و«كنبة» قائمة جانبية. لا شيء أعمق من: القسم ← الصف ← اختياره.\n\n```text\n" + "\n".join(lines) + "\n```\n")


def main():
    text = build()
    if "--check" in sys.argv:
        if not OUT.is_file() or read(OUT) != text:
            print("docs/SETTINGS-TREE.md is out of date: python3 .github/tools/settings_tree.py")
            return 1
        print("docs/SETTINGS-TREE.md is current")
        return 0
    OUT.write_text(text, encoding="utf-8")
    print(f"wrote {OUT.relative_to(ROOT)} ({text.count(chr(10))} lines)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
