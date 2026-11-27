#!/usr/bin/env python3
"""games/meta.json (what the setup screens build their chips from) and quiz-v2.json (the quiz bank as a versioned object, beside the old bare array).
   python3 -I games/tools/build_meta.py GAMES_DIR OUT_DIR        writes OUT_DIR/meta.json and OUT_DIR/quiz-v2.json
   python3 -I games/tools/build_meta.py --verify OUT_DIR         checks both (shape, counts add up, names present, ids unique)
Schema and the rules a reader follows: games/SCHEMA.md. Deterministic: sorted keys and ids, no dates, so the same data is the same bytes."""
import hashlib, json, os, sys, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import curated

# category id -> (Arabic, English). A category in the data without a name here stops the build: a chip must have a name in both languages.
CATS = {"geography": ("جغرافيا", "Geography"), "science": ("علوم", "Science"), "islamic": ("دين", "Religion"), "maths-logic": ("رياضيات ومنطق", "Maths and logic"),
        "animals": ("حيوانات", "Animals"), "saudi-gulf": ("السعودية والخليج", "Saudi Arabia and the Gulf"), "history": ("تاريخ", "History"), "food": ("طعام", "Food"),
        "sports": ("رياضة", "Sports"), "general": ("عام", "General"), "language": ("اللغة", "Language"), "technology": ("تقنية", "Technology"),
        # charades categories
        "farm": ("حيوانات المزرعة", "Farm animals"), "kids": ("أطفال", "Kids")}
# the chips the setup screens show (the owner's 8-chip model); a group lists the category ids it covers; «مزيج» is every category and is added by the reader
GROUPS = [("islamic", "دين", "Religion", ["islamic"]), ("geography", "جغرافيا", "Geography", ["geography"]), ("science", "علوم", "Science", ["science", "animals", "maths-logic", "technology"]),
          ("history", "تاريخ", "History", ["history"]), ("sports", "رياضة", "Sports", ["sports"]), ("saudi", "ثقافة سعودية", "Saudi culture", ["saudi-gulf"]),
          ("general", "عام", "General", ["general", "food", "language"])]
LEVELS = [(1, "سهل", "Easy"), (2, "متوسط", "Medium"), (3, "صعب", "Hard")]
DECKS = {"fact": ("حقائق", "Facts"), "saying": ("أقوال وأمثال", "Sayings")}
BOXES = {"animal": ("حيوان", "Animal"), "plant": ("نبات", "Plant"), "object": ("جماد", "Object"), "country": ("بلاد", "Country"), "name": ("اسم", "Name"),
         "food": ("أكلة", "Dish"), "saudi_city": ("مدينة سعودية", "Saudi city"), "city": ("مدينة", "City"), "job": ("مهنة", "Job")}
# charades and odd-one categories are the curated topics (curated.py), which carry their own names
CHARADE_CATS = {k: (v[0], v[1]) for k, v in curated.T.items()}

def load(p): return json.load(open(p, encoding="utf-8"))
def dumps(o): return json.dumps(o, ensure_ascii=False, separators=(",", ":"), sort_keys=True)

# topic tag of an item by the start of its id: a game may cap the share of one topic (e.g. 25 percent) so a category is not one template
TOPICS = [("geo-cap", "capitals"), ("geo-rev", "capitals-reverse"), ("geo-cont", "continents"), ("geo-cur", "currencies"), ("geo-lang", "languages"), ("el-", "elements"),
          ("qu-next", "surah-order"), ("qu-num", "surah-order"), ("qu-cnt", "surah-verses"), ("mt-", "arithmetic"), ("an-class", "animal-classes")]
def topic(i):
    for pre, t in TOPICS:
        if i.startswith(pre): return t
    return "facts"

def compact(items, tags=False):
    out = []
    for it in items:
        if it.get("type") != "quiz" or not it.get("verified"): continue
        q, a, w = it["question"], it["answer"], it["wrong"]
        if len(w) != 3: continue
        out.append({"i": it["id"], "c": it["category"], "d": int(it["difficulty"]), "k": 1 if it["age"] == "kids" else 0, "q": [q["ar"], q["en"]], "a": [a["ar"], a["en"]], "w": [[x["ar"], x["en"]] for x in w]})
        if tags: out[-1]["t"] = topic(it["id"])
    return sorted(out, key=lambda x: x["i"])

def named(table, key, where):
    if key not in table: raise SystemExit(f"{where}: '{key}' has no Arabic and English name in build_meta.py")
    return {"ar": table[key][0], "en": table[key][1]}

def build(g, out):
    bank = compact(load(f"{g}/data/quiz.json"))
    ids = [x["i"] for x in bank]
    by = collections.defaultdict(lambda: [0, 0, 0]); kids = collections.Counter(); tp = collections.defaultdict(collections.Counter)
    for x in bank:
        by[x["c"]][x["d"] - 1] += 1
        kids[x["c"]] += x["k"]; tp[x["c"]][topic(x["i"])] += 1
    cats = [{"id": c, **named(CATS, c, "quiz"), "n": by[c], "kids": kids[c], "topics": dict(sorted(tp[c].items()))} for c in sorted(by)]
    mw = load(f"{g}/missing-word/items.json"); d = collections.defaultdict(lambda: [0, 0, 0])
    for x in mw: d[x["deck"]][x["difficulty"] - 1] += 1
    odd = load(f"{g}/odd-one/topics.json")["topics"]
    ch = load(f"{g}/charades/words.json"); cc = collections.defaultdict(lambda: [0, 0, 0]); ck = collections.Counter()
    for x in ch: cc[x["category"]][x["difficulty"] - 1] += 1; ck[x["category"]] += 1 if x["age"] == "kids" else 0
    kh = load(f"{g}/khanat/letters.json"); kn = collections.Counter()
    for boxes in kh["samples"].values():
        for b, v in boxes.items(): kn[b] += len(v)
    hv = load(f"{g}/letter-hive/index.json")
    w5 = load(f"{g}/word5/answers.json")
    meta = {
        "v": 1, "min_items": 40,
        "levels": [{"id": i, "ar": a, "en": e} for i, a, e in LEVELS],
        "groups": [{"id": i, "ar": a, "en": e, "cats": c} for i, a, e, c in GROUPS],
        "games": {
            "quiz": {"items": len(bank), "bank": hashlib.sha1("\n".join(ids).encode()).hexdigest()[:12], "cats": cats},
            "missing-word": {"decks": [{"id": k, **named(DECKS, k, "missing-word"), "n": d[k]} for k in sorted(d)]},
            "odd-one": {"topics": [{"id": t["id"], "ar": t["ar"], "en": t["en"], "words": len(t["words"])} for t in sorted(odd, key=lambda t: t["id"])]},
            "charades": {"cats": [{"id": c, **named(CHARADE_CATS, c, "charades"), "n": cc[c], "kids": ck[c]} for c in sorted(cc)]},
            "khanat": {"boxes": [{"id": b, **named(BOXES, b, "khanat"), "extra": b in kh["extra_boxes"], "samples": kn[b]} for b in sorted(kn)], "letters": "".join(kh["letters"]), "hard": "".join(kh["hard_letters"])},
            "letter-hive": {"letters": "".join(hv["letters"]), "short": "".join(hv["short_letters"])},
            "word5": {"answers": len(w5), "guesses": len(load(f"{g}/word5/accepted.json"))},
        },
        "variants": []}
    os.makedirs(out, exist_ok=True)
    open(f"{out}/meta.json", "w", encoding="utf-8").write(dumps(meta))
    open(f"{out}/quiz-v2.json", "w", encoding="utf-8").write(dumps({"v": 2, "items": compact(load(f"{g}/data/quiz.json"), tags=True)}))
    print("meta.json", os.path.getsize(f"{out}/meta.json"), "bytes;", "quiz-v2.json", os.path.getsize(f"{out}/quiz-v2.json"), "bytes;", len(bank), "items")

def verify(out):
    bad = []
    try:
        m = load(f"{out}/meta.json"); q = load(f"{out}/quiz-v2.json")
    except Exception as e: return [f"not readable: {e}"]
    if m.get("v") != 1 or q.get("v") != 2 or not isinstance(q.get("items"), list): return ["version or shape"]
    gm = m["games"]["quiz"]
    if gm["items"] != len(q["items"]): bad.append("quiz item count differs from quiz-v2.json")
    ids = [x["i"] for x in q["items"]]
    if len(set(ids)) != len(ids) or ids != sorted(ids): bad.append("ids not unique and sorted")
    if hashlib.sha1("\n".join(ids).encode()).hexdigest()[:12] != gm["bank"]: bad.append("bank fingerprint differs")
    if sum(sum(c["n"]) for c in gm["cats"]) != len(ids): bad.append("category counts do not add up")
    for c in gm["cats"]:
        if not (c["ar"] and c["en"] and len(c["n"]) == 3 and c["kids"] <= sum(c["n"])): bad.append("category " + c["id"])
        real = collections.Counter(x["d"] for x in q["items"] if x["c"] == c["id"])
        if [real[1], real[2], real[3]] != c["n"]: bad.append("counts of " + c["id"])
    known = {c["id"] for c in gm["cats"]}
    for g in m["groups"]:
        if not (g["ar"] and g["en"] and isinstance(g["cats"], list)): bad.append("group " + g["id"])   # a group with no data yet is listed; the reader hides it below min_items
    for x in q["items"]:
        if not (isinstance(x["q"], list) and len(x["q"]) == 2 and len(x["w"]) == 3 and x["d"] in (1, 2, 3) and x["k"] in (0, 1)): bad.append(x["i"]); break
    return bad

if __name__ == "__main__":
    if sys.argv[1] == "--verify":
        pr = verify(sys.argv[2]); print("\n".join(pr) or "meta ok"); sys.exit(1 if pr else 0)
    build(sys.argv[1], sys.argv[2])
