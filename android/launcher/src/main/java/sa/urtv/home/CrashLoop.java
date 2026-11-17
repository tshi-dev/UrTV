package sa.urtv.home;

/**
 * 4.3.5 (R7, a CVTE Android 9 set left on a black screen): when the home dies again and again right after it starts, the TV must never be left
 * with nothing. A crash that comes within QUICK_MS of its process's start is a quick one; QUICK_LIMIT of them within WINDOW_MS put the home in
 * safe mode: its next start opens SafeActivity (no WebView, no other app's data) instead of the home screen, with the TV's own home one press
 * away. «حاول مرة ثانية» (or WINDOW_MS without a quick crash) clears it. Plain Java: the file is two lines of numbers (tests/crash_loop_test.py).
 */
final class CrashLoop {
    static final long QUICK_MS = 60_000L, WINDOW_MS = 10 * 60_000L;
    static final int QUICK_LIMIT = 2;
    static final String FILE = "crash-loop.txt";

    /** the last start of the process, and the times of the recent quick crashes (oldest first, at most QUICK_LIMIT) */
    final long startedAt;
    final long[] quick;

    CrashLoop(long startedAt, long[] quick) { this.startedAt = startedAt; this.quick = quick; }

    static CrashLoop parse(String t) {
        long start = 0;
        long[] q = new long[0];
        if (t != null) {
            String[] lines = t.split("\n");
            try { if (lines.length > 0 && !lines[0].trim().isEmpty()) start = Long.parseLong(lines[0].trim()); } catch (NumberFormatException e) { start = 0; }
            if (lines.length > 1 && !lines[1].trim().isEmpty()) {
                String[] p = lines[1].trim().split(",");
                long[] r = new long[p.length];
                int n = 0;
                for (String s : p) { try { r[n++] = Long.parseLong(s.trim()); } catch (NumberFormatException ignored) { } }
                q = java.util.Arrays.copyOf(r, n);
            }
        }
        return new CrashLoop(start, q);
    }

    String text() {
        StringBuilder b = new StringBuilder().append(startedAt).append('\n');
        for (int i = 0; i < quick.length; i++) b.append(i == 0 ? "" : ",").append(quick[i]);
        return b.append('\n').toString();
    }

    /** the process started now */
    CrashLoop started(long now) { return new CrashLoop(now, recent(now)); }

    /** the process is dying now: a quick crash when it is within QUICK_MS of the start */
    CrashLoop crashed(long now) {
        long[] r = recent(now);
        if (startedAt <= 0 || now - startedAt < 0 || now - startedAt > QUICK_MS) return new CrashLoop(startedAt, r);
        long[] q = java.util.Arrays.copyOf(r, r.length + 1);
        q[r.length] = now;
        if (q.length > QUICK_LIMIT) q = java.util.Arrays.copyOfRange(q, q.length - QUICK_LIMIT, q.length);
        return new CrashLoop(startedAt, q);
    }

    /** the home should not start its screen now: QUICK_LIMIT quick crashes within WINDOW_MS */
    boolean safe(long now) { return recent(now).length >= QUICK_LIMIT; }

    /** «حاول مرة ثانية»: nothing counted */
    CrashLoop cleared() { return new CrashLoop(startedAt, new long[0]); }

    private long[] recent(long now) {
        int n = 0;
        long[] r = new long[quick.length];
        for (long t : quick) if (now - t >= 0 && now - t < WINDOW_MS) r[n++] = t;
        return java.util.Arrays.copyOf(r, n);
    }
}
