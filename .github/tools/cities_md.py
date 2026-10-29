#!/usr/bin/env python3
"""Write docs/CITIES.md, the list of built-in cities a person can read, from Cities.java (the list the apps use) and the
calculation each country gets by default (PrayerMethod.authority, read from its switch). tests/cities_md_test.py fails when the
two drift apart.   usage: python3 .github/tools/cities_md.py [--check]"""
import re
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
    return out


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
        names = sorted({table.get(tz, default) for _, tz in cities})
        lines += [f"## {region} ({n(len(cities))}) · حساب {' / '.join(names)}", "", "، ".join(name for name, _ in cities), ""]
    lines += ["</div>", ""]
    return "\n".join(lines)


if __name__ == "__main__":
    text = render()
    if "--check" in sys.argv:
        sys.exit(0 if OUT.exists() and OUT.read_text(encoding="utf-8") == text else 1)
    OUT.write_text(text, encoding="utf-8")
    print(f"wrote {OUT.relative_to(ROOT)}")
