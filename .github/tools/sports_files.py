#!/usr/bin/env python3
"""4.4 «الرياضة»: the small files the sports cards read, beside the match archive (docs/MATCH-ARCHIVE.md). No API call but f1.

  sports/<leagueId>.json  one file per league, for every card of that league: its table computed from the store's current season (3 points
                          a win, 1 a draw; goal difference, goals for, name), each club's last 5 (newest first, "w" "d" "l"), the finished
                          games of the last 2 days, the games of the next 7 days, and for a cup the knockout games by round.
                          4.5: the seasons the store keeps (newest first) and each one's champion when the archive is sure of it
                          (honours_study.champion: a finished league's top, a cup's final; docs/HONOURS-STUDY.md).
  sports/<leagueId>/<season>.json  4.5 each past season the store keeps: its final table (or a cup's rounds), every result, its champion.
  f1/next.json            the next Formula 1 race and its sessions from Jolpica-F1 (the free successor of Ergast), asked at most once a
                          day; a failed ask keeps the last file.

    derive(store, out, prev, now, stamp, teams)   (called by match_archive.py build)
    python3 .github/tools/sports_files.py f1 --prev DIR --out DIR
"""
import argparse
import datetime as dt
import json
import shutil
import urllib.request
from pathlib import Path

import honours_study   # 4.5 a season's champion, said only when the archive is sure

V = 1
LAST_DAYS, NEXT_DAYS, FORM = 2, 7, 5
FINAL = {"FT", "AET", "PEN", "MATCH FINISHED", "AOT", "AP"}
# TheSportsDB's round numbers of a knockout stage (a league's rounds are 1..38): their Arabic names
KO = {"32": "دور الـ٣٢", "16": "دور الـ١٦", "125": "ربع النهائي", "150": "نصف النهائي", "160": "المركز الثالث", "200": "النهائي",
      "170": "الملحق", "180": "نهائي الملحق"}
KO_ORDER = ["32", "16", "125", "150", "160", "200"]
F1_URL = "https://api.jolpi.ca/ergast/f1/current/next.json"
F1_FIRST = "https://api.jolpi.ca/ergast/f1/{season}/1.json"   # after the last race of a season: the next season's first
F1_EVERY = dt.timedelta(hours=20)
F1_COUNTRY = {"Bahrain": "البحرين", "Saudi Arabia": "السعودية", "Australia": "أستراليا", "Japan": "اليابان", "China": "الصين", "USA": "الولايات المتحدة",
              "United States": "الولايات المتحدة", "Italy": "إيطاليا", "Monaco": "موناكو", "Spain": "إسبانيا", "Canada": "كندا", "Austria": "النمسا",
              "UK": "بريطانيا", "United Kingdom": "بريطانيا", "Hungary": "المجر", "Belgium": "بلجيكا", "Netherlands": "هولندا", "Azerbaijan": "أذربيجان",
              "Singapore": "سنغافورة", "Mexico": "المكسيك", "Brazil": "البرازيل", "Qatar": "قطر", "UAE": "أبوظبي", "United Arab Emirates": "أبوظبي",
              "Las Vegas": "لاس فيغاس", "Miami": "ميامي"}
# a race named for a city or region, not its country (three races in the USA, two in Italy): its own name first
F1_RACE = {"Las Vegas Grand Prix": "لاس فيغاس", "Miami Grand Prix": "ميامي", "United States Grand Prix": "الولايات المتحدة", "Emilia Romagna Grand Prix": "إميليا رومانيا",
           "Italian Grand Prix": "إيطاليا", "São Paulo Grand Prix": "ساو باولو", "Sao Paulo Grand Prix": "ساو باولو", "Mexico City Grand Prix": "مكسيكو سيتي",
           "Abu Dhabi Grand Prix": "أبوظبي", "Saudi Arabian Grand Prix": "السعودية", "British Grand Prix": "بريطانيا", "Dutch Grand Prix": "هولندا"}
SESSIONS = [("FirstPractice", "fp1"), ("SecondPractice", "fp2"), ("ThirdPractice", "fp3"), ("SprintQualifying", "sq"), ("SprintShootout", "sq"),
            ("Sprint", "sprint"), ("Qualifying", "q")]


def _t(e):
    try:
        return dt.datetime.fromisoformat(e["t"].replace("Z", "+00:00"))
    except (KeyError, ValueError):
        return None


def _done(e):
    return e.get("hs") not in (None, "") and e.get("as") not in (None, "") and (e.get("st") or "").upper() in FINAL


def _num(x):
    try:
        return int(x)
    except (TypeError, ValueError):
        return 0


def _game(e):
    return {"id": e.get("id") or f"d:{e['t'][:10]}:{e['h']}:{e['a']}", "lg": e.get("lg") or "", "t": e["t"], "r": e.get("r") or "", "h": e["h"], "a": e["a"],
            "hn": e.get("hn") or "", "an": e.get("an") or "", "hs": e.get("hs"), "as": e.get("as"), "st": e.get("st") or ""}


def season_now(store, lg, now):
    """the league's season that holds the games nearest to now (the newest one that has started, else the oldest ahead)"""
    seasons = sorted({s for (l, s) in store.leagues if l == lg})
    best = None
    for s in seasons:
        ts = [t for t in (_t(e) for e in store.leagues[(lg, s)].values()) if t]
        if ts and min(ts) <= now + dt.timedelta(days=30):
            best = s
    return best or (seasons[0] if seasons else None)


def table(games, names):
    """[{id, n, ar, p, w, d, l, gf, ga, pts, f}] from the finished games of a season, in order"""
    rows = {}
    for e in sorted((g for g in games if _done(g)), key=lambda g: g["t"]):
        hs, as_ = _num(e["hs"]), _num(e["as"])
        for me, them, gf, ga, n in ((e["h"], e["a"], hs, as_, e.get("hn")), (e["a"], e["h"], as_, hs, e.get("an"))):
            r = rows.setdefault(me, {"id": me, "n": n or "", "ar": names.get(me, {}).get("ar", ""), "p": 0, "w": 0, "d": 0, "l": 0, "gf": 0, "ga": 0,
                                     "pts": 0, "f": ""})
            res = "w" if gf > ga else "d" if gf == ga else "l"
            r["p"] += 1; r["gf"] += gf; r["ga"] += ga; r[res] += 1; r["pts"] += 3 if res == "w" else 1 if res == "d" else 0
            r["f"] = (res + r["f"])[:FORM]
    return sorted(rows.values(), key=lambda r: (-r["pts"], -(r["gf"] - r["ga"]), -r["gf"], r["n"]))


def movement(games, names):
    """4.4 beta 5 (league page phase a): {club id: places moved} since the highest finished round, from the table without that round's games
    (a postponed game counts in its own round). Up is positive. Empty when a finished game's round is not a number or the round is the first;
    a club with no row before it has none."""
    done = [g for g in games if _done(g)]
    rounds = [str(g.get("r") or "").strip() for g in done]
    if not done or not all(r.isdigit() for r in rounds):
        return {}
    top = max(int(r) for r in rounds)
    before = [g for g in done if int(str(g["r"]).strip()) != top]
    if not before:
        return {}
    was = {r["id"]: i for i, r in enumerate(table(before, names))}
    return {r["id"]: was[r["id"]] - i for i, r in enumerate(table(games, names)) if r["id"] in was}


def movement_for(games, names, is_cup):
    """a cup's file has no movement"""
    return {} if is_cup else movement(games, names)


TOURNAMENTS_FILE = Path(__file__).resolve().parents[2] / "assets/shared/tournaments.json"


def cup_ids():
    """the big tournaments' ids (the reviewed list the TV's «البطولات الكبرى» uses): a cup, even in its league phase"""
    try:
        return {t["id"] for t in json.loads(TOURNAMENTS_FILE.read_text(encoding="utf-8"))["tournaments"]}
    except (OSError, ValueError, KeyError, TypeError):
        return set()


def is_cup(lg, c):
    """a league of the tournaments list, or a file with knockout rounds; a league's first 8 matchdays look like a cup's phase but are not"""
    return lg in cup_ids() or bool(c and c.get("rounds"))


ZONE_KINDS = ("acle", "acl2", "ucl", "uel", "uecl", "po", "down")
ZONES_FILE = Path(__file__).resolve().parent / "league_zones.json"


def check_zones(zones, clubs=None):
    """the hand-kept zones list: {league: {season: [{from, to, k}]}}; raises ValueError on a range that is wrong, overlaps another, runs past
    the clubs (when known) or names an unknown kind"""
    if not isinstance(zones, dict):
        raise ValueError("zones: not a list by league")
    for lg, by_season in zones.items():
        for season, zs in (by_season or {}).items():
            taken = set()
            for z in zs:
                a, b, k = z.get("from"), z.get("to"), z.get("k")
                if not (isinstance(a, int) and isinstance(b, int) and 1 <= a <= b):
                    raise ValueError(f"zones {lg} {season}: bad range {z}")
                if k not in ZONE_KINDS:
                    raise ValueError(f"zones {lg} {season}: unknown kind {k}")
                if clubs is not None and b > clubs:
                    raise ValueError(f"zones {lg} {season}: {z} runs past {clubs} clubs")
                if taken & set(range(a, b + 1)):
                    raise ValueError(f"zones {lg} {season}: {z} overlaps")
                taken |= set(range(a, b + 1))
    return None


def load_zones():
    """the reviewed list: every zone carries its source (src, an https address: the competition's regulations or the season's page), added
    only after the owner says yes"""
    z = json.loads(ZONES_FILE.read_text(encoding="utf-8")) if ZONES_FILE.exists() else {}
    check_zones(z)
    for lg, by_season in z.items():
        for season, zs in by_season.items():
            for x in zs:
                if not str(x.get("src") or "").startswith("https://"):
                    raise ValueError(f"zones {lg} {season}: {x} has no source")
    return z


PHASE_MAX = 8   # a cup's league or group phase: TheSportsDB's rounds 1..8 (UCL's and AFC's league phase 8, the old group stages 6)


def _ties(es):
    """a knockout round's games as ties: the same two clubs (one or two legs), the aggregate once every leg has its score"""
    by = {}
    for e in sorted(es, key=lambda e: e["t"]):
        by.setdefault(frozenset((e["h"], e["a"])), []).append(e)
    out = []
    for legs in by.values():
        a, b = legs[0]["h"], legs[0]["a"]
        tie = {"a": a, "b": b, "an": legs[0].get("hn") or "", "bn": legs[0].get("an") or "", "legs": [_game(e) for e in legs]}
        if all(_done(e) for e in legs):
            tie["ag"] = [sum(_num(e["hs"] if e["h"] == x else e["as"]) for e in legs) for x in (a, b)]
        out.append(tie)
    return sorted(out, key=lambda t: t["legs"][0]["t"])


def _groups(phase):
    """the phase's games split by who can meet whom (UCL's one league of 36, AFC's west and east, the old groups of 4)"""
    up = {}
    def root(x):
        while up.setdefault(x, x) != x:
            up[x] = up[up[x]]; x = up[x]
        return x
    for e in phase:
        up[root(e["h"])] = root(e["a"])
    by = {}
    for e in phase:
        by.setdefault(root(e["h"]), []).append(e)
    return list(by.values())


def _last_cluster(phase, gap=dt.timedelta(days=14)):
    """each matchday's own games: TheSportsDB numbers the qualifying rounds 1, 2, 3 too (UCL's July and August), weeks before the matchday of
    that number; a matchday's games are a few days apart, so a round's games are cut where two are more than 14 days apart and the last part kept"""
    by = {}
    for e in phase:
        by.setdefault(e["r"], []).append(e)
    out = []
    for es in by.values():
        es = sorted((e for e in es if _t(e)), key=_t)
        start = 0
        for i in range(1, len(es)):
            if _t(es[i]) - _t(es[i - 1]) > gap:
                start = i
        out += es[start:]
    return out


def cup(games, names=None):
    """{rounds: [{r, ar, games, ties}], phase: {groups, md, of}} for a cup's season (TheSportsDB's round numbers), none when it has neither:
    the knockout rounds in the order they are played, each named (a 16-game «160», or a two-legged «32» of 8 ties, is the play-off), its ties
    with their aggregate; the league or group phase as each group's table, how many matchdays are played and how many there are"""
    names = names or {}
    rs = {str(e.get("r") or "") for e in games}
    if any(r.isdigit() and PHASE_MAX < int(r) < 400 and r not in KO for r in rs):
        return None   # a league's matchdays (9, 10, 11…): its «16» and «32» are matchdays, not knockout rounds
    ko, phase = {}, []
    for e in games:
        if e.get("pp"):
            continue
        r = str(e.get("r") or "")
        if r in KO:
            ko.setdefault(r, []).append(e)
        elif r.isdigit() and 1 <= int(r) <= PHASE_MAX:
            phase.append(e)
    phase = _last_cluster(phase)
    body = {}
    if ko:
        rounds = []
        for r, es in sorted(ko.items(), key=lambda x: min(e["t"] for e in x[1])):
            ties = _ties(es)
            two_legged = ties and all(len(t["legs"]) == 2 for t in ties)
            ar = "الملحق" if (r == "160" and len(ties) > 1) or (r == "32" and len(ties) == 8 and two_legged) else KO[r]
            rounds.append({"r": r, "ar": ar, "games": [_game(e) for e in sorted(es, key=lambda e: e["t"])], "ties": ties})
        body["rounds"] = rounds
    if phase:
        groups = [table(g, names) for g in _groups(phase)]
        played = [int(e["r"]) for e in phase if _done(e)]
        body["phase"] = {"groups": sorted((g for g in groups if g), key=lambda g: -len(g)), "md": max(played) if played else 0,
                         "of": max(int(e["r"]) for e in phase)}
    return body or None


def seasons_of(store, lg):
    """the league's seasons in the store, newest first"""
    return sorted({s for (l, s) in store.leagues if l == lg}, reverse=True)


def champ_of(store, lg, season, now, names):
    """{"s", "id", "n", "ar", and "pts" "gap" (a league) or "score" "legs" (a cup), "vs" "vn" "var"} when the archive is sure, else None"""
    c = honours_study.champion(lg, list(store.leagues[(lg, season)].values()), now)
    if c.get("state") != "champion":
        return None
    out = {"s": season, "id": c["id"], "n": c.get("name") or "", "ar": names.get(c["id"], {}).get("ar", ""), "vs": c.get("vs") or "",
           "vn": c.get("vn") or "", "var": names.get(c.get("vs") or "", {}).get("ar", "")}
    for k in ("pts", "gap", "score", "legs"):
        if k in c:
            out[k] = c[k]
    return out


def season_file(store, lg, season, now, stamp, names, ar=""):
    """a past season: its final table (a cup: its rounds), every result newest first, its champion when the archive is sure"""
    games = list(store.leagues[(lg, season)].values())
    done = sorted((e for e in games if _done(e) and not e.get("pp")), key=lambda e: e["t"], reverse=True)
    body = {"v": V, "id": lg, "en": store.name(lg), "ar": ar, "season": season, "updated": stamp, "table": table(games, names),
            "results": [_game(e) for e in done]}
    c = cup(games, names)
    if c:
        body["cup"] = c
    ch = champ_of(store, lg, season, now, names)
    if ch:
        body["champ"] = ch
    return body


def league_file(store, lg, now, stamp, names, ar="", zones=None):
    season = season_now(store, lg, now)
    if not season:
        return None
    games = list(store.leagues[(lg, season)].values())
    lo, hi = now - dt.timedelta(days=LAST_DAYS), now + dt.timedelta(days=NEXT_DAYS)
    last = sorted((e for e in games if _done(e) and lo <= (_t(e) or lo - dt.timedelta(1)) <= now), key=lambda e: e["t"], reverse=True)
    nxt = sorted((e for e in games if not _done(e) and not e.get("pp") and now - dt.timedelta(hours=3) <= (_t(e) or lo) <= hi), key=lambda e: e["t"])
    rows = table(games, names)
    c = cup(games, names)
    mv = movement_for(games, names, is_cup(lg, c))
    for r in rows:
        if r["id"] in mv:
            r["mv"] = mv[r["id"]]
    body = {"v": V, "id": lg, "en": store.name(lg), "ar": ar, "season": season, "updated": stamp,
            "table": rows, "last": [_game(e) for e in last], "next": [_game(e) for e in nxt]}
    zs = ((load_zones() if zones is None else zones).get(lg) or {}).get(season)
    if zs and max(z["to"] for z in zs) <= len(rows):   # a table still short of the league's clubs (a season's first days, a club with no game yet): no zones,
        body["zones"] = zs                               # never a build stopped for every league (the list itself is checked when it is read)
    if c:
        body["cup"] = c
    body["seasons"] = seasons_of(store, lg)
    body["champs"] = [ch for ch in (champ_of(store, lg, x, now, names) for x in body["seasons"]) if ch]
    return body


def derive(store, out, prev, now, stamp, names, league_ar, emit):
    """writes out/sports/<lg>.json for every league of the store, and 4.5 out/sports/<lg>/<season>.json for its past seasons; returns how many
    leagues"""
    shutil.rmtree(out / "sports", ignore_errors=True)
    (out / "sports").mkdir(parents=True)
    n = 0
    for lg in sorted({l for (l, _) in store.leagues}):
        body = league_file(store, lg, now, stamp, names, league_ar.get(lg, ""))
        if body:
            emit(out, prev, f"sports/{lg}.json", body)
            n += 1
            for season in body["seasons"]:
                if season != body["season"]:
                    emit(out, prev, f"sports/{lg}/{season}.json", season_file(store, lg, season, now, stamp, names, league_ar.get(lg, "")))
    return n


def f1_file(answer, stamp):
    """Jolpica's answer (MRData.RaceTable.Races[0]) as the card reads it, None when it has no race"""
    races = (((answer or {}).get("MRData") or {}).get("RaceTable") or {}).get("Races") or []
    if not races:
        return None
    r = races[0]
    def at(x):
        return f"{x['date']}T{(x.get('time') or '12:00:00Z').rstrip('Z')[:8]}Z" if x and x.get("date") else ""
    loc = (r.get("Circuit") or {}).get("Location") or {}
    country = loc.get("country") or ""
    sessions = [{"k": k, "t": at(r.get(src))} for src, k in SESSIONS if r.get(src)] + [{"k": "race", "t": at(r)}]
    seen, uniq = set(), []
    for s in sessions:
        if s["t"] and s["k"] not in seen:
            seen.add(s["k"]); uniq.append(s)
    return {"v": V, "updated": stamp, "season": r.get("season", ""), "round": r.get("round", ""), "name": r.get("raceName", ""),
            "ar": "جائزة " + (F1_RACE.get(r.get("raceName", "")) or F1_COUNTRY[country]) + " الكبرى" if r.get("raceName", "") in F1_RACE or country in F1_COUNTRY else "", "circuit": (r.get("Circuit") or {}).get("circuitName", ""),
            "country": country, "sessions": sorted(uniq, key=lambda s: s["t"])}


def _f1_get(fetch, url):
    raw = fetch(url) if fetch else urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "UrTV CI"}), timeout=20).read()
    return json.loads(raw)


def f1_next(fetch, stamp):
    """the next race; once a season's last race is run (Jolpica's «next» has no race) the next season's first, None when neither is known yet"""
    ans = _f1_get(fetch, F1_URL)
    body = f1_file(ans, stamp)
    if body is None:
        season = str((((ans or {}).get("MRData") or {}).get("RaceTable") or {}).get("season") or "")
        if season.isdigit():
            body = f1_file(_f1_get(fetch, F1_FIRST.format(season=int(season) + 1)), stamp)
    return body


def f1_check(fetch=None):
    """CI's proof that the real Jolpica answers and parses (tests/f1-check workflow): prints the file it would write, writes nothing"""
    now = dt.datetime.now(dt.timezone.utc).replace(microsecond=0)
    body = f1_next(fetch, now.isoformat().replace("+00:00", "Z"))
    if not body or not body.get("sessions") or not all(x.get("t") for x in body["sessions"]) or not any(x["k"] == "race" for x in body["sessions"]):
        print("f1 check: no race in Jolpica's answer:", json.dumps(body, ensure_ascii=False))
        return 1
    print("f1 check: ok", json.dumps(body, ensure_ascii=False))
    return 0


def f1(args, fetch=None):
    """f1/next.json: asked at most once in F1_EVERY; a failed or empty answer keeps the last file"""
    prev, out = Path(args.prev), Path(args.out)
    now = dt.datetime.now(dt.timezone.utc).replace(microsecond=0)
    old = None
    try:
        old = json.loads((prev / "f1/next.json").read_text(encoding="utf-8"))
    except (OSError, ValueError):
        pass
    (out / "f1").mkdir(parents=True, exist_ok=True)
    try:
        fresh = old and now - dt.datetime.fromisoformat(old["asked"].replace("Z", "+00:00")) < F1_EVERY
    except (KeyError, ValueError, TypeError):
        fresh = False
    body = None
    if not fresh:
        try:
            body = f1_next(fetch, now.isoformat().replace("+00:00", "Z"))
        except Exception as e:   # the network or the answer: the last file stays
            print("f1: no answer:", e)
    if body:
        body["asked"] = now.isoformat().replace("+00:00", "Z")
        if old and {k: v for k, v in old.items() if k not in ("updated", "asked")} == {k: v for k, v in body.items() if k not in ("updated", "asked")}:
            body["updated"] = old.get("updated", body["updated"])
    elif old:
        body = old
        if not fresh:
            body = dict(old, asked=now.isoformat().replace("+00:00", "Z"))   # one ask a day, answered or not
    if body:
        (out / "f1/next.json").write_text(json.dumps(body, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print("f1:", "kept" if fresh else "asked", (body or {}).get("name", "none"))
    return 0


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    p = sub.add_parser("f1")
    p.add_argument("--prev", required=True)
    p.add_argument("--out", required=True)
    sub.add_parser("f1check")
    a = ap.parse_args()
    return f1_check() if a.cmd == "f1check" else f1(a)


if __name__ == "__main__":
    raise SystemExit(main())
