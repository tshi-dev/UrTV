package sa.urtv.home;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** CrashLoop's file in the app's own files (written at once: the process may be dying), and the process's hook. Never throws. */
final class CrashLoopStore {
    private CrashLoopStore() { }

    static CrashLoop read(Context c) {
        try {
            File f = new File(c.getFilesDir(), CrashLoop.FILE);
            if (!f.isFile()) return CrashLoop.parse(null);
            return CrashLoop.parse(new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return CrashLoop.parse(null);
        }
    }

    /** the in-memory state: held for the few microseconds a step takes, never across the disk (4.3.5 review F7) */
    private static final Object LOCK = new Object();
    /** the file: held by the background writer and the dying process's handler while they write and fsync */
    private static final Object FILE_LOCK = new Object();
    /** 4.4 review F7 (R1): opened() and drew() are written here, never with an fsync on the UI thread; the crash handler and the hook still write at once */
    private static final java.util.concurrent.ExecutorService BG = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "crashloop-store");
        t.setDaemon(true);
        return t;
    });

    /**
     * 4.3.5 review F4 (R1): the process's state, kept here and changed at once (no disk); the file is written behind it on BG. Only the crash
     * handler writes on the spot (the process is dying). The hook still reads the small file once, at the process's start.
     */
    private static volatile CrashLoop state;

    private static void save(final Context c, final CrashLoop s) {
        final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        try {
            BG.execute(() -> {
                synchronized (FILE_LOCK) {   // the disk under its own lock: a UI step (LOCK only) never waits on this fsync
                    synchronized (LOCK) { if (state != s) return; }   // checked with the file held: a newer state (a crash) is never overwritten by this one
                    write(app, s);
                }
            });
        } catch (RuntimeException ignored) {
        }
    }

    private static void step(Context c, java.util.function.UnaryOperator<CrashLoop> f) {
        CrashLoop s;
        synchronized (LOCK) { s = f.apply(state != null ? state : read(c)); state = s; }
        save(c, s);
    }

    static void write(Context c, CrashLoop s) {
        try (FileOutputStream o = new FileOutputStream(new File(c.getFilesDir(), CrashLoop.FILE))) {
            o.write(s.text().getBytes(StandardCharsets.UTF_8));
            o.getFD().sync();
        } catch (Exception ignored) {
        }
    }

    /** HomeApp.onCreate, first: this start is counted (in memory at once, on disk behind it), and a crash of this process is written before the old handler takes it down */
    static void hook(final Context app) {
        final long now = System.currentTimeMillis();
        step(app, s -> s.started(now, BuildConfig.VERSION_CODE));   // a new version starts with nothing counted (review F5)
        if (android.os.Build.VERSION.SDK_INT >= 30) {   // 4.4 beta 3: how the last process ended, asked of Android off the UI thread (an older TV cannot say: nothing counted)
            try { EXITS.execute(() -> exits(app)); } catch (RuntimeException ignored) { }
        }
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                CrashLoop s;
                synchronized (LOCK) { s = (state != null ? state : read(app)).crashed(System.currentTimeMillis()); state = s; }
                synchronized (FILE_LOCK) { write(app, s); }   // the process is dying: written now
            } catch (Throwable ignored) { }
            if (prev != null) prev.uncaughtException(t, e);
        });
    }

    private static final java.util.concurrent.ExecutorService EXITS = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "crash-exits");
        t.setDaemon(true);
        return t;
    });

    /** the importance of a home screen in front (ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND), and of what the person still sees (PERCEPTIBLE) */
    static final int FOREGROUND = 100, PERCEPTIBLE = 230;

    /**
     * 4.4 beta 3: the home's own process ended by a low-memory kill (ApplicationExitInfo.REASON_LOW_MEMORY) since the last one judged: in front, judged by
     * CrashLoop.lowMemoryKill (it counts toward safe mode like a quick crash); in front or perceptible, a sign of a weak TV for «الوضع الخفيف» (LiteAuto.OOM).
     * The last kill judged is kept, so each counts once. Android 11+ only.
     */
    @android.annotation.TargetApi(30)
    private static void exits(Context app) {
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
            android.content.SharedPreferences sp = app.getSharedPreferences("crashloop", Context.MODE_PRIVATE);
            long seen = sp.getLong("oom_seen", 0), latest = seen;
            if (android.os.Build.VERSION.SDK_INT < 30) return;   // (the hook checks it too; ApplicationExitInfo is Android 11's)
            for (android.app.ApplicationExitInfo e : am.getHistoricalProcessExitReasons(app.getPackageName(), 0, 8)) {
                long[] k = kill(e);   // {at, importance} of a low-memory kill of the home's process; null for any other end
                if (e.getTimestamp() > latest) latest = e.getTimestamp();
                if (k == null || k[0] <= seen) continue;
                final long at = k[0], now = System.currentTimeMillis();
                final boolean front = k[1] <= FOREGROUND;
                android.util.Log.i("UrTVCrash", "low-memory kill, importance " + k[1]);
                step(app, s -> s.lowMemoryKill(at, front, now));
                if (k[1] <= PERCEPTIBLE) LiteAutoStore.noteAt(app, LiteAuto.OOM, at);   // at the kill's own time (old ones fall out of LiteAuto's 14 days)
            }
            if (latest != seen) sp.edit().putLong("oom_seen", latest).commit();
        } catch (RuntimeException ignored) { }
    }

    /** {at, importance} when Android says this ended the home's own process for memory, else null */
    @android.annotation.TargetApi(30)
    private static long[] kill(Object o) {   // (an Object: the class is named only after the version check, for an Android 8 TV)
        if (android.os.Build.VERSION.SDK_INT < 30 || !(o instanceof android.app.ApplicationExitInfo)) return null;
        android.app.ApplicationExitInfo e = (android.app.ApplicationExitInfo) o;
        if (e.getReason() != android.app.ApplicationExitInfo.REASON_LOW_MEMORY || !"sa.urtv.home".equals(e.getProcessName())) return null;
        return new long[] {e.getTimestamp(), e.getImportance()};
    }

    /** MainActivity.onCreate: the state in memory (the hook's), never the disk */
    static boolean safe(Context c) { CrashLoop s = state; return (s != null ? s : read(c)).safe(System.currentTimeMillis()); }

    /** the home screen opened (MainActivity.onCreate, not in safe mode): it must draw within CrashLoop.FRAME_MS */
    static void opened(Context c) {
        drewMarked = false;   // a second home screen in this process (the activity made again) is judged on its own first frame (4.4 review F8)
        final long now = System.currentTimeMillis();
        step(c, s -> s.opened(now));
    }

    private static volatile boolean drewMarked;

    /** the home screen drew its first frame (once per process) */
    static void drew(Context c) {
        if (drewMarked) return;
        drewMarked = true;
        final long now = System.currentTimeMillis();
        CrashLoop was = state;   // 4.4 beta 3 (LiteAuto): a first frame slower than SLOW_MS is a sign of a weak TV
        if (was != null && was.openedAt > 0 && now - was.openedAt > LiteAuto.SLOW_MS) LiteAutoStore.note(c, LiteAuto.SLOW);
        step(c, s -> s.drewFrame(now));
    }

    /** «حاول مرة ثانية» */
    static void clear(Context c) { step(c, CrashLoop::cleared); }
}
