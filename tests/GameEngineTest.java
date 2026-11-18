package sa.urtv.screensaver;

import java.util.*;

/** For tests/game_engine_test.py: the release switch, the group-B cryptography, the game definitions and the engine's rules on a plain JVM, with generic made-up games only («اختبار ١»). Prints PASS/FAIL lines. */
public final class GameEngineTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    static Object tree(String json) { return MiniJson.parse(json); }
    static GameRelease.Release rel(String json) { return GameRelease.parse(tree(json)); }
    static long t(String iso) { return GameRelease.when(iso); }
    static GameDef.Def def(String json) { return GameDef.parse(tree(json), null); }
    static List<String> why(String json) { List<String> w = new ArrayList<>(); GameDef.parse(tree(json), w); return w; }
    static final byte[] SALT1 = "saltsalt-one-001".getBytes(), SALT2 = "saltsalt-two-002".getBytes(), GK = new byte[32], IV = new byte[12];
    static { for (int i = 0; i < 32; i++) GK[i] = (byte) (i * 7 + 3); for (int i = 0; i < 12; i++) IV[i] = (byte) (i + 1); }
    static final String C1 = "K7QF2M", C2 = "9XA3BD";

    /** a made-up game with one item of the block `t` */
    static String game(String extra, String items) {
        return "{\"v\":1,\"id\":\"test-1\",\"name\":{\"ar\":\"اختبار ١\",\"en\":\"Test 1\"},\"line\":\"لعبة تجريبية صغيرة\",\"rounds\":3" + (extra.isEmpty() ? "" : "," + extra) + ",\"items\":[" + items + "]}";
    }
    static final String CHOOSE = "{\"t\":\"choose\",\"q\":\"ما أكبر كوكب؟\",\"o\":[\"الأرض\",\"المشتري\",\"المريخ\"],\"a\":1}";
    static String items(String... one) { return String.join(",", one); }

    /** 200 items of 6 options each, every text an {ar, en} pair: well over 4000 values in one file */
    static String big4000() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 200; i++) { b.append(i > 0 ? "," : "").append("{\"t\":\"choose\",\"q\":{\"ar\":\"سؤال\",\"en\":\"q\"},\"o\":["); for (int k = 0; k < 6; k++) b.append(k > 0 ? "," : "").append("{\"ar\":\"جواب\",\"en\":\"a\"}"); b.append("],\"a\":0}"); }
        return b.toString();
    }
    public static void main(String[] a) throws Exception {
        release(); crypto(); crossLanguage(); defs(); engine(); layout(); examples(a); System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ the release switch
    static void release() {
        check("when: «now» is 0, a UTC stamp is its epoch ms, anything else (false, null, a number, a date alone, a local time, month 13, 30 February, empty) is off",
            t("now") == 0 && t("2026-12-01T00:00:00Z") == 1796083200000L && GameRelease.when(Boolean.FALSE) == GameRelease.OFF && GameRelease.when(null) == GameRelease.OFF && GameRelease.when(5.0) == GameRelease.OFF
            && t("2026-12-01") == GameRelease.OFF && t("2026-12-01T00:00:00+03:00") == GameRelease.OFF && t("2026-13-01T00:00:00Z") == GameRelease.OFF && t("2026-02-30T00:00:00Z") == GameRelease.OFF && t("") == GameRelease.OFF);
        check("parse: not an object, no version, a wrong version, no games object: no release at all (nothing is on)", GameRelease.parse(null) == null && GameRelease.parse(tree("[1]")) == null && rel("{\"games\":{}}") == null && rel("{\"v\":2,\"games\":{}}") == null && rel("{\"v\":1}") == null && rel("{\"v\":1,\"games\":[]}") == null);
        boolean threw = false; try { threw = rel("{broken") == null; } catch (RuntimeException e) { threw = true; }
        check("parse: a broken text is refused by the JSON reader (the store treats that as no copy)", threw);
        GameRelease.Release none = rel("{\"v\":1,\"games\":{}}");
        check("hidden by default: an empty file, and no file, turn nothing on, for either group, far in the future", !GameRelease.on(none, "test-1", false, 4102444800000L) && !GameRelease.on(none, "test-1", true, 4102444800000L) && !GameRelease.on(null, "test-1", true, 4102444800000L));
        long d1 = t("2026-12-01T00:00:00Z"), d0 = t("2026-11-20T12:00:00Z");
        GameRelease.Release r = rel("{\"v\":1,\"games\":{\"one\":{\"a\":\"2026-12-01T00:00:00Z\",\"b\":\"2026-11-20T12:00:00Z\"},\"two\":{\"a\":false,\"b\":\"now\"},\"three\":{\"a\":\"now\"},\"four\":{\"b\":\"2026-11-20T12:00:00Z\"},\"Bad Id\":{\"a\":\"now\"}}}");
        check("a/b timing: before B's time nobody has it; after it only group B; from A's time everyone", !GameRelease.on(r, "one", true, d0 - 1) && !GameRelease.on(r, "one", false, d0 - 1) && GameRelease.on(r, "one", true, d0) && !GameRelease.on(r, "one", false, d0)
            && !GameRelease.on(r, "one", false, d1 - 1) && GameRelease.on(r, "one", false, d1) && GameRelease.on(r, "one", true, d1));
        check("b only: group B from its time, A never; a only: group B has it too; a name that is not an id is ignored", GameRelease.on(r, "two", true, 5) && !GameRelease.on(r, "two", false, Long.MAX_VALUE / 2) && GameRelease.on(r, "three", false, 5) && GameRelease.on(r, "three", true, 5)
            && !GameRelease.on(r, "four", false, Long.MAX_VALUE / 2) && !GameRelease.on(r, "five", true, Long.MAX_VALUE / 2) && !r.games.containsKey("Bad Id"));
        GameRelease.Release off = rel("{\"v\":1,\"games\":{\"one\":{\"a\":false,\"b\":false},\"two\":{\"a\":\"off\",\"b\":null}}}");
        check("turning a game off again: false, null or any other word is off for both groups (the same game was on in the file above)", !GameRelease.on(off, "one", true, d1) && !GameRelease.on(off, "one", false, d1) && !GameRelease.on(off, "two", true, d1) && GameRelease.on(r, "one", false, d1));
        String rec1 = GameCrypt.record(C1, SALT1, 100000, GK, IV), rec2 = GameCrypt.record(C2, SALT2, 100000, GK, IV);
        String json = "{\"v\":1,\"games\":{\"one\":{\"a\":\"now\"}},\"codes\":[\"" + rec1 + "\",\"" + rec2 + "\",\"2:100000:zz:bad\",42,\"2:99999:" + "ab".repeat(8) + ":" + "0".repeat(64) + ":" + "0".repeat(120) + "\"]}";
        GameRelease.Release pr = rel(json);
        check("parse: only well-formed records are kept (a bad one, a number, rounds under 1000 are dropped)", pr.codes.size() == 2 && pr.codes.contains(rec1) && pr.codes.contains(rec2));
        String[] m1 = GameRelease.match(pr, C1);
        check("match: the right code gives its record and the group key; spaces, dashes, lower case and Arabic-Indic digits do not matter", m1 != null && m1[0].equals(rec1) && m1[1].equals(GameCrypt.hex(GK)) && GameRelease.match(pr, "k7qf 2m")[0].equals(rec1)
            && GameRelease.match(pr, "٩XA٣BD")[0].equals(rec2));
        check("match: a wrong code, a near one, an empty one, null, a code of 5, 7 or a wrong sign (O, 1), or no file: nothing, and no special answer", GameRelease.match(pr, "K7QF2N") == null && GameRelease.match(pr, "") == null && GameRelease.match(pr, null) == null && GameRelease.match(null, C1) == null
            && GameRelease.match(pr, "K7QF2") == null && GameRelease.match(pr, "K7QF2MA") == null && GameRelease.match(pr, "K7QF2O") == null && GameRelease.match(pr, "K7QF21") == null && GameRelease.match(rel("{\"v\":1,\"games\":{}}"), C1) == null);
        check("a code is exactly 6 signs of the alphabet: a record made for a code with an O (not in the alphabet) never opens", GameRelease.match(rel("{\"v\":1,\"games\":{},\"codes\":[\"" + GameCrypt.record("ABC2O3", SALT1, 100000, GK, IV) + "\"]}"), "ABC2O3") == null);
        String withLine = "{\"v\":1,\"games\":{\"one\":{\"b\":\"now\"}},\"codes\":[\"" + rec1 + "\"]}", without = "{\"v\":1,\"games\":{\"one\":{\"b\":\"now\"}},\"codes\":[]}";
        GameRelease.Release w1 = rel(withLine), w0 = rel(without);
        check("revoke: a kept record is good while the file lists it; take the line out and the TV is group A and the game is off; no file or no record kept is not good",
            GameRelease.passValid(w1, rec1) && GameRelease.on(w1, "one", GameRelease.passValid(w1, rec1), 5) && !GameRelease.passValid(w0, rec1) && !GameRelease.on(w0, "one", GameRelease.passValid(w0, rec1), 5)
            && !GameRelease.passValid(null, rec1) && !GameRelease.passValid(pr, null) && !GameRelease.passValid(pr, ""));
        check("offline: the kept copy is read the same way as a fresh one: the same text gives the same answers, any number of times", GameRelease.on(rel(json), "one", false, 1) && GameRelease.on(rel(json), "one", false, 2) && GameRelease.on(rel(json), "one", false, 3));
    }

    // ------------------------------------------------------------------ group B: the cryptography
    static void crypto() throws Exception {
        String rec1 = GameCrypt.record(C1, SALT1, 100000, GK, IV);
        check("record: 2:rounds:salt:check:wrapped, hex, 64 characters of check, the wrapped key is 60 bytes; the code is in none of it; another salt gives another check", GameCrypt.wellFormed(rec1) && rec1.split(":").length == 5 && rec1.split(":")[3].length() == 64 && rec1.split(":")[4].length() == 120
            && !rec1.toLowerCase().contains("k7qf2m") && !rec1.equals(GameCrypt.record(C1, SALT2, 100000, GK, IV)));
        check("the check hash is not the key: the wrap key and the check come from the same code and salt with different purposes, so the public hash opens nothing", !Arrays.equals(GameCrypt.derive(C1, SALT1, 2000, "check"), GameCrypt.derive(C1, SALT1, 2000, "wrap")) && Arrays.equals(GameCrypt.derive(C1, SALT1, 2000, "wrap"), GameCrypt.derive("k7qf2m", SALT1, 2000, "wrap")));
        check("groupKey: the right code opens the record's group key; any spelling of it", Arrays.equals(GameCrypt.groupKey(C1, rec1), GK) && Arrays.equals(GameCrypt.groupKey("k7qf 2m", rec1), GK));
        check("groupKey: a wrong code, a short one, null, a record of another code, a changed record: null and no exception", GameCrypt.groupKey("K7QF2N", rec1) == null && GameCrypt.groupKey("K7QF", rec1) == null && GameCrypt.groupKey(null, rec1) == null
            && GameCrypt.groupKey(C1, GameCrypt.record(C2, SALT2, 100000, GK, IV)) == null && GameCrypt.groupKey(C1, rec1.substring(0, rec1.length() - 2) + "00") == null && GameCrypt.groupKey(C1, "nonsense") == null && GameCrypt.groupKey(C1, "1:2000:zz:bad:x") == null);
        byte[] plain = "{\"v\":1,\"x\":\"اختبار ١\"}".getBytes("UTF-8");
        String sealed = GameCrypt.seal(GK, plain, IV);
        check("seal/open: AES-256-GCM: iv + ciphertext + tag; the right key opens it; the text is not in it", Arrays.equals(GameCrypt.open(GK, sealed), plain) && sealed.length() == (12 + plain.length + 16) * 2 && !sealed.contains(GameCrypt.hex("اختبار".getBytes("UTF-8"))));
        byte[] other = GK.clone(); other[0] ^= 1; String tampered = sealed.substring(0, 30) + (sealed.charAt(30) == '0' ? "1" : "0") + sealed.substring(31);
        check("open: a wrong key, a changed byte, a short value, odd length, upper case, null or a null key give null: a wrong code is silent", GameCrypt.open(other, sealed) == null && GameCrypt.open(GK, tampered) == null && GameCrypt.open(GK, "00") == null && GameCrypt.open(GK, sealed + "0") == null
            && GameCrypt.open(GK, sealed.toUpperCase()) == null && GameCrypt.open(GK, null) == null && GameCrypt.open(null, sealed) == null && GameCrypt.open(new byte[5], sealed) == null);
        check("a fresh seal uses a fresh iv (two seals of the same text differ) and both open", !GameCrypt.seal(GK, plain, null).equals(GameCrypt.seal(GK, plain, null)) && Arrays.equals(GameCrypt.open(GK, GameCrypt.seal(GK, plain, null)), plain));
        // the whole path: a B definition in the file, readable only with a code of the file
        String gameJson = game("", CHOOSE).replace("\"", "\\\"");
        String file = "{\"v\":1,\"games\":{\"test-1\":{\"b\":\"now\"}},\"bdefs\":{\"test-1\":\"" + GameCrypt.seal(GK, game("", CHOOSE).getBytes("UTF-8"), IV) + "\"},\"codes\":[\"" + rec1 + "\"]}";
        GameRelease.Release r = rel(file);
        String[] m = GameRelease.match(r, C1);
        Object d = GameRelease.def(r, "test-1", GameCrypt.unhex(m[1]));
        check("B decrypt: a TV that entered a code of the file opens the sealed definition and it is a game", d != null && GameDef.parse(d, null) != null && GameDef.parse(d, null).id.equals("test-1"));
        check("B decrypt: a TV with no pass, a wrong key or a stale key reads nothing (the definition is not found); a wrong code never got a key", GameRelease.def(r, "test-1", null) == null && GameRelease.def(r, "test-1", other) == null && GameRelease.match(r, "WRONGCODE12345") == null);
        check("a game with an open definition is read by everyone; one with neither is nothing", GameRelease.def(rel("{\"v\":1,\"games\":{\"test-1\":{\"a\":\"now\"}},\"defs\":{\"test-1\":" + game("", CHOOSE) + "}}"), "test-1", null) != null && GameRelease.def(r, "nosuch", GK) == null && GameRelease.def(null, "test-1", GK) == null);
        check("a definition of a game the file does not list is dropped, and sealed junk is nothing", rel("{\"v\":1,\"games\":{},\"defs\":{\"x-1\":" + game("", CHOOSE) + "}}").defs.isEmpty() && GameRelease.def(rel("{\"v\":1,\"games\":{\"test-1\":{\"b\":\"now\"}},\"bdefs\":{\"test-1\":\"" + GameCrypt.seal(GK, "not json".getBytes(), IV) + "\"}}"), "test-1", GK) == null);
        // revoke by sealing again: the old key opens nothing in the new file
        byte[] gk2 = GK.clone(); gk2[5] ^= 0x55;
        GameRelease.Release r2 = rel("{\"v\":1,\"games\":{\"test-1\":{\"b\":\"now\"}},\"bdefs\":{\"test-1\":\"" + GameCrypt.seal(gk2, game("", CHOOSE).getBytes("UTF-8"), IV) + "\"},\"codes\":[]}");
        check("revoke for good: a file sealed again with a new group key cannot be read with the key a revoked TV kept", GameRelease.def(r2, "test-1", GK) == null && GameRelease.def(r2, "test-1", gk2) != null);
    }

    // ------------------------------------------------------------------ the tool of urtv-data (Python) and the app (Java) agree
    static void crossLanguage() throws Exception {
        String rec = "2:100000:0102030405060708090a0b0c0d0e0f10:6d8332a362c55f2ad7da9b942e65a8f9843548fc31cccec823a0f1a3202d3aba:0102030405060708090a0b0cc82675a5e853e06ae0c33c903eba5327089aac9d195d6c4cdf290dcaec3b754663e6cb1b84b8dc69a6ca29f80cf43b3b";
        String sealed = "0102030405060708090a0b0c7ec82cf7d6a5dca434805965c8b432869ae939564fca887481865806e35b3a6bff7256fc2011cac7d6473e84bf";
        byte[] key = new byte[32]; for (int i = 0; i < 32; i++) key[i] = (byte) i;
        check("the record urtv-data's tool (Python) made opens in the app (Java): the right code gives the group key, in any spelling; a wrong code gives nothing", GameCrypt.wellFormed(rec) && Arrays.equals(GameCrypt.groupKey("ABCD23", rec), key)
            && Arrays.equals(GameCrypt.groupKey("abcd 23", rec), key) && GameCrypt.groupKey("ABCD24", rec) == null);
        check("a game sealed by the tool (Python, AES-256-GCM) is read by the app (Java) with the group key; with another key it is nothing", Arrays.equals(GameCrypt.open(key, sealed), "{\"v\":1,\"x\":\"اختبار ١\"}".getBytes("UTF-8")) && GameCrypt.open(new byte[32], sealed) == null);
        check("and the other way: the app's record and seal with the same inputs are byte for byte the tool's", rec.equals(GameCrypt.record("ABCD23", GameCrypt.unhex("0102030405060708090a0b0c0d0e0f10"), 100000, key, GameCrypt.unhex("0102030405060708090a0b0c"))) && sealed.equals(GameCrypt.seal(key, "{\"v\":1,\"x\":\"اختبار ١\"}".getBytes("UTF-8"), GameCrypt.unhex("0102030405060708090a0b0c"))));
    }

    // ------------------------------------------------------------------ the definitions
    static void defs() {
        GameDef.Def d = def(game("", CHOOSE));
        check("a minimal game parses: id, both names, the defaults (8 players, 20 seconds, «first» scoring, 3 points, star icon, saffron)", d != null && d.id.equals("test-1") && d.name.ar.equals("اختبار ١") && d.name.get(true).equals("Test 1") && d.line.get(true).equals(d.line.ar) && d.maxPlayers == 8 && d.seconds == 20 && d.score.equals("first") && d.points == 3 && d.icon.equals("star") && d.accent.equals("saff") && d.rounds == 3);
        String every = items(CHOOSE, "{\"t\":\"tf\",\"q\":\"الشمس نجم؟\",\"a\":true}", "{\"t\":\"odd\",\"q\":\"من الغريب؟\",\"o\":[\"قطة\",\"كلب\",\"طاولة\"],\"a\":2}", "{\"t\":\"order\",\"q\":\"رتب\",\"o\":[\"أولا\",\"ثانيا\",\"ثالثا\"]}",
            "{\"t\":\"word\",\"q\":\"عاصمة مصر؟\",\"a\":[\"القاهرة\"]}", "{\"t\":\"blank\",\"q\":\"الشمس ___ في الشرق\",\"a\":[\"تشرق\"]}", "{\"t\":\"pairs\",\"q\":\"طابق\",\"p\":[[\"قطة\",\"مواء\"],[\"كلب\",\"نباح\"],[\"بقرة\",\"خوار\"]]}",
            "{\"t\":\"closest\",\"q\":\"كم يوما في السنة؟\",\"a\":365,\"u\":\"يوم\"}", "{\"t\":\"draw\",\"w\":{\"ar\":\"شجرة\",\"en\":\"tree\"}}", "{\"t\":\"buzz\",\"q\":\"من أسرع؟\"}", "{\"t\":\"vote\",\"q\":\"من الأكثر ضحكا؟\"}");
        GameDef.Def e = def(game("", every));
        check("every one of the 11 blocks parses (choose, tf, odd, order, word, blank, pairs, closest, draw, buzz, vote)", e != null && e.items.size() == 11 && e.usesBlock("pairs") && e.usesBlock("vote") && e.items.get(8).answers.size() == 2);
        check("unknown block = no game: a block the engine does not have, a missing «t», or a number there", def(game("", "{\"t\":\"spin\",\"q\":\"س\"}")) == null && def(game("", "{\"q\":\"س\"}")) == null && def(game("", "{\"t\":5}")) == null && def(game("", items(CHOOSE, "{\"t\":\"native-code\"}"))) == null);
        check("the reason is one line saying where", why(game("", "{\"t\":\"spin\"}")).get(0).contains("game.items[0].t") && why("[1]").size() == 1 && why(game("\"score\":\"magic\"", CHOOSE)).get(0).contains("game.score"));
        check("version and id: v must be 1; an id of capitals, spaces, one character or 41 characters is refused", def(game("", CHOOSE).replace("\"v\":1", "\"v\":2")) == null && def(game("", CHOOSE).replace("test-1", "Test 1")) == null && def(game("", CHOOSE).replace("test-1", "x")) == null && def(game("", CHOOSE).replace("test-1", "a".repeat(41))) == null && def(game("", CHOOSE).replace("test-1", "a".repeat(40))) != null);
        check("Arabic is required: a name, a line, a question or an option without Arabic letters is refused (an option may be a number)", def(game("", CHOOSE).replace("اختبار ١", "Test")) == null && def(game("", CHOOSE).replace("لعبة تجريبية صغيرة", "a small game")) == null
            && def(game("", "{\"t\":\"tf\",\"q\":\"Is the sun a star?\",\"a\":true}")) == null && def(game("", "{\"t\":\"choose\",\"q\":\"كم؟\",\"o\":[\"١٢٠\",\"150\"],\"a\":0}")) != null && def(game("", "{\"t\":\"choose\",\"q\":\"كم؟\",\"o\":[\"abc\",\"x\"],\"a\":0}")) == null);
        check("the English side is English only: Arabic letters in it, a link in it, or an English text over the cap are refused", def(game("", CHOOSE).replace("Test 1", "اختبار")) == null && def(game("", CHOOSE).replace("Test 1", "see http://x.y")) == null && def(game("", CHOOSE).replace("Test 1", "x".repeat(25))) == null);
        check("unsafe text is refused: a direction mark, a control character, a zero-width space, «·», «•», a link, leading space, an empty text", def(game("", CHOOSE).replace("ما أكبر", "\\u202bما أكبر")) == null && def(game("", CHOOSE).replace("ما أكبر", "\\u200fما أكبر")) == null && def(game("", CHOOSE).replace("ما أكبر", "ما\\u0007 أكبر")) == null
            && def(game("", CHOOSE).replace("ما أكبر", "ما · أكبر")) == null && def(game("", CHOOSE).replace("ما أكبر", "ما • أكبر")) == null && def(game("", CHOOSE).replace("ما أكبر", "ما أكبر www.x.com")) == null && def(game("", CHOOSE).replace("\"ما أكبر", "\" ما أكبر")) == null && def(game("", CHOOSE).replace("ما أكبر كوكب؟", "")) == null);
        check("size limits: a question over 160, an option over 40, a name over 24, a line over 70 characters are refused; the limits themselves are fine", def(game("", CHOOSE.replace("ما أكبر كوكب؟", "ب".repeat(161)))) == null && def(game("", CHOOSE.replace("ما أكبر كوكب؟", "ب".repeat(160)))) != null && def(game("", CHOOSE.replace("الأرض", "ب".repeat(41)))) == null
            && def(game("", CHOOSE).replace("اختبار ١", "ب".repeat(25))) == null && def(game("", CHOOSE).replace("لعبة تجريبية صغيرة", "ب".repeat(71))) == null && def(game("", CHOOSE).replace("لعبة تجريبية صغيرة", "ب".repeat(70))) != null);
        StringBuilder many = new StringBuilder(); for (int i = 0; i < 201; i++) many.append(i > 0 ? "," : "").append(CHOOSE);
        StringBuilder ok200 = new StringBuilder(); for (int i = 0; i < 200; i++) ok200.append(i > 0 ? "," : "").append("{\"t\":\"tf\",\"q\":\"سؤال\",\"a\":true}");
        check("an item list over 200 is refused, 200 is fine; a file with more than 4000 values is refused whole", def(game("", many.toString())) == null && def(game("", ok200.toString())) != null && def(game("", big4000())) == null);
        check("options: 2 to 6, the right number in range, strings; 1 or 7 options, a = 3 of 3, no a, a = 1.5 are refused", def(game("", CHOOSE.replace(",\"المريخ\"", ""))) != null && def(game("", "{\"t\":\"choose\",\"q\":\"س\",\"o\":[\"ا\"],\"a\":0}")) == null && def(game("", "{\"t\":\"choose\",\"q\":\"سؤال\",\"o\":[\"ا\",\"ب\",\"ت\",\"ث\",\"ج\",\"ح\",\"خ\"],\"a\":0}")) == null
            && def(game("", CHOOSE.replace("\"a\":1", "\"a\":3"))) == null && def(game("", CHOOSE.replace(",\"a\":1", ""))) == null && def(game("", CHOOSE.replace("\"a\":1", "\"a\":1.5"))) == null);
        check("a blank needs «___» in its Arabic; order needs 3 to 6 things; pairs 3 to 5 pairs of two; a closest answer is a number; a word needs answers", def(game("", "{\"t\":\"blank\",\"q\":\"بلا فراغ\",\"a\":[\"س\"]}")) == null && def(game("", "{\"t\":\"order\",\"q\":\"رتب\",\"o\":[\"ا\",\"ب\"]}")) == null
            && def(game("", "{\"t\":\"pairs\",\"q\":\"طابق\",\"p\":[[\"ا\",\"ب\"],[\"ت\",\"ث\"]]}")) == null && def(game("", "{\"t\":\"pairs\",\"q\":\"طابق\",\"p\":[[\"ا\",\"ب\",\"ج\"],[\"ت\",\"ث\"],[\"ح\",\"خ\"]]}")) == null && def(game("", "{\"t\":\"closest\",\"q\":\"كم\",\"a\":\"365\"}")) == null && def(game("", "{\"t\":\"word\",\"q\":\"س\",\"a\":[]}")) == null);
        check("a game's own numbers: rounds 3 to 30, seconds 5 to 120, points 1 to 10, players 1 to 10 with min not over max; score and icon and accent from the lists",
            def(game("\"rounds\":2", CHOOSE)) == null && def(game("", CHOOSE).replace("\"rounds\":3", "\"rounds\":31")) == null && def(game("\"seconds\":4", CHOOSE)) == null && def(game("\"seconds\":121", CHOOSE)) == null && def(game("\"points\":11", CHOOSE)) == null && def(game("\"points\":0", CHOOSE)) == null
            && def(game("\"players\":{\"min\":4,\"max\":2}", CHOOSE)) == null && def(game("\"players\":{\"max\":11}", CHOOSE)) == null && def(game("\"icon\":\"skull\"", CHOOSE)) == null && def(game("\"icon\":\"heart\",\"look\":{\"accent\":\"side7\"}", CHOOSE)) != null && def(game("\"look\":{\"accent\":\"#ff0000\"}", CHOOSE)) == null && def(game("\"score\":\"closest\"", CHOOSE)) == null);
        GameDef.Def pk = def("{\"v\":1,\"id\":\"test-2\",\"name\":\"اختبار ٢\",\"line\":\"من حزمة\",\"pack\":\"test-pack\"}");
        check("a game may name a pack instead of items (never both, never neither); the pack's items are checked as strictly", pk != null && pk.pack.equals("test-pack") && pk.items.isEmpty() && def(game("\"pack\":\"x-pack\"", CHOOSE)) == null && def("{\"v\":1,\"id\":\"test-2\",\"name\":\"اختبار ٢\",\"line\":\"من حزمة\"}") == null
            && GameDef.packItems(tree("{\"v\":1,\"items\":[" + CHOOSE + "]}"), null).size() == 1 && GameDef.packItems(tree("{\"v\":1,\"items\":[{\"t\":\"nope\"}]}"), null) == null && GameDef.packItems(tree("{\"v\":2,\"items\":[]}"), null) == null && pk.withItems(GameDef.packItems(tree("{\"v\":1,\"items\":[" + CHOOSE + "]}"), null)).items.size() == 1);
        check("a flag that is not a true or false (kids, hints, teams) is refused", def(game("\"kids\":1", CHOOSE)) == null && def(game("\"hints\":\"yes\"", CHOOSE)) == null && def(game("\"players\":{\"teams\":2}", CHOOSE)) == null && def(game("\"kids\":true,\"hints\":true", CHOOSE)).kids);
        check("a text may be {ar, en} or a plain string; an {en} without ar is refused; hints are texts", def(game("", CHOOSE.replace("\"ما أكبر كوكب؟\"", "{\"ar\":\"ما أكبر كوكب؟\",\"en\":\"Which planet is biggest?\"}"))).items.get(0).q.get(true).startsWith("Which") && def(game("", CHOOSE.replace("\"ما أكبر كوكب؟\"", "{\"en\":\"x\"}"))) == null
            && def(game("", CHOOSE.replace("\"a\":1", "\"a\":1,\"h\":\"تلميح صغير\""))).items.get(0).hint.ar.equals("تلميح صغير"));
        check("the engine's vocabulary is exactly what the docs say: 11 blocks, 5 scores, 12 icons, 15 accents", GameDef.BLOCKS.length == 11 && GameDef.SCORES.length == 5 && GameDef.ICONS.length == 12 && GameDef.ACCENTS.length == 15);
    }

    // ------------------------------------------------------------------ the example games of docs/examples/engine
    static void examples(String[] files) throws Exception {
        int played = 0; boolean all = files.length > 0;
        for (String f : files) {
            String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(f)), "UTF-8");
            List<String> w = new ArrayList<>(); GameDef.Def d = GameDef.parse(tree(json), w);
            if (d == null) { check("example " + f + " is a game the engine can run", false); System.out.println(w); all = false; continue; }
            int n = Math.max(d.minPlayers, 3); if (n > d.maxPlayers) n = d.maxPlayers;
            GameEngine g = new GameEngine(d, d.items, n, 0, new Random(4), null); int rounds = 0;
            while (g.phase == GameEngine.Phase.ASK && rounds < 40) {
                GameDef.Item it = g.item();
                if (it.t.equals("buzz")) { g.buzz(0, 5); g.judgeBuzz(true); }
                else {
                    for (int p = 0; p < n; p++) {
                        Object a;
                        switch (it.t) { case "choose": case "odd": a = it.a; break; case "tf": a = it.tf; break; case "order": case "pairs": { int[] sh = g.shown(), s = new int[sh.length]; for (int k = 0; k < sh.length; k++) for (int j = 0; j < sh.length; j++) if (sh[j] == k) s[k] = j; a = s; break; }
                            case "word": case "blank": a = it.answers.get(0).replace("___", ""); break; case "draw": a = it.word.ar; break; case "closest": a = it.num; break; default: a = (p + 1) % n; }
                        g.answer(p, a, 100 + p * 50);
                    }
                    g.reveal();
                }
                g.next(); rounds++;
            }
            all &= g.phase == GameEngine.Phase.END && rounds == g.total() && Arrays.stream(g.score).sum() > 0;
            played++;
        }
        check("the example games (" + played + ") parse, and a full game of each plays through every round with right answers and scores", files.length == 0 || (all && played == files.length));
    }

    // ------------------------------------------------------------------ where things stand on the screen
    static void layout() {
        boolean inside = true, apart = true, big720 = true;
        for (int n = 2; n <= 6; n++) {
            float[][] r = new float[n][];
            for (int i = 0; i < n; i++) {
                r[i] = EngineLayout.rect(i, n);
                inside &= r[i][0] >= 96 && r[i][1] >= 440 && r[i][0] + r[i][2] <= 1824 && r[i][1] + r[i][3] <= 960;
                big720 &= r[i][3] * 2f / 3f >= 56 && r[i][2] * 2f / 3f >= 300;
            }
            for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) apart &= r[i][0] + r[i][2] <= r[j][0] || r[j][0] + r[j][2] <= r[i][0] || r[i][1] + r[i][3] <= r[j][1] || r[j][1] + r[j][3] <= r[i][1];
        }
        check("layout: 2 to 6 stamps all stand inside the 96 px safe area below the question block (y 440 to 960)", inside);
        check("layout: no two stamps touch or overlap, for every count", apart);
        check("layout at 1280 x 720 (R5): a stamp is 56 px high and 300 px wide at the least, for every count", big720);
        float[] one = EngineLayout.rect(0, 3), two = EngineLayout.rect(0, 4);
        check("layout: up to three stand in one full-width column, four to six in two columns; a column is filled before the next (so the first is at the start edge)", one[2] > 1700 && two[2] < 900 && EngineLayout.rect(0, 6)[0] == EngineLayout.rect(1, 6)[0] && EngineLayout.rect(3, 6)[0] > EngineLayout.rect(0, 6)[0]);
        check("layout: six stamps in two columns of three keep their height at 132 or the room allows; two stamps are not taller than 132", EngineLayout.rect(0, 2)[3] <= 132 && EngineLayout.rect(0, 6)[3] >= 120);
    }

    // ------------------------------------------------------------------ the engine
    static GameEngine eng(String extra, String items, int players, int teams, long seed) { return new GameEngine(def(game(extra, items)), def(game(extra, items)).items, players, teams, new Random(seed), null); }
    static void engine() {
        GameEngine g = eng("", CHOOSE, 3, 0, 1);
        check("an engine starts on the first round, asking, with one side per player and the rounds of the definition (or fewer when the pool is short)", g.phase == GameEngine.Phase.ASK && g.sides == 3 && g.total() == 1 && g.index == 0 && g.item().t.equals("choose"));
        check("answers: out of range, wrong type, an unknown player, null and a second answer from the same phone are refused; the first counts", !g.answer(0, 3, 100) && !g.answer(0, -1, 100) && !g.answer(0, "المشتري", 100) && !g.answer(5, 1, 100) && !g.answer(-1, 1, 100) && !g.answer(0, null, 100) && g.answer(0, 1, 800) && !g.answer(0, 0, 900) && g.hasAnswered(0));
        g.answer(1, 0, 500); g.answer(2, 1, 1500);
        check("everyone has answered", g.allAnswered() && g.answeredCount() == 3);
        g.reveal();
        check("«first»: the first right answer scores 3, the next one 2, a wrong one nothing; the places are by time", g.score[0] == 3 && g.score[1] == 0 && g.score[2] == 2 && g.placeOf(0) == 1 && g.placeOf(2) == 2 && g.placeOf(1) == 0 && g.rightOf(0) && !g.rightOf(1) && g.phase == GameEngine.Phase.REVEAL);
        check("no answer after the reveal", !g.answer(1, 1, 5));
        g.next();
        check("the last round played: the game ends and keeps the scores", g.phase == GameEngine.Phase.END && g.leaders().equals(Arrays.asList(0)));
        // the pool and the picks
        String many = items(CHOOSE, CHOOSE.replace("المشتري", "زحل"), CHOOSE.replace("المريخ", "عطارد"), CHOOSE.replace("الأرض", "القمر"));
        GameEngine pick = new GameEngine(def(game("", many)), def(game("", many)).items, 2, 0, new Random(3), new HashSet<>(Arrays.asList(0, 1)));
        check("recent items wait: with the first two recent, the three rounds take the two fresh ones first and the pool is used without repeating", pick.total() == 3 && pick.usedItems().size() == 1 && new HashSet<>(Arrays.asList(2, 3)).contains(pick.usedItems().get(0)));
        // all / speed
        GameEngine all = eng("\"score\":\"all\",\"points\":2", CHOOSE, 3, 0, 1); all.answer(0, 1, 1000); all.answer(1, 1, 9000); all.answer(2, 0, 10); all.reveal();
        check("«all»: every right answer scores the points whatever the time", all.score[0] == 2 && all.score[1] == 2 && all.score[2] == 0);
        GameEngine sp = eng("\"score\":\"speed\",\"points\":5,\"seconds\":20", CHOOSE, 3, 0, 1); sp.answer(0, 1, 0); sp.answer(1, 1, 10000); sp.answer(2, 1, 20000); sp.reveal();
        check("«speed»: at once 5, at half the time 3, at the last moment 1", sp.score[0] == 5 && sp.score[1] == 3 && sp.score[2] == 1);
        // blocks
        GameEngine tf = eng("", "{\"t\":\"tf\",\"q\":\"الشمس نجم؟\",\"a\":true}", 2, 0, 1); tf.answer(0, true, 1); tf.answer(1, false, 2); tf.reveal();
        check("true or false: a Boolean; the right one scores", tf.score[0] == 3 && tf.score[1] == 0 && !tf.answer(0, 1, 1));
        GameEngine od = eng("", "{\"t\":\"odd\",\"q\":\"من الغريب؟\",\"o\":[\"قطة\",\"كلب\",\"طاولة\"],\"a\":2}", 2, 0, 1); od.answer(0, 2, 5); od.answer(1, 0, 6); od.reveal();
        check("odd one out: judged like choose", od.rightOf(0) && !od.rightOf(1));
        GameEngine wd = eng("", "{\"t\":\"word\",\"q\":\"عاصمة مصر؟\",\"a\":[\"القاهرة\",\"Cairo\"]}", 4, 0, 1); wd.answer(0, "القاهره", 1); wd.answer(1, " cairo ", 2); wd.answer(2, "الاسكندرية", 3); wd.answer(3, "قاهرة", 4); wd.reveal();
        check("type a word: forgiving spelling (ة and ه, «ال», the case of Latin, spaces); the English answer counts; a wrong word does not", wd.rightOf(0) && wd.rightOf(1) && !wd.rightOf(2) && wd.rightOf(3) && !wd.answer(0, "", 9) && GameEngine.norm("ٱلْقَاهِرَةُ").equals("قاهره") && GameEngine.norm("  Cairo  ").equals("cairo"));
        check("a word that is empty, only marks, or over 60 characters is refused", !eng("", "{\"t\":\"word\",\"q\":\"س؟\",\"a\":[\"ا\"]}", 2, 0, 1).answer(0, " ", 1) && !eng("", "{\"t\":\"word\",\"q\":\"س؟\",\"a\":[\"ا\"]}", 2, 0, 1).answer(0, "ا".repeat(61), 1));
        GameEngine bl = eng("", "{\"t\":\"blank\",\"q\":\"الشمس ___ في الشرق\",\"a\":[\"تشرق\"]}", 2, 0, 1); bl.answer(0, "تشرق", 1); bl.reveal();
        check("fill the blank: typed like a word", bl.rightOf(0));
        GameEngine od2 = eng("", "{\"t\":\"order\",\"q\":\"رتب\",\"o\":[\"أولا\",\"ثانيا\",\"ثالثا\",\"رابعا\"]}", 3, 0, 5);
        int[] sh = od2.shown(); int[] good = new int[4]; for (int k = 0; k < 4; k++) for (int j = 0; j < 4; j++) if (sh[j] == k) good[k] = j;
        boolean identity = true; for (int i = 0; i < 4; i++) if (sh[i] != i) identity = false;
        check("order: the TV shows the things shuffled (never already in order); the permutation is of 0 to n-1", !identity && sh.length == 4 && Arrays.stream(sh).sum() == 6);
        check("order: a phone sends the displayed numbers in the order it thinks right: the right sequence is right, any other is wrong, a repeat, a short list or a number out of range is refused", !od2.answer(0, new int[] {0, 0, 1, 2}, 1) && !od2.answer(0, new int[] {0, 1, 2}, 1) && !od2.answer(0, new int[] {0, 1, 2, 9}, 1) && !od2.answer(0, "0123", 1) && od2.answer(0, good, 100) && od2.answer(1, new int[] {0, 1, 2, 3}, 200) && !od2.hasAnswered(2));
        od2.reveal();
        check("order: scored «first» for the right sequence", od2.rightOf(0) && !od2.rightOf(1) && od2.score[0] == 3);
        GameEngine pr = eng("", "{\"t\":\"pairs\",\"q\":\"طابق\",\"p\":[[\"قطة\",\"مواء\"],[\"كلب\",\"نباح\"],[\"بقرة\",\"خوار\"]]}", 2, 0, 7);
        int[] ps = pr.shown(); int[] pg = new int[3]; for (int k = 0; k < 3; k++) for (int j = 0; j < 3; j++) if (ps[j] == k) pg[k] = j;
        pr.answer(0, pg, 5); pr.answer(1, new int[] {0, 1, 2}, 6); pr.reveal();
        check("match pairs: the phone orders the shown right-hand things to follow the left-hand list; only the whole match is right", pr.rightOf(0) && (pr.rightOf(1) == (Arrays.equals(ps, new int[] {0, 1, 2}))));
        GameEngine cl = eng("\"score\":\"closest\",\"points\":3", "{\"t\":\"closest\",\"q\":\"كم يوما في السنة؟\",\"a\":365}", 4, 0, 1);
        cl.answer(0, 365.0, 1); cl.answer(1, 360.0, 2); cl.answer(2, 400.0, 3); cl.answer(3, 5000.0, 4); cl.reveal();
        check("closest number: the nearest scores 3 (an exact one 1 more), the next 2, the next 1, the far one nothing; the distances are told", cl.score[0] == 4 && cl.score[1] == 2 + 0 && cl.score[2] == 1 && cl.score[3] == 0 && cl.distOf(1) == 5 && cl.distOf(2) == 35 && cl.placeOf(3) == 4);
        GameEngine ct = eng("\"score\":\"closest\",\"points\":3", "{\"t\":\"closest\",\"q\":\"كم؟\",\"a\":100}", 3, 0, 1); ct.answer(0, 90.0, 1); ct.answer(1, 110.0, 2); ct.answer(2, 99.0, 3); ct.reveal();
        check("closest number: the same distance shares the place", ct.placeOf(0) == ct.placeOf(1) && ct.placeOf(2) == 1 && ct.score[2] == 3 && ct.score[0] == 2 && ct.score[1] == 2);
        check("a number is read from Arabic-Indic and Persian digits, «٫» and «,», a minus; text, empty, two points and 17 digits are not numbers", GameEngine.parseNumber("٣٦٥") == 365 && GameEngine.parseNumber("۱٫۵") == 1.5 && GameEngine.parseNumber("2,5") == 2.5 && GameEngine.parseNumber("-4") == -4 && GameEngine.parseNumber("−4") == -4
            && Double.isNaN(GameEngine.parseNumber("abc")) && Double.isNaN(GameEngine.parseNumber("")) && Double.isNaN(GameEngine.parseNumber("1.2.3")) && Double.isNaN(GameEngine.parseNumber("1".repeat(17))) && Double.isNaN(GameEngine.parseNumber(null)) && Double.isNaN(GameEngine.parseNumber("١٢ كم")));
        check("a closest answer must be a finite number", !eng("\"score\":\"closest\"", "{\"t\":\"closest\",\"q\":\"كم؟\",\"a\":1}", 2, 0, 1).answer(0, Double.NaN, 1) && !eng("\"score\":\"closest\"", "{\"t\":\"closest\",\"q\":\"كم؟\",\"a\":1}", 2, 0, 1).answer(0, Double.POSITIVE_INFINITY, 1) && !eng("\"score\":\"closest\"", "{\"t\":\"closest\",\"q\":\"كم؟\",\"a\":1}", 2, 0, 1).answer(0, "5", 1));
                GameEngine dv = eng("", "{\"t\":\"vote\",\"q\":\"من الأكثر ضحكا؟\"}", 4, 0, 1);
        check("vote: never for yourself, never out of range; the most voted player is told, everyone who voted for them scores 1; a tie shares", !dv.answer(0, 0, 1) && !dv.answer(0, 4, 1) && !dv.answer(0, -1, 1) && dv.answer(0, 1, 1) && dv.answer(1, 2, 2) && dv.answer(2, 1, 3) && dv.answer(3, 1, 4));
        dv.reveal();
        check("vote: player 1 has three votes; those three score 1 and the other nothing", dv.topVoted().equals(Arrays.asList(1)) && dv.score[0] == 1 && dv.score[2] == 1 && dv.score[3] == 1 && dv.score[1] == 0 && dv.votesFor(1) == 3);
        GameEngine dt = eng("", "{\"t\":\"vote\",\"q\":\"من؟\"}", 4, 0, 1); dt.answer(0, 1, 1); dt.answer(1, 0, 1); dt.answer(2, 1, 1); dt.answer(3, 0, 1); dt.reveal();
        check("vote: a tie for the most shares it, and every voter of either scores", dt.topVoted().equals(Arrays.asList(0, 1)) && dt.score[0] == 1 && dt.score[1] == 1 && dt.score[2] == 1 && dt.score[3] == 1);
        GameEngine dn = eng("", "{\"t\":\"vote\",\"q\":\"من؟\"}", 3, 0, 1); dn.reveal();
        check("vote: nobody voted: nobody scores", dn.topVoted().isEmpty() && dn.score[0] + dn.score[1] + dn.score[2] == 0);
        // draw
        GameEngine dr = eng("", "{\"t\":\"draw\",\"w\":{\"ar\":\"شجرة\",\"en\":\"tree\"}}", 4, 0, 1);
        int drawer = dr.drawer();
        check("draw and guess: the drawer is a player of the round, cannot guess, and the round waits for everyone else", drawer == 0 && !dr.answer(drawer, "شجرة", 1) && dr.answer(1, "شجره", 10) && dr.answer(2, "tree", 20) && !dr.allAnswered() && dr.answer(3, "بيت", 30) && dr.allAnswered());
        dr.reveal();
        check("draw and guess: the first right guess scores 3, the second 2, and the drawer scores 1 for each right guess", dr.score[1] == 3 && dr.score[2] == 2 && dr.score[3] == 0 && dr.score[drawer] == 2);
        // buzz
        GameEngine bz = eng("\"points\":4", "{\"t\":\"buzz\",\"q\":\"من أسرع؟\"}", 3, 0, 1);
        check("buzz: a phone cannot answer by the usual call; the first to press is the speaker, a second press is refused, nobody else can until the host calls", !bz.answer(0, 1, 1) && bz.buzz(1, 10) && !bz.buzz(2, 11) && bz.buzzer == 1 && !bz.allAnswered());
        bz.judgeBuzz(false);
        check("buzz: a wrong call locks that phone out and opens the buzzers again; the locked phone cannot press", bz.buzzer == -1 && bz.lockedOut(1) && !bz.buzz(1, 20) && bz.buzz(2, 21) && bz.phase == GameEngine.Phase.ASK);
        bz.judgeBuzz(true);
        check("buzz: a right call scores the points for that side and closes the round", bz.score[2] == 4 && bz.score[1] == 0 && bz.phase == GameEngine.Phase.REVEAL && bz.rightOf(2));
        GameEngine b2 = eng("", "{\"t\":\"buzz\",\"q\":\"من أسرع؟\"}", 2, 0, 1); b2.buzz(0, 1); b2.judgeBuzz(false); b2.buzz(1, 2); b2.judgeBuzz(false);
        check("buzz: everyone wrong closes the round with no points; a call with nobody speaking changes nothing", b2.phase == GameEngine.Phase.REVEAL && b2.score[0] + b2.score[1] == 0 && eng("", "{\"t\":\"buzz\",\"q\":\"س؟\"}", 2, 0, 1).buzzer == -1);
        // teams
        GameEngine tm = eng("\"players\":{\"teams\":true}", CHOOSE, 5, 2, 1); tm.answer(0, 1, 1); tm.answer(1, 1, 2); tm.answer(2, 1, 3); tm.answer(3, 0, 4); tm.answer(4, 1, 5); tm.reveal();
        check("teams: the phones take the teams in turn; a team's score is its phones' points added (here 3 + 1 for team 0, 2 for team 1); the sides are the teams", tm.sides == 2 && tm.sideOf(0) == 0 && tm.sideOf(1) == 1 && tm.sideOf(2) == 0 && tm.sideOf(4) == 0 && tm.score[0] == 3 + 1 + 1 && tm.score[1] == 2);
        boolean refused = false; try { eng("", CHOOSE, 5, 2, 1); } catch (IllegalArgumentException e) { refused = true; }
        boolean one = false; try { eng("\"players\":{\"teams\":true}", CHOOSE, 5, 1, 1); } catch (IllegalArgumentException e) { one = true; }
        boolean tooMany = false; try { eng("\"players\":{\"max\":3}", CHOOSE, 5, 0, 1); } catch (IllegalArgumentException e) { tooMany = true; }
        check("teams are refused when the game does not allow them, when there is one team, and a number of players outside the game's range", refused && one && tooMany);
        // bank
        String three = items(CHOOSE, CHOOSE.replace("المشتري", "زحل"), CHOOSE.replace("المريخ", "عطارد"), CHOOSE.replace("الأرض", "القمر"), CHOOSE.replace("ما أكبر", "ما أصغر"));
        GameEngine bk = new GameEngine(def(game("\"score\":\"bank\",\"points\":4,\"rounds\":5", three)), def(game("\"score\":\"bank\",\"points\":4,\"rounds\":5", three)).items, 4, 0, new Random(1), null);
        check("bank: one team bank for everyone (a single side), three lives", bk.sides == 1 && bk.lives == 3);
        for (int p = 0; p < 4; p++) bk.answer(p, p < 3 ? bk.item().a : (bk.item().a + 1) % 3, p); bk.reveal();
        check("bank: more than half the players right adds the points and keeps the lives", bk.score[0] == 4 && bk.lives == 3);
        bk.next(); for (int p = 0; p < 4; p++) bk.answer(p, p < 2 ? bk.item().a : (bk.item().a + 1) % 3, p); bk.reveal();
        check("bank: exactly half is not more than half: a life is lost and nothing is added", bk.score[0] == 4 && bk.lives == 2);
        bk.next(); bk.reveal(); bk.next(); bk.reveal();
        check("bank: no answers lose a life each; with the last life gone the game ends at once, before the rounds run out", bk.lives == 0 && bk.index == 3 && bk.phase == GameEngine.Phase.REVEAL);
        bk.next();
        check("bank: next after the last life is the end", bk.phase == GameEngine.Phase.END && bk.index <= 3);
        // flow
        GameEngine fl = new GameEngine(def(game("", many)), def(game("", many)).items, 2, 0, new Random(9), null);
        fl.next();
        check("rounds: three rounds from four items; next before the reveal does nothing", fl.total() == 3 && fl.index == 0 && fl.phase == GameEngine.Phase.ASK);
        fl.reveal(); fl.next(); fl.reveal(); fl.next(); fl.reveal(); fl.next();
        check("rounds: after the third round the game ends, nothing more is accepted and a later reveal changes nothing", fl.phase == GameEngine.Phase.END && fl.item() == null && !fl.answer(0, 0, 1) && fl.index == 2);
        GameEngine fn = eng("", CHOOSE, 2, 0, 1); fn.finish();
        check("finish: ends at once with nobody ahead (a tie of all)", fn.phase == GameEngine.Phase.END && fn.leaders().size() == 2);
        // the input of a draw, a vote and a buzz is not accepted for another block
        GameEngine wr = eng("", CHOOSE, 2, 0, 1);
        check("a block's answer form is the only one taken: text for a choose round, a boolean for it, an array for it are all refused", !wr.answer(0, "1", 1) && !wr.answer(0, true, 1) && !wr.answer(0, new int[] {1}, 1) && !wr.answer(0, 1.0, 1));
        // a pack-less game with no items cannot be played
        boolean empty = false; try { new GameEngine(def("{\"v\":1,\"id\":\"test-2\",\"name\":\"اختبار ٢\",\"line\":\"من حزمة\",\"pack\":\"test-pack\"}"), new ArrayList<>(), 2, 0, new Random(), null); } catch (IllegalArgumentException e) { empty = true; }
        check("a game with no items (its pack not loaded) cannot start", empty);
    }
}
