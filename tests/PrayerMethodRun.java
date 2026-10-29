package sa.urtv.screensaver;

/** For tests/prayer_method_test.py: PrayerMethod.java on a plain JVM. Prints PASS/FAIL lines. */
public final class PrayerMethodRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("parse: jafari and auto, in any case and with spaces", "jafari".equals(PrayerMethod.parse(" Jafari ")) && "auto".equals(PrayerMethod.parse("AUTO")), "");
        check("parse: anything else is null", PrayerMethod.parse("shia") == null && PrayerMethod.parse("") == null && PrayerMethod.parse(null) == null && PrayerMethod.parse("4") == null, "");
        check("fragment: nothing for auto (the page is then as it was), #pm=jafari for the Ja'fari",
                PrayerMethod.fragment("auto").isEmpty() && PrayerMethod.fragment(null).isEmpty() && "&pm=jafari".equals(PrayerMethod.fragment("jafari")) && "&pm=gulf".equals(PrayerMethod.fragment("gulf")), "");
        check("gulf: the Gulf method can be chosen by hand (parse, label), and Oman's own default is its ministry's",
                "gulf".equals(PrayerMethod.parse("gulf")) && "gulf".equals(PrayerMethod.parse(" GULF ")) && PrayerMethod.label("gulf", "Asia/Muscat").equals("الخليج")
                && PrayerMethod.label("auto", "Asia/Muscat").equals("تلقائي · وزارة الأوقاف والشؤون الدينية العُمانية") && PrayerMethod.label("auto", "Asia/Bahrain").equals("تلقائي · الخليج"), "");
        check("search: Saudi Arabia, the five other Gulf states, Egypt, Jordan, Syria, Lebanon, Morocco, Algeria, Tunisia; not Palestine (its zones are unreliable in the search) or anything else",
                PrayerMethod.searchable("SA") && PrayerMethod.searchable("bh") && PrayerMethod.searchable("KW") && PrayerMethod.searchable("QA")
                && PrayerMethod.searchable("AE") && PrayerMethod.searchable("OM") && PrayerMethod.searchable("EG") && PrayerMethod.searchable("jo") && PrayerMethod.searchable("SY")
                && PrayerMethod.searchable("LB") && PrayerMethod.searchable("MA") && PrayerMethod.searchable("DZ") && PrayerMethod.searchable("TN") && !PrayerMethod.searchable("PS")
                && PrayerMethod.searchable("IL") && PrayerMethod.searchable("LY") && PrayerMethod.searchable("US") && PrayerMethod.searchable("NO") && !PrayerMethod.searchable("") && !PrayerMethod.searchable(null), "");
        check("label: auto names the country's calculation by the zone; the Ja'fari names itself",
                PrayerMethod.label("auto", "Asia/Riyadh").equals("تلقائي · أم القرى") && PrayerMethod.label("auto", "Asia/Kuwait").equals("تلقائي · الكويت")
                && PrayerMethod.label("auto", "Asia/Qatar").equals("تلقائي · قطر") && PrayerMethod.label("auto", "Asia/Dubai").equals("تلقائي · الإمارات")
                && PrayerMethod.label("auto", "Asia/Bahrain").equals("تلقائي · الخليج") && PrayerMethod.label("auto", "Africa/Cairo").equals("تلقائي · الهيئة المصرية للمساحة") && PrayerMethod.label("auto", "Asia/Beirut").equals("تلقائي · الهيئة المصرية للمساحة") && PrayerMethod.label("auto", "Africa/Casablanca").equals("تلقائي · وزارة الأوقاف المغربية") && PrayerMethod.label("auto", "Africa/Algiers").equals("تلقائي · وزارة الشؤون الدينية الجزائرية") && PrayerMethod.label("auto", "Africa/Tunis").equals("تلقائي · وزارة الشؤون الدينية التونسية") && PrayerMethod.label("auto", "Asia/Amman").equals("تلقائي · وزارة الأوقاف الأردنية") && PrayerMethod.label("jafari", "Asia/Kuwait").startsWith("جعفري"), "");
        check("label: a hand-entered Asia/Jerusalem gets the Egyptian authority's, Africa/El_Aaiun Morocco's (not Umm al-Qura)",
                PrayerMethod.label("auto", "Asia/Jerusalem").equals("تلقائي · الهيئة المصرية للمساحة") && PrayerMethod.label("auto", "Africa/El_Aaiun").equals("تلقائي · وزارة الأوقاف المغربية"), "");
        // the cities of the list that are not Saudi: each has its country's zone, in its country's box, and that zone has a method of its own
        java.util.Map<String, double[]> box = new java.util.HashMap<>();   // zone -> lat min, lat max, lon min, lon max
        for (String z : new String[] {"Asia/Bahrain", "Asia/Kuwait", "Asia/Qatar", "Asia/Dubai", "Asia/Muscat"}) box.put(z, new double[] {16, 32, 34, 60});
        box.put("Africa/Cairo", new double[] {21.5, 32, 24.5, 37});
        box.put("Asia/Amman", new double[] {29, 33.5, 34.8, 39.5});
        box.put("Asia/Damascus", new double[] {32, 37.5, 35, 42.5});
        box.put("Asia/Beirut", new double[] {33, 34.7, 35, 36.7});
        box.put("Asia/Hebron", new double[] {31, 32.6, 34.8, 35.6});
        box.put("Asia/Gaza", new double[] {31, 31.7, 34, 34.7});
        box.put("Africa/Casablanca", new double[] {27, 36, -13, -1});
        box.put("Africa/Algiers", new double[] {19, 37.5, -9, 12});
        box.put("Africa/Tunis", new double[] {30, 37.5, 7, 12});
        int gulf = 0, others = 0;
        java.util.Set<String> names = new java.util.HashSet<>();
        java.util.Set<String> ids = new java.util.HashSet<>(java.util.Arrays.asList(java.util.TimeZone.getAvailableIDs()));
        for (Cities.Region r : Cities.REGIONS) {
            for (String[] c : r.cities) {
                if (c.length > 4) continue; // world rows are checked against the reviewed data by cities_world_test.py
                check("city " + c[0] + ": a name nobody else has", names.add(c[0]), c[0]);
                boolean saudi = c.length == 3;
                double lat = Double.parseDouble(c[1]), lon = Double.parseDouble(c[2]);
                if (saudi) {
                    check("city " + c[0] + ": coordinates in Saudi Arabia's neighbourhood (16-32 N, 34-60 E)", lat > 16 && lat < 32 && lon > 34 && lon < 60, c[1] + "," + c[2]);
                    continue;
                }
                double[] bx = box.get(c[3]);
                check("city " + c[0] + ": its zone is one a country of the list has (" + c[3] + ") and exists", bx != null && ids.contains(c[3]), c[3]);
                if (bx == null) continue;
                check("city " + c[0] + ": coordinates inside its country's box", lat > bx[0] && lat < bx[1] && lon > bx[2] && lon < bx[3], c[1] + "," + c[2] + " in " + c[3]);
                check("city " + c[0] + ": its zone has a calculation of its own, not the Umm al-Qura default", !"أم القرى".equals(PrayerMethod.authority(c[3])), c[3]);
                if (bx[2] == 34 && bx[3] == 60) gulf++; else others++;
            }
        }
        check("the five Gulf states are in the list, with their big cities (at least 40)", gulf >= 40, String.valueOf(gulf));
        check("Egypt, the Levant and North Africa are in the list with their big cities (at least 120)", others >= 120, String.valueOf(others));
        for (String z : box.keySet()) {
            int n = 0;
            for (Cities.Region r : Cities.REGIONS) for (String[] c : r.cities) if (c.length > 3 && c[3].equals(z)) n++;
            check("zone " + z + ": at least 4 cities", n >= 4, String.valueOf(n));
        }
        // the «الموقع» group: the country filters the city list, and is told by the city's zone
        check("country: Saudi Arabia's regions, or the Gulf state's one", Location.regions(0).length == 13 && Location.regions(1).length == 1
                && Location.regions(1)[0].name.equals("البحرين") && Location.regions(5)[0].name.equals("سلطنة عُمان"), String.valueOf(Location.regions(0).length));
        int listed = 0;
        for (int i = 0; i < Location.COUNTRIES.length; i++) listed += Location.regions(i).length;
        check("country: every region of the list is in one country", listed == Cities.REGIONS.length, listed + " of " + Cities.REGIONS.length);
        check("country: by the city's zone; a place of one's own elsewhere says so", Location.countryLabel("Asia/Riyadh").equals("السعودية")
                && Location.countryLabel("Asia/Bahrain").equals("البحرين") && Location.countryLabel("Africa/Cairo").equals("مصر")
                && Location.countryLabel("Asia/Gaza").equals("فلسطين") && Location.countryLabel("Asia/Hebron").equals("فلسطين")
                && Location.countryLabel("Africa/El_Aaiun").equals("المغرب") && Location.countryLabel("Europe/London").equals("المملكة المتحدة"), "");
        check("country: Egypt, Palestine and Tunisia are each one region of their own", Location.regions(6).length == 1 && Location.regions(6)[0].name.equals("مصر")
                && Location.regions(10).length == 1 && Location.regions(10)[0].name.equals("فلسطين") && Location.regions(13)[0].name.equals("تونس"), "");
        int searchable = 0;
        for (int i = 0; i < Location.COUNTRIES.length; i++) if (Location.searchable(i)) searchable++;
        check("country: every country the search takes is in the list, and every one of the list but Palestine is searched",
                java.util.Arrays.stream(PrayerMethod.SEARCH_COUNTRIES).allMatch(code -> java.util.Arrays.stream(Location.COUNTRIES).anyMatch(c -> c[0].equals(code)))
                && searchable == PrayerMethod.SEARCH_COUNTRIES.length && searchable == Location.COUNTRIES.length - 1 && !Location.searchable(10), "");
        for (int i = 0; i < Location.COUNTRIES.length; i++)
            for (int k = 2; k < Location.COUNTRIES[i].length; k++)
                check("country " + Location.COUNTRIES[i][1] + ": its zone " + Location.COUNTRIES[i][k] + " has the country's calculation",
                        i == 0 ? "أم القرى".equals(PrayerMethod.authority(Location.COUNTRIES[i][0], Location.COUNTRIES[i][k])) : !"أم القرى".equals(PrayerMethod.authority(Location.COUNTRIES[i][0], Location.COUNTRIES[i][k])), "");
        // the first-run note: while the city was never set, the first three starts
        check("note: due on the first three starts while no city was set, never once one was",
                Location.noteDue(true, false, 0) && Location.noteDue(true, false, 2) && !Location.noteDue(true, false, 3) && !Location.noteDue(true, true, 0), "");
        check("note: never on a TV that had the app before and was updated, whatever its city", !Location.noteDue(false, false, 0) && !Location.noteDue(false, true, 0), "");
        check("note: its words", Location.note("الرياض").equals("المدينة: الرياض. لتغييرها: الإعدادات ثم الموقع"), Location.note("الرياض"));
        System.exit(failed == 0 ? 0 : 1);
    }
}
