package sa.urtv.screensaver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** The clock side of NextMatch (no Android, no JSON: tested on a plain JVM): a start time read from the source (UTC) and shown in the city's zone. */
final class NextMatchTime {
    private NextMatchTime() {}

    /** UTC ms from strTimestamp ("2026-11-12T19:00:00", UTC) or dateEvent + strTime; 0 if unreadable */
    static long start(String stamp, String date, String time) {
        String s = !stamp.isEmpty() ? stamp.replace(' ', 'T') : (date.isEmpty() ? "" : date + "T" + (time.isEmpty() ? "00:00:00" : time));
        if (s.length() < 16) return 0;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            return f.parse(s.substring(0, 16)).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    /** 0 today, 1 tomorrow, else 2 (a weekday name is used) in the zone `tz` */
    static int dayOffset(long at, long now, TimeZone tz) {
        Calendar a = Calendar.getInstance(tz), b = Calendar.getInstance(tz);
        a.setTimeInMillis(at);
        b.setTimeInMillis(now);
        int d = (a.get(Calendar.YEAR) - b.get(Calendar.YEAR)) * 366 + a.get(Calendar.DAY_OF_YEAR) - b.get(Calendar.DAY_OF_YEAR);
        return d <= 0 ? 0 : d == 1 ? 1 : 2;
    }

    /** the time and day as text in the city's zone: «اليوم ٩:٠٠ م», «غدًا …», or the weekday; en = English words */
    static String when(long at, long now, TimeZone tz, boolean clock24, boolean en) {
        Locale loc = en ? Locale.ENGLISH : new Locale("ar");
        SimpleDateFormat t = new SimpleDateFormat(clock24 ? "HH:mm" : "h:mm a", loc);
        t.setTimeZone(tz);
        int d = dayOffset(at, now, tz);
        String day;
        if (d == 0) day = en ? "today" : "اليوم";
        else if (d == 1) day = en ? "tomorrow" : "غدًا";
        else {
            SimpleDateFormat w = new SimpleDateFormat("EEEE", loc);
            w.setTimeZone(tz);
            day = w.format(at);
        }
        return day + " " + t.format(at);
    }

    /** the hub's line for a picked match, in the screen's language */
    static String line(long at, String homeName, String awayName, long now, TimeZone tz, boolean clock24, boolean en) {
        String w = when(at, now, tz, clock24, en);
        return en ? "Next: " + homeName + " vs " + awayName + ", " + w
                  : "التالية: " + homeName + " ضد " + awayName + "، " + w;
    }

    /** Arabic-Indic digits to 0-9 (the «الأرقام» setting "latin") */
    static String latin(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) b.append(ch >= '\u0660' && ch <= '\u0669' ? (char) ('0' + ch - '\u0660') : ch);
        return b.toString();
    }
}
