package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;

/**
 * «تبديل الصورة كل»: the time between two background photos. Minutes as always (5 10 15 30 60, 30 the default), and now 1 minute, and 30
 * seconds ("30s") on a TV with the memory for it. A photo is a full-screen decode; on the 1 GB and 1.5 GB TVs of the reports a new one every
 * 30 s would keep the WebView busy, so there 1 minute is the shortest: "30s" is not offered, refused from ADB, and one saved on another TV
 * (or before) reads as 1 minute. The page gets minutes in #photos=, 0.5 for 30 s (an older page reads 0.5 as unknown: its 30 minutes).
 * Plain Java on purpose: tests/PhotoIntervalRun.java runs it on a JVM; the memory is read in Prefs.ramMb.
 */
final class PhotoInterval {
    private PhotoInterval() { }

    static final String SECONDS_30 = "30s", ONE = "1", DEFAULT = "30";
    /** every value, shortest first; the receiver and the picker take these (the picker without 30s on a smaller TV) */
    static final String[] ALL = {SECONDS_30, ONE, "5", "10", "15", "30", "60"};

    /**
     * MemTotal at or above this is a «2 GB» TV: the reports' 2 GB sets say 1765 to 2393 MB (the kernel keeps some), a 1.5 GB Chromecast HD
     * 1397 and the 1 GB sets about 907. Unknown (0 or less) is treated as small.
     */
    static final long FAST_RAM_MB = 1600;

    static boolean fast(long ramMb) { return ramMb >= FAST_RAM_MB; }

    /** the choices this TV offers, shortest first */
    static String[] choices(long ramMb) {
        List<String> v = new ArrayList<>();
        for (String s : ALL) if (fast(ramMb) || !SECONDS_30.equals(s)) v.add(s);
        return v.toArray(new String[0]);
    }

    static boolean known(String v) {
        for (String s : ALL) if (s.equals(v)) return true;
        return false;
    }

    /** null when the receiver may save it on this TV; otherwise why not, as the receiver says it */
    static String refuse(String v, long ramMb) {
        if (!known(v)) return "error: photos is one of " + String.join(" ", ALL) + " (minutes; 30s = 30 seconds)";
        if (SECONDS_30.equals(v) && !fast(ramMb))
            return "error: photos 30s needs a TV with 2 GB of memory; this one has " + Math.max(0, ramMb) + " MB, so 1 (minute) is the shortest here";
        return null;
    }

    /** the stored value as the TV uses it: anything unknown is the default 30 minutes, 30 s on a smaller TV is 1 minute */
    static String read(String stored, long ramMb) {
        if (!known(stored)) return DEFAULT;
        return SECONDS_30.equals(stored) && !fast(ramMb) ? ONE : stored;
    }

    /** #photos= for the page: the minutes, 0.5 for 30 s */
    static String fragmentValue(String v) { return SECONDS_30.equals(v) ? "0.5" : v; }

    /** the row's words, in Arabic-Indic digits (the settings screen turns them with «الأرقام»): «٣٠ ثانية», «دقيقة واحدة», «دقيقتان», «٥ دقائق», «١٥ دقيقة» */
    static String label(String v) {
        if (SECONDS_30.equals(v)) return "٣٠ ثانية";
        int n;
        try { n = Integer.parseInt(v); } catch (NumberFormatException e) { n = Integer.parseInt(DEFAULT); }
        if (n == 1) return "دقيقة واحدة";
        if (n == 2) return "دقيقتان";
        return DigitsText.convert(String.valueOf(n), false) + (n <= 10 ? " دقائق" : " دقيقة");
    }
}
