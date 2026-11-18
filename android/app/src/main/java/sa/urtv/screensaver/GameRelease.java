package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The remote release switch of the games the engine plays (docs/GAMES-RELEASE.md; no Android here, tests/GameEngineTest.java). A game exists on a TV only when a small file of urtv-data
 * (release/games.json) names it and its time has come for the TV's group; a file that is missing, broken or old enough to say nothing means nothing is on, never the other way (R11's
 * spirit). Two groups and nothing else: «a» is everyone, «b» holds the owner's pass. No account, no id, nothing is sent: the TV only downloads the file.
 *
 *   {"v":1, "games":{"<id>":{"a":"2026-12-01T00:00:00Z","b":"now"}}, "defs":{"<id>":{game}}, "bdefs":{"<id>":"<sealed hex>"}, "codes":["1:rounds:salt:check:wrapped"]}
 *
 * A time is «now» (on at once), a UTC time «yyyy-MM-ddTHH:mm:ssZ» (on from then), or anything else, including false, a missing key or a missing game: off. Group B has a game from its «b» time
 * or its «a» time. `defs` are the games' definitions for everyone, `bdefs` the ones sealed with the group key (GameCrypt) that only a TV with a pass in `codes` can read.
 */
final class GameRelease {
    private GameRelease() {}

    static final int CODES_MAX = 200, GAMES_MAX = 60;
    static final long OFF = -1;
    static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9-]{1,39}");

    /** what a good file says: per game the two times (epoch ms, OFF when off), the open definitions, the sealed ones, and the pass records */
    static final class Release {
        final Map<String, long[]> games = new LinkedHashMap<>(); final Map<String, Object> defs = new LinkedHashMap<>(); final Map<String, String> bdefs = new LinkedHashMap<>();
        final List<String> codes = new ArrayList<>();
    }

    private static final Pattern WHEN = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})Z$");

    /** a time value: «now» is 0 (always passed), a UTC stamp is its epoch ms, everything else OFF */
    static long when(Object v) {
        if (!(v instanceof String)) return OFF;
        String s = ((String) v).trim();
        if (s.equals("now")) return 0;
        Matcher m = WHEN.matcher(s);
        if (!m.matches()) return OFF;
        int y = Integer.parseInt(m.group(1)), mo = Integer.parseInt(m.group(2)), d = Integer.parseInt(m.group(3)), h = Integer.parseInt(m.group(4)), mi = Integer.parseInt(m.group(5)), se = Integer.parseInt(m.group(6));
        if (y < 2020 || y > 2200 || mo < 1 || mo > 12 || d < 1 || d > 31 || h > 23 || mi > 59 || se > 59) return OFF;
        java.util.GregorianCalendar c = new java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC"));
        c.setLenient(false); c.clear();
        try { c.set(y, mo - 1, d, h, mi, se); return c.getTimeInMillis(); } catch (IllegalArgumentException e) { return OFF; }
    }

    /** the file's tree (what MiniJson gives): null when it is not a good file, which means «nothing is on» */
    static Release parse(Object tree) {
        if (!(tree instanceof Map)) return null;
        Map<?, ?> m = (Map<?, ?>) tree;
        if (!(m.get("v") instanceof Number) || ((Number) m.get("v")).intValue() != 1 || !(m.get("games") instanceof Map)) return null;
        Release r = new Release();
        for (Map.Entry<?, ?> e : ((Map<?, ?>) m.get("games")).entrySet()) {
            if (r.games.size() >= GAMES_MAX) break;
            if (!(e.getKey() instanceof String) || !ID.matcher((String) e.getKey()).matches() || !(e.getValue() instanceof Map)) continue;
            Map<?, ?> g = (Map<?, ?>) e.getValue();
            r.games.put((String) e.getKey(), new long[] {when(g.get("a")), when(g.get("b"))});
        }
        if (m.get("defs") instanceof Map) for (Map.Entry<?, ?> e : ((Map<?, ?>) m.get("defs")).entrySet()) if (e.getKey() instanceof String && r.games.containsKey(e.getKey())) r.defs.put((String) e.getKey(), e.getValue());
        if (m.get("bdefs") instanceof Map) for (Map.Entry<?, ?> e : ((Map<?, ?>) m.get("bdefs")).entrySet())
            if (e.getKey() instanceof String && r.games.containsKey(e.getKey()) && e.getValue() instanceof String && ((String) e.getValue()).length() <= 200_000) r.bdefs.put((String) e.getKey(), (String) e.getValue());
        if (m.get("codes") instanceof List) for (Object c : (List<?>) m.get("codes")) {
            if (r.codes.size() >= CODES_MAX) break;
            if (c instanceof String && GameCrypt.wellFormed((String) c)) r.codes.add((String) c);
        }
        return r;
    }

    /** is the game on for this group at `now`: no file, no entry, no time: off */
    static boolean on(Release r, String id, boolean groupB, long now) {
        if (r == null) return false;
        long[] w = r.games.get(id);
        if (w == null) return false;
        boolean a = w[0] != OFF && now >= w[0], b = groupB && w[1] != OFF && now >= w[1];
        return a || b;
    }

    /** the record of the file that this typed code opens, with the group key it holds: {record, key hex}, or null (too short, none matches); the typed text is never kept */
    static String[] match(Release r, String typed) {
        if (r == null || GameCrypt.normalize(typed).length() < GameCrypt.CODE_MIN) return null;
        String[] found = null;
        for (String rec : r.codes) {
            byte[] k = GameCrypt.groupKey(typed, rec);   // no early exit: the time does not tell which one
            if (k != null) found = new String[] {rec, GameCrypt.hex(k)};
        }
        return found;
    }

    /** a kept pass is still good only while the file lists its record: remove the line and the TV drops to group A at its next read */
    static boolean passValid(Release r, String kept) { return r != null && kept != null && !kept.isEmpty() && r.codes.contains(kept); }

    /** the definition of a game for this TV: the open one, or the sealed one opened with the group key (null key or a wrong one: nothing); a tree for GameDef.parse */
    static Object def(Release r, String id, byte[] groupKey) {
        if (r == null) return null;
        Object open = r.defs.get(id);
        if (open != null) return open;
        String sealed = r.bdefs.get(id);
        byte[] plain = sealed == null ? null : GameCrypt.open(groupKey, sealed);
        if (plain == null) return null;
        try { return MiniJson.parse(new String(plain, java.nio.charset.StandardCharsets.UTF_8)); } catch (RuntimeException e) { return null; }
    }
}
