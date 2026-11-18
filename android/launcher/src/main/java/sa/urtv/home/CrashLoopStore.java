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

    /** read-modify-write is one step: the crash handler, the hook and the background writer never interleave */
    private static final Object LOCK = new Object();
    /** 4.4 review F7 (R1): opened() and drew() are written here, never with an fsync on the UI thread; the crash handler and the hook still write at once */
    private static final java.util.concurrent.ExecutorService BG = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "crashloop-store");
        t.setDaemon(true);
        return t;
    });

    private static void later(final Context c, final java.util.function.Function<CrashLoop, CrashLoop> step) {
        final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        try {
            BG.execute(() -> { synchronized (LOCK) { write(app, step.apply(read(app))); } });
        } catch (RuntimeException ignored) {
        }
    }

    static void write(Context c, CrashLoop s) {
        try (FileOutputStream o = new FileOutputStream(new File(c.getFilesDir(), CrashLoop.FILE))) {
            o.write(s.text().getBytes(StandardCharsets.UTF_8));
            o.getFD().sync();
        } catch (Exception ignored) {
        }
    }

    /** HomeApp.onCreate, first: this start is counted, and a crash of this process is written before the old handler takes it down */
    static void hook(final Context app) {
        synchronized (LOCK) { write(app, read(app).started(System.currentTimeMillis())); }
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try { synchronized (LOCK) { write(app, read(app).crashed(System.currentTimeMillis())); } } catch (Throwable ignored) { }
            if (prev != null) prev.uncaughtException(t, e);
        });
    }

    static boolean safe(Context c) { return read(c).safe(System.currentTimeMillis()); }

    /** the home screen opened (MainActivity.onCreate, not in safe mode): it must draw within CrashLoop.FRAME_MS */
    static void opened(Context c) {
        drewMarked = false;   // a second home screen in this process (the activity made again) is judged on its own first frame (4.4 review F8)
        final long now = System.currentTimeMillis();
        later(c, s -> s.opened(now));
    }

    private static volatile boolean drewMarked;

    /** the home screen drew its first frame (once per process) */
    static void drew(Context c) {
        if (drewMarked) return;
        drewMarked = true;
        final long now = System.currentTimeMillis();
        later(c, s -> s.drewFrame(now));
    }

    static void clear(Context c) { synchronized (LOCK) { write(c, read(c).cleared()); } }
}
