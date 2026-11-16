package sa.urtv.home.tilawah;

/**
 * 4.4 radio reliability: the live stations' troubles of the last seven days, kept across restarts (the counters of MusicService start again
 * with the app), for the player status (radio_week=) and optimize/share.py's report. One short record in the tilawah prefs:
 * "day,stalls,errors,reconnects,rebuffers;…", one entry per day that had any, at most DAYS. Pure logic (no Android classes):
 * tests/radio_week_test.py.
 */
final class RadioWeek {
    private RadioWeek() { }

    static final int STALL = 0, ERROR = 1, RECONNECT = 2, REBUFFER = 3, KINDS = 4;
    static final int DAYS = 7;

    /** the record with one more of `kind` on `day` (days since 1970, the TV's own); a malformed entry is dropped, and days older than a week */
    static String add(String record, long day, int kind) {
        if (kind < 0 || kind >= KINDS) return record == null ? "" : record;
        StringBuilder out = new StringBuilder();
        boolean found = false;
        for (long[] e : parse(record)) {
            if (e[0] <= day - DAYS || e[0] > day) continue;
            if (e[0] == day) { e[1 + kind]++; found = true; }
            append(out, e);
        }
        if (!found) { long[] e = new long[1 + KINDS]; e[0] = day; e[1 + kind] = 1; append(out, e); }
        return out.toString();
    }

    /** stalls, errors, reconnects, rebuffers over the last DAYS days up to `today` */
    static long[] totals(String record, long today) {
        long[] t = new long[KINDS];
        for (long[] e : parse(record)) {
            if (e[0] <= today - DAYS || e[0] > today) continue;
            for (int k = 0; k < KINDS; k++) t[k] += e[1 + k];
        }
        return t;
    }

    /** the status field: "stalls.errors.reconnects.rebuffers" */
    static String field(String record, long today) {
        long[] t = totals(record, today);
        return t[0] + "." + t[1] + "." + t[2] + "." + t[3];
    }

    private static java.util.List<long[]> parse(String record) {
        java.util.List<long[]> out = new java.util.ArrayList<>();
        if (record == null || record.length() > 1024) return out;
        for (String part : record.split(";")) {
            String[] f = part.split(",");
            if (f.length != 1 + KINDS) continue;
            long[] e = new long[1 + KINDS];
            try {
                for (int i = 0; i < f.length; i++) { e[i] = Long.parseLong(f[i]); if (e[i] < 0 || e[i] > 1_000_000_000L) throw new NumberFormatException(); }
            } catch (NumberFormatException x) {
                continue;
            }
            out.add(e);
        }
        return out;
    }

    private static void append(StringBuilder b, long[] e) {
        if (b.length() > 0) b.append(';');
        for (int i = 0; i < e.length; i++) { if (i > 0) b.append(','); b.append(e[i]); }
    }
}
