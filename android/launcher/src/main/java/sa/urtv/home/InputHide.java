package sa.urtv.home;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 4.5 «المصادر»: the inputs the owner hid (an HDMI port nothing is plugged into, AV) leave the sources popup; they stay in the inputs screen
 * («أسماء المصادر»), dimmed, with «إظهار». Kept on this TV only, by the same key as the names (InputLabels.key: the port, so a CEC device that
 * comes and goes keeps it), one id per line in the home's preferences. The idea is tvOS Launcher's input manager (github.com/techgenius1/
 * tvOS-Launcher, Apache-2.0); this is our own code on our own names. No Android in here (tests/InputHideRun.java runs it).
 */
final class InputHide {
    private InputHide() { }

    /** an input id as the TV writes them (letters, digits and . _ / % : - $, as dock.sh input-name takes them), at most 200: anything else is never kept */
    static boolean validId(String id) { return id != null && id.length() <= 200 && id.matches("[A-Za-z0-9._/%:$-]+"); }

    static Set<String> parse(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null) return out;
        for (String l : text.split("\n")) if (validId(l.trim())) out.add(l.trim());
        return out;
    }

    static String serialize(Set<String> hidden) {
        StringBuilder b = new StringBuilder();
        for (String id : hidden) if (validId(id)) b.append(id).append('\n');
        return b.toString();
    }

    /** the set with {@code id} hidden or shown again; an invalid id changes nothing */
    static Set<String> with(Set<String> hidden, String id, boolean hide) {
        Set<String> out = new LinkedHashSet<>(hidden);
        if (!validId(id)) return out;
        if (hide) out.add(id); else out.remove(id);
        return out;
    }

    /** the positions of {@code keys} (the reported inputs' keys, in order) that the sources popup shows */
    static List<Integer> shown(List<String> keys, Set<String> hidden) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) if (!hidden.contains(keys.get(i))) out.add(i);
        return out;
    }
}
