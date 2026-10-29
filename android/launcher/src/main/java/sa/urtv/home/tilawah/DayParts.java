package sa.urtv.home.tilawah;

import android.content.Context;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * What the home screen's greeting and its bar before the adhan need from the prayer times (PrayerTimes stays private to this
 * package): the moments of the five prayers today, and the next sunrise or sunset. The same numbers as the adhan.
 */
public final class DayParts {
    /** Fajr, Dhuhr, Asr, Maghrib and Isha today, in epoch millis */
    public final long fajr, dhuhr, asr, maghrib, isha;
    /** «الغروب» until Maghrib, then «الشروق» (the coming one); sunClock is "17:57" in the city's time */
    public final String sunLabel, sunClock;
    /** it is Friday in the city: its Dhuhr is the Jumu'ah, with no iqama countdown */
    public final boolean friday;

    private DayParts(long fajr, long dhuhr, long asr, long maghrib, long isha, String sunLabel, String sunClock, boolean friday) {
        this.fajr = fajr; this.dhuhr = dhuhr; this.asr = asr; this.maghrib = maghrib; this.isha = isha;
        this.sunLabel = sunLabel; this.sunClock = sunClock; this.friday = friday;
    }

    public static DayParts now(Context c) { return now(c, Place.get(c)); }

    /** From the screensaver's settings fragment the caller has read already (null: it is missing or old): one read per tick; `ticket` as taken before that read (PlaceTicket). */
    public static DayParts now(Context c, String fragment, long ticket) { return now(c, Place.get(c, fragment, ticket)); }

    private static String sunriseKey, sunriseClock;   // tomorrow's sunrise, worked out once a day

    private static DayParts now(Context c, Place place) {
        PrayerTimes t = PrayerTimes.today(c, place);
        long now = System.currentTimeMillis();
        String label, clock;
        if (now < t.at[PrayerTimes.SUNRISE]) {            // before dawn: today's sunrise is next
            label = "الشروق"; clock = t.clock[PrayerTimes.SUNRISE];
        } else if (now < t.at[PrayerTimes.MAGHRIB]) {
            label = "الغروب"; clock = t.clock[PrayerTimes.MAGHRIB];
        } else {                                          // after sunset: tomorrow's sunrise
            Calendar tomorrow = Calendar.getInstance(TimeZone.getTimeZone(place.tz), Locale.US);
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            String key = PrayerTimes.dayKey(tomorrow, place);
            synchronized (DayParts.class) {
                if (!key.equals(sunriseKey)) { sunriseClock = PrayerTimes.of(tomorrow, place).clock[PrayerTimes.SUNRISE]; sunriseKey = key; }
                label = "الشروق"; clock = sunriseClock;
            }
        }
        boolean friday = Calendar.getInstance(TimeZone.getTimeZone(place.tz), Locale.US).get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        return new DayParts(t.at[PrayerTimes.FAJR], t.at[PrayerTimes.DHUHR], t.at[PrayerTimes.ASR],
                t.at[PrayerTimes.MAGHRIB], t.at[PrayerTimes.ISHA], label, clock, friday);
    }
}
