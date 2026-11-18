"""4.3.3 A3: a person followed 3 teams and saw 1. Every step from the screensaver's list to the page carries all three: Prefs keeps up to 40 (20 is only where the TV warns)
and writes every id into the fragment, the provider hands the fragment over whole, the launcher's link keeps #teams=, and the page reads
them all and asks each team's games (the server's free answer and the archive's team file; tests/matches_archive.test.mjs draws them).
Run: python3 tests/three_teams_test.py"""
import re, shutil, subprocess, sys, tempfile
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
prefs, prov, settings, snap, page = read(APP / "Prefs.java"), read(APP / "ConfigProvider.java"), read(APP / "SettingsActivity.java"), read(HOME / "ConfigSnapshot.java"), read(ROOT / "src/template.html")

check("saver: a team is added at the end of the list (past 20 only after the warning, up to 40: 4.4 FollowLimit), never in place of another",
      "MAX_TEAMS = FollowLimit.MAX_TEAMS;" in prefs and "if (FollowLimit.check(FollowLimit.TEAMS, v.size(), followWarned(FollowLimit.TEAMS)) != FollowLimit.ADD) return false;" in prefs and 'v.add(id + "|" + name.replace("|", " ").replace(";", " "));' in prefs)
check("saver: the fragment carries every followed team's id", '+ "&teams=" + join(teamIds(), ",")' in prefs and "for (String t : teams()) ids.add(t.split(\"\\\\|\", 2)[0]);" in prefs)
check("saver: «الفرق المتابعة» keeps the ticked ones, all of them", "for (int i = 0; i < cur.size(); i++) if (keep[i]) v.add(cur.get(i));" in settings)
check("launcher: the provider's first column is the fragment, read whole", "fragment = cur.getString(0);" in snap)
check("page: #teams= read up to 40 ids (4.4: FollowLimit's ceiling)", ".map(x=>(x||'').trim()).filter(x=>/^\\d+$/.test(x)).slice(0, 40)," in page)
check("page: every followed team is asked (the server, then its archive file beside the queue; 4.4: past 20 every 2nd load, tests/many_follows.test.mjs)",
      "for(const [i, id] of CONFIG.teamIds.entries()){" in page and "if(!followDue(i, SOFT_TEAMS, !ASKED_TEAMS.has(id))) continue;" in page
      and "arch = inBatches(CONFIG.teamIds, 4, async id => { archN.push(await archTeam(id).catch(() => 0)); })" in page)
check("page: every followed team's games are in the page's list (the server's and the archive's)",
      "CONFIG.teamIds.forEach(id => {\n    const next = (NEXT_LIST[id] || (MATCHES[id] ? [MATCHES[id]] : [])).concat(archOf(id, 'next'));" in page)

if not shutil.which("javac"):
    print("SKIP the launcher's link: no javac here")
else:
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "HomeCardChoice.java"), str(ROOT / "tests/ThreeTeamsRun.java")], check=True)
        out = dict(l.split(" ", 1) for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.ThreeTeamsRun"], capture_output=True, text=True, check=True).stdout.splitlines())
    check("launcher: «المباريات»'s link keeps the three ids", re.search(r"[#&]teams=136013,136014,136015(&|$)", out["page"]) and "matchpage=1" in out["page"], out["page"])
    check("launcher: …with a game to open too", "teams=136013,136014,136015" in out["event"] and out["event"].endswith("&event=11"), out["event"])
sys.exit(1 if failed else 0)
