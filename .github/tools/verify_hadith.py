#!/usr/bin/env python3
"""Check every hadith shown on the screen against the Sunni collections -> hadith/report.json.

Runs in CI (.github/workflows/hadith.yml): the texts are unreachable from the dev container. Each hadith in
assets/shared/hadith.json is searched (letters only, harakat and punctuation ignored) in the Arabic text of
the six books from github.com/fawazahmed0/hadith-api, and every book and hadith number it appears in is
recorded. A hadith found in none of them must not be shown.

For the four Sunan the scholars' grades of each narration (Al-Albani and others, as the API's English editions
carry them, matched by hadith number) are read too, and kept in the report. A Sunan narration is accepted only
when Al-Albani grades it Sahih, Hasan or Hasan Sahih (albani_accepts); one he grades weak in any form, or does
not grade, is not picked when another holds the words, and is dropped by apply_hadith_report.py when none does.

For each found hadith the full narration is kept too, exactly as the book has it (chain of narrators and all):
one whose isnad is at most ISNAD_MAX_WORDS words (a very long chain is not picked when a shorter one exists), in
the first book in the order below that has one, then the shortest (in al-Tirmidhi, one he grades صحيح or حسن
first), so the screen can show the whole hadith, not an excerpt.
"""
import json
import re
import time
import urllib.request
from pathlib import Path

BOOKS = {  # edition -> Arabic name
    "ara-bukhari": "صحيح البخاري", "ara-muslim": "صحيح مسلم", "ara-abudawud": "سنن أبي داود",
    "ara-tirmidhi": "جامع الترمذي", "ara-nasai": "سنن النسائي", "ara-ibnmajah": "سنن ابن ماجه",
}
URL = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/{}.min.json"
ISNAD_MAX_WORDS = 35   # the words before the Prophet's first quoted words; src/build.py has the same limit
GRADED = {"ara-abudawud": "eng-abudawud", "ara-tirmidhi": "eng-tirmidhi", "ara-nasai": "eng-nasai",
          "ara-ibnmajah": "eng-ibnmajah"}   # Bukhari and Muslim are sahih by their own standard
ALBANI_OK = {"sahih", "hasan", "hasan sahih"}   # src/build.py has the same rule


def albani_accepts(book, grades):
    """Bukhari and Muslim: yes. The four Sunan: only if Al-Albani grades the narration Sahih, Hasan or Hasan Sahih."""
    if book in ("صحيح البخاري", "صحيح مسلم"):
        return True
    albani = [g.get("grade", "") for g in grades or [] if "albani" in g.get("name", "").lower()]
    return bool(albani) and all(" ".join(re.sub(r"[^a-z ]", " ", a.lower()).split()) in ALBANI_OK for a in albani)


def standard_number(rows, n):
    """The standard (Abd al-Baqi) number of row n of a book whose rows carry `arabicnumber` (Sahih Muslim: "55.01").
    Some rows have none; their running index (hadithnumber) is NOT a number anyone can look up. Then: the next row of
    the same text if it has one; else the numbered rows around it, when they leave one number only (2699.02 | ? | 2701
    -> 2700); else None (do not guess: 2564.03 | ? | 2565.03 could be either)."""
    def num(r):
        a = (r or {}).get("arabicnumber")
        return int(float(a)) if a not in (None, "") else None
    row = rows.get(n)
    if num(row) is not None:
        return num(row)
    nxt = rows.get(n + 1)
    if row and nxt and nxt.get("text") == row.get("text") and num(nxt) is not None:
        return num(nxt)
    keys = sorted(rows)
    before = [num(rows[k]) for k in keys if k < n and num(rows[k]) is not None]
    after = [num(rows[k]) for k in keys if k > n and num(rows[k]) is not None]
    if before and after and after[0] == before[-1] + 2:
        return before[-1] + 1
    return None


def norm(s):
    s = re.sub("[ؐ-ًؚ-ٰٟۖ-ۭـ]", "", s)   # harakat, tatweel
    s = re.sub("[إأآٱ]", "ا", s).replace("ى", "ي").replace("ة", "ه").replace("ؤ", "و").replace("ئ", "ي")
    return " ".join(re.sub("[^ء-ي ]", " ", s).split())


def get(url):
    for wait in (0, 3, 10, 30):
        time.sleep(wait)
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "tv"}), timeout=120) as r:
                return json.load(r)
        except Exception as e:  # noqa: BLE001
            err = e
    raise err


def main():
    wanted = json.loads(Path("assets/shared/hadith.json").read_text(encoding="utf-8"))
    books, raw = {}, {}
    for ed, name in BOOKS.items():
        hadiths = get(URL.format(ed))["hadiths"]
        if ed == "ara-bukhari":
            print("fields:", sorted(hadiths[0]))
        raw[ed] = {h["hadithnumber"]: h for h in hadiths}
        books[ed] = [(h["hadithnumber"], norm(h.get("text") or "")) for h in hadiths]
        print(name, len(books[ed]), "hadith")
    grades = {}
    for ed, eng in GRADED.items():
        try:
            grades[ed] = {h["hadithnumber"]: h.get("grades") or [] for h in get(URL.format(eng))["hadiths"]}
            sample = next((g for g in grades[ed].values() if g), [])
            print(eng, len(grades[ed]), "hadith; grades like", sample[:2])
        except Exception as e:  # noqa: BLE001  (no grades: the narration is then picked without them)
            print(eng, "unavailable:", e)
    grade_of = lambda ed, n: grades.get(ed, {}).get(n)
    report = []
    for h in wanted:
        key = norm(h["text"])
        found = [{"book": BOOKS[ed], "edition": ed, "number": n}
                 for ed, rows in books.items() for n, t in rows if key in t]
        full = None
        if found:
            graded = lambda t: "صحيح" in norm(t) or "حسن" in norm(t)   # al-Tirmidhi's own grading
            isnad = lambda t: len(t.split('"')[0].split())
            order = list(BOOKS)
            ed, n = min(((f["edition"], f["number"]) for f in found), key=lambda e: (
                not albani_accepts(BOOKS[e[0]], grade_of(*e)), isnad(raw[e[0]][e[1]]["text"]) > ISNAD_MAX_WORDS, order.index(e[0]),
                not graded(raw[e[0]][e[1]]["text"]), len(raw[e[0]][e[1]]["text"])))
            src = raw[ed][n]
            full = {"book": BOOKS[ed], "number": n, "arabicnumber": src.get("arabicnumber"),
                    "standard": standard_number(raw[ed], n) if ed == "ara-muslim" else n,
                    "reference": src.get("reference"), "grades": grade_of(ed, n), "text": src["text"]}
        report.append({"text": h["text"], "found": found, "full": full})
        print("OK " if found else "NOT FOUND", h["text"], [(f["book"], f["number"]) for f in found][:6])
    Path("hadith").mkdir(exist_ok=True)
    Path("hadith/report.json").write_text(json.dumps(report, ensure_ascii=False, indent=1), encoding="utf-8")


if __name__ == "__main__":
    main()
