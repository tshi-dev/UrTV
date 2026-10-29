package sa.urtv.home.safe;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * «أرسل تقرير المشكلة» of the safe mode: a very short problem report as a link to docs/share/ (the page optimize/share.sh's QR code opens: the report
 * travels after the #, and reaches the author only when the owner presses «أرسل» there). Every line is one of share.py's fields (its ALLOWED_LINE;
 * tests/safe_mode_test.py checks them against share.py itself), with no name, address or serial in it. The link must fit the TV's own QR encoder
 * (phone.QrCode, version 10 at level M: MAX_BYTES), so lines are dropped from the end until it does. No Android in it.
 * (work/phone-setup has the full setup report with the same link, SetupReport.shareUrl; this is the safe mode's own few lines.)
 */
public final class SafeReport {
    private SafeReport() { }

    /** optimize/share.py's PAGE */
    public static final String PAGE = "https://tshi-dev.github.io/UrTV/share/";
    /** what phone.QrCode holds at its largest (version 10, level M, byte mode) */
    public static final int MAX_BYTES = 213;
    public static final String ISSUES = "github.com/tshi-dev/UrTV/issues";

    /** The report's lines: what happened to the home screen, the Android version, our version, the home switch's state. */
    public static List<String> lines(CrashLoop loop, int sdk, String release, String version, String switchState) {
        List<String> l = new ArrayList<>();
        l.add("UrTV problem report, safe mode");
        int crash = 0, hang = 0;
        String w = loop.write();
        for (String line : w.split("\n")) {
            if (line.startsWith("crash ") || line.startsWith("native ")) crash++;
            else if (line.startsWith("hang ") || line.startsWith("anr ")) hang++;
        }
        l.add("Home: " + crash + " crash, " + hang + " hang");
        l.add("Versions: home " + safe(version));
        l.add("Device: Android " + safe(release) + ", API " + sdk);
        if (switchState != null && switchState.matches("[a-z]{1,12}")) l.add("Home switch: " + switchState);
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

    /** a version or release word: letters, digits, dots and dashes only, at most 20 */
    private static String safe(String s) {
        if (s == null) return "?";
        String t = s.replaceAll("[^A-Za-z0-9.-]", "");
        return t.isEmpty() ? "?" : t.length() > 20 ? t.substring(0, 20) : t;
    }
}
