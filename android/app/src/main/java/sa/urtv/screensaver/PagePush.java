package sa.urtv.screensaver;

import java.util.Map;
import java.util.TreeMap;

/**
 * 4.4 beta 4 (a user: «شريط الأسواق» turned off, the screensaver already on the screen kept showing the markets): a saved change that the running
 * page can take as it is, without being built again. Today one: the markets line (#mkbar=). Given the address the page was opened with and the
 * one the settings make now, the script that applies the difference to the page, or null when anything else differs too (the page is built again,
 * as before). The page's function answers true; an older page that has none answers false, and the dream builds it again. No Android in it
 * (tests/PagePushRun.java runs it).
 */
final class PagePush {
    private PagePush() {}

    static String script(String shown, String now) {
        if (shown == null || now == null || shown.equals(now)) return null;
        int a = shown.indexOf('#'), b = now.indexOf('#');
        if (a < 0 || b < 0 || !shown.substring(0, a).equals(now.substring(0, b))) return null;
        Map<String, String> x = params(shown.substring(a + 1)), y = params(now.substring(b + 1));
        String was = x.remove("mkbar"), is = y.remove("mkbar");
        if (!x.equals(y) || is == null || is.equals(was)) return null;
        if (!"on".equals(is) && !"off".equals(is)) return null;
        return "(window.urtvMkBar?urtvMkBar(" + "on".equals(is) + "):false)";
    }

    private static Map<String, String> params(String frag) {
        Map<String, String> m = new TreeMap<>();
        for (String part : frag.split("&")) {
            if (part.isEmpty()) continue;
            int eq = part.indexOf('=');
            m.put(eq < 0 ? part : part.substring(0, eq), eq < 0 ? "" : part.substring(eq + 1));
        }
        return m;
    }
}
