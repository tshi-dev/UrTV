#!/usr/bin/env python3
"""The word files of «واحد مو معنا» and «بدون كلام / وصّف» in the compact form the TV reads (OddOneWords.parse, CharadesWords.parse), from the checked
data of the branch work/games-data (games/odd-one/topics.json, games/charades/words.json; games/check_words.py is their check).

    python3 .github/tools/make_party_words.py <games-dir> <out-dir>     writes <out-dir>/odd-one.json and <out-dir>/charades.json, then verifies them
    python3 .github/tools/make_party_words.py --verify <dir>            verifies the two files of <dir> (assets/games in the repository)

verify() is the same rule the app applies before it plays a file: any problem refuses the whole file."""
import json, os, sys

BANNED = ["·", "•", "israel", "إسرائيل", "اسرائيل", "الصحراء الغربية", "western sahara"]
TEXT_MAX = 40


def compact(obj):
    return json.dumps(obj, ensure_ascii=False, separators=(",", ":"))


def build(games, out):
    topics = json.load(open(os.path.join(games, "odd-one/topics.json"), encoding="utf-8"))["topics"]
    odd = {"v": 1, "topics": [{"i": t["id"], "n": [t["ar"], t["en"]], "w": [[w["ar"], w["en"], w["difficulty"]] for w in t["words"]]} for t in topics]}
    words = json.load(open(os.path.join(games, "charades/words.json"), encoding="utf-8"))
    cha = {"v": 1, "words": [[w["id"], w["ar"], w["en"], w["category"], w["difficulty"], 1 if w["age"] == "kids" else 0,
                              (1 if "act" in w["modes"] else 0) | (2 if "describe" in w["modes"] else 0)] for w in words]}
    os.makedirs(out, exist_ok=True)
    for name, d in (("odd-one.json", odd), ("charades.json", cha)):
        with open(os.path.join(out, name), "w", encoding="utf-8") as f:
            f.write(compact(d).replace("},{", "},\n{").replace("],[", "],\n[") + "\n")


def text_ok(s):
    return isinstance(s, str) and s == s.strip() and 0 < len(s) <= TEXT_MAX


def verify(d):
    errors = []
    def err(m):
        errors.append(m)
    for name in ("odd-one.json", "charades.json"):
        p = os.path.join(d, name)
        try:
            raw = open(p, encoding="utf-8").read()
            data = json.loads(raw)
        except (OSError, ValueError) as e:
            err(f"{name}: {e}"); continue
        low = raw.lower()
        for b in BANNED:
            if b in low: err(f"{name}: banned text {b!r}")
        if not isinstance(data, dict) or data.get("v") != 1: err(f"{name}: not version 1"); continue
        if name == "odd-one.json":
            ts = data.get("topics") or []
            if len(ts) < 30: err(f"{name}: {len(ts)} topics, need 30")
            if len({t.get("i") for t in ts}) != len(ts): err(f"{name}: duplicate topic id")
            for t in ts:
                if set(t) != {"i", "n", "w"} or not text_ok(t["i"]) or len(t["n"]) != 2 or not all(text_ok(x) for x in t["n"]): err(f"{name}: bad topic {t.get('i')}"); continue
                if not 20 <= len(t["w"]) <= 40 or len({w[0] for w in t["w"]}) != len(t["w"]): err(f"{name}/{t['i']}: needs 20 to 40 distinct words")
                for w in t["w"]:
                    if len(w) != 3 or not text_ok(w[0]) or not text_ok(w[1]) or w[2] not in (1, 2, 3): err(f"{name}/{t['i']}: bad word {w}")
        else:
            ws = data.get("words") or []
            if len(ws) < 600: err(f"{name}: {len(ws)} words, need 600")
            if len({w[0] for w in ws}) != len(ws) or len({w[1] for w in ws}) != len(ws): err(f"{name}: duplicate id or word")
            for w in ws:
                if len(w) != 7 or not all(text_ok(x) for x in w[:4]) or w[4] not in (1, 2, 3) or w[5] not in (0, 1) or w[6] not in (1, 2, 3): err(f"{name}: bad word {w}")
            for describe in (False, True):
                for kids in (False, True):
                    n = sum(1 for w in ws if len(w) == 7 and w[6] & (2 if describe else 1) and (not kids or (w[5] == 1 and w[4] == 1)))
                    if n < 100: err(f"{name}: only {n} words for describe={describe} kids={kids}, need 100")
    return errors


if __name__ == "__main__":
    a = sys.argv[1:]
    if len(a) == 2 and a[0] == "--verify":
        target = a[1]
    elif len(a) == 2:
        build(a[0], a[1]); target = a[1]
    else:
        sys.exit(__doc__)
    problems = verify(target)
    for e in problems: print("!! " + e)
    print(f"party words: {'refused' if problems else 'ok'} ({target})")
    sys.exit(1 if problems else 0)
