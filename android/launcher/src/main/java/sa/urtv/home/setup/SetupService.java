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
 * resumes after a restart. Not exported; it stops itself when the run ends.
 */
public class SetupService extends Service {
    private static final String CHANNEL = "urtv-setup";
    private static final int NOTIF = 7301;
    private static final ArrayDeque<Runnable> JOBS = new ArrayDeque<>();
    private static int live;

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
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    job.run();
                } finally {
                    synchronized (JOBS) {
                        live--;
                        if (live == 0 && JOBS.isEmpty()) {
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
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.createNotificationChannel(new NotificationChannel(CHANNEL, "إعداد تلفزيوني", NotificationManager.IMPORTANCE_LOW));
        return new Notification.Builder(this, CHANNEL)
            .setContentTitle("إعداد تلفزيوني")
            .setContentText("الإعداد يعمل… لا تطفئ التلفزيون.")
            .setSmallIcon(sa.urtv.home.R.drawable.ic_settings)
            .setOngoing(true)
            .build();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
