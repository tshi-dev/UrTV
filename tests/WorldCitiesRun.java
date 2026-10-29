package sa.urtv.screensaver;

/** Dump actual decoded app rows and resolved country methods for Python tests. */
public final class WorldCitiesRun {
    public static void main(String[] args) {
        int matched = 0;
        for (Cities.Region r : Cities.REGIONS) for (String[] c : r.cities) if (c.length > 4) {
            for (String name : new String[] {c[0], c[4]}) {
                java.util.List<String[]> hits = CityMatch.find(r.country + "|" + name);
                if (!hits.contains(c) || hits.stream().anyMatch(h -> !Cities.country(h).equals(r.country)))
                    throw new AssertionError("bilingual city match " + r.country + " " + name);
                matched++;
            }
        }
        if (matched != 6610) throw new AssertionError("world name coverage");
        System.out.println("MATCHED\t" + matched);
        for (Cities.Region r : Cities.REGIONS) for (String[] c : r.cities)
            System.out.println("CITY\t" + r.country + "\t" + c[0] + "\t" + c[1] + "\t" + c[2] + "\t"
                + (c.length > 3 ? c[3] : "Asia/Riyadh") + "\t" + (c.length > 4 ? c[4] : "") + "\t" + r.method + "\t" + Cities.display(c));
        for (String[] c : Location.COUNTRIES)
            System.out.println("COUNTRY\t" + c[0] + "\t" + c[1] + "\t" + PrayerMethod.methodId(c[0], c[2]) + "\t"
                + PrayerMethod.fragment("auto", c[0], c[2]) + "\t" + PrayerMethod.authority(c[0], c[2]));
        // Brussels is shared by several countries. An explicit country wins.
        if (PrayerMethod.methodId("FR", "Europe/Brussels") != 12 || PrayerMethod.methodId("IR", "Asia/Tehran") != 3
                || !PrayerMethod.searchable("BQ") || PrayerMethod.searchable("PS") || PrayerMethod.searchable("IL") || PrayerMethod.searchable("EH")) throw new AssertionError("country choice");
    }
}
