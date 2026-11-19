package sa.urtv.home.tilawah;

/**
 * 4.4 beta 3 «الأذان محفوظ مسبقاً»: the recordings the adhan plays (the usual voice and the Fajr one) are kept on the TV in the app's own files, not the
 * cache Android may empty when the TV runs short of space, and checked for from the home's minute tick (more often in the last {@link #SOON_MS} before
 * a prayer). At the prayer, a chosen voice that is not on the TV while the network is down (or whose stream fails) gives way to another voice that is,
 * and an adhan that could not be played at all is said in «الأذان» (never a silent no-op). The rules, with no Android in here (tests/AdhanKeepRun.java).
 */
final class AdhanKeep {
    private AdhanKeep() { }

    static final long MINUTE = 60_000L;
    /** after a failed try, the next one: an hour, or ten minutes in the last 45 before a prayer */
    static final long RETRY_MS = 60 * MINUTE, SOON_MS = 45 * MINUTE, SOON_RETRY_MS = 10 * MINUTE;
    /** a missed adhan is said for a day */
    static final long MISSED_SHOWN_MS = 24 * 60 * MINUTE;

    /** the voices kept on the TV: the usual one and the Fajr one (once when the same); 1..count, anything else left out */
    static int[] wanted(int chosen, int fajr, int count) {
        boolean c = chosen >= 1 && chosen <= count, f = fajr >= 1 && fajr <= count && fajr != chosen;
        if (c && f) return new int[] {chosen, fajr};
        if (c) return new int[] {chosen};
        if (f) return new int[] {fajr};
        return new int[0];
    }

    static boolean isWanted(int voice, int[] wanted) {
        for (int w : wanted) if (w == voice) return true;
        return false;
    }

    /** whether to fetch now: something wanted is missing, and the last try was long enough ago (a clock that went back counts as long enough) */
    static boolean due(boolean missing, long now, long lastTry, long nextAdhan) {
        if (!missing) return false;
        if (lastTry <= 0 || lastTry > now) return true;
        boolean soon = nextAdhan > now && nextAdhan - now <= SOON_MS;
        return now - lastTry >= (soon ? SOON_RETRY_MS : RETRY_MS);
    }

    /**
     * The saved voice to play when {@code voice} cannot be: the other chosen voice first (the Fajr one for the usual, and the other way), then the
     * lowest saved; -1 when none is saved. {@code saved[v]} for v in 1..count.
     */
    static int stand(int voice, int other, boolean[] saved) {
        if (saved == null) return -1;
        if (other != voice && other >= 1 && other < saved.length && saved[other]) return other;
        for (int v = 1; v < saved.length; v++) if (v != voice && saved[v]) return v;
        return -1;
    }

    /** the saved voices as the prefs keep them, «3,10»: one short text written by the check, read at the prayer (no file is looked at on the UI thread for it) */
    static String savedText(boolean[] saved) {
        StringBuilder b = new StringBuilder();
        for (int v = 1; saved != null && v < saved.length; v++) if (saved[v]) b.append(b.length() == 0 ? "" : ",").append(v);
        return b.toString();
    }

    static boolean[] parseSaved(String text, int count) {
        boolean[] out = new boolean[count + 1];
        if (text == null) return out;
        for (String s : text.split(",")) {
            try { int v = Integer.parseInt(s.trim()); if (v >= 1 && v <= count) out[v] = true; } catch (NumberFormatException ignored) { }
        }
        return out;
    }

    /** whether the wanted voices are all saved */
    static boolean allSaved(int[] wanted, boolean[] saved) {
        for (int w : wanted) if (saved == null || w >= saved.length || !saved[w]) return false;
        return true;
    }

    /** whether «الأذان» says the last adhan was missed: within a day, and not from the future */
    static boolean missedShown(long missedAt, long now) {
        return missedAt > 0 && missedAt <= now && now - missedAt < MISSED_SHOWN_MS;
    }
}
