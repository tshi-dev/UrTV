#!/usr/bin/env python3
"""The photo rotation across «صورنا» and every pack is shuffled (4.3): the page's own shuffle, taken out of the template and run in node:
a permutation, the same for one round and different between rounds, and the rotation uses it (not for the owner's own photos alone).

    python3 tests/shuffle_test.py
"""
import re, shutil, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


tpl = (ROOT / "src/template.html").read_text(encoding="utf-8")
src = re.search(r"const shuf = (.*?return r; \};)", tpl, re.S)
check("the rotation picks from a shuffled pool, a new order for each round, never for the owner's own photos alone",
      src is not None and "(only ? pool : shuf(pool, Math.floor(slot / pool.length)))[slot % pool.length]" in tpl)
if src and shutil.which("node"):
    js = "const shuf = " + src.group(1) + """
const pool = Array.from({length: 40}, (_, i) => i);
const a = shuf(pool, 0), a2 = shuf(pool, 0), b = shuf(pool, 1);
const same = (x, y) => x.every((v, i) => v === y[i]);
const perm = x => x.slice().sort((p, q) => p - q).every((v, i) => v === i);
const moved = a.filter((v, i) => v !== i).length;
console.log(JSON.stringify({perm: perm(a) && perm(b), det: same(a, a2), diff: !same(a, b), moved, one: shuf([7], 3)[0] === 7, none: shuf([], 3).length === 0}));
"""
    out = subprocess.run(["node", "-e", js], capture_output=True, text=True).stdout.strip()
    import json
    r = json.loads(out)
    check("a permutation of the pool (nothing lost, nothing twice)", r["perm"], out)
    check("the same order inside a round, another one in the next", r["det"] and r["diff"], out)
    check("it is a real shuffle (most photos moved from their place), and one photo or none are fine", r["moved"] > 30 and r["one"] and r["none"], out)
sys.exit(1 if failed else 0)
