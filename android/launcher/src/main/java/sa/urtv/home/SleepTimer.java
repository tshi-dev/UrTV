package sa.urtv.home;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;

import sa.urtv.home.tilawah.MusicService;

/**
 * «مؤقت النوم» for the whole TV: 15, 30, 60 or 90 minutes, then the TV goes to sleep. One alarm (AlarmManager, so it fires with the
 * TV idle and the home screen closed); the end time is kept, so the pill and `optimize/dock.sh sleep` can say how long is left.
 *
 * What happens at the end, in this order:
 *  1. whatever is playing (the Quran, the radio, the adhan) is stopped;
 *  2. if the sleep service is on (SleepService, an accessibility service), the screen is turned off like the power key;
 *  3. otherwise the screensaver is brought up with no wake lock, so the TV's own idle timer takes over from there. Android 10 and
 *     later may refuse that from the background when another app is in front.
 * The timer does not survive a restart of the TV: it is not shown after one (SleepWindow), and SleepBootReceiver forgets it.
 */
final class SleepTimer {
    static final String ACTION = "sa.urtv.home.SLEEP_NOW";
    /** sent to our own screens when the timer starts, ends or is cancelled */
    static final String CHANGED = "sa.urtv.home.SLEEP_CHANGED";
    static final int[] MINUTES = {15, 30, 60, 90};

    private SleepTimer() { }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("sleep", Context.MODE_PRIVATE); }

    private static PendingIntent alarm(Context c) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getBroadcast(c, 7, new Intent(c, SleepReceiver.class).setAction(ACTION), flags);
    }

    /** minutes from now; 0 or less cancels. */
    static void start(Context c, int minutes) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(alarm(c));
        if (minutes <= 0) {
            prefs(c).edit().remove("end").remove("since").apply();
        } else {
            long at = System.currentTimeMillis() + minutes * 60_000L;
            try {
                if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarm(c));
                else am.setExact(AlarmManager.RTC_WAKEUP, at, alarm(c));
            } catch (SecurityException e) {
                am.set(AlarmManager.RTC_WAKEUP, at, alarm(c));   // exact alarms refused: a minute or so late is still the same night
            }
            prefs(c).edit().putLong("end", at).putLong("since", SystemClock.elapsedRealtime()).apply();
        }
        c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));
    }

    /** when it ends, in millis; 0 when no timer is running (or the TV restarted and lost it) */
    static long endsAt(Context c) {
        long end = prefs(c).getLong("end", 0);
        return SleepWindow.running(end, System.currentTimeMillis(), prefs(c).getLong("since", 0), SystemClock.elapsedRealtime()) ? end : 0;
    }

    /** The TV has started again: no alarm survived it, so a timer saved before is forgotten and never begins late. */
    static void forgetAfterBoot(Context c) {
        if (prefs(c).getLong("end", 0) == 0) return;
        prefs(c).edit().remove("end").remove("since").apply();
        c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));
    }

    /** whole minutes left, rounded up; 0 when none */
    static int minutesLeft(Context c) {
        long end = endsAt(c);
        return end == 0 ? 0 : (int) Math.max(1, (end - System.currentTimeMillis() + 59_999) / 60_000);
    }

    /** The alarm: the time is up. Returns what was done, for the log and for dock.sh. */
    static String fire(Context c) {
        prefs(c).edit().remove("end").remove("since").apply();
        String done = turnOff(c);
        prefs(c).edit().putString("last", done).apply();
        c.sendBroadcast(new Intent(CHANGED).setPackage(c.getPackageName()));
        return done;
    }

    /**
     * The TV goes to sleep now, the way the timer's end does it (and «أطفئ الآن» in «مركز التحكم»): the sound stops, then the screen is
     * turned off by the sleep service, or the screensaver comes up without the service. Returns «screen off», «screensaver» or «sound stopped only».
     */
    static String turnOff(Context c) {
        String done;
        try { MusicService.stopIfPlaying(c); } catch (RuntimeException ignored) { }
        if (Extras.sleepNow()) {
            done = "screen off";
        } else {
            try {
                c.startActivity(new Intent(Intent.ACTION_MAIN).setClassName("sa.urtv.screensaver", "sa.urtv.screensaver.MainActivity")
                    .putExtra("sleeping", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                done = "screensaver";
            } catch (RuntimeException e) {
                done = "sound stopped only";
            }
        }
        return done;
    }

    /** One line for `optimize/dock.sh sleep`: the time left, and what the end will do. */
    static String status(Context c) {
        int left = minutesLeft(c);
        String last = prefs(c).getString("last", "");
        return "sleep: " + (left == 0 ? "off" : left + " minutes left")
            + " | at the end: " + (Extras.enabled() ? "the screen turns off" : "the sound stops and the screensaver comes up (UrTV Extras is not installed or its service is off: optimize/dock.sh extras-setup)")
            + (last.isEmpty() ? "" : " | last time: " + last);
    }
}
