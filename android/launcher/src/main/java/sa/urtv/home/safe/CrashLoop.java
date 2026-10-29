package sa.urtv.home.safe;

import java.util.ArrayList;
import java.util.List;

/**
 * The crash-loop counter of the home screen, with no Android in it (tests/CrashLoopRun.java). The main process writes one line per trouble of
 * MainActivity to a small file (files/safe/crash-loop.txt): «crash <ms>» (an uncaught exception), «hang <ms>» (the screen's thread did not answer for
 * HANG_MS while it was in front), and, read back from Android 11's exit reasons at the next start, «anr <ms>» and «native <ms>». LIMIT of them inside
 * WINDOW_MS is a loop: the next Home press opens «الوضع الآمن» (SafeActivity, its own process) instead, which reads the same file to say what
 * happened. A start that stays up STABLE_MS clears it, and so does «افتح الشاشة الرئيسية من جديد».
 */
public final class CrashLoop {
    public static final int LIMIT = 3, KEEP = 10;
    public static final long WINDOW_MS = 120_000L, STABLE_MS = 60_000L, HANG_MS = 8_000L;
    /** an ANR of Android's own list this close after a «hang» we wrote is the same trouble, counted once */
    static final long SAME_MS = 30_000L;
    static final String[] KINDS = {"crash", "hang", "anr", "native"};

    private final List<String> kinds = new ArrayList<>();
    private final List<Long> times = new ArrayList<>();
    private long seen;   // the newest exit reason already read (so one is never counted twice)

    /** The file's text (null or garbage: nothing happened). Unknown lines are dropped. */
    public static CrashLoop parse(String text) {
        CrashLoop c = new CrashLoop();
        if (text == null) return c;
        for (String line : text.split("\n")) {
            String[] p = line.trim().split(" ");
            if (p.length != 2) continue;
            long at;
            try { at = Long.parseLong(p[1]); } catch (NumberFormatException e) { continue; }
            if (at < 0) continue;
            if (p[0].equals("seen")) c.seen = Math.max(c.seen, at);
            else if (known(p[0])) c.add(p[0], at);
        }
        return c;
    }

    private static boolean known(String kind) {
        for (String k : KINDS) if (k.equals(kind)) return true;
        return false;
    }

    /** One trouble now (crash or hang). The oldest are dropped past KEEP. */
    public void add(String kind, long at) {
        if (!known(kind)) throw new IllegalArgumentException(kind);
        int i = times.size();
        while (i > 0 && times.get(i - 1) > at) i--;   // kept in time order
        kinds.add(i, kind);
        times.add(i, at);
        while (times.size() > KEEP) { kinds.remove(0); times.remove(0); }
    }

    /**
     * One of Android's exit reasons of the main process («anr» or «native»; a Java crash is already written by our own handler). Returns whether it
     * was counted: not when it is not newer than the last one read, nor when it is the ANR of a «hang» we wrote already.
     */
    public boolean addExit(String kind, long at) {
        if (!kind.equals("anr") && !kind.equals("native")) throw new IllegalArgumentException(kind);
        if (at <= seen) return false;
        seen = at;
        if (kind.equals("anr")) for (int i = 0; i < times.size(); i++) {
            if (kinds.get(i).equals("hang") && at >= times.get(i) && at - times.get(i) <= SAME_MS) return false;
        }
        add(kind, at);
        return true;
    }

    /** LIMIT troubles inside any WINDOW_MS: a loop. It stays a loop until it is cleared (a stable start, or the safe screen's button). */
    public boolean tripped() {
        for (int i = 0; i + LIMIT - 1 < times.size(); i++) if (times.get(i + LIMIT - 1) - times.get(i) <= WINDOW_MS) return true;
        return false;
    }

    /** Forget the troubles (a stable start, or «افتح الشاشة الرئيسية من جديد»); the exit reasons already read stay read. */
    public void clear() {
        kinds.clear();
        times.clear();
    }

    public int count() { return times.size(); }

    /** The time of the newest trouble, 0 without one. */
    public long newest() { return times.isEmpty() ? 0 : times.get(times.size() - 1); }

    public long seen() { return seen; }

    /** The file's text. */
    public String write() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < times.size(); i++) b.append(kinds.get(i)).append(' ').append(times.get(i)).append('\n');
        if (seen > 0) b.append("seen ").append(seen).append('\n');
        return b.toString();
    }

    /** The safe screen's line: what happened, in plain Arabic (empty when nothing did). */
    public String what() {
        if (times.isEmpty()) return "";
        int stops = 0, hangs = 0;
        for (String k : kinds) if (k.equals("hang") || k.equals("anr")) hangs++; else stops++;
        String n = arabicCount(times.size());
        if (hangs == 0) return "الشاشة الرئيسية توقفت فجأة " + n + " متتالية، ففتحنا لك «الوضع الآمن».";
        if (stops == 0) return "الشاشة الرئيسية علّقت ولم تستجب " + n + " متتالية، ففتحنا لك «الوضع الآمن».";
        return "الشاشة الرئيسية توقفت أو علّقت " + n + " متتالية، ففتحنا لك «الوضع الآمن».";
    }

    static String arabicCount(int n) {
        switch (n) {
            case 1: return "مرة";
            case 2: return "مرتين";
            default: return n + " مرات";   // 3 to 10 (KEEP)
        }
    }
}
