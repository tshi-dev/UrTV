#!/usr/bin/env python3
"""The last price of every market and stock the screen offers, in one small JSON, for the markets line and the
stocks card. Run in CI (.github/workflows/market-prices.yml), the way league_fixtures.py is for the leagues.

Why a file and not every TV asking Yahoo: Yahoo sends no CORS headers, so a TV's page goes through public proxies that
are slow and rate-limited, and each of N TVs repeats the same requests. One run here asks once per symbol and every TV
downloads one file (about 20 kB) from raw.githubusercontent.com, branch data-markets.

The symbols are built from what the page offers (src/template.html): the MARKETS list, the TADAWUL table of the
30 largest companies (a 4-digit code is asked of Yahoo as CODE.SR) and COMMON_US below for the free-text US tickers.
A symbol that is not in the file is still asked by the page itself (direct, then the proxies).

Out: markets/markets.json = {"updated", "quotes": {SYMBOL: {"p": price, "v": the close before, "c": [the day's
closes, at most 30], "t": when this price was last confirmed, and when Yahoo's answer has them: "vol": the day's volume,
"yh"/"yl": the 52-week high and low}}} and markets/report.json. The last three cost nothing: they are in the same answer's
meta (the home's «الأسواق» page shows them; docs/PROPOSALS-NOV2.md §6), and a quote without them is as before.
Once a day, a second request per symbol (DAILY: a year of daily closes) writes markets/daily.json = {"updated", "daily":
{SYMBOL: {"d": [the closes, oldest first], "df"/"dl": the first and last day (YYYY-MM-DD), "dt": when they were fetched}}},
for the home's «الأسواق» page («أسبوع / شهر / سنة»). A file of its own: every TV's page downloads markets.json every 5 minutes
and stays about 20 kB; only a TV that opens the markets page downloads the year (about 140 kB), once a day. Crypto has none. A symbol that fails this
time keeps its previous quote and its old "t" (MARKETS_PREV is a directory holding the last file), so the page sees
how old it is. Which symbols a run asks for depends on the clock (due()): the open exchanges and the 24-hour assets
every run, the closed ones about hourly.

Requests (build 179, the owner): symbols go in batches (Yahoo's spark endpoint, BATCH per request; a symbol the batch did not answer is asked alone),
with a generic User-Agent, a pause between requests, and 2/4/8 s of backoff on a 429, a 5xx or a timeout (Retry-After honoured up to 30 s).
When no symbol answers the run publishes nothing, so the last good file stays. MARKETS_PROVIDERS="yahoo" (default) or "yahoo,stooq": the second
is asked only for what the first did not answer (Stooq's free CSV has US stocks and indices, not Tadawul; its terms are the maintainer's to
check before the switch is turned on). What is written passes an allowlist (QUOTE_KEYS, TOP_KEYS, DAILY_KEYS): nothing else of a provider's answer is published.

    python3 .github/tools/market_prices.py          what is due now
    python3 .github/tools/market_prices.py --all    everything
"""
import datetime
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from zoneinfo import ZoneInfo

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
OUT = Path(os.environ.get("MARKETS_OUT", "markets"))
PREV = Path(os.environ["MARKETS_PREV"]) if os.environ.get("MARKETS_PREV") else None
CHART = "https://query1.finance.yahoo.com/v8/finance/chart/{}?interval=15m&range=1d"
DAILY = "https://query1.finance.yahoo.com/v8/finance/chart/{}?interval=1d&range=1y"
DAILY_EVERY_H = 20   # the daily closes are asked again after this many hours: once a day
DAILY_KEYS = ("d", "df", "dl", "dt")
QUOTE_MAX_BYTES = 32 * 1024   # markets.json, downloaded by every TV every 15 minutes: the year never goes in it
PAUSE_S = float(os.environ.get("MARKETS_PAUSE", "0.4"))   # between two requests
UA = "UrTV-markets/1.0 (+https://github.com/tshi-dev/UrTV)"   # generic: a project, no person, no address
RETRY_WAITS = (2, 4, 8)   # seconds before the 2nd, 3rd and 4th try of one request
RETRY_AFTER_MAX = 30
SLEEP = time.sleep   # a test replaces it
SPARK = "https://query1.finance.yahoo.com/v8/finance/spark?symbols={}&range=1d&interval=15m"
BATCH = int(os.environ.get("MARKETS_BATCH", "20"))   # symbols per batch request; 0 = one request each
PROVIDERS = [x.strip() for x in os.environ.get("MARKETS_PROVIDERS", "yahoo").split(",") if x.strip()]   # the switch: "yahoo,stooq" turns the fallback on
STOOQ = "https://stooq.com/q/l/?s={}&f=sd2t2ohlcv&h&e=csv"
STOOQ_INDEX = {"^GSPC": "^spx", "^IXIC": "^ndq", "^DJI": "^dji"}
# the only fields that are published (a provider's answer carries many more; none of them leaves this script)
QUOTE_KEYS = ("p", "v", "c", "t", "vol", "yh", "yl")
TOP_KEYS = ("updated", "quotes", "src")
CLOSED_REFRESH_MIN = 55   # a closed exchange's last close is confirmed about hourly
CLOSES_MAX = 30

# These futures and crypto pairs are checked every run; Yahoo returns their latest available quote outside trading hours too.
ALWAYS_ON = {"GC=F", "SI=F", "BZ=F", "BTC-USD", "XRP-USD"}
CG_TO_YAHOO = {"bitcoin": "BTC-USD", "ripple": "XRP-USD"}
# the most common US tickers people type into «الأسهم»
COMMON_US = ["AAPL", "MSFT", "GOOGL", "GOOG", "AMZN", "NVDA", "META", "TSLA", "BRK-B", "AVGO", "AMD", "NFLX", "INTC", "ORCL",
             "CRM", "ADBE", "CSCO", "QCOM", "IBM", "UBER", "PYPL", "SHOP", "PLTR", "COIN", "JPM", "BAC", "WFC", "GS", "V", "MA",
             "XOM", "CVX", "JNJ", "PFE", "UNH", "LLY", "WMT", "COST", "KO", "PEP", "MCD", "SBUX", "NKE", "DIS", "BA", "F", "GM",
             "T", "VZ", "SPY", "QQQ", "VOO", "GLD"]


def symbols():
    """-> {Yahoo symbol: 'sa' | 'us' | 'always'} for everything the page offers."""
    src = (ROOT / "src" / "template.html").read_text(encoding="utf-8")
    out = {}
    block = re.search(r"const MARKETS = \{(.*?)\n\};", src, re.S).group(1)
    for m in re.finditer(r"(\w+):\s*\{[^}]*?(?:sym:'([^']+)'|cg:'(\w+)')", block):
        sym = m.group(2) or CG_TO_YAHOO[m.group(3)]
        out[sym] = "sa" if sym.endswith(".SR") else "always" if sym in ALWAYS_ON else "us"
    tad = re.search(r"const TADAWUL = \{(.*?)\n\};", src, re.S).group(1)
    for code in re.findall(r"'(\d{4})'\s*:", tad):
        out[code + ".SR"] = "sa"
    for t in COMMON_US:
        out.setdefault(t, "us")
    return out


def market_open(kind, now):
    """Tadawul Sun-Thu 10:00-15:00 Riyadh; the US Mon-Fri 09:30-16:00 New York time (holidays ignored)."""
    if kind == "always":
        return True
    if kind == "sa":
        d = now.astimezone(ZoneInfo("Asia/Riyadh"))
        return d.weekday() in (6, 0, 1, 2, 3) and 10 * 60 <= d.hour * 60 + d.minute < 15 * 60
    d = now.astimezone(ZoneInfo("America/New_York"))
    return d.weekday() < 5 and 9 * 60 + 30 <= d.hour * 60 + d.minute < 16 * 60


def due(sym, kind, prev, now, everything=False):
    if everything or kind == "always" or market_open(kind, now):
        return True
    t = (prev.get(sym) or {}).get("t")
    try:
        return not t or now - datetime.datetime.fromisoformat(t) >= datetime.timedelta(minutes=CLOSED_REFRESH_MIN)
    except ValueError:
        return True


class NotFound(Exception):
    pass


REQUESTS = [0]   # how many HTTP requests this run made (the report says)


def _get(url, read):
    """One GET with the generic User-Agent; 404 -> NotFound; a 429, a 5xx, a timeout or a bad body is tried again after RETRY_WAITS."""
    for attempt in range(len(RETRY_WAITS) + 1):
        wait = RETRY_WAITS[attempt] if attempt < len(RETRY_WAITS) else None
        REQUESTS[0] += 1
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=20) as r:
                return read(r)
        except urllib.error.HTTPError as e:
            if e.code == 404:
                raise NotFound(url)
            if not (e.code == 429 or 500 <= e.code <= 599):
                raise
            last = e
            ra = (e.headers.get("Retry-After") if e.headers else None) or ""
            if ra.isdigit():
                wait = min(RETRY_AFTER_MAX, int(ra)) if wait is not None else None
        except (urllib.error.URLError, OSError, ValueError) as e:   # a timeout, a reset, a body that is not JSON
            last = e
        if wait is None:
            raise last
        SLEEP(wait)


def fetch_json(url):
    return _get(url, json.load)


def fetch_text(url):
    return _get(url, lambda r: r.read(512 * 1024).decode("utf-8", "replace"))


def quote(sym, now):
    """-> the quote of one symbol, or None when Yahoo has no price for it. Raises on a failed request."""
    j = fetch_json(CHART.format(urllib.parse.quote(sym, safe="")))
    res = ((j.get("chart") or {}).get("result") or [None])[0]
    if not res:
        raise NotFound(sym)
    return parse_result(res, now)


def parse_result(res, now):
    """-> the quote out of one result of a chart (or spark) answer: only the fields QUOTE_KEYS name are taken, rounded; None without a price."""
    meta = res.get("meta") or {}
    price = meta.get("regularMarketPrice")
    if not isinstance(price, (int, float)):
        return None
    closes = [x for x in (((res.get("indicators") or {}).get("quote") or [{}])[0].get("close") or []) if isinstance(x, (int, float))]
    if len(closes) > CLOSES_MAX:
        n = -(-len(closes) // CLOSES_MAX)
        closes = [x for i, x in enumerate(closes) if i % n == 0 or i == len(closes) - 1]
    prev = meta.get("chartPreviousClose") or meta.get("previousClose")
    q = {"p": round(price, 4), "v": round(prev, 4) if prev else None, "c": [round(x, 4) for x in closes],
         "t": now.isoformat(timespec="seconds")}
    # the same answer's volume and 52-week range (no extra request); only real positive numbers are kept
    for key, field in (("vol", "regularMarketVolume"), ("yh", "fiftyTwoWeekHigh"), ("yl", "fiftyTwoWeekLow")):
        v = meta.get(field)
        if isinstance(v, (int, float)) and not isinstance(v, bool) and v > 0:
            q[key] = round(float(v), 4)
    return q


def batch(syms, now):
    """-> {symbol: quote} for what Yahoo's spark endpoint answers, BATCH symbols per request. A batch that fails answers nothing (the caller asks
    those alone); the spark answer is read with the same parser as a chart's."""
    out = {}
    if BATCH <= 0:
        return out
    for i in range(0, len(syms), BATCH):
        chunk = syms[i:i + BATCH]
        if i:
            SLEEP(PAUSE_S)
        try:
            j = fetch_json(SPARK.format(urllib.parse.quote(",".join(chunk), safe=",")))
            for r in ((j.get("spark") or {}).get("result") or []):
                resp = (r.get("response") or [None])[0]
                q = parse_result(resp, now) if isinstance(resp, dict) and r.get("symbol") in chunk else None
                if q:
                    out[r["symbol"]] = q
        except Exception:
            continue
    return out


def stooq_symbol(sym):
    """Stooq's name for a symbol, or None when it has no such market (Tadawul, futures, crypto)."""
    if sym in STOOQ_INDEX:
        return STOOQ_INDEX[sym]
    if sym.endswith(".SR") or sym.endswith("-USD") or "=" in sym or sym.startswith("^"):
        return None
    return sym.lower() + ".us"


def stooq_quotes(syms, prev, now):
    """-> {symbol: quote} from one request of Stooq's quote CSV for the symbols it covers (US stocks and three indices). It has no previous close:
    the one of the last good file is kept, and the price is added to the day's closes."""
    names = {stooq_symbol(x): x for x in syms if stooq_symbol(x)}
    if not names:
        return {}
    rows = fetch_text(STOOQ.format("+".join(names))).strip().splitlines()[1:]
    out = {}
    for row in rows:
        f = row.split(",")
        sym = names.get(f[0].lower()) if f else None
        try:
            price = float(f[6])
        except (IndexError, ValueError):
            continue
        if not sym or price <= 0:
            continue
        old = prev.get(sym) or {}
        closes = list(old.get("c") or [])
        closes = (closes + [round(price, 4)])[-CLOSES_MAX:]
        out[sym] = {"p": round(price, 4), "v": old.get("v"), "c": closes, "t": now.isoformat(timespec="seconds")}
    return out


def allowlist(doc):
    """What may be published of a quotes file: the keys of TOP_KEYS, and per quote only QUOTE_KEYS with numbers as numbers."""
    quotes = {}
    for sym, q in (doc.get("quotes") or {}).items():
        if not isinstance(q, dict) or not isinstance(q.get("p"), (int, float)):
            continue
        kept = {}
        for k in QUOTE_KEYS:
            v = q.get(k)
            if k == "c":
                if isinstance(v, list):
                    kept[k] = [round(float(x), 4) for x in v if isinstance(x, (int, float)) and not isinstance(x, bool)][-CLOSES_MAX:]
            elif k == "t":
                if isinstance(v, str):
                    kept[k] = v
            elif isinstance(v, (int, float)) and not isinstance(v, bool):
                kept[k] = round(float(v), 4) if k != "p" else round(v, 4)
            elif k == "v" and v is None:
                kept[k] = None
        quotes[sym] = kept
    out = {k: doc[k] for k in TOP_KEYS if k in doc and k != "quotes"}
    out["quotes"] = quotes
    return out


def small(x):
    """a close as kept in the year's line: 2 decimals under 1000, whole above (a chart needs no more, and the file stays small)"""
    return round(x) if x >= 1000 else round(x, 2)


def daily(sym, now):
    """-> {"d": [a year of daily closes], "df", "dl", "dt"} or None. Raises on a failed request."""
    j = fetch_json(DAILY.format(urllib.parse.quote(sym, safe="")))
    res = ((j.get("chart") or {}).get("result") or [None])[0]
    if not res:
        raise NotFound(sym)
    stamps = res.get("timestamp") or []
    closes = (((res.get("indicators") or {}).get("quote") or [{}])[0].get("close") or [])
    pairs = [(t, c) for t, c in zip(stamps, closes) if isinstance(c, (int, float)) and not isinstance(c, bool) and c > 0 and isinstance(t, int)]
    if len(pairs) < 2:
        return None
    day = lambda t: datetime.datetime.fromtimestamp(t, datetime.timezone.utc).date().isoformat()
    return {"d": [small(c) for _, c in pairs], "df": day(pairs[0][0]), "dl": day(pairs[-1][0]), "dt": now.isoformat(timespec="seconds")}


def daily_due(sym, q, now, everything=False):
    if sym.endswith("-USD"):   # crypto: no year's line on the page
        return False
    if everything:
        return True
    t = (q or {}).get("dt")
    try:
        return not t or now - datetime.datetime.fromisoformat(t) >= datetime.timedelta(hours=DAILY_EVERY_H)
    except ValueError:
        return True


def main(argv):
    everything = "--all" in argv
    now = datetime.datetime.now(datetime.timezone.utc)
    prev = {}
    if PREV and (PREV / "markets.json").exists():
        try:
            prev = json.loads((PREV / "markets.json").read_text(encoding="utf-8")).get("quotes", {})
        except ValueError:
            prev = {}
    quotes = {sym: {k: v for k, v in q.items() if k not in DAILY_KEYS} for sym, q in prev.items()}   # an older file may still carry a year
    days, days_updated = {}, None
    if PREV and (PREV / "daily.json").exists():
        try:
            old = json.loads((PREV / "daily.json").read_text(encoding="utf-8"))
            days, days_updated = old.get("daily", {}), old.get("updated")
        except ValueError:
            days = {}
    report = {"asked": 0, "ok": 0, "failed": [], "notfound": [], "skipped": 0, "stopped": ""}
    todo = []
    for sym, kind in symbols().items():
        if due(sym, kind, prev, now, everything):
            todo.append(sym)
        else:
            report["skipped"] += 1
    report["asked"] = len(todo)
    got, notfound, used = {}, set(), []
    for name in PROVIDERS:
        remaining = [x for x in todo if x not in got and x not in notfound]
        if not remaining:
            break
        if name == "yahoo":
            if report["stopped"]:
                continue
            found = batch(remaining, now)   # a few requests for most of them
            asked = 0
            for sym in remaining:
                if sym in found:
                    continue
                if asked:
                    SLEEP(PAUSE_S)
                asked += 1
                try:
                    q = quote(sym, now)
                except NotFound:
                    notfound.add(sym)
                    continue
                except Exception as e:   # 429 after its tries, a timeout, a bad answer: keep the old quote, its old "t" says how old it is
                    report["failed"].append(f"{sym}: {e}")
                    if len(report["failed"]) >= 5 and not report["ok"] and not found:   # the first five all failed: blocked, do not hammer
                        report["stopped"] = "five failures in a row"
                        break
                    continue
                if q:
                    found[sym] = q
        elif name == "stooq":
            try:
                found = stooq_quotes(remaining, prev, now)
            except Exception as e:
                report["failed"].append(f"stooq: {e}")
                found = {}
        else:
            continue
        if found:
            got.update(found)
            used.append(name)
    report["providers"] = used
    report["ok"] = len(got)
    quotes.update(got)
    # a symbol Yahoo says it does not have is dropped, but only in a run that mostly worked (an outage must not empty the file)
    if len(notfound) * 2 <= max(1, len(todo)) and report["ok"]:
        for sym in notfound:
            quotes.pop(sym, None)
    report["notfound"] = sorted(notfound)
    # the year's daily closes, once a day per symbol (a second request each); a failure keeps the old line
    report.update({"daily_asked": 0, "daily_ok": 0, "daily_failed": 0})
    if not report["stopped"]:
        for sym in list(quotes):
            if not daily_due(sym, days.get(sym), now, everything):
                continue
            SLEEP(PAUSE_S)
            report["daily_asked"] += 1
            try:
                d = daily(sym, now)
            except Exception:   # NotFound included: the quote stays, only without its year's line
                report["daily_failed"] += 1
                if report["daily_failed"] >= 5 and not report["daily_ok"]:
                    break
                continue
            if d:
                days[sym] = d
                report["daily_ok"] += 1
    days = {sym: d for sym, d in days.items() if sym in quotes}   # a symbol that left the list leaves the year too
    OUT.mkdir(parents=True, exist_ok=True)
    report["requests"] = REQUESTS[0]
    # nothing answered (Yahoo down, blocked, or a run with nothing due): nothing is published, the last good file stays as it is
    if report["ok"] or report["daily_ok"]:
        # "updated" moves only when something was confirmed: the page trusts a file for 3 hours from it
        updated = now.isoformat(timespec="seconds") if report["ok"] else None
        if not updated and PREV and (PREV / "markets.json").exists():
            try:
                updated = json.loads((PREV / "markets.json").read_text(encoding="utf-8")).get("updated")
            except ValueError:
                pass
        src = "yahoo+stooq" if len(used) > 1 else (used[0] if used else None)
        if src is None and PREV and (PREV / "markets.json").exists():
            try:
                src = json.loads((PREV / "markets.json").read_text(encoding="utf-8")).get("src")
            except ValueError:
                pass
        doc = allowlist({"updated": updated, "quotes": quotes, "src": src or "yahoo"})
        (OUT / "markets.json").write_text(json.dumps(doc, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    if report["daily_ok"] or days:
        (OUT / "daily.json").write_text(json.dumps({"updated": now.isoformat(timespec="seconds") if report["daily_ok"] else days_updated, "daily": days},
                                                   ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    (OUT / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=1), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False))
    gh = os.environ.get("GITHUB_OUTPUT")
    if gh:
        with open(gh, "a") as f:
            f.write(f"changed={'true' if report['ok'] or report['daily_ok'] else 'false'}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
