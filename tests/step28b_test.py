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
check("4: a station that gives nothing in 20 s is retried once with its own address straight to the player, then says «تعذّر التشغيل»; the host (never the token) is logged",
      "radioDirectTried" in ms and "trying \" + hostOf(radioAddress) + \" as given" in ms and 'S.t("تعذّر التشغيل")' in ms)
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
check("9: the stock's card has «رجوع» under its actions (▼ reaches it, OK closes the card; first focus on the first action, else on it), and the row that opened the card has the focus again when it closes; the matches card has it too (tests/football_page.test.mjs)",
      'final View back = button(S.t("رجوع"), false, v -> d.dismiss());' in text(HOME / "MarketsActivity.java") and "(made.isEmpty() ? back : made.get(0)).requestFocus();" in text(HOME / "MarketsActivity.java")
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
      "const lg = new Map(leagueGames().map(e => [String(e.idEvent), e]));" in tpl and "Object.assign({}, ev, lg.get(String(ev.idEvent)))" in tpl)
sys.exit(1 if failed else 0)
