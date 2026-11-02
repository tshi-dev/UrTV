package sa.urtv.home;

/**
 * How an on/off setting looks, everywhere in the settings (the owner's choice I, notes/settings-proto/v5-toggle-I.png): no switch; the row's
 * value starts with its state word, and while it is on three green triangles (R.drawable.ic_on) stand at the row's start and that word is
 * green. The greens are the DGA platforms' «sa-flag» ones (@platformscode/core): 600 on a light fill, 400 on a dark one.
 * This reads the state from the value the row already shows, so no row needs another string. The screensaver app has the same class (sa.urtv.screensaver.OnOff).
 * Plain Java: tests/OnOffRun.java runs it.
 */
public final class OnOff {
    private OnOff() { }

    /** the words a value starts with when it is on, and when it is off (the settings' own words; a translation adds its own here) */
    public static final String[] ON = {"يعمل", "تعمل", "مفعّل", "مفعّلة", "ظاهر", "ظاهرة"};
    public static final String[] OFF = {"متوقف", "متوقفة", "مخفي", "مخفية"};
    /** the two words of a plain on/off value («الأذان» on the first screen) */
    public static final String ON_WORD = "مفعّل", OFF_WORD = "متوقف";

    /** DGA sa-flag 600 (the primary) and 400: green ink on a light fill, and on a dark one */
    public static final int GREEN_ON_LIGHT = 0xFF1B8354, GREEN_ON_DARK = 0xFF54C08A;

    /** 1 when the value says on, 0 off, -1 when it is not an on/off value: by its first word. */
    public static int state(String value) {
        if (value == null) return -1;
        String w = value.substring(0, wordEnd(value));
        for (String s : ON) if (s.equals(w)) return 1;
        for (String s : OFF) if (s.equals(w)) return 0;
        return -1;
    }

    /** Where the first word ends: at the first space, «·» or «•». */
    public static int wordEnd(String value) {
        int i = 0;
        while (i < value.length() && " ·• ".indexOf(value.charAt(i)) < 0) i++;
        return i;
    }

    /** The green that reads on {@code bg} (an ARGB colour): the dark green on a light fill, the light green on a dark one. */
    public static int green(int bg) {
        int r = (bg >> 16) & 0xff, g = (bg >> 8) & 0xff, b = bg & 0xff;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b > 140 ? GREEN_ON_LIGHT : GREEN_ON_DARK;
    }
}
