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

    static final int KEEP = 0, AUTO = 1;
    static final long UNUSED_MS = 14L * 24 * 3600 * 1000, INDEX_MAX = 100_000, PACK_MAX = 60_000_000L, LOW_FREE = 400L * 1024 * 1024;

    static final class Pack {
        final String id, ar, en, sha; final int ver; final long bytes;
        Pack(String id, String ar, String en, int ver, long bytes, String sha) { this.id = id; this.ar = ar; this.en = en; this.ver = ver; this.bytes = bytes; this.sha = sha; }
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
                out.add(new Pack((String) id, (String) ar, en instanceof String ? (String) en : "", ((Number) ver).intValue(), b, (String) sha));
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
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder b = new StringBuilder();
            for (byte x : d) b.append(String.format("%02x", x));
            return b.toString().equals(hex);
        } catch (java.security.NoSuchAlgorithmException e) { return false; }
    }

    /** «12.4 MB» / «830 KB» for the packs list (digits are written by the caller's digits setting) */
    static String size(long bytes) { return bytes >= 1_000_000 ? String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1e6) : Math.max(1, bytes / 1000) + " KB"; }
}
