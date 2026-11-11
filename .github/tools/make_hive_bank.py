#!/usr/bin/env python3
"""«خلية الحروف»: the questions by first letter, in the form the TV reads (HiveBank.java).
   python3 .github/tools/make_hive_bank.py GAMES_DIR OUT_DIR             every indexed question: OUT_DIR/bank.tsv and OUT_DIR/hints.tsv
   python3 .github/tools/make_hive_bank.py GAMES_DIR OUT_DIR --starter   the small bundled set (assets/games/letter-hive), so the first game works offline
GAMES_DIR is games/ of the games data (branch work/games-data): data/quiz.json, letter-hive/index.json (which letter an answer starts with: «ال» skipped, the
hamza forms folded; this tool only reads it) and khanat/letters.json (sample answers for the category question of a letter that has run out of questions).
A line of bank.tsv: id, Arabic letter, English letter (empty when the item has no real English or its answer does not start with A to Z), level, kids,
question ar, en, answer ar, en, three wrong options (ar, en each). A line of hints.tsv: a letter (Arabic or Latin), then up to 6 samples in its language.
Sorted, so the same data gives the same files."""
import json, random, re, sys

AR = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"
NEVER = ("إسرائيل", "اسرائيل", "الصحراء الغربية", "israel", "western sahara")   # never listed as a country
# letters the samples file has nothing for: a few plain animals and places, so their category question still has a hint
EXTRA = {"ظ": ["ظبي", "ظربان", "ظفار"], "ض": ["ضفدع", "ضبع", "ضب"], "ث": ["ثعلب", "ثور", "ثعبان"], "ذ": ["ذئب", "ذبابة", "ذمار"]}

def arabic(s): return any("؀" <= c <= "ۿ" for c in s)
def clean(s): return isinstance(s, str) and 1 <= len(s) <= 200 and "\t" not in s and "\n" not in s and not any(n in s.lower() for n in NEVER)

def en_letter(it):
    texts = [it["question"]["en"], it["answer"]["en"]] + [w["en"] for w in it["wrong"]]
    if any(not t or arabic(t) for t in texts): return ""
    first = re.sub(r"^(the|a|an)\s+", "", it["answer"]["en"].strip(), flags=re.I)[:1].upper()
    return first if "A" <= first <= "Z" else ""

def rows(games):
    index = json.load(open(f"{games}/letter-hive/index.json", encoding="utf-8"))["index"]
    letter = {i: l for l, ids in index.items() for i in ids}
    out = []
    for it in json.load(open(f"{games}/data/quiz.json", encoding="utf-8")):
        q, a, w = it.get("question") or {}, it.get("answer") or {}, it.get("wrong") or []
        if it.get("type") != "quiz" or not it.get("verified") or it["id"] not in letter or letter[it["id"]] not in AR or len(w) != 3: continue
        texts = [q.get("ar"), q.get("en"), a.get("ar"), a.get("en")] + [x.get(k) for x in w for k in ("ar", "en")]
        if not all(clean(t) for t in texts) or len({a["ar"], *(x["ar"] for x in w)}) != 4 or len({a["en"], *(x["en"] for x in w)}) != 4: continue
        out.append([it["id"], letter[it["id"]], en_letter(it), str(int(it["difficulty"])), "1" if it.get("age") == "kids" else "0"] + texts)
    return sorted(out, key=lambda r: r[0])

def starter(all_rows, per_ar=8, per_en=6, seed=43):
    """up to 8 of every Arabic letter (kids and easy ones first in line, so the kids filter and «سهل» have something), then every English letter topped up to 6"""
    rnd, chosen = random.Random(seed), {}
    def take(pool, n):
        pool = [r for r in pool if r[0] not in chosen]
        rnd.shuffle(pool)
        pool.sort(key=lambda r: (r[4] != "1", r[3]))   # stable: kids first, then by level
        half = pool[:n // 2] + pool[len(pool) - (n - n // 2):] if len(pool) > n else pool   # the easy end and the hard end
        for r in half: chosen[r[0]] = r
    for l in AR: take([r for r in all_rows if r[1] == l], per_ar)
    for l in "ABCDEFGHIJKLMNOPQRSTUVWXYZ":
        take([r for r in all_rows if r[2] == l], max(0, per_en - sum(1 for r in chosen.values() if r[2] == l)))
    return sorted(chosen.values(), key=lambda r: r[0])

def hints(games):
    samples = json.load(open(f"{games}/khanat/letters.json", encoding="utf-8"))["samples"]
    ar, en = {}, {}
    for l in sorted(samples):
        for box in ("animal", "country", "saudi_city"):   # the third box holds cities of everywhere
            for x in samples[l].get(box, []):
                if clean(x.get("ar")) and l in AR: ar.setdefault(l, {}).setdefault(box, []).append(x["ar"])
                e = (x.get("en") or "").strip()
                if clean(e) and not arabic(e) and "A" <= e[:1].upper() <= "Z": en.setdefault(e[:1].upper(), {}).setdefault(box, []).append(e[:1].upper() + e[1:])
    for l, v in EXTRA.items(): ar.setdefault(l, {}).setdefault("animal", [])[:0] = v
    en.setdefault("X", {}).setdefault("animal", [])[:0] = ["Xerus", "Xiamen", "Xi'an"]
    out = []
    for l, boxes in sorted({**ar, **en}.items()):
        picked = []
        for n in range(6):   # in turn from each box: two animals, two countries, two cities when there are
            for box in ("animal", "country", "saudi_city"):
                v = boxes.get(box, [])
                if n < len(v) and v[n] not in picked and len(picked) < 6: picked.append(v[n])
        out.append([l] + picked)
    return out

if __name__ == "__main__":
    games, dst = sys.argv[1], sys.argv[2]
    bank = rows(games)
    if "--starter" in sys.argv: bank = starter(bank)
    name = "starter.tsv" if "--starter" in sys.argv else "bank.tsv"
    open(f"{dst}/{name}", "w", encoding="utf-8", newline="\n").write("".join("\t".join(r) + "\n" for r in bank))
    h = hints(games)
    open(f"{dst}/hints.tsv", "w", encoding="utf-8", newline="\n").write("".join("\t".join(r) + "\n" for r in h))
    print(len(bank), "questions,", len(h), "letters with hints")
