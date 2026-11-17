package sa.urtv.home;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * 4.4 (the owner, on the compact screens: «ONE BOX for everything»): the one box of the setup, update, install, rescue and add-on screens. Always the same
 * size (Compact.BOX_W_DP x Compact.BOX_H_DP, never more than the 90 % safe area), in the middle of the screen; the title fixed at its top; what is under it
 * scrolls INSIDE the box, with a small scroll bar and a «⌄» under it while there is more. Content never changes the box's size.
 *
 * Read before focus: when the words are longer than the box, the box's reading area takes the focus first; DOWN scrolls the words, and only when they
 * have all been seen does DOWN move to the first button under them. When everything fits, the button has the focus at once.
 * No resources: the rescue app uses it too (android/rescue/build.gradle copies this file and Compact.java).
 */
public final class Box {
    public final FrameLayout page;
    public final LinearLayout box;
    public final TextView title;
    public final ReadScroll scroll;
    public final LinearLayout content;
    public final TextView more;

    private Box(Context c, Drawable boxBackground, int titleColor, int hintColor, Typeface titleFace, int direction) {
        float d = c.getResources().getDisplayMetrics().density;
        int sw = c.getResources().getDisplayMetrics().widthPixels, sh = c.getResources().getDisplayMetrics().heightPixels;
        page = new FrameLayout(c);
        int padX = Math.round(sw * 0.05f), padY = Math.round(sh * 0.05f);
        page.setPadding(padX, padY, padX, padY);
        box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(direction);
        box.setPadding(dp(d, 24), dp(d, 20), dp(d, 24), dp(d, 12));
        if (boxBackground != null) box.setBackground(boxBackground);
        page.addView(box, new FrameLayout.LayoutParams(width(c), height(c), Gravity.CENTER));
        title = new TextView(c);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, Compact.TITLE_SP);
        title.setTextColor(titleColor);
        if (titleFace != null) title.setTypeface(titleFace);
        title.setGravity(Gravity.START);
        title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.bottomMargin = dp(d, 12);
        box.addView(title, tp);
        scroll = new ReadScroll(c);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(false);
        scroll.setFillViewport(false);
        content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(direction);
        scroll.addView(content, new FrameLayout.LayoutParams(-1, -2));
        box.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        more = new TextView(c);
        more.setText("⌄");
        more.setTextSize(TypedValue.COMPLEX_UNIT_SP, Compact.SECONDARY_SP);
        more.setTextColor(hintColor);
        more.setGravity(Gravity.CENTER);
        more.setVisibility(View.INVISIBLE);
        box.addView(more, new LinearLayout.LayoutParams(-1, -2));
        scroll.more = more;
        ViewTreeObserver.OnScrollChangedListener s = scroll::hint;
        scroll.getViewTreeObserver().addOnScrollChangedListener(s);
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(scroll::hint);
    }

    public static Box make(Context c, Drawable boxBackground, int titleColor, int hintColor, Typeface titleFace, int direction) {
        return new Box(c, boxBackground, titleColor, hintColor, titleFace, direction);
    }

    /** the box's width: Compact.BOX_W_DP, never wider than the 90 % safe area */
    public static int width(Context c) {
        int sw = c.getResources().getDisplayMetrics().widthPixels;
        return Math.min(Math.round(Compact.BOX_W_DP * c.getResources().getDisplayMetrics().density), sw - 2 * Math.round(sw * 0.05f));
    }

    /** the box's height: Compact.BOX_H_DP, never taller than the 90 % safe area (on a 1080p TV at density 2 the screen is 540 dp: 486) */
    public static int height(Context c) {
        int sh = c.getResources().getDisplayMetrics().heightPixels;
        return Math.min(Math.round(Compact.BOX_H_DP * c.getResources().getDisplayMetrics().density), sh - 2 * Math.round(sh * 0.05f));
    }

    /** The first focus of a page: the reading area when the words are longer than the box (read before focus), else {@code button} at once. */
    public void focusFirst(final View button) {
        scroll.post(() -> {
            scroll.hint();
            if (scroll.overflows()) { scroll.scrollTo(0, 0); scroll.requestFocus(); }
            else if (button != null) button.requestFocus();
        });
    }

    private static int dp(float d, int v) { return Math.round(v * d); }

    /** The reading area: while it has the focus, DOWN scrolls the words; at their end DOWN moves to the first button under what is shown. */
    public static final class ReadScroll extends ScrollView {
        TextView more;

        ReadScroll(Context c) {
            super(c);
            setFocusable(true);
            setFocusableInTouchMode(false);
            setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        }

        /** the words are longer than the box */
        public boolean overflows() { return getChildCount() > 0 && getChildAt(0).getHeight() > getHeight() - getPaddingTop() - getPaddingBottom(); }

        void hint() { if (more != null) more.setVisibility(canScrollVertically(1) ? View.VISIBLE : View.INVISIBLE); }

        @Override public boolean dispatchKeyEvent(KeyEvent e) {
            if (isFocused() && e.getAction() == KeyEvent.ACTION_DOWN) {
                int k = e.getKeyCode();
                if (k == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (canScrollVertically(1)) { smoothScrollBy(0, Math.max(1, Math.round(getHeight() * 0.7f))); return true; }
                    View b = firstButtonInView();
                    if (b != null) { b.requestFocus(); return true; }
                    return true;   // nothing to go to: stay on the words
                }
                if (k == KeyEvent.KEYCODE_DPAD_UP) {
                    if (canScrollVertically(-1)) { smoothScrollBy(0, -Math.max(1, Math.round(getHeight() * 0.7f))); return true; }
                    return super.dispatchKeyEvent(e);
                }
            }
            return super.dispatchKeyEvent(e);
        }

        /** the first focusable view under the top of what is shown now (the button under the words) */
        View firstButtonInView() {
            if (getChildCount() == 0) return null;
            ArrayList<View> all = new ArrayList<>();
            getChildAt(0).addFocusables(all, View.FOCUS_DOWN);
            int top = getScrollY();
            View best = null;
            int bestY = Integer.MAX_VALUE;
            int[] here = new int[2], at = new int[2];
            getLocationInWindow(here);
            for (View v : all) {
                if (v == this || !v.isShown()) continue;
                v.getLocationInWindow(at);
                int y = at[1] - here[1] + top;
                if (y >= top && y < bestY) { bestY = y; best = v; }
            }
            return best;
        }
    }
}
