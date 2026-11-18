package sa.urtv.home;

import java.util.List;

/**
 * Typing from the phone into ANOTHER app, through the accessibility service: which text field, what the field holds afterwards, and when it is
 * refused. No Android in it (tests/TypeIntoRun.java runs it); the add-on's ExtrasA11yService wraps the real screen in {@link Node}. The service is off until the
 * owner turns it on. It looks at the screen only at the moment a phone sends text, only to find the field, and nothing it sees is kept, logged
 * or sent back: the phone gets one of a few fixed words.
 */
final class TypeInto {
    static final int MAX_FIELD = 2000;   // characters: a field that already holds more is left alone

    private TypeInto() {}

    /** One node of the screen's tree, as much as the choice needs. */
    interface Node {
        boolean editable();
        boolean focused();
        boolean password();
        boolean visible();
        List<? extends Node> children();
    }

    enum Refusal { NONE, SERVICE_OFF, LOCKED, REFUSED, NO_FIELD, PASSWORD_FIELD }

    /**
     * Before the field is looked for: the service must be on; the app in front must be known (a window that does not say its package is not typed
     * into) and not one of our own (our screens take the text by their own path); and it must be one the home screen would open now: not locked
     * behind the PIN, in kids mode one of the allowed apps, and not the screen of the TV's settings or Wi-Fi while those are locked
     * (lockedSystemScreen): typing follows the same policy as opening it.
     */
    static Refusal allowed(boolean serviceOn, String pkg, boolean ours, boolean needsPin, boolean hiddenByKidsMode, boolean lockedSystemScreen) {
        if (!serviceOn) return Refusal.SERVICE_OFF;
        if (pkg == null || pkg.isEmpty() || ours) return Refusal.REFUSED;
        if (needsPin || hiddenByKidsMode || lockedSystemScreen) return Refusal.LOCKED;
        return Refusal.NONE;
    }

    /** The field to type into: the one with the input focus, else the first visible editable one in reading order. null when there is none. */
    static Node pick(Node root) {
        if (root == null) return null;
        Node focused = firstWhere(root, true);
        return focused != null ? focused : firstWhere(root, false);
    }

    private static Node firstWhere(Node n, boolean needFocus) {
        if (n.editable() && n.visible() && (!needFocus || n.focused())) return n;
        for (Node c : n.children()) {
            if (c == null) continue;
            Node found = firstWhere(c, needFocus);
            if (found != null) return found;
        }
        return null;
    }

    /** A password field is never typed into from the phone. */
    static Refusal fieldProblem(Node field) {
        if (field == null) return Refusal.NO_FIELD;
        return field.password() ? Refusal.PASSWORD_FIELD : Refusal.NONE;
    }

    /**
     * What the field holds after: the typed text alone ("replace", the default) or after what it already holds ("append"); null when the result
     * would be longer than MAX_FIELD. A field's hint, which some apps report as its text, is not content: pass null for an empty field.
     */
    static String compose(String current, String typed, boolean append) {
        String base = append && current != null ? current : "";
        String out = base + typed;
        return out.length() > MAX_FIELD ? null : out;
    }
}
