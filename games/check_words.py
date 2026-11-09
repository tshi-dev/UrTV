#!/usr/bin/env python3
"""Checks the non-question data of the family games: odd-one, charades, khanat, word5, missing-word, letter-hive.
Run: python3 games/check_words.py [games-dir]. Exit 1 on any problem."""
import json, os, re, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))
LET28 = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"
BAD = ["·", "israel", "إسرائيل", "الصحراء الغربية", "western sahara"]
errors = []
def err(where, msg): errors.append(f"{where}: {msg}")
def load(rel):
    p = os.path.join(ROOT, rel)
    if not os.path.exists(p): err(rel, "missing"); return None
    return json.load(open(p, encoding="utf-8"))
def norm(s):
    s = re.sub(r"[ً-ٰٟـۖ-ۭ]", "", s)
    return re.sub("[أإآٱ]", "ا", s).replace("ة", "ه").replace("ى", "ي")
def src_ok(s): return isinstance(s, str) and s.startswith("https://")
def clean(where, blob):
    b = json.dumps(blob, ensure_ascii=False).lower()
    for x in BAD:
        if x in b: err(where, f"banned text {x!r}")

# odd-one
d = load("odd-one/topics.json")
if d:
    clean("odd-one", d)
    if len(d["topics"]) < 8: err("odd-one", "fewer than 8 topics")
    for t in d["topics"]:
        w = t["words"]
        if not 12 <= len(w) <= 40: err("odd-one/" + t["id"], f"{len(w)} words, need 12 to 40")
        if len({norm(x["ar"]) for x in w}) != len(w): err("odd-one/" + t["id"], "duplicate word")
        for x in w:
            if not (x["ar"].strip() and x["en"].strip() and 1 <= x["difficulty"] <= 3 and src_ok(x["source"]) and len(x["ar"]) <= 18):
                err("odd-one/" + t["id"], f"bad word {x}")
# charades
d = load("charades/words.json")
if d:
    clean("charades", d)
    if len({x["id"] for x in d}) != len(d): err("charades", "duplicate id")
    if len({norm(x["ar"]) for x in d}) != len(d): err("charades", "duplicate word")
    for x in d:
        if not (x["ar"] and x["en"] and x["age"] in ("kids", "all") and x["difficulty"] in (1, 2, 3) and x["verified"] is True
                and src_ok(x["source"]) and x["modes"] and len(x["ar"]) <= 18): err("charades/" + x["id"], "bad entry")
# khanat
d = load("khanat/letters.json")
if d:
    clean("khanat", d)
    if len(d["letters"]) != 25 or len(set(d["letters"])) != 25: err("khanat", "need 25 distinct letters")
    for l, boxes in d["samples"].items():
        for b, lst in boxes.items():
            for x in lst:
                if norm(x["ar"])[:1] != l and not norm(x["ar"]).startswith("ال" + l): err("khanat", f"{x['ar']} does not start with {l}")
                if not src_ok(x["source"]): err("khanat", "sample without source")
# word5
a, acc = load("word5/answers.json"), load("word5/accepted.json")
if a and acc is not None:
    clean("word5", a)
    S = set(acc)
    if acc != sorted(S): err("word5/accepted", "must be a sorted array without duplicates")
    for w in acc:
        if not re.fullmatch(f"[{LET28}]{{5}}", w): err("word5/accepted", f"bad entry {w!r}")
    if len({x["word"] for x in a}) != len(a): err("word5/answers", "duplicate word")
    for x in a:
        if not re.fullmatch(f"[{LET28}]{{5}}", x["word"]): err("word5/answers", f"bad word {x['word']!r}")
        elif norm(x["display"]) != x["word"]: err("word5/answers", f"display does not normalize to word: {x['word']}")
        elif x["word"] not in S: err("word5/answers", f"answer not in accepted: {x['word']}")
        if not (x["verified"] is True and src_ok(x["source"]) and x["en"]): err("word5/answers", f"bad entry {x['word']}")
    if len(a) < 100: err("word5/answers", f"only {len(a)} answers, need at least 100")
    if len(acc) < 2000: err("word5/accepted", f"only {len(acc)} accepted words, need at least 2000")
# missing-word
d = load("missing-word/items.json")
if d:
    clean("missing-word", d)
    if len({x["id"] for x in d}) != len(d): err("missing-word", "duplicate id")
    for x in d:
        t = x["text"]["ar"]
        if t.count("___") != 1 or len(t) > 90 or not x["answer"]["ar"].strip(): err("missing-word/" + x["id"], "bad blank or length")
        if x["deck"] == "saying":
            if x["verified"] != "repo" or x["source"] != ["repo:assets/shared/wisdom.json"]: err("missing-word/" + x["id"], "saying must be repo-checked")
        elif x["verified"] is not True or not all(src_ok(s) for s in x["source"]): err("missing-word/" + x["id"], "fact needs https sources and verified true")
# letter-hive
d, L = load("letter-hive/index.json"), load("letter-hive/letters.json")
if d and L:
    if len(L) < 16: err("letter-hive", f"only {len(L)} letters have 8 or more questions, need 16")
    for l in L:
        if len(d["index"].get(l, [])) < 8: err("letter-hive", f"letter {l} has fewer than 8 questions")
    ids = set()
    for f in ("quiz", "truefalse"):
        p = os.path.join(ROOT, "data", f + ".json")
        if os.path.exists(p): ids |= {i["id"] for i in json.load(open(p, encoding="utf-8"))}
    for l, v in d["index"].items():
        for i in v:
            if i not in ids: err("letter-hive", f"unknown question id {i}")
for e in errors: print(e)
print(f"words check: {len(errors)} problems")
sys.exit(1 if errors else 0)
