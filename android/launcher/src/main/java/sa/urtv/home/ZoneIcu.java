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
        return own.getOffset(now) == want ? own : new android.icu.util.SimpleTimeZone(want, id);
    }
}
