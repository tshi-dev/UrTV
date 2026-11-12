package sa.urtv.home.setup.pair;

/** The six digits the TV shows in «إقران الجهاز برمز الإقران», as plain rules (no Android: tests/PairRun.java runs it). The code is never logged, never stored, never put in a message. */
public final class PairCode {
    private PairCode() { }

    public static final int LENGTH = 6;

    /** the digits of what was typed or pasted (Arabic-Indic and Latin digits both count), nothing else; null when it is not exactly six */
    public static String clean(String typed) {
        if (typed == null) return null;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < typed.length(); i++) {
            char c = typed.charAt(i);
            if (c >= '0' && c <= '9') b.append(c);
            else if (c >= '٠' && c <= '٩') b.append((char) ('0' + (c - '٠')));
            else if (c >= '۰' && c <= '۹') b.append((char) ('0' + (c - '۰')));
            else if (c == ' ' || c == '-') continue;
            else return null;
        }
        return b.length() == LENGTH ? b.toString() : null;
    }

    /** a port as typed (the pairing dialog shows «192.168.x.x:41233»): 1024 to 65535, else -1 */
    public static int port(String typed) {
        String digits = clean0(typed);
        if (digits == null || digits.isEmpty() || digits.length() > 5) return -1;
        int p = Integer.parseInt(digits);
        return p >= 1024 && p <= 65535 ? p : -1;
    }

    private static String clean0(String typed) {
        if (typed == null) return null;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < typed.length(); i++) {
            char c = typed.charAt(i);
            if (c >= '0' && c <= '9') b.append(c);
            else if (c >= '٠' && c <= '٩') b.append((char) ('0' + (c - '٠')));
            else if (c == ' ') continue;
            else return null;
        }
        return b.toString();
    }
}
