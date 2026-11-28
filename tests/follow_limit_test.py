#!/usr/bin/env python3
"""4.4 more than 8 leagues and 20 clubs (the owner): 8 and 20 stay the soft limits; the 9th league or 21st club shows ONE warning, once
(«إضافة أكثر من ٨ قد تبطّئ التلفزيون إذا كان ضعيف», «أضف» / «لا»; after «أضف» never again); at most 24 leagues and 40 clubs, with words that
say so at the ceiling (never a silent no). The decision runs on a JVM (tests/FollowLimitRun.java); every place a person adds one is pinned:
the settings (a club, the two lists of leagues), «الدوريات كاملة», the launcher's matches page (through the screensaver's provider).
The page's side (pacing, the bar, the warning's two OKs, 720p) is tests/many_follows.test.mjs.

    python3 tests/follow_limit_test.py
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "FollowLimit.java"), str(ROOT / "tests/FollowLimitRun.java")], capture_output=True, text=True)
        check("FollowLimit compiles on a plain JVM", r.returncode == 0, r.stderr[-300:])
        if r.returncode == 0:
            o = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.FollowLimitRun"], capture_output=True, text=True)
            bad = [l for l in o.stdout.splitlines() if l.startswith("FAIL")]
            check(f"the decision: add, the one warning, the ceiling ({o.stdout.count('PASS ')} cases)", o.returncode == 0 and not bad and o.stdout.count("PASS ") >= 6, bad[:3] or o.stderr[-300:])
else:
    print("SKIP the JVM run (no javac here)")

lim, prefs, st, la, prov = read(APP / "FollowLimit.java"), read(APP / "Prefs.java"), read(APP / "SettingsActivity.java"), read(APP / "LeaguesActivity.java"), read(APP / "ConfigProvider.java")
check("the numbers: 8 and 20 soft, 24 and 40 the ceiling, in one place", "SOFT_LEAGUES = 8, MAX_LEAGUES = 24, SOFT_TEAMS = 20, MAX_TEAMS = 40;" in lim
      and "SOFT_TEAMS = FollowLimit.SOFT_TEAMS, MAX_TEAMS = FollowLimit.MAX_TEAMS;" in prefs and "SOFT_LEAGUES = FollowLimit.SOFT_LEAGUES, MAX_LEAGUES = FollowLimit.MAX_LEAGUES;" in prefs)
check("once: «أضف» is kept per kind and never asked again", 'boolean followWarned(String kind) { return sp.getBoolean("followWarned_" + kind, false); }' in prefs
      and 'void setFollowWarned(String kind) { sp.edit().putBoolean("followWarned_" + kind, true).apply(); }' in prefs)
check("Prefs never adds past the soft limit before «أضف», nor past the ceiling", prefs.count("!= FollowLimit.ADD) return false;") == 2)
ask = st.split("private void askFollow(", 1)[1].split("\n    }\n", 1)[0]
check("the settings' warning: «أضف» remembers it and adds, «لا» does nothing; the ceiling says so in a dialog",
      "FollowLimit.warning(kind)" in ask and '.setPositiveButton("أضف", (d, w) -> { prefs.setFollowWarned(kind); go.run(); })' in ask and '.setNegativeButton("لا", null)' in ask
      and "FollowLimit.full(kind, prefs.teamCap())" in ask and "if (st == FollowLimit.ADD) { go.run(); return; }" in ask)
check("a club from the settings (a preset, a league's clubs, a search) goes through it; «إضافة فريق» is always there and says the ceiling",
      "askFollow(FollowLimit.TEAMS, () -> {" in st and 'b.setNeutralButton("إضافة فريق", (d, w) -> askFollow(FollowLimit.TEAMS, () -> pickTeam(0)));' in st)
tick = st.split("private void tickLeague(", 1)[1].split("\n    }\n", 1)[0]
check("both lists of leagues: a tick past 8 waits for «أضف», at 24 it stays off with the words (the count is the dialog's own)",
      st.count("tickLeague((AlertDialog) d, on, i, ") == 2 and "on[i] = false;" in tick and "FollowLimit.full(FollowLimit.LEAGUES)" in tick
      and "prefs.setFollowWarned(FollowLimit.LEAGUES); on[i] = true; d.getListView().setItemChecked(i, true);" in tick)
check("«الدوريات كاملة» (LeaguesActivity): the same warning and ceiling", "FollowLimit.check(FollowLimit.LEAGUES, v.size(), prefs.followWarned(FollowLimit.LEAGUES))" in la
      and "prefs.setFollowWarned(FollowLimit.LEAGUES); toggleLeague(stored, tag);" in la and "FollowLimit.full(FollowLimit.LEAGUES)" in la)
check("the launcher's page asks the screensaver (behind the same permission as every phone_ call) and tells it «أضف»",
      prov.index("c.checkCallingPermission(PHONE_PERMISSION)") < prov.index('case "phone_follow_check"') and 'out.putString("state", FollowLimit.word(p.followState(kind)));' in prov
      and 'case "phone_follow_warned"' in prov and "p.setFollowWarned(kind);" in prov)
ma, hf, fm = read(HOME / "MatchesActivity.java"), read(HOME / "HomeFollows.java"), read(HOME / "FollowMerge.java")
check("the launcher's bridge: followCheck and followWarned; an older screensaver answers nothing and the page keeps 8 and 20",
      "@JavascriptInterface public String followCheck(String kind) { return HomeFollows.state(c, kind); }" in ma and '"phone_follow_warned"' in ma
      and 'call(CONFIG, "phone_follow_check"' in hf and 'return s != null && s.matches("add|warn|full") ? s : "";' in hf and "MAX_TEAMS = 40, MAX_LEAGUES = 24;" in fm)
page = read(ROOT / "src/template.html")
check("the page: «تابع» on a club, a game's club and a league go through the gate", page.count("mpGate('teams'") == 2 and page.count("mpGate('leagues'") == 1
      and "if(!st) st = (kind === 'leagues' ? mpLeagueIds().length >= 8 : CONFIG.teamIds.length >= 20) ? 'old' : 'add';" in page)
check("agents keep their own limit, the ceiling (an agent's list is taken as given; 4.4 R-472: the ceiling the person chose, 40 by default)",
      'if (teams.size() > ceiling) return "error: at most " + ceiling + " teams' in read(APP / "SaverReceiver.java")
      and 'echo "At most $cap teams can be followed' in read(ROOT / "optimize/saver.sh") and "cap=40" in read(ROOT / "optimize/saver.sh"))
sys.exit(1 if failed else 0)
