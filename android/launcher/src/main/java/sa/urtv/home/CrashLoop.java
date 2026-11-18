package sa.urtv.home;

/**
 * 4.3.5 (R7, a CVTE Android 9 set left on a black screen): when the home dies or hangs again and again right after it starts, the TV must never be
 * left with nothing. A crash within QUICK_MS of its process's start is a quick one; so is a home screen that opened and never drew its first frame
 * within FRAME_MS (a hang: counted at the next start, once, and not again when its crash was already counted). QUICK_LIMIT of them within WINDOW_MS
 * put the home in safe mode: its next start opens SafeActivity (no WebView, no other app's data) instead of the home screen, with the TV's own home
 * one press away. «حاول مرة ثانية» (or WINDOW_MS without one) clears it. Plain Java; the file is three lines of numbers (tests/crash_loop_test.py).
 */
final class CrashLoop {
    static final long QUICK_MS = 60_000L, FRAME_MS = 20_000L, WINDOW_MS = 10 * 60_000L;
    static final int QUICK_LIMIT = 2;
    static final String FILE = "crash-loop.txt";

    /** the last start of the process; the recent quick crashes (oldest first, at most QUICK_LIMIT); the home screen of this start: when it opened (0: not), whether it drew in time, whether its crash was counted */
    final long startedAt;
    final long[] quick;
    final long openedAt;
    final boolean drew, crashCounted;

    CrashLoop(long startedAt, long[] quick) { this(startedAt, quick, 0, false, false); }

    /** 4.3.5 review F5: the app's versionCode at the last start; a new version starts with nothing counted (an update that fixes the crash) */
    long code;

    /** 4.4 beta 3: the start before this one and whether it opened the home screen: a low-memory kill of that process is judged against them (lowMemoryKill) */
    long prevStart;
    boolean prevOpened;

    CrashLoop(long startedAt, long[] quick, long openedAt, boolean drew, boolean crashCounted) {
        this.startedAt = startedAt; this.quick = quick; this.openedAt = openedAt; this.drew = drew; this.crashCounted = crashCounted;
    }

    static CrashLoop parse(String t) {
        long start = 0, opened = 0;
        boolean drew = false, counted = false;
        long[] q = new long[0];
        if (t != null) {
            String[] lines = t.split("\n");
            start = num(lines, 0);
            if (lines.length > 1 && !lines[1].trim().isEmpty()) {
                String[] p = lines[1].trim().split(",");
                long[] r = new long[p.length];
                int n = 0;
                for (String s : p) { try { r[n++] = Long.parseLong(s.trim()); } catch (NumberFormatException ignored) { } }
                q = java.util.Arrays.copyOf(r, n);
            }
            if (lines.length > 2) {
                String[] f = lines[2].trim().split(" ");
                try { opened = Long.parseLong(f[0]); } catch (NumberFormatException e) { opened = 0; }
                drew = f.length > 1 && "1".equals(f[1]);
                counted = f.length > 2 && "1".equals(f[2]);
            }
        }
        CrashLoop out = new CrashLoop(start, q, opened, drew, counted);
        if (t != null) { String[] l = t.split("\n"); if (l.length > 3) { try { out.code = Long.parseLong(l[3].trim()); } catch (NumberFormatException e) { out.code = 0; } } }
        if (t != null) {   // 4.4 beta 3: the fifth line, «prevStart prevOpened» (an older file has none: nothing to judge)
            String[] l = t.split("\n");
            if (l.length > 4) { String[] f = l[4].trim().split(" "); try { out.prevStart = Long.parseLong(f[0]); } catch (NumberFormatException e) { out.prevStart = 0; } out.prevOpened = f.length > 1 && "1".equals(f[1]); }
        }
        return out;
    }

    private static long num(String[] lines, int i) {
        try { return lines.length > i && !lines[i].trim().isEmpty() ? Long.parseLong(lines[i].trim()) : 0; } catch (NumberFormatException e) { return 0; }
    }

    String text() {
        StringBuilder b = new StringBuilder().append(startedAt).append('\n');
        for (int i = 0; i < quick.length; i++) b.append(i == 0 ? "" : ",").append(quick[i]);
        return b.append('\n').append(openedAt).append(drew ? " 1" : " 0").append(crashCounted ? " 1" : " 0").append('\n').append(code).append('\n')
            .append(prevStart).append(prevOpened ? " 1" : " 0").append('\n').toString();
    }

    /** the process started now: a home screen of the last start that never drew in time (and was not counted as a crash) is a quick one */
    CrashLoop started(long now) { return started(now, code); }

    /** the process started now, as versionCode {@code ver}: another version than the last start's begins with nothing counted (review F5) */
    CrashLoop started(long now, long ver) {
        long[] r = recent(now);
        if (openedAt > 0 && !drew && !crashCounted) r = add(r, openedAt, now);
        if (code != 0 && ver != code) r = new long[0];
        CrashLoop out = new CrashLoop(now, r, 0, false, false);
        out.code = ver;
        out.prevStart = startedAt;   // 4.4 beta 3: the start that just ended, for a low-memory kill of it (lowMemoryKill)
        out.prevOpened = openedAt > 0;
        return out;
    }

    /** the home screen opened now (not the safe page) */
    CrashLoop opened(long now) { return keep(new CrashLoop(startedAt, quick, now, false, crashCounted)); }

    /** the same version as this one (every step but started keeps it) */
    private CrashLoop keep(CrashLoop n) { n.code = code; n.prevStart = prevStart; n.prevOpened = prevOpened; return n; }

    /**
     * 4.4 beta 3: Android said (ApplicationExitInfo, Android 11+, read at this start) that the last process was killed for memory at {@code killedAt}, in front
     * ({@code foreground}) or not. A kill of the home screen in front within QUICK_MS of that process's start counts as a quick crash (two put the home in safe mode,
     * as crashes do); a kill in the background (normal on any TV) or of a process that never opened the home screen does not. Once per kill (the caller keeps the
     * last one it judged).
     */
    CrashLoop lowMemoryKill(long killedAt, boolean foreground, long now) {
        long[] r = recent(now);
        boolean quickOne = foreground && prevOpened && prevStart > 0 && killedAt - prevStart >= 0 && killedAt - prevStart <= QUICK_MS && now - killedAt >= 0 && now - killedAt < WINDOW_MS;
        return keep(new CrashLoop(startedAt, quickOne ? add(r, killedAt, now) : r, openedAt, drew, crashCounted));
    }

    /** the home screen drew its first frame now: in time when within FRAME_MS of its opening */
    CrashLoop drewFrame(long now) {
        boolean inTime = openedAt > 0 && now - openedAt >= 0 && now - openedAt <= FRAME_MS;
        return keep(new CrashLoop(startedAt, quick, openedAt, drew || inTime, crashCounted));
    }

    /**
     * the process is dying now: a quick crash when it is within QUICK_MS of the start AND the home screen was opened in this process
     * (4.3.5 review F3: a background service or receiver that crash-loops never takes the working home screen away)
     */
    CrashLoop crashed(long now) {
        long[] r = recent(now);
        if (openedAt <= 0 || startedAt <= 0 || now - startedAt < 0 || now - startedAt > QUICK_MS) return keep(new CrashLoop(startedAt, r, openedAt, drew, crashCounted));
        return keep(new CrashLoop(startedAt, add(r, now, now), openedAt, drew, true));
    }

    /** the home should not start its screen now: QUICK_LIMIT quick crashes or hangs within WINDOW_MS */
    boolean safe(long now) { return recent(now).length >= QUICK_LIMIT; }

    /** «حاول مرة ثانية»: nothing counted (this start's screen is not held against it either) */
    CrashLoop cleared() { return keep(new CrashLoop(startedAt, new long[0], 0, false, false)); }

    private long[] add(long[] r, long at, long now) {
        if (now - at < 0 || now - at >= WINDOW_MS) return r;
        long[] q = java.util.Arrays.copyOf(r, r.length + 1);
        q[r.length] = at;
        java.util.Arrays.sort(q);
        return q.length > QUICK_LIMIT ? java.util.Arrays.copyOfRange(q, q.length - QUICK_LIMIT, q.length) : q;
    }

    private long[] recent(long now) {
        int n = 0;
        long[] r = new long[quick.length];
        for (long t : quick) if (now - t >= 0 && now - t < WINDOW_MS) r[n++] = t;
        return java.util.Arrays.copyOf(r, n);
    }
}
