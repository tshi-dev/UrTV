#!/usr/bin/env python3
"""4.3.1: «فرقي» in the matches page's header opens the settings' teams chooser (the page is made again on return); not in kids mode.

    python3 tests/match_teams_button_test.py
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


page = (ROOT / "src/template.html").read_text(encoding="utf-8")
act = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MatchesActivity.java").read_text(encoding="utf-8")
saver = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
tsv = (ROOT / "i18n/en/page.tsv").read_text(encoding="utf-8")
check("the header has «فرقي» only where the app can open it (the bridge has openTeams), ▲ from the days reaches it, OK opens it, ▼ goes back to the days",
      "teams:'فرقي'" in page and "window.UrtvMatches.openTeams ?" in page and "k === 'ArrowUp' && document.getElementById('mp-teams')) MP.zone = 'head'" in page
      and "MP.zone === 'head'" in page and "window.UrtvMatches.openTeams()" in page and "MP.zone = 'days'; e.preventDefault(); return mpRender();" in page)
check("in the page's zone 'head' the days do not also show the focus when a day has no games", "(!MP.rows.length && MP.zone !== 'head')" in page)
check("its English word is in the table", "فرقي\tMy teams" in tsv)
check("the app opens the screensaver's settings on the teams chooser, never in kids mode, and makes the page again on return (the new teams)",
      'putExtra("manageTeams", true)' in act and "Parental.state(this).kids" in act and "if (teamsOpened) { teamsOpened = false; recreate(); }" in act)
check("the settings open the chooser on the extra, at start and when already open", 'getBooleanExtra("manageTeams", false)' in saver and "manageTeams();" in saver.split("onNewIntent", 1)[1][:500])
sys.exit(1 if failed else 0)
