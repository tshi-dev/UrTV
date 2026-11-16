package sa.urtv.screensaver;

/**
 * Where the sun is (no Android, tested on a plain JVM): the low-precision solar position formulas (about 0.1 degree), enough to say whether it is night at a place.
 * «وضع ليلي: تلقائي بعد المغرب» uses it: Maghrib is sunset, so the games turn to the night look when the sun is below the horizon at the TV's city and back at sunrise. The
 * city's coordinates are the ones the person set for the prayer times; no network and no prayer calendar is needed.
 */
final class SunPos {
    private SunPos() {}

    /** the sun's height above the horizon in degrees (negative below it) for a latitude, a longitude (east positive) and a UTC time in milliseconds */
    static double elevationDeg(double latDeg, double lonDeg, long utcMillis) {
        double n = utcMillis / 86400000.0 + 2440587.5 - 2451545.0;                       // days since J2000
        double meanLon = mod(280.460 + 0.9856474 * n, 360), g = Math.toRadians(mod(357.528 + 0.9856003 * n, 360));
        double lambda = Math.toRadians(meanLon + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g));
        double eps = Math.toRadians(23.439 - 0.0000004 * n);
        double dec = Math.asin(Math.sin(eps) * Math.sin(lambda));
        double ra = Math.atan2(Math.cos(eps) * Math.sin(lambda), Math.cos(lambda));
        double gmst = mod(280.46061837 + 360.98564736629 * n, 360);
        double ha = Math.toRadians(gmst + lonDeg) - ra;
        double lat = Math.toRadians(latDeg);
        return Math.toDegrees(Math.asin(Math.sin(lat) * Math.sin(dec) + Math.cos(lat) * Math.cos(dec) * Math.cos(ha)));
    }

    /** night: the sun's upper edge is below the horizon (-0.833 degrees: its radius and the air's bending); Maghrib is the moment this becomes true */
    static boolean isNight(double latDeg, double lonDeg, long utcMillis) { return elevationDeg(latDeg, lonDeg, utcMillis) < -0.833; }

    private static double mod(double x, double m) { double r = x % m; return r < 0 ? r + m : r; }
}
