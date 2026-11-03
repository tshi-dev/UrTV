#!/usr/bin/env python3
"""Step 27 (build 181), the source-level checks of the fixes that have no pure part of their own: the update flow's end (UpdateActivity), the tasks page,
the English hint, reset.sh's appops, the setup's community list line. (The rules with a pure part have their own tests: saver_hold, mute_keep, markets_page,
football_page, setup_runner.) Run: python3 tests/fix181_test.py"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


upd = text(HOME / "UpdateActivity.java")
check("update: «تم» on the done screen and Back both leave for MainActivity (clear top), and the screen finishes", "private View doneButton()" in upd and 'S.t("تم")' in upd and "new android.content.Intent(this, MainActivity.class)" in upd
      and "FLAG_ACTIVITY_CLEAR_TOP" in upd and "public void onBackPressed()" in upd and "toHome();" in upd.split("public void onBackPressed()")[1][:300])
check("update: both success paths (the record of the replace, the live 'done' state) end with the «تم» button", upd.count("return doneButton();") == 2)
tasks = text(HOME / "TasksActivity.java")
check("tasks: the 13th is refused with «١٢ مهمة كحد أقصى» (the limit, said), shown on the page as well as in a toast", 'S.t("%s مهمة كحد أقصى", ar(MAX_TASKS))' in tasks and "if (status != null) status.setText(s);" in tasks and "MAX_TASKS = 12" in tasks)
check("tasks: every row the same minimum height with its card and buttons filling it, so the gap between rows is one number", "r.setMinimumHeight(dp(56));" in tasks and "new LinearLayout.LayoutParams(0, -1, 1f)" in tasks)
tsv = text(ROOT / "i18n/en/home.tsv")
for ar in ("%s مهمة كحد أقصى", "اكتب الكمية", "اكتب متوسط التكلفة"):
    check(f"i18n: «{ar}» has its English", re.search("^" + re.escape(ar) + "\t.+$", tsv, re.M) is not None)
tw = text(ROOT / "optimize/tweaks.sh")
check("reset: an appop put back to «allow» that reads «default» is not a problem (no !! line), nothing else changes", 'appop_*) [ "${kv#*=}" = allow ] && [ "$got" = default ]' in tw)
st = text(HOME / "setup/SetupTweaks.java")
check("setup: the same rule in the app (allow reads back as default)", 'k.startsWith("appop_") && v.equals("allow") && got.equals("default")' in st)
com = text(HOME / "setup/SetupCommunity.java")
check("setup: the community list used is named in the run's lines (and so in the report)", 'out.line("قائمة المجتمع لهذا الطراز: " + choice.meta.name);' in com)
mp = text(HOME / "MarketData.java")
check("markets: both spark shapes are read (spark.result[].response[].meta and the map keyed by symbol)", "// shape 1:" in mp and "// shape 2:" in mp)
mf = text(HOME / "MarketFile.java")
check("markets: an answer that parses to nothing logs its first 300 characters (UrTVMarkets body_start)", 'body_start=' in mf and "Math.min(300, body.length())" in mf)
py = text(ROOT / ".github/tools/market_prices.py")
check("markets CI: fx.json is carried by every run (the one-commit branch is rebuilt from the folder)", "carried_fx = True" in py and "allowlist_fx(prev_fx)" in py)

# ---- items 14-19 (the owner's TV photos)
main = text(HOME / "MainActivity.java")
check("item 14: two more round buttons in the top row, «الأسواق» and «المباريات», the same Pill (size and 12 dp = 24 px gap) and the same focus", 'pill(S.t("الأسواق"), R.drawable.ic_markets)' in main and 'pill(S.t("المباريات"), R.drawable.ic_matches)' in main
      and "row.add(marketsBtn);" in main and "row.add(matchesBtn);" in main and "static final int GAP = 12;" in text(HOME / "Pill.java").replace("    static final int GAP = 12;", "static final int GAP = 12;"))
act = text(HOME / "MarketsActivity.java")
check("item 15/19: the title, then ONE controls row (the live switch, «صرف العملات», «أخفِ المبالغ» on «محفظتي»), then one quiet 12 sp status line, then the tabs", "controls.addView(liveSwitch()" in act and "controls.addView(fxBtn" in act and "ctl.addView(hideBtn" in act
      and act.index("page.addView(controls") < act.index("page.addView(state") < act.index("page.addView(tabRow"))
check("item 19: the tab row has السوق / قائمتي / محفظتي only; no bar under the chosen tab and no white box (a green word, the focus fill when focused); the thin rule stays", 'new String[] {S.t("السوق"), S.t("قائمتي"), S.t("محفظتي")}' in act
      and "setLayerHeight(1, dp(3))" not in act and "page.addView(rule" in act)
check("item 15: the live switch is toggle style I (the triangles at the chosen word's start, green), not a box with a bar", "R.drawable.ic_on" in act.split("private void paintLive()")[1].split("private void keepAwake")[0] and "dp(7), dp(22)" in act)
check("item 15: the status is one line «تداول مغلق · يفتح الأربعاء ١٠:٠٠ ص · آخر تحديث …» (no «بعد ١١ س» that ran off the screen), 12 sp, at most two lines", 'S.t("يفتح %s %s"' in act and 'state = text("", 12, "Regular", DIM)' in act and "state.setMaxLines(2);" in act)
check("item 16: a card or dialog is a SOLID surface (the look's dialog fill over its base) with the page dimmed behind it", "GlassStyle.over(look.dialog, look.base)" in act and "FLAG_DIM_BEHIND" in act and "setDimAmount" in act)
check("item 19: density: title 27 sp, tabs 17 sp, column headers 12 sp, a list row 60 dp (120 px) with its 12 dp (24 px) padding, card sides 12 dp (24 px)", 'text(S.t("الأسواق"), 27' in act and 'text(names[i], 17' in act and 'S.t("السهم"), 12' in act
      and "r.setMinimumHeight(dp(60));" in act and "c.setPadding(dp(12), dp(16), dp(12), dp(16));" in act)
tpl = text(ROOT / "src/template.html")
check("item 17: the home stocks card has 24 px inside on every side, and «السوق مغلق» sits on the header line right after «الأسهم» (order 0), not at the far end over the first column",
      ".week.stocks{padding:calc(24px*var(--z,1))}" in tpl and "card.el.classList.toggle('stocks', P === 'stocks');" in tpl and 'class="until near"' in tpl and ".week .hd .until.near{order:0" in tpl)


# ---- item 18: «تطبيقات في الشريط»
prefs = text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java")
rcv = text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SaverReceiver.java")
prov = text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/ConfigProvider.java")
snap = text(HOME / "ConfigSnapshot.java")
check("bar size: 5 (default) to 10 or all; a bad value keeps 5; the command checks the word before anything is set, and `show` says it", 'FAV_BARS = {"5", "6", "7", "8", "9", "10", "all"}' in prefs and 'sp.getString("favBar", "5")' in prefs
      and 'error: favbar is one of' in rcv and "if (favBar != null) p.setFavBar(favBar);" in rcv and '"\\nfavbar: " + p.favBar()' in rcv)
check("bar size: the provider gives the home fav_bar, the home reads it (5 for an older screensaver) and the phone API sets and states it", '"home_layout", "fav_bar"}' in prov and 'out.putString("fav_bar", p.favBar())' in prov
      and 'cur.getColumnIndex("fav_bar")' in snap and 'case "fav_bar":' in text(HOME / "phone/Schema.java") and 'm.put("fav_bar"' in text(HOME / "phone/PhoneActions.java"))
mn = text(HOME / "MainActivity.java")
check("bar size: every favourite's cell is as wide as FavBar.tileW(h, shown) says (the tray keeps its box, the insets and the gap), the watch-next rows share the tray's cells, a changed setting resizes the cells", "FavBar.tileW(getResources().getDisplayMetrics().heightPixels, FavBar.shown(barSetting, favs))" in mn
      and "applyBarSize(cfg.favBar);" in mn and "dockCell(i, n, dockCards.size())" in mn)
check("bar size: ten favourites at most; the eleventh is refused with «١٠ كحد أقصى»", "static final int MAX = 10;" in text(HOME / "Dock.java") and 'S.t("%s كحد أقصى", arabicDigits(String.valueOf(MAX_DOCK)))' in mn and 'S.t("%s كحد أقصى", ar(Dock.MAX))' in text(HOME / "DockActivity.java"))
check("bar size: a row «تطبيقات في الشريط» in «التطبيقات والمفضلة» (المزيد) with its seven choices, the command in saver.sh, the row in docs/SETTINGS.md", 'row("تطبيقات في الشريط"' in text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java")
      and '{"تطبيقات في الشريط", APPS}' in text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsHub.java") and "favbar)    set1 favbar" in text(ROOT / "optimize/saver.sh") and "optimize/saver.sh favbar 7" in text(ROOT / "docs/SETTINGS.md"))
check("bar size: the tray CI measures 7 (Arabic) and 10 (English) whole tiles and takes a picture of the markets page", 'for n, lang, favs in ((7, "ar", 7), (10, "en", 10)):' in text(ROOT / ".github/screens/tray_check.py") and "markets.png" in text(ROOT / ".github/screens/tray_check.py")
      and "make_stubs.sh stubs 12" in text(ROOT / ".github/workflows/tray.yml"))

print("ALL PASS" if not failed else f"{failed} failed")
raise SystemExit(1 if failed else 0)
