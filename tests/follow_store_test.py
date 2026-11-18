"""4.3.3: following from «المباريات» when the screensaver app is missing or older. The home keeps the team or league (HomeFollows), joins it to the
page's lists none twice (FollowMerge, run here on a plain JVM), and hands it to the screensaver the next time the page opens (sync); the
screensaver's own answer is final when it answers. The page's side: tests/matches_archive.test.mjs.   Run: python3 tests/follow_store_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
read = lambda p: p.read_text(encoding="utf-8")
hf, ma, prov, prefs = read(HOME / "HomeFollows.java"), read(HOME / "MatchesActivity.java"), read(APP / "ConfigProvider.java"), read(APP / "Prefs.java")

check("the bridge: the screensaver's answer when it answers (its no is final: there already, or full); missing or older: the home keeps it",
      'return saver != null ? saver : HomeFollows.addTeam(c, id, name);' in ma and 'return saver != null ? saver : HomeFollows.addLeague(c, id, ar, en, token);' in ma
      and 'return out == null || !out.containsKey("ok") ? null : out.getBoolean("ok");' in hf)
check("unfollow takes it from both lists", "boolean home = HomeFollows.removeTeam(c, id);" in ma and "boolean home = HomeFollows.removeLeague(c, id);" in ma)
check("the page opens: the kept follows go to the screensaver first, then what is still kept joins the page's lists",
      ma.index("new Thread(() -> HomeFollows.sync(syncApp), \"follows-sync\").start();") < ma.index("String frag = HomeFollows.fragment(this, cfg.fragment);") < ma.index("HomeCardChoice.matchesUrl(frag, event)"))
check("sync: one the screensaver has already, or takes now, leaves the home's list; one it cannot take stays; nothing when it does not answer",
      "if (frag == null) return;" in hf and "if (!ids.contains(FollowMerge.key(t)) && !ok(c, \"phone_follow\"" in hf and "if (!FollowMerge.hasLeague(toks, l) && !ok(c, \"phone_follow_league\"" in hf)
check("the screensaver takes a league: a built-in one by its English name, else the catalogue's entry; never twice; past 8 only after the warning, up to 24 (4.4)",
      'case "phone_follow_league": {' in prov and 'case "phone_unfollow_league": {' in prov and "String e = b != null ? b[0] : CatalogueLeagues.entry(id, ar, en);" in prefs
      and "for (String s : v) if (sameLeague(s, id, en)) return false;" in prefs and "if (FollowLimit.check(FollowLimit.LEAGUES, v.size(), followWarned(FollowLimit.LEAGUES)) != FollowLimit.ADD) return false;" in prefs)
check("…behind the launcher's own permission, as the other phone_ calls", prov.index("c.checkCallingPermission(PHONE_PERMISSION)") < prov.index('case "phone_follow_league"'))

if not shutil.which("javac"):
    print("SKIP the merge on a JVM: no javac here")
else:
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "FollowMerge.java"), str(ROOT / "tests/FollowMergeRun.java")], check=True, capture_output=True)
        out = dict(l.split(" ", 1) for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.FollowMergeRun"], capture_output=True, text=True, check=True).stdout.splitlines())
    q = lambda f: dict(kv.split("=", 1) for kv in f.split("&"))
    check("nothing kept and no fragment: none (the page's defaults, as before)", out["nothing"] == "null", out["nothing"])
    check("nothing kept: the screensaver's fragment exactly as it was", out["asIs"] == "city=X&teams=1,2&leagues=English%20Premier%20League&panel=verse", out["asIs"])
    ns = q(out["noSaver"])
    check("no screensaver: the kept teams and leagues alone (the three teams, a built-in league by its name, a catalogue one with its name)",
          ns["teams"] == "136013,136014,136015" and unquote(ns["leagues"]) == "Dutch Eredivisie,id4396" and unquote(ns["lgn"]) == "4396~English League 1", out["noSaver"])
    j = q(out["joined"])
    check("joined: the screensaver's first, none twice (a team, a built-in league by its id, a catalogue league), the new one's name added",
          j["teams"] == "136013,9,136015" and unquote(j["leagues"]) == "English Premier League,id4719,id5000" and unquote(j["lgn"]) == "4719~آسيا|5000~كأس" and j["city"] == "X" and j["panel"] == "verse", out["joined"])
    check("a space is %20, never + (the page reads + as a space)", "+" not in out["joined"] and "%20" in out["joined"])
    check("40 teams (4.4: the ceiling): no 41st", q(out["fullTeams"])["teams"].count(",") == 39 and "999" not in out["fullTeams"], out["fullTeams"])
    check("add: the same id never twice, a name loses the list's separators, a bad id or a full list changes nothing", out["add"] == "[1|a] [1|a, 2|b c d] null null", out["add"])
    check("a league entry: its token checked (a built-in name, else id + its id), a bad id refused", out["league"] == "4337|هولندا|Dutch Eredivisie|Dutch Eredivisie 4337|a|b|id4337 null", out["league"])
    check("remove by id", out["remove"] == "[2|b]", out["remove"])
sys.exit(1 if failed else 0)
