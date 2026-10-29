package sa.urtv.home.tilawah;

import java.net.URLDecoder;

/** A settings fragment ("city=..&lat=24.7&lon=46.7&tz=Asia%2FRiyadh&..." as the page reads it) as a place; null when it has no coordinates. No Android in it. */
final class PlaceFragment {
    final double lat, lng;
    final String tz, pm;
    final int[] adj;

    private PlaceFragment(double lat, double lng, String tz, int[] adj, String pm) {
        this.lat = lat; this.lng = lng; this.tz = tz; this.adj = adj; this.pm = pm;
    }

    static PlaceFragment parse(String fragment) {
        if (fragment == null) return null;
        try {
            String lat = null, lon = null, tz = null, pm = null;
            for (String part : fragment.split("&")) {
                int eq = part.indexOf('=');
                if (eq < 0) continue;
                String key = part.substring(0, eq), value = URLDecoder.decode(part.substring(eq + 1), "UTF-8");
                if ("lat".equals(key)) lat = value;
                else if ("lon".equals(key)) lon = value;
                else if ("tz".equals(key)) tz = value;
                else if ("pm".equals(key)) pm = value;
            }
            if (lat == null || lon == null) return null;
            return new PlaceFragment(number(lat, 24.6877), number(lon, 46.7219), orDefault(tz, "Asia/Riyadh"), PrayerAdjust.fromFragment(fragment), pm);
        } catch (Exception e) {
            return null;
        }
    }

    private static double number(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return fallback; }
    }

    private static String orDefault(String s, String fallback) {
        return s == null || s.trim().isEmpty() ? fallback : s.trim();
    }
}
