package sa.urtv.home.tilawah;

/**
 * 4.4 «مستوى صوت الأذان» (a user report: «صوت الأذان ثابت على ٢٥ ومرتفع، ولا أستطيع تغييره»): the adhan's level, for all the prayers and for each.
 *   TV (-1)    «مثل صوت التلفزيون»: the TV's volume is left as it is (a muted TV is still unmuted for the adhan and muted again after, as before)
 *   0..100     percent of the TV's maximum (was 10..100 in steps of 10; 30 by default, unchanged)
 * Each prayer's choice is one word in the "tilawah" preferences as pv_<prayer>: "" (the general level, the default), "tv", or "0".."100".
 * No Android in it: tests/adhan_level_test.py runs it.
 */
final class AdhanLevel {
    private AdhanLevel() { }

    static final int TV = -1, MIN = 0, MAX = 100, STEP = 5;

    static int clamp(int percent) { return Math.max(MIN, Math.min(MAX, percent)); }

    /** the general level: TV, or the percent */
    static int general(int percent, boolean tv) { return tv ? TV : clamp(percent); }

    static boolean valid(String choice) {
        if (choice == null) return false;
        if (choice.isEmpty() || choice.equals("tv")) return true;
        if (!choice.matches("[0-9]{1,3}")) return false;
        int n = Integer.parseInt(choice);
        return n >= MIN && n <= MAX;
    }

    /** a prayer's level: its own choice, or the general one */
    static int forPrayer(String choice, int general) {
        if (!valid(choice) || choice.isEmpty()) return general;
        return choice.equals("tv") ? TV : Integer.parseInt(choice);
    }

    /** the word kept for a level */
    static String choice(int level) { return level == TV ? "tv" : String.valueOf(clamp(level)); }

    /** left/right on the row, no wrapping: «مثل صوت التلفزيون», 0, 5, 10 … 100 */
    static int step(int level, int dir) {
        if (level == TV) return dir > 0 ? MIN : TV;
        int n = (clamp(level) / STEP) * STEP + (dir > 0 ? STEP : (level % STEP != 0 ? 0 : -STEP));
        if (n < MIN) return TV;
        return Math.min(MAX, n);
    }

    /** the TV's own volume step for a level (max: its highest step); -1 for TV: the volume is not set. Any level above 0 is at least step 1. */
    static int index(int level, int max) {
        if (level == TV) return -1;
        int p = clamp(level);
        if (p == 0) return 0;
        return Math.max(1, Math.min(max, Math.round(max * p / 100f)));
    }
}
