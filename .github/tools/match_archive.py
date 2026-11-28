#!/usr/bin/env python3
"""The match archive (4.3.1): every game of the leagues and tournaments we fetch, kept on the data-archive branch, and small files a TV
downloads only when a page that needs them opens (a team's page, a head-to-head). Pure file work from what league_fixtures.py already wrote
(data-leagues: one file per league, tournaments.json); the live job and the backfill are the only parts that ask the network.

  build     merge data-leagues (and a live patch) into the store; write the derived files and health.json. Seconds, no API call.
  live      the games of the store that are in play or ended in the last 30 minutes: their newest copy from TheSportsDB (lookupevent.php,
            one request each, at most LIVE_MAX, 2.5 s apart, a 429 waited out); writes a patch for build. Exits at once when none is live.
  backfill  past seasons from openfootball/football.json (public domain, CC0) for the leagues it has, names mapped to TheSportsDB ids by
            teams.json and archive_names.json (edit that file by hand for a name that is not found; the report lists them).

The store (data-archive/store/<league id>/<season>.json): {"league", "id", "season", "updated", "events": {key: event}}, append-only and
idempotent: the key is TheSportsDB's idEvent, else "d:<date>:<home id>:<away id>" (a backfilled game; dropped when TheSportsDB's copy of the
same game arrives). A newer copy replaces an older one unless it is less far on (finished > under way > not started), so a corrected score lands.
Derived: teams/<id>.json (the last 10 results, the next 5), h2h/<low id>-<high id>.json (every meeting known, newest first, at most 100),
index.json (team id -> names and leagues; league id -> names), health.json (the run, the counts, the problems).

    python3 .github/tools/match_archive.py build --leagues DIR --prev DIR --out DIR [--live FILE]
    python3 .github/tools/match_archive.py live --prev DIR --out FILE
    python3 .github/tools/match_archive.py backfill --prev DIR --out DIR [--seasons 2021-22,...] [--comps en.1,...]
"""
import argparse
import datetime as dt
import json
import os
import re
import shutil
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import sports_files   # noqa: E402  (4.4: sports/<league>.json, f1/next.json)

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
TEAMS = ROOT / "assets/shared/teams.json"
NAMES = HERE / "archive_names.json"   # openfootball name -> TheSportsDB id, by hand (the report lists the names not found)
FINAL = {"FT", "AET", "PEN", "MATCH FINISHED", "AOT", "AP"}
DUR_H = {"Soccer": 2.5, "Basketball": 3.0, "American Football": 3.75}
LAST, NEXT, H2H_MAX, RESULTS_MAX = 10, 5, 100, 30
V = 1   # the files' format; new fields may appear without a change here, a field changing meaning or going away raises it
LIVE_MAX, LIVE_GAP = 40, 2.5
KEEP_SEASONS = 8         # a league keeps its last 8 seasons in the store
SIZE_CAP = 150_000_000   # over this, health.json says so (a problem, not a red run)
LEAGUE_AR = {"4668": "دوري روشن السعودي", "5627": "دوري يلو", "4328": "الدوري الإنجليزي", "4335": "الدوري الإسباني", "4332": "الدوري الإيطالي",
             "4331": "الدوري الألماني", "4334": "الدوري الفرنسي", "4344": "الدوري البرتغالي", "4337": "الدوري الهولندي", "4339": "الدوري التركي",
             "4829": "الدوري المصري", "4678": "الدوري الإماراتي", "4663": "الدوري القطري", "5056": "دوري نجوم العراق",
             "4719": "دوري أبطال آسيا للنخبة", "4480": "دوري أبطال أوروبا", "4346": "الدوري الأمريكي", "4387": "NBA", "4391": "NFL"}
# openfootball's competitions -> TheSportsDB league ids
OPENFOOTBALL = {"en.1": "4328", "es.1": "4335", "de.1": "4331", "it.1": "4332", "fr.1": "4334", "pt.1": "4344", "nl.1": "4337", "tr.1": "4339",
                "uefa.cl": "4480", "mls": "4346"}
OF_CALENDAR = {"mls"}   # a season is a year (2025/mls.json), not 2024-25
OF_BASE = "https://raw.githubusercontent.com/openfootball/football.json/master/"


def now_utc():
    return dt.datetime.now(dt.timezone.utc)


def iso(t):
    return t.strftime("%Y-%m-%dT%H:%M:%SZ")


def kickoff(e):
    s = e.get("strTimestamp") or ((e.get("dateEvent") or "") + "T" + (e.get("strTime") or "00:00:00"))
    s = s.replace("Z", "").split("+")[0]
    try:
        return dt.datetime.fromisoformat(s[:19]).replace(tzinfo=dt.timezone.utc)
    except ValueError:
        return None


def scored(e):
    return e.get("hs") not in (None, "") and e.get("as") not in (None, "")


def stage(e):
    """3 finished with a score, 2 under way with a score, 1 anything else"""
    if not scored(e):
        return 1
    return 3 if (e.get("st") or "").upper() in FINAL else 2


def compact(e, sport=None):
    """A TheSportsDB event as the store keeps it."""
    t = kickoff(e)
    return {"id": str(e.get("idEvent") or ""), "lg": str(e.get("idLeague") or ""), "s": e.get("strSeason") or "",
            "t": iso(t) if t else "", "h": str(e.get("idHomeTeam") or ""), "a": str(e.get("idAwayTeam") or ""),
            "hn": e.get("strHomeTeam") or "", "an": e.get("strAwayTeam") or "",
            "hs": e.get("intHomeScore") if e.get("intHomeScore") not in ("",) else None,
            "as": e.get("intAwayScore") if e.get("intAwayScore") not in ("",) else None,
            "st": e.get("strStatus") or "", "r": e.get("intRound") or "", "sp": e.get("strSport") or sport or "Soccer",
            "pp": (e.get("strPostponed") or "").lower() == "yes", "src": "tsdb"}


def key_of(e):
    return e["id"] if e.get("id") else f"d:{e['t'][:10]}:{e['h']}:{e['a']}"


def date_key(e):
    return f"d:{e['t'][:10]}:{e['h']}:{e['a']}"


def season_of(e):
    s = e.get("s") or ""
    if s:
        return s
    t = e.get("t") or ""
    return t[:4] if t else "unknown"


class Store:
    def __init__(self):
        self.leagues = {}   # (lg, season) -> {key: event}
        self.meta = {}      # lg -> {"en": name}
        self.merged = 0     # 4.4 (#70, review F90) copies put() removed from another season of the same game: counted as dropped by the guard

    def load(self, root):
        n = 0
        for f in sorted((root / "store").glob("*/*.json")) if (root / "store").is_dir() else []:
            d = json.loads(f.read_text(encoding="utf-8"))   # a broken file is a real breakage: the run fails (red), nothing is pushed
            self.leagues[(d["id"], d["season"])] = d["events"]
            self.meta.setdefault(d["id"], {"en": d.get("league", "")})
            n += len(d["events"])
        return n

    def put(self, e, league_name=""):
        """Merge one event; returns "new", "updated" or "".

        A TheSportsDB copy of a game the backfill had (same date and teams) replaces it."""
        if not e.get("h") or not e.get("a") or not e.get("t"):
            return ""
        lg, season = e["lg"] or "0", season_of(e)
        bucket = self.leagues.setdefault((lg, season), {})
        if league_name and not self.meta.get(lg, {}).get("en"):
            self.meta[lg] = {"en": league_name}
        k = key_of(e)
        if e.get("id"):
            dk = date_key(e)
            if dk in bucket and bucket[dk].get("src") != "tsdb":
                del bucket[dk]
        elif any(x.get("src") == "tsdb" and date_key(x) == k for x in bucket.values()):
            return ""   # a backfilled copy of a game TheSportsDB already gave
        if e.get("id"):   # 4.4 (#70) one game, one copy: TheSportsDB can move a game to another season label (MLS 2026 and 2026-2027); the copy
            for (l2, s2), b2 in self.leagues.items():   # with a result is never replaced by a stale one, and the one kept is the newest report
                if l2 == lg and s2 != season and k in b2:
                    if stage(e) < stage(b2[k]):
                        return ""
                    del b2[k]
                    if k in bucket:   # a duplicate is removed (the store really shrinks by one): counted, so the «store shrank» guard does not mistake it for a loss; a game that only MOVED between season labels is not (review F132)
                        self.merged += 1
        old = bucket.get(k)
        if old is None:
            bucket[k] = e
            return "new"
        if stage(e) < stage(old):
            return ""   # never a step back (a finished score is not undone by an older «not started» copy)
        if {k2: v for k2, v in e.items() if k2 != "src"} == {k2: v for k2, v in old.items() if k2 != "src"}:
            return ""
        bucket[k] = e
        return "updated"

    def dedupe(self):
        """4.4 (#70) a game stored under two seasons of its league (the same id): one copy kept, the one with a result (else the one in the season
        with the most games, the league's own); returns how many copies went"""
        dropped = 0
        by_lg = {}
        for (lg, season), b in self.leagues.items():
            by_lg.setdefault(lg, []).append((season, b))
        for lg, buckets in by_lg.items():
            seen = {}
            for season, b in sorted(buckets, key=lambda x: -len(x[1])):
                for k in [k for k in b if not k.startswith("d:")]:
                    if k not in seen:
                        seen[k] = b
                        continue
                    keep = seen[k]
                    if stage(b[k]) > stage(keep[k]):
                        del keep[k]
                        seen[k] = b
                    else:
                        del b[k]
                    dropped += 1
        for key in [key for key, b in self.leagues.items() if not b]:
            del self.leagues[key]
        return dropped

    def total(self):
        return sum(len(v) for v in self.leagues.values())

    def prune(self, now):
        """Drop the seasons past KEEP_SEASONS per league; nothing else is ever removed from the store."""
        dropped = 0
        by_lg = {}
        for (lg, season) in self.leagues:
            by_lg.setdefault(lg, []).append(season)
        for lg, seasons in by_lg.items():
            for s in sorted(seasons)[:-KEEP_SEASONS]:
                dropped += len(self.leagues.pop((lg, s)))
        return dropped

    def events(self):
        for v in self.leagues.values():
            yield from v.values()

    def name(self, lg):
        return self.meta.get(lg, {}).get("en") or LEAGUE_EN.get(lg, "")

    def write(self, out, prev, stamp):
        for (lg, season), evs in self.leagues.items():
            body = {"v": V, "league": self.name(lg), "id": lg, "season": season, "updated": stamp,
                    "events": dict(sorted(evs.items(), key=lambda kv: (kv[1]["t"], kv[0])))}
            emit(out, prev, f"store/{lg}/{season}.json", body)


def emit(out, prev, rel, body):
    """Writes out/rel. "updated" is when its content last changed: the previous file's stamp is kept when nothing else in it differs, so a
    file a run did not change is byte for byte the same (and a client's cached copy stays good)."""
    try:
        old = json.loads((prev / rel).read_text(encoding="utf-8"))
        if {k: v for k, v in old.items() if k != "updated"} == {k: v for k, v in body.items() if k != "updated"}:
            body = dict(body, updated=old.get("updated", body["updated"]))
    except (OSError, ValueError):
        pass
    p = out / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(body, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")


def teams_table():
    try:
        rows = json.loads(TEAMS.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}
    return {str(r["id"]): r for r in rows}


def game(e):
    """A finished game as h2h and results give it."""
    return {"id": e["id"] or key_of(e), "lg": e["lg"], "s": e["s"], "t": e["t"], "r": e["r"], "h": e["h"], "a": e["a"], "hn": e["hn"],
            "an": e["an"], "hs": e["hs"], "as": e["as"], "st": e["st"]}


def brief(e, team):
    """An event as a team's page reads it: its side, the other team, the score from its side."""
    home = e["h"] == team
    return {"id": e["id"] or key_of(e), "lg": e["lg"], "s": e["s"], "t": e["t"], "home": home, "vs": e["a"] if home else e["h"],
            "vsn": e["an"] if home else e["hn"], "gf": (e["hs"] if home else e["as"]), "ga": (e["as"] if home else e["hs"]),
            "st": e["st"], "r": e["r"]}


def derive(store, out, prev, now, stamp):
    table = teams_table()
    per_team, pairs, names = {}, {}, {}
    for e in store.events():
        for side, name in ((e["h"], e["hn"]), (e["a"], e["an"])):
            per_team.setdefault(side, []).append(e)
            if name:
                names.setdefault(side, {"en": name, "leagues": set()})
            if side in names:
                names[side]["leagues"].add(e["lg"])
        if stage(e) == 3:
            lo, hi = sorted((e["h"], e["a"]), key=lambda x: (len(x), x))
            pairs.setdefault(f"{lo}-{hi}", []).append(e)
    for d in ("teams", "h2h", "results"):
        shutil.rmtree(out / d, ignore_errors=True)
        (out / d).mkdir(parents=True)
    for team, evs in per_team.items():
        done = sorted((e for e in evs if stage(e) == 3), key=lambda e: e["t"], reverse=True)[:LAST]
        ahead = sorted((e for e in evs if stage(e) == 1 and not e.get("pp") and (kickoff_t(e) or now) >= now - dt.timedelta(hours=3)),
                       key=lambda e: e["t"])[:NEXT]
        row = table.get(team, {})
        body = {"v": V, "id": team, "en": row.get("en") or names.get(team, {}).get("en", ""), "ar": row.get("ar", ""), "updated": stamp,
                "last": [brief(e, team) for e in done], "next": [brief(e, team) for e in ahead]}
        emit(out, prev, f"teams/{team}.json", body)
    for pair, evs in pairs.items():
        evs = sorted(evs, key=lambda e: e["t"], reverse=True)[:H2H_MAX]
        body = {"v": V, "pair": pair, "updated": stamp, "meetings": [game(e) for e in evs]}
        emit(out, prev, f"h2h/{pair}.json", body)
    by_lg = {}
    for e in store.events():
        if stage(e) == 3:
            by_lg.setdefault(e["lg"], []).append(e)
    for lg, evs in by_lg.items():
        evs = sorted(evs, key=lambda e: (e["t"], key_of(e)), reverse=True)[:RESULTS_MAX]
        body = {"v": V, "id": lg, "en": store.name(lg), "ar": LEAGUE_AR.get(lg, ""), "updated": stamp,
                "results": [game(e) for e in evs]}
        emit(out, prev, f"results/{lg}.json", body)
    index = {"v": V, "updated": stamp,
             "teams": {t: {"en": table.get(t, {}).get("en") or n["en"], "ar": table.get(t, {}).get("ar", ""), "leagues": sorted(n["leagues"])}
                       for t, n in sorted(names.items())},
             "leagues": {lg: {"en": store.name(lg), "ar": LEAGUE_AR.get(lg, "")} for lg in sorted(store.meta)}}
    emit(out, prev, "index.json", index)
    sports_files.derive(store, out, prev, now, stamp, index["teams"], LEAGUE_AR, emit)   # 4.4 the sports cards' one file per league
    return len(per_team), len(pairs)


def kickoff_t(e):
    try:
        return dt.datetime.fromisoformat(e["t"].replace("Z", "+00:00"))
    except (KeyError, ValueError):
        return None


def read_league_files(leagues):
    """(events, league names) from data-leagues: every leagues/<slug>.json and tournaments.json"""
    out, names = [], {}
    for f in sorted(leagues.glob("*.json")) if leagues.is_dir() else []:
        if f.name.endswith("report.json"):
            continue
        d = json.loads(f.read_text(encoding="utf-8"))
        for e in d.get("events") or []:
            c = compact(e)
            if not c["lg"] and d.get("id"):
                c["lg"] = str(d["id"])
            out.append(c)
            if c["lg"] and e.get("strLeague"):
                names.setdefault(c["lg"], e["strLeague"])
        if d.get("id") and d.get("league"):
            names[str(d["id"])] = d["league"]
        if isinstance(d.get("names"), dict):
            for k, v in d["names"].items():
                LEAGUE_AR.setdefault(str(k), v)
    return out, names


STALE_DAYS = 10   # a followed league in its season with no new result this long is a problem (health.json, the data-health issue)


def league_health(store, now, state):
    """Each followed league (HISTORY): its newest result and its games of the last 10 days; and the problems: a league in its season whose
    games of the last 10 days have no result (or that has no game known in 10 days between games before and after), a past season short of
    its sure expected count."""
    summary, problems = {}, []
    lo, soon = now - dt.timedelta(days=STALE_DAYS), now - dt.timedelta(hours=4)
    by = {}
    for e in store.events():
        by.setdefault(e["lg"], []).append(e)
    for lg, name, _ in HISTORY:
        evs = by.get(lg, [])
        times = [(kickoff_t(e), e) for e in evs if kickoff_t(e)]
        done = [t for t, e in times if stage(e) == 3]
        recent = [e for t, e in times if lo <= t <= soon]
        recent_done = [e for e in recent if stage(e) == 3]
        row = {"name": name, "last_result": iso(max(done)) if done else "", "games_10d": len(recent), "results_10d": len(recent_done)}
        summary[lg] = row
        played = [e for t, e in times if lo <= t <= now - dt.timedelta(days=2) and stage(e) != 3 and not e.get("pp")]
        row["without_result"] = len(played)
        if played:   # games played 2 to 10 days ago that still have no result (a break between rounds is no problem)
            problems.append(f"{name} ({lg}): {len(played)} games played 2 to {STALE_DAYS} days ago have no result")
        for season, x in sorted(state.get(lg, {}).items()):
            if not x.get("current") and x.get("expected_sure") and x.get("expected") and x.get("finished", 0) < x["expected"]:
                problems.append(f"{name} ({lg}) {season}: {x.get('finished', 0)} results of {x['expected']} expected")
    return summary, problems


def build(args):
    t0 = time.time()
    now = now_utc()
    stamp = iso(now)
    prev, out = Path(args.prev), Path(args.out)
    store = Store()
    before = store.load(prev)
    old_health = {}
    if (prev / "health.json").is_file():
        old_health = json.loads((prev / "health.json").read_text(encoding="utf-8"))
    problems = []
    if old_health.get("events", 0) > 0 and before == 0:
        raise SystemExit("the previous archive said %d events but none could be read: refusing to start the store again" % old_health["events"])
    evs, names = read_league_files(Path(args.leagues))
    counts = {"new": 0, "updated": 0}
    for e in evs:
        r = store.put(e, names.get(e["lg"], ""))
        if r:
            counts[r] += 1
    if args.live and Path(args.live).is_file():
        for e in json.loads(Path(args.live).read_text(encoding="utf-8")).get("events") or []:
            r = store.put(compact(e))
            if r:
                counts[r] += 1
    if getattr(args, "history", None) and Path(args.history).is_file():
        for e in json.loads(Path(args.history).read_text(encoding="utf-8")).get("events") or []:
            r = store.put(compact(e), e.get("strLeague") or "")
            if r:
                counts[r] += 1
    if args.backfill and Path(args.backfill).is_file():
        for e in json.loads(Path(args.backfill).read_text(encoding="utf-8")).get("events") or []:
            r = store.put(e)
            if r:
                counts[r] += 1
    dropped = store.prune(now) + store.dedupe() + store.merged   # 4.4 (#70, F90) every copy removed, by put() or dedupe(), counts as dropped
    after = store.total()
    if after < before - dropped:   # the store only grows (a season past KEEP_SEASONS aside): anything else is a bug, and nothing is pushed
        raise SystemExit(f"the store shrank from {before} to {after} events: not pushed")
    if not evs:
        problems.append("no events read from data-leagues")
    if out.exists() and out != prev:
        shutil.rmtree(out)
    out.mkdir(parents=True, exist_ok=True)
    store.write(out, prev, stamp)
    teams, pairs = derive(store, out, prev, now, stamp)
    if (prev / "f1").is_dir() and prev != out:   # the F1 file is asked once a day (sports_files.py f1): every other run carries it over
        shutil.copytree(prev / "f1", out / "f1", dirs_exist_ok=True)
    size = sum(f.stat().st_size for f in out.rglob("*.json"))
    health = {"updated": stamp, "events": after, "new": counts["new"], "updated_events": counts["updated"], "pruned": dropped,
              "leagues": len({lg for lg, _ in store.leagues}), "seasons": len(store.leagues), "teams": teams, "pairs": pairs,
              "bytes": size, "seconds": round(time.time() - t0, 2), "problems": problems,
              "last_live": old_health.get("last_live", ""), "live_requests": old_health.get("live_requests", 0)}
    if args.live_note and Path(args.live_note).is_file():
        health.update(json.loads(Path(args.live_note).read_text(encoding="utf-8")))
    names = Path(args.names) if args.names else prev / "names.json"   # what the backfill found, kept for the next one
    if names.is_file():
        shutil.copyfile(names, out / "names.json")
    for name, given in (("history.json", getattr(args, "history_state", None)), ("history-report.json", getattr(args, "history_report", None))):
        src = Path(given) if given else prev / name
        if src.is_file():
            shutil.copyfile(src, out / name)
    rep = prev / "backfill.json"
    if args.backfill_report and Path(args.backfill_report).is_file():
        rep = Path(args.backfill_report)
    if rep.is_file():
        shutil.copyfile(rep, out / "backfill.json")
        r = json.loads(rep.read_text(encoding="utf-8"))
        health["backfill"] = {"games": r.get("games", 0), "names_not_found": len(r.get("names_not_found") or {})}
    hist = out / "history.json"
    summary, issues = league_health(store, now, json.loads(hist.read_text(encoding="utf-8")) if hist.is_file() else {})
    health["followed"] = summary
    problems += issues
    if size > SIZE_CAP:
        problems.append(f"the archive is {size // 1_000_000} MB, over {SIZE_CAP // 1_000_000} MB: lower KEEP_SEASONS")
    (out / "health.json").write_text(json.dumps(health, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(json.dumps(health, ensure_ascii=False))
    return 0


def live(args):
    """The games in play or ended in the last 30 minutes, their newest copy; nothing asked when none is."""
    now = now_utc()
    store = Store()
    store.load(Path(args.prev))
    want = []
    for e in store.events():
        t = kickoff_t(e)
        if not t or not e.get("id") or stage(e) == 3 or e.get("pp"):
            continue
        end = t + dt.timedelta(hours=DUR_H.get(e.get("sp") or "Soccer", 2.5))
        if t - dt.timedelta(minutes=5) <= now <= end + dt.timedelta(minutes=30):
            want.append(e["id"])
    note = {"last_live": iso(now), "live_requests": 0, "live_games": len(want)}
    Path(args.note).write_text(json.dumps(note) + "\n", encoding="utf-8")
    if not want:
        print("nothing live")
        return 0
    sys.path.insert(0, str(HERE))
    import team_ids as ti   # the same fetch as the other jobs: a 429 waited out, the free key
    got = []
    for i in want[:LIVE_MAX]:
        try:
            d = ti.fetch(ti.API + f"lookupevent.php?id={i}", pause=LIVE_GAP) or {}
        except Exception:   # noqa: BLE001 - one game not read this time: the next run asks again
            continue
        got += [e for e in (d.get("events") or []) if str(e.get("idEvent")) == i]
    note["live_requests"] = ti.STATS.get("thesportsdb", 0)
    Path(args.note).write_text(json.dumps(note) + "\n", encoding="utf-8")
    Path(args.out).write_text(json.dumps({"events": got}, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{len(want)} live, {len(got)} fetched, {note['live_requests']} requests")
    return 0


OF_ZONE = {"en.1": "Europe/London", "pt.1": "Europe/Lisbon", "tr.1": "Europe/Istanbul", "mls": "America/New_York"}   # openfootball's times are local; the rest CET
LEAGUE_EN = {"4328": "English Premier League", "4335": "Spanish La Liga", "4331": "German Bundesliga", "4332": "Italian Serie A",
             "4334": "French Ligue 1", "4344": "Portuguese Primeira Liga", "4337": "Dutch Eredivisie", "4339": "Turkish Super Lig",
             "4480": "UEFA Champions League"}


def clean_name(name):
    """"FC Bayern München (GER)" -> "FC Bayern München"; "Bologna FC 1909", "FC Twente '65" lose the year"""
    s = re.sub(r"\s*\([A-Z]{3}\)\s*$", "", name or "")
    return re.sub(r"\s+'?\d{2,4}\b", "", s).strip()


def norm(s):
    s = unicodedata.normalize("NFKD", (s or "").replace("ı", "i").replace("ß", "ss")).encode("ascii", "ignore").decode().lower()
    s = re.sub(r"\b(fc|cf|afc|sc|ac|as|ss|ssc|us|aj|ogc|cd|ud|sd|rc|rcd|ca|bc|cfc|hsc|sco|club|de|del|da|calcio|football|futbol|vfb|vfl|tsg|sv|fk|sk|bk|kv|sbv|and)\b", " ", s)
    return re.sub(r"[^a-z0-9]+", "", s)


COMMON_WORDS = {"town", "county", "rovers", "wanderers", "athletic", "albion", "olympique", "racing", "stade", "red", "bull", "bulls", "union",
                "borussia", "dynamo", "dinamo", "olympiacos", "rapid", "young", "boys", "hotspur", "palace", "villa", "forest", "clube", "portugal"}


class Names:
    """openfootball name -> TheSportsDB id: the hand file first, then what an earlier backfill found (data-archive/names.json), then teams.json
    (the same name, else one club of that league sharing a distinctive word), then one TheSportsDB search (cached, a miss too)."""

    def __init__(self, cache_file, online=True):
        self.online = online
        self.asked = set()   # the names looked up in this run (a miss is not searched twice)
        sys.path.insert(0, str(HERE))
        import team_ids as ti
        self.ti = ti
        self.table = teams_table()
        self.hand = json.loads(NAMES.read_text(encoding="utf-8")) if NAMES.is_file() else {}
        self.cache_file = cache_file
        self.cache = json.loads(cache_file.read_text(encoding="utf-8")) if cache_file and cache_file.is_file() else {}
        self.by_norm = {}
        for t in self.table.values():
            for n in [t.get("en", "")] + list(t.get("alt") or []):
                self.by_norm.setdefault(norm(n), str(t["id"]))

    def id(self, raw, lg):
        name = clean_name(raw)
        for src in (self.hand.get(raw), self.hand.get(name)):
            if src is not None:
                return src or None   # the hand file may say "" (no club of ours): not searched
        if self.cache.get(name) or name in self.asked:
            return self.cache.get(name) or None   # a miss ("") of an earlier run is searched again, once a run: TheSportsDB may know it by now
        self.asked.add(name)
        i = self.by_norm.get(norm(name))
        if not i:
            league = LEAGUE_EN.get(lg)
            hits = {str(t["id"]) for t in self.table.values() if league in (t.get("leagues") or []) and self.alike(name, t.get("en", ""))}
            i = hits.pop() if len(hits) == 1 and lg != "4480" else None   # a European cup's clubs come from everywhere: exact names only
        if not i and self.online:
            i = self.search(name, lg)
            if i is False:   # TheSportsDB did not answer: not kept as «not found», asked again next time
                return None
        self.cache[name] = i or ""
        return i

    def alike(self, a, b):
        """a distinctive word in common ("Luton Town" is not "Ipswich Town"; "FC Bayern München" is "Bayern Munich")"""
        w = lambda x: {v for v in self.ti.latin(x).split() if len(v) >= 4 and v not in self.ti.GENERIC and v not in COMMON_WORDS}
        return bool(w(a) & w(b))

    def search(self, name, lg):
        bare = re.sub(r"\b(FC|CF|AFC|SC|AC|AS|US|AJ|OGC|CD|UD|RC|RCD|BC|BSC|SSC|HSC|SCO|KV|SBV)\b\.?", "", name).strip()
        for q in dict.fromkeys([name, bare, self.ti.fold(name), self.ti.fold(bare)]):
            try:
                d = self.ti.fetch(self.ti.API + "searchteams.php?t=" + urllib.parse.quote(q), pause=2.5) or {}
            except Exception:   # noqa: BLE001 - no answer this time; asked again on the next backfill
                return False
            rows = [t for t in (d.get("teams") or []) if t.get("strSport") == "Soccer"
                    and (norm(t.get("strTeam")) == norm(name) or self.alike(name, t.get("strTeam", "")))]
            best = [t for t in rows if lg in {str(t.get(k) or "") for k in ("idLeague", "idLeague2", "idLeague3", "idLeague4")}] or rows[:1]
            if best:
                return str(best[0]["idTeam"])
        return None

    def save(self):
        if self.cache_file:
            self.cache_file.write_text(json.dumps(dict(sorted(self.cache.items())), ensure_ascii=False, indent=0) + "\n", encoding="utf-8")


def round_of(text):
    """openfootball's round: "Matchday 7", "League, Matchday 7", "Round 7" -> "7"; a knockout round keeps its words ("Quarter-finals")."""
    m = re.search(r"(?:Matchday|Round|Spieltag|Jornada|Giornata|Journée)\s*(\d+)\s*$", text or "", re.I)
    return m.group(1) if m else (text or "").strip()[:40]


def calendar_years(seasons):
    """2024-25, 2025-26 -> 2024, 2025, 2026 (the years a calendar-year league's seasons are named by)"""
    years = set()
    for s in seasons:
        if re.fullmatch(r"\d{4}-\d{2}", s):
            years |= {s[:4], s[:2] + s[5:]}
        elif re.fullmatch(r"\d{4}", s):
            years.add(s)
    return sorted(years)


def read_json(url, tries=4, ua="UrTV-archive/1.0 (https://github.com/tshi-dev/UrTV; open data)"):
    """(data, None) or (None, why): a 404 is «not in the source» at once; anything else is asked again, 5 s, 10 s, 20 s apart."""
    why = ""
    for i in range(tries):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": ua}), timeout=60) as r:
                return json.load(r), None
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None, "not in the source (404)"
            why = f"HTTP {e.code}"
            wait = e.headers.get("Retry-After") if e.headers else None
            time.sleep(int(wait) if wait and wait.isdigit() and int(wait) < 120 else 5 * 2 ** i)
        except (urllib.error.URLError, TimeoutError, ValueError) as e:
            why = type(e).__name__
            time.sleep(5 * 2 ** i)
    return None, f"not read after {tries} tries: {why}"


def backfill(args):
    """openfootball/football.json's seasons into a file build merges: the names mapped to TheSportsDB ids, the ones not found reported."""
    from zoneinfo import ZoneInfo
    names = Names(Path(args.names) if args.names else None, online=not args.no_search)
    seasons = args.seasons.split(",")
    comps = args.comps.split(",") if args.comps else list(OPENFOOTBALL)
    out, missing, report = [], {}, {}
    for comp in comps:
        lg, zone = OPENFOOTBALL[comp], ZoneInfo(OF_ZONE.get(comp, "Europe/Paris"))
        for season in (calendar_years(seasons) if comp in OF_CALENDAR else seasons):
            d, why = read_json(f"{OF_BASE}{season}/{comp}.json")
            if d is None:
                report[f"{comp} {season}"] = why
                continue
            n = 0
            for m in d.get("matches") or []:
                sc = m.get("score")   # {"ft": [a, b]}; [a, b] or score1/score2 in some older files
                ft = sc.get("ft") if isinstance(sc, dict) else sc if isinstance(sc, list) else [m.get("score1"), m.get("score2")]
                if not ft or len(ft) != 2 or ft[0] is None or ft[1] is None or not m.get("date"):
                    continue
                ids = []
                for name in (m.get("team1"), m.get("team2")):
                    i = names.id(name, lg)
                    if not i:
                        missing[clean_name(name)] = missing.get(clean_name(name), 0) + 1
                    ids.append(i)
                if not all(ids):
                    continue
                local = dt.datetime.fromisoformat(f"{m['date']}T{(m.get('time') or '12:00')[:5]}").replace(tzinfo=zone)
                tsdb_season = season.replace("-", "-20") if re.fullmatch(r"\d{4}-\d{2}", season) else season   # 2024-25 -> 2024-2025
                out.append({"id": "", "lg": lg, "s": tsdb_season, "t": iso(local.astimezone(dt.timezone.utc)), "h": ids[0], "a": ids[1],
                            "hn": names.table.get(ids[0], {}).get("en") or clean_name(m["team1"]),
                            "an": names.table.get(ids[1], {}).get("en") or clean_name(m["team2"]),
                            "hs": str(ft[0]), "as": str(ft[1]), "st": "FT", "r": round_of(m.get("round")), "sp": "Soccer", "pp": False,
                            "src": "openfootball"})
                n += 1
            report[f"{comp} {season}"] = n
    names.save()
    Path(args.out).write_text(json.dumps({"events": out}, ensure_ascii=False) + "\n", encoding="utf-8")
    rep = {"games": len(out), "read": report, "requests": names.ti.STATS.get("thesportsdb", 0),
           "names_not_found": dict(sorted(missing.items(), key=lambda kv: -kv[1]))}
    Path(args.report).write_text(json.dumps(rep, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(json.dumps(rep, ensure_ascii=False)[:3000])
    return 0


# ---------- the seasons' history from TheSportsDB (free key) ----------

# The leagues people follow (Prefs.LEAGUES and the catalogue's top ones), Saudi first. teams: a double round-robin's clubs, by season
# (the expected count is teams * (teams - 1)); None: a cup or a format with play-offs, reported without an expected count.
HISTORY = [
    ("4668", "Saudi Pro League", {"2021-2022": 16, "2022-2023": 16, "2023-2024": 18, "2024-2025": 18, "2025-2026": 18, "2026-2027": 18}),
    ("4339", "Turkish Super Lig", {"2021-2022": 20, "2022-2023": 19, "2023-2024": 20, "2024-2025": 19, "2025-2026": 18, "2026-2027": 18}),
    ("4480", "UEFA Champions League", None),
    ("4719", "AFC Champions League Elite", None),
    ("4829", "Egyptian Premier League", None),
    ("5649", "Saudi King Cup", None),
    ("4663", "Qatar Stars League", "auto"),
    ("4678", "UAE Pro League", "auto"),
    ("5056", "Iraqi Premier League", "auto"),
    ("4520", "Moroccan Championship", "auto"),
    ("4346", "American Major League Soccer", None),
    ("5627", "Saudi First Division League", "auto"),
    ("4328", "English Premier League", 20), ("4335", "Spanish La Liga", 20), ("4332", "Italian Serie A", 20),
    ("4331", "German Bundesliga", 18), ("4334", "French Ligue 1", {"2021-2022": 20, "2022-2023": 20, "*": 18}),
    ("4344", "Portuguese Primeira Liga", 18), ("4337", "Dutch Eredivisie", 18),
]
# teams: the clubs of a double round-robin (expected games = teams * (teams - 1)), by season where it changed; "auto": the same formula from
# the clubs the season's games name (reported, never a problem: a format may have play-offs); None: a cup or a league with play-offs.
CUP_ROUNDS = [105, 110, 115, 120, 125, 130, 140, 150, 160, 170, 180, 190, 200, 500]   # TheSportsDB's knockout rounds (125 quarter-finals, 150 semis, 200 final)
WIKI_PAGE = {"4668": "{y1}–{y2} Saudi Pro League"}   # the season's results table, for the check (en.wikipedia.org, CC BY-SA)
WIKI_API = "https://en.wikipedia.org/w/api.php"


def expected(spec, season, teams=0):
    """(the expected games of the season, whether that count is sure enough to call a shortfall a problem)"""
    if spec == "auto":
        return (teams * (teams - 1) if teams > 1 else None), False
    n = spec.get(season, spec.get("*")) if isinstance(spec, dict) else spec
    return (n * (n - 1) if n else None), bool(n)


def clubs(evs):
    return len(({e.get("idHomeTeam") for e in evs.values()} | {e.get("idAwayTeam") for e in evs.values()}) - {None, ""})


def plan(args):
    """The matrix of the history workflow: the leagues of HISTORY (in its order, Saudi first), or those asked for. Every league is in it every
    night: its current season is read again (that is how a followed league's finished games all land, whatever the free key's league calls
    give), its complete past seasons are skipped by history itself."""
    only = [x for x in (args.leagues or "").replace(" ", "").split(",") if x]
    ids = [lg for lg, _, _ in HISTORY if not only or lg in only]
    print(json.dumps({"league": ids}))
    return 0


def merge_history(args):
    """The matrix's league files into one: games (one file for build), state (history.json: the previous one with each league's seasons
    replaced by what its job read) and report."""
    state = json.loads(Path(args.state).read_text(encoding="utf-8")) if args.state and Path(args.state).is_file() else {}
    games, report, failed = [], {}, []
    for d in sorted(x for x in Path(args.dir).glob("*") if x.is_dir()):
        g, st, rp = d / "games.json", d / "state.json", d / "report.json"
        if not (g.is_file() and st.is_file() and rp.is_file()):
            failed.append(d.name)
            continue
        games += json.loads(g.read_text(encoding="utf-8")).get("events") or []
        for lg, seasons in json.loads(st.read_text(encoding="utf-8")).items():
            if lg in json.loads(rp.read_text(encoding="utf-8")):   # only the league this job read
                state.setdefault(lg, {}).update(seasons)
        r = json.loads(rp.read_text(encoding="utf-8"))
        for k, v in r.items():
            if k in ("requests", "stopped", "check"):
                report.setdefault(k, {})[d.name] = v
            else:
                report[k] = v
    want = [x for x in (args.expect or "").split(",") if x]
    got = {d.name.replace("history-", "") for d in Path(args.dir).glob("*") if d.is_dir() and d.name not in failed}
    report["failed"] = sorted(set(failed) | {f"history-{x}" for x in want if x not in got})
    Path(args.out).write_text(json.dumps({"events": games}, ensure_ascii=False) + "\n", encoding="utf-8")
    Path(args.state_out).write_text(json.dumps(state, ensure_ascii=False, indent=1, sort_keys=True) + "\n", encoding="utf-8")
    Path(args.report).write_text(json.dumps(report, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"{len(games)} games from {len(got)} leagues; failed: {report['failed']}")
    return 0


def history(args):
    """Every game of the last seasons of the followed leagues, from TheSportsDB with the free key: the season list (at most 100 games),
    then round by round, then (only where a season is still short and the budget allows) day by day. A past season that reached its
    expected count is not asked again (history.json). Writes the games for build (--history) and a report per league and season."""
    sys.path.insert(0, str(HERE))
    import team_ids as ti
    pace = args.pace if args.pace is not None else float(os.environ.get("PACE") or (2.4 if ti.PAID else 2.2))
    n_seasons = args.seasons or (5 if ti.PAID else 3)   # the paid key reads a whole season in one request: two more seasons cost two requests
    state = json.loads(Path(args.state).read_text(encoding="utf-8")) if args.state and Path(args.state).is_file() else {}
    only = set(args.leagues.split(",")) if args.leagues else None
    budget = args.budget
    got_all, report, errors = [], {}, {}

    def ask(path):
        if ti.STATS["thesportsdb"] >= budget:
            raise TimeoutError("budget")
        try:
            return ti.fetch(ti.API + path, pause=pace) or {}
        except Exception as e:   # noqa: BLE001 - a 404 or no answer is an empty answer, counted; never the key (redact)
            errors[path.split("?")[0]] = errors.get(path.split("?")[0], 0) + 1
            print(ti.redact(f"{path.split('?')[0]}: {type(e).__name__} {getattr(e, 'code', '')}"), flush=True)
            return {}

    def keep(evs, lg, season, into):
        n = 0
        for e in evs or []:
            if str(e.get("idLeague") or "") == lg and e.get("idEvent") and (not season or e.get("strSeason") in (None, "", season)):
                if e["idEvent"] not in into:
                    into[e["idEvent"]] = e
                    n += 1
        return n

    try:
        for lg, name, spec in HISTORY:
            if only and lg not in only:
                continue
            seasons = sorted({x.get("strSeason") for x in (ask(f"search_all_seasons.php?id={lg}").get("seasons") or []) if x.get("strSeason")})
            seasons = seasons[-n_seasons:]
            report[lg] = {"name": name, "key": "paid" if ti.PAID else "free", "seasons": {}}
            for season in seasons:
                done = state.get(lg, {}).get(season, {})
                if done.get("complete") and season != seasons[-1]:
                    report[lg]["seasons"][season] = dict(done, asked="no (complete before)")
                    continue
                evs, how = {}, {}
                how["season"] = keep(ask(f"eventsseason.php?id={lg}&s={season}").get("events"), lg, season, evs)
                rounds = {int(e["intRound"]) for e in evs.values() if str(e.get("intRound") or "").isdigit()}
                want, sure = expected(spec, season, clubs(evs))
                n, empty, r, found = 0, 0, 1, 0
                limit = max([60] + [x + 3 for x in rounds if x < 100])
                whole = ti.PAID and how["season"] > 0 and not (want and sure and len(evs) < want)   # the paid key's season list is the whole season
                while not whole and r <= limit and empty < (3 if found else 4):
                    k = keep(ask(f"eventsround.php?id={lg}&r={r}&s={season}").get("events"), lg, season, evs)
                    got_round = k or any(str(e.get("intRound")) == str(r) for e in evs.values())
                    found += bool(got_round)
                    empty = 0 if got_round else empty + 1
                    n += k
                    r += 1
                    if want and len(evs) >= want:
                        break
                for cr in sorted(set(CUP_ROUNDS) | {x for x in rounds if x >= 100}) if not want and not whole else []:
                    n += keep(ask(f"eventsround.php?id={lg}&r={cr}&s={season}").get("events"), lg, season, evs)
                how["rounds"] = n
                if want and len(evs) < want and args.days:
                    dates = sorted(e.get("dateEvent") for e in evs.values() if e.get("dateEvent"))
                    if dates:
                        d0 = dt.date.fromisoformat(dates[0]) - dt.timedelta(days=21)   # the season's first and last weeks may be missing
                        d1 = dt.date.fromisoformat(dates[-1]) + dt.timedelta(days=21)
                        have = {e.get("dateEvent") for e in evs.values()}
                        n, d = 0, d0
                        while d <= min(d1, now_utc().date()) and len(evs) < want:
                            if d.isoformat() not in have:
                                n += keep(ask(f"eventsday.php?d={d.isoformat()}&l={lg}").get("events"), lg, season, evs)
                            d += dt.timedelta(days=1)
                        how["days"] = n
                finished = sum(1 for e in evs.values() if stage(compact(e)) == 3)
                teams = clubs(evs)
                want, sure = expected(spec, season, teams)
                past = season != seasons[-1]
                complete = past and finished > 0 and ((finished >= want) if (want and sure) else (whole or not want or finished >= want))
                row = {"games": len(evs), "finished": finished, "expected": want, "expected_sure": sure, "teams": teams, "how": how,
                       "current": not past, "complete": complete, "at": iso(now_utc())}
                old = state.get(lg, {}).get(season, {})
                if old.get("finished", 0) > finished:   # a short answer this time (a limit, an error) never lowers what was read before
                    row["note"] = f"read {finished} finished this time, {old['finished']} before: the store keeps them all"
                state.setdefault(lg, {})[season] = {k: row[k] for k in ("games", "finished", "expected", "expected_sure", "teams", "current",
                                                                         "complete", "at")}
                report[lg]["seasons"][season] = row
                got_all += list(evs.values())
                print(f"{lg} {name} {season}: {len(evs)} games, {finished} finished, expected {want}, {how}", flush=True)
    except TimeoutError:
        report["stopped"] = f"the request budget ({budget}) ran out: the next run goes on from here"
    if args.check:
        report["check"] = wiki_check(got_all, ask_ok=True)
    report["requests"] = dict(ti.STATS, errors=errors)
    Path(args.out).write_text(json.dumps({"events": got_all}, ensure_ascii=False) + "\n", encoding="utf-8")
    if args.state:
        Path(args.state).write_text(json.dumps(state, ensure_ascii=False, indent=1, sort_keys=True) + "\n", encoding="utf-8")
    Path(args.report).write_text(json.dumps(report, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    return 0


def wiki_results(title):
    """{(home name, away name): (hs, as)} from a season's «Sports results» table on en.wikipedia.org (the table may be in the page or
    in a template it transcludes), and the clubs' names {abbreviation: name}."""
    d, why = read_json(WIKI_API + "?" + urllib.parse.urlencode({"action": "parse", "page": title, "prop": "wikitext", "format": "json",
                                                                 "formatversion": "2", "redirects": "1"}))
    if d is None:
        return None, why
    text = (d.get("parse") or {}).get("wikitext") or ""
    if "match_" not in text:
        for t in re.findall(r"\{\{\s*([^{}|]*?results[^{}|]*)\}\}", text, re.I)[:3]:
            d2, _ = read_json(WIKI_API + "?" + urllib.parse.urlencode({"action": "parse", "page": "Template:" + t.strip(), "prop": "wikitext",
                                                                       "format": "json", "formatversion": "2", "redirects": "1"}))
            text += ((d2 or {}).get("parse") or {}).get("wikitext") or ""
    names = {}
    for ab, val in re.findall(r"\|\s*name_([A-Z0-9]+)\s*=\s*([^\n]+)", text):
        v = re.sub(r"\{\{\s*nobr\s*\|", "", val)                       # {{nobr|[[Al Taawoun FC|Al-Taawoun]]}}
        v = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", v)
        v = re.sub(r"<ref.*?(</ref>|/>)|\{\{.*?\}\}|<[^>]+>", "", v)
        names[ab] = v.replace("}}", "").split("|")[0].strip()
    out = {}
    for h, a, val in re.findall(r"\|\s*match_([A-Z0-9]+)_([A-Z0-9]+)\s*=\s*([^|\n]*(?:\|[^\]\n]*\]\])?)", text):
        m = re.search(r"(\d+)\s*[–—-]\s*(\d+)", re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", val))   # the score, not a link's season
        if m and h in names and a in names:
            out[(names[h], names[a])] = (m.group(1), m.group(2))
    return out, (None if out else "no results table found")


def wiki_check(events, ask_ok=True):
    """Each season of WIKI_PAGE's leagues checked against Wikipedia: every finished game of ours with Wikipedia's score for the same home and
    away clubs; the counts and the first differences, for the report."""
    sys.path.insert(0, str(HERE))
    import team_ids as ti
    out = {}
    by = {}
    for e in events:
        if str(e.get("idLeague")) in WIKI_PAGE and e.get("strSeason"):
            by.setdefault((str(e["idLeague"]), e["strSeason"]), []).append(e)
    for (lg, season), evs in sorted(by.items()):
        y1, y2 = season[:4], season[-2:]
        title = WIKI_PAGE[lg].format(y1=y1, y2=y2)
        table, why = wiki_results(title)
        row = {"page": title}
        if not table:
            out[f"{lg} {season}"] = dict(row, problem=why)
            continue
        known = teams_table()
        clubs = {}   # team id -> every name it goes by (TheSportsDB's spellings vary: "Al Feiha", "Dhamk"; teams.json's English names)
        for e in evs:
            for i, n in ((e.get("idHomeTeam"), e.get("strHomeTeam")), (e.get("idAwayTeam"), e.get("strAwayTeam"))):
                if i:
                    clubs.setdefault(str(i), set()).update({n or ""} | {known.get(str(i), {}).get("en", "")} | set(known.get(str(i), {}).get("alt") or []))
        wnames = {n for pair in table for n in pair}

        def ours(wname):
            hits = {i for i, ns in clubs.items() if any(n and ti.team_key(n) == ti.team_key(wname) for n in ns)}
            hits = hits or {i for i, ns in clubs.items() if any(n and ti.same_team(n, wname) for n in ns)}
            stem = lambda x: {w[:5] for w in ti.latin(x).split() if len(w) >= 5}   # "Al-Najmah" / "Al Najma"
            hits = hits or {i for i, ns in clubs.items() if any(n and stem(n) & stem(wname) for n in ns)}
            return hits.pop() if len(hits) == 1 else None
        mapping = {w: ours(w) for w in wnames}
        wt = {(mapping[h], mapping[a]): sc for (h, a), sc in table.items() if mapping[h] and mapping[a]}
        same = differ = missing = 0
        diffs = []
        for e in evs:
            c = compact(e)
            if stage(c) != 3:
                continue
            w = wt.get((str(e.get("idHomeTeam")), str(e.get("idAwayTeam"))))
            if not w:
                missing += 1
            elif (str(c["hs"]), str(c["as"])) == w:
                same += 1
            else:
                differ += 1
                diffs.append(f"{e.get('dateEvent')} {e.get('strHomeTeam')} {c['hs']}-{c['as']} {e.get('strAwayTeam')} (wiki {w[0]}-{w[1]})")
        out[f"{lg} {season}"] = dict(row, wiki_games=len(table), clubs_unmatched=sorted(w for w, v in mapping.items() if not v),
                                     same=same, differ=differ, not_in_wiki=missing, differences=diffs[:10])
    return out

def main():
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="cmd", required=True)
    b = sub.add_parser("build")
    b.add_argument("--leagues", required=True)
    b.add_argument("--prev", required=True)
    b.add_argument("--out", required=True)
    b.add_argument("--live")
    b.add_argument("--live-note", dest="live_note")
    b.add_argument("--backfill")
    b.add_argument("--backfill-report", dest="backfill_report")
    b.add_argument("--names")
    b.add_argument("--history", help="the games history wrote (TheSportsDB's form)")
    b.add_argument("--history-state", dest="history_state", help="history.json, kept on the branch")
    b.add_argument("--history-report", dest="history_report", help="the history run's report, kept on the branch as history-report.json")
    lv = sub.add_parser("live")
    lv.add_argument("--prev", required=True)
    lv.add_argument("--out", required=True)
    lv.add_argument("--note", required=True)
    bf = sub.add_parser("backfill")
    bf.add_argument("--out", required=True)
    bf.add_argument("--report", required=True)
    bf.add_argument("--seasons", default="2021-22,2022-23,2023-24,2024-25,2025-26,2026-27")
    bf.add_argument("--comps", default="")
    bf.add_argument("--no-search", dest="no_search", action="store_true", help="teams.json and the hand file only, no TheSportsDB search")
    bf.add_argument("--names", help="the names found by earlier backfills (data-archive/names.json), read and written")
    hi = sub.add_parser("history")
    hi.add_argument("--out", required=True)
    hi.add_argument("--report", required=True)
    hi.add_argument("--state", help="history.json: the seasons already complete, read and written")
    hi.add_argument("--leagues", default="", help="ids, comma-separated (default: every league of HISTORY, Saudi first)")
    hi.add_argument("--seasons", type=int, default=0, help="the last N seasons of each league, the current one included (default 5 with the paid key, 3 with the free one)")
    hi.add_argument("--budget", type=int, default=2500, help="at most this many requests (the free key: about 30 a minute)")
    hi.add_argument("--pace", type=float, default=None, help="seconds between requests (default: env PACE, else 2.4 paid, 2.2 free)")
    hi.add_argument("--days", action="store_true", help="a season still short after its rounds is asked day by day (costly)")
    hi.add_argument("--check", action="store_true", help="check the seasons against Wikipedia's results tables (WIKI_PAGE)")
    pl = sub.add_parser("plan")
    pl.add_argument("--leagues", default="")
    mh = sub.add_parser("merge-history")
    mh.add_argument("--dir", required=True, help="one folder per league job: games.json, state.json, report.json")
    mh.add_argument("--state", help="the previous history.json")
    mh.add_argument("--expect", default="", help="the leagues the matrix had (a missing one is listed as failed)")
    mh.add_argument("--out", required=True)
    mh.add_argument("--state-out", dest="state_out", required=True)
    mh.add_argument("--report", required=True)
    a = p.parse_args()
    return {"build": build, "live": live, "backfill": backfill, "history": history, "plan": plan, "merge-history": merge_history}[a.cmd](a)


if __name__ == "__main__":
    sys.exit(main())
