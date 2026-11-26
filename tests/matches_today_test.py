"""«بطاقة المباريات لليوم فقط» (matchestoday): the setting from the command to the page. What the card then shows is
tests/page.test.mjs («matches today only»), the export and import tests/settings_move_test.py.
Run: python3 tests/matches_today_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    if not ok:
        failed += 1


def text(p):
    return Path(p).read_text(encoding="utf-8")


TITLE, ON, OFF = "بطاقة المباريات لليوم فقط", "يعمل، مباريات اليوم فقط، كل مباراة في يومها", "متوقف، أقرب مباريات الأسبوع"
prefs, rcv, st = text(SAVER / "Prefs.java"), text(SAVER / "SaverReceiver.java"), text(SAVER / "SettingsActivity.java")
check("Prefs: off by default, and #mtoday=1 joins the page's address only when on (the address is as before when off)",
      'boolean matchesToday() { return sp.getBoolean("matchesToday", false); }' in prefs
      and '+ (matchesToday() ? "&mtoday=1" : "")' in prefs.split("String fragment()")[1].split("\n    }\n")[0])
check("saver: matchestoday's word is checked before anything is set, then set, and `show` says it",
      'error: matchestoday is one of on off' in rcv and 'p.setMatchesToday("on".equals(matchesToday))' in rcv
      and '"\\nmatchestoday: " + (p.matchesToday() ? "on" : "off")' in rcv and "[--es matchestoday on|off]" in rcv
      and rcv.index("error: matchestoday is one of on off") < rcv.index("if (idle != null) {   // first"))
check("settings: the row is under «المباريات», after «الدوريات كاملة», and OK flips it",
      st.index('row("الدوريات كاملة"') < st.index(f'row("{TITLE}"') < st.index('header("العرض")')
      and f'prefs.matchesToday() ? "{ON}" : "{OFF}"' in st and "prefs.setMatchesToday(!prefs.matchesToday()); render();" in st)
words, hub, tsv = text(SAVER / "Words.java"), text(SAVER / "SettingsHub.java"), text(ROOT / "i18n/en/saver.tsv")
check("English: the row's three texts are words with an entry, and the row has its place in the settings' map",
      all(f'S.n("{w}")' in words and f"\n{w}\t" in tsv for w in (TITLE, ON, OFF)) and f'{{"{TITLE}", CONTENT}}' in hub)
sh = text(ROOT / "optimize/saver.sh")
check("saver.sh: matchestoday on|off, in the usage, a wrong word refused",
      "  matchestoday) case" in sh and 'set1 matchestoday "$2"' in sh and "usage: optimize/saver.sh matchestoday on|off" in sh
      and "optimize/saver.sh matchestoday on " in sh.split("set -euo")[0])
tpl = text(ROOT / "src/template.html")
check("page: #mtoday=1 is read, only a card of kind «المباريات» keeps today's games (the slot, the smart card and «يومك» do not)",
      "matchesToday: _Q.get('mtoday') === '1'," in tpl
      and "const todayOnly = card => !!(CONFIG.matchesToday && card && card.kind === 'matches');" in tpl
      and ".filter(x => !today || gameDay(x.ev, x.t) === today)" in tpl and "matchesEmptyText(card)" in tpl and "'لا مباريات اليوم'" in tpl)
check("page: a game's day is the city's (FMT_DATE); a game with no time set keeps its own date",
      "const gameDay = (ev, t) => timeTbd(ev) && ev.dateEvent ? ev.dateEvent : FMT_DATE.format(new Date(t));" in tpl)
for name in ("tv.html", "tv-en.html"):
    check(f"dist/{name} is built from this template (src/build.py)", "const todayOnly = card =>" in text(ROOT / "dist" / name))
docs = text(ROOT / "docs/SETTINGS.md")
check("docs/SETTINGS.md: a row says the command and where it is on the TV", "`optimize/saver.sh matchestoday on`" in docs and TITLE in docs)
sys.exit(1 if failed else 0)
