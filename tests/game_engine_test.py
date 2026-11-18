#!/usr/bin/env python3
"""The games engine and its remote release switch (docs/GAME-ENGINE.md, docs/GAMES-RELEASE.md): the rules, the definitions and the group-B cryptography run on a plain JVM
(tests/GameEngineTest.java, made-up games only), and the pins keep the Android side to what the docs promise: a game appears in the hub only when the release file says so for the TV's
group; a definition the engine cannot run keeps its game hidden; the file is read from memory on the UI thread (R1); nothing but that one file is downloaded; the owner's pass is a hidden
gesture and the code is never kept. Needs javac (exit 1 without)."""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
if not shutil.which("javac"): print("SKIP: no javac"); sys.exit(1)
PURE = ["GameEngine", "GameDef", "GameCrypt", "GameRelease", "GameRegistry", "EngineLayout", "MiniJson"]
with tempfile.TemporaryDirectory() as t:
    c = subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-d", t, *[str(APP / (n + ".java")) for n in PURE], str(ROOT / "tests/GameEngineTest.java")], capture_output=True, text=True)
    check("the engine, the definitions, the release switch and the cryptography compile on a plain JVM (no Android)", c.returncode == 0, c.stderr[-600:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", t, "sa.urtv.screensaver.GameEngineTest", *map(str, sorted((ROOT / "docs/examples/engine").glob("*.json")))], capture_output=True, text=True, timeout=300)
        for l in r.stdout.splitlines():
            if l.startswith(("PASS", "FAIL")): print(l); failed += l.startswith("FAIL")
        check("the JVM test ran to the end", r.returncode == 0 or "FAIL" in r.stdout, r.stderr[-400:])
def src(n): return (APP / n).read_text(encoding="utf-8")
reg, hub, st, gr, gd, ge, gc, act, ic, look, sett = (src(n) for n in ("GameRegistry.java", "QuizGamesActivity.java", "GameReleaseStore.java", "GameRelease.java", "GameDef.java", "GameEngine.java", "GameCrypt.java", "EngineGameActivity.java", "EngineIcons.java", "GameLook.java", "SettingsActivity.java"))
room, page, cf = src("GamePhoneRoom.java"), (ROOT / "assets/quiz/game.html").read_text(encoding="utf-8"), src("ContentFile.java")
manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
vocab = lambda name: re.findall(r'"([a-z0-9]+)"', gd.split(f"static final String[] {name} = {{")[1].split("};")[0])
check("the hub unchanged with everything off: the registry holds no engine game (no «e:» id), the engine's games are added from memory only and only the ones that are on, before the last tile; the filter and the first focus work on that list",
      "e:" not in reg.split("static final Game[] ALL")[1].split("};")[0] and "for (GameDef.Def d : GameReleaseStore.games())" in hub and hub.count("GameReleaseStore.games()") == 1 and "GameRegistry.firstFocus(shown(), GamePrefs.last(this))" in hub and "GameRegistry.ALL" in hub)
check("no placeholder, no count, no locked tile for a game that is not on: the hub says nothing of games it does not show", not re.search(r"GameReleaseStore\.(visible|games)\(\)\.size|\.size\(\) *[<>=]+ *0.*GameReleaseStore|locked|soon", hub.split("private java.util.List<GameRegistry.Game> shown()")[1].split("private void draw()")[0]))
check("the page asks for the answer after it is drawn (the tiles as they always were), then draws again only when the set of games on has changed; it keeps the selected game", "GameReleaseStore.warm(this" in hub and "GameReleaseStore.signature().equals(shownSig)" in hub and "keep = was.get(sel).id" in hub)
check("a game opens only through the hub with its id; the screen closes at once when the game is not on or is not one the engine can run; the activity is not exported", 'getIntent().getStringExtra("game")' in act and "def = GameReleaseStore.def(" in act and "if (def == null) { finish(); return; }" in act
      and re.search(r'android:name="\.EngineGameActivity"\s+android:exported="false"\s+android:screenOrientation="landscape"', manifest) is not None and "class EngineGameActivity extends GameActivity" in act)
check("unknown block = the game stays hidden: the store keeps a definition only when GameDef.parse accepts it, its id is the file's id, and its pack's items pass too; nothing else reaches a screen",
      "GameDef.parse(tree, null)" in st and "d == null || !d.id.equals(id)" in st and "GameDef.packItems(GameBundles.load(app, d.pack), null)" in st and "if (items == null) continue;" in st and "defs = m;" in st and "defs.containsKey(id) && GameRelease.on(" in st)
check("a game of the file is read for everyone from `defs`, and for a TV with a pass also from `bdefs` opened with the group key; a TV without a pass never gets the key", "GameRelease.def(r, id, key)" in st and "byte[] key = b ? GameCrypt.unhex(keyHex) : null;" in st and "static Object def(Release r, String id, byte[] groupKey)" in gr and "GameCrypt.open(groupKey, sealed)" in gr)
check("R1: the UI thread asks only memory: visible(), games(), def() and signature() read volatile fields; the reading, the fetch, the packs and the code check are on their own threads", not re.search(r"File\b|Net\.|ContentFile|getSharedPreferences|MiniJson|GameBundles|Thread", st.split("static boolean visible(String id)")[1].split("static void warm")[0])
      and st.count("new Thread(") == 2 and "private static volatile GameRelease.Release current; private static volatile boolean groupB; private static volatile Map<String, GameDef.Def> defs" in st)
check("R1: the hub, the settings and the engine screen never read the file or the preferences themselves", not re.search(r"ContentFile|Net\.get|getSharedPreferences\(\"games_release|GameBundles", hub + act) and "getSharedPreferences" not in sett.split("private void versionTap()")[1].split("private void ")[0])
check("held games: the hub shows a game of GameRegistry.HELD only when GameReleaseStore.nativeOn says so (memory only, on the UI thread); the signature includes them so the hub draws again when one is turned on or off; nothing else is hidden",
      "(!GameRegistry.held(g.id) || GameReleaseStore.nativeOn(g.id))" in hub and "for (String id : GameRegistry.HELD) if (nativeOn(id))" in st
      and "static boolean nativeOn(String id) { return GameRelease.heldOn(current, id, groupB, System.currentTimeMillis()); }" in st and "static boolean heldOn(Release r, String id, boolean groupB, long now) { return GameRegistry.held(id) && on(r, id, groupB, now); }" in gr)
check("offline: the copy is the kept file (ContentFile.Kept: only an object with v = 1 is ever written, a bad answer never replaces a good one), read again after every fetch; the fetch tells when it is over",
      "KEPT.load(app)" in st and "ContentFile.refresh(app, PATH, NAME, MAX_AGE_MS," in st and "static Object valid(Object o)" in cf and "valid(MiniJson.parse(r.text())) == null) return;" in cf and "after.run()" in cf)
check("a missing, unreadable or wrong file means nothing is on: parse gives null, on() answers false without a release, no definition is read, and until the first answer the store is empty", "if (r == null) return false;" in gr and "private static volatile Map<String, GameDef.Def> defs = Collections.emptyMap();" in st and "current = r; groupB = b; defs = m;" in st)
check("privacy: the only request is the one file (release/games.json on urtv-data, the same GET as the other hand-edited files); no account, no id, no query, no POST",
      'PATH = "release/games.json"' in st and not re.search(r"Settings\.Secure|ANDROID_ID|getDeviceId|Build\.SERIAL|getMacAddress|Net\.post|HttpURLConnection|openConnection|setRequestProperty", st + gr + gc + gd + ge) and "Net.get(DataHost.NEW + path, null," in cf)
check("the pass: seven presses on the version lines, each within four seconds of the last, open a code box; the check is on a thread, five tries a minute; a wrong code says nothing; only the record and the group key are kept, never the code; a code is exactly 6 signs of the alphabet (never opened otherwise)",
      "verTaps < 7" in sett and "now - verAt > 4000" in sett and "GameReleaseStore.tryCode(SettingsActivity.this, typed," in sett and "TRIES = 5" in st and "TRY_WINDOW_MS = 60_000L" in st and "if (m == null) return;" in st and "putString(PASS, m[0]).putString(KEY, m[1])" in st and "typed" not in st.split("putString")[1] and "CODE_LEN = 6" in gc and 'ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"' in gc and "validCode(n)" in gc)
check("group B: the code opens nothing by itself: PBKDF2-HMAC-SHA256 with a random salt per record, run once and named by HMAC (the public check hash is never the key), the group key sealed with AES-256-GCM, constant-time comparison, a wrong code gives null and never an exception",
      "PBKDF2WithHmacSHA256" in gc and 'named(master, "check")' in gc and 'named(master, "wrap")' in gc and gc.count("new PBEKeySpec") == 3 and "AES/GCM/NoPadding" in gc and "MessageDigest.isEqual" in gc and "ROUNDS_MIN = 100_000, ROUNDS_MAX = 1_000_000, ROUNDS_DEFAULT = 300_000" in gc and "CODES_MAX = 30" in gr and '!p[0].equals("2")' in gc)
box = sett.split("private void versionTap()")[1].split("\n    private ")[0]
check("the code box: six square boxes (one sign each, no rounded shape) under «اكتب الرمز», the typing field over them clear, capitals only, six signs at most, the check starts at the sixth; 6 x 52 + 5 x 8 dp = 352 dp wide so it fits a 720p screen at any TV density",
      "GameCrypt.CODE_LEN" in box and "dp(52)" in box and "dp(8)" in box and "dp(64)" in box and 'setTitle("اكتب الرمز")' in box and "setAlpha(0f)" in box and "AllCaps" in box and "LengthFilter(n)" in box and "e.length() == n" in box and "setCornerRadius" not in box and 6 * 52 + 5 * 8 <= 400)
check("revoking: at every read a kept record the file no longer lists is dropped with its group key, so the TV is everyone's again at the next fetch", "boolean b = GameRelease.passValid(r, kept)" in st and "p.edit().remove(PASS).remove(KEY).apply()" in st)
check("no code and no record in the sources", not re.search(r"1:\d{4,6}:[0-9a-f]{16,}:[0-9a-f]{64}", gr + st + gc + sett + hub + act))
# the vocabulary of the docs is the code's
doc = (ROOT / "docs/GAME-ENGINE.md").read_text(encoding="utf-8") if (ROOT / "docs/GAME-ENGINE.md").is_file() else ""
check("the docs list exactly the engine's blocks, scores, icons and accents", doc != "" and all(f"`{w}`" in doc for n in ("BLOCKS", "SCORES", "ICONS", "ACCENTS") for w in vocab(n)), [w for n in ("BLOCKS", "SCORES", "ICONS", "ACCENTS") for w in vocab(n) if f"`{w}`" not in doc])
check("every picture of the set is drawn (a shape from its points, or circle, check, clock, question)", all(f'case "{i}"' in ic for i in vocab("ICONS") if i in ("circle", "check", "clock")) and all(f'case "{i}"' in ic for i in vocab("ICONS") if i not in ("circle", "check", "clock", "question")))
check("every accent is a colour of the look: the five named ones and side0 to side9", all(a in ("saff", "ok", "pink", "deep", "ink") or re.fullmatch(r"side[0-9]", a) for a in vocab("ACCENTS")) and 'case "ok": return l.ok;' in look and 'name.startsWith("side")' in look and len(vocab("ACCENTS")) == 15)
check("every block of the engine has its phone card: pick, order, text, draw, act (buzz) and the vote's pick of the other players; the order card is answered as one string and refused unless it is a permutation",
      all(f'case "{b}"' in act or b in act for b in vocab("BLOCKS")) and "GamePhoneRoom.PICK" in act and "GamePhoneRoom.ORDER" in act and "GamePhoneRoom.TEXT" in act and "GamePhoneRoom.DRAW" in act and "GamePhoneRoom.ACT_BUZZ" in act and 'ORDER = "order"' in room and "permutation(x, s.opts.length)" in room and "c.kind === 'order'" in page)
check("the engine screen draws only with the kit: no colour of its own, no socket of its own, no «·» or «•», no rounded shape; it keeps nothing but the chips and the numbers of the items played",
      not re.search(r"0x[0-9A-Fa-f]{8}|Color\.parseColor|ServerSocket|QuizServer|java\.net\.|setCornerRadius|setCornerRadii", act + ic) and "·" not in act and "•" not in act and "GamePrefs.remember(this, id(), r)" in act)
check("the TV waits for the host on a buzz: OK is right, DOWN is wrong, and the phones are told who answers; a wrong call locks that phone and opens the buzzers again", "judge(true)" in act and "judge(false)" in act and "eng.lockedOut(i)" in act and "GameEngine" in act)
check("the engine's own rules know nothing of Android or of any game: no import of android, no id, no name of a game", "import android" not in ge + gd + gr + gc and not re.search(r"android\.|Context", ge + gd + gr + gc))
sys.exit(1 if failed else 0)
