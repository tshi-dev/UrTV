#!/usr/bin/env python3
"""The owner (2026-11-27): no Palestine and no Israel in any game or quiz, in any spelling. Every bundled quiz and game
file (assets/quiz, assets/games), the encrypted banks opened in memory (games/tools/pack_bundle.py). The hadith and the
verses are not game data and are not checked. Exit 1 on a hit."""
import glob, importlib.util, re, sys, unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NAMES = re.compile(r"اسرا[ءئي]?يل|فلسطين|israel|isreal|palestin|filast", re.I)   # as games/check.py (work/games-data)
fails = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  [{detail}]"))
    if not ok:
        fails.append(name)


def fold(t):   # harakat, hamza forms, tatweel and accents folded first
    t = unicodedata.normalize("NFKD", t)
    return re.sub("[̀-ًͯ-ٰٟـ]", "", t)


def found(text):
    return sorted({m.lower() for m in NAMES.findall(fold(text))})


check("the check finds every spelling it must", all(found(t) for t in
      ["فِلَسْطِين", "إسرائيل", "إسرائيـل", "اسرائيلية", "الفلسطينيون", "Palestine", "Israël", "ISRAELI"]))
check("and nothing else (القدس، الأردن، إسرافيل)", not any(found(t) for t in ["القدس", "الأردن", "إسرافيل", "إسراء"]))

plain = sorted(glob.glob(str(ROOT / "assets/quiz/*.json")) + glob.glob(str(ROOT / "assets/games/*.json")))
hits = [(Path(f).name, found(open(f, encoding="utf-8").read())) for f in plain]
check(f"no Palestine or Israel in the {len(plain)} quiz and game files", not any(h for _, h in hits), [h for h in hits if h[1]])

spec = importlib.util.spec_from_file_location("pack_bundle", ROOT / "games/tools/pack_bundle.py")
pb = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pb)
banks = sorted(glob.glob(str(ROOT / "assets/games/*.gb")))
hits = [(Path(f).name, found(pb.unpack(Path(f).stem, open(f, "rb").read()).decode("utf-8"))) for f in banks]
check(f"nor in the {len(banks)} encrypted banks (opened in memory)", banks and not any(h for _, h in hits), [h for h in hits if h[1]])

sys.exit(1 if fails else 0)
