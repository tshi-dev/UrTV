package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A game as data (docs/GAME-ENGINE.md; no Android, tests/GameEngineTest.java): what the engine plays, read from JSON that arrives from urtv-data, never code. The reading is strict and
 * all or nothing: a definition with a block the engine does not have, a value outside its limits, text that is not safe to draw right to left, or a size over the caps is NOT a game
 * (`parse` gives null and says why), so the game stays hidden and nobody ever sees a broken screen. tools/check_content.py of urtv-data runs the same rules before anything is published.
 *
 *   {"v":1, "id":"test-1", "name":{"ar":"اختبار ١","en":"Test 1"}, "line":{"ar":"…","en":"…"}, "icon":"star", "look":{"accent":"saff"}, "kids":false,
 *    "players":{"min":1,"max":8,"teams":true}, "rounds":8, "seconds":20, "score":"first", "points":3, "hints":true,
 *    "items":[{"t":"choose","q":"…","o":["…","…"],"a":0}]    or    "pack":"<name of a game pack with {"v":1,"items":[…]}>"}
 *
 * A text is a string (Arabic) or {"ar":"…","en":"…"}; the Arabic is required and must have Arabic letters (a number or a code may have digits only in an option or an answer).
 */
final class GameDef {
    private GameDef() {}

    // ---------------------------------------------------------------- the limits and the vocabulary
    static final int NAME_MAX = 24, LINE_MAX = 70, Q_MAX = 160, OPT_MAX = 40, ANS_MAX = 40, ITEMS_MAX = 200, OPTS_MIN = 2, OPTS_MAX = 6, ANSWERS_MAX = 6, PAIRS_MIN = 3, PAIRS_MAX = 5, ORDER_MIN = 3,
        ROUNDS_MIN = 3, ROUNDS_MAX = 30, SECONDS_MIN = 5, SECONDS_MAX = 120, POINTS_MAX = 10, PLAYERS_MAX = 10, TREE_MAX = 4000;
    static final String[] BLOCKS = {"choose", "tf", "order", "word", "blank", "pairs", "odd", "closest", "draw", "buzz", "vote"};
    static final String[] SCORES = {"first", "all", "closest", "speed", "bank"};
    static final String[] ICONS = {"star", "heart", "circle", "square", "triangle", "diamond", "question", "check", "clock", "bulb", "flag", "drop"};
    static final String[] ACCENTS = {"saff", "ok", "pink", "deep", "ink", "side0", "side1", "side2", "side3", "side4", "side5", "side6", "side7", "side8", "side9"};

    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9-]{1,39}"), PACK = Pattern.compile("[a-z0-9][a-z0-9-]{1,39}");
    private static final Pattern ARABIC = Pattern.compile("[\\u0621-\\u064a\\u0671]"), DIGIT = Pattern.compile("[0-9\\u0660-\\u0669\\u06f0-\\u06f9]");

    /** a text in the two languages; the English may be empty (the Arabic is shown then) */
    static final class Text {
        final String ar, en;
        Text(String ar, String en) { this.ar = ar; this.en = en; }
        String get(boolean english) { return english && !en.isEmpty() ? en : ar; }
    }

    /** one question, task or word; the fields are the ones its block uses */
    static final class Item {
        String t; Text q = new Text("", ""), hint; List<Text> o = new ArrayList<>(); int a; boolean tf; double num; String unit = ""; List<String> answers = new ArrayList<>(); List<Text[]> pairs = new ArrayList<>(); Text word;
    }

    static final class Def {
        String id; Text name, line; String icon = "star", accent = "saff", score = "first", pack = ""; boolean kids, hints, teams; int minPlayers = 1, maxPlayers = 8, rounds = 8, seconds = 20, points = 3;
        List<Item> items = new ArrayList<>();
        boolean usesBlock(String t) { for (Item i : items) if (i.t.equals(t)) return true; return false; }
        /** the same game with the items of a pack (a game whose items are not in the file) */
        Def withItems(List<Item> more) { Def d = copy(); d.items = more; return d; }
        Def copy() { Def d = new Def(); d.id = id; d.name = name; d.line = line; d.icon = icon; d.accent = accent; d.score = score; d.pack = pack; d.kids = kids; d.hints = hints; d.teams = teams; d.minPlayers = minPlayers; d.maxPlayers = maxPlayers; d.rounds = rounds; d.seconds = seconds; d.points = points; d.items = items; return d; }
    }

    private static boolean in(String v, String[] set) { for (String s : set) if (s.equals(v)) return true; return false; }

    // ---------------------------------------------------------------- the text rules
    /** is this safe to draw: no control or direction-changing characters, no «·» or «•», no link, not empty, not over `max` characters */
    static boolean clean(String s, int max) {
        if (s == null || s.isEmpty() || s.length() > max || !s.equals(s.trim())) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 || c == 0x7f || (c >= 0x200b && c <= 0x200f) || (c >= 0x202a && c <= 0x202e) || (c >= 0x2066 && c <= 0x2069) || c == '﻿' || c == '·' || c == '•' || c == '•') return false;
        }
        return !s.contains("://") && !s.toLowerCase(java.util.Locale.ROOT).contains("www.");
    }
    static boolean hasArabic(String s) { return ARABIC.matcher(s).find(); }
    static boolean hasDigit(String s) { return DIGIT.matcher(s).find(); }

    // ---------------------------------------------------------------- reading
    private static final class Fail extends RuntimeException { Fail(String m) { super(m, null, false, false); } }
    private static Fail fail(String where, String what) { return new Fail(where + ": " + what); }

    private static int count(Object o, int[] n) {
        if (++n[0] > TREE_MAX) throw fail("file", "too many values (" + TREE_MAX + " at most)");
        if (o instanceof Map) for (Object v : ((Map<?, ?>) o).values()) count(v, n);
        else if (o instanceof List) for (Object v : (List<?>) o) count(v, n);
        return n[0];
    }

    /** a text: a string (Arabic only) or {ar, en}; `digitsOk` lets an option or an answer be a number or a code with no Arabic letter */
    private static Text text(Object o, String where, int max, boolean digitsOk, boolean required) {
        if (o == null) { if (required) throw fail(where, "missing"); return null; }
        String ar, en = "";
        if (o instanceof String) ar = (String) o;
        else if (o instanceof Map && ((Map<?, ?>) o).get("ar") instanceof String) {
            ar = (String) ((Map<?, ?>) o).get("ar"); Object e = ((Map<?, ?>) o).get("en");
            if (e != null && !(e instanceof String)) throw fail(where + ".en", "a text");
            if (e != null) en = (String) e;
        } else throw fail(where, "a text: a string or {ar, en}");
        if (!clean(ar, max)) throw fail(where + ".ar", "empty, over " + max + " characters, with a direction mark, a control character, a link, «·» or «•»");
        if (!(hasArabic(ar) || (digitsOk && hasDigit(ar)))) throw fail(where + ".ar", "no Arabic letters");
        if (!en.isEmpty() && (!clean(en, max) || hasArabic(en))) throw fail(where + ".en", "English only, within " + max + " characters, no direction marks or links");
        return new Text(ar, en);
    }

    private static int num(Object o, String where, int min, int max, int dflt) {
        if (o == null) return dflt;
        if (!(o instanceof Number) || ((Number) o).doubleValue() != Math.rint(((Number) o).doubleValue()) || ((Number) o).intValue() < min || ((Number) o).intValue() > max) throw fail(where, "a whole number " + min + " to " + max);
        return ((Number) o).intValue();
    }
    private static boolean flag(Object o, String where) {
        if (o == null) return false;
        if (!(o instanceof Boolean)) throw fail(where, "true or false");
        return (Boolean) o;
    }
    private static List<?> list(Object o, String where, int min, int max) {
        if (!(o instanceof List) || ((List<?>) o).size() < min || ((List<?>) o).size() > max) throw fail(where, "a list of " + min + " to " + max);
        return (List<?>) o;
    }
    private static List<String> answers(Object o, String where) {
        List<String> r = new ArrayList<>();
        for (Object a : list(o, where, 1, ANSWERS_MAX)) {
            if (!(a instanceof String) || !clean((String) a, ANS_MAX) || !(hasArabic((String) a) || hasDigit((String) a) || ((String) a).matches("[A-Za-z][A-Za-z ]*"))) throw fail(where, "answers: Arabic words, numbers or English words, within " + ANS_MAX + " characters");
            r.add((String) a);
        }
        return r;
    }

    /** one item of the block its "t" names; anything else is not a block the engine has */
    static Item item(Object o, String where) {
        if (!(o instanceof Map)) throw fail(where, "an object");
        Map<?, ?> m = (Map<?, ?>) o;
        Item it = new Item();
        if (!(m.get("t") instanceof String) || !in((String) m.get("t"), BLOCKS)) throw fail(where + ".t", "not a block of the engine: " + Arrays.toString(BLOCKS));
        it.t = (String) m.get("t");
        if (m.get("h") != null) it.hint = text(m.get("h"), where + ".h", Q_MAX, false, false);
        switch (it.t) {
            case "choose": case "odd": {
                it.q = text(m.get("q"), where + ".q", Q_MAX, false, true);
                List<?> os = list(m.get("o"), where + ".o", OPTS_MIN, OPTS_MAX);
                for (int i = 0; i < os.size(); i++) it.o.add(text(os.get(i), where + ".o[" + i + "]", OPT_MAX, true, true));
                it.a = num(m.get("a"), where + ".a", 0, os.size() - 1, -1);
                if (it.a < 0) throw fail(where + ".a", "the number of the right option");
                break;
            }
            case "tf": it.q = text(m.get("q"), where + ".q", Q_MAX, false, true); if (!(m.get("a") instanceof Boolean)) throw fail(where + ".a", "true or false"); it.tf = (Boolean) m.get("a"); break;
            case "order": {
                it.q = text(m.get("q"), where + ".q", Q_MAX, false, true);
                List<?> os = list(m.get("o"), where + ".o", ORDER_MIN, OPTS_MAX);   // in the right order: the TV shuffles them
                for (int i = 0; i < os.size(); i++) it.o.add(text(os.get(i), where + ".o[" + i + "]", OPT_MAX, true, true));
                break;
            }
            case "word": case "blank": {
                it.q = text(m.get("q"), where + ".q", Q_MAX, false, true);
                if (it.t.equals("blank") && !it.q.ar.contains("___")) throw fail(where + ".q", "a blank needs «___» in the Arabic");
                it.answers = answers(m.get("a"), where + ".a"); break;
            }
            case "pairs": {
                it.q = text(m.get("q"), where + ".q", Q_MAX, false, true);
                for (Object p : list(m.get("p"), where + ".p", PAIRS_MIN, PAIRS_MAX)) {
                    List<?> two = list(p, where + ".p", 2, 2);
                    it.pairs.add(new Text[] {text(two.get(0), where + ".p[0]", OPT_MAX, true, true), text(two.get(1), where + ".p[1]", OPT_MAX, true, true)});
                }
                break;
            }
            case "closest": {
                it.q = text(m.get("q"), where + ".q", Q_MAX, false, true);
                if (!(m.get("a") instanceof Number) || Math.abs(((Number) m.get("a")).doubleValue()) > 1e12) throw fail(where + ".a", "a number (at most a trillion)");
                it.num = ((Number) m.get("a")).doubleValue();
                if (m.get("u") != null) { if (!(m.get("u") instanceof String) || !clean((String) m.get("u"), 16) || !hasArabic((String) m.get("u"))) throw fail(where + ".u", "a unit in Arabic, 16 characters at most"); it.unit = (String) m.get("u"); }
                break;
            }
            case "draw": it.word = text(m.get("w"), where + ".w", OPT_MAX, false, true); it.answers = new ArrayList<>(); it.answers.add(it.word.ar); if (!it.word.en.isEmpty()) it.answers.add(it.word.en); break;
            case "buzz": it.q = text(m.get("q"), where + ".q", Q_MAX, false, true); if (m.get("a") != null) it.answers = answers(m.get("a"), where + ".a"); break;
            default: /* vote */ it.q = text(m.get("q"), where + ".q", Q_MAX, false, true);
        }
        return it;
    }

    /** the items of a pack's file («v»: 1, «items»), checked as strictly as the ones inline; null when there is anything wrong */
    static List<Item> packItems(Object tree, List<String> why) {
        try {
            if (!(tree instanceof Map) || !(((Map<?, ?>) tree).get("v") instanceof Number) || ((Number) ((Map<?, ?>) tree).get("v")).intValue() != 1) throw fail("pack", "{\"v\": 1, \"items\": [...]}");
            count(tree, new int[1]);
            List<?> l = list(((Map<?, ?>) tree).get("items"), "pack.items", 1, ITEMS_MAX);
            List<Item> out = new ArrayList<>();
            for (int i = 0; i < l.size(); i++) out.add(item(l.get(i), "pack.items[" + i + "]"));
            return out;
        } catch (Fail f) { if (why != null) why.add(f.getMessage()); return null; }
    }

    /** the game of a tree, or null (and why, one line) when it is not one the engine can run */
    static Def parse(Object tree, List<String> why) {
        try {
            if (!(tree instanceof Map)) throw fail("game", "an object");
            Map<?, ?> m = (Map<?, ?>) tree;
            if (!(m.get("v") instanceof Number) || ((Number) m.get("v")).intValue() != 1) throw fail("game.v", "1");
            count(tree, new int[1]);
            Def d = new Def();
            if (!(m.get("id") instanceof String) || !ID.matcher((String) m.get("id")).matches()) throw fail("game.id", "lower-case letters, digits and dashes, 2 to 40");
            d.id = (String) m.get("id");
            d.name = text(m.get("name"), "game.name", NAME_MAX, false, true);
            d.line = text(m.get("line"), "game.line", LINE_MAX, false, true);
            if (m.get("icon") != null) { if (!(m.get("icon") instanceof String) || !in((String) m.get("icon"), ICONS)) throw fail("game.icon", "one of " + Arrays.toString(ICONS)); d.icon = (String) m.get("icon"); }
            if (m.get("look") != null) {
                if (!(m.get("look") instanceof Map)) throw fail("game.look", "an object");
                Object ac = ((Map<?, ?>) m.get("look")).get("accent");
                if (ac != null) { if (!(ac instanceof String) || !in((String) ac, ACCENTS)) throw fail("game.look.accent", "one of " + Arrays.toString(ACCENTS)); d.accent = (String) ac; }
            }
            d.kids = flag(m.get("kids"), "game.kids"); d.hints = flag(m.get("hints"), "game.hints");
            if (m.get("players") != null) {
                if (!(m.get("players") instanceof Map)) throw fail("game.players", "an object");
                Map<?, ?> p = (Map<?, ?>) m.get("players");
                d.minPlayers = num(p.get("min"), "game.players.min", 1, PLAYERS_MAX, 1); d.maxPlayers = num(p.get("max"), "game.players.max", 1, PLAYERS_MAX, 8); d.teams = flag(p.get("teams"), "game.players.teams");
                if (d.minPlayers > d.maxPlayers) throw fail("game.players", "min over max");
            }
            d.rounds = num(m.get("rounds"), "game.rounds", ROUNDS_MIN, ROUNDS_MAX, 8); d.seconds = num(m.get("seconds"), "game.seconds", SECONDS_MIN, SECONDS_MAX, 20); d.points = num(m.get("points"), "game.points", 1, POINTS_MAX, 3);
            if (m.get("score") != null) { if (!(m.get("score") instanceof String) || !in((String) m.get("score"), SCORES)) throw fail("game.score", "one of " + Arrays.toString(SCORES)); d.score = (String) m.get("score"); }
            boolean inline = m.get("items") != null, packed = m.get("pack") != null;
            if (inline == packed) throw fail("game", "either \"items\" or \"pack\", not both and not neither");
            if (packed) {
                if (!(m.get("pack") instanceof String) || !PACK.matcher((String) m.get("pack")).matches()) throw fail("game.pack", "a pack name: lower-case letters, digits and dashes");
                d.pack = (String) m.get("pack");
            } else {
                List<?> l = list(m.get("items"), "game.items", 1, ITEMS_MAX);
                for (int i = 0; i < l.size(); i++) d.items.add(item(l.get(i), "game.items[" + i + "]"));
            }
            if (d.score.equals("closest") && !packed && !d.usesBlock("closest")) throw fail("game.score", "«closest» scoring needs a «closest» block");
            if (d.maxPlayers < 1) throw fail("game.players", "none");
            return d;
        } catch (Fail f) {
            if (why != null) why.add(f.getMessage());
            return null;
        }
    }
}
