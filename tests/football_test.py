"""The football home and «المباريات» (docs/PROPOSALS-NOV2.md §1): the setting «المظهر ← الواجهة» (off by default) from the screensaver to the
home, the pages HomeCardChoice opens, the widgets' focus targets and the tray's one move, the page of the games and what it may ask of the
app, the venue kept by league_fixtures.py, and the broadcast table's rules (assets/shared/broadcast.json, .github/tools/broadcast_check.py).
The pages themselves: tests/football_page.test.mjs. Run: python3 tests/football_test.py"""
import datetime, importlib.util, json, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
read = lambda p: p.read_text(encoding="utf-8")

prefs, recv, prov, st = read(APP / "Prefs.java"), read(APP / "SaverReceiver.java"), read(APP / "ConfigProvider.java"), read(APP / "SettingsActivity.java")
check("the setting: classic (the default, the home as it always was) or football, nothing else kept",
      'static final String[] HOME_LAYOUTS = {"classic", "football"};' in prefs and 'sp.getString("homeLayout", HOME_LAYOUTS[0])' in prefs
      and "if (java.util.Arrays.asList(HOME_LAYOUTS).contains(v)) sp.edit().putString(\"homeLayout\", v).apply();" in prefs)
check("saver.sh homelayout: refused unless classic or football, saved, and said by show",
      'return "error: homelayout is one of "' in recv and "if (homeLayout != null) p.setHomeLayout(homeLayout);" in recv
      and '"\\nhomelayout: " + p.homeLayout()' in recv and 'homelayout) set1 homelayout "${2:?usage: optimize/saver.sh homelayout classic|football}" ;;' in read(ROOT / "optimize/saver.sh"))
check("the home reads it as a column of its own after the others (an older screensaver has none: classic)",
      '"watch_next", "look", "home_layout", "fav_bar", "pages_dark", "pills_hidden", "look_tweaks", "match_app"});' in prov and "p.look(), p.homeLayout()," in prov and "p.favBar(), p.pagesDark()" in prov
      and 'layout = l >= 0 && c.getString(l) != null ? c.getString(l) : "classic";' in read(HOME / "HomeCard.java"))
appr = read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/AppearanceActivity.java")
check("on the TV: «المظهر» (its one page, since design/43) has the «الواجهة» choice, and «المباريات والأسواق» a row that opens the page of the games",
      'w("الواجهة")' in appr and "prefs.setHomeLayout(key)" in appr
      and 'row("مباريات اليوم", "الأيام والدوريات، وفرقك أولاً"' in st and 'setClassName("sa.urtv.home", "sa.urtv.home.MatchesActivity")' in st)
check("SETTINGS.md names the command", "`optimize/saver.sh homelayout football`" in read(ROOT / "docs/SETTINGS.md"))
check("«تابع» goes through the screensaver's provider, the launcher's own permission, the same list as «الفرق المتابعة»",
      'case "phone_follow": {' in prov and "ok = p.addTeam(kv[0], name);" in prov
      and prov.index('c.checkCallingPermission(PHONE_PERMISSION)') < prov.index('case "phone_follow"'))

card, main, ff, ma = read(HOME / "HomeCard.java"), read(HOME / "MainActivity.java"), read(HOME / "FootballFocus.java"), read(HOME / "MatchesActivity.java")
check("HomeCard: football only when the setting says so (and something is known), its page instead of the cards",
      'football = "football".equals(layout) && footballHost != null && choice != null;' in card
      and "return withGlass(HomePage.fresh(activity, HomeCardChoice.footballUrl(choice)));" in card)
check("HomeCard: the widgets' box is the screen's width from its top, 832 high at 1080p, and the cards' own place comes back without them",
      "params.width = dm.widthPixels;" in card and "FootballFocus.HEIGHT_1080 * dm.heightPixels / 1080f" in card and "static final int HEIGHT_1080 = 832;" in ff
      and "params.gravity = gravity0; params.leftMargin = left0; params.topMargin = top0; params.rightMargin = right0;" in card)
check("a focus target over each widget, placed where the page says, its OK the home's; ◀ ▶ on the games walk them and never leave",
      'js("typeof fbRects===\'function\'?fbRects():\'\'"' in ff and "js(\"fbFocus('\" + key + \"')\", this::place);" in ff
      and 'js("fbMove(" + (code == Dir.forwardKey() ? 1 : -1) + ")", null);' in ff and "return true;   // ◀ ▶ walk the games" in ff)
check("the games' focus drops the tray 40 px (lowerTray), a favourite's focus brings it back before the tile lifts",
      "lowerTray(FootballFocus.MT.equals(which));" in main and "long wait = inDock && has && trayLowered ? FavBar.DROP_MS : 0;" in main
      and "if (inDock && has) lowerTray(false);" in main and ".setStartDelay(wait)" in main)
check("the focus: ▼ from a widget is the tray, ▲ from the tray the games, ▲ from the games the prayers, the buttons down to the prayers",
      "v.setNextFocusDownId(dockId != View.NO_ID ? dockId : v.getId());" in main and "fMt.setNextFocusUpId(fPr.getId());" in main
      and "if (fMt != null && fPr != null && fWx != null) topId = fMt.getId();" in main and "card = fPr;" in main)
check("the football home hides the greeting (the weather has its card) and its glass follows the light", "private void footballHeader()" in main
      and "greeting.setVisibility(football ? View.GONE : View.VISIBLE);" in main and "if (homeCard != null) homeCard.light(light);" in main)
check("OK on the games opens «المباريات» on the chosen game", "guarded(ParentalPolicy.WIDGET_MATCHES, null, S.t(\"المباريات\"), () -> launch(games));" in main
      and 'js("typeof fbPick===\'function\'?fbPick():\'\'", id -> host.open(MT, unquote(id)));' in ff)
check("«المباريات»: the page's own mode, Back closes a game's card first, its WebView pauses and resumes like HomeCard's",
      "HomeCardChoice.matchesUrl(cfg.fragment, event)" in ma and "mpBack()" in ma and 'if (!"closed".equals(FootballFocus.unquote(v))) finish();' in ma
      and 'android:name=".MatchesActivity"' in read(ROOT / "android/launcher/src/main/AndroidManifest.xml"))
check("what the page may ask: a task (the reminder's hook, MatchReminder), a followed club added or removed, the broadcaster's app (installed? open), and 4.3 «شاهد المباراة» (is a match app there? the game's checked facts to it), nothing else",
      ma.count("@JavascriptInterface") == 7 and "public boolean matchApp()" in ma and "public boolean findMatch(String json)" in ma and "public boolean remind(String text, double at)" in ma and "public boolean follow(String id, String name)" in ma and "public boolean unfollow(String id)" in ma
      and "public String appOf(String csv)" in ma and "public boolean open(String csv)" in ma
      and 'if (id == null || !id.matches("\\\\d{1,9}")) return false;' in ma and "static boolean add(Context c, String text, long at)" in ma
      and 'pkg.matches("[A-Za-z0-9_.]{3,120}")' in ma and "getLeanbackLaunchIntentForPackage" in ma)   # a package name only, never a link or an intent from the page
check("«ذكّرني قبلها»: a task with a reminder at kick-off, «قبلها بـ» 30, silent (ReminderPlan.forMatch, tests/ReminderHomeRun.java), the alarm set again",
      "long[] r = ReminderPlan.forMatch(at);" in ma and 'v.put("due", r[0]);' in ma and 'v.put("bf", (int) r[1]);' in ma and 'v.put("snd", (int) r[3]);' in ma
      and "if (ok) ReminderScheduler.schedule(c);" in ma
      and "static final int MATCH_BEFORE = 30;" in read(HOME / "ReminderPlan.java") and "return new long[] {kickoff, MATCH_BEFORE, 0, 0, 0};" in read(HOME / "ReminderPlan.java"))
check("the tray still makes only the one move", main.count("tray.animate()") == 1)

# league_fixtures.py keeps the venue for the game's card
spec = importlib.util.spec_from_file_location("lf", ROOT / ".github/tools/league_fixtures.py")
sys.path.insert(0, str(ROOT / ".github/tools"))
lf = importlib.util.module_from_spec(spec); spec.loader.exec_module(lf)
check("league_fixtures.py keeps strVenue (the game's card says where it is played)", "strVenue" in lf.KEEP)

# the broadcast table
spec = importlib.util.spec_from_file_location("bc", ROOT / ".github/tools/broadcast_check.py")
bc = importlib.util.module_from_spec(spec); spec.loader.exec_module(bc)
data = json.loads(read(ROOT / "assets/shared/broadcast.json"))
check("broadcast.json: a list of rows (none until a source has been opened and read: the proposal's rows could not be checked)",
      isinstance(data.get("rows"), list) and all(r.get("verified") for r in data["rows"]), data.get("rows"))
row = {"idLeague": "4668", "competition": "Saudi Pro League", "ar": "ثمانية", "en": "Thmanyah", "from": "2025-26", "until": "2030-31",
       "source": "https://example.org/a", "confidence": "high", "whole": True, "verified": "2026-11-02"}
day = datetime.date(2026, 11, 2)
check("broadcast_check: a good row passes", bc.problems([row], day) == ([], []), bc.problems([row], day))
for why, bad in (("no source", {**row, "source": ""}), ("no end season", {**row, "until": ""}), ("a source that is no link", {**row, "source": "sportbusiness"}),
                 ("an unreadable season", {**row, "until": "2030-32"}), ("not verified", {**row, "verified": "yes"}), ("whole not a yes or no", {**row, "whole": "all"})):
    check(f"broadcast_check: fails on {why}", bool(bc.problems([bad], day)[0]), bc.problems([bad], day))
check("broadcast_check: a row whose last season has ended fails (CI flags it)", bool(bc.problems([{**row, "until": "2025-26"}], day)[0]))
check("broadcast_check: one ending within a month is a warning, not a failure",
      bc.problems([{**row, "until": "2026-27"}], datetime.date(2027, 7, 15)) == ([], [bc.problems([{**row, "until": "2026-27"}], datetime.date(2027, 7, 15))[1][0]]))
check("broadcast_check: two rows for one league fail", bool(bc.problems([row, row], day)[0]))
check("broadcast_check: a season ends with July of its second year, a calendar season with December",
      bc.season_end("2027-28") == datetime.date(2028, 7, 31) and bc.season_end("2026") == datetime.date(2026, 12, 31))
tpl = read(ROOT / "src/template.html")
check("the page and the checker read a season's end the same way (31 July, 31 December)",
      "m[2] ? (end ? Date.UTC(y + 1, 6, 31, 23, 59)" in tpl and "(end ? Date.UTC(y, 11, 31, 23, 59)" in tpl)
check("the page shows a mark only for a high, whole, verified, sourced row in season, as text", "x.confidence === 'high'" in tpl and "x.whole === true" in tpl
      and "/^\\d{4}-\\d{2}-\\d{2}$/.test(x.verified || '')" in tpl and "/^https:\\/\\//.test(x.source || '')" in tpl and '<span class="fmk">${esc(m.mark)}</span>' in tpl)
check("tests/check.py runs the checker", ".github/tools/broadcast_check.py" in read(ROOT / "tests/check.py"))

if not shutil.which("javac"):
    print("SKIP the pages' links: no javac here")
else:
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "HomeCardChoice.java"), str(ROOT / "tests/FootballRun.java")], check=True)
        out = dict(l.split(" ", 1) for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.FootballRun"], check=True, capture_output=True, text=True).stdout.splitlines())
    B = "file:///android_asset/web/"
    check("the football home's page: the settings without the card choice, widget=1&football=1",
          out["home"] == B + "tv.html#city=Riyadh&teams=136013&widget=1&football=1", out["home"])
    check("…the English page in English", out["homeEn"] == B + "tv-en.html#city=Riyadh&lang=en&widget=1&football=1", out["homeEn"])
    check("…and none when nothing is known (the classic home then)", out["homeNone"] == "null", out["homeNone"])
    check("«المباريات»: matchpage=1 and the chosen game", out["page"] == B + "tv.html#city=Riyadh&teams=136013&matchpage=1&event=2573409", out["page"])
    check("…a game id that is not a number is dropped", out["pageBad"] == B + "tv.html#city=Riyadh&matchpage=1", out["pageBad"])
    check("…with no settings at all, the page alone", out["pageNull"] == B + "tv.html#&matchpage=1", out["pageNull"])

print()
print(f"{failed} failed" if failed else "all football tests passed")
sys.exit(1 if failed else 0)
