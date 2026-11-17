package sa.urtv.home.tilawah;

/**
 * 4.4 «متابعة السورة»: where the person stopped in each surah, for the last KEEP surahs heard (any reciter), so picking a surah heard half way
 * goes on from there (before, a pick always started at 0:00 and wiped the one place «متابعة التلاوة» kept). One short record in the tilawah
 * prefs: "reciterId:surah:ms;…", the latest first. A place in the first FROM_MS or the last NEAR_END_MS is no place (from the start). Pure logic
 * (no Android classes): tests/surah_places_test.py.
 */
final class SurahPlaces {
    private SurahPlaces() { }

    static final int KEEP = 20;
    static final long FROM_MS = 10_000, NEAR_END_MS = 30_000;

    /** the record with this surah's place now (first in the list); a place too near either end forgets it */
    static String put(String record, String reciter, int surah, long ms, long durationMs) {
        String key = key(reciter, surah);
        if (key == null) return record == null ? "" : record;
        boolean none = ms < FROM_MS || (durationMs > 0 && durationMs - ms < NEAR_END_MS);
        StringBuilder out = new StringBuilder();
        int n = 0;
        if (!none) { out.append(key).append(':').append(ms); n++; }
        for (String e : split(record)) {
            if (n >= KEEP) break;
            if (e.startsWith(key + ":")) continue;
            if (out.length() > 0) out.append(';');
            out.append(e);
            n++;
        }
        return out.toString();
    }

    /** the record without this surah (it ended) */
    static String forget(String record, String reciter, int surah) { return put(record, reciter, surah, 0, 0); }

    /** where it stopped in this surah, 0 when nowhere */
    static int get(String record, String reciter, int surah) {
        String key = key(reciter, surah);
        if (key == null) return 0;
        for (String e : split(record)) {
            if (!e.startsWith(key + ":")) continue;
            try {
                long ms = Long.parseLong(e.substring(key.length() + 1));
                return ms > 0 && ms < Integer.MAX_VALUE ? (int) ms : 0;
            } catch (NumberFormatException x) {
                return 0;
            }
        }
        return 0;
    }

    private static String key(String reciter, int surah) {
        if (reciter == null || reciter.isEmpty() || !reciter.matches("[A-Za-z0-9_.-]{1,40}") || surah < 1 || surah > SurahSet.LAST) return null;
        return reciter + ":" + surah;
    }

    private static String[] split(String record) {
        if (record == null || record.isEmpty() || record.length() > 4096) return new String[0];
        java.util.List<String> ok = new java.util.ArrayList<>();
        for (String e : record.split(";")) if (e.matches("[A-Za-z0-9_.-]{1,40}:[0-9]{1,3}:[0-9]{1,10}")) ok.add(e);
        return ok.toArray(new String[0]);
    }
}
