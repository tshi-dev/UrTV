#!/usr/bin/env python3
"""4.3 «شاهد المباراة» and «تطبيق المباريات»: on a game's card the button shows only when the chosen app is there (UrTV Player only when its
certificate is the pinned one); UrTV Player gets the game by an explicit intent, the contract below, and nothing else; any other app is only
opened. The setting is the screensaver's (Prefs.matchApp, ConfigProvider's match_app, saver.sh matchapp). MatchFacts runs for real.
    python3 tests/match_app_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
A = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
read = lambda p: p.read_text(encoding="utf-8")
app, matches, tpl = read(H / "MatchApp.java"), read(H / "MatchesActivity.java"), read(ROOT / "src/template.html")
check("the contract: explicit to com.urtv.player, action sa.urtv.player.action.FIND_MATCH, the six names, kickoff (long), channels (String[])",
      'ACTION = "sa.urtv.player.action.FIND_MATCH"' in app and "i = new Intent(ACTION).setPackage(pkg);" in app
      and "i.putExtra(MatchFacts.TEXTS[k], f.texts[k]);" in app and 'i.putExtra("kickoff", f.kickoff);' in app
      and 'i.putExtra("channels", f.channels.toArray(new String[0]));' in app and "sendBroadcast" not in app)
check("UrTV Player only when signed by exactly the pinned certificate and it takes the action", "sigs.length != 1" in app
      and "sha.equalsIgnoreCase(SuggestedApps.PLAYER_CERT)" in app and "pm.resolveActivity(new Intent(ACTION).setPackage(SuggestedApps.PLAYER), 0) != null" in app)
check("auto = UrTV Player, none = no button, another app is only opened (its launcher entry)", '"auto".equals(v) ? SuggestedApps.PLAYER : v' in app
      and 'if ("none".equals(v)) return null;' in app and "getLeanbackLaunchIntentForPackage(pkg)" in app.split("} else {")[1])
check("the page: «شاهد المباراة» only when the app says one is there; the facts go through MatchFacts",
      "if(host && typeof host.matchApp === 'function' && host.matchApp()) out.push({k: 'player', t: FB_W.player});" in tpl
      and "host.findMatch(JSON.stringify(mpFacts(ev)))" in tpl and "MatchFacts.of(sa.urtv.home.phone.Json.parseObject(" in matches)
check("4.3 step 64: with the Player missing the card offers «ثبّت UrTV Player للمشاهدة» (only when the setting is auto or the Player), opening «تطبيقات مقترحة»; no permission added",
      "host.playerOffer()" in tpl and "host.installPlayer()" in tpl and "public boolean playerOffer()" in matches and "public boolean installPlayer()" in matches
      and '(v == null || "auto".equals(v) || SuggestedApps.PLAYER.equals(v)) && MatchApp.chosen(c, v) == null' in matches
      and "new android.content.Intent(a, SuggestedAppsActivity.class)" in matches and "REQUEST_INSTALL" not in matches)
check("4.3 step 64: «مباريات اليوم وبكرة» below «فرقك»: top leagues (Saudi first, big five, Champions League), today and tomorrow only, at most 12, only for a person with no league of their own",
      "const MP_MORE = CONFIG.matchPage && !CONFIG.leagues.length;" in tpl and "MP_TOP_IDS = ['4668', '4328', '4335', '4332', '4331', '4334', '4480']" in tpl
      and "const MP_MORE_MAX = 12;" in tpl and "if(!days.includes(MP.day)) return [];" in tpl and "if(more.length) out.push({key: 'more'" in tpl
      and tpl.index("const more = mpMore(shown)") > tpl.index("keys.forEach(k => {") and "!shown.has(" in tpl and "FB_W.more" in tpl)
check("the page sends the game only: names, competition, kick-off, channel names (no stream, no link)",
      re.search(r"return \{home_ar: .*?away_en: .*?competition_en: .*?kickoff: String\(fbKick\(ev\)\), channels: .*?\};", tpl, re.S) is not None
      and not re.search(r"mpFacts[^}]*(url|link|stream)", tpl.split("function mpFacts")[1][:900]))
check("the setting: the screensaver's Prefs.matchApp (auto by default), its row, its provider column, saver.sh and the docs",
      'sp.getString("matchApp", MATCH_AUTO)' in read(A / "Prefs.java") and 'row("تطبيق المباريات"' in read(A / "SettingsActivity.java")
      and '"match_app"' in read(A / "ConfigProvider.java") and 'cur.getColumnIndex("match_app")' in read(H / "ConfigSnapshot.java")
      and '  matchapp) set1 matchapp ' in read(ROOT / "optimize/saver.sh") and "optimize/saver.sh matchapp auto" in read(ROOT / "docs/SETTINGS.md")
      and "optimize/saver.sh matchapp auto" in read(ROOT / "docs/FEATURES.md"))
check("the launcher may see UrTV Player (Android 11+)", '<package android:name="com.urtv.player" />' in read(ROOT / "android/launcher/src/main/AndroidManifest.xml"))
ma, mact = read(H / "MatchApp.java"), read(H / "MatchesActivity.java")
check("the Player is started for a result from the activity, never with NEW_TASK (it knows its caller by getCallingPackage)",
      "static boolean open(Activity c," in ma and "c.startActivityForResult(i, REQUEST)" in ma and "addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)" not in ma)
check("the page's bridge hands «شاهد المباراة» to the activity's UI thread", "new Bridge(getApplicationContext(), this)" in mact
      and "a.runOnUiThread(" in mact and "MatchApp.open(a, setting, facts)" in mact)
if not shutil.which("javac"):
    print("SKIP the facts' run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "MatchFacts.java"), str(ROOT / "tests/MatchFactsRun.java")], capture_output=True, text=True)
        check("MatchFacts compiles on a plain JVM", r.returncode == 0, r.stderr[-300:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.MatchFactsRun"], capture_output=True, text=True).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            bad = [n for n, v in lines if v != "ok"]
            check(f"MatchFacts: every case ({len(lines)})", len(lines) == 9 and not bad, bad or out)
sys.exit(1 if failed else 0)
