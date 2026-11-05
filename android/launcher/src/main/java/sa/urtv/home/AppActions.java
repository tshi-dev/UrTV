package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/**
 * The row of small buttons under the app's long-press menu (a user's request): «تفاصيل التطبيق» opens Android's own app-details screen
 * for that app, «حذف التطبيق» hands it to Android's own uninstall (the system asks to confirm; nothing is removed by us). No Android in
 * here (tests/AppActionsRun.java runs it); MainActivity makes the buttons and opens them through the family lock.
 *
 * There is no «إيقاف إجباري»: a normal app cannot force-stop another one, and Android TV's app-details screen takes no extra that puts
 * the focus on its own «فرض الإيقاف», so the button would only have opened the same screen as «تفاصيل التطبيق» under another name.
 */
final class AppActions {
    private AppActions() {}

    static final String DETAILS = S.n("تفاصيل التطبيق"), DELETE = S.n("حذف التطبيق");
    /** the two buttons by what they do */
    static final int OPEN_DETAILS = 1, OPEN_DELETE = 2;
    /** our own two apps: the home and the screensaver are never offered for removal from the home itself */
    static final String[] OURS = {"sa.urtv.home", "sa.urtv.screensaver"};

    /**
     * The buttons for one app, right to left. Kids mode edits nothing (the menu's «تعديل…» rows are not there either), so it has none.
     * system: the app is part of the TV (ApplicationInfo.FLAG_SYSTEM, or an update of one: removing that only takes the update off).
     */
    static List<Integer> buttons(String pkg, boolean system, boolean kids) {
        List<Integer> out = new ArrayList<>();
        if (kids || pkg == null || pkg.isEmpty()) return out;
        out.add(OPEN_DETAILS);
        if (deletable(pkg, system)) out.add(OPEN_DELETE);
        return out;
    }

    /** «حذف التطبيق» only for an app the person installed: not a system app, not ours */
    static boolean deletable(String pkg, boolean system) {
        if (system || pkg == null || pkg.isEmpty()) return false;
        for (String o : OURS) if (o.equals(pkg)) return false;
        return true;
    }
}
