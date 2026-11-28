#!/usr/bin/env python3
"""4.4 the owner (R-472) «حد الفرق»: the followed clubs stay at most 40 by default; the person may raise it to 80, then to no limit, only after a
clear warning («كثرة الفرق ممكن تبطّئ التلفزيون»). Lowering it removes no club. The decision on a JVM (tests/TeamCapRun.java), then every place.
Run: python3 tests/team_cap_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def read(p):
    return Path(p).read_text(encoding="utf-8")


if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "FollowLimit.java"), str(ROOT / "tests/TeamCapRun.java")], capture_output=True, text=True)
        check("FollowLimit and TeamCapRun compile on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            o = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.TeamCapRun"], capture_output=True)
            out = o.stdout.decode("utf-8")
            print(out, end="")
            check("every case of the decision", o.returncode == 0 and "BAD" not in out and out.count("ok ") >= 9, out[-300:])
else:
    print("SKIP the JVM part: no javac here")
prefs, st, rec, sh = read(APP / "Prefs.java"), read(APP / "SettingsActivity.java"), read(APP / "SaverReceiver.java"), read(ROOT / "optimize/saver.sh")
check("Prefs: 40 unless stored, every add and the page's question held to it",
      'int teamCap() { try { return FollowLimit.teamCap(sp.getInt("teamCap", FollowLimit.MAX_TEAMS)); }' in prefs
      and "followWarned(kind), teamCap());" in prefs and "followWarned(FollowLimit.TEAMS), teamCap()) != FollowLimit.ADD) return false;" in prefs)
pick = st.split("private void pickTeamCap() {", 1)[1].split("\n    }\n", 1)[0]
check("the row «حد الفرق» in «المباريات», after «الفرق المتابعة»",
      st.index('row("الفرق المتابعة ("') < st.index('row("حد الفرق"') < st.index('row("تطبيق المباريات"') and "pickTeamCap();" in st)
check("a higher limit only after the warning and «ارفع الحد»; «لا» changes nothing; a lower one at once",
      "if (!FollowLimit.raises(from, to)) { prefs.setTeamCap(to); return; }" in pick and "setTitle(FollowLimit.TEAM_CAP_WARNING)" in pick
      and '.setPositiveButton("ارفع الحد", (d, w) -> { prefs.setTeamCap(to); render(); })' in pick and '.setNegativeButton("لا", null)' in pick
      and pick.count("setTeamCap(") == 2)
check("the teams row counts against the chosen limit (none: the count alone)", '(prefs.teamCap() == 0 ? "" : "/" + arabicNum(prefs.teamCap()))' in st)
check("saver.sh teamcap 40|80|0 (the warning said to the agent), the receiver checks it and «show» says it",
      'teamcap) case "${2:-}" in' in sh and "80|0) echo \"Many teams can slow the TV down" in sh and 'set1 teamcap "$2"' in sh
      and 'teamCap.matches("40|80|0")' in rec and "p.setTeamCap(Integer.parseInt(teamCap));" in rec and '"\\nteamcap: " + p.teamCap()' in rec)
check("saver.sh teams: trimmed to the TV's limit (asked only past 40; an older app: 40)", "sed -n 's/^teamcap: *//p'" in sh and 'case "$cap" in 80|0) ;; *) cap=40 ;; esac' in sh)
check("the page reads the whole list (the screensaver keeps the ceiling)", ".slice(0, 40)," not in read(ROOT / "src/template.html").split("teamIds:", 1)[1].split("sportsKey", 1)[0])
docs = read(ROOT / "docs/SETTINGS.md")
check("docs/SETTINGS.md has the row and the command", "saver.sh teamcap" in docs and "حد الفرق" in docs and "حد الفرق" in read(ROOT / "docs/SETTINGS-TREE.md"))
sys.exit(1 if failed else 0)
