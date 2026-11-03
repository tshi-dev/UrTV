package sa.urtv.home.setup;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import java.util.ArrayDeque;

/**
 * Holds the process while a setup run is going: a foreground service whose only job is to run the runner's thread, so the run outlives the setup
 * screen (Back, Home, the screensaver) and is not ended as a cached process. Started from the screen, and from the TV's start when a full run
 * resumes after a restart. Not exported; it stops itself when the run ends. While a run goes it also holds the screen on with a wake lock of the
 * process (not of the setup screen's window, whose keep-the-screen-on flag a TCL ignored): no screensaver and no sleep while the TV's prompt waits.
 */
public class SetupService extends Service {
    private static final String CHANNEL = "urtv-setup";
    private static final int NOTIF = 7301;
    private static final ArrayDeque<Runnable> JOBS = new ArrayDeque<>();
    private static int live;
    private static android.os.PowerManager.WakeLock awake;

    /** The screen kept on while a run goes; let go when the last one ends (ON_AFTER_RELEASE: the TV's own idle timer starts from then). */
    @SuppressWarnings("deprecation")
    private void hold(boolean on) {
        synchronized (JOBS) {
            if (on && awake == null) {
                android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
                if (pm == null) return;
                awake = pm.newWakeLock(android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK | android.os.PowerManager.ON_AFTER_RELEASE, "urtv:setup");
                awake.setReferenceCounted(false);
                awake.acquire(2 * 3600_000L);   // never longer than two hours, whatever happens
            } else if (!on && awake != null) {
                try { awake.release(); } catch (RuntimeException ignored) { }
                awake = null;
            }
        }
    }

    /** Runs the job inside the service; where the service cannot be started, on a plain thread (the run still happens). */
    static void run(Context c, Runnable job) {
        Context app = c.getApplicationContext();
        synchronized (JOBS) { JOBS.addLast(job); }
        try {
            app.startForegroundService(new Intent(app, SetupService.class));
        } catch (RuntimeException e) {
            Runnable r;
            synchronized (JOBS) { r = JOBS.pollLast(); }
            if (r != null) new Thread(r, "setup-run").start();
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, final int startId) {
        startForeground(NOTIF, notification());
        final Runnable job;
        synchronized (JOBS) { job = JOBS.pollFirst(); if (job != null) live++; }
        if (job == null) {
            synchronized (JOBS) { if (live == 0) stopSelf(startId); }
            return START_NOT_STICKY;
        }
        hold(true);
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    job.run();
                } finally {
                    synchronized (JOBS) {
                        live--;
                        if (live == 0 && JOBS.isEmpty()) {
                            hold(false);
                            stopForeground(true);
                            stopSelf();
                        }
                    }
                }
            }
        }, "setup-run").start();
        return START_NOT_STICKY;
    }

    private Notification notification() {
        sa.urtv.home.S.init(this);   // the kept language (a service has no screen to ask)
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.createNotificationChannel(new NotificationChannel(CHANNEL, sa.urtv.home.S.t("إعداد تلفزيوني"), NotificationManager.IMPORTANCE_LOW));
        return new Notification.Builder(this, CHANNEL)
            .setContentTitle(sa.urtv.home.S.t("إعداد تلفزيوني"))
            .setContentText(sa.urtv.home.S.t("الإعداد يعمل… لا تطفئ التلفزيون."))
            .setSmallIcon(sa.urtv.home.R.drawable.ic_settings)
            .setOngoing(true)
            .build();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
