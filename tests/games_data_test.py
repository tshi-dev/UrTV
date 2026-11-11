#!/usr/bin/env python3
"""The family games' data (docs/GAMES-DATA.md): the tool's verify refuses what the app will refuse, the bundled starters pass it and stay small, the packed guesses
round-trip, and the workflow keeps its pins (checks first, fails closed, one orphan commit by tshi-dev, every game's file published)."""
import json, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / ".github/tools"))
import make_games_data as m

failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1

quiz = ROOT / "assets/quiz/starter.json"
pr = m.verify(str(ROOT / "assets/games"), str(quiz), minimum=False)
check("the bundled starters pass the same verification as the published files (cross-checked against the quiz starter's ids)", pr == [], pr)
size = sum(p.stat().st_size for p in (ROOT / "assets/games").glob("*.json")) + quiz.stat().st_size
check("the starters of all games together, with the quiz starter, stay under 300 KB", size < 300_000, size)
check("a starter per game exists", sorted(p.stem for p in (ROOT / "assets/games").glob("*.json")) == sorted(m.LIMITS), [p.name for p in (ROOT / "assets/games").glob("*.json")])

words = ["ابتثج", "بابها", "كتابا", "مدرسه"]
words = sorted(set(w for w in words if len(w) == 5))
check("the packed guesses round-trip and use one character per letter", m.unpack(m.pack(words)) == words and len(m.pack(words)["w"]) == 5 * len(words))
check("the same letters always pack to the same bytes (a rebuild is the same file)", m.dumps(m.pack(words)) == m.dumps(m.pack(list(words))))

with tempfile.TemporaryDirectory() as t:
    def one(name, obj):
        p = Path(t) / (name + ".json"); p.write_text(json.dumps(obj, ensure_ascii=False), encoding="utf-8"); return m.verify_file(name, str(p), minimum=False)
    ok_k = {"v": 1, "L": m.ALPHA[:0] + "ابتجحخدذرزسشصطعغفقكلمنهوي", "H": "", "R": "", "B": [], "X": [], "s": {"ب": {"animal": [["بقرة", "cow"]]}}}
    check("khanat: a good file passes; a sample under the wrong letter, a control character, a wrong version, a missing version all fail",
          one("khanat", ok_k) == [] and one("khanat", {**ok_k, "s": {"ت": {"animal": [["بقرة", "cow"]]}}}) and one("khanat", {**ok_k, "s": {"ب": {"animal": [["بق\x01رة", "cow"]]}}})
          and one("khanat", {**ok_k, "v": 2}) and one("khanat", {k: v for k, v in ok_k.items() if k != "v"}))
    mw = lambda **kw: {"v": 1, "w": [{"i": "a1", "k": "f", "d": 1, "a": 0, "t": ["عاصمة مصر ___", "x ___"], "w": ["القاهرة", "Cairo"], **kw}]}
    check("missing-word: a good file passes; no blank, two blanks, difficulty 4, a duplicate id and an extra key fail",
          one("missing-word", mw()) == [] and one("missing-word", mw(t=["بلا فراغ", "x ___"])) and one("missing-word", mw(t=["___ و___", "x ___"])) and one("missing-word", mw(d=4))
          and one("missing-word", {"v": 1, "w": [mw()["w"][0], mw()["w"][0]]}) and one("missing-word", mw(extra=1)))
    wd = lambda w: {"v": 1, "w": [[w, w, "gloss", 1, 0]]}
    check("word5-answers: five normalized letters only (no ة, no hamza forms, not four letters)", one("word5-answers", wd("مدرسه")) == [] and one("word5-answers", wd("مدرسة")) and one("word5-answers", wd("أدرسه")) and one("word5-answers", wd("مدرس")))
    check("word5-guesses: the length must match the count, only the packed letters, sorted and unique", one("word5-guesses", m.pack(["ابتثج", "بابها"])) == []
          and one("word5-guesses", {**m.pack(["ابتثج"]), "n": 2}) and one("word5-guesses", {**m.pack(["ابتثج"]), "w": "abcdZ"}) and one("word5-guesses", m.pack(["بابها", "ابتثج"])))
    big = {"v": 1, "t": [{"i": "t1", "n": ["a", "b"], "w": [[f"كلمة{i}", "w", 1] for i in range(12)]}], "pad": "x" * 200_000}
    check("a file over its byte limit is refused", one("odd-one", big))
    check("odd-one: a topic with fewer than 12 words or a repeated word fails", one("odd-one", {"v": 1, "t": [{"i": "t1", "n": ["ا", "a"], "w": [["كلمة", "w", 1]] * 12}]}) and one("odd-one", {"v": 1, "t": [{"i": "t1", "n": ["ا", "a"], "w": [[f"ك{i}", "w", 1] for i in range(5)]}]}))

wf = "\n".join(l for l in (ROOT / ".github/workflows/quiz-bank.yml").read_text(encoding="utf-8").splitlines() if not l.lstrip().startswith("#"))
check("the workflow checks the games data (check.py and check_words.py) before it builds, verifies every built file, and only then publishes quiz and games in the one orphan commit",
      wf.index("games/check_words.py") < wf.index("make_games_data.py build") < wf.index("make_games_data.py --verify") < wf.index("git push -f origin data-games") and "git add quiz games" in wf and "--orphan data-games" in wf)
src = (ROOT / ".github/tools/make_games_data.py").read_text(encoding="utf-8")
check("the tool and its docs carry no personal data and no AI lines", "Claude" not in src + wf and "Co-Authored" not in src + wf)
sys.exit(1 if failed else 0)
