package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The downloadable content packs of the games (docs/GAMES-PACKS.md; no Android, tests/GamePacksTest.java): what the index says, when a pack may be downloaded,
 * when a pack nobody opened deletes itself. A pack is one GameBundle file in urtv-data (games/packs/&lt;id&gt;/pack.gb) with a version, a size and a sha256 in
 * games/packs/index.json. The person chooses per pack: «احتفظ دائمًا» (KEEP) or «تلقائي» (AUTO: gone after 14 days unused, downloaded again when the game opens).
 */
final class GamePacks {
    private GamePacks() {}

    static final int KEEP = 0, AUTO = 1, NEW_DAYS = 21;
    static final long DAY_MS = 24L * 3600 * 1000;
    /** the day number (since 1970-01-01, UTC) of «yyyy-mm-dd», or -1 when it is not a date */
    static long epochDay(String s) {
        if (s == null || !s.matches("\\d{4}-\\d{2}-\\d{2}")) return -1;
        int y = Integer.parseInt(s.substring(0, 4)), m = Integer.parseInt(s.substring(5, 7)), d = Integer.parseInt(s.substring(8, 10));
        if (m < 1 || m > 12 || d < 1 || d > 31) return -1;
        y -= m <= 2 ? 1 : 0; long era = (y >= 0 ? y : y - 399) / 400; long yoe = y - era * 400; long doy = (153L * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1; long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }
    static final long UNUSED_MS = 14L * 24 * 3600 * 1000, INDEX_MAX = 100_000, PACK_MAX = 60_000_000L, LOW_FREE = 400L * 1024 * 1024;

    static final class Pack {
        final String id, ar, en, sha, kind, date; final int ver, count; final long bytes;
        Pack(String id, String ar, String en, int ver, long bytes, String sha) { this(id, ar, en, ver, bytes, sha, "", "", 0); }
        /** `kind` «quiz» = a pack of questions that the quiz bank adds to itself; `date` = the day it was published (yyyy-mm-dd, for the «جديد» tag); `count` = its questions */
        Pack(String id, String ar, String en, int ver, long bytes, String sha, String kind, String date, int count) { this.id = id; this.ar = ar; this.en = en; this.ver = ver; this.bytes = bytes; this.sha = sha; this.kind = kind; this.date = date; this.count = count; }
        boolean quiz() { return "quiz".equals(kind); }
        /** published in the last 21 days: the list shows «جديد» */
        boolean isNew(long nowMs) { long day = epochDay(date); return day >= 0 && nowMs / DAY_MS - day >= 0 && nowMs / DAY_MS - day <= NEW_DAYS; }
        String name(boolean en) { return en && !this.en.isEmpty() ? this.en : ar; }
    }

    /** the index: {"v":1,"packs":[{"id","ar","en","ver","bytes","sha"}]}; a pack that is not complete is skipped, an unknown version gives nothing */
    static List<Pack> parse(byte[] raw) {
        List<Pack> out = new ArrayList<>();
        if (raw == null || raw.length > INDEX_MAX) return out;
        try {
            Object root = MiniJson.parse(new String(raw, StandardCharsets.UTF_8));
            if (!(root instanceof Map)) return out;
            Map<?, ?> m = (Map<?, ?>) root;
            if (!(m.get("v") instanceof Number) || ((Number) m.get("v")).intValue() != 1 || !(m.get("packs") instanceof List)) return out;
            for (Object o : (List<?>) m.get("packs")) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> p = (Map<?, ?>) o;
                Object id = p.get("id"), ar = p.get("ar"), en = p.get("en"), ver = p.get("ver"), bytes = p.get("bytes"), sha = p.get("sha");
                if (!(id instanceof String) || !((String) id).matches("[a-z0-9\\-]{1,40}") || !(ar instanceof String) || !(ver instanceof Number) || !(bytes instanceof Number) || !(sha instanceof String)) continue;
                long b = ((Number) bytes).longValue();
                if (b <= 0 || b > PACK_MAX || !((String) sha).matches("[0-9a-f]{64}")) continue;
                Object kind = p.get("kind"), date = p.get("date"), n = p.get("n");
                out.add(new Pack((String) id, (String) ar, en instanceof String ? (String) en : "", ((Number) ver).intValue(), b, (String) sha,
                    kind instanceof String && ((String) kind).matches("[a-z]{1,12}") ? (String) kind : "", date instanceof String && epochDay((String) date) >= 0 ? (String) date : "", n instanceof Number ? Math.max(0, ((Number) n).intValue()) : 0));
            }
        } catch (RuntimeException e) { out.clear(); }
        return out;
    }

    /** a pack the person left on «تلقائي» and nobody opened for 14 days goes; «احتفظ دائمًا» never */
    static boolean expired(int mode, long lastUsed, long now) { return mode == AUTO && lastUsed > 0 && now - lastUsed > UNUSED_MS; }

    /** the TV has room: the pack three times over (the download and the copy) and a floor of 400 MB, so a full TV is never filled to the brim */
    static boolean canDownload(long freeBytes, long packBytes) { return freeBytes >= Math.max(LOW_FREE, packBytes * 3); }

    /** the file is the one the index describes */
    static boolean shaOk(byte[] data, String hex) {
        try { return hex(MessageDigest.getInstance("SHA-256").digest(data)).equals(hex); }
        catch (java.security.NoSuchAlgorithmException e) { return false; }
    }
    /** lower-case hex of a digest */
    static String hex(byte[] d) {
        StringBuilder b = new StringBuilder();
        for (byte x : d) b.append(String.format(java.util.Locale.ROOT, "%02x", x));
        return b.toString();
    }

    /** «12.4 MB» / «830 KB» for the packs list (digits are written by the caller's digits setting) */
    static String size(long bytes) { return bytes >= 1_000_000 ? String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1e6) : Math.max(1, bytes / 1000) + " KB"; }
}
