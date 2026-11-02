package sa.urtv.screensaver;

/**
 * «الآية والحديث»: what the verse-or-hadith card («آية أو حديث اليوم») shows. "both" (the default, as it always was: a verse one day, a hadith
 * the next; with «تبديل الآية والحديث» every 3 minutes, a verse then a hadith), "verse" (verses only) or "hadith" (hadith only). It goes to the
 * page and the home's cards in the settings fragment as {@code &versekind=verse} or {@code &versekind=hadith}, only while chosen, so with the
 * default the fragment (and the page) is exactly what it was. The settings row and `optimize/saver.sh versekind both|verse|hadith` change the
 * same setting. Plain Java on purpose: tests/VerseKindRun.java runs it on a JVM.
 */
final class VerseKind {
    private VerseKind() { }

    static final String BOTH = "both", VERSE = "verse", HADITH = "hadith";
    private static final String[] VALUES = {BOTH, VERSE, HADITH};

    /** what the receiver accepts: both, verse or hadith, nothing else */
    static boolean valid(String v) { return BOTH.equals(v) || VERSE.equals(v) || HADITH.equals(v); }

    /** a stored value as one of the three; anything unknown is both, as always */
    static String read(String v) { return VERSE.equals(v) || HADITH.equals(v) ? v : BOTH; }

    /** What goes in the settings fragment: nothing for both (the page is then as it was). */
    static String fragment(String v) { String k = read(v); return BOTH.equals(k) ? "" : "&versekind=" + k; }

    /** the row's three choices, both (the default) first; the row's value is the one set */
    static final String[] CHOICES = {"آية وحديث", "آية فقط", "حديث فقط"};

    static int choice(String v) { String k = read(v); for (int i = 0; i < VALUES.length; i++) if (VALUES[i].equals(k)) return i; return 0; }

    static String label(String v) { return CHOICES[choice(v)]; }

    /** the picker's choice i as the setting */
    static String fromChoice(int i) { return i >= 0 && i < VALUES.length ? VALUES[i] : BOTH; }
}
