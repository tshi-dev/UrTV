#!/usr/bin/env python3
"""«الأسرع»: the quiz bank in the compact form the TV reads (docs/QUIZ.md).
   python3 .github/tools/make_quiz_bank.py games/data/quiz.json OUT.json                  the whole verified bank, compact (published as quiz/quiz.json on the data-games branch)
   python3 .github/tools/make_quiz_bank.py games/data/quiz.json assets/quiz/starter.json --starter   the small bundled set, so the first game works offline
The compact item: {"i": id, "c": category, "d": 1..3, "k": 1 for kids, "q": [ar, en], "a": [ar, en], "w": [[ar, en] x3]}. Only verified quiz items with both languages and
3 wrong options are kept; the file is sorted by id so a rebuild with the same bank is the same file (no noise in the data branch)."""
import json, random, re, sys

BYTES_MAX, ITEMS_MAX, TEXT_MAX, MIN_ITEMS = 1_500_000, 5000, 200, 200

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

def _text(s):
    return isinstance(s, str) and 1 <= len(s) <= TEXT_MAX and not any(ord(c) < 32 or 127 <= ord(c) < 160 for c in s)

def _pair(p):
    return isinstance(p, list) and len(p) == 2 and all(_text(x) for x in p)

def verify(path, min_items=MIN_ITEMS):
    """the app's own rules (QuizBankCheck.java) on a built file: returns a list of problems, empty when the file is good"""
    raw = open(path, "rb").read()
    if len(raw) > BYTES_MAX: return [f"{len(raw)} bytes, over {BYTES_MAX}"]
    try: bank = json.loads(raw.decode("utf-8"))
    except ValueError as e: return [f"not JSON: {e}"]
    if not isinstance(bank, list) or not raw.lstrip().startswith(b"["): return ["not an array"]
    if not (min_items <= len(bank) <= ITEMS_MAX): return [f"{len(bank)} items, not {min_items} to {ITEMS_MAX}"]
    bad, ids = [], set()
    for it in bank:
        i = it.get("i") if isinstance(it, dict) else None
        ok = (isinstance(it, dict) and set(it) == {"i", "c", "d", "k", "q", "a", "w"} and isinstance(i, str) and re.fullmatch(r"[A-Za-z0-9._:\-]{1,64}", i)
              and isinstance(it["c"], str) and re.fullmatch(r"[a-z\-]{1,24}", it["c"]) and it["d"] in (1, 2, 3) and it["k"] in (0, 1)
              and _pair(it["q"]) and _pair(it["a"]) and isinstance(it["w"], list) and len(it["w"]) == 3 and all(_pair(x) for x in it["w"])
              and all(len({it["a"][n], *(x[n] for x in it["w"])}) == 4 for n in (0, 1)) and i not in ids)
        if not ok: bad.append(str(i))
        ids.add(i)
    return [f"{len(bad)} bad items, e.g. {bad[:3]}"] if bad else []

if __name__ == "__main__":
    if sys.argv[1] == "--verify":
        problems = verify(sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else MIN_ITEMS)
        print("\n".join(problems) or "bank ok")
        sys.exit(1 if problems else 0)
    src, dst = sys.argv[1], sys.argv[2]
    bank = compact(json.load(open(src, encoding="utf-8")))
    if "--starter" in sys.argv: bank = starter(bank)
    json.dump(bank, open(dst, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print(len(bank), "items")
