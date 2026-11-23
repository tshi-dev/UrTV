package sa.urtv.screensaver;

/**
 * 4.4 beta 3 (a Bahrain user: Umm al-Qura at every start until «المدينة» was opened): a place of one's own saved without its zone (saver.sh with
 * coordinates, an older search) took its zone and country from the TV's zone at every read, and at a cold start, before the TV has set its zone,
 * that is the wrong country's calculation. Its zone and country now come from the nearest built-in city instead, found once and saved (Prefs.cityTz,
 * Prefs.cityCountry), so the saved place's calculation is in the first page the home and the screensaver draw. Farther than {@link #NEAR_KM} from
 * every built-in city: null (the TV's zone, as before). No Android in here (tests/OwnPlaceRun.java).
 */
final class OwnPlace {
    private OwnPlace() { }

    static final double NEAR_KM = 150;
    /** a built-in Saudi city carries no zone: Prefs.TZ */
    static final String RIYADH = "Asia/Riyadh";

    /** the last place asked and its answer: a place with nothing near is asked at every settings read (the home reads them every few seconds), one scan is enough */
    private static volatile Object[] last;

    /** {zone, country} of the nearest built-in city within NEAR_KM of lat/lon, or null (unreadable numbers, or nothing near) */
    static String[] near(String lat, String lon) {
        String key = lat + "|" + lon;
        Object[] l = last;
        if (l != null && key.equals(l[0])) return (String[]) l[1];
        String[] found = scan(lat, lon);
        last = new Object[] {key, found};
        return found;
    }

    private static String[] scan(String lat, String lon) {
        double la, lo;
        try { la = Double.parseDouble(lat.trim()); lo = Double.parseDouble(lon.trim()); } catch (RuntimeException e) { return null; }
        if (Double.isNaN(la) || Double.isNaN(lo) || Math.abs(la) > 90 || Math.abs(lo) > 180) return null;
        String[] best = null;
        double bestKm = NEAR_KM;
        for (Cities.Region r : Cities.REGIONS) for (String[] c : r.cities) {
            double km;
            try { km = km(la, lo, Double.parseDouble(c[1]), Double.parseDouble(c[2])); } catch (RuntimeException e) { continue; }
            if (km <= bestKm) { bestKm = km; best = new String[] {c.length > 3 && c[3] != null && !c[3].isEmpty() ? c[3] : RIYADH, r.country}; }
        }
        return best;
    }

    static double km(double la1, double lo1, double la2, double lo2) {
        double p1 = Math.toRadians(la1), p2 = Math.toRadians(la2), dp = p2 - p1, dl = Math.toRadians(lo2 - lo1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
