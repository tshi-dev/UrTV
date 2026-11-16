#!/usr/bin/env python3
"""4.4 «الرياضة»: the small files the sports cards read, beside the match archive (docs/MATCH-ARCHIVE.md). No API call but f1.

  sports/<leagueId>.json  one file per league, for every card of that league: its table computed from the store's current season (3 points
                          a win, 1 a draw; goal difference, goals for, name), each club's last 5 (newest first, "w" "d" "l"), the finished
                          games of the last 2 days, the games of the next 7 days, and for a cup the knockout games by round.
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

V = 1
LAST_DAYS, NEXT_DAYS, FORM = 2, 7, 5
FINAL = {"FT", "AET", "PEN", "MATCH FINISHED", "AOT", "AP"}
# TheSportsDB's round numbers of a knockout stage (a league's rounds are 1..38): their Arabic names
KO = {"32": "دور الـ٣٢", "16": "دور الـ١٦", "125": "ربع النهائي", "150": "نصف النهائي", "160": "المركز الثالث", "200": "النهائي",
      "170": "الملحق", "180": "نهائي الملحق"}
KO_ORDER = ["32", "16", "125", "150", "160", "200"]
F1_URL = "https://api.jolpi.ca/ergast/f1/current/next.json"
F1_EVERY = dt.timedelta(hours=20)
F1_COUNTRY = {"Bahrain": "البحرين", "Saudi Arabia": "السعودية", "Australia": "أستراليا", "Japan": "اليابان", "China": "الصين", "USA": "الولايات المتحدة",
              "United States": "الولايات المتحدة", "Italy": "إيطاليا", "Monaco": "موناكو", "Spain": "إسبانيا", "Canada": "كندا", "Austria": "النمسا",
              "UK": "بريطانيا", "United Kingdom": "بريطانيا", "Hungary": "المجر", "Belgium": "بلجيكا", "Netherlands": "هولندا", "Azerbaijan": "أذربيجان",
              "Singapore": "سنغافورة", "Mexico": "المكسيك", "Brazil": "البرازيل", "Qatar": "قطر", "UAE": "أبوظبي", "United Arab Emirates": "أبوظبي",
              "Las Vegas": "لاس فيغاس", "Miami": "ميامي"}
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


def cup(games):
    """{rounds: [{r, ar, games}]} for the knockout games of a season (TheSportsDB's round numbers), none when it has no knockout round"""
    by = {}
    for e in games:
        r = str(e.get("r") or "")
        if r in KO and not e.get("pp"):
            by.setdefault(r, []).append(_game(e))
    if not by:
        return None
    order = [r for r in KO_ORDER if r in by] + sorted(r for r in by if r not in KO_ORDER)
    return {"rounds": [{"r": r, "ar": KO[r], "games": sorted(by[r], key=lambda g: g["t"])} for r in order]}


def league_file(store, lg, now, stamp, names, ar=""):
    season = season_now(store, lg, now)
    if not season:
        return None
    games = list(store.leagues[(lg, season)].values())
    lo, hi = now - dt.timedelta(days=LAST_DAYS), now + dt.timedelta(days=NEXT_DAYS)
    last = sorted((e for e in games if _done(e) and lo <= (_t(e) or lo - dt.timedelta(1)) <= now), key=lambda e: e["t"], reverse=True)
    nxt = sorted((e for e in games if not _done(e) and not e.get("pp") and now - dt.timedelta(hours=3) <= (_t(e) or lo) <= hi), key=lambda e: e["t"])
    body = {"v": V, "id": lg, "en": store.name(lg), "ar": ar, "season": season, "updated": stamp,
            "table": table(games, names), "last": [_game(e) for e in last], "next": [_game(e) for e in nxt]}
    c = cup(games)
    if c:
        body["cup"] = c
    return body


def derive(store, out, prev, now, stamp, names, league_ar, emit):
    """writes out/sports/<lg>.json for every league of the store; returns how many"""
    shutil.rmtree(out / "sports", ignore_errors=True)
    (out / "sports").mkdir(parents=True)
    n = 0
    for lg in sorted({l for (l, _) in store.leagues}):
        body = league_file(store, lg, now, stamp, names, league_ar.get(lg, ""))
        if body:
            emit(out, prev, f"sports/{lg}.json", body)
            n += 1
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
            "ar": "جائزة " + F1_COUNTRY[country] + " الكبرى" if country in F1_COUNTRY else "", "circuit": (r.get("Circuit") or {}).get("circuitName", ""),
            "country": country, "sessions": sorted(uniq, key=lambda s: s["t"])}


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
            raw = fetch() if fetch else urllib.request.urlopen(urllib.request.Request(F1_URL, headers={"User-Agent": "UrTV CI"}), timeout=20).read()
            body = f1_file(json.loads(raw), now.isoformat().replace("+00:00", "Z"))
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
    a = ap.parse_args()
    return f1(a)


if __name__ == "__main__":
    raise SystemExit(main())
