#!/usr/bin/env python3
"""Unreleased games never ship by accident (the owner's rule: unreleased games, designs and content stay in the private repositories).
Every game id of GameRegistry.ALL must have its line in tests/regressions/games_released.txt: a new game id with no line fails at once, on
every branch. A line says the version it was released in, «pending» (not released to everyone) or «always» (not a game: «حزم المحتوى»).
The release gate (regressions.yml called by android.yml with release: true, GAMES_RELEASE_STRICT=1) also fails on a game that is ready (a
playable tile) and «pending» unless it is held back: in GameRegistry.HELD, which the games list hides unless the release switch turns it on
(QuizGamesActivity.shown: `!GameRegistry.held(g.id) || GameReleaseStore.nativeOn(g.id)`). Nothing reaches apk-latest as a tile everyone sees
that the owner did not release (the owner, 18 Nov: 4.4's eleven new games ship held, group B first).
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


def problems(games, rel, strict, held=()):
    out = [f"{g}: no line in games_released.txt" for g in games if g not in rel]
    out += [f"{g}: «{rel[g]}» is neither a version, «pending» nor «always»" for g in games
            if g in rel and not re.fullmatch(r"\d+\.\d+(\.\d+)?|pending|always", rel[g])]
    out += [f"{g}: held back (GameRegistry.HELD) but released for everyone in {rel[g]}" for g in held if re.fullmatch(r"\d+\.\d+(\.\d+)?", rel.get(g, ""))]
    out += [f"{g}: in GameRegistry.HELD but not in the registry" for g in held if g not in games]
    if strict:
        out += [f"{g}: ready, «pending» and not held back (GameRegistry.HELD): a tile everyone would see (the owner's OK, docs/dev/RELEASE.md step 5)"
                for g, ready in games.items() if ready and rel.get(g) == "pending" and g not in held]
    return out


def main():
    probe_reg = 'new Game("a", "x", "y", "A", false, true),\n new Game("b", "x", "y (z)", "B", true, true),\n new Game("c", "x", "y", "C", false, false),'
    g = registry(probe_reg)
    check("the registry is read: ids and their «ready»", g == {"a": True, "b": True, "c": False}, g)
    p = problems({"a": True, "b": True, "c": False, "d": True, "e": True, "f": True, "g": True},
                 {"a": "4.3", "b": "pending", "c": "pending", "d": "soon", "e": "pending", "f": "always", "g": "4.4"}, strict=True, held=("e", "g", "x"))
    check("a bad release word fails; HELD must be pending and in the registry; in a release a ready «pending» fails unless held; «قريبًا» and «always» pass",
          sorted(x.split(":")[0] for x in p) == ["b", "d", "g", "x"], p)
    if not REG.is_file():
        print("SKIP no GameRegistry.java on this branch")
        return 1 if failed else 0
    src = REG.read_text(encoding="utf-8")
    games = registry(src)
    m = re.search(r"static final String\[\] HELD = \{([^}]*)\}", src)
    held = re.findall(r'"([a-z0-9-]+)"', m.group(1)) if m else []
    hub = ROOT / "android/app/src/main/java/sa/urtv/screensaver/QuizGamesActivity.java"
    if held:
        check("the games list hides a held game unless the release switch turns it on (QuizGamesActivity.shown)",
              hub.is_file() and "!GameRegistry.held(g.id) || GameReleaseStore.nativeOn(g.id)" in hub.read_text(encoding="utf-8"))
    rel = released(LIST.read_text(encoding="utf-8"))
    check(f"the registry has games ({len(games)})", len(games) >= 8, games)
    strict = os.environ.get("GAMES_RELEASE_STRICT") == "1"
    bad = problems(games, rel, strict, held)
    check(("a release: " if strict else "") + "every game of the registry has its release line" + (" and none ready is pending" if strict else ""), not bad, bad)
    gone = sorted(set(rel) - set(games))
    if gone:
        print("NOTE in games_released.txt but not in the registry: " + ", ".join(gone))
    pend = sorted(x for x, ready in games.items() if ready and rel.get(x) == "pending" and x not in held)
    if pend and not strict:
        print(f"NOTE {len(pend)} ready games «pending» and not held (a release to master fails on them): " + ", ".join(pend))
    print(f"NOTE held back (the release switch only): {len(held)}: " + ", ".join(held))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
