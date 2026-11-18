#!/usr/bin/env python3
"""4.5 honours study (docs/HONOURS-STUDY.md): a season's champion from our own match archive, and the season keys and club names that
let it be checked against Wikidata (CC0) and ar.wikipedia. Phase 1 is research: this file is the logic the proposed urtv-data job would
use, kept testable (tests/honours_study_test.py); it reads and writes nothing by itself.

- A league's champion is the top of its final table, said only when the season is surely over: every game played, the last one at least
  21 days ago, and close to a full double round-robin (a league with a split or play-offs, or whose games the archive lacks, says nothing).
- A cup's champion is the final's winner, both legs of a two-legged final added up; a level final says nothing (the archive has no
  penalties or away goals).
- MLS's table leader is the Supporters' Shield, not the champion (that is the MLS Cup, a play-off): never called champion.
"""
import datetime as dt
import re

FINAL = {"FT", "AET", "PEN", "MATCH FINISHED", "AOT", "AP"}
CUPS = {"4480", "4483", "4484", "4490", "4719", "5649"}   # UCL, Copa del Rey, Coupe de France, Nations League, AFC Elite, King Cup
TABLE_ONLY = {"4346"}   # MLS: the table leader is not the champion
RECENT = dt.timedelta(days=21)
FULL_SHARE = 0.9


def done(e):
    return e.get("hs") not in (None, "") and e.get("as") not in (None, "") and str(e.get("st") or "").upper() in FINAL


def table(games):
    """[(id, name, pts, gd, gf)] best first, from finished games: 3 a win, 1 a draw"""
    t = {}
    for e in games:
        if not done(e):
            continue
        hs, as_ = int(e["hs"]), int(e["as"])
        for me, n, gf, ga in ((e["h"], e.get("hn", ""), hs, as_), (e["a"], e.get("an", ""), as_, hs)):
            r = t.setdefault(me, [me, n, 0, 0, 0])
            r[2] += 3 if gf > ga else 1 if gf == ga else 0
            r[3] += gf - ga
            r[4] += gf
    return sorted((tuple(r) for r in t.values()), key=lambda r: (-r[2], -r[3], -r[4], r[1]))


def champion(league, events, now):
    """{"state": ..., and for a champion "id", "name", "how"} for one season's events of the archive's store"""
    ev = [e for e in events if not e.get("pp")]
    if not ev:
        return {"state": "no games"}
    if league in CUPS:
        fin = sorted((e for e in ev if str(e.get("r")) == "200"), key=lambda e: e["t"])
        if not fin:
            return {"state": "no final yet"}
        if not all(done(e) for e in fin):
            return {"state": "final not played"}
        a, b = fin[0]["h"], fin[0]["a"]
        name = {fin[0]["h"]: fin[0].get("hn", ""), fin[0]["a"]: fin[0].get("an", "")}
        ga = sum(int(e["hs"]) if e["h"] == a else int(e["as"]) for e in fin)
        gb = sum(int(e["as"]) if e["a"] == b else int(e["hs"]) for e in fin)
        how = f"final ({len(fin)} leg{'s' if len(fin) > 1 else ''}) {name[a]} {ga}-{gb} {name[b]}"
        if ga == gb:
            return {"state": "final level (penalties or away goals are not in the archive)", "how": how}
        w = a if ga > gb else b
        return {"state": "champion", "id": w, "name": name[w], "how": how}
    games = [e for e in ev if str(e.get("r") or "").isdigit() and int(e["r"]) < 100]
    left = [e for e in games if not done(e)]
    if left:
        return {"state": f"season not finished ({len(left)} games left)"}
    t = table(games)
    if len(t) < 2:
        return {"state": "no table"}
    last = max(dt.datetime.fromisoformat(e["t"].replace("Z", "+00:00")).replace(tzinfo=dt.timezone.utc) for e in games)
    if now - last < RECENT:
        return {"state": "season under way (its last game is recent)"}
    full = len(t) * (len(t) - 1)
    if len(games) < FULL_SHARE * full:
        return {"state": f"games missing ({len(games)} of a full {full}): no champion said"}
    if league in TABLE_ONLY:
        return {"state": "table leader only (not the champion)", "name": t[0][1]}
    gap = t[0][2] - t[1][2]
    how = f"{t[0][2]} pts, {gap} ahead of {t[1][1]}" + (" (level on points: goal difference here, the league's own tie-break may differ)" if gap == 0 else "")
    return {"state": "champion", "id": t[0][0], "name": t[0][1], "how": how}


def season_keys(start):
    """a Wikidata season's start date as the archive's season keys: a season starting from June is «y-y+1», else «y-1-y»; and the year"""
    y, m = int(start[:4]), int(start[5:7])
    return [f"{y}-{y + 1}" if m >= 6 else f"{y - 1}-{y}", str(y)]


STOP = {"fc", "cf", "sc", "afc", "club", "football", "fk", "sk", "ac", "as", "ss", "calcio", "de", "the", "cp", "sl", "ssc", "saudi", "futbol",
        "sport", "sporting", "sfc", "al"}
ALIAS = {"munchen": "munich", "inter": "internazionale", "psg": "paris"}


def words(name):
    s = name.lower()
    for a, b in (("ç", "c"), ("é", "e"), ("ü", "u"), ("ö", "o"), ("ú", "u"), ("-", " ")):
        s = s.replace(a, b)
    return {ALIAS.get(w, w) for w in re.sub(r"[^a-z0-9 ]", " ", s).split() if w not in STOP and len(w) > 1}


def same_club(a, b):
    """our name and Wikidata's label of a club (or its men's team, Wikidata names either) share a distinctive word"""
    return bool(words(a) & words(b))
