package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.4 beta 3: «الوضع الخفيف» on «تلقائي» also turns on where THIS TV shows it is weak, not only by its specs (Ram.tier: the low-RAM flag, under 2000 MB, two
 * cores). A signal is one of: the system asked the home in front to give memory back (onTrimMemory RUNNING_LOW or CRITICAL), the home's first frame took longer
 * than {@link #SLOW_MS}, or the system killed the home for memory (the crash counter's low-memory kill). Two signals within {@link #WINDOW} turn it on; it
 * stays on until {@link #HOLD} pass with none. It counts from the home's next start (nothing changes under the person), and it is said once (LiteAutoStore).
 * «تشغيل» and «إيقاف» still win, and `dock.sh ramtier` wins over all. The state is one short text: «kind time» per line. No Android in here (tests/LiteAutoRun.java).
 */
final class LiteAuto {
    private LiteAuto() { }

    static final long DAY = 86_400_000L, WINDOW = 7 * DAY, HOLD = 14 * DAY;
    static final long SLOW_MS = 8_000;
    static final String TRIM = "trim", SLOW = "slow", OOM = "oom";
    static final int KEEP = 20;
    /** one signal of a kind per this long: Android sends the low-memory callbacks in bursts seconds apart, and a burst is one event (review F99) */
    static final long DEBOUNCE = 3 * 3_600_000L;
    /** where the signals are kept, in the «ram» preferences (Ram.tier reads them; Ram.java is shared with the add-on, so the key lives here) */
    static final String KEY = "auto_signals";

    static final class Event { final String kind; final long at; Event(String k, long t) { kind = k; at = t; } }

    static List<Event> parse(String text) {
        List<Event> out = new ArrayList<>();
        if (text == null) return out;
        for (String l : text.split("\n")) {
            String[] w = l.trim().split(" ");
            if (w.length != 2 || !(TRIM.equals(w[0]) || SLOW.equals(w[0]) || OOM.equals(w[0]))) continue;
            try { out.add(new Event(w[0], Long.parseLong(w[1]))); } catch (NumberFormatException ignored) { }
        }
        return out;
    }

    /** the text with this signal added; signals older than HOLD, from the future, and all but the last KEEP are let go */
    static String add(String text, String kind, long now) { return add(text, kind, now, now); }

    /** the text as it is, cleaned of what is too old, from the future or past KEEP */
    private static String add(String text, long now) {
        List<Event> kept = new ArrayList<>();
        for (Event e : parse(text)) if (e.at <= now && now - e.at <= HOLD) kept.add(e);
        kept.sort((a, b) -> Long.compare(a.at, b.at));
        StringBuilder b = new StringBuilder();
        for (Event e : kept.subList(Math.max(0, kept.size() - KEEP), kept.size())) b.append(e.kind).append(' ').append(e.at).append('\n');
        return b.toString();
    }

    /** a signal that happened at {@code at} (a low-memory kill read at the next start), added {@code now}: the window is reckoned from now, never from at */
    static String add(String text, String kind, long at, long now) {
        List<Event> all = parse(text);
        for (Event e : all) if (e.kind.equals(kind) && Math.abs(e.at - at) < DEBOUNCE) return add(text, now);   // the same burst: nothing new
        all.add(new Event(kind, at));
        List<Event> kept = new ArrayList<>();
        for (Event e : all) if (e.at <= now && now - e.at <= HOLD) kept.add(e);
        kept.sort((a, b) -> Long.compare(a.at, b.at));
        StringBuilder b = new StringBuilder();
        for (Event e : kept.subList(Math.max(0, kept.size() - KEEP), kept.size())) b.append(e.kind).append(' ').append(e.at).append('\n');
        return b.toString();
    }

    /** on: two signals within WINDOW of each other, and the last one less than HOLD ago (a clock that went back counts nothing from the future) */
    static boolean on(String text, long now) {
        List<Event> es = new ArrayList<>();
        for (Event e : parse(text)) if (e.at <= now) es.add(e);
        if (es.size() < 2) return false;
        es.sort((a, b) -> Long.compare(a.at, b.at));
        if (now - es.get(es.size() - 1).at > HOLD) return false;
        for (int i = 1; i < es.size(); i++) if (es.get(i).at - es.get(i - 1).at <= WINDOW) return true;
        return false;
    }

    /** when it came on: the later signal of the first pair within WINDOW (the mark that it was said once), 0 when off */
    static long since(String text, long now) {
        if (!on(text, now)) return 0;
        List<Event> es = new ArrayList<>();
        for (Event e : parse(text)) if (e.at <= now) es.add(e);
        es.sort((a, b) -> Long.compare(a.at, b.at));
        for (int i = 1; i < es.size(); i++) if (es.get(i).at - es.get(i - 1).at <= WINDOW) return es.get(i).at;
        return 0;
    }

    /** why: the kind seen most in the last HOLD (OOM first, then TRIM, then SLOW); "" when off */
    static String reasonKind(String text, long now) {
        if (!on(text, now)) return "";
        int trim = 0, slow = 0, oom = 0;
        for (Event e : parse(text)) if (e.at <= now && now - e.at <= HOLD) { if (TRIM.equals(e.kind)) trim++; else if (SLOW.equals(e.kind)) slow++; else oom++; }
        if (oom >= trim && oom >= slow) return OOM;
        return trim >= slow ? TRIM : SLOW;
    }

    /** the report's word (letters, digits and dashes only, as Report.safe keeps them): «auto-on-t2-s1-o0» or «auto-off-t0-s1-o0» */
    static String report(String text, long now) {
        int trim = 0, slow = 0, oom = 0;
        for (Event e : parse(text)) if (e.at <= now && now - e.at <= HOLD) { if (TRIM.equals(e.kind)) trim++; else if (SLOW.equals(e.kind)) slow++; else oom++; }
        return (on(text, now) ? "auto-on" : "auto-off") + "-t" + trim + "-s" + slow + "-o" + oom;
    }
}
