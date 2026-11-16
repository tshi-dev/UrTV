package sa.urtv.home;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import sa.urtv.home.sports.KickoffPlan;

/**
 * 4.4 the kick-off notice over another app (YouTube, Shahid…), with «فوق تطبيق آخر» and the TV's «الظهور فوق التطبيقات» permission: the
 * page's own notice drawn natively, the top right, 8 seconds, then it slides away. It takes no focus and no key (NOT_FOCUSABLE, NOT_TOUCHABLE):
 * what plays under it goes on. Cream text on dark glass, the two clubs' colours, no crest.
 */
final class KickoffOverlay {
    private KickoffOverlay() { }

    static final long SHOW_MS = 8_000L;
    private static final int CREAM = 0xFFFFF8EC, GOLD = 0xFFF0D48E, SUB = 0xFFE5DCC8;

    /** false when it could not be shown (no permission, the TV refused): the caller says it another way */
    static boolean show(Context c, KickoffPlan.Game g) {
        final WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return false;
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        final View box = view(c, g, dm);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams((int) (dm.widthPixels * 0.36f), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Dir.start(true);   // the top right in both languages (the owner's corner, as the mockup)
        p.x = (int) (dm.widthPixels * 0.025f);
        p.y = (int) (dm.heightPixels * 0.045f);
        try { wm.addView(box, p); } catch (RuntimeException e) { return false; }
        final float away = dm.widthPixels * 0.4f;
        box.setTranslationX(away);
        box.animate().translationX(0).setDuration(450).start();
        new Handler(Looper.getMainLooper()).postDelayed(() -> box.animate().translationX(away).setDuration(450).withEndAction(() -> {
            try { wm.removeView(box); } catch (RuntimeException ignored) { }
        }).start(), SHOW_MS);
        return true;
    }

    private static View view(Context c, KickoffPlan.Game g, DisplayMetrics dm) {
        float u = dm.heightPixels / 1080f;
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(Dir.layout());
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE60C0E12);
        bg.setCornerRadius(28 * u);
        bg.setStroke(Math.max(1, (int) (2 * u)), 0x99E9C46A);
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
        mid.addView(swatch(c, g.hc, u));
        mid.addView(text(c, "  " + g.h + "   ×   " + g.a + "  ", CREAM, 38 * u, true));
        mid.addView(swatch(c, g.ac, u));
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

    /** the club's two colours, half and half, in a small square (no crest is bundled) */
    private static View swatch(Context c, String[] col, float u) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {Color.parseColor(col[0]), Color.parseColor(col[0]), Color.parseColor(col[1]), Color.parseColor(col[1])});
        d.setCornerRadius(5 * u);
        View v = new View(c);
        v.setBackground(d);
        int s = (int) (26 * u);
        v.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return v;
    }
}
