#!/usr/bin/env python3
"""4.4 «الرياضة»: the sports cards' files (.github/tools/sports_files.py), offline: sports/<league>.json from the archive's store (the table
computed from the season's finished games, each club's last 5, the last 2 days, the next 7, a cup's knockout rounds), written by every
archive build; f1/next.json from Jolpica-F1's answer, asked at most once a day, a failure keeps the last file; the workflow step.
Run: python3 tests/sports_files_test.py"""
import datetime as dt
import importlib.util
import json
import sys
import tempfile
from pathlib import Path
from types import SimpleNamespace

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / ".github/tools"))
spec = importlib.util.spec_from_file_location("match_archive", ROOT / ".github/tools/match_archive.py")
ma = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ma)
import sports_files as sf   # noqa: E402
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


now = dt.datetime.now(dt.timezone.utc).replace(microsecond=0)
ts = lambda d, h=15: (now + dt.timedelta(days=d)).replace(hour=h, minute=0, second=0).strftime("%Y-%m-%dT%H:%M:%S")
def ev(i, h, a, when, hs=None, as_=None, st="NS", lg="4668", r="1"):
    return {"idEvent": i, "idLeague": lg, "idHomeTeam": h, "idAwayTeam": a, "strHomeTeam": "T" + h, "strAwayTeam": "T" + a, "intHomeScore": hs,
            "intAwayScore": as_, "strStatus": st, "strTimestamp": when, "strSeason": "2026-2027", "strSport": "Soccer", "intRound": r, "strPostponed": "no"}
games = [ev("1", "10", "20", ts(-20), "2", "0", "FT"), ev("2", "30", "10", ts(-13), "1", "1", "FT"), ev("3", "20", "30", ts(-6), "0", "3", "FT"),
         ev("4", "10", "30", ts(-1), "3", "1", "FT"), ev("5", "20", "10", ts(2)), ev("6", "30", "20", ts(9)), ev("7", "10", "20", ts(-3), "1", "0", "FT")]
cupg = [ev("c1", "10", "40", ts(-30), "2", "1", "FT", lg="4719", r="16"), ev("c2", "40", "10", ts(-23), "0", "0", "FT", lg="4719", r="16"),
        ev("c3", "10", "50", ts(5), lg="4719", r="125"), ev("c4", "60", "70", ts(-40), "1", "0", "FT", lg="4719", r="3")]
lg_dir = Path(tempfile.mkdtemp())
(lg_dir / "saudi.json").write_text(json.dumps({"league": "Saudi-Arabian Pro League", "id": "4668", "events": games}))
(lg_dir / "afc.json").write_text(json.dumps({"league": "AFC Champions League Elite", "id": "4719", "events": cupg}))
prev = Path(tempfile.mkdtemp())
(prev / "f1").mkdir()
(prev / "f1/next.json").write_text('{"v":1,"name":"Kept GP","asked":"2000-01-01T00:00:00Z","updated":"2000-01-01T00:00:00Z","sessions":[]}')
out = Path(tempfile.mkdtemp()) / "out"
ma.build(SimpleNamespace(leagues=str(lg_dir), prev=str(prev), out=str(out), live=None, live_note=None, backfill=None, backfill_report=None, names=None))

s = json.loads((out / "sports/4668.json").read_text())
check("every archive build writes sports/<league>.json (v 1, its names and season)", s["v"] == 1 and s["id"] == "4668" and s["season"] == "2026-2027" and s["ar"] == "دوري روشن السعودي", s)
tab = [(r["id"], r["p"], r["pts"], r["gf"] - r["ga"], r["f"]) for r in s["table"]]
check("the table: 3 a win, 1 a draw, by points then goal difference; each club's last 5 newest first", tab == [("10", 4, 10, 5, "wwdw"), ("30", 3, 4, 1, "lwd"), ("20", 3, 0, -6, "lll")], tab)
check("the last 2 days' results only, newest first", [g["id"] for g in s["last"]] == ["4"], s["last"])
check("the next 7 days' games only", [g["id"] for g in s["next"]] == ["5"], s["next"])
check("a league with no knockout round has no cup", "cup" not in s)
c = json.loads((out / "sports/4719.json").read_text())["cup"]["rounds"]
check("a cup: its knockout games by round, named, in the cup's order (a group game left out)", [(r["r"], r["ar"], [g["id"] for g in r["games"]]) for r in c]
      == [("16", "دور الـ١٦", ["c1", "c2"]), ("125", "ربع النهائي", ["c3"])], c)
check("the F1 file is carried over by a build that does not ask for it", json.loads((out / "f1/next.json").read_text())["name"] == "Kept GP")
size = (out / "sports/4668.json").stat().st_size
check("the file is small", size < 20000, size)

answer = {"MRData": {"RaceTable": {"Races": [{"season": "2026", "round": "24", "raceName": "Abu Dhabi Grand Prix", "date": "2026-12-06", "time": "13:00:00Z",
          "Circuit": {"circuitName": "Yas Marina Circuit", "Location": {"country": "UAE", "locality": "Abu Dhabi"}},
          "FirstPractice": {"date": "2026-12-04", "time": "09:30:00Z"}, "Qualifying": {"date": "2026-12-05", "time": "14:00:00Z"}}]}}}
f = sf.f1_file(answer, "2026-11-16T00:00:00Z")
check("f1: the race, its Arabic name and its sessions in time order (UTC)", f["name"] == "Abu Dhabi Grand Prix" and f["ar"] == "جائزة أبوظبي الكبرى"
      and [(x["k"], x["t"]) for x in f["sessions"]] == [("fp1", "2026-12-04T09:30:00Z"), ("q", "2026-12-05T14:00:00Z"), ("race", "2026-12-06T13:00:00Z")], f)
check("f1: no race in the answer: none", sf.f1_file({"MRData": {"RaceTable": {"Races": []}}}, "x") is None)
asked = []
fetch = lambda: (asked.append(1), json.dumps(answer).encode())[1]
o2 = Path(tempfile.mkdtemp())
sf.f1(SimpleNamespace(prev=str(out), out=str(o2)), fetch)
n1 = json.loads((o2 / "f1/next.json").read_text())
check("f1: a stale file is asked again and replaced", asked == [1] and n1["name"] == "Abu Dhabi Grand Prix" and n1.get("asked"), n1)
o3 = Path(tempfile.mkdtemp())
sf.f1(SimpleNamespace(prev=str(o2), out=str(o3)), fetch)
check("f1: asked at most once a day (a fresh file is kept, no request)", asked == [1] and json.loads((o3 / "f1/next.json").read_text())["name"] == "Abu Dhabi Grand Prix")
def boom():
    raise OSError("no network")
o4 = Path(tempfile.mkdtemp())
sf.f1(SimpleNamespace(prev=str(out), out=str(o4)), boom)
check("f1: no answer keeps the last file", json.loads((o4 / "f1/next.json").read_text())["name"] == "Kept GP")
wf = (ROOT / ".github/workflows/league-fixtures.yml").read_text(encoding="utf-8")
check("the workflow asks for F1 after the build, before the push, and a failure never fails the run",
      wf.index("match_archive.py build") < wf.index("sports_files.py f1 --prev prev --out out || echo") < wf.index("Push to data-archive"))
sys.exit(1 if failed else 0)
