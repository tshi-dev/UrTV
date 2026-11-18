package sa.urtv.home;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 4.4 «فوق تطبيق آخر»: the short corner notice over another app (the kick-off alert, «تنبيه الإقامة»), drawn here because this add-on holds the TV's
 * «الظهور فوق التطبيقات» permission and the home does not (Play Protect). The home sends the words (ExtrasWords.NOTICE); the look is the home's own
 * notice: the top right in both languages, cream words on dark glass with a gold edge, 8 seconds, then it slides away. It takes no focus and no key
 * (NOT_FOCUSABLE, NOT_TOUCHABLE): what plays under it goes on. Nothing is read, kept or logged.
 */
final class ExtrasNotice {
    private ExtrasNotice() { }

    static final long SHOW_MS = 8_000L;
    private static final int CREAM = 0xFFFFF8EC, GOLD = 0xFFF0D48E, SUB = 0xFFE5DCC8;

    /** the TV lets this app draw over other apps */
    static boolean canDraw(Context c) {
        try { return Settings.canDrawOverlays(c); } catch (RuntimeException e) { return false; }
    }

    /** adds the notice (on the main thread, waited for at most 2 s by the provider's binder thread); false when it could not be shown */
    static boolean show(final Context c, final Bundle in) {
        if (in == null || !canDraw(c)) return false;
        final boolean[] ok = {false};
        final CountDownLatch done = new CountDownLatch(1);
        // 4.4 review: 0 = posted, 1 = the main thread has taken it, 2 = given up (the home then shows its toast): a late runnable must not draw too
        final java.util.concurrent.atomic.AtomicInteger state = new java.util.concurrent.atomic.AtomicInteger();
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!state.compareAndSet(0, 1)) return;
            try { ok[0] = add(c.getApplicationContext(), in); } catch (RuntimeException ignored) { } finally { done.countDown(); }
        });
        try {
            if (!done.await(2, TimeUnit.SECONDS)) {
                if (state.compareAndSet(0, 2)) return false;   // never started: it will not draw, the caller says it another way
                done.await(1, TimeUnit.SECONDS);   // already drawing: its answer is a moment away
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return ok[0];
    }

    private static boolean add(Context c, Bundle in) {
        final WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return false;
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        boolean rtl = in.getBoolean(ExtrasWords.RTL, true);
        final View box = view(c, in, dm, rtl);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams((int) (dm.widthPixels * 0.36f), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.RIGHT;   // the top right in both languages (the owner's corner, as the home's own notice)
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

    private static View view(Context c, Bundle in, DisplayMetrics dm, boolean rtl) {
        float u = dm.heightPixels / 1080f;
        String[] c1 = in.getStringArray(ExtrasWords.C1), c2 = in.getStringArray(ExtrasWords.C2);
        String sub = in.getString(ExtrasWords.SUB);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(rtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE60C0E12);
        bg.setCornerRadius(28 * u);
        bg.setStroke(Math.max(1, (int) (2 * u)), 0x99E9C46A);
        box.setBackground(bg);
        int pad = (int) (28 * u);
        box.setPadding(pad, (int) (22 * u), pad, (int) (24 * u));
        String head = in.getString(ExtrasWords.HEAD, ""), line = in.getString(ExtrasWords.LINE, "");
        if (c1 != null && c2 != null && c1.length > 1 && c2.length > 1) {   // the kick-off's two clubs
            LinearLayout top = new LinearLayout(c);
            top.addView(text(c, head, GOLD, 24 * u, false), new LinearLayout.LayoutParams(0, -2, 1));
            if (sub != null) top.addView(text(c, sub, SUB, 22 * u, false));
            box.addView(top);
            LinearLayout mid = new LinearLayout(c);
            mid.setGravity(Gravity.CENTER);
            mid.setPadding(0, (int) (16 * u), 0, 0);
            mid.addView(swatch(c, c1, u));
            mid.addView(text(c, line, CREAM, 38 * u, true));
            mid.addView(swatch(c, c2, u));
            box.addView(mid);
        } else {   // a head and one line (the iqama)
            box.addView(text(c, head, GOLD, 24 * u, false));
            TextView l = text(c, line, CREAM, 34 * u, true);
            l.setPadding(0, (int) (12 * u), 0, 0);
            box.addView(l);
        }
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

    /** the club's two colours, half and half, in a small square (no crest is bundled); a colour that does not parse is grey */
    private static View swatch(Context c, String[] col, float u) {
        int a = parse(col[0], 0xFF8A8F98), b = parse(col[1], 0xFF5A5F68);
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {a, a, b, b});
        d.setCornerRadius(5 * u);
        View v = new View(c);
        v.setBackground(d);
        int s = (int) (26 * u);
        v.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return v;
    }

    private static int parse(String s, int fallback) {
        try { return Color.parseColor(s); } catch (RuntimeException e) { return fallback; }
    }
}
