#!/usr/bin/env python3
"""«خمّن الكلمة»: the two word lists in the form the TV reads (Word5Words.java).
   python3 .github/tools/make_word5.py GAMES_DIR OUT_DIR     writes OUT_DIR/accepted.bin and OUT_DIR/answers.tsv (assets/games/word5: both ship whole in the APK)
GAMES_DIR is games/ of the games data (branch work/games-data): word5/accepted.json (normalized five-letter strings) and word5/answers.json.
accepted.bin: «W5A1», the count (4 bytes, big-endian), then the words as numbers (letters as base-28 digits in keyboard order), sorted, each stored as the
difference from the one before it in a varint. answers.tsv: word, the spelling to show, the English gloss (may be empty), level 1..3, kids 0/1; sorted by word.
A word that is not five of the 28 letters, or an answer that is not an accepted guess or whose spelling does not normalize to its word, stops the tool."""
import json, re, sys

ALPHABET = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"

def normalize(s):
    s = re.sub("[ً-ٰٟـ]", "", s)
    return s.translate(str.maketrans({"أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ة": "ه", "ى": "ي"}))

def code(w):
    if len(w) != 5 or any(c not in ALPHABET for c in w): sys.exit(f"not five keyboard letters: {w!r}")
    n = 0
    for c in w: n = n * 28 + ALPHABET.index(c)
    return n

def varint(n):
    out = bytearray()
    while True:
        out.append((n & 127) | (128 if n > 127 else 0)); n >>= 7
        if not n: return bytes(out)

def main(games, dst):
    answers = json.load(open(f"{games}/word5/answers.json", encoding="utf-8"))
    codes = sorted({code(w) for w in json.load(open(f"{games}/word5/accepted.json", encoding="utf-8"))} | {code(a["word"]) for a in answers})
    with open(f"{dst}/accepted.bin", "wb") as f:
        f.write(b"W5A1" + len(codes).to_bytes(4, "big"))
        last = -1
        for n in codes: f.write(varint(n - last)); last = n
    lines, seen = [], set()
    for a in sorted(answers, key=lambda a: a["word"]):
        en = (a.get("en") or "").strip()
        if normalize(a["display"]) != a["word"] or a["word"] in seen or not all(" " <= c <= "~" for c in en) or a["difficulty"] not in (1, 2, 3):
            sys.exit(f"bad answer: {a['word']!r}")
        seen.add(a["word"])
        lines.append("\t".join([a["word"], a["display"], en, str(a["difficulty"]), "1" if a.get("age") == "kids" else "0"]))
    open(f"{dst}/answers.tsv", "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")
    print(len(codes), "accepted,", len(lines), "answers")

if __name__ == "__main__": main(*sys.argv[1:3])
