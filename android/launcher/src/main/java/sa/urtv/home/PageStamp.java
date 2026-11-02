package sa.urtv.home;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which page is newer, the app's own copy (assets/tv.html) or the one downloaded: by the build stamp src/build.py puts in the page's head
 * (<meta name="urtv-build" content="YYYYMMDDHHMMSS">, the UTC time of the newest commit of its sources), never by file time. A download from
 * a CDN can be minutes or hours older than the page the app carries yet get a newer file time. Plain Java: tests/PageStampRun.java runs it. The same file as the screensaver's but its package (tests/home_page_test.py).
 */
final class PageStamp {
    private static final Pattern STAMP = Pattern.compile("<meta\\s+name=\"urtv-build\"\\s+content=\"(\\d{14})\"");
    /** the stamp is in the first bytes of the page */
    private static final int HEAD = 8192;

    private PageStamp() {}

    /** the stamp in the start of a page; "" for a page with none (an old one) */
    static String of(String head) {
        Matcher m = STAMP.matcher(head == null ? "" : head);
        return m.find() ? m.group(1) : "";
    }

    /** the stamp of a page read from a stream (closed here); "" when it has none or cannot be read */
    static String read(InputStream in) {
        try {
            byte[] buf = new byte[HEAD];
            int n = 0, k;
            while (n < buf.length && (k = in.read(buf, n, buf.length - n)) > 0) n += k;
            return of(new String(buf, 0, n, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return "";
        } finally {
            try { in.close(); } catch (Exception ignored) { }
        }
    }

    /** is the page with stamp a newer than the one with stamp b? Fourteen digits compare as text; no stamp is older than any. */
    static boolean newer(String a, String b) { return a.compareTo(b) > 0; }
}
