#!/usr/bin/env python3
"""docs/CITIES.md is exactly what .github/tools/cities_md.py writes from Cities.java, and it names every built-in city.
After a change to the list: python3 .github/tools/cities_md.py   Run: python3 tests/cities_md_test.py"""
import importlib.util
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("cities_md", ROOT / ".github/tools/cities_md.py")
md = importlib.util.module_from_spec(spec)
spec.loader.exec_module(md)

failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + detail))
    if not ok:
        failed += 1


regs = md.regions()
cities = [c for _, cs in regs for c, _ in cs]
check("Cities.java read: 26 regions and countries, over 300 cities", len(regs) >= 26 and len(cities) > 300, f"{len(regs)} / {len(cities)}")
doc = (ROOT / "docs/CITIES.md").read_text(encoding="utf-8") if (ROOT / "docs/CITIES.md").exists() else ""
check("docs/CITIES.md is up to date (run: python3 .github/tools/cities_md.py)", doc == md.render(), "out of date")
missing = [c for c in cities if c not in doc]
check("every built-in city is named in docs/CITIES.md", not missing, ", ".join(missing[:10]))
world = md.world_regions()
check("every world country is documented in app order with its calculation", len(world) == 230 and all(doc.index('## ' + a[0] + ' (') < doc.index('## ' + b[0] + ' (') for a, b in zip(world, world[1:])) and all(md.method_names()[method] in doc for _, _, method, _ in world), "world country order/method")
check("all 3307 world cities are documented in both languages", sum(len(cities) for _, _, _, cities in world) == 3307 and all(c[0] + ' · ' + c[4] in doc for _, _, _, cities in world for c in cities), "world names")
table, default = md.authorities()
check("the calculations are read from PrayerMethod.authority (Umm al-Qura by default, Oman's ministry for Muscat)",
      default == "أم القرى" and table.get("Asia/Muscat", "").startswith("وزارة الأوقاف"), f"{default} {table}")
sys.exit(1 if failed else 0)
