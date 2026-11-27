package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/**
 * An input (HDMI 1, the receiver…) as a favourite: the saved favourites list (Dock's "dock", comma separated) holds it as "@input:" and the
 * input's id (InputLabels.key), beside the apps' packages. The owner adds or removes it from «أسماء المصادر» (InputNamesActivity); the home
 * draws it as a tile with the input's name (the one given there) and OK switches to that input, as its row in «المصادر» does. An input the
 * TV no longer reports draws nothing and keeps its place, like an app that is not installed for now.
 *
 * No Android in here: tests/InputFavRun.java runs it.
 */
final class InputFav {
    static final String PREFIX = "@input:";

    private InputFav() {}

    static boolean isInput(String entry) {
        return entry != null && entry.startsWith(PREFIX) && entry.length() > PREFIX.length();
    }

    static String token(String inputId) { return PREFIX + inputId; }

    static String idOf(String entry) { return isInput(entry) ? entry.substring(PREFIX.length()) : null; }

    /** The input's id may hold '/' and '.', never the list's ',' (a TV input id is a component name and a hardware id). */
    static boolean usable(String inputId) {
        return inputId != null && !inputId.isEmpty() && inputId.indexOf(',') < 0 && inputId.length() <= 300;
    }

    static boolean has(List<String> dock, String inputId) {
        return dock != null && dock.contains(token(inputId));
    }

    /**
     * The favourites with this input added at the end (or removed when it is there). Adding past {@code max} entries changes nothing: the
     * caller says the row is full, as «ترتيب المفضلة» does.
     */
    static List<String> toggle(List<String> dock, String inputId, int max) {
        List<String> out = new ArrayList<>(dock == null ? new ArrayList<String>() : dock);
        if (!usable(inputId)) return out;
        String t = token(inputId);
        if (out.remove(t)) return out;
        if (out.size() >= max) return out;
        out.add(t);
        return out;
    }

    /**
     * The row an editor of apps saves: its apps, then the inputs the saved list holds that it did not show (an editor of apps lists only
     * apps; saving its row alone dropped the HDMI favourite, review F100). Never past {@code max}: the apps come first.
     */
    static List<String> keepInputs(List<String> saved, List<String> row, int max) {
        List<String> out = new ArrayList<>(row);
        if (saved == null) return out;
        for (String e : saved) if (isInput(e) && !out.contains(e) && out.size() < max) out.add(e);
        return out;
    }
}
