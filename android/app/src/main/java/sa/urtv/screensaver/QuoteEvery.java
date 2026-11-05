package sa.urtv.screensaver;

/**
 * «تبديل الآية والحديث»: how often the verse-or-hadith card («آية أو حديث اليوم») changes. "day" (the default, as it always was: a verse one
 * day, a hadith the next) or "3": a new one every 3 minutes, a verse then a hadith. It goes to the page and the home's cards in the settings
 * fragment as {@code &versemin=3}, only while chosen, so with the default the fragment (and the page) is exactly what it was. The settings
 * row and `optimize/saver.sh verse day|3` change the same setting. Plain Java on purpose: tests/QuoteEveryRun.java runs it on a JVM.
 */
final class QuoteEvery {
    private QuoteEvery() { }

    static final String DAY = "day", THREE = "3";

    /** what the receiver accepts: day or 3, nothing else */
    static boolean valid(String v) { return DAY.equals(v) || THREE.equals(v); }

    /** a stored value as one of the two; anything unknown is the day, as always */
    static String read(String v) { return THREE.equals(v) ? THREE : DAY; }

    /** What goes in the settings fragment: nothing for the day (the page is then as it was). */
    static String fragment(String v) { return THREE.equals(read(v)) ? "&versemin=3" : ""; }

    /** the row's two choices, the day (the default) first; the row's value is the one set */
    static final String[] CHOICES = {"كل يوم", "كل ٣ دقائق"};

    static int choice(String v) { return THREE.equals(read(v)) ? 1 : 0; }

    static String label(String v) { return CHOICES[choice(v)]; }

    /** the picker's choice i as the setting */
    static String fromChoice(int i) { return i == 1 ? THREE : DAY; }
}
