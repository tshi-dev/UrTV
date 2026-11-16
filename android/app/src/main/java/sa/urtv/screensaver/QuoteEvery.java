package sa.urtv.screensaver;

/**
 * «تبديل الآية والحديث»: how often the verse-or-hadith card («آية أو حديث اليوم») changes. "day" (the default, as it always was: a verse one
 * day, a hadith the next), "3" or "10" (4.3.3): a new one every 3 or 10 minutes, a verse then a hadith. It goes to the page and the home's cards in the settings
 * fragment as {@code &versemin=3} or {@code &versemin=10}, only while chosen, so with the default the fragment (and the page) is exactly what it was. The settings
 * row and `optimize/saver.sh verse day|3|10` change the same setting. Plain Java on purpose: tests/QuoteEveryRun.java runs it on a JVM.
 */
final class QuoteEvery {
    private QuoteEvery() { }

    static final String DAY = "day", THREE = "3", TEN = "10";

    /** what the receiver accepts: day, 3 or 10, nothing else */
    static boolean valid(String v) { return DAY.equals(v) || THREE.equals(v) || TEN.equals(v); }

    /** a stored value as one of the three; anything unknown is the day, as always */
    static String read(String v) { return THREE.equals(v) ? THREE : TEN.equals(v) ? TEN : DAY; }

    /** What goes in the settings fragment: nothing for the day (the page is then as it was). */
    static String fragment(String v) { String r = read(v); return DAY.equals(r) ? "" : "&versemin=" + r; }

    /** the row's choices, the day (the default) first; the row's value is the one set */
    static final String[] CHOICES = {"كل يوم", "كل ٣ دقائق", "كل ١٠ دقائق"};

    static int choice(String v) { String r = read(v); return THREE.equals(r) ? 1 : TEN.equals(r) ? 2 : 0; }

    static String label(String v) { return CHOICES[choice(v)]; }

    /** the picker's choice i as the setting */
    static String fromChoice(int i) { return i == 1 ? THREE : i == 2 ? TEN : DAY; }
}
