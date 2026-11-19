#!/usr/bin/env python3
"""4.5 honours study (.github/tools/honours_study.py): a season's champion from our archive only when it is sure, a cup's final with both
legs, a level final or an unfinished season says nothing, MLS's table leader is never «champion»; the keys and names used for the checks.

    python3 tests/honours_study_test.py
"""
import datetime as dt
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / ".github/tools"))
import honours_study as hs  # noqa: E402

failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


NOW = dt.datetime(2026, 11, 18, tzinfo=dt.timezone.utc)
day = lambda d: (NOW - dt.timedelta(days=d)).strftime("%Y-%m-%dT%H:%M:%SZ")
def g(h, a, hs_, as_, ago=100, r="1", st="FT", hn=None, an=None):
    return {"h": h, "a": a, "hn": hn or "T" + h, "an": an or "T" + a, "hs": None if hs_ is None else str(hs_), "as": None if as_ is None else str(as_),
            "st": st, "t": day(ago), "r": r}
# a full double round-robin of 4 clubs: A wins everything at home, the rest draws
TEAMS = ["A", "B", "C", "D"]
full = [g(h, a, 2 if h == "A" else 1, 0 if h == "A" else 1, ago=100 + i) for i, (h, a) in enumerate((h, a) for h in TEAMS for a in TEAMS if h != a)]
c = hs.champion("4668", full, NOW)
check("a finished league: the top of the final table, and by how much", c["state"] == "champion" and c["id"] == "A" and "ahead of" in c["how"], c)
c = hs.champion("4668", full[:-1] + [g("D", "C", None, None, ago=-3, st="NS")], NOW)
check("a game still to play: no champion", c["state"].startswith("season not finished"), c)
c = hs.champion("4668", full[:-1] + [g("D", "C", 1, 1, ago=5)], NOW)
check("the last game less than 21 days ago: no champion yet (later games may be missing)", c["state"].startswith("season under way"), c)
c = hs.champion("4668", full[:8], NOW)
check("far fewer games than a full double round-robin (a split, play-offs, or the archive lacks them): no champion", c["state"].startswith("games missing"), c)
c = hs.champion("4346", full, NOW)
check("MLS: the table leader is never called champion (that is the MLS Cup)", c["state"].startswith("table leader only") and "id" not in c, c)
c = hs.champion("4480", [g("X", "Y", 1, 0, r="200", hn="Manchester City", an="Inter")], NOW)
check("a cup: the final's winner", c["state"] == "champion" and c["name"] == "Manchester City", c)
two = [g("H", "U", 1, 1, ago=200, r="200", hn="Al Hilal", an="Urawa"), g("U", "H", 1, 0, ago=193, r="200", hn="Urawa", an="Al Hilal")]
c = hs.champion("4719", two, NOW)
check("a two-legged final: both legs added up (AFC 2022: Urawa 2-1 on aggregate)", c["state"] == "champion" and c["id"] == "U" and "2 legs" in c["how"], c)
c = hs.champion("4480", [g("P", "R", 1, 1, r="200")], NOW)
check("a level final: nothing said (penalties are not in the archive)", c["state"].startswith("final level") and "id" not in c, c)
check("season keys: a season from August is «2023-2024», one from February is «2023-2024» too, and its year",
      hs.season_keys("2023-08-11") == ["2023-2024", "2023"] and hs.season_keys("2024-02-10") == ["2023-2024", "2024"])
check("club names: ours and Wikidata's match on a distinctive word, never on «FC» or «Al»",
      hs.same_club("Bayern Munich", "FC Bayern München") and hs.same_club("Paris SG", "Paris Saint-Germain FC") and hs.same_club("Al Hilal", "Al Hilal SFC")
      and not hs.same_club("Al Hilal", "Al Nassr") and not hs.same_club("FC Porto", "FC Barcelona"))
sys.exit(1 if failed else 0)
