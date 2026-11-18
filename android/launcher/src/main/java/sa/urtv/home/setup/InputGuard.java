package sa.urtv.home.setup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * optimize/disable.sh's input guard, ported: before anything is turned off the TV is asked, in one round trip, which of the packages drive its HDMI,
 * AV or tuner inputs or are a keyboard (their own «requested permissions» hold TV_INPUT_HARDWARE, or they serve android.media.tv.TvInputService or
 * android.view.InputMethod). Those stay on, and so does a package whose answer cannot be read (NOCHK). A port whose input driver was disabled
 * notices the cable and stays black, and only a full restore and a cold boot bring the picture back (Clean Your TV issue #321,
 * github.com/Gathin23/cleanyourtv, MIT). The command is the script's, to the letter (tests/input_guard_test.py). Plain Java.
 */
final class InputGuard {
    private InputGuard() { }

    /** disable.sh's GUARD, with @PKGS@ for the names. */
    static final String TEMPLATE = "s=$(getprop ro.build.version.sdk); for p in @PKGS@; do d=$(dumpsys package $p 2>/dev/null); case \"$d\" in *\"hung system process\"*) echo \"HUNG $p\";; *\"requested permissions:\"*) printf \"%s\\n\" \"$d\" | sed -n -E \"/requested permissions:/,/(install|runtime) permissions:|User [0-9]+:/p\" | grep -q TV_INPUT_HARDWARE && echo \"INPUT $p\";; *\"grantedPermissions:\"*) printf \"%s\\n\" \"$d\" | sed -n -E \"/grantedPermissions:/,/(install|runtime) permissions:|User [0-9]+:/p\" | grep -q TV_INPUT_HARDWARE && echo \"INPUT $p\";; *\"Package [$p]\"*) [ \"${s:-0}\" -ge 23 ] 2>/dev/null || echo \"NOCHK $p\";; *\"Unable to find\"*) ;; *) echo \"NOCHK $p\";; esac; printf \"%s\\n\" \"$d\" | grep -qE \"android\\.media\\.tv\\.TvInputService|android\\.view\\.InputMethod\" && echo \"INPUT $p\"; done; echo END_OF_LIST";

    /**
     * disable.sh's HANDLERS (4.4): which apps answer android.settings.SETTINGS, the VIEW intent of a TV channel or input and the setup of the inputs.
     * They stay on whatever a list says: a vendor's Settings or Live TV app taken off leaves no way back to the inputs or the network (Clean Your
     * TV's handler guard, github.com/Gathin23/cleanyourtv, MIT). The command is the script's, to the letter (tests/input_guard_test.py).
     */
    static final String HANDLERS = "echo \"== sdk $(getprop ro.build.version.sdk)\"; echo \"== settings\"; cmd package query-activities --brief -a android.settings.SETTINGS 2>/dev/null; echo \"== tv\"; for d in content://android.media.tv/channel/1 content://android.media.tv/passthrough/1; do cmd package query-activities --brief -a android.intent.action.VIEW -d $d 2>/dev/null; done; cmd package query-activities --brief -a android.intent.action.VIEW -d content://android.media.tv/channel/1 -t vnd.android.cursor.item/channel 2>/dev/null; cmd package query-activities --brief -a android.media.tv.action.SETUP_INPUTS 2>/dev/null; echo \"== home\"; cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null; echo END_OF_LIST";

    static final String HANDLERS_LEFT = "تركنا شغّالاً ما يفتح إعدادات التلفزيون أو قنواته أو مداخله: ";
    static final String HANDLERS_SAID = "لم يقل التلفزيون أي التطبيقات تفتح إعداداته وقنواته، فما أوقفنا شيئاً. جرّب مرة ثانية.";

    /**
     * The packages that answer those intents (the «== settings» and «== tv» parts; «== home» only proves the TV answered); null when the end
     * marker is missing or the TV named neither a Settings app nor a Home app (it did not say: nothing is turned off).
     */
    static Set<String> handlers(String text) {
        if (text == null) return null;
        boolean end = false, any = false;
        String section = "";
        Set<String> out = new LinkedHashSet<>();
        for (String raw : text.replace("\r", "").split("\n")) {
            String l = raw.trim();
            if (l.equals("END_OF_LIST")) { end = true; section = ""; }
            else if (l.startsWith("== ")) section = l.substring(3);
            else if (l.matches("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+")) {
                String pkg = l.substring(0, l.indexOf('/'));
                if (section.equals("settings") || section.equals("home")) any = true;
                if ((section.equals("settings") || section.equals("tv")) && !pkg.startsWith("sa.urtv.")) out.add(pkg);
            }
        }
        return end && any ? out : null;
    }

    /** The command for these packages (bare package names: the callers check them). */
    static String command(List<String> pkgs) { return TEMPLATE.replace("@PKGS@", String.join(" ", pkgs)); }

    /** The answer, read: «held» (an input or a keyboard), «unread» (left on: the answer cannot be read), «hung» (dumps are off). */
    static final class Answer {
        final Set<String> held = new LinkedHashSet<>(), unread = new LinkedHashSet<>();
        boolean hung;

        /** Every package that stays on. */
        Set<String> left() {
            Set<String> s = new LinkedHashSet<>(held);
            s.addAll(unread);
            return s;
        }
    }

    /** null when the end marker is missing (the TV did not say: nothing is turned off then). */
    static Answer parse(String text) {
        if (text == null) return null;
        boolean end = false;
        Answer a = new Answer();
        for (String l : text.replace("\r", "").split("\n")) {
            if (l.equals("END_OF_LIST")) end = true;
            else if (l.startsWith("HUNG ")) a.hung = true;
            else if (l.matches("INPUT [A-Za-z0-9_.]+")) a.held.add(l.substring(6));
            else if (l.matches("NOCHK [A-Za-z0-9_.]+")) a.unread.add(l.substring(6));
        }
        return end ? a : null;
    }

    /** The packages without those that stay on. */
    static List<String> without(List<String> pkgs, Set<String> left) {
        List<String> out = new ArrayList<>();
        for (String p : pkgs) if (!left.contains(p)) out.add(p);
        return out;
    }
}
