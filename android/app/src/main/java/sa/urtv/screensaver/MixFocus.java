package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Window;
import android.view.WindowManager;

/**
 * 4.4 beta 2, the owner's test of build 525 («أشكال جاهزة» → «تركيبات مقترحة», FAIL on usability): every card had the same full fill, so neither the
 * focused one nor the one on now could be told apart, and the busy settings page showed through. A choice card is a quiet panel at rest and the
 * look's focus fill with a white 3 dp ring when focused; the one on now says «الحالي» in green; the picker sits over a deep dim.
 */
final class MixFocus {
    private MixFocus() { }

    static final int RING = 0xFFFFFFFF, NOW = 0xFF5FD38A, NOW_ON_FOCUS = 0xFF1A7A3C;
    /** how much of the page behind the picker is hidden (0 none, 1 black) */
    static final float DIM = 0.85f;

    static Drawable card(Context c, GlassStyle.Look l, float radiusDp) {
        StateListDrawable s = new StateListDrawable();
        GradientDrawable on = GlassPaint.focused(c, l, radiusDp);
        on.setStroke(Math.max(2, Math.round(3 * c.getResources().getDisplayMetrics().density)), RING);
        s.addState(new int[] {android.R.attr.state_focused}, on);
        s.addState(new int[] {}, GlassPaint.panel(c, l, radiusDp));
        return s;
    }

    static void calm(Window w) {
        if (w == null) return;
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        w.setDimAmount(DIM);
    }
}
