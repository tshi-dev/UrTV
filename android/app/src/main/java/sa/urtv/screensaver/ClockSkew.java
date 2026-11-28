package sa.urtv.screensaver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 4.4 beta 4 (a user: the screensaver showed the wrong Gregorian year): the TV's clock against the network's. A server's «Date:» header (RFC 1123,
 * always GMT) is the network's time; a TV clock more than LIMIT_MS away from it is «غير مضبوطة», and the page says so instead of a wrong date. The
 * limit is wide on purpose: a minute or two off is not worth a word, and a time zone set wrong does not move this clock at all (both are UTC).
 * No Android in it (tests/ClockSkewRun.java runs it).
 */
final class ClockSkew {
    private ClockSkew() {}

    static final long LIMIT_MS = 12 * 3600_000L;

    /** an HTTP date («Sat, 28 Nov 2026 11:39:30 GMT») in ms, -1 when it is not one */
    static long parse(String httpDate) {
        if (httpDate == null) return -1;
        SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));   // an HTTP date is GMT, which is UTC; the text «GMT» is still what it reads
        f.setLenient(false);
        try { return f.parse(httpDate.trim()).getTime(); } catch (ParseException | RuntimeException e) { return -1; }
    }

    /** 1: the TV's clock is off, 0: it is right, -1: the network's time is not known */
    static int judge(long tvMs, long netMs) {
        if (netMs <= 0) return -1;
        return Math.abs(tvMs - netMs) > LIMIT_MS ? 1 : 0;
    }
}
