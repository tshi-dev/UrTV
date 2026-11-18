package sa.urtv.home;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * 4.5 (docs/ui/PROPOSAL.md, approved): the one way the home changes its look while it is on the screen. A still picture of the screen as it is
 * covers it, the change is made behind it (layout, background, clock, the apps' way, the cards' page), and once the new state has drawn (the
 * cards' page too, at most {@link #WAIT_MS}) the picture fades out in {@link #FADE_MS} on Motion's curve. The person sees the old look, then
 * the new one, and nothing in between: no blank frame, no row rebuilt in front of them, no black box where the cards' page is made again.
 *
 * The owner's protections: a change that throws is undone (the saved look applied again, still behind the cover); «الوضع الخفيف» (Ram.low, as Motion.move)
 * cuts from the picture to the new look with no fade; a change asked while one is under way waits for it (the last one wins). When the home is not
 * on the screen (another app or the settings in front) there is nothing to cover: the change is made at once and the next frame shown is the new look.
 */
final class HomeLook {
    static final long FADE_MS = 250;
    /** the longest the cover waits for the new state (the cards' page loading on a weak TV); then it fades anyway */
    static final long WAIT_MS = 1500;
    /** the least it waits, so the change's own reads (the cards' url, read off the UI thread) have started */
    static final long MIN_MS = 120;

    interface Ready { boolean now(); }

    private final Activity activity;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private ImageView cover;
    private Bitmap shot;
    private Runnable queued;

    HomeLook(Activity activity) { this.activity = activity; }

    /** a change is under way (its cover is up) */
    boolean busy() { return cover != null; }

    /**
     * Makes the change behind a still picture of the screen. {@code apply} is the change; {@code undo} puts the saved look back if it throws;
     * {@code ready} says the new state has drawn. On a screen that is not shown, call {@code apply} directly instead.
     */
    void change(final Runnable apply, final Runnable undo, final Ready ready) {
        if (busy()) { queued = () -> change(apply, undo, ready); return; }
        final ViewGroup root = (ViewGroup) activity.getWindow().getDecorView();
        Bitmap b = snapshot(root);
        if (b == null) { run(apply, undo); return; }   // no picture (no memory for it): the change in place, as before 4.5
        shot = b;
        cover = new ImageView(activity);
        cover.setImageBitmap(b);
        cover.setScaleType(ImageView.ScaleType.FIT_XY);
        cover.setClickable(false);
        cover.setFocusable(false);
        cover.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(cover, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        run(apply, undo);
        final long start = SystemClock.uptimeMillis();
        ui.post(new Runnable() {
            @Override public void run() {
                long t = SystemClock.uptimeMillis() - start;
                boolean drawn = t >= MIN_MS && safe(ready);
                if (!drawn && t < WAIT_MS) { ui.postDelayed(this, 50); return; }
                AfterFrame.run(root, HomeLook.this::reveal);   // one more frame of the new state drawn under the cover first
            }
        });
    }

    private static boolean safe(Ready r) { try { return r == null || r.now(); } catch (RuntimeException e) { return true; } }

    private static void run(Runnable apply, Runnable undo) {
        try { apply.run(); }
        catch (RuntimeException e) {   // the owner: a failed change falls back to the saved look, with no blank frame (the cover is still up)
            android.util.Log.w("UrTVHome", "look change failed, the saved look again: " + e);
            try { if (undo != null) undo.run(); } catch (RuntimeException ignored) { }
        }
    }

    private void reveal() {
        final ImageView c = cover;
        if (c == null) return;
        if (Ram.low(activity)) { done(c); return; }   // «الوضع الخفيف» (Ram.low, as Motion.move): a cut, never a fade
        c.animate().alpha(0f).setDuration(FADE_MS).setInterpolator(Motion.ease()).withEndAction(() -> done(c)).start();
    }

    private void done(ImageView c) {
        ViewGroup p = (ViewGroup) c.getParent();
        if (p != null) p.removeView(c);
        c.setImageDrawable(null);
        if (shot != null) { shot.recycle(); shot = null; }
        cover = null;
        Runnable q = queued;
        queued = null;
        if (q != null) q.run();
    }

    /** the window as drawn now, at half size (a quarter of the memory; it is on screen for a fraction of a second); null when it cannot be made */
    private static Bitmap snapshot(View root) {
        int w = root.getWidth() / 2, h = root.getHeight() / 2;
        if (w <= 0 || h <= 0) return null;
        try {
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            c.scale(0.5f, 0.5f);
            root.draw(c);   // the views as they are (a WebView in a software canvas draws its last frame, or nothing: then the photo behind shows)
            return b;
        } catch (OutOfMemoryError | RuntimeException e) {
            return null;
        }
    }

    /** the screen was left (onStop) with a cover up: no fade nobody sees, the change is complete */
    void finishNow() {
        if (cover != null) { cover.animate().cancel(); done(cover); }
    }
}
