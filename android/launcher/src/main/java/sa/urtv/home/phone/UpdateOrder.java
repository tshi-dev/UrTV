package sa.urtv.home.phone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** «تحديث تلفزيوني», the order: the screensaver app first, the launcher last (its process dies when it is replaced). Anything else is not ours and is dropped. */
public final class UpdateOrder {
    public static final String SAVER = "sa.urtv.screensaver", LAUNCHER = "sa.urtv.home";
    /** 4.3.3 «إضافات تلفزيونك»: updated with the others where it is installed, before the launcher */
    public static final String EXTRAS = "sa.urtv.extras";

    private UpdateOrder() { }

    public static List<String> order(Collection<String> packages) {
        List<String> out = new ArrayList<>();
        if (packages == null) return out;
        if (packages.contains(SAVER)) out.add(SAVER);
        if (packages.contains(EXTRAS)) out.add(EXTRAS);
        if (packages.contains(LAUNCHER)) out.add(LAUNCHER);
        return out;
    }

    /** The step is the launcher's: the process that runs the update is replaced by it. */
    public static boolean replacesUs(String pkg) { return LAUNCHER.equals(pkg); }

    /** The step's name on the screen. */
    public static String label(String step) {
        if (SAVER.equals(step)) return "تحديث تطبيق الحافظة";
        if (LAUNCHER.equals(step)) return "تحديث الواجهة الرئيسية";
        if (EXTRAS.equals(step)) return "تحديث إضافات تلفزيونك";
        return "التحقق من الملفات";
    }
}
