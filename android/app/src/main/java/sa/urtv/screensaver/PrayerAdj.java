package sa.urtv.screensaver;

/**
 * «تعديل وقت الصلاة»: a person's mosque calls the adhan a few minutes before or after the calculated time, so each of the five
 * prayers can be moved by whole minutes, −20 to +20. Off by default. The offsets go to the page and the home screen in the settings
 * fragment as {@code adj=0,2,0,0,-1} (الفجر الظهر العصر المغرب العشاء), only while the setting is on; the sunrise is never moved.
 * Plain Java on purpose: tests/PrayerAdjRun.java runs it on a JVM.
 */
final class PrayerAdj {
    static final int MAX = 20;
    static final int COUNT = 5;
    /** the names optimize/saver.sh and the receiver use, in the order of the fragment */
    static final String[] KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
    static final String[] NAMES = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};

    private PrayerAdj() { }

    /** A value from the shell: a whole number of minutes, "+2", "2", "-1", "0"; null when it is not one or is out of range. */
    static Integer parse(String s) {
        if (s == null || !s.matches("[+-]?\\d{1,2}")) return null;
        int v = Integer.parseInt(s.startsWith("+") ? s.substring(1) : s);
        return v < -MAX || v > MAX ? null : v;
    }

    /** The saved text "0,2,0,0,-1" as five numbers; anything else is no adjustment. Each is held to −20..20. */
    static int[] decode(String saved) {
        int[] out = new int[COUNT];
        if (saved == null) return out;
        String[] parts = saved.split(",", -1);
        if (parts.length != COUNT) return new int[COUNT];
        for (int i = 0; i < COUNT; i++) {
            Integer v = parse(parts[i].trim());
            if (v == null) return new int[COUNT];
            out[i] = v;
        }
        return out;
    }

    static String encode(int[] v) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < COUNT; i++) b.append(i == 0 ? "" : ",").append(v[i]);
        return b.toString();
    }

    /** One step of the + / − keys, held to −20..20. */
    static int step(int v, int delta) { return Math.max(-MAX, Math.min(MAX, v + delta)); }

    static boolean any(int[] v) {
        for (int x : v) if (x != 0) return true;
        return false;
    }

    /** What goes in the settings fragment: nothing while the setting is off or every offset is zero (the page is then as it was). */
    static String fragment(boolean on, int[] v) {
        boolean any = false;
        for (int x : v) any |= x != 0;
        return on && any ? "&adj=" + encode(v) : "";
    }

    /** «الفجر +٢ د», «العشاء −١ د», «الظهر ٠ د» (Arabic digits, the real minus sign) */
    static String label(int index, int v) {
        String sign = v > 0 ? "+" : v < 0 ? "−" : "";
        return NAMES[index] + " " + sign + DigitsText.convert(String.valueOf(Math.abs(v)), false) + " د";
    }

    /** "fajr=+2 dhuhr=0 asr=0 maghrib=-1 isha=0" for optimize/saver.sh show */
    static String describe(int[] v) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < COUNT; i++) b.append(i == 0 ? "" : " ").append(KEYS[i]).append('=').append(v[i] > 0 ? "+" : "").append(v[i]);
        return b.toString();
    }
}
