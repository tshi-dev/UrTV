package sa.urtv.home;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 4.4: the card on the home while a dua or reminder plays (docs/mockups/audio-card.png): its name, a line under it, and «إيقاف», which has the
 * focus while it shows (OK stops it: AdhanKeys, as the adhan's bar). A glass card at the top corner, over nothing the person is using; it goes
 * when the dua ends, and the focus goes back where it was.
 */
final class DuaCard {
    final LinearLayout view;
    final TextView name, line, stop;
    private View before;

    DuaCard(Context c) {
        view = new LinearLayout(c);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(c, 16);
        view.setPadding(dp(c, 20), p, dp(c, 20), p);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xB81C1C24);
        bg.setCornerRadius(dp(c, 20));
        bg.setStroke(dp(c, 1), 0x38FFFFFF);
        view.setBackground(bg);
        view.setElevation(dp(c, 6));
        LinearLayout words = new LinearLayout(c);
        words.setOrientation(LinearLayout.VERTICAL);
        name = new TextView(c);
        name.setTextColor(0xFFFFFFFF);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        line = new TextView(c);
        line.setTextColor(0xFFD2CED6);
        line.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        line.setMaxLines(1);
        words.addView(name);
        words.addView(line);
        view.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        stop = new TextView(c);
        stop.setText(S.t("إيقاف"));
        stop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        stop.setGravity(Gravity.CENTER);
        stop.setPadding(dp(c, 22), dp(c, 9), dp(c, 22), dp(c, 9));
        stop.setFocusable(true);
        stop.setOnFocusChangeListener((v, f) -> paintStop(c, f));
        paintStop(c, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
        sp.setMarginStart(dp(c, 18));
        view.addView(stop, sp);
        view.setVisibility(View.GONE);
    }

    private void paintStop(Context c, boolean focused) {
        GradientDrawable b = new GradientDrawable();
        b.setCornerRadius(dp(c, 12));
        b.setColor(focused ? 0xFFFFFFFF : 0x33FFFFFF);
        stop.setBackground(b);
        stop.setTextColor(focused ? 0xFF16161A : 0xFFFFFFFF);
    }

    /** where it sits: the top corner, under the greeting line */
    FrameLayout.LayoutParams place(Context c) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(c, 420), -2, Gravity.TOP | Gravity.END);
        lp.topMargin = dp(c, 92);
        lp.setMarginEnd(dp(c, 48));
        return lp;
    }

    /** shown with this name (null: hidden); the focus goes to «إيقاف» and back */
    void show(String dua, View focused) {
        boolean was = view.getVisibility() == View.VISIBLE;
        if (dua == null) {
            if (!was) return;
            boolean had = stop.hasFocus();
            view.setVisibility(View.GONE);
            if (had && before != null && before.isAttachedToWindow()) before.requestFocus();
            before = null;
            return;
        }
        name.setText(dua.isEmpty() ? S.t("تذكير") : dua);
        line.setText(S.t("اختيار يوقفه، والتلاوة تعود بعده"));
        if (!was) {
            before = focused;
            view.setVisibility(View.VISIBLE);
            stop.requestFocus();
        }
    }

    boolean showing() { return view.getVisibility() == View.VISIBLE; }

    private static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
