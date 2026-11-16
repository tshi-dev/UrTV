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
    "الشكل الجاهز": ("LOOKS", None, None),
    "التخطيط": ("HOME_LAYOUTS", "HOME_LAYOUT_NAMES", "homeLayout"),
    "الخلفية": ("HOME_BGS", "HOME_BG_NAMES", "homeBg"),
    "الساعة": ("HOME_CLOCKS", "HOME_CLOCK_NAMES", "homeClock"),
    "شكل التطبيقات": ("HOME_APPS", "HOME_APPS_NAMES", "appsStyle"),
    "حزم المشاهد": ("SCENE_PACKS", None, None),
    "بقاء كل مشهد": ("SCENE_STAYS", "SCENE_STAY_NAMES", "sceneStay"),
    "المشهد يتبع الوقت": (None, None, "sceneByTime"),
    "المشهد يتبع الطقس": (None, None, "sceneByWeather"),
    "شكل البطاقة": ("CARD_STYLES", None, "cardStyle"),
    "حجم الساعة": ("CLOCK_SIZES", None, "clockSize"),
    "عرض الأسواق": ("MK_STYLES", None, "mkStyle"),
    "تغيّر الأسهم": ("STK_CHG", None, "stkChg"),
    "الخانة اليسرى": ("SLOTS", None, "slot"),
    "المناسبات الوطنية": ("NATIONAL", None, "national"),
    "تطبيقات في الشريط": ("FAV_BARS", None, "favBar"),
}
# the phone page's «set» keys (phone/Schema.java) and the row each one changes; None: not a row of the settings (a one-off action)
PHONE = {"city": "المدينة", "home_cards": "البطاقة الأولى", "adhan_on": "الأذان", "adhan_short": "الأذان", "adhan_stopkey": "الأذان", "adhan_voice": "الأذان",
         "adhan_fajr_voice": "الأذان", "adhan_volume": "الأذان", "sleep_minutes": None, "saver_idle": "بدء شاشة التوقف بعد", "lang": "اللغة",
         "fav_bar": "تطبيقات في الشريط"}
# the control centre's rows (ControlCenter.ROWS) that open a setting's row; the others are actions or screens of their own
CONTROL = {"net": None, "sound": None, "speed": None, "ours": None, "games": None, "city": "المدينة", "tv": None, "saver": None}
YES, NO = "مفعّل", "متوقف"


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
    names = [n for _, n in pairs]
    dn = next((n for v, n in pairs if v == d), None)
    return names, dn


def commands():
    """row name -> the agent's command, from docs/SETTINGS.md's tables (the «where» column names the row, the first command in the last column)"""
    out = {}
    for line in read(ROOT / "docs/SETTINGS.md").splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 3 or not line.startswith("|"):
            continue
        where = cells[1].split("←")[-1].strip()
        cmd = re.search(r"`(optimize/[a-z_.]+\.(?:sh|py) [^`]*)`", cells[-1])
        if where and cmd and where not in out:
            out[where] = cmd.group(1)
    return out


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
    cmds = commands()
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
    for cat, items in hub():
        lines.append("— " + cat)
        depth = 2
        for header, text in items:
            if header:
                lines.append("—— " + text)
                depth = 3
                continue
            c = choices(prefs, text)
            line = "—" * depth + " " + text
            if c:
                names, d = c
                line += ": " + " / ".join(names) + (f" (الأصل: {d})" if d else "")
            lines.append(line)
            pad = " " * (depth + 1)
            if text in cmds:
                lines.append(pad + "الوكيل: " + cmds[text])
            if text in by_phone:
                lines.append(pad + "صفحة الجوال: " + ", ".join(by_phone[text]))
            if text in by_control:
                lines.append(pad + "مركز التحكم: " + ", ".join(by_control[text]))
    head = ("# شجرة الإعدادات\n\n"
            "كل إعداد في التلفزيون بمكانه الكامل، مولّدة من الكود (`.github/tools/settings_tree.py`: SettingsHub وPrefs وdocs/SETTINGS.md وصفحة الجوال ومركز التحكم).\n"
            "لا تعدّلها باليد: شغّل الأداة بعد أي تغيير في الإعدادات، و`tests/settings_tree_test.py` يفشل إذا صارت قديمة.\n\n"
            "Every setting of the TV with its full path, generated from the code; do not edit by hand. «الوكيل» is the agent's command, «صفحة الجوال» the phone\n"
            "page's key, «مركز التحكم» the control centre's row that opens it; «الأصل» is the default.\n\n")
    return head + "```text\n" + "\n".join(lines) + "\n```\n"


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
