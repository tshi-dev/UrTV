package sa.urtv.home;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import sa.urtv.home.sports.KickoffPlan;

/**
 * 4.4 the kick-off notice in our own home's window (showIn: no permission, never focused), the top right, 8 seconds, then it slides away. Cream
 * text on dark glass, the two clubs' names on the flat fill (no colour square: the app keeps no crest). Over another app the add-on draws the same notice (OverlayNotice, ExtrasNotice): the home
 * holds no overlay permission (Play Protect).
 */
final class KickoffOverlay {
    private KickoffOverlay() { }

    static final long SHOW_MS = 8_000L;
    private static final int CREAM = 0xFFFFF8EC, GOLD = 0xFFF0D48E, SUB = 0xFFE5DCC8;

    /** on our own home: the same notice in the home's window (no permission), the top right, never focused; false when it could not be added */
    static boolean showIn(android.app.Activity a, KickoffPlan.Game g) {
        final android.view.ViewGroup root;
        try { root = (android.view.ViewGroup) a.getWindow().getDecorView(); } catch (RuntimeException e) { return false; }
        DisplayMetrics dm = a.getResources().getDisplayMetrics();
        final View box = view(a, g, dm);
        box.setFocusable(false);
        android.widget.FrameLayout.LayoutParams p = new android.widget.FrameLayout.LayoutParams((int) (dm.widthPixels * 0.36f),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Dir.start(true));
        p.rightMargin = (int) (dm.widthPixels * 0.025f);
        p.topMargin = (int) (dm.heightPixels * 0.045f);
        try { root.addView(box, p); } catch (RuntimeException e) { return false; }
        final float away = dm.widthPixels * 0.4f;
        box.setTranslationX(away);
        box.animate().translationX(0).setDuration(450).start();
        new Handler(Looper.getMainLooper()).postDelayed(() -> box.animate().translationX(away).setDuration(450).withEndAction(() -> {
            if (box.getParent() == root) root.removeView(box);
        }).start(), SHOW_MS);
        return true;
    }

    private static View view(Context c, KickoffPlan.Game g, DisplayMetrics dm) {
        float u = dm.heightPixels / 1080f;
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(Dir.layout());
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0C0E12);   // the UI review: a flat opaque fill, no edge (nothing behind reads)
        bg.setCornerRadius(28 * u);
        box.setBackground(bg);
        int pad = (int) (28 * u);
        box.setPadding(pad, (int) (22 * u), pad, (int) (24 * u));
        LinearLayout top = new LinearLayout(c);
        top.addView(text(c, S.t("بدأت المباراة"), GOLD, 24 * u, false), new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(text(c, g.lg, SUB, 22 * u, false));
        box.addView(top);
        LinearLayout mid = new LinearLayout(c);
        mid.setGravity(Gravity.CENTER);
        mid.setPadding(0, (int) (16 * u), 0, 0);
        mid.addView(text(c, "  " + g.h + "   ×   " + g.a + "  ", CREAM, 38 * u, true));
        box.addView(mid);
        return box;
    }

    private static TextView text(Context c, String s, int color, float px, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setSingleLine(true);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }
}
