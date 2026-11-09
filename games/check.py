#!/usr/bin/env python3
"""Checks games/data/*.json. Run: python3 games/check.py [dir]. Exit 1 on any problem."""
import json, sys, os, glob, re

TYPES = {"quiz", "truefalse", "proverb", "estimate", "riddle", "word"}
CATS = {"general", "geography", "science", "animals", "history", "islamic", "language",
        "saudi-gulf", "sports", "food", "maths-logic", "kids"}
BANNED = ["israel", "western sahara", "إسرائيل", "الصحراء الغربية", "·"]
Q_MAX, A_MAX = 90, 28
ARABIC = re.compile(r"[؀-ۿ]")
errors = []

def err(f, i, msg):
    errors.append(f"{os.path.basename(f)}:{i}: {msg}")

def bi(v):
    return isinstance(v, dict) and all(isinstance(v.get(k), str) and v[k].strip() for k in ("ar", "en"))

def check_item(f, it, ftype):
    i = it.get("id", "?")
    for k in ("id", "type", "category", "difficulty", "age", "question", "answer", "source", "verified"):
        if k not in it:
            return err(f, i, f"missing {k}")
    if it["type"] != ftype: err(f, i, f"type {it['type']} in a {ftype} file")
    if it["type"] not in TYPES: err(f, i, "unknown type")
    if it["category"] not in CATS: err(f, i, f"unknown category {it['category']}")
    if it["difficulty"] not in (1, 2, 3): err(f, i, "difficulty must be 1, 2 or 3")
    if it["age"] not in ("kids", "all"): err(f, i, "age must be kids or all")
    if it["verified"] is not True: err(f, i, "verified must be true in a shipped file")
    src = it["source"]
    if not (isinstance(src, list) and src and all(isinstance(s, str) and s.startswith("https://") for s in src)):
        err(f, i, "source must be a non-empty list of https URLs")
    elif (it["type"] == "estimate" or it.get("numeric") is True) and len(set(src)) < 2:
        err(f, i, "numeric/dated/record items need two distinct sources")
    if not bi(it["question"]): return err(f, i, "question needs ar and en")
    if len(it["question"]["ar"]) > Q_MAX: err(f, i, f"question ar {len(it['question']['ar'])} > {Q_MAX}")
    if not ARABIC.search(it["question"]["ar"]): err(f, i, "question ar has no Arabic")
    t = it["type"]
    if t == "truefalse":
        if it["answer"] not in (True, False): err(f, i, "answer must be true or false")
        if it.get("wrong"): err(f, i, "truefalse has no wrong options")
    else:
        if not bi(it["answer"]): return err(f, i, "answer needs ar and en")
        if len(it["answer"]["ar"]) > A_MAX: err(f, i, f"answer ar {len(it['answer']['ar'])} > {A_MAX}")
        if t in ("quiz", "proverb"):
            w = it.get("wrong")
            if not (isinstance(w, list) and len(w) == 3 and all(bi(x) for x in w)):
                return err(f, i, "needs exactly 3 wrong options with ar and en")
            for lang in ("ar", "en"):
                opts = [it["answer"][lang].strip().lower()] + [x[lang].strip().lower() for x in w]
                if len(set(opts)) != 4: err(f, i, f"options not distinct in {lang} (exactly one correct answer)")
            for x in w:
                if len(x["ar"]) > A_MAX: err(f, i, "wrong option ar too long")
        elif it.get("wrong"):
            err(f, i, f"{t} has no wrong options")
    blob = json.dumps(it, ensure_ascii=False).lower()
    for b in BANNED:
        if b in blob: err(f, i, f"banned text {b!r}")

def main():
    d = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "data")
    ids, qs, total = set(), set(), 0
    for f in sorted(glob.glob(os.path.join(d, "*.json"))):
        try:
            data = json.load(open(f, encoding="utf-8"))
        except Exception as e:
            err(f, 0, f"bad JSON: {e}"); continue
        ftype = os.path.basename(f)[:-5]
        if not isinstance(data, list): err(f, 0, "file must be a list"); continue
        for it in data:
            total += 1
            check_item(f, it, ftype)
            if it.get("id") in ids: err(f, it.get("id"), "duplicate id")
            ids.add(it.get("id"))
            q = it.get("question", {}).get("ar", "") if isinstance(it.get("question"), dict) else ""
            if q in qs: err(f, it.get("id"), "duplicate question")
            qs.add(q)
    for e in errors: print(e)
    print(f"{total} items, {len(errors)} problems")
    return 1 if errors else 0

if __name__ == "__main__":
    sys.exit(main())
