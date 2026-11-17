#!/usr/bin/env python3
"""Regression gate R-KEYS (4.3.4: «تعديل المفضلة» and «تعديل التطبيقات» swallowed a remote's app, colour and maker keys; a Sony remote
only had its numbers, Home, volume and Settings). Over the home, the screensaver app and the rescue app:

1. every key method of ours (`boolean x(KeyEvent e)`, onKeyDown/onKeyUp/onKey/dispatchKeyEvent) has a way to say «not mine»: a
   `return false`, a `return super.…`, or a returned condition — a handler that can only answer true takes every key;
2. a key handler whose last word is `return true` (every key that reached its end is taken) is named in KEEPS_ALL below with its reason;
   a new one fails until a reviewer adds it (docs/dev/RELEASE.md);
3. the two edit modes still ask FavEdit.owns first (the JVM rule itself is tests/edit_keys_test.py).
Run: python3 tests/regressions/key_handlers_test.py"""
import re, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import javagraph as g  # noqa: E402

ROOT = g.ROOT
SRCS = g.SRC
HANDLERS = ("onKeyDown", "onKeyUp", "onKeyLongPress", "onKey", "dispatchKeyEvent")
# app:Class.method -> why it may take every key that reaches its end
KEEPS_ALL = {
    "saver:GameActivity.dispatchKeyEvent": "a full-screen game: Back and Home are handled before the end; the game owns the rest",
    "saver:QuizActivity.dispatchKeyEvent": "a full-screen game (the quiz room): Back is handled before the end",
    "saver:MainActivity.dispatchKeyEvent": "the screensaver's preview: any key ends the preview (as the dream does), Back included",
    "launcher:DockActivity.dispatchKeyEvent": "QA-5 open: while an app is being moved in «ترتيب المفضلة» every key is taken (the edit-mode bug of 4.3.4, in its own screen); outside the move every key goes on",
}
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

PASSES = re.compile(r"return\s+false|return\s+super\.|return\s+!?[a-z]\w*\s*;|return\s+!?[\w.]+\(|return\s+[^;]*(&&|\|\||\?|!=|==|<|>)")
no_pass, takes_all, seen = [], [], 0
for app, src in SRCS.items():
    for p in sorted(src.rglob("*.java")):
        s = g.blank(p.read_text(encoding="utf-8"))
        cls = (g._CLASS.search(s) or [None, p.stem])[1]
        heads = [(m.group(1), m.end() - 1) for m in re.finditer(r"\bboolean\s+(\w+)\s*\(\s*(?:final\s+)?KeyEvent\s+\w+\s*\)\s*\{", s)]
        heads += [(m.group(1), m.end() - 1) for m in re.finditer(r"\bboolean\s+(" + "|".join(HANDLERS) + r")\s*\([^)]*\)\s*\{", s)]
        for b0, name in {b: n for n, b in heads}.items():
            body = s[b0:g.match_paren(s, b0)]
            seen += 1
            key = f"{app}:{cls}.{name}"
            if not PASSES.search(body):
                no_pass.append(key)
            stmts = [x.strip() for x in re.split(r"[;{}]", body) if x.strip()]
            if stmts and re.fullmatch(r"return\s+true", stmts[-1]) and name in HANDLERS:
                takes_all.append(key)
check(f"the key handlers were found ({seen})", seen >= 20, seen)
check("every key method can answer «not mine» (false, super, or a condition)", not no_pass, no_pass)
new = sorted(set(takes_all) - set(KEEPS_ALL))
check("a handler that takes every key reaching its end is named in KEEPS_ALL with a reason", not new, new)
gone = sorted(set(KEEPS_ALL) - set(takes_all))
if gone:
    print("NOTE no longer takes every key (remove from KEEPS_ALL): " + ", ".join(gone))
main = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
for fn in ("editKey", "appsEditKey"):
    body = main.split(f"private boolean {fn}(KeyEvent e) {{")[1].split("\n    }\n")[0] if f"private boolean {fn}(KeyEvent e) {{" in main else ""
    check(f"{fn} lets a key it does not use go on (FavEdit.owns)", "FavEdit.owns(k)) return false;" in body)
sys.exit(1 if failed else 0)
