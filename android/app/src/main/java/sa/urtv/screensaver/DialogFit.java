package sa.urtv.screensaver;

/**
 * The settings' lists with «حفظ» («الدوريات كاملة», «الفرق المتابعة», «التصنيفات», «اللغات», «رياضات أخرى», «الأسواق», «مسلسلات مختارة»):
 * on a TV a long list is taller than the screen, and its buttons were below it, out of reach of the remote (a user's report: no way to
 * save «الدوريات كاملة»). The list gets a height that leaves room for the title and the buttons, DOWN on its last row goes to «حفظ», and
 * Back saves too (SettingsActivity.Dlg.multi). Plain Java: tests/DialogFitRun.java runs it.
 */
final class DialogFit {
    private DialogFit() { }

    /** dp the dialog needs besides its list: the title (two lines), the buttons, the dialog's own padding and its margin on the screen */
    static final int RESERVED_DP = 230;
    /** the list never takes more than this share of the screen's height */
    static final float MAX_SHARE = 0.6f;
    /** dp: the least height a list keeps, about two rows, even on the smallest screen */
    static final int MIN_DP = 96;

    /** The most the list may take on a screen this tall (pixels; density = pixels per dp). */
    static int maxHeight(int screenH, float density) {
        if (density <= 0) density = 1;
        int byShare = Math.round(screenH * MAX_SHARE), byRoom = screenH - Math.round(RESERVED_DP * density);
        return Math.max(Math.round(MIN_DP * density), Math.min(byShare, byRoom));
    }

    /** The list's height: -2 (as tall as its rows) when they fit, else the most it may take (and it scrolls). content = its rows' height. */
    static int listHeight(int content, int screenH, float density) {
        int max = maxHeight(screenH, density);
        return content <= max ? -2 : max;
    }

    /** DOWN from this row goes to «حفظ»: the last row of a list that has rows (anything else is the list's own move) */
    static boolean downToSave(int selected, int count) {
        return count > 0 && selected == count - 1;
    }
}
