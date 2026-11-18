#!/usr/bin/env python3
"""Regression gate R-PACKS: the photo packs are downloaded on the TV (docs/PHOTO-PACKS.md), never put in an APK (an APK that carries
one grows by tens of MB for a setting most people never turn on). Checked: no assets source of any app names photos/, a pack or the
data-photo-packs branch; no image over 600 KB under any app's src/main (assets or res); the repository's assets/ has no packs folder.
The same for game and question packs (4.4: 580 pack questions, assets/games/bank-extra.gb, almost shipped inside the APK): only the public
starter banks below may be in assets/ (which the screensaver's APK carries whole); any other *.gb, or a quiz bank file beyond them, fails
until a reviewer adds it here with a reason.
Run: python3 tests/regressions/no_bundled_packs_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

for gr in sorted((ROOT / "android").glob("*/build.gradle")):
    src = gr.read_text(encoding="utf-8")
    bad = [ln.strip() for ln in src.splitlines() if re.search(r"from\s*\(|srcDirs", ln) and re.search(r"photos|packs?\b|photo-packs", ln.split("//")[0])]
    check(f"{gr.parent.name}/build.gradle copies no photo pack into the APK", not bad, bad)
big = [str(p.relative_to(ROOT)) for p in (ROOT / "android").glob("*/src/main/**/*")
       if p.is_file() and p.suffix.lower() in (".jpg", ".jpeg", ".png", ".webp") and p.stat().st_size > 600_000]
check("no image over 600 KB inside an app's sources", not big, big)
check("assets/ has no packs folder", not any((ROOT / "assets").glob("*pack*")), list((ROOT / "assets").glob("*pack*")))
# the public starter content of «الألعاب» (4.4, next): every other *.gb is a pack, downloaded on the TV, never in an APK
STARTER_GB = {"author.gb", "closest.gb", "emoji.gb", "guess.gb", "liar.gb", "link.gb", "logic.gb"}
STARTER_QUIZ = {"bank.json", "starter.json", "game.html", "play.html"}
gb = sorted(str(p.relative_to(ROOT)) for p in ROOT.glob("assets/**/*.gb") if p.name not in STARTER_GB)
gb += sorted(str(p.relative_to(ROOT)) for p in (ROOT / "android").glob("*/src/main/assets/**/*.gb"))
check("no game pack in the APKs' assets: only the starter banks (" + ", ".join(sorted(STARTER_GB)) + ")", not gb, gb)
quiz = sorted(p.name for p in (ROOT / "assets/quiz").glob("*") if p.is_file() and p.name not in STARTER_QUIZ) if (ROOT / "assets/quiz").is_dir() else []
check("no question pack in assets/quiz (the starter bank only)", not quiz, quiz)
extra = [str(p.relative_to(ROOT)) for p in ROOT.glob("assets/**/*") if p.is_file() and re.search(r"(extra|pack)", p.name, re.I) and p.suffix in (".gb", ".json", ".bin")]
check("nothing named *extra*/*pack* in assets/ (bank-extra.gb was the near miss)", not extra, extra)

def probe(names):
    return [n for n in names if n.endswith(".gb") and n not in STARTER_GB]
check("the rule catches bank-extra.gb and lets the starter banks through", probe(["bank-extra.gb", "logic.gb"]) == ["bank-extra.gb"])
sys.exit(1 if failed else 0)
