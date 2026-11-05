#!/usr/bin/env python3
"""The CI price collector (.github/tools/market_prices.py) with Yahoo mocked: the symbols are what the page offers, which
are due at which hour (Riyadh, New York in summer and winter time), a failed symbol keeps its old quote and its old time,
a 404 symbol is left out, a run that got nothing changes nothing, and the workflow only pushes when something changed.
Run: python3 tests/market_prices_test.py"""
import datetime
import urllib.parse
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
real_fetch_json, real_fetch_text = mp.fetch_json, mp.fetch_text
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
check("blocked: it stops after five failures instead of hammering", json.loads((Path(out2) / "report.json").read_text())["stopped"] != "", )
check("blocked: nothing is written, so the last good file stays as it was (never an empty one)", not (Path(out2) / "markets.json").exists())
check("blocked: nothing is pushed", "changed=false" in gh.read_text(), gh.read_text())

# ---- build 179: retries and backoff, one batch request, the allowlist, the fallback provider and its switch
import io, urllib.error
mp.fetch_json = real_fetch_json
real_get_sleep = mp.SLEEP
slept = []
mp.SLEEP = slept.append
class Resp(io.BytesIO):
    def __enter__(self): return self
    def __exit__(self, *a): return False
def http(code, retry_after=None):
    h = {"Retry-After": retry_after} if retry_after else {}
    return urllib.error.HTTPError("u", code, "x", h, None)
def scripted(*steps):
    it = iter(steps)
    def urlopen(req, timeout=0):
        step = next(it)
        if isinstance(step, Exception):
            raise step
        return Resp(step)
    return urlopen
real_urlopen = mp.urllib.request.urlopen
mp.urllib.request.urlopen = scripted(http(429), http(503), b'{"ok":1}')
check("retry: a 429 then a 503 are tried again after 2 s and 4 s, then it answers", mp.fetch_json("https://x/y") == {"ok": 1} and slept == [2, 4], slept)
slept.clear(); mp.urllib.request.urlopen = scripted(http(500), http(500), http(500), http(500))
try:
    mp.fetch_json("https://x/y"); err = None
except Exception as e:
    err = e
check("retry: four tries in all (waits 2, 4, 8 s), then the error is raised", isinstance(err, urllib.error.HTTPError) and slept == [2, 4, 8], (err, slept))
slept.clear(); mp.urllib.request.urlopen = scripted(http(429, "5"), b"{}")
mp.fetch_json("https://x/y")
check("retry: Retry-After is honoured (up to 30 s)", slept == [5], slept)
slept.clear(); mp.urllib.request.urlopen = scripted(http(404))
try:
    mp.fetch_json("https://x/y"); nf = False
except mp.NotFound:
    nf = True
check("retry: a 404 is not tried again", nf and slept == [])
slept.clear(); mp.urllib.request.urlopen = scripted(OSError("timed out"), b"[1]")
check("retry: a timeout is tried again", mp.fetch_json("https://x/y") == [1] and slept == [2], slept)
mp.urllib.request.urlopen = real_urlopen
check("requests: a generic User-Agent (a project, no person, no address)", "@" not in mp.UA and "UrTV-markets/" in mp.UA and mp.UA.startswith("Mozilla/5.0 (compatible;"), mp.UA)
mp.SLEEP = real_get_sleep

# one batch request holds many symbols; a symbol the batch did not answer is asked alone
calls = []
def spark_fake(url):
    calls.append(url)
    if "/spark?" in url:
        names = urllib.parse.unquote(re.search(r"symbols=([^&]+)", url).group(1)).split(",")
        return {"spark": {"result": [{"symbol": n, "response": [{"meta": {"regularMarketPrice": 10.0, "chartPreviousClose": 9.0}, "indicators": {"quote": [{"close": [9.5, 10.0]}]}}]}
                                     for n in names if n != "NVDA"]}}
    return chart(77.0, 70.0, [70, 77])
import urllib.parse
mp.fetch_json = spark_fake
b = mp.batch(["AAPL", "MSFT", "NVDA"], RUN)
check("batch: three symbols in one request, a symbol the answer lacks is simply missing", len(calls) == 1 and set(b) == {"AAPL", "MSFT"} and b["AAPL"]["p"] == 10.0, (calls, b))
calls.clear(); out_b = tempfile.mkdtemp(); mp.OUT, mp.PREV = Path(out_b), Path(tempfile.mkdtemp())
mp.main(["--all"])
rb = json.loads((Path(out_b) / "report.json").read_text())
n_batches = sum("/spark?" in c for c in calls); n_single = sum("/chart/" in c and "interval=15m" in c for c in calls)
check("batch: the whole list is asked in a handful of batch requests (<= symbols / 20 + 1), and the singles are only the symbols a batch lacked",
      n_batches <= len(mp.symbols()) // mp.BATCH + 1 + (len(mp.FX_CODES) // mp.BATCH + 1) and n_single <= 1, (n_batches, n_single, rb["asked"]))

# the allowlist: nothing of a provider's answer but the fields the page uses
dirty = {"updated": "u", "quotes": {"AAA": {"p": 1.23456789, "v": 1.2, "c": [1, 2, "x", None], "t": "t", "vol": 5, "yh": 3, "yl": 1,
                                             "regularMarketTime": 1, "exchangeName": "SAU", "longName": "A B", "currency": "SAR", "extra": {"a": 1}},
                                    "BAD": {"p": "nope"}}, "meta": {"user": "x"}, "src": "yahoo"}
clean = mp.allowlist(dirty)
check("allowlist: only p v c t vol yh yl per quote, numbers rounded, a quote with no price dropped, no other top-level key",
      set(clean["quotes"]["AAA"]) <= set(mp.QUOTE_KEYS) and clean["quotes"]["AAA"]["p"] == 1.2346 and clean["quotes"]["AAA"]["c"] == [1.0, 2.0] and "BAD" not in clean["quotes"]
      and set(clean) <= set(mp.TOP_KEYS), clean)
written = json.loads((Path(out_b) / "markets.json").read_text())
allowed = set(mp.QUOTE_KEYS)
check("allowlist: the published file of a run has nothing else (and the year stays out of it)", all(set(q) <= allowed for q in written["quotes"].values()) and set(written) <= set(mp.TOP_KEYS), set(written))

# the fallback provider and its switch
def yahoo_down(url):
    raise OSError("HTTP Error 503")
stooq_csv = "Symbol,Date,Time,Open,High,Low,Close,Volume\nAAPL.US,2026-10-22,21:00:00,1,2,0.5,150.25,100\nMSFT.US,N/D,N/D,N/D,N/D,N/D,N/D,N/D\n"
asked_text = []
mp.fetch_text = lambda url: (asked_text.append(url), stooq_csv)[1]
mp.fetch_json = yahoo_down
prevd = tempfile.mkdtemp()
(Path(prevd) / "markets.json").write_text(json.dumps({"updated": "2026-10-22T10:00:00+00:00", "quotes": {
    "AAPL": {"p": 140.0, "v": 139.0, "c": [139, 140], "t": "2026-10-22T10:00:00+00:00"}}}))
mp.PROVIDERS = ["yahoo"]; mp.PROVIDER_ARGS = None
out_s = tempfile.mkdtemp(); mp.OUT, mp.PREV = Path(out_s), Path(prevd)
mp.main(["--all"])
check("switch off (the default): Stooq is never asked, and with Yahoo down nothing is written", not asked_text and not (Path(out_s) / "markets.json").exists(), asked_text)
mp.PROVIDERS = ["yahoo", "stooq"]
out_s2 = tempfile.mkdtemp(); mp.OUT = Path(out_s2)
mp.main(["--all"])
fb = json.loads((Path(out_s2) / "markets.json").read_text())
check("switch on: with Yahoo down, Stooq covers the US stocks it has (one request), keeps the old previous close, and says so (src)",
      len(asked_text) == 1 and fb["quotes"]["AAPL"]["p"] == 150.25 and fb["quotes"]["AAPL"]["v"] == 139.0 and fb["src"] == "stooq", (asked_text, fb["quotes"].get("AAPL"), fb.get("src")))
check("switch on: Tadawul, futures and crypto are not Stooq's: they are not asked of it, and keep no new quote", "2222.SR" not in fb["quotes"] and "BTC-USD" not in fb["quotes"]
      and "2222.sr" not in asked_text[0].lower() and "btc" not in asked_text[0].lower() and "gc=f" not in asked_text[0].lower())
check("switch on: a row Stooq has no data for (N/D) is skipped", "MSFT" not in fb["quotes"])
mp.fetch_json = spark_fake; asked_text.clear()
out_s3 = tempfile.mkdtemp(); mp.OUT, mp.PREV = Path(out_s3), Path(tempfile.mkdtemp())
mp.main(["--all"])
fb3 = json.loads((Path(out_s3) / "markets.json").read_text())
check("switch on, Yahoo up: Stooq is not asked at all (only what Yahoo did not answer)", not asked_text and fb3["src"] == "yahoo", (asked_text, fb3.get("src")))
mp.PROVIDERS = ["yahoo"]

# an outage must not empty the file: every symbol 404s -> nothing dropped, nothing written
mp.fetch_json = lambda url: (_ for _ in ()).throw(mp.NotFound(url))
out_n = tempfile.mkdtemp(); mp.OUT, mp.PREV = Path(out_n), Path(prevd)
mp.main(["--all"])
check("outage: a run where every symbol comes back 'not found' publishes nothing", not (Path(out_n) / "markets.json").exists())

# ---- «صرف العملات»: the currencies file
fxdoc = mp.allowlist_fx({"updated": "2026-11-03T08:00:00+00:00", "rates": {"KWD": 0.30712345678, "EGP": 48.5, "XXX": 5, "EUR": -1, "GBP": "x", "SAR": 3.75}, "debug": {"a": 1}})
check("fx: the file keeps only known codes with a positive number (6 significant digits), no peg (SAR) and no other key", fxdoc == {"updated": "2026-11-03T08:00:00+00:00", "rates": {"KWD": 0.307123, "EGP": 48.5}}, fxdoc)
check("fx: the pegged currencies are never fetched (SAR AED QAR BHD OMR JOD), the others are: USD per code, as 'CCY=X'", not {"SAR", "AED", "QAR", "BHD", "OMR", "JOD", "USD"} & set(mp.FX_CODES)
      and {"KWD", "EGP", "EUR", "GBP", "TRY", "INR", "PKR", "IQD", "MAD", "DZD", "TND", "LYD", "SDG", "YER", "LBP", "SYP", "MRU", "PHP", "IDR", "BDT", "LKR", "NPR", "MYR", "ETB", "KES",
              "JPY", "CNY", "CHF", "CAD", "AUD", "SGD", "HKD", "KRW", "RUB", "ZAR"} <= set(mp.FX_CODES))
asked_fx = []
def fx_fake(url):
    asked_fx.append(url)
    names = urllib.parse.unquote(re.search(r"symbols=([^&]+)", url).group(1)).split(",") if "/spark?" in url else [urllib.parse.unquote(re.search(r"chart/([^?]+)\?", url).group(1))]
    res = [{"symbol": n, "response": [{"meta": {"regularMarketPrice": {"KWD=X": 0.307, "SYP=X": 13000.0}.get(n, 2.5)}, "indicators": {"quote": [{"close": [1.0]}]}}]} for n in names if n != "SDG=X"]
    return {"spark": {"result": res}} if "/spark?" in url else {"chart": {"result": [r["response"][0] for r in res]}} if res else {"chart": {"result": None}}
mp.fetch_json = fx_fake
rates = mp.fx_rates(RUN)
check("fx: one or two batch requests for every code; a code Yahoo has no price for (SDG here) is left out, with no error", rates["KWD"] == 0.307 and "SDG" not in rates and len(rates) == len(mp.FX_CODES) - 1
      and sum("/spark?" in u for u in asked_fx) == len(mp.FX_CODES) // mp.BATCH + 1, (len(rates), asked_fx[:2]))
check("fx: asked once a day (20 hours), the first time at once", mp.fx_due({}, RUN) and not mp.fx_due({"updated": (RUN - datetime.timedelta(hours=3)).isoformat()}, RUN) and mp.fx_due({"updated": (RUN - datetime.timedelta(hours=21)).isoformat()}, RUN))
out_f = tempfile.mkdtemp(); prev_f = tempfile.mkdtemp()
(Path(prev_f) / "fx.json").write_text(json.dumps({"updated": "2026-11-02T08:00:00+00:00", "rates": {"SDG": 600.0, "KWD": 0.3}}))
mp.OUT, mp.PREV = Path(out_f), Path(prev_f)
mp.fetch_json = fx_fake
mp.main(["--all"])
fxw = json.loads((Path(out_f) / "fx.json").read_text())
check("fx: a run writes markets/fx.json; a code that fails today keeps its old rate, and the file is small", fxw["rates"]["SDG"] == 600.0 and fxw["rates"]["KWD"] == 0.307 and set(fxw) == {"updated", "rates"} and len((Path(out_f) / "fx.json").read_bytes()) < 2000)
# a run where the currencies are not due (3 hours since the last) still publishes the last file: the branch is rebuilt from the folder each time
out_g = tempfile.mkdtemp(); prev_g = tempfile.mkdtemp()
(Path(prev_g) / "fx.json").write_text(json.dumps({"updated": mp.datetime.datetime.now(mp.datetime.timezone.utc).isoformat(timespec="seconds"), "rates": {"KWD": 0.3, "EGP": 48.5}}))
mp.OUT, mp.PREV = Path(out_g), Path(prev_g)
asked_fx.clear()
mp.main([])   # no --all: nothing is forced
check("fx: a run where the currencies are not due writes the last fx.json again (the one-commit branch would lose it), and asks Yahoo for no currency",
      (Path(out_g) / "fx.json").exists() and json.loads((Path(out_g) / "fx.json").read_text())["rates"] == {"KWD": 0.3, "EGP": 48.5} and not any("%3DX" in u or "=X" in u for u in asked_fx), asked_fx[:2])
check("fx: the five-minute file does not carry the currencies (it stays under its 32 kB)", "KWD" not in (Path(out_f) / "markets.json").read_text() and len((Path(out_f) / "markets.json").read_bytes()) <= mp.QUOTE_MAX_BYTES)
mp.PREV = Path(PREV)

wf = (ROOT / ".github/workflows/market-prices.yml").read_text()
loop = (ROOT / ".github/tools/market_loop.sh").read_text()
check("workflow: pushes only when something changed, never from a work branch", "grep -q '^changed=true$'" in loop and "branches: [master, next]" in wf)
check("workflow: a timer at :00, :20 or :40 in the open hours starts a loop (timers get dropped), off-hours every 30 minutes",
      all(c in wf for c in ["0,20,40 7-11 * * 0-4", "0,20,40 13-20 * * 1-5", "7,37 * * * *"]) and "*/5" not in wf and "bash .github/tools/market_loop.sh" in wf, wf[:900])
check("workflow: the loop fetches every 5 minutes until two minutes before the next full hour, inside the open hours only, and the job's limit covers it",
      'STEP="${LOOP_STEP:-300}"' in loop and "3600 - 120" in loop and "in_hours || break" in loop and "timeout-minutes: 65" in wf)
check("workflow: one force-pushed parentless commit, no history of snapshots, the working tree untouched so the loop can push again", "git commit-tree" in loop and 'git push -f origin "$commit:refs/heads/data-markets"' in loop and "--orphan" not in loop)
check("workflow: the fallback switch is visible and off", "MARKETS_PROVIDERS: yahoo" in wf and "stooq" in wf)
print("\n%d failed" % failed if failed else "\nall market price tests passed")
raise SystemExit(1 if failed else 0)
