package sa.urtv.home.tilawah;

import android.content.Context;

import java.util.Calendar;
import java.util.Locale;

/**
 * 4.4: the days DuaRule looks through, as the city sees them: the weekday, the Gregorian date, the Hijri one (Umm al-Qura, as the prayer card and
 * the alarm use) and that day's prayer times (today's as the home shows them, Aladhan's when they came; later days computed).
 */
final class DuaDays implements DuaRule.Days {
    private final Context c;
    private final Place place;
    private final DuaRule.Day[] cache = new DuaRule.Day[DuaRule.HORIZON + 2];   // -1 (yesterday) to HORIZON
    /** «فرق التاريخ الهجري» (4.4: the duas' Hijri date moves with the home's) */
    private final int hijriShift;
    private final Calendar start;

    DuaDays(Context c) {
        this.c = c.getApplicationContext();
        this.place = Place.get(c);
        start = Calendar.getInstance(sa.urtv.home.ZoneFix.zone(place.tz), Locale.US);
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
        hijriShift = sa.urtv.home.HijriShift.fromFragment(this.c.getSharedPreferences("place", Context.MODE_PRIVATE).getString("fragment", null));
    }

    @Override public DuaRule.Day day(int k) {
        if (k < -1 || k > DuaRule.HORIZON) return null;
        if (cache[k + 1] != null) return cache[k + 1];
        Calendar d = (Calendar) start.clone();
        d.add(Calendar.DAY_OF_MONTH, k);
        long midnight = d.getTimeInMillis();
        long[] prayers = null;
        try {
            Calendar noon = (Calendar) d.clone();
            noon.set(Calendar.HOUR_OF_DAY, 12);
            PrayerTimes t = k == 0 ? PrayerTimes.today(c, place) : PrayerTimes.of(noon, place);
            prayers = t.at.clone();
        } catch (RuntimeException ignored) { }
        int hy = 0, hm = 0, hd = 0;
        try {
            android.icu.util.Calendar h = android.icu.util.Calendar.getInstance(sa.urtv.home.ZoneIcu.of(place.tz), new android.icu.util.ULocale("en-u-ca-islamic-umalqura"));
            Calendar noonShifted = (Calendar) d.clone();
            noonShifted.set(Calendar.HOUR_OF_DAY, 12);
            h.setTime(sa.urtv.home.HijriShift.date(noonShifted, hijriShift));   // the day's noon (a date never slips at midnight), moved by «فرق التاريخ الهجري» as the home's date
            hy = h.get(android.icu.util.Calendar.EXTENDED_YEAR);
            hm = h.get(android.icu.util.Calendar.MONTH) + 1;
            hd = h.get(android.icu.util.Calendar.DAY_OF_MONTH);
        } catch (Throwable ignored) { }
        final Calendar day = d;
        cache[k + 1] = new DuaRule.Day(d.get(Calendar.DAY_OF_WEEK) - 1, d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1, d.get(Calendar.DAY_OF_MONTH),
                hy, hm, hd, midnight, prayers, minutes -> {   // 4.4: the clock as the city reads it that day (a DST day: 10:00 is 10:00, not midnight + 10 h)
                    Calendar w = (Calendar) day.clone();
                    w.set(Calendar.HOUR_OF_DAY, minutes / 60);
                    w.set(Calendar.MINUTE, minutes % 60);
                    return w.getTimeInMillis();
                });
        return cache[k + 1];
    }

    /** today's Hijri year, month, day (for a new reminder's date) */
    int[] hijriToday() { DuaRule.Day d = day(0); return new int[]{d.hy, d.hm, d.hd}; }
}
