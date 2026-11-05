package sa.urtv.screensaver;

/**
 * «الخروج من تلفزيونك»: how Back leaves our home on a TV that keeps its own launcher (our home opened from its tile, Back at its top goes back
 * to the TV's home). "press" (the default, as it always was: one Back), "hold" (a long Back) or "three" (three Backs in a row). Only there:
 * while ours is the default home Back never leaves it anyway. The home app reads it from the settings fragment, {@code &backexit=hold} or
 * {@code &backexit=three}, sent only while chosen, so with the default the fragment (and the page) is exactly what it was; the home's own
 * BackExit does the keys. The settings row and `optimize/saver.sh backexit press|hold|three` change the same setting.
 * Plain Java on purpose: tests/HomeExitRun.java runs it on a JVM.
 */
final class HomeExit {
    private HomeExit() { }

    static final String PRESS = "press", HOLD = "hold", THREE = "three";
    private static final String[] VALUES = {PRESS, HOLD, THREE};

    /** what the receiver accepts: press, hold or three, nothing else */
    static boolean valid(String v) { return PRESS.equals(v) || HOLD.equals(v) || THREE.equals(v); }

    /** a stored value as one of the three; anything unknown is one press, as always */
    static String read(String v) { return HOLD.equals(v) || THREE.equals(v) ? v : PRESS; }

    /** What goes in the settings fragment: nothing for one press (the fragment is then as it was). */
    static String fragment(String v) { String k = read(v); return PRESS.equals(k) ? "" : "&backexit=" + k; }

    /** the row's three choices, one press (the default) first; the row's value is the one set */
    static final String[] CHOICES = {"ضغطة رجوع", "ضغطة رجوع مطوّلة", "ثلاث ضغطات رجوع"};

    static int choice(String v) { String k = read(v); for (int i = 0; i < VALUES.length; i++) if (VALUES[i].equals(k)) return i; return 0; }

    static String label(String v) { return CHOICES[choice(v)]; }

    /** the picker's choice i as the setting */
    static String fromChoice(int i) { return i >= 0 && i < VALUES.length ? VALUES[i] : PRESS; }
}
