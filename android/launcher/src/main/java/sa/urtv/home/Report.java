package sa.urtv.home;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * «أرسل تقرير» of the settings: a very short problem report as a link to docs/share/ (the page optimize/share.sh's QR code opens: the report
 * travels after the #, and reaches the author only when the owner presses «أرسل» there). Every line is one of share.py's fields (its
 * ALLOWED_LINE; tests/settings_report_test.py checks them against share.py itself): the apps' versions and the TV's Android and WebView, nothing
 * else (no city, name, address, network or serial is ever read here). The words of the screen are here too, in one place. No Android in it.
 * (work/safe-mode's SafeReport is the same kind of link for the safe mode; the two can become one when it is merged.)
 */
public final class Report {
    private Report() { }

    /** optimize/share.py's PAGE */
    public static final String PAGE = "https://tshi-dev.github.io/UrTV/share/";
    /** the longest link phone.QrCode.encodeLarge holds (version 40, level L, byte mode), as share.py's segno */
    public static final int MAX_BYTES = 2953;

    public static final String TITLE = S.n("أرسل تقرير");
    public static final String HOW = S.n("امسح الرمز بكاميرا الجوال: تفتح صفحة فيها هذا التقرير، ولا يوصلنا شيء إلا إذا ضغطت «أرسل» فيها. اكتب فيها المشكلة بكلماتك.");
    public static final String WHAT = S.n("ما في التقرير:");
    public static final String BACK = S.n("رجوع للإعدادات");

    /** The report's lines: the apps' versions, the TV's Android and its WebView. */
    public static List<String> lines(String home, String saver, String release, int sdk, String webView) {
        List<String> l = new ArrayList<>();
        l.add("UrTV problem report, settings");
        l.add("Versions: home " + safe(home) + ", screensaver " + safe(saver));
        l.add("Device: Android " + safe(release) + ", API " + sdk);
        l.add("WebView: " + safe(webView));
        return l;
    }

    /** The link for the QR code, or null when not even the first two lines fit. */
    public static String url(List<String> lines) {
        List<String> l = new ArrayList<>(lines);
        while (l.size() >= 2) {
            String u = PAGE + "#" + quote(String.join("\n", l) + "\n");
            if (u.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES) return u;
            l.remove(l.size() - 1);
        }
        return null;
    }

    /** Python's urllib.parse.quote(text): UTF-8, everything but letters, digits, _ . - ~ and / as %XX (upper case). */
    public static String quote(String text) {
        StringBuilder b = new StringBuilder();
        for (byte x : text.getBytes(StandardCharsets.UTF_8)) {
            int c = x & 0xff;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-' || c == '~' || c == '/') b.append((char) c);
            else b.append('%').append(String.format(Locale.ROOT, "%02X", c));
        }
        return b.toString();
    }

    /** a version or release word: letters, digits, dots and dashes only, at most 24 ("?" for none) */
    static String safe(String s) {
        if (s == null) return "?";
        String t = s.replaceAll("[^A-Za-z0-9.-]", "");
        return t.isEmpty() ? "?" : t.length() > 24 ? t.substring(0, 24) : t;
    }
}
