#!/usr/bin/env python3
"""games/meta.json and quiz-v2.json: the builder is deterministic and verifies, the reader skips what it does not know instead of refusing the file, and the old bare array
stays a valid input of the old apps (games/SCHEMA.md)."""
import json, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "games/tools"))
import meta_reader as r

failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1

with tempfile.TemporaryDirectory() as t:
    run = lambda *a: subprocess.run([sys.executable, "-I", str(ROOT / "games/tools/build_meta.py"), *a], capture_output=True, text=True)
    a, b = Path(t, "a"), Path(t, "b")
    p1, p2 = run(str(ROOT / "games"), str(a)), run(str(ROOT / "games"), str(b))
    check("the builder runs and its output verifies", p1.returncode == 0 and run("--verify", str(a)).returncode == 0, p1.stderr[-300:])
    check("a rebuild is the same bytes (sorted, no dates)", p2.returncode == 0 and (a / "meta.json").read_bytes() == (b / "meta.json").read_bytes() and (a / "quiz-v2.json").read_bytes() == (b / "quiz-v2.json").read_bytes())
    m = r.read((a / "meta.json").read_text(encoding="utf-8"))
    chips = r.quiz_chips(m) if m else []
    check("the reader builds the quiz chips from the data: geography and science are offered, a group with no data yet (sports) is listed but not offered", m is not None
          and any(c[0] == "geography" and c[4] for c in chips) and any(c[0] == "science" and c[4] for c in chips) and any(c[0] == "sports" and not c[4] for c in chips), chips)
    check("level and kids change what is offered (hard Islamic is offered, easy Saudi culture is not)", any(c[0] == "islamic" and c[4] for c in r.quiz_chips(m, 3)) and any(c[0] == "saudi" and not c[4] for c in r.quiz_chips(m, 1)))
    # growth without an app update: a new category, new fields, a nameless entry, an unknown game
    g = json.loads(json.dumps(m))
    g["future"] = {"x": 1}; g["games"]["new-game"] = {"cats": []}; g["games"]["quiz"]["extra"] = 5
    g["games"]["quiz"]["cats"].append({"id": "cinema", "ar": "سينما", "en": "Cinema", "n": [50, 50, 50], "kids": 0, "color": "red"})
    g["games"]["quiz"]["cats"].append({"id": "broken", "n": [1, 2, 3]})
    g["groups"].append({"id": "nameless", "cats": ["cinema"]})
    c2 = r.quiz_chips(g)
    check("growth: a category new in the data shows up by itself under its own name, unknown fields and games are skipped, a nameless entry is skipped and the file still works",
          any(c[0] == "cinema" and c[4] and c[1] == "سينما" for c in c2) and not any(c[0] in ("broken", "nameless") for c in c2) and len(c2) >= len(chips))
    check("a file of another version, a broken file and an HTML page are ignored (the app keeps its built-in lists)", r.read(json.dumps({**m, "v": 2})) is None and r.read("{") is None and r.read("<html>") is None)
    q = r.read_quiz_v2((a / "quiz-v2.json").read_text(encoding="utf-8"))
    d = json.loads((a / "quiz-v2.json").read_text(encoding="utf-8"))
    d["items"][0]["tags"] = ["new"]; d["items"].append({"i": "bad"}); d["items"].append({**d["items"][1], "i": "dup-options", "w": [d["items"][1]["w"][0]] * 3})
    q2 = r.read_quiz_v2(json.dumps(d))
    check("quiz-v2: an item with an extra field is kept, a broken item and an item with repeated options are skipped, not the whole bank", q is not None and q2 is not None and len(q2) == len(q) and "tags" in q2[0])
sys.exit(1 if failed else 0)
