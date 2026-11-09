#!/usr/bin/env python3
"""4.3 report only: how much of «every competition» TheSportsDB's free key gives, before a catalogue is built. Run in CI
(catalogue-probe.yml): the host is unreachable from the dev container. At most one request every 2.5 s (the free key: 30 a minute).
It records: all_leagues.php (how many soccer competitions, with which fields), all_countries.php, search_all_leagues.php for every country
(soccer only: how many each gives, with badge and type), and lookupleague.php for a sample (the logo and the current season), and whether
any answer looks capped. Out: catalogue-probe/report.md and report.json.

    python3 .github/tools/catalogue_probe.py
"""
import json
import time
import urllib.parse
import urllib.request
from pathlib import Path

BASE = "https://www.thesportsdb.com/api/v1/json/123/"   # the free key the page and team_ids.py use
GAP = 2.5
OUT = Path("catalogue-probe")
stats = {"requests": 0, "429": 0, "errors": 0}


def get(path, **q):
    url = BASE + path + ("?" + urllib.parse.urlencode(q) if q else "")
    for attempt in range(3):
        time.sleep(GAP)
        stats["requests"] += 1
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "UrTV-probe"}), timeout=30) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            if e.code == 429:
                stats["429"] += 1
                time.sleep(30)
                continue
            stats["errors"] += 1
            stats.setdefault("http", []).append(e.code)
            return None
        except Exception as e:   # noqa: BLE001 - a probe reports
            stats["errors"] += 1
            stats.setdefault("other", []).append(type(e).__name__)
            return None
    return None


def main():
    OUT.mkdir(exist_ok=True)
    rep = {}
    al = get("all_leagues.php") or {}
    leagues = al.get("leagues") or []
    soccer = [l for l in leagues if l.get("strSport") == "Soccer"]
    rep["all_leagues"] = {"total": len(leagues), "soccer": len(soccer), "fields": sorted(leagues[0].keys()) if leagues else [],
                          "sports": sorted({l.get("strSport") for l in leagues})}
    co = get("all_countries.php") or {}
    countries = [c.get("name_en") for c in (co.get("countries") or []) if c.get("name_en")]
    rep["countries"] = len(countries)
    per = {}
    seen = {}
    started = time.time()
    for c in countries:
        if time.time() - started > 40 * 60:   # the job's time: what is left is said
            per["_stopped_at"] = c
            break
        got = get("search_all_leagues.php", c=c, s="Soccer") or {}
        rows = got.get("countries") or got.get("leagues") or []
        per[c] = len(rows)
        for r in rows:
            seen[r.get("idLeague")] = {"id": r.get("idLeague"), "name": r.get("strLeague"), "alt": r.get("strLeagueAlternate"), "country": r.get("strCountry"),
                                       "badge": bool(r.get("strBadge")), "type": r.get("strCurrentSeason") and "season" or "", "gender": r.get("strGender")}
    rep["search_by_country"] = {"countries_asked": len([k for k in per if not k.startswith("_")]), "leagues_found": len(seen),
                                "with_badge": sum(1 for v in seen.values() if v["badge"]), "max_in_one_country": max([v for k, v in per.items() if not k.startswith("_")] or [0]),
                                "countries_with_none": sum(1 for k, v in per.items() if not k.startswith("_") and v == 0), "stopped_at": per.get("_stopped_at")}
    top = sorted(((k, v) for k, v in per.items() if not k.startswith("_")), key=lambda kv: -kv[1])[:15]
    rep["most_per_country"] = top
    sample = [l.get("idLeague") for l in soccer[:5]]
    rep["lookup_sample"] = []
    for lid in sample:
        lk = get("lookupleague.php", id=lid) or {}
        row = (lk.get("leagues") or [{}])[0]
        rep["lookup_sample"].append({"id": lid, "name": row.get("strLeague"), "badge": bool(row.get("strBadge")), "season": row.get("strCurrentSeason")})
    rep["stats"] = stats
    (OUT / "report.json").write_text(json.dumps({"report": rep, "leagues": list(seen.values())}, ensure_ascii=False, indent=1), encoding="utf-8")
    lines = ["# TheSportsDB free key: how many competitions", "", "```", json.dumps(rep, ensure_ascii=False, indent=1), "```"]
    (OUT / "report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
