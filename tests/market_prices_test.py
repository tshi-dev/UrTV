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
check("run: the one that failed keeps its old quote and its old time", q["NVDA"] == {"p": 100.0, "v": 99.0, "c": [99, 100], "t": "2026-10-22T10:00:00+00:00"}, q.get("NVDA"))
check("run: the 404 symbol is left out", "MSFT" not in q and "MSFT" in rep["notfound"], rep)
check("run: Tadawul symbols are closed and not asked (none had a quote yet, so they are asked once)", "2222.SR" in q, list(q)[:5])
check("run: the file says when it was confirmed", got["updated"] == RUN.isoformat(timespec="seconds"), got["updated"])
check("run: the file is small", len(json.dumps(got)) < 60000, len(json.dumps(got)))

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
