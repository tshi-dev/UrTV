#!/usr/bin/env python3
"""Build 179's re-check (tester step 23): the numbers pinned. 1 dp = 2 px at the tester's 1080p.   python3 tests/fix179_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
RES = ROOT / "android/launcher/src/main/res"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


main = text(HOME / "MainActivity.java")
pills = re.findall(r"sideLlp\(-2, dp\((?:Pill|NowCard)\.HEIGHT\), 0, [-\w]+, dp\(([\w.]+)\), [-\w]+\)", main)
gap = int(re.search(r"static final int GAP = (\d+);", text(HOME / "Pill.java")).group(1))
check("the round buttons at the top of the home: every one (and the now-playing card) is added with the one gap Pill.GAP, which is 24 px at 1080p (12 dp at density 2) and on the 8 px scale",
      len(pills) == 6 and set(pills) == {"Pill.GAP"} and gap * 2 == 24 and (gap * 2) % 8 == 0, (pills, gap))
row = text(RES / "layout/item_row.xml")
check("Tilawah rows: 32 px of padding above and below the text (16 dp), 32 at the start, 32 at the end; rows 16 px apart",
      'android:paddingTop="16dp"' in row and 'android:paddingBottom="16dp">' in row and 'android:paddingEnd="16dp"' in row
      and all('android:dividerHeight="8dp"' in text(RES / f"layout/{f}.xml") for f in ("activity_main", "activity_adhan"))
      and 'android:verticalSpacing="8dp"' in text(RES / "layout/activity_surah.xml"))
pin = text(HOME / "PinUi.java")
check("PIN pad: no focus scale on a key (it ate 4 px of the gap beside it); the gaps between keys are one value, 24 px",
      "scaleX" not in pin and text(HOME / "PinPrompt.java").count("dp(a, 12)") >= 5)
settings = text(APP / "SettingsActivity.java")
check("settings pages: 64 px above the first line, a group's header 32 above and 16 under it (the first row was 124 px down)",
      "list.setPadding(dp(8), dp(32), dp(8), dp(16));" in settings and "t.setPadding(dp(10), dp(16), dp(10), dp(8));" in settings and "head.setPadding(0, 0, 0, dp(16));" in settings)
mk = text(HOME / "MarketsActivity.java")
check("markets: the page ends at the safe area (5 % under it), so no list starts under y 1026 and the lists scroll in",
      "page.setPadding(Math.round(dm.widthPixels * 0.05f), Math.round(dm.heightPixels * 0.05f), Math.round(dm.widthPixels * 0.05f), Math.round(dm.heightPixels * 0.05f));" in mk
      and "page.setPadding(dp(" not in mk)
check("markets: stock, volume and watchlist rows have 24 px above and below their text (box wider than 600 px)", mk.count("r.setPadding(dp(16), dp(12), dp(16), dp(12));") >= 3 and "r.setPadding(dp(16), dp(8), dp(16), dp(8));" not in mk)
check("markets: pressing «أخفِ/أظهر المبالغ» keeps «محفظتي»: the tab's own re-draw does not let the focus that falls on the tabs switch the tab",
      "private boolean rebuilding;" in mk and "if (has && k != tab && !rebuilding) want(k);" in mk and "if (k == tab) {\n            rebuilding = true;" in mk)
tpl = text(ROOT / "src/template.html")
check("football games card: a club's name takes two lines and the league label wraps to two, nothing is cut with «…» before a third line",
      re.search(r"\.ftm \.nm\{white-space:normal;[^}]*-webkit-line-clamp:2", tpl) is not None
      and re.search(r"\.fct span:first-child\{[^}]*-webkit-line-clamp:2", tpl) is not None)
check("dist matches the source (src/build.py)", text(ROOT / "dist/tv.html").count("-webkit-line-clamp:2") >= 2 and text(ROOT / "dist/tv-en.html").count("-webkit-line-clamp:2") >= 2)
check("markets (review R11): no file is read or parsed on the UI thread: onCreate takes the in-memory copy, the worker parses the copy on the TV and then loads, a loading note shows meanwhile",
      "data = MarketFile.last();" in mk and "MarketFile.cached(this)" not in mk and "deliver(gen, MarketFile.cached(app))" in mk and "deliver(gen, MarketFile.load(app, false))" in mk
      and "private static volatile MarketData lastGood;" in text(HOME / "MarketFile.java") and 'loading ? S.t("جارٍ التحميل…")' in mk)
check("…and a result is dropped by a pause, a newer load, a finishing or destroyed page", "if (gen != loadGen || isFinishing() || isDestroyed()) return;" in mk and "loadGen++;   // a load still running" in mk)
fav = text(HOME / "FavBar.java")
check("favourites tray (review R10): it rests 40 px higher (94 from the bottom) so the lowered tray ends at 1026; the fully lowered state is tested in fav_bar_test", "REST_BOTTOM = BOTTOM + 40" in fav and "bar(FavBar.REST_BOTTOM)" in main)
svc = text(HOME / "setup/SetupService.java")
check("the setup's ongoing notification is in the person's language (review R12)", svc.count("S.t(\"") == 3 and "S.init(this)" in svc and '"إعداد تلفزيوني", NotificationManager' not in svc)
md = text(HOME / "MarketData.java")
check("market data (owner): the TV asks the file every 5 minutes (the open page too), a late file shows its own time «آخر تحديث», no error",
      "FRESH_MS = 5 * 60_000L" in text(HOME / "MarketFile.java") and "if (++ticks % 5 == 0) refreshPrices();" in mk and "periodic(loadMarkets, (CONFIG.lowram ? 15 : 5)*60*1000);" in tpl and "15*60*1000);" not in tpl[tpl.index("const loadStocks = loadMarkets"):][:6000])
check("market data: «الأسعار متأخرة · المصدر: Yahoo Finance» under every tab of the markets page and on the stock card, small, in the file's own provider name",
      'S.t("الأسعار متأخرة · المصدر: %s", name)' in mk and "content.addView(sourceNote(), gap(16));" in mk and "cardNote = (TextView) sourceNote();" in mk and 'return "stooq".equals(src)' in md
      and "الأسعار متأخرة · المصدر: %s\tDelayed prices · Source: %s" in text(ROOT / "i18n/en/home.tsv"))
# ---- build 180's final test (work/fix-180)
ml = text(RES / "layout/activity_main.xml")
check("B3: the Tilawah tabs are 24 px apart (12 dp)", ml.count('android:layout_marginStart="12dp"') >= 2 and 'android:layout_marginStart="10dp"' not in ml)
check("B6: a change is one left-to-right isolate (MarketData.isolate) in the lists and in the price line, so the minus and the percent sign stay put in Arabic",
      "MarketData.isolate(MarketData.show(MarketData.signed(v, 2) + \"%\", Digits.latin()))" in mk and "MarketData.changeLine(r.change(q.p), r.pct(q.p), Digits.latin())" in mk)   # the price line: two isolates, one per number (step 27)
check("B6: a time never breaks («10:00 / AM»: a no-break space), the trend card's title is one line and gets its width (the note beside it gives way)",
      'Digits.of(h12 + ":" + mm) + "\\u00A0"' in mk and "trendTitle.setSingleLine(true);" in mk and "weight(1.6f, 0)" in mk and "weight(1.15f, 16)" in mk)
check("«الذهب والنفط»: the name is on a line of its own (no 260 dp of fixed columns beside it), so the card never squeezes it into single letters",
      "r.addView(text(name, 16, \"Regular\", TEXT), wrap());" in mk and "dp(112), -2" not in mk.split("private void commodity")[1].split("into.addView(r, gap(12));")[0])
check("B10: every live request is logged under tag UrTVMarkets: the status, how many symbols were asked and parsed, the size, the reason (http_<status>, redirected, empty_body, parsed_none, the exception class); no symbol or price",
      'android.util.Log.i("UrTVMarkets", "live status=" + status + " asked=" + symbols.size() + " parsed=" + out.size()' in text(HOME / "MarketFile.java")
      and '"redirected"' in text(HOME / "MarketFile.java") and '"parsed_none"' in text(HOME / "MarketFile.java") and '"empty_body"' in text(HOME / "MarketFile.java"))
check("B10: the live request has a generic honest User-Agent (Mozilla/5.0 compatible start), asks for JSON, and the JSON reader goes deep enough for the spark answer",
      'UA = "Mozilla/5.0 (compatible; UrTV-markets/1.0; +https://github.com/tshi-dev/UrTV)"' in text(HOME / "MarketFile.java") and 'setRequestProperty("User-Agent", UA)' in text(HOME / "MarketFile.java")
      and "depth > 12" in text(HOME / "MarketData.java"))
check("item 10: after a holding is saved, deleted or the portfolio wiped the page is drawn again with the focus kept (redraw), and a focus that is lost goes to «أضف سهمًا», never to «مباشر»",
      mk.count("redraw();") >= 6 and "tab == 2 && actions.getChildCount() > 0 ? actions.getChildAt(0)" in mk and "liveButtons.contains(getCurrentFocus())" in mk
      and "show(2, false);\n        });\n        save.setBackground" not in mk)
check("item 11: the dock is topped up from the other apps in the order of their package names, never of their labels (labels follow the language: Downloader in Arabic, Apple TV in English)",
      "AppsEdit.topUp(have, other, MIN_DOCK)" in main and "java.util.Collections.sort(pool);" in text(HOME / "AppsEdit.java"))
tpl2 = text(ROOT / "src/template.html")
check("item 9: the matches page opens on the day the widget shows (today's games, else the next day that has games, in the city's zone) until the person picks a day",
      "if(!MP.picked){" in tpl2 and "const next = fbGames().filter(ev => fbKick(ev) > now).sort((a, b) => fbKick(a) - fbKick(b))[0];" in tpl2 and "MP.picked = true;" in tpl2)
check("item 9: a match's card opens with no button focused (OK adds nothing by accident); ◀ ▶ go to the buttons",
      "MP.btn = Math.max(-1, Math.min(MP.btn, btns.length));" in tpl2 and ": -1;   // «شاهد على …» is ringed when it is there" in tpl2 and "ringed = document.querySelector('#mp-detail .mdb button.f, #mp-detail .mdq button.f')" in tpl2)   # step 28b: btns.length is «رجوع»
sys.exit(1 if failed else 0)
