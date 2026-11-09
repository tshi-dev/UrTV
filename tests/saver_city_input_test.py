"""4.3 R1-2 / R1-3: `saver.sh city` (SaverReceiver's SET) refuses lat without lon (or lon without lat) instead of dropping it and answering
«saved», and refuses coordinates off the Earth (lat beyond ±90, lon beyond ±180); both before anything is saved. No TV needed.

    python3 tests/saver_city_input_test.py
"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
src = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SaverReceiver.java").read_text(encoding="utf-8")
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

one = src.find('if ((lat == null) != (lon == null)) return "error: lat and lon go together')
rng = src.find('return "error: lat is -90 to 90 and lon is -180 to 180"')
num = src.find('return "error: lat/lon must be numbers')
first_save = src.find("p.setCity(")
check("one of lat/lon alone is an error, not a silent drop", one >= 0)
check("the range is checked after the number pattern (so the parse cannot fail) and is ±90 / ±180",
      0 <= num < rng and re.search(r"Math\.abs\(Double\.parseDouble\(lat\)\) > 90 \|\| Math\.abs\(Double\.parseDouble\(lon\)\) > 180", src))
check("both checks come before anything is saved", 0 <= one < first_save and 0 <= rng < first_save, (one, rng, first_save))

# the same rules on the values (a mirror of the Java: pattern, then range)
def verdict(lat, lon):
    if (lat is None) != (lon is None): return "together"
    if lat is None: return "ok"
    if not re.fullmatch(r"-?\d{1,2}(\.\d+)?", lat) or not re.fullmatch(r"-?\d{1,3}(\.\d+)?", lon): return "numbers"
    return "range" if abs(float(lat)) > 90 or abs(float(lon)) > 180 else "ok"
cases = {("25.86", "43.49"): "ok", ("25.86", None): "together", (None, "43.49"): "together", ("95", "43"): "range", ("25", "181.5"): "range",
         ("-90", "-180"): "ok", ("x", "1"): "numbers"}
bad = {k: (verdict(*k), v) for k, v in cases.items() if verdict(*k) != v}
check("the cases: Buraydah ok, one alone refused, 95° and 181.5° refused, the edges ±90 / ±180 ok", not bad, bad)
sys.exit(1 if failed else 0)
