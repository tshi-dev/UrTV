#!/usr/bin/env python3
"""4.3 A1: a game TheSportsDB marks postponed or abandoned (its short codes PST and ABD, and AWD for awarded) is never shown as live: the page's
CALLED_OFF knows them, every list that shows games uses it (a followed team, a league, «المباريات», the events), and the built pages carry it.
    python3 tests/called_off_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

for f in ("src/template.html", "dist/tv.html", "dist/tv-en.html"):
    t = (ROOT / f).read_text(encoding="utf-8")
    m = re.search(r"const CALLED_OFF = /(.*?)/i;", t)
    check(f"{f}: CALLED_OFF is there", m is not None)
    if not m:
        continue
    rx = re.compile(m.group(1), re.I)
    off = ["PST", "ABD", "AWD", "Postponed", "POSTP", "CANC", "Cancelled", "Abandoned", "ABAN", "SUSP", "Suspended", "WO", "POST", "INT",
           "Interrupted", "Match Postponed", "Match Cancelled", "Match Abandoned", "Match Suspended", "Awarded", "W.O"]
    on = ["NS", "1H", "2H", "HT", "ET", "P", "LIVE", "FT", "AET", "PEN", "Match Finished", "", "Not Started", "BT", "TBD", "Time To Be Defined",
          "In Progress", "Second Half", "First Half", "Break Time", "Extra Time", "Penalty In Progress", "1st Half", "Half Time"]
    check(f"{f}: postponed, abandoned, awarded, cancelled, suspended and walk-over are called off", all(rx.match(s) for s in off), [s for s in off if not rx.match(s)])
    check(f"{f}: a game not started, under way, at half time or finished is not", not any(rx.match(s) for s in on), [s for s in on if rx.match(s)])
    over = t[t.index("function eventsOf("):t.index("function eventWhen(")]
    check(f"{f}: the events' list leaves called-off ones out too", "calledOff(e)" in over, over[:200])
    check(f"{f}: every list of games asks calledOff (result, pending, team card, team week, team today, league, the matches page, loadLive, events)",
          t.count("calledOff(") >= 14, t.count("calledOff("))
    check(f"{f}: loadLive never asks for a called-off game or one with no time", "calledOff(ev) || timeTbd(ev)) return false" in t)
    check(f"{f}: A3: two empty «next games» answers in a row drop a kept fixture that has not kicked off", "(NEXT_EMPTY[id] = (NEXT_EMPTY[id] || 0) + 1) >= 2" in t and "delete MATCHES[id]" in t)
    check(f"{f}: A2: the matches page reads each game from the copy further on, not the league file's always", "matchEvents().map(ev => lg.has(" not in t)
    check(f"{f}: a postponed game first in eventsnext does not hide the next real one", "evs.find(ev => ev && !calledOff(ev))" in t)

# behaviour, in node: the page's own functions
import shutil, subprocess, json
t = (ROOT / "src/template.html").read_text(encoding="utf-8")
def grab(start, end):
    i = t.index(start); return t[i:t.index(end, i)]
if shutil.which("node"):
    code = (grab("const FINAL = ", "// how long a game lasts") + grab("const DUR_H", "// A game past its end") + grab("function finishedRecently", "// A game past its end")
            + grab("function pendingResult", "// the order of the screen") + grab("const scoredEv", "// a followed team's coming games"))
    js = """
const CONFIG = {leagues: ['x'], weekWindowDays: 7};
let LG = [];
const leagueGames = () => LG;
function toDate(ev){ const s = ev.strTimestamp || `${ev.dateEvent}T${ev.strTime || '00:00:00'}`; return new Date(s.endsWith('Z') ? s : s+'Z'); }
const RESULTS = {}, KEPT = {}, MATCHES = {};
""" + code + """
const H = 3600000, now = Date.parse('2026-11-08T22:00:00Z');
const out = {};
out.pst = calledOff({strStatus: 'PST'}) && calledOff({strStatus: 'Match Postponed'}) && calledOff({strStatus: 'NS', strPostponed: 'yes'});
out.notOff = !calledOff({strStatus: 'NS', strPostponed: 'no'}) && !calledOff({strStatus: 'FT'}) && !calledOff({strStatus: '1H'});
out.words = [offWord({strStatus: 'PST'}), offWord({strStatus: 'Match Cancelled'}), offWord({strStatus: 'ABD'}), offWord({strStatus: 'NS', strPostponed: 'yes'})];
out.tbd = timeTbd({strStatus: 'TBD'}) && !timeTbd({strStatus: 'NS'});
// a postponed game past its time is no result waiting
out.pending = pendingResult({idEvent: '1', strStatus: 'PST', strTimestamp: '2026-11-08T15:00:00'}, now);
out.pendingNs = pendingResult({idEvent: '1', strStatus: 'NS', strTimestamp: '2026-11-08T15:00:00'}, now);
// Barcelona's case: eventslast lags (no Atletico game), the league file has it FT
RESULTS['133739'] = [{idEvent: '9', strStatus: 'FT', intHomeScore: '3', intAwayScore: '0', strTimestamp: '2026-11-01T20:00:00', idHomeTeam: '133739', idAwayTeam: '5'}];
MATCHES['133739'] = {idEvent: '10', strStatus: 'NS', intHomeScore: null, intAwayScore: null, strTimestamp: '2026-11-08T17:30:00', idHomeTeam: '134', idAwayTeam: '133739'};
LG = [{idEvent: '10', strStatus: 'FT', intHomeScore: '1', intAwayScore: '2', strTimestamp: '2026-11-08T17:30:00', idHomeTeam: '134', idAwayTeam: '133739'},
      {idEvent: '11', strStatus: 'FT', intHomeScore: '0', intAwayScore: '0', strTimestamp: '2026-11-07T17:30:00', idHomeTeam: '7', idAwayTeam: '8'}];
const lg = new Map(LG.map(e => [String(e.idEvent), e]));
const rs = resultsFor('133739', lg);
const g = rs.find(e => e.idEvent === '10');
out.fresh = g && g.strStatus === 'FT' && g.intAwayScore === '2' && finishedRecently(g, now);
out.otherLeagueGame = rs.some(e => e.idEvent === '11');
// a team's live copy at minute 70 is not undone by an older league file at minute 10
out.keepLive = fresher({idEvent: '12', strStatus: '2H', intHomeScore: '1', intAwayScore: '1', strProgress: '70'}, new Map([['12', {idEvent: '12', strStatus: '1H', intHomeScore: '0', intAwayScore: '0', strProgress: '10'}]])).strProgress === '70';
// the league file says postponed: that wins over the team's «NS»
out.offWins = calledOff(fresher({idEvent: '13', strStatus: 'NS'}, new Map([['13', {idEvent: '13', strStatus: 'Match Postponed'}]])));
// step 29: a team's own «postponed» copy of a league game does not take it away: the league file's copy is read
out.step29 = !calledOff(fresher({idEvent: '15', strStatus: 'Match Postponed', dateEvent: '2026-11-09'}, new Map([['15', {idEvent: '15', strStatus: 'NS', dateEvent: '2026-11-04'}]])));
// the team's copy that wins keeps the league's crest
out.crest = fresher({idEvent: '16', strStatus: 'FT', intHomeScore: '1', intAwayScore: '0'}, new Map([['16', {idEvent: '16', strStatus: 'NS', strHomeTeamBadge: 'b.png'}]])).strHomeTeamBadge === 'b.png';
// an awarded game with a score is not shown as a played result
out.awarded = finishedRecently({idEvent: '14', strStatus: 'AWD', intHomeScore: '3', intAwayScore: '0', strTimestamp: '2026-11-08T15:00:00'}, now);
console.log(JSON.stringify(out));
"""
    r = subprocess.run(["node", "-e", js], capture_output=True, text=True)
    try:
        o = json.loads(r.stdout.strip().splitlines()[-1])
    except Exception:
        o = None
    check("node: the page's functions run", o is not None, r.stderr[-600:])
    if o:
        check("PST, «Match Postponed» and strPostponed yes are called off", o["pst"])
        check("not started, finished and under way are not", o["notOff"])
        check("the words: postponed, called off, stopped", o["words"] == ["تأجلت", "أُلغيت", "توقفت", "تأجلت"], o["words"])
        check("TBD is a game with no time", o["tbd"])
        check("a postponed game past its time is no result waiting; a not-started one is", not o["pending"] and o["pendingNs"], (o["pending"], o["pendingNs"]))
        check("A2: a followed team's game is read from the league file when that is further on (FT there, NS in the team's)", o["fresh"])
        check("A2: a game of the team's that only the league file has is among its results; another's is not", not o["otherLeagueGame"])
        check("A2: a team's later live copy is kept over an older league file", o["keepLive"])
        check("A2: the league file's «postponed» wins over the team's «not started»", o["offWins"])
        check("step 29: a team's own «postponed» copy does not take a league's game away", o["step29"])
        check("A2: the team's copy that wins keeps the league file's crest", o["crest"])
        check("an awarded game is not shown as a played result", not o["awarded"])
else:
    print("SKIP node is not installed: the behaviour checks did not run")
sys.exit(1 if failed else 0)
