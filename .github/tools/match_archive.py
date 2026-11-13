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
import re
import shutil
import sys
import time
import unicodedata
import urllib.parse
import urllib.request
from pathlib import Path

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
                "uefa.cl": "4480"}
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
    if args.backfill and Path(args.backfill).is_file():
        for e in json.loads(Path(args.backfill).read_text(encoding="utf-8")).get("events") or []:
            r = store.put(e)
            if r:
                counts[r] += 1
    dropped = store.prune(now)
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
    rep = prev / "backfill.json"
    if args.backfill_report and Path(args.backfill_report).is_file():
        rep = Path(args.backfill_report)
    if rep.is_file():
        shutil.copyfile(rep, out / "backfill.json")
        r = json.loads(rep.read_text(encoding="utf-8"))
        health["backfill"] = {"games": r.get("games", 0), "names_not_found": len(r.get("names_not_found") or {})}
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


OF_ZONE = {"en.1": "Europe/London", "pt.1": "Europe/Lisbon", "tr.1": "Europe/Istanbul"}   # openfootball's times are local; the rest CET
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
        for src in (self.hand.get(raw), self.hand.get(name), self.cache.get(name)):
            if src is not None:
                return src or None
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
        for q in dict.fromkeys([name, re.sub(r"\b(FC|CF|AFC|SC|AC|US|AJ|OGC|CD|UD|RC|RCD|BC|SSC|HSC|SCO|KV|SBV)\b\.?", "", name).strip()]):
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


def backfill(args):
    """openfootball/football.json's seasons into a file build merges: the names mapped to TheSportsDB ids, the ones not found reported."""
    from zoneinfo import ZoneInfo
    names = Names(Path(args.names) if args.names else None, online=not args.no_search)
    seasons = args.seasons.split(",")
    comps = args.comps.split(",") if args.comps else list(OPENFOOTBALL)
    out, missing, report = [], {}, {}
    for comp in comps:
        lg, zone = OPENFOOTBALL[comp], ZoneInfo(OF_ZONE.get(comp, "Europe/Paris"))
        for season in seasons:
            url = f"{OF_BASE}{season}/{comp}.json"
            try:
                with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "UrTV archive"}), timeout=60) as r:
                    d = json.load(r)
            except Exception as e:   # noqa: BLE001 - a season that is not there is reported, not fatal
                report[f"{comp} {season}"] = f"not read: {type(e).__name__}"
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
                            "hs": str(ft[0]), "as": str(ft[1]), "st": "FT", "r": "", "sp": "Soccer", "pp": False, "src": "openfootball"})
                n += 1
            report[f"{comp} {season}"] = n
    names.save()
    Path(args.out).write_text(json.dumps({"events": out}, ensure_ascii=False) + "\n", encoding="utf-8")
    rep = {"games": len(out), "read": report, "requests": names.ti.STATS.get("thesportsdb", 0),
           "names_not_found": dict(sorted(missing.items(), key=lambda kv: -kv[1]))}
    Path(args.report).write_text(json.dumps(rep, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(json.dumps(rep, ensure_ascii=False)[:3000])
    return 0


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
    a = p.parse_args()
    return {"build": build, "live": live, "backfill": backfill}[a.cmd](a)


if __name__ == "__main__":
    sys.exit(main())
