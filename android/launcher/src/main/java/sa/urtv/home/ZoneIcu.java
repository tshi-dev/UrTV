package sa.urtv.home;

/**
 * 4.3 (issue #55): the ICU zone for the Hijri date (android.icu, Umm al-Qura), with the same offset as ZoneFix: the TV's own ICU zone when it
 * agrees with ZoneFix now, else a fixed zone at ZoneFix's offset (the date of today is all it is used for; asked again every minute).
 */
public final class ZoneIcu {
    private ZoneIcu() { }

    public static android.icu.util.TimeZone of(String id) {
        long now = System.currentTimeMillis();
        android.icu.util.TimeZone own = android.icu.util.TimeZone.getTimeZone(id);
        int want = (int) ZoneFix.offset(id, now);
        return own.getOffset(now) == want ? own : fixed(want);
    }

    /** a fixed zone at {@code ms} (android.icu has no SimpleTimeZone class): ICU reads the custom id «GMT+03:00» */
    private static android.icu.util.TimeZone fixed(int ms) {
        int m = Math.abs(ms) / 60_000;
        return android.icu.util.TimeZone.getTimeZone(String.format(java.util.Locale.US, "GMT%s%02d:%02d", ms < 0 ? "-" : "+", m / 60, m % 60));
    }
}
