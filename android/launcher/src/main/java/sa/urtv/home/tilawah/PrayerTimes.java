package sa.urtv.home.tilawah;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Prayer times for the TV: the calculation of the city's country (Umm al-Qura for Saudi Arabia, and for anywhere outside the Gulf;
 * see PrayerMath.Method) or the Ja'fari one when the settings ask, the same numbers the screensaver's page computes and the ones
 * Aladhan gives for that method. Everything is computed on the TV with no network; once a day the times are quietly replaced by
 * Aladhan's. Where a method fixes Isha as a delay after Maghrib (Umm al-Qura: 90 minutes, 120 in Ramadan) Isha stays ours.
 */
final class PrayerTimes {
    static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;
    /** the five the TV shows and plays the adhan for — sunrise is not a prayer */
    static final int[] PRAYERS = {FAJR, DHUHR, ASR, MAGHRIB, ISHA};
    static final String[] NAMES = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
    private static final String[] KEYS = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};

    final long[] at = new long[6];
    final String[] clock = new String[6];
    /** the city's zone these times belong to, local midnight of their day, and whether it is Ramadan */
    private String tz = "Asia/Riyadh";
    private int year, month, day, method = PrayerMath.UMM_AL_QURA;
    private boolean ramadan;
    /** «تعديل وقت الصلاة»: minutes added to PRAYERS[k] (PrayerAdjust); the sunrise is never moved */
    private int[] adj = new int[PrayerAdjust.COUNT];

    static final class Next {
        final int index;
        final long at;
        final String clock;
        Next(int index, long at, String clock) { this.index = index; this.at = at; this.clock = clock; }
        String name() { return NAMES[index]; }
    }

    // ---------------------------------------------------------------- the times

    static PrayerTimes of(Calendar when, Place place) {
        TimeZone zone = TimeZone.getTimeZone(place.tz);
        Calendar local = Calendar.getInstance(zone, Locale.US);
        local.setTimeInMillis(when.getTimeInMillis());
        long day0 = PrayerMath.midnight(local.get(Calendar.YEAR), local.get(Calendar.MONTH) + 1,
                local.get(Calendar.DAY_OF_MONTH), place.tz);
        return of(local, place, hijriMonth(day0, place.tz) == 9);
    }

    private static PrayerTimes of(Calendar local, Place place, boolean ramadan) {
        long[] times = PrayerMath.compute(local.get(Calendar.YEAR), local.get(Calendar.MONTH) + 1,
                local.get(Calendar.DAY_OF_MONTH), place.lat, place.lng, place.tz, ramadan, place.method);
        PrayerTimes t = new PrayerTimes();
        t.tz = place.tz;
        t.ramadan = ramadan;
        t.method = place.method;
        t.year = local.get(Calendar.YEAR);
        t.month = local.get(Calendar.MONTH) + 1;
        t.day = local.get(Calendar.DAY_OF_MONTH);
        for (int i = 0; i < 6; i++) t.set(i, times[i]);
        t.adj = place.adj;
        t.shift();
        return t;
    }

    /**
     * «تعديل وقت الصلاة»: each of the five prayers moved by its own minutes, once, on the times as computed or as Aladhan gave them
     * (Isha is its own raw moment plus its own offset, not the moved Maghrib plus a delay). An Isha moved past midnight is still the
     * moment it is: the clock text wraps, the instant does not.
     */
    private void shift() {
        if (!PrayerAdjust.any(adj)) return;
        long[] moved = at.clone();
        PrayerAdjust.shift(moved, adj);
        for (int i = 0; i < 6; i++) if (moved[i] != at[i]) set(i, moved[i]);
    }

    /** Today's times: the ones fetched from Aladhan when they are in, the computed ones otherwise. */
    static PrayerTimes today(Context c) { return today(c, Place.get(c)); }

    /**
     * The same for a place the caller already has. Kept in memory for the day (and the city): the home screen asks every minute,
     * and the sun arithmetic and the preferences need not be gone through each time. New times from Aladhan drop the copy.
     */
    static synchronized PrayerTimes today(Context c, Place place) {
        Calendar day = Calendar.getInstance(TimeZone.getTimeZone(place.tz), Locale.US);
        String key = key(day, place);
        String copyKey = key + "|" + PrayerAdjust.text(place.adj);   // a change of the adjustments is a different day's copy
        if (todayCopy != null && copyKey.equals(todayKey)) return todayCopy;
        PrayerTimes t = of(day, place);
        String saved = prefs(c).getString(key, null);
        if (saved != null) t.apply(saved);   // Aladhan's, when they came; a bad copy is ignored
        todayCopy = t;
        todayKey = copyKey;
        return t;
    }

    private static PrayerTimes todayCopy;
    private static String todayKey;

    private static synchronized void forgetToday() { todayCopy = null; todayKey = null; }

    /** The next of the five prayers to show, looking at tomorrow when today's Isha has passed. One 20 seconds away is already «الآن». */
    static Next next(Context c) { return next(c, 30_000); }

    /** The next prayer the adhan alarm is for: one still due even a second from now, so a refresh just before it keeps it. */
    static Next nextAlarm(Context c) { return next(c, 0, Place.get(c)); }

    /** The same for a place the caller already has (the alarm is set from the very place it was judged by). */
    static Next nextAlarm(Context c, Place place) { return next(c, 0, place); }

    private static Next next(Context c, long aheadMs) { return next(c, aheadMs, Place.get(c)); }

    private static Next next(Context c, long aheadMs, Place place) {
        long now = System.currentTimeMillis();
        Calendar day = Calendar.getInstance(TimeZone.getTimeZone(place.tz), Locale.US);
        for (int d = 0; d < 3; d++) {
            PrayerTimes t = d == 0 ? today(c, place) : of(day, place);
            int i = PrayerMath.firstAfter(t.at, PRAYERS, now + aheadMs);
            if (i >= 0) return new Next(i, t.at[i], t.clock[i]);
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        return null;
    }

    /** "بعد ساعتين و١٥ دقيقة" — the time left, in the project's Arabic. */
    static String remaining(long millis) {
        long minutes = Math.max(0, (millis + 30_000) / 60_000);
        long h = minutes / 60, m = minutes % 60;
        String out = null;
        if (h > 0) out = hours((int) h);
        if (m > 0) out = out == null ? minutes((int) m) : out + " و" + minutes((int) m);
        return out == null ? "الآن" : "بعد " + out;
    }

    private static String hours(int n) {
        if (n == 1) return "ساعة";
        if (n == 2) return "ساعتين";
        return Ui.arabic(n) + (n >= 3 && n <= 10 ? " ساعات" : " ساعة");
    }

    private static String minutes(int n) {
        if (n == 1) return "دقيقة";
        if (n == 2) return "دقيقتين";
        return Ui.arabic(n) + (n >= 3 && n <= 10 ? " دقائق" : " دقيقة");
    }

    /** "05:12" → "٥:١٢ ص", "18:40" → "٦:٤٠ م"; with the screensaver's «ساعة ٢٤» on: "٠٥:١٢", "١٨:٤٠" (ClockText) */
    static String clockText(String hhmm) {
        return sa.urtv.home.ClockText.time(hhmm, sa.urtv.home.Digits.clock24(), sa.urtv.home.Digits.latin());
    }

    // ---------------------------------------------------------------- Aladhan

    /**
     * Replaces today's computed times with Aladhan's (the place's method) once they arrive, and puts the alarm back for the
     * new times. Runs off the main thread; `done` runs on the main thread when something changed.
     */
    static void refresh(final Context c, final Runnable done) {
        // standby: not asked now (the adhan itself rings on the computed times, and the home's minute tick asks when it is back in front)
        if (!sa.urtv.home.NetGate.backgroundOk(sa.urtv.home.NetGateAndroid.displayOn(c))) return;
        new Thread(() -> {
            if (refreshNow(c) && done != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(done);
        }, "adhan-times").start();
    }

    /**
     * The home screen's minute tick asks for today's times from Aladhan like the page does, so the two agree to the minute: at most one
     * try in 30 minutes while it has not worked, none once today's are kept (the answer is on disk, no thread, no network).
     */
    static void refreshIfDue(Context c, Place place) {
        if (!PrayerMath.aladhanKnows(place.method)) return;   // Oman's ministry: Aladhan has no such method, the computed times are the times
        if (!sa.urtv.home.NetGate.backgroundOk(sa.urtv.home.NetGateAndroid.displayOn(c))) return;   // a try is not counted while nothing could be asked
        Calendar day = Calendar.getInstance(TimeZone.getTimeZone(place.tz), Locale.US);
        String key = key(day, place);
        SharedPreferences sp = prefs(c);
        long now = System.currentTimeMillis();
        if (sp.getBoolean("fetched_" + key, false) || now - sp.getLong("refresh_try", 0) < 30 * 60_000L) return;
        sp.edit().putLong("refresh_try", now).apply();
        refresh(c, null);
    }

    /** The same, on the caller's thread (which must not be the main one). True when new times were kept. */
    static boolean refreshNow(Context c) {
        final Place place = Place.get(c);
        final Calendar day = Calendar.getInstance(TimeZone.getTimeZone(place.tz), Locale.US);
        final String key = key(day, place);
        if (!PrayerMath.aladhanKnows(place.method)) return false;   // Aladhan has no id for it: nothing to ask
        if (prefs(c).getBoolean("fetched_" + key, false)) return false;
        try {
            String url = "https://api.aladhan.com/v1/timings/" + PrayerMath.aladhanDate(day.getTimeInMillis(), place.tz)
                    + "?latitude=" + place.lat + "&longitude=" + place.lng
                    + "&method=" + place.method + "&timezonestring=" + URLEncoder.encode(place.tz, "UTF-8");
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(6000);
            con.setReadTimeout(8000);
            con.setRequestProperty("User-Agent", "urtv-home");
            StringBuilder body = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) body.append(line);
            } finally {
                con.disconnect();
            }
            JSONObject timings = new JSONObject(body.toString()).getJSONObject("data").getJSONObject("timings");
            StringBuilder out = new StringBuilder();
            // Isha stays ours where the method fixes it as a delay after Maghrib (90/120 minutes); an angle's Isha is Aladhan's, the sixth
            for (int i = 0; i <= (PrayerMath.method(place.method).ishaIsDelay() ? MAGHRIB : ISHA); i++) {
                int[] hm = PrayerMath.parseClock(timings.optString(KEYS[i]));   // «5:12 (+03)» is 5:12
                if (hm == null) throw new IllegalStateException("no " + KEYS[i]);
                if (out.length() > 0) out.append(',');
                out.append(String.format(Locale.US, "%02d:%02d", hm[0], hm[1]));
            }
            SharedPreferences.Editor edit = prefs(c).edit();
            for (String k : prefs(c).getAll().keySet()) {   // yesterday's and another city's copies are not kept
                if ((k.startsWith("times_") || k.startsWith("fetched_")) && !k.endsWith(key.substring("times_".length()))) edit.remove(k);
            }
            edit.putString(key, out.toString()).putBoolean("fetched_" + key, true).apply();
            forgetToday();
            AdhanScheduler.schedule(c);   // the alarm was set for the computed times: the new ones may differ by minutes
            return true;
        } catch (Exception ignored) {
            // offline or Aladhan is down: the computed times are already right
            return false;
        }
    }

    /**
     * Aladhan's times, "HH:mm,..." as saved: Fajr to Maghrib, and for a method whose Isha is an angle the Isha too. Otherwise Isha is
     * Maghrib's moment plus its delay, never wrapped into the day. Each is the city's clock reading of the day, taken as the moment it
     * is in the city's zone (also on a day the clocks change).
     */
    private void apply(String saved) {
        String[] values = saved.split(",");
        boolean delay = PrayerMath.method(method).ishaIsDelay();
        int count = delay ? MAGHRIB + 1 : ISHA + 1;
        if (values.length < count) return;
        long[] parsed = new long[6];
        for (int i = 0; i < count; i++) {
            int[] hm = PrayerMath.parseClock(values[i]);
            if (hm == null) return;   // not a time: keep the computed ones
            parsed[i] = PrayerMath.wall(year, month, day, hm[0] * 60 + hm[1], tz);
        }
        if (delay) parsed[ISHA] = parsed[MAGHRIB] + PrayerMath.ishaDelayMillis(ramadan, method);
        else if (parsed[ISHA] < parsed[MAGHRIB]) parsed[ISHA] += 24 * 3600_000L;   // «00:10»: after midnight, the next day's
        for (int i = 0; i <= ISHA; i++) set(i, parsed[i]);
        shift();   // Aladhan's times are the raw ones: the person's minutes go on top of them
    }

    private void set(int i, long moment) {
        at[i] = moment;
        clock[i] = PrayerMath.clockOf(moment, tz);
    }

    // ---------------------------------------------------------------- the Islamic month

    /** 9 in Ramadan, from the Umm al-Qura calendar the whole project uses, never shifted by «فرق التاريخ الهجري»: prayer times do not follow it. */
    private static int hijriMonth(long millis, String tz) {
        try {
            android.icu.util.Calendar cal = android.icu.util.Calendar.getInstance(
                    android.icu.util.TimeZone.getTimeZone(tz), new android.icu.util.ULocale("en-u-ca-islamic-umalqura"));   // the city's day, not the device's
            cal.setTimeInMillis(millis);
            return cal.get(android.icu.util.Calendar.MONTH) + 1;
        } catch (Throwable t) {
            return 0;
        }
    }

    // ---------------------------------------------------------------- prefs

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("tilawah", Context.MODE_PRIVATE);
    }

    static String dayKey(Calendar day, Place place) { return key(day, place); }

    /** The day's key in the city's zone and for that city: a new city the same day asks Aladhan again. */
    private static String key(Calendar day, Place place) {
        return PrayerMath.key(day.getTimeInMillis(), place.lat, place.lng, place.tz, place.method);
    }
}
