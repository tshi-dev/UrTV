package sa.urtv.home.safe;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The home screen's side of the crash-loop guard (CrashLoop has the rule). MainActivity calls four things: {@link #redirect} first in onCreate and
 * onNewIntent (a loop: «الوضع الآمن» opens instead, and MainActivity does nothing else), {@link #arm} right after it (the crash and
 * hang recorders), {@link #started} at the end of onCreate (the clear after a stable minute), {@link #resumed} from onResume and onPause. Everything here is small and never throws into
 * the home screen; the file is shared with the safe process (same app, same files folder).
 */
public final class CrashGuard {
    private CrashGuard() { }

    static final String DIR = "safe", FILE = "crash-loop.txt";
    /** the extra of «افتح الشاشة الرئيسية من جديد»: MainActivity opens without the redirect once (the troubles were just cleared) */
    public static final String FROM_SAFE = "sa.urtv.home.FROM_SAFE";

    private static volatile boolean installed, inFront;
    private static volatile int alive;   // MainActivity instances in this process: only a crash while the home screen exists is counted
    private static final Object LOCK = new Object();

    // ---- the file (both processes) -----------------------------------------------------------------------------------------------------

    static File file(Context c) {
        File d = new File(c.getFilesDir(), DIR);
        d.mkdirs();
        return new File(d, FILE);
    }

    public static CrashLoop read(Context c) {
        synchronized (LOCK) {
            File f = file(c);
            if (!f.isFile()) return CrashLoop.parse(null);
            try (FileInputStream in = new FileInputStream(f)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] b = new byte[2048];
                int n;
                while ((n = in.read(b)) > 0 && out.size() < 16_384) out.write(b, 0, n);
                return CrashLoop.parse(out.toString("UTF-8"));
            } catch (IOException | RuntimeException e) {
                return CrashLoop.parse(null);
            }
        }
    }

    static void save(Context c, CrashLoop loop) {
        synchronized (LOCK) {
            File f = file(c), tmp = new File(f.getPath() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(loop.write().getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            } catch (IOException | RuntimeException e) {
                return;
            }
            if (!tmp.renameTo(f)) tmp.delete();
        }
    }

    /** «افتح الشاشة الرئيسية من جديد», and a stable minute: the troubles are forgotten. */
    public static void clear(Context c) {
        CrashLoop loop = read(c);
        if (loop.count() == 0) return;
        loop.clear();
        save(c, loop);
    }

    // ---- MainActivity ------------------------------------------------------------------------------------------------------------------

    /**
     * First thing in MainActivity.onCreate and onNewIntent. True when the home screen is in a loop: «الوضع الآمن» was opened and the activity
     * finished; the caller returns at once. Android 11's exit reasons are read here too (an ANR or a native crash leaves no line of ours).
     */
    public static boolean redirect(Activity a, Intent intent) {
        try {
            if (intent != null && intent.getBooleanExtra(FROM_SAFE, false)) return false;
            CrashLoop loop = read(a);
            if (readExitReasons(a, loop)) save(a, loop);
            if (!loop.tripped()) return false;
            openSafe(a);
            a.finish();
            a.overridePendingTransition(0, 0);
            return true;
        } catch (RuntimeException e) {
            return false;   // the home screen comes first
        }
    }

    /**
     * Right after {@link #redirect} said no, before the rest of onCreate (most crashes of a home screen are there): this MainActivity counts as alive,
     * and the crash and hang recorders are put in place once per process.
     */
    public static void arm(Activity a) {
        try {
            final Context c = a.getApplicationContext();
            alive++;
            if (installed) return;
            installed = true;
            final Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override public void uncaughtException(Thread t, Throwable e) {
                    try { if (alive > 0) trouble(c, "crash"); } catch (Throwable ignored) { }
                    if (before != null) before.uncaughtException(t, e);
                    else { Process.killProcess(Process.myPid()); System.exit(10); }
                }
            });
            watchdog(c);
        } catch (RuntimeException ignored) { }
    }

    /** At the end of MainActivity.onCreate: once it has stayed up STABLE_MS with no new trouble, the count is cleared. */
    public static void started(Activity a, final Handler main) {
        final Context c = a.getApplicationContext();
        main.postDelayed(new Runnable() {
            @Override public void run() {
                try {
                    if (alive == 0) return;
                    long left = read(c).newest() + CrashLoop.STABLE_MS - System.currentTimeMillis();
                    if (left > 0) main.postDelayed(this, left);   // a hang it got over in the meantime: a full stable minute after it
                    else clear(c);
                } catch (RuntimeException ignored) { }
            }
        }, CrashLoop.STABLE_MS);
    }

    public static void destroyed() { if (alive > 0) alive--; }

    public static void resumed(boolean front) { inFront = front; }

    /** One trouble is written; when that makes a loop, «الوضع الآمن» opens now (the home process is the home: Android lets it start an activity). */
    static void trouble(Context c, String kind) {
        CrashLoop loop = read(c);
        loop.add(kind, System.currentTimeMillis());
        save(c, loop);
        if (loop.tripped()) openSafe(c);
    }

    static void openSafe(Context c) {
        try {
            c.startActivity(new Intent(c, SafeActivity.class).putExtra(SafeActivity.EXTRA_LOOP, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
        } catch (RuntimeException ignored) { }
    }

    /**
     * The hang recorder: while the home screen is in front, a thread asks the screen's thread for a tick every second; no answer for HANG_MS while the home screen is in front is one
     * «hang» (once per hang). A hang that makes a loop opens «الوضع الآمن» and ends this process, which is not answering anyway.
     */
    private static void watchdog(final Context c) {
        final Handler main = new Handler(Looper.getMainLooper());
        final long[] lastTick = {android.os.SystemClock.uptimeMillis()};
        final Runnable tick = new Runnable() { @Override public void run() { lastTick[0] = android.os.SystemClock.uptimeMillis(); } };
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                boolean reported = false;
                while (true) {
                    if (!inFront || alive == 0) {   // asks nothing while the home screen is not in front (an idle TV is left alone)
                        reported = false;
                        try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
                        lastTick[0] = android.os.SystemClock.uptimeMillis();
                        continue;
                    }
                    main.post(tick);
                    try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
                    long late = android.os.SystemClock.uptimeMillis() - lastTick[0];
                    if (late < CrashLoop.HANG_MS) { reported = false; continue; }
                    if (reported) continue;
                    reported = true;
                    try {
                        trouble(c, "hang");
                        if (read(c).tripped()) { Process.killProcess(Process.myPid()); return; }
                    } catch (RuntimeException ignored) { }
                }
            }
        }, "urtv-home-watchdog");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /** Android 11 and newer: the main process's ANRs and native crashes since the last look; true when something was added. */
    static boolean readExitReasons(Context c, CrashLoop loop) {
        if (Build.VERSION.SDK_INT < 30) return false;
        try {
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return false;
            List<ApplicationExitInfo> list = am.getHistoricalProcessExitReasons(c.getPackageName(), 0, 8);
            boolean added = false;
            long now = System.currentTimeMillis();
            for (int i = list.size() - 1; i >= 0; i--) {   // oldest first
                ApplicationExitInfo x = list.get(i);
                if (!c.getPackageName().equals(x.getProcessName()) || now - x.getTimestamp() > CrashLoop.WINDOW_MS) continue;
                if (x.getReason() == ApplicationExitInfo.REASON_ANR) added |= loop.addExit("anr", x.getTimestamp());
                else if (x.getReason() == ApplicationExitInfo.REASON_CRASH_NATIVE) added |= loop.addExit("native", x.getTimestamp());
            }
            return added;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
