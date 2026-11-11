#!/usr/bin/env python3
"""The family games' data (games/<id>/ on branch work/games-data) in the compact forms the TV reads, one file per game.
   python3 .github/tools/make_games_data.py build   GAMES_DIR OUT_DIR           writes OUT_DIR/games/*.json (published on the branch data-games under games/)
   python3 .github/tools/make_games_data.py starter GAMES_DIR OUT_DIR QUIZ.json the small bundled sets, so the first round works offline (assets/games/)
   python3 .github/tools/make_games_data.py --verify DIR [QUIZ.json]            the app's own rules on a built directory (non-zero exit on any problem)
All text is checked like the quiz bank (1 to 200 characters, no control characters), a file is JSON with {"v": 1, ...}, and a build is deterministic (sorted,
compact separators), so the same data is the same bytes. The limits below are what the app will enforce on a download (a file that breaks one is ignored and the
last good copy, or the bundled starter, stays). Formats:
  khanat.json         {"v":1,"L":letters,"H":hard,"R":rare,"B":boxes,"X":extra boxes,"s":{letter:{box:[[ar,en],...]}}}   (en may be "" for names)
  missing-word.json   {"v":1,"w":[{"i":id,"k":"f"|"s","d":1..3,"a":0|1,"t":[ar,en],"w":[ar,en]}]}   (k: f fact, s saying; the text holds one ___; en may be "" for a saying)
  odd-one.json        {"v":1,"t":[{"i":id,"n":[ar,en],"w":[[ar,en,d],...]}]}
  charades.json       {"v":1,"c":[category ids],"w":[[ar,en,category index,d,kids 0|1,modes bits: 1 act, 2 describe],...]}
  letter-hive.json    {"v":1,"L":28 letters,"S":short letters,"a":{letter:[quiz ids]},"e":{"A":[quiz ids]}}   (ids of items in quiz/quiz.json)
  word5-answers.json  {"v":1,"w":[[word,display,en,d,kids 0|1],...]}   (word: 5 normalized letters; en may be "" for dictionary words)
  word5-guesses.json  {"v":1,"A":alphabet,"n":count,"w":string}   (sorted five-letter words packed as one string, 5 characters each, character k = a..z,0,1 for the k-th letter of A:
                       look a word up by binary search on the string, no table in memory)"""
import json, os, re, sys, collections

ALPHA = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"
PACK = "abcdefghijklmnopqrstuvwxyz01"
TEXT_MAX = 200
#            game file            max bytes   min items  max items
LIMITS = {"khanat": (200_000, 200, 4000), "missing-word": (300_000, 100, 5000), "odd-one": (150_000, 8, 200),
          "charades": (250_000, 200, 10000), "letter-hive": (250_000, 200, 12000),
          "word5-answers": (120_000, 100, 5000), "word5-guesses": (700_000, 2000, 120000)}

def norm(s):
    s = re.sub(r"[ً-ٰٟـۖ-ۭ]", "", s)
    return re.sub("[أإآٱ]", "ا", s).replace("ة", "ه").replace("ى", "ي")

def first_letter(s):
    n = norm(s).strip()
    if n.startswith("ال") and len(n) > 3: n = n[2:]
    return n[:1]

def dumps(o): return json.dumps(o, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
def load(p): return json.load(open(p, encoding="utf-8"))
def write(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    open(p, "w", encoding="utf-8").write(dumps(o))

# ---------------------------------------------------------------- build
def build_khanat(g):
    d = load(f"{g}/khanat/letters.json")
    s = {l: {b: [[x["ar"], x["en"]] for x in lst] for b, lst in sorted(boxes.items())} for l, boxes in sorted(d["samples"].items())}
    return {"v": 1, "L": "".join(d["letters"]), "H": "".join(d["hard_letters"]), "R": "".join(d["rare_letters"]), "B": d["boxes"], "X": d["extra_boxes"], "s": s}

def build_missing(g):
    out = []
    for x in load(f"{g}/missing-word/items.json"):
        out.append({"i": x["id"], "k": "f" if x["deck"] == "fact" else "s", "d": x["difficulty"], "a": 1 if x["age"] == "kids" else 0,
                    "t": [x["text"]["ar"], x["text"]["en"]], "w": [x["answer"]["ar"], x["answer"]["en"]]})
    return {"v": 1, "w": sorted(out, key=lambda x: x["i"])}

def build_odd(g):
    t = [{"i": x["id"], "n": [x["ar"], x["en"]], "w": [[w["ar"], w["en"], w["difficulty"]] for w in x["words"]]} for x in load(f"{g}/odd-one/topics.json")["topics"]]
    return {"v": 1, "t": sorted(t, key=lambda x: x["i"])}

def build_charades(g):
    ws = load(f"{g}/charades/words.json")
    cats = sorted({x["category"] for x in ws}); ci = {c: i for i, c in enumerate(cats)}
    bits = lambda m: (1 if "act" in m else 0) | (2 if "describe" in m else 0)
    rows = sorted(([x["ar"], x["en"], ci[x["category"]], x["difficulty"], 1 if x["age"] == "kids" else 0, bits(x["modes"])] for x in ws), key=lambda r: (r[2], r[3], r[0]))
    return {"v": 1, "c": cats, "w": rows}

def build_hive(g):
    d = load(f"{g}/letter-hive/index.json")
    return {"v": 1, "L": "".join(d["letters"]), "S": "".join(d["short_letters"]), "a": {l: sorted(v) for l, v in sorted(d["index"].items())},
            "e": {l: sorted(v) for l, v in sorted(d["en_index"].items())}}

def build_word5(g):
    a = load(f"{g}/word5/answers.json")
    ans = sorted(([x["word"], x["display"], x["en"], x["difficulty"], 1 if x["age"] == "kids" else 0] for x in a), key=lambda r: r[0])
    guesses = sorted(set(load(f"{g}/word5/accepted.json")) | {x["word"] for x in a})
    return {"v": 1, "w": ans}, pack(guesses)

def pack(words):
    return {"v": 1, "A": ALPHA, "n": len(words), "w": "".join(PACK[ALPHA.index(c)] for w in words for c in w)}

def unpack(d): return ["".join(ALPHA[PACK.index(c)] for c in d["w"][i:i + 5]) for i in range(0, len(d["w"]), 5)]

def build(g, out):
    files = {"khanat": build_khanat(g), "missing-word": build_missing(g), "odd-one": build_odd(g), "charades": build_charades(g), "letter-hive": build_hive(g)}
    files["word5-answers"], files["word5-guesses"] = build_word5(g)
    for name, obj in files.items():
        write(f"{out}/games/{name}.json", obj)
        print(f"{name}.json", os.path.getsize(f"{out}/games/{name}.json"), "bytes")

# ---------------------------------------------------------------- starter (bundled, small)
def starter(g, out, quizpath):
    quiz_ids = {x["i"] for x in load(quizpath)}
    k = build_khanat(g)
    for l in k["s"]:
        for b in k["s"][l]: k["s"][l][b] = k["s"][l][b][:5]
    mw = build_missing(g)["w"]
    facts = [x for x in mw if x["k"] == "f"][:30]; sayings = [x for x in mw if x["k"] == "s"][:50]
    od = build_odd(g); od["t"] = od["t"][:10]
    ch = build_charades(g); keep = [r for r in ch["w"] if r[3] == 1][:60] + [r for r in ch["w"] if r[3] == 2][:50] + [r for r in ch["w"] if r[3] == 3][:40]; ch["w"] = keep
    hv = build_hive(g); hv["a"] = {l: [i for i in v if i in quiz_ids] for l, v in hv["a"].items()}; hv["e"] = {l: [i for i in v if i in quiz_ids] for l, v in hv["e"].items()}
    hv["S"] = "".join(l for l in hv["L"] if len(hv["a"].get(l, [])) < 12)
    ans, _ = build_word5(g)
    lem = sorted(set(load(f"{g}/word5/lemmas.json")) | {r[0] for r in ans["w"]})
    for name, obj in {"khanat": k, "missing-word": {"v": 1, "w": facts + sayings}, "odd-one": od, "charades": ch, "letter-hive": hv, "word5-answers": ans, "word5-guesses": pack(lem)}.items():
        write(f"{out}/{name}.json", obj); print(f"{name}.json", os.path.getsize(f"{out}/{name}.json"), "bytes")

# ---------------------------------------------------------------- verify
def _t(s, blank_ok=False):
    return isinstance(s, str) and (blank_ok and s == "" or 1 <= len(s) <= TEXT_MAX) and not any(ord(c) < 32 or 127 <= ord(c) < 160 for c in s)

def _id(s): return isinstance(s, str) and re.fullmatch(r"[A-Za-z0-9._:\-]{1,64}", s) is not None
def _d(v): return v in (1, 2, 3)

def verify_file(name, path, answers=None, quiz=None, minimum=True):
    raw = open(path, "rb").read(); mx, mn, top = LIMITS[name]
    if len(raw) > mx: return [f"{len(raw)} bytes, over {mx}"]
    try: d = json.loads(raw.decode("utf-8"))
    except ValueError as e: return [f"not JSON: {e}"]
    if not isinstance(d, dict) or d.get("v") != 1 or not raw.lstrip().startswith(b"{"): return ["not an object with v 1"]
    bad = []
    def count(n):
        if not ((mn if minimum else 1) <= n <= top): bad.append(f"{n} items, not {mn} to {top}")
    if name == "khanat":
        s = d.get("s"); ok = isinstance(s, dict) and isinstance(d.get("L"), str) and len(d["L"]) == 25
        n = 0
        if ok:
            for l, boxes in s.items():
                if l not in d["L"] or not isinstance(boxes, dict): bad.append(f"letter {l}"); continue
                for b, lst in boxes.items():
                    for p in lst:
                        n += 1
                        if not (isinstance(p, list) and len(p) == 2 and _t(p[0]) and _t(p[1], blank_ok=True) and len(p[0]) <= 24 and first_letter(p[0]) == l): bad.append(f"{l}/{b}/{p}")
        else: bad.append("shape")
        count(n)
    elif name == "missing-word":
        w = d.get("w"); ids = set()
        if not isinstance(w, list): return ["no list"]
        count(len(w))
        for x in w:
            ok = (isinstance(x, dict) and set(x) == {"i", "k", "d", "a", "t", "w"} and _id(x["i"]) and x["i"] not in ids and x["k"] in ("f", "s") and _d(x["d"]) and x["a"] in (0, 1)
                  and isinstance(x["t"], list) and len(x["t"]) == 2 and _t(x["t"][0]) and x["t"][0].count("___") == 1 and _t(x["t"][1], blank_ok=x["k"] == "s")
                  and isinstance(x["w"], list) and len(x["w"]) == 2 and _t(x["w"][0]) and _t(x["w"][1], blank_ok=x["k"] == "s") and len(x["t"][0]) <= 90)
            if not ok: bad.append(str(x.get("i") if isinstance(x, dict) else x))
            ids.add(x.get("i") if isinstance(x, dict) else None)
    elif name == "odd-one":
        t = d.get("t")
        if not isinstance(t, list): return ["no list"]
        count(len(t)); ids = set()
        for x in t:
            ok = (isinstance(x, dict) and set(x) == {"i", "n", "w"} and _id(x["i"]) and x["i"] not in ids and isinstance(x["n"], list) and len(x["n"]) == 2 and all(_t(y) for y in x["n"])
                  and isinstance(x["w"], list) and 12 <= len(x["w"]) <= 100 and all(isinstance(w, list) and len(w) == 3 and _t(w[0]) and len(w[0]) <= 24 and _t(w[1]) and _d(w[2]) for w in x["w"])
                  and len({w[0] for w in x["w"]}) == len(x["w"]))
            if not ok: bad.append(str(x.get("i") if isinstance(x, dict) else x))
            ids.add(x.get("i") if isinstance(x, dict) else None)
    elif name == "charades":
        c, w = d.get("c"), d.get("w")
        if not (isinstance(c, list) and isinstance(w, list) and all(isinstance(x, str) and re.fullmatch(r"[a-z_\-]{1,24}", x) for x in c)): return ["shape"]
        count(len(w)); seen = set()
        for r in w:
            ok = (isinstance(r, list) and len(r) == 6 and _t(r[0]) and len(r[0]) <= 24 and _t(r[1]) and r[2] in range(len(c)) and _d(r[3]) and r[4] in (0, 1) and r[5] in (1, 2, 3) and norm(r[0]) not in seen)
            if not ok: bad.append(str(r))
            if isinstance(r, list) and r: seen.add(norm(r[0]))
    elif name == "letter-hive":
        a, e = d.get("a"), d.get("e")
        if not (isinstance(a, dict) and isinstance(e, dict) and isinstance(d.get("L"), str) and len(d["L"]) == 28 and set(a) <= set(d["L"])): return ["shape"]
        n = sum(len(v) for v in a.values()) + sum(len(v) for v in e.values()); count(n)
        for v in list(a.values()) + list(e.values()):
            for i in v:
                if not _id(i) or (quiz is not None and i not in quiz): bad.append(str(i))
        if sorted(l for l in d["L"] if len(a.get(l, [])) < 12) != sorted(d.get("S", "")): bad.append("S does not match the index")
    elif name == "word5-answers":
        w = d.get("w")
        if not isinstance(w, list): return ["no list"]
        count(len(w)); seen = set()
        for r in w:
            ok = (isinstance(r, list) and len(r) == 5 and isinstance(r[0], str) and re.fullmatch(f"[{ALPHA}]{{5}}", r[0]) and _t(r[1]) and norm(r[1]) == r[0] and _t(r[2], blank_ok=True)
                  and _d(r[3]) and r[4] in (0, 1) and r[0] not in seen)
            if not ok: bad.append(str(r))
            if isinstance(r, list) and r: seen.add(r[0])
    elif name == "word5-guesses":
        s = d.get("w")
        if not (d.get("A") == ALPHA and isinstance(s, str) and len(s) % 5 == 0 and len(s) // 5 == d.get("n") and all(c in PACK for c in s)): return ["shape"]
        count(d["n"]); ws = unpack(d)
        if ws != sorted(set(ws)): bad.append("not sorted and unique")
        if answers is not None:
            have = set(ws); miss = [a for a in answers if a not in have]
            if miss: bad.append(f"{len(miss)} answers are not accepted guesses, e.g. {miss[:3]}")
    return [f"{len(bad)} bad entries, e.g. {bad[:3]}"] if bad else []

def verify(directory, quizpath=None, minimum=True):
    quiz = {x["i"] for x in load(quizpath)} if quizpath else None
    answers = None
    problems = []
    base = f"{directory}/games" if os.path.isdir(f"{directory}/games") else directory
    p = f"{base}/word5-answers.json"
    if os.path.exists(p): answers = [r[0] for r in load(p)["w"]]
    for name in LIMITS:
        p = f"{base}/{name}.json"
        if not os.path.exists(p): problems.append(f"{name}.json missing"); continue
        problems += [f"{name}.json: {x}" for x in verify_file(name, p, answers, quiz, minimum)]
    return problems

if __name__ == "__main__":
    a = sys.argv[1:]
    if a and a[0] == "--verify":
        pr = verify(a[1], a[2] if len(a) > 2 else None, minimum="--starter" not in a)
        print("\n".join(pr) or "games data ok"); sys.exit(1 if pr else 0)
    if a and a[0] == "build": build(a[1], a[2])
    elif a and a[0] == "starter": starter(a[1], a[2], a[3])
    else: print(__doc__); sys.exit(2)
