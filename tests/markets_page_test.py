#!/usr/bin/env python3
"""«الأسواق» (docs/PROPOSALS-NOV2.md §6, §7): the home's markets page. The data and the sums on a JVM (tests/MarketsRun.java), then the source:
only the data we have (CI's file, the pegs, Tadawul's hours), no card for a source we do not have, «الأخبار» off, the ways in (the home's
stocks card, the settings), and the lists that must agree with the page and the screensaver. No TV needed.
Run: python3 tests/markets_page_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


page = text(ROOT / "src/template.html")
data = text(HOME / "MarketData.java")
act = text(HOME / "MarketsActivity.java")
prefs = text(APP / "Prefs.java")

# ---- the lists that must agree
tad_page = re.findall(r"'(\d{4})':'([^']+)'", re.search(r"const TADAWUL = \{(.*?)\n\};", page, re.S).group(1))
tad_java = re.findall(r'\{"(\d{4})", "([^"]+)"\}', data[data.index("TADAWUL = {"):data.index("};", data.index("TADAWUL = {"))])
check("the 30 companies and their names are the page's TADAWUL, in its order", tad_page == tad_java and len(tad_java) == 30, (len(tad_page), len(tad_java)))
check("the file is the page's MARKET_URL", re.search(r"const MARKET_URL = '([^']+)'", page).group(1) in data)
check("the default watchlist is the screensaver's", re.search(r'DEFAULT_STOCKS = "([^"]+)"', prefs).group(1) == re.search(r'DEFAULT_STOCKS = "([^"]+)"', act).group(1))
bar = re.findall(r'\{"[^"]+", "(\w+)"\}', act[act.index("BAR_KEYS = {"):act.index("};", act.index("BAR_KEYS = {"))])
offered = re.findall(r'\{"(\w+)", "', prefs[prefs.index("String[][] MARKETS = {"):prefs.index("};", prefs.index("String[][] MARKETS = {"))])
check("the markets bar's keys the stock card offers are the screensaver's", bar and set(bar) <= set(offered), (bar, offered))
pegs = re.findall(r'\{"([^"]+)", "[\d.]+"\}', data[data.index("PEGS = {"):data.index("};", data.index("PEGS = {"))])
names = re.findall(r'S\.n\("([^"]+)"\)', act[act.index("PEG_NAMES = {"):act.index("};", act.index("PEG_NAMES = {"))])
check("the pegs' names on the screen are MarketData.PEGS', in order", pegs == names and len(pegs) == 4, (pegs, names))

# ---- only the data we have
check("«الأخبار» is off until a licensed source exists (NEWS = false), and no tab is built for it then",
      "static final boolean NEWS = false;" in act and 'NEWS ? new String[] {S.t("السوق"), S.t("قائمتي"), S.t("محفظتي"), S.t("الأخبار")}' in act)
urls = set(re.findall(r'https?://[^"\s)]+', act + data + text(HOME / "MarketFile.java")))
check("one source only: CI's two files, the 15-minute one and the daily one (no other address in the page's code)",
      urls == {"https://raw.githubusercontent.com/tshi-dev/UrTV/data-markets/markets/markets.json",
               "https://raw.githubusercontent.com/tshi-dev/UrTV/data-markets/markets/daily.json"}, urls)
check("the daily file is asked only by the markets page, at most once a day; the screensaver's page never asks it",
      "MarketFile.loadDaily(app);" in act and "DAY_OF_YEAR" in text(HOME / "MarketFile.java") and "daily.json" not in page
      and sum("loadDaily(" in text(p) for p in HOME.rglob("*.java")) == 2)
check("no card for a source we do not have: sectors, whole-market turnover, dividends, announcements, news",
      not re.search(r"القطاعات|توزيع|إعلانات الشركة|السيولة|قيمة التداول", act), re.findall(r"القطاعات|توزيع|إعلانات الشركة", act))
check("«اتجاه السوق» says it counts the 30 we fetch, never the whole market", 'S.t("أكبر %s شركة", digits(MarketData.TADAWUL.length))' in act and "breadth()" in act)
check("«الأكثر نشاطًا», the volume and the 52-week range only when CI kept them from the same answer",
      "if (!active.isEmpty())" in act and "!Double.isNaN(q.yl) && !Double.isNaN(q.yh)" in act and "!Double.isNaN(q.vol) && q.vol > 0" in act)
mp = text(ROOT / ".github/tools/market_prices.py")
check("CI keeps volume and the 52-week range from the same answer (no new request)",
      '("vol", "regularMarketVolume"), ("yh", "fiftyTwoWeekHigh"), ("yl", "fiftyTwoWeekLow")' in mp and mp.count("CHART = ") == 1 and "range=1d" in mp)
check("«أسبوع / شهر / سنة»: from the year's daily closes CI asks once a day (DAILY, interval 1d), only when the file has them; «اليوم» always",
      'DAILY = "https://query1.finance.yahoo.com/v8/finance/chart/{}?interval=1d&range=1y"' in mp and "DAILY_EVERY_H = 20" in mp
      and "if (MarketData.hasRanges(q)) {" in act and 'S.t("اليوم"), S.t("أسبوع"), S.t("شهر"), S.t("سنة")' in act)
check("the download: one file, kept in the app's files, size-capped, at most every 5 minutes",
      "FRESH_MS = 5 * 60_000L" in text(HOME / "MarketFile.java") and "MAX_BYTES" in text(HOME / "MarketFile.java"))

# ---- the ways in
man = text(ROOT / "android/launcher/src/main/AndroidManifest.xml")
check("the activity is in the manifest", 'android:name=".MarketsActivity"' in man)
card = text(HOME / "HomeCard.java")
check("the home's stocks card is a button: its own target, the page's ring, OK opens «الأسواق»",
      "if (stocks) addMarket();" in card and "setLiveFocus(\" + has + \",'stocks')" in card and "market.setOnClickListener(v -> live.markets())" in card
      and "launch(new Intent(MainActivity.this, MarketsActivity.class))" in text(HOME / "MainActivity.java"))
check("the page: the stocks card gets the ring and no mic mark; with no kind, both calls still mean «المهام»",
      "c.kind === 'stocks' ? c.el.classList.add('tap')" in page and "function setLiveFocus(on, kind = 'tasks')" in page
      and "function liveRect(kind = 'tasks')" in page and "html.widget .week.tap.lf{" in page)
st = text(APP / "SettingsActivity.java")
check("the settings: «صفحة الأسواق» in «المباريات والأسواق» opens it", 'row("صفحة الأسواق"' in st and '"sa.urtv.home.MarketsActivity"' in st
      and '{"صفحة الأسواق", CONTENT}' in text(APP / "SettingsHub.java"))
cp = text(APP / "ConfigProvider.java")
check("the stock card's buttons change the watchlist and the bar through the screensaver's own checks (phone_set -> SaverReceiver.set)",
      'new String[] {"stocks", "markets"}' in cp and 'getContentResolver().call(SAVER, "phone_set", null, e)' in act)
check("the phone API cannot send stocks or markets (Schema's set has neither)", '"stocks"' not in text(HOME / "phone/Schema.java") and '"markets"' not in text(HOME / "phone/Schema.java"))

# ---- the design rules
check("the look's tokens, no colours of its own but the allocation's and the greens of OnOff",
      "GlassSource.page(this, root)" in act and "OnOff.GREEN_ON_DARK" in act and "look.warn" in act and not re.search(r"0x[0-9A-F]{8}", act.replace(act[act.index("ALLOC = {"):act.index("};", act.index("ALLOC = {"))], "")
                                                                                                                         .replace("0xFF166A45", "").replace("0xFFFFFFFF", "").replace("0xFF000000", "")))
check("no pills: buttons and rows are GlassPaint tiles and rows", "Pill" not in act and "GlassPaint.card(b, look, 14)" in act and "GlassPaint.row(this, look, radius)" in act)
check("toggle style I for «اقفل محفظتي» (OnOff: the state word first, the three green marks at the start)", "R.drawable.ic_on" in act and "OnOff.wordEnd(value)" in act)
check("every word through S.t / S.tv (English in i18n/en/home.tsv; tests/i18n_test.py checks each one)", "attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }" in act)
pads = [int(x) for x in re.findall(r"setPadding\(dp\((\d+)\)", act)]
check("text never crowds a box: 16 dp or more inside every padded box (at 1080p that is 32 px)", pads and min(pads) >= 16, sorted(set(pads)))

# ---- the logic on a JVM
if not shutil.which("javac"):
    print("SKIP markets logic: no javac here")
else:
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "MarketData.java"), str(HOME / "Holdings.java"), str(HOME / "DigitsText.java"), str(ROOT / "tests/MarketsRun.java")], check=True)
        run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.MarketsRun"], capture_output=True)
    out = run.stdout.decode("utf-8")
    print(out, end="")
    failed += run.returncode != 0
sys.exit(1 if failed else 0)
