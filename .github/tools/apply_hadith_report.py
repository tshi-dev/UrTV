#!/usr/bin/env python3
"""Apply the CI hadith check (data-hadith branch, hadith/report.json) to assets/shared/hadith.json.

Keeps only the hadith found in the six books, records where, and writes the source line from that check:
the first two books in the usual order (رواه البخاري ومسلم), or the one book. Also keeps the full narration
the check picked (isnad and all, exactly as the book has it) with its standard number; two excerpts of the
same narration become one entry, and so do two texts found in the same book under the same number. A narration
Al-Albani does not grade Sahih, Hasan or Hasan Sahih (any of the four Sunan) is dropped.

usage: git show origin/data-hadith:hadith/report.json > /tmp/report.json
       python3 .github/tools/apply_hadith_report.py /tmp/report.json
"""
import json
import sys
from pathlib import Path

from verify_hadith import albani_accepts

ORDER = [("صحيح البخاري", "البخاري"), ("صحيح مسلم", "مسلم"), ("سنن أبي داود", "أبو داود"),
         ("جامع الترمذي", "الترمذي"), ("سنن النسائي", "النسائي"), ("سنن ابن ماجه", "ابن ماجه")]
DATA = Path(__file__).resolve().parents[2] / "assets/shared/hadith.json"


def standard(f, old):
    """The number the screen prints. Sahih Muslim's rows in the dataset are numbered by a running index; only the
    standard (Abd al-Baqi) number may be printed: the check's `standard`, else `arabicnumber` (55.01 -> 55), else the
    number already in hadith.json for this text in this book (a hand-checked fix), else None. Never the index."""
    if f["book"] != "صحيح مسلم":
        return f["number"]
    if f.get("standard") is not None:
        return int(f["standard"])
    if f.get("arabicnumber"):
        return int(float(f["arabicnumber"]))
    kept = (old or {}).get("full") or {}
    if kept.get("book") == f["book"] and kept.get("text") == f["text"] and kept.get("number") != f["number"]:
        return kept["number"]
    return None


def main():
    report = {r["text"]: r["found"] for r in json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))}
    narration = {r["text"]: r.get("full") for r in json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))}
    kept, dropped, seen, weak, numbers = [], [], {}, [], {}
    for h in json.loads(DATA.read_text(encoding="utf-8")):
        found = report.get(h["text"])
        if found is None:
            sys.exit(f"not in the report (run the check again): {h['text']}")
        books = {short: sorted({f["number"] for f in found if f["book"] == full}) for full, short in ORDER}
        books = {k: v for k, v in books.items() if v}
        if not books:
            dropped.append(h["text"])
            continue
        same = [numbers[(b, n)] for b, ns in books.items() for n in ns if (b, n) in numbers]
        if same:
            print(f"same hadith as «{same[0]}»: {h['text']}")
            continue
        names = list(books)[:2]
        entry = {"text": h["text"], "source": "رواه " + " و".join(names), "found": books}
        f = narration[h["text"]]
        if not f or not albani_accepts(f["book"], f.get("grades")):
            weak.append(h["text"])
            continue
        number = standard(f, h)
        if number is None:
            weak.append(h["text"] + f"  (no standard number for {f['book']} row {f['number']})")
            continue
        key = (f["book"], number)
        if key in seen:
            print(f"same narration as «{seen[key]}»: {h['text']}")
            continue
        seen[key] = h["text"]
        entry["full"] = {"book": f["book"], "number": number, "text": f["text"]}
        if f.get("grades") is not None:   # the four Sunan: the scholars' grades of this narration
            entry["full"]["grades"] = f["grades"]
        numbers.update({(b, n): h["text"] for b, ns in books.items() for n in ns})
        kept.append(entry)
    DATA.write_text(json.dumps(kept, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"kept {len(kept)}, dropped {len(dropped)} (not found in these words):")
    for t in dropped:
        print("  ", t)
    print(f"dropped {len(weak)} (no narration Al-Albani grades Sahih, Hasan or Hasan Sahih):")
    for t in weak:
        print("  ", t)


if __name__ == "__main__":
    main()
