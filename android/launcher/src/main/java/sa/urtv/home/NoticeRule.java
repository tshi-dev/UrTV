package sa.urtv.home;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 4.3.3 Short remote notices on the home (urtv-data notices/announcements.json, docs/SCHEMAS.md there): one line in the bar at the top of
 * the home (the same bar as «خلّ تلفزيونك يفتح على UrTV», and only when that bar has nothing else to say), never while our tilawah or adhan
 * plays, never in kids mode or «UrTV النظيف». At most one at a time: the first in the file's order that is due. Each id is shown once: on the
 * visit it first appears it stays until the home goes behind (or «تم» for one that is dismissible), and never again after. A notice with a
 * link, over 140 characters, outside its days or outside its app versions is never shown. Plain Java on a JVM (tests/NoticeRuleRun.java).
 */
final class NoticeRule {
    private NoticeRule() { }

    static final String PATH = "notices/announcements.json", NAME = "announcements.json";
    static final int TEXT_MAX = 140;

    static final class Notice {
        final String id, ar, en;
        final boolean dismissible;
        Notice(String id, String ar, String en, boolean dismissible) { this.id = id; this.ar = ar; this.en = en; this.dismissible = dismissible; }
        String text(boolean english) { return english && !en.isEmpty() ? en : ar; }
    }

    /**
     * The notice to show now, or null: due on {@code day} (YYYY-MM-DD, the TV's day), for {@code version} (the app's "4.3.3"), and either not in
     * {@code seen} or the one already on screen in this visit ({@code current}).
     */
    static Notice pick(Object doc, String day, String version, Set<String> seen, String current) {
        if (!(doc instanceof Map) || day == null) return null;
        Object list = ((Map<?, ?>) doc).get("notices");
        if (!(list instanceof List)) return null;
        for (Object o : (List<?>) list) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> n = (Map<?, ?>) o;
            String id = str(n.get("id"));
            if (!id.matches("[a-z0-9][a-z0-9-]{1,47}")) continue;
            if (seen.contains(id) && !id.equals(current)) continue;
            String from = str(n.get("from")), to = str(n.get("to"));
            if (from.isEmpty() || to.isEmpty() || day.compareTo(from) < 0 || day.compareTo(to) > 0) continue;
            String lo = str(n.get("min_app")), hi = str(n.get("max_app"));
            if (!lo.isEmpty() && compare(version, lo) < 0) continue;
            if (!hi.isEmpty() && compare(version, hi) > 0) continue;
            Map<?, ?> t = n.get("text") instanceof Map ? (Map<?, ?>) n.get("text") : java.util.Collections.emptyMap();
            String ar = str(t.get("ar")).trim(), en = str(t.get("en")).trim();
            if (!fits(ar) || (!en.isEmpty() && !fits(en))) continue;
            return new Notice(id, ar, en, !Boolean.FALSE.equals(n.get("dismissible")));
        }
        return null;
    }

    /** a line the bar can show: not empty, at most TEXT_MAX, one line, no link (a TV cannot open one, and a link is a way to mislead) */
    static boolean fits(String s) {
        String l = s.toLowerCase(java.util.Locale.ROOT);
        return !s.isEmpty() && s.length() <= TEXT_MAX && s.indexOf('\n') < 0 && !l.contains("http") && !l.contains("www.") && !l.matches(".*\\b[a-z0-9-]+\\.(com|net|org|sa|io|ly|me)\\b.*");
    }

    /** "4.3.3" against "4.3": numbers part by part, a missing part is 0; a part that is not a number counts as 0 */
    static int compare(String a, String b) {
        String[] x = (a == null ? "" : a).split("\\."), y = (b == null ? "" : b).split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? num(x[i]) : 0, q = i < y.length ? num(y[i]) : 0;
            if (p != q) return p < q ? -1 : 1;
        }
        return 0;
    }

    private static int num(String s) { try { return Integer.parseInt(s.replaceAll("[^0-9].*$", "")); } catch (NumberFormatException e) { return 0; } }

    private static String str(Object o) { return o instanceof String ? (String) o : ""; }
}
