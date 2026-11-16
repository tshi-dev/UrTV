package sa.urtv.screensaver;

import java.util.List;
import java.util.Map;

/**
 * 4.3.3 Published prayer time corrections (urtv-data prayer/adjustments.json, docs/SCHEMAS.md there): minutes added to a prayer in a country
 * or a city between two days, e.g. an Umm al-Qura notice. They go into the page and the home through the same {@code adj=} the person's own
 * «تعديل وقت الصلاة» uses (PrayerAdj), so the page, the home's times and the adhan alarms all move together, and:
 *  - a prayer the person moved themselves (the setting on and that prayer not 0) keeps the person's minutes; the correction never overrides it;
 *  - a city's entry wins over its country's; two entries for the same place and day add nothing more (the first counts);
 *  - each correction is held to −20..20, like the person's own, because that is what the page and the home accept.
 * The settings screen says when a correction is applied (SettingsActivity). Plain Java on a JVM (tests/PrayerFixRun.java).
 */
final class PrayerFix {
    private PrayerFix() { }

    static final String PATH = "prayer/adjustments.json", NAME = "prayer-adjustments.json";
    static final String[] KEYS = PrayerAdj.KEYS;   // fajr dhuhr asr maghrib isha; a "sunrise" entry is never applied (the sunrise is not moved)

    /** the corrections for {@code country} and {@code city} on {@code day} (YYYY-MM-DD in the city's zone): five minutes, zeros when none */
    static int[] forDay(Object doc, String country, String city, String day) {
        int[] byCountry = new int[PrayerAdj.COUNT], byCity = new int[PrayerAdj.COUNT];
        boolean[] setCountry = new boolean[PrayerAdj.COUNT], setCity = new boolean[PrayerAdj.COUNT];
        if (!(doc instanceof Map) || country == null || day == null) return new int[PrayerAdj.COUNT];
        Object list = ((Map<?, ?>) doc).get("adjustments");
        if (!(list instanceof List)) return new int[PrayerAdj.COUNT];
        for (Object o : (List<?>) list) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> a = (Map<?, ?>) o;
            Object w = a.get("where"), from = a.get("from"), to = a.get("to"), mins = a.get("minutes");
            if (!(w instanceof Map) || !(from instanceof String) || !(to instanceof String) || !(mins instanceof Map)) continue;
            if (!day.matches("\\d{4}-\\d{2}-\\d{2}") || day.compareTo((String) from) < 0 || day.compareTo((String) to) > 0) continue;
            if (!country.equalsIgnoreCase(String.valueOf(((Map<?, ?>) w).get("country")))) continue;
            Object wc = ((Map<?, ?>) w).get("city");
            boolean forCity = wc != null;
            if (forCity && !(wc instanceof String && ((String) wc).equals(city))) continue;
            for (int k = 0; k < PrayerAdj.COUNT; k++) {
                Object m = ((Map<?, ?>) mins).get(KEYS[k]);
                if (!(m instanceof Double) || (Double) m != Math.rint((Double) m)) continue;
                int v = Math.max(-PrayerAdj.MAX, Math.min(PrayerAdj.MAX, ((Double) m).intValue()));
                if (forCity && !setCity[k]) { byCity[k] = v; setCity[k] = true; }
                else if (!forCity && !setCountry[k]) { byCountry[k] = v; setCountry[k] = true; }
            }
        }
        int[] out = new int[PrayerAdj.COUNT];
        for (int k = 0; k < PrayerAdj.COUNT; k++) out[k] = setCity[k] ? byCity[k] : byCountry[k];
        return out;
    }

    /** what goes to the page and the home: the person's own minutes where they set one (the setting on, not 0), the correction elsewhere */
    static int[] combine(boolean ownOn, int[] own, int[] fix) {
        int[] out = new int[PrayerAdj.COUNT];
        for (int k = 0; k < PrayerAdj.COUNT; k++) out[k] = ownOn && own[k] != 0 ? own[k] : fix[k];
        return out;
    }

    /** the corrections that are really applied (not hidden by the person's own minutes): «العشاء +٥ د · المغرب −١ د», or "" when none */
    static String applied(boolean ownOn, int[] own, int[] fix) {
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < PrayerAdj.COUNT; k++) {
            if (fix[k] == 0 || (ownOn && own[k] != 0)) continue;
            b.append(b.length() == 0 ? "" : " · ").append(PrayerAdj.label(k, fix[k]));
        }
        return b.toString();
    }
}
