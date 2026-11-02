#!/usr/bin/env python3
"""Write docs/CITIES.md, the list of built-in cities a person can read, from Cities.java (the list the apps use) and the
calculation each country gets by default (PrayerMethod.authority, read from its switch). tests/cities_md_test.py fails when the
two drift apart.   usage: python3 .github/tools/cities_md.py [--check]"""
import re
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
OUT = ROOT / "docs/CITIES.md"
DIGITS = str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩")


def regions():
    """[(region name, [(city, tz or None), ...]), ...] in the app's order."""
    src = (JAVA / "Cities.java").read_text(encoding="utf-8")
    out = []
    for m in re.finditer(r'new Region\("([^"]+)", new String\[\]\[\] \{(.*?)\}\)', src, re.S):
        rows = re.findall(r'\{"([^"]+)", "[-\d.]+", "[-\d.]+"(?:, "([^"]+)")?\}', m.group(2))
        out.append((m.group(1), [(name, tz or None) for name, tz in rows]))
    out.extend((name, [(c[0], c[3]) for c in cities]) for name, _, _, cities in world_regions())
    return out


def world_regions():
    """Read the packed strings the app actually decodes, not a second city list."""
    src = (JAVA / 'Cities.java').read_text(encoding='utf-8')
    result = []
    string = r'"(?:[^"\\]|\\.)*"'
    pattern = r'new Region\((' + string + r'), "([A-Z]{2})", (\d+), unpack\("\2", new String\[\] \{(.*?)\}\)\)'
    for match in re.finditer(pattern, src, re.S):
        cities = []
        for literal in re.findall(string, match[4]):
            cities.extend(line.split('|') for line in json.loads(literal).splitlines())
        result.append((json.loads(match[1]), match[2], int(match[3]), cities))
    return result


def method_names():
    src = (JAVA / 'PrayerMethod.java').read_text(encoding='utf-8')
    body = src[src.index('static String authority(int'):]
    return {int(i): name for i, name in re.findall(r'case (\d+): return "([^"]+)";', body)}


def authorities():
    """time zone -> the calculation's name, and the default for every other zone."""
    src = (JAVA / "PrayerMethod.java").read_text(encoding="utf-8")
    body = src[src.index("static String authority("):]
    body = body[:body.index("default:") + 60]
    table = {}
    for cases, name in re.findall(r'((?:case "[^"]+":\s*)+)return "([^"]+)";', body):
        for tz in re.findall(r'case "([^"]+)"', cases):
            table[tz] = name
    default = re.search(r'default: return "([^"]+)";', body).group(1)
    return table, default


def render():
    table, default = authorities()
    world = {name: (method, cities) for name, _, method, cities in world_regions()}
    methods = method_names()
    regs = regions()
    total = sum(len(c) for _, c in regs)
    saudi = [(r, c) for r, c in regs if all(tz is None for _, tz in c)]
    others = [(r, c) for r, c in regs if any(tz for _, tz in c)]
    n = lambda k: str(k).translate(DIGITS)
    lines = [
        "<div dir=\"rtl\">",
        "",
        "# المدن الموجودة في تلفزيونك",
        "",
        f"فيه **{n(total)} مدينة** جاهزة في القائمة، في {n(len(others) + 1)} دولة. تختارها من الإعدادات ← الموقع والصلاة: الدولة ثم المدينة.",
        "",
        "- المدينة تحدد الوقت والطقس، وطريقة الحساب تمشي على دولة المدينة (تقدر تغيّرها من «طريقة الحساب»).",
        "- مدينتك مو موجودة؟ ابحث عنها من نفس الشاشة، أو قل لوكيلك: `optimize/saver.sh city 'اسم المدينة' خط_العرض خط_الطول المنطقة_الزمنية`.",
        "- هذي الصفحة تنكتب من قائمة التطبيق نفسها (`Cities.java`)، فما تختلف عنها.",
        "",
        f"## السعودية ({n(sum(len(c) for _, c in saudi))} مدينة) · حساب {default}",
        "",
    ]
    for region, cities in saudi:
        lines += [f"**{region}** ({n(len(cities))}): " + "، ".join(name for name, _ in cities), ""]
    for region, cities in others:
        if region in world:
            method, packed = world[region]
            names = [methods[method]]
            labels = [c[0] + ' · ' + c[4] for c in packed]
        else:
            names = sorted({table.get(tz, default) for _, tz in cities})
            labels = [name for name, _ in cities]
        lines += [f"## {region} ({n(len(cities))}) · حساب {' / '.join(names)}", "", "، ".join(labels), ""]
    lines += ["</div>", ""]
    return "\n".join(lines)


if __name__ == "__main__":
    text = render()
    if "--check" in sys.argv:
        sys.exit(0 if OUT.exists() and OUT.read_text(encoding="utf-8") == text else 1)
    OUT.write_text(text, encoding="utf-8")
    print(f"wrote {OUT.relative_to(ROOT)}")
