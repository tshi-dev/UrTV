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
closes, at most 30], "t": when this price was last confirmed}}} and markets/report.json. A symbol that fails this
time keeps its previous quote and its old "t" (MARKETS_PREV is a directory holding the last file), so the page sees
how old it is. Which symbols a run asks for depends on the clock (due()): the open exchanges and the 24-hour assets
every run, the closed ones about hourly.

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
PAUSE_S = float(os.environ.get("MARKETS_PAUSE", "0.4"))   # between two requests
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


def fetch_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (urtv-markets)"})
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        if e.code == 404:
            raise NotFound(url)
        raise


def quote(sym, now):
    """-> the quote of one symbol, or None when Yahoo has no price for it. Raises on a failed request."""
    j = fetch_json(CHART.format(urllib.parse.quote(sym, safe="")))
    res = ((j.get("chart") or {}).get("result") or [None])[0]
    if not res:
        raise NotFound(sym)
    meta = res.get("meta") or {}
    price = meta.get("regularMarketPrice")
    if not isinstance(price, (int, float)):
        return None
    closes = [x for x in (((res.get("indicators") or {}).get("quote") or [{}])[0].get("close") or []) if isinstance(x, (int, float))]
    if len(closes) > CLOSES_MAX:
        n = -(-len(closes) // CLOSES_MAX)
        closes = [x for i, x in enumerate(closes) if i % n == 0 or i == len(closes) - 1]
    prev = meta.get("chartPreviousClose") or meta.get("previousClose")
    return {"p": round(price, 4), "v": round(prev, 4) if prev else None, "c": [round(x, 4) for x in closes],
            "t": now.isoformat(timespec="seconds")}


def main(argv):
    everything = "--all" in argv
    now = datetime.datetime.now(datetime.timezone.utc)
    prev = {}
    if PREV and (PREV / "markets.json").exists():
        try:
            prev = json.loads((PREV / "markets.json").read_text(encoding="utf-8")).get("quotes", {})
        except ValueError:
            prev = {}
    quotes = dict(prev)
    report = {"asked": 0, "ok": 0, "failed": [], "notfound": [], "skipped": 0, "stopped": ""}
    asked = 0
    for sym, kind in symbols().items():
        if not due(sym, kind, prev, now, everything):
            report["skipped"] += 1
            continue
        if asked:
            time.sleep(PAUSE_S)
        asked += 1
        report["asked"] += 1
        try:
            q = quote(sym, now)
        except NotFound:
            report["notfound"].append(sym)
            quotes.pop(sym, None)
            continue
        except Exception as e:   # 429, a timeout, a bad answer: keep the old quote, its old "t" says how old it is
            report["failed"].append(f"{sym}: {e}")
            if len(report["failed"]) >= 5 and not report["ok"]:   # the first five all failed: blocked, do not hammer
                report["stopped"] = "five failures in a row"
                break
            continue
        if q:
            quotes[sym] = q
            report["ok"] += 1
    OUT.mkdir(parents=True, exist_ok=True)
    # "updated" moves only when something was confirmed: the page trusts a file for 3 hours from it
    updated = now.isoformat(timespec="seconds") if report["ok"] else None
    if not updated and PREV and (PREV / "markets.json").exists():
        try:
            updated = json.loads((PREV / "markets.json").read_text(encoding="utf-8")).get("updated")
        except ValueError:
            pass
    (OUT / "markets.json").write_text(json.dumps({"updated": updated, "quotes": quotes}, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    (OUT / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=1), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False))
    gh = os.environ.get("GITHUB_OUTPUT")
    if gh:
        with open(gh, "a") as f:
            f.write(f"changed={'true' if report['ok'] else 'false'}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
