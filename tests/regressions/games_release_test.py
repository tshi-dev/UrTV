#!/usr/bin/env python3
"""Unreleased games never ship by accident (the owner's rule: unreleased games, designs and content stay in the private repositories).
Every game id of GameRegistry.ALL must have its line in tests/regressions/games_released.txt: a new game id with no line fails at once, on
every branch. A line says the version it was released in, or «pending» (on next for testing, the owner's OK not given yet). The release gate
(regressions.yml called by android.yml with release: true, GAMES_RELEASE_STRICT=1) also fails on a game that is ready (a playable tile)
and still «pending»: nothing reaches apk-latest that the owner did not release. A game that is not ready shows «قريبًا» and opens nothing.
Run: python3 tests/regressions/games_release_test.py"""
import os, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REG = ROOT / "android/app/src/main/java/sa/urtv/screensaver/GameRegistry.java"
LIST = Path(__file__).resolve().parent / "games_released.txt"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def registry(src):
    """{id: ready} of GameRegistry.ALL"""
    rx = r'new Game\("([a-z0-9-]+)",(?:[^()]|\([^()]*\))*?,\s*(true|false)\s*,\s*(true|false)\s*\)'
    return {m.group(1): m.group(3) == "true" for m in re.finditer(rx, src)}


def released(text):
    out = {}
    for ln in text.splitlines():
        if ln.strip() and not ln.startswith("#"):
            p = [x.strip() for x in ln.split("|")]
            out[p[0]] = p[1] if len(p) > 1 else ""
    return out


def problems(games, rel, strict):
    out = [f"{g}: no line in games_released.txt" for g in games if g not in rel]
    out += [f"{g}: «{rel[g]}» is neither a version nor «pending»" for g in games if g in rel and not re.fullmatch(r"\d+\.\d+(\.\d+)?|pending", rel[g])]
    if strict:
        out += [f"{g}: ready in the registry and still «pending» (the owner's OK, docs/dev/RELEASE.md step 5)" for g, ready in games.items()
                if ready and rel.get(g) == "pending"]
    return out


def main():
    probe_reg = 'new Game("a", "x", "y", "A", false, true),\n new Game("b", "x", "y (z)", "B", true, true),\n new Game("c", "x", "y", "C", false, false),'
    g = registry(probe_reg)
    check("the registry is read: ids and their «ready»", g == {"a": True, "b": True, "c": False}, g)
    p = problems({"a": True, "b": True, "c": False, "d": True}, {"a": "4.3", "b": "pending", "c": "pending", "d": "soon"}, strict=True)
    check("a new id fails, a bad release word fails, a ready «pending» fails in a release, a «قريبًا» one does not",
          [x.split(":")[0] for x in p] == ["d", "b"] and "neither" in p[0], p)
    if not REG.is_file():
        print("SKIP no GameRegistry.java on this branch")
        return 1 if failed else 0
    games = registry(REG.read_text(encoding="utf-8"))
    rel = released(LIST.read_text(encoding="utf-8"))
    check(f"the registry has games ({len(games)})", len(games) >= 8, games)
    strict = os.environ.get("GAMES_RELEASE_STRICT") == "1"
    bad = problems(games, rel, strict)
    check(("a release: " if strict else "") + "every game of the registry has its release line" + (" and none ready is pending" if strict else ""), not bad, bad)
    gone = sorted(set(rel) - set(games))
    if gone:
        print("NOTE in games_released.txt but not in the registry: " + ", ".join(gone))
    pend = sorted(x for x, ready in games.items() if ready and rel.get(x) == "pending")
    if pend and not strict:
        print(f"NOTE {len(pend)} ready games still «pending» (a release to master fails until the owner releases them): " + ", ".join(pend))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
