package sa.urtv.screensaver;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;

/**
 * The screens' direction from «اللغة» (docs/ENGLISH.md): right-to-left for Arabic, the default, left-to-right for English. A screen that
 * hard-codes a side uses these instead, and each one gives with Arabic the very constant it replaces, so the Arabic screen stays exactly
 * what it was:
 *   Gravity.RIGHT → Dir.start()      Gravity.LEFT → Dir.end()      (never Gravity.START/END: those follow the view's own direction)
 *   View.LAYOUT_DIRECTION_RTL → Dir.layout()      View.TEXT_DIRECTION_RTL → Dir.text()
 *   KEYCODE_DPAD_LEFT as "forward" (the next item in reading order) → Dir.forwardKey(); KEYCODE_DPAD_RIGHT as "back" → Dir.backKey()
 */
final class Dir {
    private Dir() { }

    static boolean rtl() { return rtl(S.lang()); }
    static boolean rtl(String lang) { return !Tr.EN.equals(lang); }

    /** where a line of text starts: the right in Arabic */
    static int start() { return start(rtl()); }
    static int start(boolean rtl) { return rtl ? Gravity.RIGHT : Gravity.LEFT; }

    /** where it ends: the left in Arabic */
    static int end() { return end(rtl()); }
    static int end(boolean rtl) { return rtl ? Gravity.LEFT : Gravity.RIGHT; }

    static int layout() { return layout(rtl()); }
    static int layout(boolean rtl) { return rtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR; }

    static int text() { return text(rtl()); }
    static int text(boolean rtl) { return rtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR; }

    /** the D-pad key that moves to the next item in reading order: left in Arabic */
    static int forwardKey() { return forwardKey(rtl()); }
    static int forwardKey(boolean rtl) { return rtl ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT; }

    static int backKey() { return backKey(rtl()); }
    static int backKey(boolean rtl) { return rtl ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT; }

    /** a view laid out and its text written in the setting's direction */
    static void apply(View v) {
        v.setLayoutDirection(layout());
        v.setTextDirection(text());
    }
}
