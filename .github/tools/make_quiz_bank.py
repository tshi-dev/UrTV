#!/usr/bin/env python3
"""«الأسرع»: the quiz bank in the compact form the TV reads (docs/QUIZ.md).
   python3 .github/tools/make_quiz_bank.py games/data/quiz.json OUT.json                  the whole verified bank, compact (published as quiz/quiz.json on the data-games branch)
   python3 .github/tools/make_quiz_bank.py games/data/quiz.json assets/quiz/starter.json --starter   the small bundled set, so the first game works offline
The compact item: {"i": id, "c": category, "d": 1..3, "k": 1 for kids, "q": [ar, en], "a": [ar, en], "w": [[ar, en] x3]}. Only verified quiz items with both languages and
3 wrong options are kept; the file is sorted by id so a rebuild with the same bank is the same file (no noise in the data branch)."""
import json, random, sys

def compact(items):
    out = []
    for it in items:
        if it.get("type") != "quiz" or not it.get("verified"): continue
        q, a, w = it.get("question") or {}, it.get("answer") or {}, it.get("wrong") or []
        if not (q.get("ar") and q.get("en") and a.get("ar") and a.get("en")) or len(w) != 3 or any(not (x.get("ar") and x.get("en")) for x in w): continue
        if len({a["ar"], *(x["ar"] for x in w)}) != 4: continue
        out.append({"i": it["id"], "c": it["category"], "d": int(it["difficulty"]), "k": 1 if it.get("age") == "kids" else 0,
                    "q": [q["ar"], q["en"]], "a": [a["ar"], a["en"]], "w": [[x["ar"], x["en"]] for x in w]})
    return sorted(out, key=lambda x: x["i"])

def starter(bank, per_level=70, kids_min=45, seed=215):
    """at least 40 of each level and 40 kids items (so «مزيج» works with any level and the kids filter), a few of every category"""
    rnd = random.Random(seed)
    chosen = {}
    def take(pool, n):
        pool = [x for x in pool if x["i"] not in chosen]
        rnd.shuffle(pool)
        for x in pool[:n]: chosen[x["i"]] = x
    for cat in sorted({x["c"] for x in bank}):
        if cat not in ("geography",): take([x for x in bank if x["c"] == cat], 10)   # a few of every other category
    for lv in (1, 2, 3): take([x for x in bank if x["d"] == lv], per_level - sum(1 for x in chosen.values() if x["d"] == lv))
    take([x for x in bank if x["k"]], max(0, kids_min - sum(1 for x in chosen.values() if x["k"])))
    return sorted(chosen.values(), key=lambda x: x["i"])

if __name__ == "__main__":
    src, dst = sys.argv[1], sys.argv[2]
    bank = compact(json.load(open(src, encoding="utf-8")))
    if "--starter" in sys.argv: bank = starter(bank)
    json.dump(bank, open(dst, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print(len(bank), "items")
