package sa.urtv.home;

import android.content.Context;
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

/**
 * 4.4 a short notice over another app with the kick-off notice's look and rules (KickoffOverlay): the top right, cream words on dark glass with a
 * gold edge, 8 seconds, no focus and no key. Used by «تنبيه الإقامة» (tilawah.IqamaReceiver), which judges where it may show (KickoffPlan.where).
 */
public final class NoticeOverlay {
    private NoticeOverlay() { }

    private static final int CREAM = 0xFFFFF8EC, GOLD = 0xFFF0D48E;

    /** kids mode is on (the notice stays on the home then: KickoffPlan.where) */
    public static boolean kids(Context c) {
        try { return Parental.state(c).kids; } catch (RuntimeException e) { return true; }   // not known: as kids mode, nothing over another app
    }

    /** false when it could not be shown (no permission, the TV refused): the caller says it another way */
    public static boolean show(Context c, String head, String line) {
        final WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return false;
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
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
        box.addView(text(c, head, GOLD, 24 * u, false));
        TextView l = text(c, line, CREAM, 34 * u, true);
        l.setPadding(0, (int) (12 * u), 0, 0);
        box.addView(l);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams((int) (dm.widthPixels * 0.36f), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Dir.start(true);
        p.x = (int) (dm.widthPixels * 0.025f);
        p.y = (int) (dm.heightPixels * 0.045f);
        try { wm.addView(box, p); } catch (RuntimeException e) { return false; }
        final float away = dm.widthPixels * 0.4f;
        box.setTranslationX(away);
        box.animate().translationX(0).setDuration(450).start();
        new Handler(Looper.getMainLooper()).postDelayed(() -> box.animate().translationX(away).setDuration(450).withEndAction(() -> {
            try { wm.removeView(box); } catch (RuntimeException ignored) { }
        }).start(), KickoffOverlay.SHOW_MS);
        return true;
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
