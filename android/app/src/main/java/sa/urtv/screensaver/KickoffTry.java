package sa.urtv.screensaver;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 4.4 «جرّب التنبيه»: the kick-off notice now, with two sample clubs, in the settings' own window (no permission: it is this screen's view),
 * the same look as the home's (KickoffOverlay, spKick): the top right, cream words on dark glass with a gold edge, 8 s; and the launcher's
 * whistle once when «صوت التنبيه» is on (NowPlayingBridge.kickoffWhistle; never over the adhan). Never focused: the remote stays on the rows.
 */
final class KickoffTry {
    private KickoffTry() { }

    static final long SHOW_MS = 8000;
    private static final int CREAM = 0xFFFFF8EC, GOLD = 0xFFF0D48E, SUB = 0xFFE5DCC8;
    private static View shown;

    static void show(Activity a, boolean sound) {
        final ViewGroup root = (ViewGroup) a.getWindow().getDecorView();
        if (shown != null && shown.getParent() instanceof ViewGroup) ((ViewGroup) shown.getParent()).removeView(shown);
        DisplayMetrics dm = a.getResources().getDisplayMetrics();
        final View box = view(a, dm);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Dir.start(true));   // the top right in both languages (the owner's corner, as the home's)
        p.rightMargin = (int) (dm.widthPixels * 0.025f);
        p.topMargin = (int) (dm.heightPixels * 0.045f);
        root.addView(box, p);
        shown = box;
        final float away = dm.widthPixels * 0.4f;
        box.setTranslationX(away);
        box.animate().translationX(0).setDuration(450).start();
        if (sound) { final android.content.Context app = a.getApplicationContext(); new Thread(() -> NowPlayingBridge.kickoffWhistle(app), "kickoff-whistle").start(); }   // 4.4 QA-12: the call into the launcher is not made on the press
        new Handler(Looper.getMainLooper()).postDelayed(() -> box.animate().translationX(away).setDuration(450).withEndAction(() -> {
            if (box.getParent() == root) root.removeView(box);
            if (shown == box) shown = null;
        }).start(), SHOW_MS);
    }

    private static View view(Activity a, DisplayMetrics dm) {
        float u = dm.heightPixels / 1080f;
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(Dir.layout());
        box.setFocusable(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0C0E12);   // the UI review: a flat opaque fill, no edge (nothing behind reads)
        bg.setCornerRadius(18 * u);
        box.setBackground(bg);
        int pad = Math.round(22 * u);
        box.setPadding(pad, Math.round(16 * u), pad, Math.round(18 * u));
        LinearLayout top = new LinearLayout(a);
        top.setLayoutDirection(Dir.layout());
        top.addView(text(a, S.t("بدأت المباراة"), GOLD, 24 * u, false), new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(text(a, S.t("دوري روشن السعودي"), SUB, 22 * u, false));
        box.addView(top);
        box.addView(text(a, "  " + (S.en() ? "Al Hilal" : "الهلال") + "   ×   " + (S.en() ? "Al Nassr" : "النصر") + "  ", CREAM, 38 * u, true));   // sample clubs: names, not words to translate
        View bar = new View(a);
        bar.setBackgroundColor(0x59FFFFFF);   // the timer: neutral white at 35 %, square ends, 3 px at 1080 (2 at 720)
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, Math.max(2, Math.round(3 * u)));
        bp.topMargin = Math.round(12 * u);
        box.addView(bar, bp);
        bar.post(() -> { bar.setPivotX(Dir.rtl() ? 0 : bar.getWidth()); bar.animate().scaleX(0).setDuration(SHOW_MS).start(); });
        return box;
    }

    private static TextView text(Activity a, String s, int color, float px, boolean bold) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px);
        t.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        t.setTextDirection(Dir.text());
        t.setFocusable(false);
        return t;
    }
}
