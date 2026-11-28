#!/usr/bin/env python3
"""The phone / no-phone flow of the games (docs/research/GAMES-FLOW.md): ONE choice in ONE place, on the game's own page.
1. «كيف تلعبون؟» (ريموت واحد / جوالات) is the first row of the seven older games, kept per game; the list has no phones filter and no extra.
2. With phones the rows that the phones decide (the players) and «الأسماء» are not shown; the focus passes over them (JVM: GameChipsFlowTest).
3. The room is one block: the address in Latin digits (the code inside it), the name tiles, «ناقص …» only while short, «ابدأ» (the focus, grey while short), no «أغلق الغرفة»;
   «نلعب بالريموت» only for a game that has a remote way; a phone-only game shows «ابدأ» alone.
4. The tile and the setup page carry one quiet outlined tag: «ريموت», «جوالات» or «ريموت أو جوالات».
Run: python3 tests/games_flow_test.py   (needs javac)"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def src(n): return (APP / n).read_text(encoding="utf-8")

if not shutil.which("javac"):
    print("SKIP games flow: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    c = subprocess.run(["javac", "-nowarn", "-d", tmp, str(APP / "GameChips.java"), str(ROOT / "tests/GameChipsFlowTest.java")], capture_output=True, text=True)
    check("the chips with the mode row and the hidden rows compile on a plain JVM", c.returncode == 0, c.stderr[-600:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.GameChipsFlowTest"], capture_output=True, text=True)
        for line in r.stdout.splitlines():
            if line.startswith(("PASS", "FAIL")):
                print(line); failed += line.startswith("FAIL")
        check("the JVM test ran to the end", r.returncode == 0, r.stderr[-400:])

base, hub, reg = src("GameActivity.java"), src("QuizGamesActivity.java"), src("GameRegistry.java")
OLD = {"Khanat": ["count"], "MissingWord": ["teams"], "OddOne": ["players"], "Domino": ["people", "bots"], "Charades": [], "LetterHive": [], "Word5": []}
for n, hid in OLD.items():
    a = src(n + "Activity.java")
    check(f"{n}: the mode row is the shared «كيف تلعبون؟», the old switch and the list's extra are gone",
          "modeChip(chips);" in a and "اللعب بالجوالات" not in a and 'getBooleanExtra("phones"' not in a and "اللعب بالريموت بس" not in a)
    check(f"{n}: its way out of the room starts the same game on the remote, and the chips are the game's own",
          "protected Runnable remoteWay() { return () -> { screen = Screen.SETUP; redraw(); start(); }; }" in a and "protected GameChips lobbyChips() { return chips; }" in a)
    check(f"{n}: the rows the phones decide are skipped with phones: {hid or 'none'}",
          (("phoneHidden() { return new String[] {" + ", ".join(f'"{k}"' for k in hid) + "}; }") in a) if hid else "phoneHidden" not in a)
check("the base: the first row, kept per game, applies the hidden rows and no names with phones",
      'c.add("phones", S.t("كيف تلعبون؟"), new String[]{S.t("ريموت واحد"), S.t("جوالات")}, 0)' in base and 'c.first("phones"); phoneRows(c);' in base
      and 'c.names = namesKind() != null && !ph;' in base and "for (String k : phoneHidden()) c.hide(k, ph);" in base)
check("the list: no filter, no phones extra, no sentence in the header, the tile tag is one quiet outline",
      "filter" not in hub and 'putExtra("phones"' not in hub and "TextView mode" not in hub and "mb.setColor(0)" in hub and "GameRegistry.way(g.id)" in hub)
check("the registry says how a game can be played: 2 phones only, 1 both, 0 the remote", "static int way(String id) { return needsPhones(id) ? 2 : inPhones(id) ? 1 : 0; }" in reg)
check("the tag's three words", all(w in base for w in ('S.t("جوالات")', 'S.t("ريموت أو جوالات")', 'S.t("ريموت")')))
lob = base[base.index("protected final void drawPhoneLobby"):]
check("the room: no «أغلق الغرفة» BUTTON (only the footer names what Back does), no heading, no code box, the address is Latin and the code is inside it",
      'button(S.t("أغلق الغرفة")' not in lob and "اللي دخلوا" not in lob and "codeBox" not in lob and "phones.code" not in lob and 'url = phones.url.replace("http://", "")' in lob and "TEXT_DIRECTION_LTR" in lob)
check("the room (UI review at 720p, font 1.3): the address is never wider than the QR (360) and breaks before «/» when it must, «ناقص …» is in the middle column with a gap from the name tiles, the screen title stands 14 px higher so its tails clear the band (no padded line box: it clipped the dots under ب and ج)",
      "measure(url, us) > 360 - 32" in lob and 'url.substring(slash)' in lob and "at(552, 72, 504, 250)" in lob and "root.addView(text(big, 96, L.onDeep, true), at(1100, 130, 96, 40));" in base and "t.setPadding(0, 0, 0, px(14))" not in base)
check("the room (UI review of the real build): «ما فتح؟ …» sits right under the address box, and the setup header has no tag where the row «كيف تلعبون؟» says it",
      'root.addView(how, at(360, 76, 96, 766));' in lob and "if (ph == null) {" in base and "wayWord(way), 30" in base)
check("the room: the count only while short («ناقص لاعب» for one, «ناقص ٢» for more)",
      "if (!can) {" in lob and 'missing == 1 ? S.t("ناقص لاعب") : S.t("ناقص %s", num(missing))' in lob)
check("the room: «ابدأ» has the focus even while it is grey; «نلعب بالريموت» only when the game has a remote way",
      'button(S.t("ابدأ"), lobbyBtn == 0)' in lob and "if (!can) go.setAlpha(.55f);" in lob and 'if (way) root.addView(button(S.t("نلعب بالريموت"), lobbyBtn == 1)' in lob and "boolean can = missing <= 0, way = remoteWay() != null;" in lob)
check("the room's way out: closes the room, remembers the remote for this game, starts the game on it; Back is the other way (the footer says «رجوع» «أغلق الغرفة»: what happens)",
      "closePhones(); go.run();" in base and 'c.get("phones").picked = 0; GamePrefs.saveChips(this, id(), c.save());' in base and 'S.t("رجوع"), S.t("أغلق الغرفة"))' in lob
      and "else if (!lobbyKey(ok, dx)) onKey(back, ok, dx, dy);" in base)
for n in ("Forehead", "Liar", "Night", "Draw", "Link", "Author", "Closest", "Emoji", "Guess", "Math", "Logic", "QuranVerse", "QuranReciter", "EngineGame"):
    try: a = src(n + "Activity.java")
    except FileNotFoundError: continue
    check(f"{n}: a phone-only game has no way out of the room (only «ابدأ»)", "remoteWay()" not in a)
tsv = {l.split("\t")[0]: l.rstrip("\n").split("\t")[1] for l in (ROOT / "i18n/en/saver.tsv").read_text(encoding="utf-8").splitlines() if "\t" in l}
words = src("Words.java")
new = ["كيف تلعبون؟", "ريموت واحد", "جوالات", "ريموت أو جوالات", "ناقص لاعب", "نلعب بالريموت"]
check("the new words have their English and their mark", all(tsv.get(w) and f'S.n("{w}")' in words for w in new) and tsv.get("ناقص %s") and 'S.n("ناقص %s")' in words)
print("ok" if failed == 0 else "failed")
sys.exit(1 if failed else 0)
