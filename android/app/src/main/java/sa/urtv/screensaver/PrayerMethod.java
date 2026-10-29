package sa.urtv.screensaver;

/**
 * Which calculation the prayer times use, for the settings row «طريقة الحساب» and `optimize/saver.sh prayermethod`.
 * «تلقائي» resolves the country retained with the city. Cities.Region carries each country's method; the resolved numeric
 * id goes to the page and launcher through the existing pm fragment. Old explicit choices are preserved. Iran uses MWL;
 * no world country introduces a Shia method. Legacy zone-only labels remain for old fragments and documentation.
 * tests/prayer_method_test.py keeps method parameters and labels equal across the three consumers.
 */
final class PrayerMethod {
    static final String AUTO = "auto", JAFARI = "jafari", GULF = "gulf";
    /**
     * The app's listed countries offered by the search. Search results from any valid country code are accepted.
     * Palestine ("PS") is left out on purpose: GeoNames may put a place of East Jerusalem or Bethlehem in Asia/Jerusalem, Israel's zone, which
     * would silently get Umm al-Qura; the cities of the list carry Asia/Hebron or Asia/Gaza and do get the right one.
     */
    static final String[] SEARCH_COUNTRIES = searchCountries();

    private static String[] searchCountries() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String[] c : Location.COUNTRIES) if (searchable(c[0])) out.add(c[0]);
        return out.toArray(new String[0]);
    }

    private PrayerMethod() { }

    /** "jafari" or "auto"; null when it is neither (a typo from the shell). */
    static String parse(String s) {
        if (s == null) return null;
        String v = s.trim().toLowerCase(java.util.Locale.ROOT);
        return AUTO.equals(v) || JAFARI.equals(v) || GULF.equals(v) ? v : null;
    }

    /** What goes in the settings fragment: nothing for «تلقائي» (the page is then as it was). */
    static String fragment(String saved) { return JAFARI.equals(saved) ? "&pm=jafari" : GULF.equals(saved) ? "&pm=gulf" : ""; }

    /** Automatic selection is resolved here, by country, and sent to both renderers.
     * The page needs no country table, and a shared zone cannot change the method. */
    static String fragment(String saved, String country, String tz) {
        return AUTO.equals(saved) || saved == null ? "&pm=" + methodId(country, tz) : fragment(saved);
    }

    static int methodId(String country, String tz) {
        String code = country == null || country.isEmpty() ? Location.codeOf(tz) : country;
        for (Cities.Region r : Cities.REGIONS) if (r.country.equalsIgnoreCase(code)) return r.method;
        return 3; // MWL for a country without a specific authority in the data
    }

    static boolean searchable(String countryCode) {
        // All Open-Meteo countries. PS retains its checked Hebron/Gaza list:
        // the service may misassign Palestinian places to Asia/Jerusalem.
        return countryCode != null && countryCode.matches("[A-Za-z]{2}") && !"PS".equalsIgnoreCase(countryCode);
    }

    /** The country's calculation as the settings row names it, by the city's zone. */
    static String authority(String tz) {
        if (tz == null) return "أم القرى";
        switch (tz) {
            case "Asia/Kuwait": return "الكويت";
            case "Asia/Qatar": return "قطر";
            case "Asia/Dubai": return "الإمارات";
            case "Asia/Bahrain": return "الخليج";
            case "Asia/Muscat": return "وزارة الأوقاف والشؤون الدينية العُمانية";
            case "Africa/Cairo": case "Asia/Damascus": case "Asia/Beirut": case "Asia/Gaza": case "Asia/Hebron": case "Asia/Jerusalem": return "الهيئة المصرية للمساحة";
            case "Asia/Amman": return "وزارة الأوقاف الأردنية";
            case "Africa/Casablanca": case "Africa/El_Aaiun": return "وزارة الأوقاف المغربية";
            case "Africa/Algiers": return "وزارة الشؤون الدينية الجزائرية";
            case "Africa/Tunis": return "وزارة الشؤون الدينية التونسية";
            default: return "أم القرى";
        }
    }

    static String label(String saved, String tz) {
        return JAFARI.equals(saved) ? "جعفري · معهد ليفا، قم" : GULF.equals(saved) ? "الخليج" : "تلقائي · " + authority(tz);
    }

    static String authority(String country, String tz) { return authority(methodId(country, tz)); }

    static String label(String saved, String country, String tz) {
        return JAFARI.equals(saved) || GULF.equals(saved) ? label(saved, tz) : "تلقائي · " + authority(country, tz);
    }

    static String authority(int method) {
        switch (method) {
            case 1: return "جامعة العلوم الإسلامية بكراتشي";
            case 2: return "الجمعية الإسلامية لأمريكا الشمالية";
            case 3: return "رابطة العالم الإسلامي";
            case 4: return "أم القرى";
            case 5: return "الهيئة المصرية للمساحة";
            case 8: return "الخليج";
            case 9: return "الكويت";
            case 10: return "قطر";
            case 11: return "المجلس الإسلامي السنغافوري";
            case 12: return "اتحاد المنظمات الإسلامية في فرنسا";
            case 13: return "رئاسة الشؤون الدينية التركية";
            case 14: return "الإدارة الدينية لمسلمي روسيا";
            case 15: return "لجنة رصد الهلال العالمية";
            case 16: return "الإمارات";
            case 17: return "إدارة التنمية الإسلامية الماليزية";
            case 18: return "وزارة الشؤون الدينية التونسية";
            case 19: return "وزارة الشؤون الدينية الجزائرية";
            case 20: return "وزارة الشؤون الدينية الإندونيسية";
            case 21: return "وزارة الأوقاف المغربية";
            case 23: return "وزارة الأوقاف الأردنية";
            case 100: return "وزارة الأوقاف والشؤون الدينية العُمانية";
            default: return "رابطة العالم الإسلامي";
        }
    }
}
