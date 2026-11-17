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

    static void write(Context c, CrashLoop s) {
        try (FileOutputStream o = new FileOutputStream(new File(c.getFilesDir(), CrashLoop.FILE))) {
            o.write(s.text().getBytes(StandardCharsets.UTF_8));
            o.getFD().sync();
        } catch (Exception ignored) {
        }
    }

    /** HomeApp.onCreate, first: this start is counted, and a crash of this process is written before the old handler takes it down */
    static void hook(final Context app) {
        write(app, read(app).started(System.currentTimeMillis()));
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try { write(app, read(app).crashed(System.currentTimeMillis())); } catch (Throwable ignored) { }
            if (prev != null) prev.uncaughtException(t, e);
        });
    }

    static boolean safe(Context c) { return read(c).safe(System.currentTimeMillis()); }

    static void clear(Context c) { write(c, read(c).cleared()); }
}
