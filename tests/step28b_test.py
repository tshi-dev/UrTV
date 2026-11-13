"""Tester step 28b (build 183): source checks of the fixes that no pure-Java run reaches.
Run: python3 tests/step28b_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    if not ok:
        failed += 1


def text(p):
    return Path(p).read_text(encoding="utf-8")


main = text(HOME / "MainActivity.java")
check("1: ▲ from a home card goes to a pill that is on screen (the pill it came from may have left the row), by its own key handler; OK is left to the click",
      "keyCode != KeyEvent.KEYCODE_DPAD_UP) return false;" in main and "pv.isShown() && pv.isFocusable()" in main and "lastPillId = v.getId()" in main)
tpl = text(ROOT / "src/template.html")
check("2: the matches list keeps its day when a team is followed or unfollowed (the new team's games must not move it), and Back on the open card draws nothing of it",
      "if(ok && b.k !== 'remind') MP.picked = true;" in tpl and "mpBack(); return e.preventDefault(); }" in tpl)
mf, md = text(HOME / "MarketFile.java"), text(HOME / "MarketData.java")
check("3: with no fx file from CI (or an old one, step 182) the app asks Yahoo itself (two batches of the 35 codes), once a day, and keeps the result as CI's file",
      "private static String fxFromYahoo()" in mf and "if (MarketData.fxNeedsOwn(text, t) && t - fxOwnAskedAt >= FX_OWN_PAUSE_MS) {" in mf and "from += 18" in mf and "static String fxFile(" in md)
ms = text(HOME / "tilawah/MusicService.java")
check("4: a station that gives nothing in 20 s is retried once with its own address straight to the player, then goes to RadioPolicy (4.3: tried again, its second source, then «تعذّر الاتصال بالبث»); the host (never the token) is logged",
      "radioDirectTried" in ms and "trying \" + hostOf(radioAddress) + \" as given" in ms and "onPlaybackError(MediaPlayer.MEDIA_ERROR_UNKNOWN, PlayerFault.TIMED_OUT);" in ms)
check("5: a Tadawul row with no timestamp and no close is read from fulldayPrice, its time is now", 'meta.get("fulldayPrice")' in md and "System.currentTimeMillis() / 1000.0" in md)
rc = text(HOME / "tilawah/AdhanReceiver.java")
check("6 (the owner's choice B): a muted TV is unmuted for the adhan and muted again at its end; the mute is read FIRST (before the unmute) and logged as tv_muted_before; the re-mute result is logged; every end goes through adhanVolumeBack (over, stopped, left, the screen off, the service gone, a restart after a crash)",
      "final boolean mutedBefore = tvMuted();" in ms and '" tv_muted_before=" + mutedBefore' in ms and ms.index("tv_muted_before=") < ms.index("adhanVolumeUp();\n        boolean comeBack")
      and 'adhan end: re-mute " + (audio.isStreamMute(AudioManager.STREAM_MUSIC) ? "done" : "FAILED' in ms and "adhan volume: the TV was muted, unmuted for the adhan" in ms
      and ms.count("adhanVolumeBack();") >= 5 and "if (source == Prefs.SOURCE_ADHAN) finishAdhan();" in ms and "savedVolume = prefs.savedTvVolume();" in ms
      and "muted(context)" not in rc and "adhan skipped: the TV is muted" not in rc and "adhan skipped: the TV is muted" not in ms)
sc = text(HOME / "setup/SetupCommunity.java")
check("7: the community list's name is in the report also when our own lists know the TV", "choice.meta != null ? \" (\" + choice.meta.name + \")\" : \"\"" in sc)

# the owner's ask: «صفحات الأسهم والمباريات داكنة» (default on)
SA = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
pf, rv, cpv, st = text(SA / "Prefs.java"), text(SA / "SaverReceiver.java"), text(SA / "ConfigProvider.java"), text(SA / "SettingsActivity.java")
check("8: the setting: Prefs (on by default), the command's word checked before anything is set, `show` says it, the provider gives the home pages_dark, and the settings row is next to «المظهر»",
      'boolean pagesDark() { return sp.getBoolean("pagesDark", true); }' in pf and 'error: pagesdark is one of on off' in rv and 'p.setPagesDark("on".equals(pagesDark))' in rv
      and '"\\npagesdark: " + (p.pagesDark() ? "on" : "off")' in rv and '"pages_dark"' in cpv and 'p.pagesDark() ? "1" : "0"' in cpv
      and st.index('row("المظهر"') < st.index('row("صفحات الأسهم والمباريات داكنة"') < st.index('row("الخروج من تلفزيونك"'))
cfg, gs = text(HOME / "ConfigSnapshot.java"), text(HOME / "GlassSource.java")
check("8: the home reads it (on for an older screensaver); Markets uses the dark look (GlassStyle.DARK over the same backdrop) while on, else the home's look; the matches page gets #pagedark=1",
      "boolean pagesDark = true;" in cfg and 'cur.getColumnIndex("pages_dark")' in cfg and "GlassStyle.Look l = GlassStyle.DARK;" in gs and "if (!ConfigSnapshot.read(a).pagesDark) return page(a, root);" in gs
      and "GlassSource.pageFor(this, root)" in text(HOME / "MarketsActivity.java") and '(cfg.pagesDark ? "&pagedark=1" : "")' in text(HOME / "MatchesActivity.java"))
check("8: the page has its dark theme (#pagedark=1 → html.mpdark), the sand look is the default of the page without it", "pageDark: _Q.get('pagedark') === '1'," in tpl and "html.mpage.mpdark,html.mpage.mpdark body{background:#17181B}" in tpl
      and "classList.toggle('mpdark', !!CONFIG.pageDark)" in tpl)
sh = text(ROOT / "optimize/saver.sh")
check("8: saver.sh pagesdark on|off, and the export / import carry it (settings_move.py: SWITCHES, the allow-list, the import)", 'pagesdark) set1 pagesdark' in sh
      and '"pagesdark"' in text(ROOT / "optimize/settings_move.py") and text(ROOT / "optimize/settings_move.py").count('"pagesdark"') >= 5)

# the owner: a «رجوع» under the stock's card, and the focus back on its row
check("9: the stock's card has «رجوع» under its actions (▼ reaches it, OK closes the card; the first focus is on it, step 30), and the row that opened the card has the focus again when it closes; the matches card has it too (tests/football_page.test.mjs)",
      'final View back = button(S.t("رجوع"), false, v -> d.dismiss());' in text(HOME / "MarketsActivity.java") and "back.requestFocus();" in text(HOME / "MarketsActivity.java")
      and "final View from = getCurrentFocus();" in text(HOME / "MarketsActivity.java") and "from.requestFocus();" in text(HOME / "MarketsActivity.java")
      and 'data-k="back"' in tpl and "if(ringed && ringed.dataset.k === 'back'){ mpBack(); return; }" in tpl)

# step 29
mk = text(HOME / "MuteKeep.java")
check("10: the pad's keys centre their glyph (no padding above or below, no font padding): the bottom row's ⌫ and «٫» were drawn low and cut",
      "b.setPadding(dp(20), 0, dp(20), 0);" in text(HOME / "MarketsActivity.java").split("private void padStyle")[1].split("private View button")[0]
      and "b.setIncludeFontPadding(false);" in text(HOME / "MarketsActivity.java") and "b.setGravity(Gravity.CENTER);" in text(HOME / "MarketsActivity.java").split("private void padStyle")[1])
check("11: the options hint shows again once, six times, in either language (the Arabic count of a TV in use was spent)",
      'prefs().getInt("options_hint_v", 0) < 2' in main and 'putInt("options_hint", 0).putInt("options_hint_en", 0).putInt("options_hint_v", 2)' in main)
check("12: every volume or mute change of ours is logged (UrTVMute stage=ours: what we did and the state after), the system's own mute changes are logged with who did it, and the unmutes we can cause (the adhan's, the phone's «كتم» and volume keys) call it",
      "public static void ours(Context c, String why)" in mk and '"android.media.STREAM_MUTE_CHANGED_ACTION"' in mk and 'by=" + (SystemClock.elapsedRealtime() - oursAt < 3000 ? "us"' in mk
      and all(w in ms + text(HOME / "phone/PhoneActions.java") for w in ('ours(this, "adhan_unmute")', 'ours(this, "adhan_volume_set")', 'ours(this, "adhan_volume_back")', 'ours(this, "adhan_remute")', '"phone_mute_toggle"', '"phone_vol_up"')))
check("13: a game the league's file also has is read from the league's copy (a followed team's own copy of it may carry another day or status: «فرقك» was lost after «أضف لفرقي»)",
      # 4.3 A2: through fresher() (matchEvents): the league's copy, unless the team's is further on (a score, a later minute); tests/called_off_test.py runs it
      "const leagueMap = () => CONFIG.leagues.length ? new Map(leagueGames().map(e => [String(e.idEvent), e])) : new Map();" in tpl
      and "? Object.assign({}, l, ev) : Object.assign({}, ev, l);" in tpl and "return matchEvents().filter(" in tpl)

# step 30
mka = text(HOME / "MarketsActivity.java")
sc = mka.split("private void stockCard(")[1].split("/** «الأسعار متأخرة")[0]
check("14: the stock card fits the safe area: its text part is a scroll view that shrinks when the card is taller than the screen minus 5 % top and bottom, the action buttons and «رجوع» are a fixed footer; the first focus is «رجوع», never an action; the chart is shorter",
      "final ScrollView bodyScroll = new ScrollView(this);" in sc and "box.addView(bodyScroll, new LinearLayout.LayoutParams(-1, -2));" in sc and "int room = screenH - 2 * Math.round(screenH * 0.05f)" in sc and "inner.measure(wide, any);" in sc and "bodyScroll.setForeground(ring);" in sc and "box.post(() -> back.requestFocus());" in sc
      and "back.requestFocus();" in sc and "(made.isEmpty() ? back : made.get(0)).requestFocus()" not in sc and "chartBlock(null, q, 40, 96, MarketData.symbol(code))" in sc and "made.get(0).requestFocus()" not in sc)
check("14: after an action that draws the page again the focus is on the row that opened the card (its place is kept), not on «مباشر»", "focusIndexAfter = from == null ? -1 : all.indexOf(from);" in sc and "if (at < 0 && focusIndexAfter >= 0) at = focusIndexAfter;" in mka)
check("15: both number pads' key rows are not baseline-aligned (the bigger «٫» pushed the other keys 8 px down and cut their bottoms)", mka.count("line.setBaselineAligned(false);") == 2)
gt = text(HOME / "GreetingText.java")
check("16: the greeting is «مساء الخير» until two hours after Isha (not «تصبح على خير» at 19:18), and the plain one says the night from 22:00", "now < isha + NIGHT_AFTER_ISHA_MS" in gt and "2 * 3600_000L" in gt and text(HOME / "Greeting.java").count("hour < 4 || hour >= 22") == 2)
check("17: the followed teams' games are kept on the TV and a team whose requests were all refused is asked again once, 20 s later (a quick reopen lost «فرقك» when the sports server refused it)",
      "localStorage.getItem('teamData')" in tpl and "saveTeamData();" in tpl and "setTimeout(loadMatches, 20000)" in tpl and "if(!got) failed.push(id);" in tpl)

# step 30b
mdt = text(HOME / "MarketData.java")
la = mka.split("private void liveAsk()")[1].split("private void liveDone")[0]
hd = mka.split("private void header()")[1].split("private static final String[] DAYS")[0]
check("30b-1: a closed exchange's symbols are not asked (and the log says so with reason=closed); asked symbols are the file's keys (2222.SR)", "MarketData.openFor(it.next(), at0)" in la and 'reason=closed' in la and "MarketData.symbol(c)" in la)
check("30b-2: the header says New York's session when US symbols are on the page and it is open, and «مباشر» (not «أسعار متأخرة») while live prices are fresh",
      'S.t("السوق الأمريكي مفتوح")' in hd and "usShown()" in hd and "MarketData.usCloseAt(now)" in hd and 'S.t("مباشر")' in hd and "MarketData.liveFresh(liveOkAt, now)" in hd)
ax = mka.split("private View axis(")[1].split("private static final String[] MONTHS")[0]
check("30b-3: a US symbol's chart axis shows New York's hours, and its card shows «–» for the open and range while the file's line is the session before's",
      'clock(9, 30)' in ax and 'clock(16, 0)' in ax and "MarketData.staleSession(MarketData.symbol(code), q.t, System.currentTimeMillis())" in sc and "l.c.length > 0 && !\"SA\".equals(exchange(e.getKey())) ? l.c : q.c" in mdt)
tp = text(ROOT / "src/template.html")
check("30b-4: «تغيّر الأسهم» as an amount applies to every row, Saudi ones too", "CONFIG.stkChg === 'amount' && !isSA(k)" not in tp and "CONFIG.stkChg === 'amount'\n" in tp)
check("30b-5: «السوق» has a small row of S&P 500, Nasdaq and Dow, asked live while New York is open", "private View usIndices()" in mka and '{"^GSPC", "^IXIC", "^DJI"}' in mka and "usIndices()" in mka.split("private void market()")[1].split("private View usIndices")[0])

# step 31
main31 = text(HOME / "MainActivity.java")
check("31-3 (revised, step 32): the glow takes the focused tile's colour at once on ONE gradient whose colours change in place (no fade, no stacked layers: the cross-fade made the tray worse), the same colour does nothing; fadeGlow starts an animator only when its target changes",
      "glowDrawable.setColors(" in main31 and "if (to != glowColor) setGlow(to);" in main31 and "glowTop" not in main31 and "ValueAnimator" not in main31
      and "if (glow != null && glowAlphaTo != alpha)" in main31 and "if (shadeTo != shade)" in main31)
check("32-3: the stock card's scroll view keeps its text 12 dp clear of its focus ring", "bodyScroll.setPadding(dp(12), dp(12), dp(12), dp(12));" in sc)
check("31-4: a live tick that says the same header text draws nothing", "if (!shown.equals(lastHeader))" in mka and "contentEquals(cardNote.getText())" in mka)

# step 32
mt = text(HOME / "MatchesActivity.java")
sv = text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/ScreenView.java")
check("32: the matches page opens in its own ground (dark #17181B or sand) in the view and the window, and is shown only once its first frame is painted (visual-state callback, 2 s fallback)",
      "cfg.pagesDark ? Color.rgb(0x17, 0x18, 0x1B)" in mt and "getWindow().setBackgroundDrawable" in mt and "web.setAlpha(0f);" in mt and "postVisualStateCallback" in mt and "web.postDelayed(reveal, 1500)" in mt)
check("32: the screensaver's page is shown only once painted, on a dream window of its own ground", "if (hold) w.setAlpha(0f);" in sv and "postVisualStateCallback" in sv
      and "0xFF040E09" in text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/ScreenDream.java"))

# step 33
check("33-2: the screensaver page opened as an activity has the page's ground (#040E09) as its window background, like the dream",
      "0xFF040E09" in text(ROOT / "android/app/src/main/java/sa/urtv/screensaver/MainActivity.java"))
check("33-3: the prayer cards have the computed times at once (not «—» until the network answers), and the answer replaces them",
      "if(!PRAYERS.length) try{" in tp and "buildPrayers(computePrayers()); PRAYERS_DAY = want + '|calc'; }" in tp and "if(kept && kept.d === want){ buildPrayers(kept.t);" in tp and "localStorage.setItem(PT, JSON.stringify({d: want, t}))" in tp)

# step 34
import re as _re
check("34-A: the English page's radial gradients go with their side (the top veil's centre at 100% 0% in English), checked by pixels in page.test",
      "radial-gradient\\(" in text(ROOT / "src/build.py") and "100 - int(m.group(2))" in text(ROOT / "src/build.py") and "top veil, ${file}: no hard edge" in text(ROOT / "tests/page.test.mjs"))
check("34-B: while a symbol's exchange is open a market file older than 20 minutes is not trusted (Yahoo is asked, the older file only when Yahoo refuses), and a quiet «متأخر» shows beside the time",
      "LATE_MS = 20*60000" in tp and "(!lax && sesOpen(sym) ? LATE_MS : MARKET_TRUST_MS)" in tp and "directQuote(j) || fileQuote(j.key, 1)" in tp and "' · ' + 'متأخر'" in tp and "? 'متأخر' : ''" in tp)

# step 35 (release)
check("35-1: Cairo's radio, LiveFeed and LiveProxy are gone (the station was the last: saved numbers stay), cleartext is for radiojar.com only",
      not (ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/LiveFeed.java").exists() and not (ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/LiveProxy.java").exists()
      and "radiojar.com/8s5u5tpdtwzuv" not in text(ROOT / "android/launcher/src/main/assets/quran/stations.json") and "127.0.0.1" not in text(ROOT / "android/launcher/src/main/res/xml/network_security_config.xml")
      and "LiveFeed" not in text(HOME / "tilawah/MusicService.java"))
check("35-2: the S&P 500 row has 16 and 8 dp between name, price and change and its price is TEXT-coloured (the focus turns it dark on the pill)",
      "pp.setMarginStart(dp(16));" in mka and "cp.setMarginStart(dp(8));" in mka and 'ltr(text(price(q.p), 16, "Regular", TEXT)), pp' in mka)
check("35-3: the stock card's «رجوع» is in the actions' row (one footer row, not two) and the chart is 96 dp, so the card fits without scrolling", "List<View> row = new ArrayList<>(made);" in sc and "row.add(back);" in sc)
sys.exit(1 if failed else 0)
