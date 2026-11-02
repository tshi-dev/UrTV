#!/usr/bin/env python3
"""The CI price collector (.github/tools/market_prices.py) with Yahoo mocked: the symbols are what the page offers, which
are due at which hour (Riyadh, New York in summer and winter time), a failed symbol keeps its old quote and its old time,
a 404 symbol is left out, a run that got nothing changes nothing, and the workflow only pushes when something changed.
Run: python3 tests/market_prices_test.py"""
import datetime
import importlib.util
import json
import os
import re
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT, PREV = tempfile.mkdtemp(), tempfile.mkdtemp()
os.environ.update(MARKETS_OUT=OUT, MARKETS_PREV=PREV, MARKETS_PAUSE="0")
spec = importlib.util.spec_from_file_location("market_prices", ROOT / ".github/tools/market_prices.py")
mp = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mp)
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


UTC = datetime.timezone.utc
syms = mp.symbols()
page = (ROOT / "src/template.html").read_text(encoding="utf-8")
check("symbols: every market of the page's list", {"^TASI.SR", "2222.SR", "^GSPC", "GC=F", "SI=F", "BZ=F", "BTC-USD", "XRP-USD"} <= set(syms), sorted(syms)[:10])
check("symbols: Brent oil is refreshed with the market data feed", syms.get("BZ=F") == "always", syms.get("BZ=F"))
check("symbols: every Tadawul code of the page's table, as CODE.SR",
      all(c + ".SR" in syms for c in re.findall(r"'(\d{4})'\s*:", re.search(r"const TADAWUL = \{(.*?)\n\};", page, re.S).group(1))))
check("symbols: the common US tickers", {"AAPL", "NVDA", "TSLA"} <= set(syms))
check("symbols: kinds", syms["2222.SR"] == "sa" and syms["AAPL"] == "us" and syms["BTC-USD"] == "always" and syms["GC=F"] == "always")

# hours: Tadawul Sun-Thu 10-15 Riyadh; the US 9:30-16:00 New York time (summer 13:30-20:00 UTC, winter 14:30-21:00 UTC)
def at(y, m, d, h, mi=0):
    return datetime.datetime(y, m, d, h, mi, tzinfo=UTC)
cases = [("sa", at(2026, 10, 25, 7, 5), True, "Sunday 10:05 Riyadh"), ("sa", at(2026, 10, 25, 12, 30), False, "Sunday 15:30 Riyadh"),
         ("sa", at(2026, 10, 23, 8, 0), False, "Friday"), ("sa", at(2026, 10, 24, 8, 0), False, "Saturday"),
         ("us", at(2026, 10, 23, 13, 45), True, "Friday 9:45 New York, summer time"), ("us", at(2026, 10, 23, 13, 15), False, "9:15 New York, summer time"),
         ("us", at(2026, 12, 15, 14, 45), True, "9:45 New York, winter time"), ("us", at(2026, 12, 15, 14, 15), False, "9:15 New York, winter time"),
         ("us", at(2026, 12, 15, 20, 45), True, "15:45 New York, winter time"), ("us", at(2026, 12, 15, 21, 15), False, "16:15 New York, winter time"),
         ("us", at(2026, 10, 24, 15, 0), False, "Saturday"), ("always", at(2026, 10, 24, 3, 0), True, "gold and Bitcoin at night")]
for kind, when, want, why in cases:
    check(f"hours: {kind} {why} -> {'open' if want else 'closed'}", mp.market_open(kind, when) is want)
now = at(2026, 10, 24, 8, 0)
fresh, old = (now - datetime.timedelta(minutes=20)).isoformat(), (now - datetime.timedelta(minutes=70)).isoformat()
check("due: a closed exchange's quote confirmed 20 minutes ago waits", not mp.due("AAPL", "us", {"AAPL": {"t": fresh}}, now))
check("due: ... and one confirmed 70 minutes ago is asked", mp.due("AAPL", "us", {"AAPL": {"t": old}}, now))
check("due: a symbol never asked is asked", mp.due("AAPL", "us", {}, now))

# a run: AAPL answers, MSFT is 404, NVDA fails (rate limit), everything else answers
RUN = at(2026, 10, 22, 14, 0)   # Thursday 17:00 Riyadh, 10:00 New York: the US is open, Tadawul is closed
answers = {}
def chart(price, prev, closes):
    return {"chart": {"result": [{"meta": {"regularMarketPrice": price, "chartPreviousClose": prev}, "indicators": {"quote": [{"close": closes}]}}]}}
def fake(url):
    sym = re.search(r"chart/([^?]+)\?", url).group(1).replace("%5E", "^").replace("%3D", "=")
    a = answers.get(sym, chart(10.0, 9.0, [9.5 + i / 100 for i in range(100)]))
    if isinstance(a, Exception):
        raise a
    return a
mp.fetch_json = fake
class FakeNow(datetime.datetime):
    @classmethod
    def now(cls, tz=None):
        return RUN
mp.datetime.datetime = FakeNow
answers["MSFT"] = mp.NotFound("MSFT")
answers["NVDA"] = OSError("HTTP Error 429")
(Path(PREV) / "markets.json").write_text(json.dumps({"updated": "2026-10-22T10:00:00+00:00", "quotes": {
    "NVDA": {"p": 100.0, "v": 99.0, "c": [99, 100], "t": "2026-10-22T10:00:00+00:00"},
    "MSFT": {"p": 5.0, "v": 5.0, "c": [], "t": "2026-10-22T10:00:00+00:00"}}}))
mp.main([])
got = json.loads((Path(OUT) / "markets.json").read_text())
rep = json.loads((Path(OUT) / "report.json").read_text())
q = got["quotes"]
check("run: a quote has price, the close before, at most 30 closes and its time", q["AAPL"]["p"] == 10.0 and q["AAPL"]["v"] == 9.0
      and 2 <= len(q["AAPL"]["c"]) <= 30 and q["AAPL"]["t"] == RUN.isoformat(timespec="seconds"), q.get("AAPL"))
# the same answer's volume and 52-week range are kept when there (the home's «الأسواق» page), and left out when not
answers["2222.SR"] = {"chart": {"result": [{"meta": {"regularMarketPrice": 26.1, "chartPreviousClose": 25.92, "regularMarketVolume": 14200000,
                                                     "fiftyTwoWeekHigh": 28.9, "fiftyTwoWeekLow": 23.2}, "indicators": {"quote": [{"close": [25.96, 26.1]}]}}]}}
q2 = mp.quote("2222.SR", RUN)
check("quote: the same answer's volume and 52-week range are kept", q2.get("vol") == 14200000 and q2.get("yh") == 28.9 and q2.get("yl") == 23.2, q2)
q3 = mp.quote("AAPL", RUN)
check("quote: none of them when Yahoo's meta has none (a quote as before)", set(q3) == {"p", "v", "c", "t"}, q3)
del answers["2222.SR"]
# the year's daily closes (DAILY, once a day): rounded small, the first and last day, crypto none; kept between runs
day0 = int(at(2025, 11, 3, 13).timestamp())
yahoo_year = {"chart": {"result": [{"meta": {}, "timestamp": [day0 + i * 86400 for i in range(250)],
                                    "indicators": {"quote": [{"close": [9800.123 + i if i != 7 else None for i in range(250)]}]}}]}}
saved_fetch = mp.fetch_json
mp.fetch_json = lambda url: yahoo_year if "interval=1d" in url else saved_fetch(url)
y = mp.daily("^TASI.SR", RUN)
check("daily: a year of closes, oldest first, a missing day left out, whole numbers above 1000",
      len(y["d"]) == 249 and y["d"][0] == 9800 and all(isinstance(v, int) for v in y["d"]) and y["df"] == "2025-11-03" and y["dt"] == RUN.isoformat(timespec="seconds"), {k: y[k] for k in ("df", "dl", "dt")})
check("daily: 2 decimals under 1000", mp.small(26.1049) == 26.1 and mp.small(337.064) == 337.06 and mp.small(9882.85) == 9883)
check("daily: once a day (20 hours), never for crypto", mp.daily_due("2222.SR", {}, RUN) and not mp.daily_due("2222.SR", {"dt": (RUN - datetime.timedelta(hours=3)).isoformat()}, RUN)
      and mp.daily_due("2222.SR", {"dt": (RUN - datetime.timedelta(hours=21)).isoformat()}, RUN) and not mp.daily_due("BTC-USD", {}, RUN, True))
mp.fetch_json = saved_fetch
check("run: the one that failed keeps its old quote and its old time", q["NVDA"] == {"p": 100.0, "v": 99.0, "c": [99, 100], "t": "2026-10-22T10:00:00+00:00"}, q.get("NVDA"))
check("run: the 404 symbol is left out", "MSFT" not in q and "MSFT" in rep["notfound"], rep)
check("run: Tadawul symbols are closed and not asked (none had a quote yet, so they are asked once)", "2222.SR" in q, list(q)[:5])
check("run: the file says when it was confirmed", got["updated"] == RUN.isoformat(timespec="seconds"), got["updated"])
check("run: the file is small", len(json.dumps(got)) < 60000, len(json.dumps(got)))

# the year goes to daily.json, never to the 15-minute file: a run where every daily request answers, from an older file that still had a year in it
legacy = tempfile.mkdtemp()
(Path(legacy) / "markets.json").write_text(json.dumps({"updated": "2026-10-22T10:00:00+00:00", "quotes": {
    "2222.SR": {"p": 26.0, "v": 25.9, "c": [26.0], "t": "2026-10-22T10:00:00+00:00", "d": [1, 2], "df": "2025-11-03", "dl": "2025-11-04", "dt": "x"}}}))
out_d = tempfile.mkdtemp()
mp.OUT, mp.PREV = Path(out_d), Path(legacy)
year_closes = lambda n: [round(20 + i / 37 + (i % 7) / 13, 4) for i in range(n)]
def fake_year(url):
    if "interval=1d" in url:
        big = "%5ETASI" in url or "GSPC" in url
        return {"chart": {"result": [{"meta": {}, "timestamp": [day0 + i * 86400 for i in range(250)],
                                      "indicators": {"quote": [{"close": [9000 + v * 10 if big else v for v in year_closes(250)]}]}}]}}
    return {"chart": {"result": [{"meta": {"regularMarketPrice": 26.1, "chartPreviousClose": 25.92, "regularMarketVolume": 14200000, "fiftyTwoWeekHigh": 28.9,
                                           "fiftyTwoWeekLow": 23.2}, "indicators": {"quote": [{"close": year_closes(100)}]}}]}}
mp.fetch_json = fake_year
mp.main(["--all"])
mp.fetch_json = saved_fetch
quotes_text = (Path(out_d) / "markets.json").read_text()
daily_file = json.loads((Path(out_d) / "daily.json").read_text())
check("split: the 15-minute file carries no year (an older file's year is dropped), the daily file has it",
      '"d":' not in quotes_text and '"df"' not in quotes_text and len(daily_file["daily"]["2222.SR"]["d"]) == 250 and "BTC-USD" not in daily_file["daily"]
      and daily_file["updated"] == RUN.isoformat(timespec="seconds"), list(daily_file["daily"])[:3])
check(f"split: the 15-minute file every TV downloads stays small ({len(quotes_text.encode()) // 1024} kB, at most {mp.QUOTE_MAX_BYTES // 1024} kB)",
      len(quotes_text.encode()) <= mp.QUOTE_MAX_BYTES, len(quotes_text.encode()))
check(f"split: the daily file, fetched only by the markets page once a day, under 200 kB ({len(json.dumps(daily_file, separators=(',', ':'))) // 1024} kB)",
      len(json.dumps(daily_file, separators=(",", ":"))) < 200 * 1024)
# the next run, the same day: the year is not asked again and is kept
out_e = tempfile.mkdtemp()
mp.OUT, mp.PREV = Path(out_e), Path(out_d)
asked_daily = []
mp.fetch_json = lambda url: (asked_daily.append(url), fake_year(url))[1] if "interval=1d" in url else fake_year(url)
mp.main([])
mp.fetch_json = saved_fetch
kept = json.loads((Path(out_e) / "daily.json").read_text())
check("split: later the same day the year is not asked again, and is kept as it was", not asked_daily and kept["daily"] == daily_file["daily"], len(asked_daily))
mp.OUT, mp.PREV = Path(OUT), Path(PREV)

# a run in which everything fails: nothing new, the old file stays as it was
(Path(PREV) / "markets.json").write_text((Path(OUT) / "markets.json").read_text())
out2 = tempfile.mkdtemp(); mp.OUT = Path(out2)
mp.fetch_json = lambda url: (_ for _ in ()).throw(OSError("HTTP Error 429"))
gh = Path(tempfile.mkdtemp()) / "out"
os.environ["GITHUB_OUTPUT"] = str(gh)
RUN = at(2026, 10, 22, 15, 0)
mp.main(["--all"])
got2 = json.loads((Path(out2) / "markets.json").read_text())
check("blocked: it stops after five failures instead of hammering", json.loads((Path(out2) / "report.json").read_text())["stopped"] != "", )
check("blocked: the old quotes and the old 'updated' stay", got2["quotes"]["AAPL"] == got["quotes"]["AAPL"] and got2["updated"] == got["updated"])
check("blocked: nothing is pushed", "changed=false" in gh.read_text(), gh.read_text())

wf = (ROOT / ".github/workflows/market-prices.yml").read_text()
check("workflow: pushes only when something changed, never from a work branch", "steps.run.outputs.changed == 'true'" in wf and "branches: [master, next]" in wf)
check("workflow: a timer in each window and one off-hours", all(c in wf for c in ["*/15 7-11 * * 0-4", "*/15 13-20 * * 1-5", "7,37 * * * *"]))
print("\n%d failed" % failed if failed else "\nall market price tests passed")
raise SystemExit(1 if failed else 0)
