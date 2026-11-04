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
      "static final boolean NEWS = false;" in act and 'NEWS ? new String[] {S.t("السوق"), S.t("قائمتي"), S.t("محفظتي"), S.t("الأخبار")}' in act)   # «صرف العملات» is a button of the controls row now, not a tab
urls = set(re.findall(r'https?://[^"\s)]+', act + data + text(HOME / "MarketFile.java")))
check("two sources only: CI's two files (the 5-minute one and the daily one) and, while the page is in front, Yahoo's quote endpoint for live mode (no other address in the page's code)",
      urls - {"https://github.com/tshi-dev/UrTV"} == {"https://raw.githubusercontent.com/tshi-dev/UrTV/data-markets/markets/markets.json",
               "https://raw.githubusercontent.com/tshi-dev/UrTV/data-markets/markets/daily.json",
               "https://raw.githubusercontent.com/tshi-dev/UrTV/data-markets/markets/fx.json",
               "https://query1.finance.yahoo.com/v8/finance/spark?symbols=%s&range=1d&interval=5m",
               }, urls)
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
check("text never crowds a box: 12 dp or more (24 px at 1080p; the design rule is 16 px) inside every padded box", pads and min(pads) >= 12, sorted(set(pads)))

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
act2 = text(HOME / "MarketsActivity.java")
check("live mode: a switch «مباشر | عادي» on the page, «مباشر» by default", 'S.t("مباشر"), S.t("عادي")' in act2 and "private boolean liveOn = true" in act2 and "controls.addView(liveSwitch()" in act2)
check("live mode: asks only while resumed (starts in onResume, ends in onPause and with the generation), dropped when paused or switched off meanwhile",
      "resumed = true;" in act2 and "resumed = false;   // live mode ends" in act2 and "if (liveOn) main.postDelayed(liveTick, 1500);" in act2
      and "if (run != liveRun || !resumed || !liveOn || isFinishing() || isDestroyed()) return;" in act2 and "main.removeCallbacks(liveTick);" in act2.split("protected void onPause()")[1][:400])
check("live mode: one batched request for the symbols on screen and the open card; the pace and the backoff come from MarketData.liveDelay; nothing is stored",
      "MarketFile.live(syms)" in act2 and "main.postDelayed(liveTick, MarketData.liveDelay(liveFails));" in act2 and "liveFails++" in act2 and "liveFails = 0;" in act2
      and "save(" not in text(HOME / "MarketFile.java").split("static java.util.Map<String, MarketData.Live> live")[1].split("static MarketData cached")[0])
check("live mode: it falls back to the 5-minute file: «عادي» puts the file's prices back (MarketFile.last), a failed ask leaves the data and the footer returns to «الأسعار متأخرة»",
      "MarketData file = MarketFile.last();" in act2 and "shownLive = liveOn && MarketData.liveFresh(liveOkAt, System.currentTimeMillis());" in act2
      and 'S.t("الأسعار متأخرة · المصدر: %s", name)' in act2 and 'S.t("مباشر · المصدر: %s", name)' in act2)
check("live mode: labels: Tadawul «متأخرة ~١٥ د», any other the quote's time; the footer says «مباشر» while live", 'S.t("متأخرة ~%s د", digits(15))' in act2 and "MarketData.liveKind(code)" in act2
      and "clock(k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE))" in act2)
check("step 27: live mode keeps the screen awake while the page is in front (FLAG_KEEP_SCREEN_ON set when live and resumed, cleared otherwise)", "FLAG_KEEP_SCREEN_ON" in act2 and "if (liveOn && resumed) getWindow().addFlags" in act2
      and "keepAwake();" in act2.split("protected void onPause()")[1][:300] and "keepAwake();" in act2.split("protected void onResume()")[1][:300])
check("step 27: «اختر العملات»: «تم» is not squeezed (list 300 dp), the chosen ones carry the state word and the on mark (style I), and the focus returns to «اختر العملات»",
      "sized(-1, 300, 12)" in act2.split("private void chooseCurrencies()")[1].split("private void pickCurrency")[0] and 'S.t("مفعّلة") : S.t("متوقفة")' in act2
      and "actions.getChildAt(1)" in act2.split("private void chooseCurrencies()")[1].split("private void pickCurrency")[0] and "again[0] = true;" in act2)
check("step 27: after «احفظ» the focus goes to the new holding's row (tagged with its code), else «أضف سهمًا»; «احفظ» names what is missing in the dialog", "row.setTag(code);" in act2 and "scroll.post(() -> focusHolding(code));" in act2
      and "Holdings.missing(" in act2 and 'S.t("اكتب متوسط التكلفة")' in act2 and 'S.t("اكتب الكمية")' in act2)
check("step 27: the page closes its dialogs when it pauses (no WindowLeaked) and recreates itself when the language changes", "closeDialogs();" in act2.split("protected void onPause()")[1][:400] and "recreate();" in act2 and "S.lang()" in act2)
check("step 27: the gold card's line keeps 16 dp from the price on both sides", "lp.setMarginEnd(dp(16));" in act2 and "pxl.setMarginStart(dp(16));" in act2)
check("live mode: only this page: the screensaver's page and the home's card never call the live ask", "MarketFile.live" not in "".join(text(p) for p in HOME.rglob("*.java") if p.name != "MarketsActivity.java" and p.name != "MarketFile.java")
      and "finance/spark" not in text(ROOT / "src/template.html").split("const MARKET_URL")[0])
fxn = re.findall(r'\{"([A-Z]{3})", S\.n\("([^"]+)"\)\}', act2)
codes = [c for c, _ in fxn]
asked = ["USD", "KWD", "AED", "QAR", "BHD", "OMR", "JOD", "EGP", "EUR", "GBP", "TRY", "INR", "PKR"] + ["IQD", "MAD", "DZD", "TND", "LYD", "SDG", "YER", "LBP", "SYP", "MRU"] \
    + ["PHP", "IDR", "BDT", "LKR", "NPR", "MYR", "ETB", "KES"] + ["JPY", "CNY", "CHF", "CAD", "AUD", "SGD", "HKD", "KRW", "RUB", "ZAR"]
check("currencies: every one the owner asked for has an Arabic name (and SAR is there as the base), with an English one in home.tsv", set(asked) <= set(codes) and "SAR" in codes
      and all(f"{ar}\t" in text(ROOT / "i18n/en/home.tsv") for _, ar in fxn), sorted(set(asked) - set(codes)))
check("currencies: the list shows the defaults first (the dollar, KWD, AED, QAR, BHD, OMR, JOD, EGP, EUR, GBP, TRY, INR, PKR); the others are chosen and saved on this TV",
      'FX_DEFAULT = {"USD", "KWD", "AED", "QAR", "BHD", "OMR", "JOD", "EGP", "EUR", "GBP", "TRY", "INR", "PKR"}' in act2 and 'getSharedPreferences("markets_fx"' in act2 and 'putString("list"' in act2)
ci = text(ROOT / ".github/tools/market_prices.py")
fxc = re.search(r"FX_CODES = \[(.*?)\]", ci, re.S).group(1)
needed = [c for c in codes if c not in ("USD", "SAR") and c not in ("AED", "QAR", "BHD", "OMR", "JOD")]
check("currencies: CI asks every one the app lists that is not a peg (and none of the pegs), in a file of its own, so markets.json stays small", all(f'"{c}"' in fxc for c in needed)
      and not any(f'"{c}"' in fxc for c in ("SAR", "AED", "QAR", "BHD", "OMR", "JOD", "USD")) and "markets/fx.json" in text(HOME / "MarketData.java") + ci.replace("fx.json", "markets/fx.json"))
check("currencies: a fourth tab «صرف العملات» with the list, «حوّل» (the converter with the number pad, USD to SAR at first), and «اختر العملات»; the file is loaded once a day by the page only",
      'S.t("صرف العملات")' in act2 and 'TABS = {"market", "watch", "portfolio", "fx"}' in act2 and "else if (k == 3) currencies();" in act2 and "MarketFile.loadFx(app);" in act2
      and 'fxPrefs().getString("from", "USD"), fxPrefs().getString("to", "SAR")' in act2 and "MarketFile.loadFx" not in "".join(text(p) for p in HOME.rglob("*.java") if p.name not in ("MarketsActivity.java", "MarketFile.java")))
app_fx = re.findall(r'"([A-Z]{3})"', text(HOME / "MarketData.java").split("static final String[] FX_CODES = {")[1].split("};")[0])
check("step 182: the app's own FX_CODES are CI's, in the same order (a TV asks Yahoo for exactly what CI would publish)", app_fx == re.findall(r'"([A-Z]{3})"', fxc), (app_fx, fxc))
mf182 = text(HOME / "MarketFile.java")
lf = mf182[mf182.index("static void loadFx("):mf182.index("private static String fxFromYahoo(")]
check("step 182: CI's fx.json first; Yahoo asked only when it is missing, empty or older than 48 h, once a day (the copy of today returns first), not again for 30 min after a failure; only today's rates are saved",
      "DAY_OF_YEAR" in lf and lf.index("download(MarketData.FX_URL)") < lf.index("MarketData.fxNeedsOwn(text, t) && t - fxOwnAskedAt >= FX_OWN_PAUSE_MS") < lf.index("fxFromYahoo()")
      and "FX_OWN_PAUSE_MS = 30 * 60_000L" in mf182 and "FX_STALE_MS = 48 * 3600_000L" in text(HOME / "MarketData.java")
      and "if (!MarketData.fxNeedsOwn(text, t)) { save(c, FX, text); fxText = text; }" in lf and "fxText == null ||" not in lf)
peg_doc = text(HOME / "MarketData.java")
check("step 182: FX_PEGS keeps its own doc (no two docs stacked over FX_CODES)", "«مربوط بالدولار». */\n    static final String[][] FX_PEGS" in peg_doc and "*/\n    /** the currencies the page" not in peg_doc)
check("currencies: a missing rate shows «—» (MarketData.fxNumber of NaN), never 0; the pegs say «مربوط بالدولار»", 'MarketData.pegged(code) ? code + " · " + S.t("مربوط بالدولار")' in act2 and 'return "—"' in text(HOME / "MarketData.java"))
check("step 28 (the owner's photo): the pad's zero is a Latin-style 0 in Arabic (the Arabic one is a dot beside the separator), the separator key is large, and ⌫ is mirrored in RTL",
      'if (key.equals("0") && !Digits.latin()) return "0";' in act2 and 'sp + 10' in act2 and 'key.equals("⌫") && Dir.rtl()) b.setScaleX(-1f)' in act2
      and act2.count("padStyle((TextView) b,") == 2 and act2.count("button(padLabel(") == 2)
sys.exit(1 if failed else 0)
