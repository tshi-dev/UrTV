package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * The start of the screensaver page, the same for the dream (ScreenDream) and for the app's own page (MainActivity): the window gets its content at once (the last picture of
 * the page, SaverSnapshot, or the ground colour), the first frame is drawn, and only AFTER it the WebView is created (0.8 to 2.7 s on a cold start, all on the main thread),
 * put under the picture; the picture fades when the page has painted. One log line each (tag UrTVSaver, «snapshot: ...») says what was found, when the first frame was
 * drawn, how long the view took and when the page was live.
 */
final class SaverStart {
    private SaverStart() { }

    /** what the start needs from the window that shows the page */
    interface Host {
        Context context();
        void setContent(View v);
        /** the page's view, made now (the host keeps it as its own `web`) */
        WebView createPage();
        /** the window is still there (a dream detached, an activity finished: nothing is created) */
        boolean alive();
    }

    static void begin(final Host host, final String url, final Runnable afterContent) {
        final long t0 = android.os.SystemClock.elapsedRealtime();
        final Context c = host.context();
        final FrameLayout box = new FrameLayout(c);
        box.setBackgroundColor(0xFF040E09);
        android.util.DisplayMetrics dm = c.getResources().getDisplayMetrics();
        Bitmap last = SaverSnapshot.load(c, url, dm.widthPixels, dm.heightPixels);
        final ImageView cover = new ImageView(c);
        cover.setScaleType(ImageView.ScaleType.FIT_XY);
        if (last != null) { cover.setImageBitmap(last); box.addView(cover, new FrameLayout.LayoutParams(-1, -1)); }
        android.util.Log.i("UrTVSaver", "snapshot: " + SaverSnapshot.why(c, url, dm.widthPixels, dm.heightPixels) + (last != null ? ", decoded " + last.getWidth() + "x" + last.getHeight() : "") + ", ready at " + (android.os.SystemClock.elapsedRealtime() - t0) + " ms");
        host.setContent(box);
        if (afterContent != null) afterContent.run();
        final boolean coverOn = last != null;
        box.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                box.getViewTreeObserver().removeOnPreDrawListener(this);
                android.util.Log.i("UrTVSaver", "snapshot: first frame (" + (coverOn ? "the picture" : "the ground colour") + ") at " + (android.os.SystemClock.elapsedRealtime() - t0) + " ms; the page's view is created next");
                box.post(() -> addPage(host, box, cover, coverOn, t0));   // after the frame, not before it
                return true;
            }
        });
    }

    private static void addPage(final Host host, final FrameLayout box, final ImageView cover, final boolean coverOn, final long t0) {
        if (!host.alive()) return;
        final long c0 = android.os.SystemClock.elapsedRealtime();
        final WebView page = host.createPage();
        if (page == null) return;
        box.addView(page, 0, new FrameLayout.LayoutParams(-1, -1));   // under the picture
        android.util.Log.i("UrTVSaver", "snapshot: the page's view created in " + (android.os.SystemClock.elapsedRealtime() - c0) + " ms");
        if (!coverOn) return;
        final long since = android.os.SystemClock.elapsedRealtime();
        cover.postDelayed(new Runnable() {
            @Override public void run() {
                boolean live = page.getAlpha() >= 1f || android.os.SystemClock.elapsedRealtime() - since >= SaverSnapshot.LIVE_WAIT_MS;
                if (!live) { cover.postDelayed(this, 100); return; }
                android.util.Log.i("UrTVSaver", "snapshot: the page is live at " + (android.os.SystemClock.elapsedRealtime() - t0) + " ms from the start; the picture fades");
                cover.animate().alpha(0f).setDuration(SaverSnapshot.FADE_MS).withEndAction(() -> box.removeView(cover)).start();
            }
        }, 100);
    }
}
