#!/usr/bin/env python3
"""Sahih Muslim's standard numbers in the hadith check (verify_hadith.standard_number, apply_hadith_report.standard):
the dataset's running index is never printed. Made-up rows, no network."""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / ".github/tools"))
import verify_hadith as vh   # noqa: E402
import apply_hadith_report as ap   # noqa: E402

fails = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  [{detail}]"))
    if not ok:
        fails.append(name)


def rows(*r):
    return {n: {"hadithnumber": n, "arabicnumber": a, "text": t} for n, a, t in r}


R = rows((10, "2699.02", "a"), (11, None, "b"), (12, "2701", "c"),
         (20, "2731", "d"), (21, None, "e"), (22, "2732.03", "e"),
         (30, "2564.03", "f"), (31, None, "g"), (32, "2565.03", "h"),
         (40, "3005", "i"), (41, None, "j"), (50, "3014", "k"), (60, "55.01", "l"))
check("a numbered row: its own number (55.01 -> 55)", vh.standard_number(R, 60) == 55, vh.standard_number(R, 60))
check("no number, the rows around leave one (2699.02 | ? | 2701 -> 2700)", vh.standard_number(R, 11) == 2700,
      vh.standard_number(R, 11))
check("no number, the next row of the same text has one (2732.03 -> 2732)", vh.standard_number(R, 21) == 2732,
      vh.standard_number(R, 21))
check("no number and two possible (2564.03 | ? | 2565.03): None, not a guess", vh.standard_number(R, 31) is None)
check("no number and a wide gap (3005 | ? | 3014): None", vh.standard_number(R, 41) is None)
check("never the running index", all(vh.standard_number(R, n) not in (11, 21, 31, 41) for n in R))

MUSLIM = "صحيح مسلم"
f = {"book": MUSLIM, "number": 6989, "arabicnumber": None, "standard": 2759, "text": "t"}
check("apply: the check's standard number", ap.standard(f, None) == 2759)
f = {"book": MUSLIM, "number": 534, "arabicnumber": "223", "text": "t"}
check("apply: an older report without `standard`: arabicnumber", ap.standard(f, None) == 223)
f = {"book": MUSLIM, "number": 7512, "arabicnumber": None, "standard": None, "text": "t"}
old = {"full": {"book": MUSLIM, "number": 3006, "text": "t"}}
check("apply: no standard number: the hand-checked one already in hadith.json", ap.standard(f, old) == 3006)
check("apply: no standard number and none kept: None (the entry is dropped, never the index)",
      ap.standard(f, None) is None and ap.standard(f, {"full": {"book": MUSLIM, "number": 7512, "text": "t"}}) is None)
check("apply: the other books keep their number", ap.standard({"book": "صحيح البخاري", "number": 1, "text": "t"}, None) == 1)

# the owner's rule (2026-11-27): a recognised scholar's Sahih or Hasan keeps a narration
SUNAN = "سنن ابن ماجه"
check("accept: Al-Albani Hasan, others weak (Ibn Majah 4181)", vh.scholar_accepts(SUNAN, [
    {"name": "Al-Albani", "grade": "Hasan"}, {"name": "Shuaib Al Arnaut", "grade": "Daif"}]))
check("accept: no Al-Albani, Shuaib al-Arnaut Hasan Lighairihi", vh.scholar_accepts(SUNAN, [{"name": "Shuaib Al Arnaut", "grade": "Hasan Lighairihi"}]))
check("refuse: weak for everyone, or only Zubair Ali Zai Sahih", not vh.scholar_accepts(SUNAN, [{"name": "Al-Albani", "grade": "Daif"}])
      and not vh.scholar_accepts(SUNAN, [{"name": "Zubair Ali Zai", "grade": "Sahih"}]))
check("refuse: a chain-only grade (Sahih Isnaad) is not a grade of the hadith", not vh.scholar_accepts(SUNAN, [{"name": "Al-Albani", "grade": "Sahih Isnaad"}]))

sys.exit(1 if fails else 0)
