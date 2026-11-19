package sa.urtv.home;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * 4.4 beta 3 (the UI session): the home's own notice, the same as the screensaver settings' (SettingsActivity.notice), never a Toast (a pill on some TVs,
 * or nothing): a rectangle with an 8 dp corner at the bottom centre, 36 dp up, 0xF02B2E36 under 0xFFF1EBDD text, Thmanyah 16 sp, padding 22/12 dp, up to two
 * lines (no ellipsis) at most 60% of the screen wide; not focusable, read once by accessibility; a 200 ms fade in (none on a weak TV), gone after
 * {@link #SHOW_MS}, then taken off the root. The caller shows it on the UI thread after the home's first frame, never over the adhan or a kick-off notice.
 */
final class HomeNotice {
    private HomeNotice() { }

    static final long SHOW_MS = 6_000, FADE_MS = 200;

    /** on the UI thread; false when it could not be added (the caller tries at the next resume) */
    static boolean show(FrameLayout root, String text, Typeface font, boolean low) {
        if (root == null || text == null || text.isEmpty()) return false;
        float d = root.getResources().getDisplayMetrics().density;
        final TextView v = new TextView(root.getContext());
        v.setText(text);
        v.setMaxLines(2);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        if (font != null) v.setTypeface(font);
        v.setTextColor(0xFFF1EBDD);
        v.setGravity(Gravity.CENTER);
        v.setPadding(Math.round(22 * d), Math.round(12 * d), Math.round(22 * d), Math.round(12 * d));
        v.setMaxWidth(Math.round(root.getResources().getDisplayMetrics().widthPixels * 0.6f));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(8 * d);   // a rectangle, never a pill
        bg.setColor(0xF02B2E36);
        v.setBackground(bg);
        v.setFocusable(false);
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = Math.round(36 * d);
        try { root.addView(v, lp); } catch (RuntimeException e) { return false; }
        v.bringToFront();
        if (low) v.setAlpha(1f);
        else { v.setAlpha(0f); v.animate().alpha(1f).setDuration(FADE_MS).start(); }
        v.announceForAccessibility(text);   // once
        v.postDelayed(() -> { if (v.getParent() == root) root.removeView(v); }, SHOW_MS);
        return true;
    }
}
