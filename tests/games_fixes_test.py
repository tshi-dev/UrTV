#!/usr/bin/env python3
"""The tester's ten game bugs of build 629 (tester/GAMES-REVIEW.md, ranked top 10), each pinned: the Android side by its source (there is no emulator here),
the readable colours by computing them, «بدون كلام»'s cover by the JVM test in tests/PartyGamesTest.java (run by tests/party_games_test.py)."""
import json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def src(n): return (APP / n).read_text(encoding="utf-8")
def code(s): return re.sub(r"/\*.*?\*/|//[^\n]*", "", s, flags=re.S)
base, hub, reg = src("GameActivity.java"), src("QuizGamesActivity.java"), src("GameRegistry.java")
tsv = {l.split("\t")[0]: l.split("\t")[1] for l in (ROOT / "i18n/en/saver.tsv").read_text(encoding="utf-8").split("\n") if "\t" in l}
words = src("Words.java")

# 1: the badge says how a game is played
need = set(re.findall(r'new Game\("([\w-]+)", "[^"]+", "[^"]+", "\w+", true, true\)', reg))
free = set(re.findall(r'new Game\("([\w-]+)", "[^"]+", "[^"]+", "\w+", false, true\)', reg)) - {"packs"}
check("1: «جوالات» for the games that need phones (the registry's `phones` flag: «الأسرع» and the party games), «ريموت أو جوالات» for the older games that have both, «ريموت» for the one-remote games",
      "static boolean needsPhones(String id)" in reg and {"quiz", "forehead", "night", "draw"} <= need and {"khanat", "domino", "word5", "charades"} <= free and not (need & free)
      and "int way = GameRegistry.way(g.id);" in hub and "static int way(String id) { return needsPhones(id) ? 2 : inPhones(id) ? 1 : 0; }" in reg and 'id.startsWith("e:") || id.startsWith("e-")' in reg)
check("1: the tile's word is a square (no pill, no rounded shape), ONE quiet outlined style for the three words (no saffron fill); the setup page says the same at its end corner (the mode row turns it to «جوالات»)",
      "setCornerRadius" not in code(hub + base) and "mb.setColor(0)" in hub and "L.saff" not in hub[hub.index("int way = GameRegistry.way"):hub.index("tile.addView(mark, kp);", hub.index("int way = GameRegistry.way"))]
      and "ph.picked == 1 ? 2 : 0" in base and "wayWord(way)" in base)
check("1: both words have their English", all(tsv.get(w) for w in ("جوالات", "ريموت", "ريموت أو جوالات")) and all(f'S.n("{w}")' in words for w in ("جوالات", "ريموت", "ريموت أو جوالات")))

# 2: «ابدأ» always starts
b = code(base)
check("2: OK on «ابدأ» starts the game; the names are only the «الأسماء» button beside it", "namesAsked(this, namesKind())" not in b and "if (c.btn == 1) { openNames(); return false; }" in b
      and "return true;" in b[b.index("if (c.onButton()) {"):b.index("if (c.onButton()) {") + 200])

# 3: Domino: the opponents' tiles are drawn backs
dom = src("DominoActivity.java"); cv = dom[dom.index("final class CountsView"):]
check("3: the other players' hands are small solid tile backs with a line across (an empty outline was read as missing glyphs)",
      "paint.setColor(L.deep); c.drawRect(lo, px(3), hi, px(37), paint);" in cv and "paint.setColor(L.onDeep)" in cv and "paint.setColor(L.tile); c.drawRect" not in cv)

# 4: the hex letters are readable on every palette
look = src("GameLook.java")
def lum(c):
    r, g, b2 = ((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255
    f = lambda v: v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
    return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b2)
def ratio(a, b2): x, y = sorted((lum(a), lum(b2)), reverse=True); return (x + 0.05) / (y + 0.05)
sets = re.findall(r"new GameLook\((?:false|true), (0x[0-9A-Fa-f]{8}), (0x[0-9A-Fa-f]{8}), (0x[0-9A-Fa-f]{8}), (0x[0-9A-Fa-f]{8}), (0x[0-9A-Fa-f]{8}), (0x[0-9A-Fa-f]{8}), (0x[0-9A-Fa-f]{8})", look)
sets = [[int(x, 16) for x in s] for s in sets]   # paper, ink, tile, deep, tickOff, saff, onSaff
side_sets = [[int(x, 16) for x in re.findall(r"0x[0-9A-Fa-f]{8}", m)] for m in re.findall(r"SIDES(?:_NIGHT)? = \{([^}]*)\}", look)]
hive = src("LetterHiveActivity.java")
check("4: «خلية الحروف»: a free cell's letter is the ink (a free cell is the tile's colour, dark at night), a taken or focused cell's letter is the dark word on a bright colour",
      "ink.setColor(owner == HiveBoard.FREE && !focused ? L.ink : L.onSaff)" in hive and "ink.setColor(L.onSaff)" not in hive)
check("4: and on all four palettes the ink on the tile and the dark word on every side's colour and on saffron read at 4.5:1 or more",
      len(sets) == 4 and all(ratio(s[1], s[2]) >= 4.5 and ratio(s[6], s[5]) >= 4.5 for s in sets)
      and all(ratio(sets[i][6], c) >= 4.5 for i, sd in ((0, side_sets[0]), (1, side_sets[1]), (2, side_sets[1]), (3, side_sets[1])) for c in sd),
      [round(ratio(s[1], s[2]), 1) for s in sets])
check("4: the old colour (the dark word on every cell) was unreadable on a night tile, which is what the tester saw", all(ratio(s[6], s[2]) < 3 for s in sets[1:]), [round(ratio(s[6], s[2]), 1) for s in sets])

# 5: «بدون كلام»: no word on the TV by itself
ch = src("CharadesActivity.java"); eng = src("games/charades/CharadesGame.java")
check("5: a new word waits behind a cover on one remote (OK or ▲ shows it, the clock stands still while it is open, never with phones); the screen says so and shows nothing of the word",
      "public boolean coverNext = true;" in eng and "if (coverNext) { visible = false; covered = true; }" in eng and "held = Math.max(0, deadline - clock.now())" in eng and "game.coverNext = !onPhones();" in ch
      and "game.covered() && !onPhones()" in ch and 'S.t("الممثل: اضغط اختيار لتشوفها")' in ch and "game.uncover()" in ch)
check("5: its words have their English", all(tsv.get(w) for w in ("الكلمة الجاية جاهزة", "الممثل: اضغط اختيار لتشوفها", "الباقين: غطّوا عيونكم قبل ما يضغط")))

# 6: the letter «ه» and the digit «٥», the zero: the cause is the script, not a missing glyph
kh = src("KhanatActivity.java"); table = re.search(r'LETTERS = "([^"]+)"', kh).group(1).split(" ")
names = {t[0]: t[1:] for t in table}
kd = json.loads((ROOT / "assets/games/khanat.json").read_text(encoding="utf-8"))
used = set(kd["L"]) | set(kd["R"])
check("6: every letter of «خمس خانات» has its name (ألف … ياء), said under the big letter and in the writing line; the letter «ه» says «هاء»",
      used <= set(names) and names.get("ه") == "هاء" and names.get("ا") == "ألف" and len(set(names.values())) >= 27 and "letterName(game.letter, en)" in kh and 'S.t("كلمة لكل خانة تبدأ بحرف %s"' in kh)

# 7: «خمّن الكلمة»
w5 = src("Word5Activity.java"); w5c = code(w5)
check("7: a colour key is a shortcut (green «إدخال», red «حذف»): the base takes the remote's four colour keys, the game answers them", "KeyEvent.KEYCODE_PROG_GREEN" in base and "protected boolean onColourKey(int colour)" in base and "if (colour == 2)" in w5c and "enterGuess();" in w5c.split("onColourKey")[1][:300] and "if (colour == 1)" in w5c)
check("7: a guess that is not in the list says so once (a typo like «ابتثج» costs no try) and the same word entered again is taken (the list is short and refused real words before)",
      "!words.accepts(game.typing()) && !game.typing().equals(maybe)" in w5c and "maybe = game.typing();" in w5c and 'S.t("مو في قائمة الكلمات، إدخال مرة ثانية إذا أنت متأكد")' in w5c)
check("7: Back pauses at once (it no longer deletes letters first); the last hint is «توقف»", "if (back) { openPause(); return; }" in w5c and "game.delete()) { message = \"\"; typedChanged(); } else openPause()" not in w5c and 'S.t("رجوع"), S.t("توقف")' in w5c)
check("7: the words have their English", all(tsv.get(w) for w in ("مو في قائمة الكلمات، إدخال مرة ثانية إذا أنت متأكد", "أخضر")))

# 8: the how-to steps
hv = src("GameHowToView.java")
check("8: a step's text keeps its last line whole (font padding on, 12 px under it, no clip), so the next number box never covers it", "s.setIncludeFontPadding(true); s.setPadding(0, 0, 0, px(12));" in hv and "step.setClipChildren(false)" in hv)

# 9: the room page
lob = base[base.index("protected final void drawPhoneLobby"):]
check("9: the room page's header is the one short line (the long one was cut after «اكتب»); the instruction and the ADDRESS stand big in the middle (as large as 56 design pixels, in its own box, Latin digits, the code inside it: no separate code box)",
      'title(S.t("اللعب بالجوالات"), S.t("امسح الرمز بكاميرا الجوال"))' in lob and 'S.t("ما فتح؟ اكتب هذا العنوان في متصفح الجوال")' in lob and "float us = 56; while (us > 30 && measure(url, us) > 552 - 40) us -= 2;" in lob and "phones.code" not in lob and "codeBox" not in lob)
check("9: its words have their English", all(tsv.get(w) for w in ("امسح الرمز بكاميرا الجوال", "ما فتح؟ اكتب هذا العنوان في متصفح الجوال")))

# 10: the rows are never under the fold
su = base[base.index("protected final void setup("):base.index("private float chipRow")]
check("10: every row of the setup page fits above the start button (a row is 88 down to 56 px high), so «اللعب بالجوالات» and «الصوت» are never under the fold; the scroll stays for a game with more than nine rows",
      "float rowH = Math.min(88, Math.max(56, (840 - y) / Math.max(1, n) - gap))" in su and "(int) ((840 - y) / step + 0.02f)" in su and 'centered("▼"' in su)
# the rows' height by the same arithmetic, for each game's setup (the rows each game really has are counted from its chips)
def fits(n): y = 224 if n > 6 else 232; h = min(88, max(56, (840 - y) / n - 12)); return int((840 - y) / (h + 12) + 0.02) >= n
counts = {}
for f in sorted(APP.glob("*Activity.java")):
    t = f.read_text(encoding="utf-8"); n = len(re.findall(r"chips\.add\(|\.add\(\"", t))
    if "extends GameActivity" in t: counts[f.name] = n
check("10: with the sound row and the phones row, a game's rows (counted from its chips.add calls) all fit: nine or fewer", all(fits(n + 2 if n else 2) for n in counts.values() if n + 2 <= 9) and max(counts.values()) <= 9, counts)

print("failed" if failed else "ok"); sys.exit(1 if failed else 0)
