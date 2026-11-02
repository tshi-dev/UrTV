package sa.urtv.home.tilawah;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The arithmetic of the prayer times and of their cache, with nothing of Android in it, so a plain JVM can run it
 * (tests/PrayerMathRun.java). PrayerTimes holds the rest: the place, the preferences, Aladhan.
 */
final class PrayerMath {
    static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;

    private static final double RAD = Math.PI / 180;
    private static final Pattern CLOCK = Pattern.compile("^\\s*(\\d{1,2}):(\\d{2})");

    private PrayerMath() {}

    /**
     * A calculation, named by the id Aladhan gives it (the page and the online times ask Aladhan for the same one). Fajr and Isha are
     * angles below the horizon, or Isha is a fixed delay after Maghrib; the tune moves sunrise, Dhuhr, Asr and Maghrib by whole minutes
     * (the safety margins some authorities publish). Sources: Aladhan's list (aladhan.com/calculation-methods, api.aladhan.com/v1/methods)
     * and, for the Dubai margins, the Batoul Apps adhan library's METHODS.md:
     *   0  Shia Ithna-Ashari, Leva Institute, Qum: Fajr 16°, Isha 14°, Maghrib when the sun is 4° below the horizon
     *   4  Umm al-Qura (Saudi Arabia; also the fallback for old fragments without a resolved country): Fajr 18.5°, Isha 90 minutes after Maghrib, 120 in Ramadan
     *   8  Gulf Region (Bahrain, Oman): Fajr 19.5°, Isha 90 minutes after Maghrib
     *   9  Kuwait: Fajr 18°, Isha 17.5°
     *  10  Qatar: Fajr 18°, Isha 90 minutes after Maghrib
     *  16  Dubai (the UAE's General Authority of Islamic Affairs): Fajr 18.2°, Isha 18.2°, sunrise −3, Dhuhr, Asr and Maghrib +3 minutes
     *   5  Egypt, the Egyptian General Authority of Survey: Fajr 19.5°, Isha 17.5° (praytimes.org/docs/methods and the ITL library's
     *      method-info list the same angles; Aladhan's id 5 is this authority). Syria, Lebanon and Palestine (Asia/Damascus, Asia/Beirut,
     *      Asia/Gaza, Asia/Hebron) get this one too: Aladhan has no method of their own, none of their religious authorities publishes
     *      angles that Aladhan carries, and praytimes.org names the Egyptian method for Syria and Lebanon, so it is the closest honest choice
     *  23  Jordan, the Ministry of Awqaf, Islamic Affairs and Holy Places: Fajr 18°, Isha 18°, Maghrib 5 minutes after sunset (the idea
     *      and the figures of pull request 34; Aladhan's id 23)
     *  21  Morocco, the Ministry of Habous and Islamic Affairs: Fajr 19°, Isha 17°, Dhuhr and Maghrib +5 minutes (Aladhan's id 21; angles and margins as the ITL
     *      library's method-info lists the Moroccan Awqaf ministry)
     *  19  Algeria, the Ministry of Religious Affairs and Awqaf: Fajr 18°, Isha 17° (Aladhan's id 19; the angles salahtimes.com and Aladhan's list give)
     *  18  Tunisia, the Ministry of Religious Affairs: Fajr 18°, Isha 18° (Aladhan's id 18; salahtimes.com lists the ministry's method with these angles)
     * 100  Oman, the Ministry of Awqaf and Religious Affairs (Muscat's default): Fajr 18°, Isha 18°, Dhuhr, Asr and Maghrib +5 minutes, sunrise, Fajr
     *      and Isha untouched. **Not an Aladhan id** (Aladhan has no such method; 100 is ours), so it is always computed here and never asked of
     *      Aladhan. The ministry publishes no parameters: these are the ones egyjs/afnon-android-tv-launcher (MIT, Method.OMAN_AWQAF) fitted to the
     *      ministry's own 2026 timetable (Muscat, Sohar, Nizwa, Salalah) to within about two minutes. The older Gulf method (8) stays available
     *      for Oman as the settings' {@code pm=gulf}.
     * Asr is the shadow-length-one (Shafi'i) Asr for all of them; Maghrib is sunset (0.833°) except for the Ja'fari.
     */
    static final class Method {
        final int id;
        final double fajr, isha, maghrib;
        final int ishaMinutes;
        final int[] tune;   // sunrise, Dhuhr, Asr, Maghrib
        Method(int id, double fajr, double isha, int ishaMinutes, double maghrib, int... tune) {
            this.id = id; this.fajr = fajr; this.isha = isha; this.ishaMinutes = ishaMinutes; this.maghrib = maghrib;
            this.tune = tune.length == 4 ? tune : new int[4];
        }
        /** Isha is the delay after Maghrib (so Aladhan's Isha need not be kept); false: an angle, Aladhan's Isha is kept too */
        boolean ishaIsDelay() { return ishaMinutes > 0; }
    }

    static final int JAFARI = 0, UMM_AL_QURA = 4, GULF = 8, KUWAIT = 9, QATAR = 10, DUBAI = 16, EGYPT = 5, JORDAN = 23, MOROCCO = 21, ALGERIA = 19, TUNISIA = 18;
    /** our own id for Oman's ministry: Aladhan has none, so ids from here up are never sent to it (see {@link #aladhanKnows}) */
    static final int OMAN = 100;
    static final int KARACHI = 1, ISNA = 2, MWL = 3, SINGAPORE = 11, FRANCE_UOIF = 12,
            DIYANET = 13, RUSSIA = 14, MOONSIGHTING = 15, JAKIM = 17, KEMENAG = 20;
    private static final Method[] METHODS = {
        new Method(JAFARI, 16, 14, 0, 4),
        new Method(UMM_AL_QURA, 18.5, 0, 90, .833),
        new Method(GULF, 19.5, 0, 90, .833),
        new Method(KUWAIT, 18, 17.5, 0, .833),
        new Method(QATAR, 18, 0, 90, .833),
        new Method(DUBAI, 18.2, 18.2, 0, .833, -3, 3, 3, 3),
        new Method(EGYPT, 19.5, 17.5, 0, .833),
        new Method(JORDAN, 18, 18, 0, .833, 0, 0, 0, 5),
        new Method(MOROCCO, 19, 17, 0, .833, 0, 5, 0, 5),
        new Method(ALGERIA, 18, 17, 0, .833),
        new Method(TUNISIA, 18, 18, 0, .833),
        new Method(OMAN, 18, 18, 0, .833, 0, 5, 5, 5),
        new Method(KARACHI, 18, 18, 0, .833),
        new Method(ISNA, 15, 15, 0, .833),
        new Method(MWL, 18, 17, 0, .833),
        new Method(SINGAPORE, 20, 18, 0, .833),
        new Method(FRANCE_UOIF, 12, 12, 0, .833),
        new Method(DIYANET, 18, 17, 0, .833, -7, 5, 4, 7),
        new Method(RUSSIA, 16, 15, 0, .833),
        new Method(MOONSIGHTING, 0, 0, 0, .833), // seasonal twilight below, not a fixed-angle method
        new Method(JAKIM, 20, 18, 0, .833),
        new Method(KEMENAG, 20, 18, 0, .833),
    };

    /** Does Aladhan have this method under this id? Ours (OMAN) it has not: the times are computed here only. */
    static boolean aladhanKnows(int id) { return id < OMAN; }

    static Method method(int id) {
        for (Method m : METHODS) if (m.id == id) return m;
        return METHODS[1];
    }

    /**
     * The method of a place: an explicit legacy preference or the validated numeric country method sent by the screensaver.
     * Old fragments without that id retain their zone-based behavior. The page (CONFIG.prayerMethod) and the app (PrayerMethod)
     * say the same; tests/prayer_method_test.py compares the three.
     */
    static int methodFor(String tz, String pm) {
        if ("jafari".equals(pm)) return JAFARI;
        if ("gulf".equals(pm)) return GULF;   // «الخليج» chosen by hand, for Oman or anywhere
        // The screensaver resolves the city's country and sends its numeric id.
        // Validate against METHODS, including id 0 only as the existing explicit choice.
        if (pm != null && pm.matches("[0-9]{1,3}")) {
            int id = Integer.parseInt(pm);
            for (Method m : METHODS) if (m.id == id && id != JAFARI) return id;
        }
        if (tz == null) return UMM_AL_QURA;
        switch (tz) {
            case "Asia/Kuwait": return KUWAIT;
            case "Asia/Qatar": return QATAR;
            case "Asia/Dubai": return DUBAI;
            case "Asia/Bahrain": return GULF;
            case "Asia/Muscat": return OMAN;
            case "Africa/Cairo": case "Asia/Damascus": case "Asia/Beirut": case "Asia/Gaza": case "Asia/Hebron": case "Asia/Jerusalem": return EGYPT;
            case "Asia/Amman": return JORDAN;
            case "Africa/Casablanca": case "Africa/El_Aaiun": return MOROCCO;
            case "Africa/Algiers": return ALGERIA;
            case "Africa/Tunis": return TUNISIA;
            default: return UMM_AL_QURA;
        }
    }

    /** Isha comes this long after Maghrib (Umm al-Qura): 90 minutes, 120 in Ramadan. */
    static long ishaDelayMillis(boolean ramadan) { return ishaDelayMillis(ramadan, UMM_AL_QURA); }

    /** The same for a method with a delay: the Ramadan half hour is Umm al-Qura's alone. */
    static long ishaDelayMillis(boolean ramadan, int methodId) {
        Method m = method(methodId);
        return (m.ishaMinutes + (ramadan && m.id == UMM_AL_QURA ? 30 : 0)) * 60_000L;
    }

    /** Local midnight of that day in that zone. */
    static long midnight(int y, int mo, int d, String tz) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone(tz), Locale.US);
        c.clear();
        c.set(y, mo - 1, d, 0, 0, 0);
        return c.getTimeInMillis();
    }

    /** The moment a wall clock of that day reads {@code minutes} after midnight in that zone: 24*60+10 is 00:10 of the next day. */
    static long wall(int y, int mo, int d, int minutes, String tz) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone(tz), Locale.US);
        c.clear();
        c.set(y, mo - 1, d, minutes / 60, minutes % 60, 0);   // lenient: past 24:00 rolls into the next day; a clock that does not exist (the hour skipped) reads an hour later
        return c.getTimeInMillis();
    }

    static long[] compute(int y, int mo, int d, double lat, double lng, String tz, boolean ramadan) {
        return compute(y, mo, d, lat, lng, tz, ramadan, UMM_AL_QURA);
    }

    /**
     * The six times of that local day (Fajr, sunrise, Dhuhr, Asr, Maghrib, Isha) as moments. The sun gives each as a clock reading of the
     * city's day, and the moment is that clock reading in the city's zone (wall()), so a day when the clocks change (8 March, 1 November
     * in New York) puts every time where the clock says, not an hour off. Isha is an absolute time: at a high latitude, where Maghrib is
     * late, it falls after midnight, on the next day, instead of wrapping back to an hour of this day that has already gone.
     */
    static long[] compute(int y, int mo, int d, double lat, double lng, String tz, boolean ramadan, int methodId) {
        Method m = method(methodId);
        TimeZone zone = TimeZone.getTimeZone(tz);
        long day0 = midnight(y, mo, d, tz);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        utc.clear();
        utc.set(y, mo - 1, d, 0, 0, 0);
        double jd0 = utc.getTimeInMillis() / 86400000.0 + 2440587.5 - lng / 360.0;
        double tzHours = zone.getOffset(day0 + 12 * 3600_000L) / 3600000.0;
        // Across the date line (e.g. Samoa), solar noon still belongs to this
        // local day. Preserve later night rollover relative to that noon.
        tzHours -= 24 * Math.floor(adj(noon(jd0, 12), tzHours, lng) / 24);

        // Polar day/night has no sunrise/sunset to divide. Use 48 degrees on
        // that day at the same longitude/zone, then divide that surrogate night.
        double calcLat = lat;
        if (Double.isNaN(angleTime(jd0, lat, .833, 6, true)) || Double.isNaN(angleTime(jd0, lat, .833, 18, false)))
            calcLat = Math.copySign(48, lat);

        double fajr = adj(angleTime(jd0, calcLat, m.fajr, 5, true), tzHours, lng);
        double sunrise = adj(angleTime(jd0, calcLat, .833, 6, true), tzHours, lng);
        double dhuhr = adj(noon(jd0, 12), tzHours, lng);
        double asr = adj(asrTime(jd0, calcLat, 13), tzHours, lng);
        double sunset = adj(angleTime(jd0, calcLat, .833, 18, false), tzHours, lng);
        double maghrib = adj(angleTime(jd0, calcLat, m.maghrib, 18, false), tzHours, lng);
        double night = fix(sunrise - sunset, 24);
        double isha = adj(angleTime(jd0, calcLat, m.isha, 18, false), tzHours, lng);
        if (m.id == MOONSIGHTING) {
            double dawn = seasonal(calcLat, utc.get(Calendar.DAY_OF_YEAR), y, true) / 60;
            double dusk = seasonal(calcLat, utc.get(Calendar.DAY_OF_YEAR), y, false) / 60;
            // Aladhan method 15 reports latitudeAdjustmentMethod NONE:
            // use the seasonal equations directly, including above 55 degrees.
            fajr = sunrise - dawn;
            isha = sunset + dusk;
        } else {
            double dawn = night * m.fajr / 60, dusk = night * m.isha / 60;
            if (Double.isNaN(fajr) || sunrise - fajr > dawn) fajr = sunrise - dawn;
            if (Double.isNaN(isha) || isha - sunset > dusk) isha = sunset + dusk;
        }

        int[] minutes = new int[6];
        minutes[FAJR] = minutesOfDay(fajr);
        minutes[SUNRISE] = minutesOfDay(sunrise) + m.tune[0];
        minutes[DHUHR] = minutesOfDay(dhuhr) + m.tune[1];
        minutes[ASR] = minutesOfDay(asr) + m.tune[2];
        minutes[MAGHRIB] = (int) Math.round(maghrib * 60) + m.tune[3];
        if (m.ishaIsDelay()) {
            minutes[ISHA] = minutes[MAGHRIB] + (int) (ishaDelayMillis(ramadan, m.id) / 60_000L);
        } else {
            minutes[ISHA] = Math.max(minutes[MAGHRIB], (int) Math.round(isha * 60));   // absolute, like the delay: never wrapped to the morning
        }
        long[] at = new long[6];
        for (int i = 0; i < 6; i++) at[i] = wall(y, mo, d, minutes[i], tz);
        return at;
    }

    /**
     * The first of those prayers (indexes into {@code at}, in the order they come) that is later than {@code after}, or −1. The screen's
     * «next prayer» asks with now + 30 seconds (a prayer 20 seconds away is already «الآن» to look at); the adhan alarm asks with now alone, so
     * a refresh just before a prayer cannot move the alarm on to the one after it.
     */
    static int firstAfter(long[] at, int[] order, long after) {
        for (int i : order) if (at[i] > after) return i;
        return -1;
    }

    /** Aladhan clocks wrap at midnight. A summer sunset may itself be on day 1. */
    static int[] orderedClockMinutes(int[] clocks, boolean lateSunset) {
        int[] out = clocks.clone();
        if (lateSunset && out[MAGHRIB] < out[ASR]) out[MAGHRIB] += 1440;
        if (out[ISHA] < out[MAGHRIB]) out[ISHA] += 1440;
        return out;
    }

    /** What the adhan alarm was set for, to tell when it must be set again: the place, its zone, the minutes added and the method. */
    static String identity(double lat, double lng, String tz, String adjustments, int methodId) {
        return String.format(Locale.US, "%.4f,%.4f|%s|%s|%d", lat, lng, tz, adjustments, methodId);
    }

    /** The preference key of the day's times: the date in the city's zone and the city itself, so a new city refetches. */
    static String key(long millis, double lat, double lng, String tz) { return key(millis, lat, lng, tz, UMM_AL_QURA); }

    /** The same for a method: another method the same day is another copy (Umm al-Qura's key is as it always was). */
    static String key(long millis, double lat, double lng, String tz, int methodId) {
        return "times_" + format("yyyyMMdd", millis, tz) + "_" + String.format(Locale.US, "%.3f_%.3f", lat, lng) + "_" + tz
                + (methodId == UMM_AL_QURA ? "" : "_m" + methodId);
    }

    /** The date Aladhan is asked for: the city's own day, not the TV's. */
    static String aladhanDate(long millis, String tz) { return format("dd-MM-yyyy", millis, tz); }

    /** "5:12" or "05:12 (+03)" -> {5, 12}; null when it is not a time. Aladhan puts a zone after the time on some days. */
    static int[] parseClock(String v) {
        if (v == null) return null;
        Matcher m = CLOCK.matcher(v);
        if (!m.find()) return null;
        int h = Integer.parseInt(m.group(1)), min = Integer.parseInt(m.group(2));
        return h < 24 && min < 60 ? new int[]{h, min} : null;
    }

    /** "HH:mm" of that moment in that zone. */
    static String clockOf(long at, String tz) { return format("HH:mm", at, tz); }

    private static String format(String pattern, long millis, String tz) {
        SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.US);
        f.setTimeZone(TimeZone.getTimeZone(tz));
        return f.format(millis);
    }

    private static int minutesOfDay(double hours) { return (int) Math.round(fix(hours, 24) * 60) % 1440; }

    private static double adj(double h, double tzHours, double lng) { return h + tzHours - lng / 15.0; }

    private static double noon(double jd0, double t) { return fix(12 - sun(jd0, t / 24)[1], 24); }

    private static double angleTime(double jd0, double lat, double angle, double t, boolean before) {
        double decl = sun(jd0, t / 24)[0];
        double n = noon(jd0, t);
        double cosT = (-Math.sin(angle * RAD) - Math.sin(decl * RAD) * Math.sin(lat * RAD))
                / (Math.cos(decl * RAD) * Math.cos(lat * RAD));
        double T = Math.acos(cosT) / RAD / 15; // unreachable twilight stays NaN until the high-latitude rule
        return n + (before ? -T : T);
    }

    /** Moonsighting Committee's general shafaq, latitude/season interpolation.
     * Equations: moonsighting.com/how-we.html; Batoul Apps adhan-js Astronomical
     * (MIT). Aladhan method 15 uses these without adhan's extra safety margins. */
    private static double seasonal(double lat, int day, int year, boolean morning) {
        boolean leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
        int length = leap ? 366 : 365;
        double x = Math.abs(lat) / 55;
        double a = 75 + (morning ? 28.65 : 25.60) * x;
        double b = 75 + (morning ? 19.44 : 2.05) * x;
        double c = 75 + (morning ? 32.74 : -9.21) * x;
        double d = 75 + (morning ? 48.10 : 6.14) * x;
        double days = fix(day + (lat >= 0 ? 10 : -(leap ? 173 : 172)), length);
        double[] ends = {91, 137, 183, 229, 275, 366};
        double[] values = {a, b, c, d, c, b, a};
        double start = 0;
        for (int i = 0; i < ends.length; i++) {
            if (days < ends[i]) return values[i] + (values[i + 1] - values[i]) * (days - start) / (i == 5 ? 91 : ends[i] - start);
            start = ends[i];
        }
        return a;
    }

    private static double asrTime(double jd0, double lat, double t) {
        double decl = sun(jd0, t / 24)[0];
        double a = -Math.atan(1 / (1 + Math.tan(Math.abs(lat - decl) * RAD))) / RAD;
        return angleTime(jd0, lat, a, t, false);
    }

    /** declination and equation of time */
    private static double[] sun(double jd0, double t) {
        double D = jd0 + t - 2451545;
        double g = fix(357.529 + .98560028 * D, 360);
        double q = fix(280.459 + .98564736 * D, 360);
        double L = fix(q + 1.915 * Math.sin(g * RAD) + .020 * Math.sin(2 * g * RAD), 360);
        double e = 23.439 - .00000036 * D;
        double ra = fix(Math.atan2(Math.cos(e * RAD) * Math.sin(L * RAD), Math.cos(L * RAD)) / RAD / 15, 24);
        double decl = Math.asin(Math.sin(e * RAD) * Math.sin(L * RAD)) / RAD;
        return new double[]{decl, q / 15 - ra};
    }

    private static double fix(double a, double b) {
        a = a - b * Math.floor(a / b);
        return a < 0 ? a + b : a;
    }
}
