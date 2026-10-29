package sa.urtv.home;

import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Which time zone the home screen's clock, date and greeting go by: the city's, picked in the screensaver (#tz= in its
 * ConfigProvider), like the prayer times beside them. No Android in here, so tests/clock_zone_test.py runs it.
 */
final class ClockZone {
    static final String DEFAULT = "Asia/Riyadh";

    private ClockZone() { }

    /** A zone id the platform knows; anything else (missing, misspelt) is Riyadh, as the page does. */
    static String valid(String id) {
        if (id == null) return DEFAULT;
        id = id.trim();
        return ids().contains(id) ? id : DEFAULT;
    }

    private static java.util.Set<String> known;   // the platform's ids, listed once: valid() runs every minute

    private static synchronized java.util.Set<String> ids() {
        if (known == null) known = new java.util.HashSet<>(java.util.Arrays.asList(TimeZone.getAvailableIDs()));
        return known;
    }

    /** {city, tz, pm} from the screensaver's "city=..&tz=..&pm=.." fragment; the city is "" when it names none, pm "" for «تلقائي». */
    static String[] parse(String fragment) {
        String city = "", tz = null, pm = "";
        if (fragment != null) {
            for (String part : fragment.split("&")) {
                int eq = part.indexOf('=');
                if (eq < 0) continue;
                try {
                    String key = part.substring(0, eq), value = URLDecoder.decode(part.substring(eq + 1), "UTF-8");
                    if ("city".equals(key)) city = value.trim();
                    else if ("tz".equals(key)) tz = value;
                    else if ("pm".equals(key)) pm = value.trim();
                } catch (Exception ignored) { }
            }
        }
        return new String[]{city, valid(tz), pm};
    }

    /** The calendar of the moment in the zone: its hour, minute and day are the city's. */
    static Calendar at(long millis, String tz) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone(tz), Locale.US);
        c.setTimeInMillis(millis);
        return c;
    }

    /** The city's clock shows a different time than the TV's at that moment (same offset: the same time, nothing to say). */
    static boolean differs(String tvTz, String cityTz, long millis) {
        return TimeZone.getTimeZone(tvTz).getOffset(millis) != TimeZone.getTimeZone(cityTz).getOffset(millis);
    }

    /** The small line under the clock, as the screensaver's: «بتوقيت دبي». */
    static String label(String city) {
        return "بتوقيت " + (city.isEmpty() ? "المدينة" : city);
    }

    /**
     * The prayer calculation's name when it is not Umm al-Qura (the Saudi default says nothing): the country's by the city's zone, or the
     * Ja'fari one (pm=jafari) or the Gulf one (pm=gulf). The same words as the screensaver's line (CAL_LABEL in template.html); PrayerMethod.authority in the app and
     * PrayerMath.methodFor here use the same zones (tests/prayer_method_test.py).
     */
    static String calendar(String tz, String pm) {
        if ("jafari".equals(pm)) return "الحساب الجعفري";
        if ("gulf".equals(pm)) return "حساب الخليج";
        if (pm != null) switch (pm) {
            case "1": return "جامعة العلوم الإسلامية بكراتشي";
            case "2": return "الجمعية الإسلامية لأمريكا الشمالية";
            case "3": return "رابطة العالم الإسلامي";
            case "4": return "";
            case "5": return "حساب الهيئة المصرية";
            case "8": return "حساب الخليج";
            case "9": return "حساب الكويت";
            case "10": return "حساب قطر";
            case "11": return "المجلس الإسلامي السنغافوري";
            case "12": return "اتحاد المنظمات الإسلامية في فرنسا";
            case "13": return "رئاسة الشؤون الدينية التركية";
            case "14": return "الإدارة الدينية لمسلمي روسيا";
            case "15": return "لجنة رصد الهلال العالمية";
            case "16": return "حساب الإمارات";
            case "17": return "إدارة التنمية الإسلامية الماليزية";
            case "18": return "حساب تونس";
            case "19": return "حساب الجزائر";
            case "20": return "وزارة الشؤون الدينية الإندونيسية";
            case "21": return "حساب المغرب";
            case "23": return "حساب الأردن";
            case "100": return "حساب عُمان";
            default: break;
        }
        if (tz == null) return "";
        switch (tz) {
            case "Asia/Kuwait": return "حساب الكويت";
            case "Asia/Qatar": return "حساب قطر";
            case "Asia/Dubai": return "حساب الإمارات";
            case "Asia/Bahrain": return "حساب الخليج";
            case "Asia/Muscat": return "حساب عُمان";
            case "Africa/Cairo": case "Asia/Damascus": case "Asia/Beirut": case "Asia/Gaza": case "Asia/Hebron": case "Asia/Jerusalem": return "حساب الهيئة المصرية";
            case "Asia/Amman": return "حساب الأردن";
            case "Africa/Casablanca": case "Africa/El_Aaiun": return "حساب المغرب";
            case "Africa/Algiers": return "حساب الجزائر";
            case "Africa/Tunis": return "حساب تونس";
            default: return "";
        }
    }

    /** «بتوقيت المنامة · حساب الخليج»; with Umm al-Qura «بتوقيت الرياض» as it always was. */
    static String label(String city, String calendar) {
        return label(city) + (calendar.isEmpty() ? "" : " · " + calendar);
    }

    /** The line shows when the city's clock is not the TV's, or when the calculation is not Umm al-Qura (its name is then worth saying). */
    static boolean showLine(boolean differs, String calendar) {
        return differs || !calendar.isEmpty();
    }
}
