package sa.urtv.home.phone;

/**
 * 4.3 #53 «اكتب وارسم», the text: a short message from a paired phone shown on the TV for SHOW_MS (Back hides it at once; MessageActivity).
 * Only a phone the owner allowed (the session) sends one; «رسائل الجوال» (optimize/dock.sh message on|off) turns it off; «فوق التطبيقات»
 * (dock.sh message overapps off) shows it only on our home and the screensaver, never over a film or a match. A phone sends at most one
 * every GAP_MS and BURST in WINDOW_MS. Plain Java (tests/phone_message_test.py runs it).
 */
public final class PhoneMessage {
    public static final int MAX = 200, MAX_LINES = 4;
    public static final long SHOW_MS = 20_000, GAP_MS = 3_000, WINDOW_MS = 10 * 60_000L;
    public static final int BURST = 20;

    public enum Verdict { SHOW, OFF, BUSY_APP, SLOW_DOWN, EMPTY }

    private final long[] sent = new long[BURST];
    private int n;

    private PhoneMessage() { }

    public static PhoneMessage limiter() { return new PhoneMessage(); }

    /** The text as it is shown: spaces folded, at most MAX_LINES lines and MAX characters; "" when nothing is left. */
    public static String clean(String text) {
        if (text == null) return "";
        StringBuilder b = new StringBuilder();
        int lines = 0;
        for (String l : text.replace("\r", "").split("\n")) {
            String s = l.replaceAll("[\\s\\u00A0]+", " ").trim();
            if (s.isEmpty()) continue;
            if (lines == MAX_LINES) break;
            if (b.length() > 0) b.append('\n');
            b.append(s);
            lines++;
        }
        String out = b.toString();
        return out.length() > MAX ? out.substring(0, MAX) : out;
    }

    /**
     * on: «رسائل الجوال» is on. overApps: it may show over another app. ourScreen: our home (or the message itself) is in front, or the
     * screensaver is on. now: elapsedRealtime. A SHOW is counted for the rate.
     */
    public synchronized Verdict decide(String text, boolean on, boolean overApps, boolean ourScreen, long now) {
        if (!on) return Verdict.OFF;
        if (clean(text).isEmpty()) return Verdict.EMPTY;
        if (!overApps && !ourScreen) return Verdict.BUSY_APP;
        int recent = 0;
        long last = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            if (now - sent[i] < WINDOW_MS) recent++;
            last = Math.max(last, sent[i]);
        }
        if (n > 0 && now - last < GAP_MS) return Verdict.SLOW_DOWN;
        if (recent >= BURST) return Verdict.SLOW_DOWN;
        long[] keep = new long[BURST];
        int k = 0;
        for (int i = 0; i < n; i++) if (now - sent[i] < WINDOW_MS && k < BURST - 1) keep[k++] = sent[i];
        keep[k++] = now;
        System.arraycopy(keep, 0, sent, 0, k);
        n = k;
        return Verdict.SHOW;
    }

    /** The word the phone page shows for a verdict (its MSG table). */
    public static String code(Verdict v) {
        switch (v) {
            case SHOW: return "message_shown";
            case OFF: return "message_off";
            case BUSY_APP: return "message_busy_app";
            case EMPTY: return "bad_request";
            default: return "slow_down";
        }
    }
}
