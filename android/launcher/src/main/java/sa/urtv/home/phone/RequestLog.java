package sa.urtv.home.phone;

import java.util.Locale;

/**
 * What the TV logs about a request to the phone page (X, 19 Nov: a QR that «could not be reached»; the next report must be able to say whether the request got to
 * the TV at all): the path and the browser's family, nothing else. Never an address, a query, a token, an id or the whole user agent. No Android in here:
 * tests/RequestLogRun.java runs it.
 */
public final class RequestLog {
    private RequestLog() { }

    /** a short family word from a User-Agent: «iphone-safari», «android-chrome», «android-other», «chrome», «safari», «firefox», «other» or «none» */
    public static String family(String ua) {
        if (ua == null || ua.trim().isEmpty()) return "none";
        String u = ua.toLowerCase(Locale.ROOT);
        boolean android = u.contains("android"), ios = u.contains("iphone") || u.contains("ipad");
        if (ios) return "iphone-safari";
        if (android) return u.contains("chrome") ? "android-chrome" : "android-other";
        if (u.contains("firefox")) return "firefox";
        if (u.contains("chrome") || u.contains("chromium")) return "chrome";
        if (u.contains("safari")) return "safari";
        return "other";
    }

    /** the path without its query, each segment longer than 12 characters (a token, an id) as «*», cut to 60 characters */
    public static String path(String p) {
        if (p == null) return "-";
        int q = p.indexOf('?');
        if (q >= 0) p = p.substring(0, q);
        StringBuilder b = new StringBuilder();
        for (String seg : p.split("/", -1)) {
            if (b.length() > 0 || !seg.isEmpty()) b.append('/');
            b.append(seg.length() > 12 ? "*" : seg);
        }
        String s = b.length() == 0 ? "/" : b.toString();
        return s.length() > 60 ? s.substring(0, 60) : s;
    }

    /** the one log line: «request path=/api/state ua=android-chrome» */
    public static String line(String method, String path, String ua) {
        String m = method == null ? "-" : method.replaceAll("[^A-Za-z]", "");
        return "request " + (m.isEmpty() ? "-" : m) + " path=" + path(path) + " ua=" + family(ua);
    }
}
