#!/usr/bin/env python3
"""Pick clubs at random from our list, for notes/CYCLE-TEST-PROMPT.md: names to try with find-team, the way people
would write them. Always one Saudi club in Arabic, one national team, and one club abroad in English; the rest at
random, Arabic or English. Prints the seed first, so a run can be repeated.

    optimize/venv/bin/python -I optimize/random-clubs.py            6 to 10 names
    optimize/venv/bin/python -I optimize/random-clubs.py 8 1234     8 names, seed 1234
"""
import json
import random
import sys
from pathlib import Path

for stream in (sys.stdout, sys.stderr):   # Windows writes pipes in its own code page
    if hasattr(stream, "reconfigure"):
        stream.reconfigure(encoding="utf-8", errors="replace", newline="\n")

CLUBS = json.loads((Path(__file__).resolve().parents[1] / "assets/shared/teams.json").read_text(encoding="utf-8"))


def main():
    seed = int(sys.argv[2]) if len(sys.argv) > 2 else random.randrange(10 ** 6)
    rnd = random.Random(seed)
    count = int(sys.argv[1]) if len(sys.argv) > 1 else rnd.randint(6, 10)
    saudi = [c for c in CLUBS if c["ar"] and any("Saudi" in l for l in c["leagues"])]
    national = [c for c in CLUBS if "National teams" in c["leagues"]]
    abroad = [c for c in CLUBS if not any("Saudi" in l or l == "National teams" for l in c["leagues"])]
    picks = [(rnd.choice(saudi), "ar"), (rnd.choice(national), "any"), (rnd.choice(abroad), "en")]
    chosen = {c["id"] for c, _ in picks}
    rest = [c for c in CLUBS if c["id"] not in chosen]
    picks += [(c, "any") for c in rnd.sample(rest, max(0, min(count, 10) - len(picks)))]
    print(f"seed: {seed}")
    for c, how in picks:
        arabic = how == "ar" or (how == "any" and c["ar"] and rnd.random() < 0.5)
        print(c["ar"] if arabic else c["en"])


if __name__ == "__main__":
    main()
