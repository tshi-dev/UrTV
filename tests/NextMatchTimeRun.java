package sa.urtv.screensaver;

import java.util.TimeZone;

/** For tests/your_tv_hub_test.py: NextMatchTime on a plain JVM. args: stampUTC nowUTC(ms) tz clock24 en */
public final class NextMatchTimeRun {
    public static void main(String[] a) {
        long at = NextMatchTime.start(a[0], "", "");
        long now = java.time.Instant.parse(a[1]).toEpochMilli();
        TimeZone tz = TimeZone.getTimeZone(a[2]);
        System.out.println(NextMatchTime.line(at, "A", "B", now, tz, a[3].equals("1"), a[4].equals("1")));
    }
}
