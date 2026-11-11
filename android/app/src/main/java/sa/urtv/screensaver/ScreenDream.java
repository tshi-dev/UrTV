package sa.urtv.screensaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.dreams.DreamService;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** The system screensaver: starts on the TV's own idle timer, any key wakes it. */
public class ScreenDream extends DreamService {
    private WebView web;
    private final RenderGuard guard = new RenderGuard();
    private boolean attached;
    private String shownUrl;   // the address the page was opened with (S6)
    private final AdhanKeys adhanKeys = new AdhanKeys();
    private final ReminderKeys reminderKeys = new ReminderKeys();
    /** settings were saved while this dream is on (an agent's `saver.sh city`): start the page over with them, not at the next dream */
    private final BroadcastReceiver changed = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (!attached || web == null) return;
            // S6 (P4): the page is built again only when what it is opened with changed; a setting that does not reach it (the phone's, the home's) left the dream as it was
            if (new Prefs(c).screenUrl(c, false).equals(shownUrl)) return;
            WebView old = web;
            web = null;
            ScreenView.release(old);
            show(false);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (NowPlayingBridge.setupWaiting(this)) {   // «إعداد تلفزيوني» waits for «السماح بتصحيح USB؟»: never cover it (it would stay stuck)
            android.util.Log.i("UrTVSaver", "not started: the setup is waiting for the debugging prompt");
            finish();
            return;
        }
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        attached = true;
        Showing.enter();
        show(true);
        InternalReceivers.register(this, changed, new IntentFilter(Prefs.CHANGED));
    }

    /**
     * Any key wakes the dream and the adhan, which is the launcher's, plays on. While it plays the bar says «اختيار: إيقاف الأذان»: OK
     * stops it and is consumed (the dream stays), and with «إيقاف الأذان بأي زر» any key does, once. Home and Power are Android's.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
        org.json.JSONObject adhan = down ? NowPlayingBridge.adhanState(this) : null;   // asked on a press only; a release belongs to the one before
        boolean live = adhan != null && adhan.optBoolean("live");
        int d = adhanKeys.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), live, new Prefs(this).adhanBar(), true, live && adhan.optBoolean("any"));
        if (d == AdhanKeys.STOP) NowPlayingBridge.stopAdhan(this);
        if (d != AdhanKeys.PASS) return true;
        int r = reminderKeys.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), NowPlayingBridge.reminderCard());
        if (r != ReminderKeys.PASS) {   // a reminder's card is up: the first press is the card's, the screensaver stays
            if (r != ReminderKeys.EAT && web != null)
                web.evaluateJavascript("window.reminderKey&&reminderKey('" + (r == ReminderKeys.OK ? "ok" : r == ReminderKeys.MOVE ? "move" : "away") + "')", null);
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    /**
     * While «إعداد تلفزيوني» runs a setup the dream steps aside at once: a USB-debugging prompt sent while the screensaver showed stayed pending until
     * the TV restarted (tester's diagnosis, 30 Oct), and a TCL ignores the setup screen's keep-the-screen-on flag. wakeUp (Android 10+) brings the
     * screen back to what was under the dream (the setup screen), not to sleep; older TVs end the dream.
     */
    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (!NowPlayingBridge.setupRunning(this)) return;
        if (android.os.Build.VERSION.SDK_INT >= 29) wakeUp(); else finish();
    }

    private final android.os.Handler snapHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable snapTick = new Runnable() {
        @Override public void run() {
            // not while a reminder's card is up (it would come back with the next start's first frame)
            if (attached && web != null && !NowPlayingBridge.reminderCard()) SaverSnapshot.capture(ScreenDream.this, getWindow(), shownUrl, web);
            if (attached) snapHandler.postDelayed(this, SaverSnapshot.EVERY_MS);
        }
    };

    /**
     * start: the dream starting (counted for the first-run note), not a renderer's re-creation.
     * The first frame is drawn BEFORE the WebView exists: creating Chromium's view takes 0.8 to 2.7 s on a cold start (the tester's TCL), and while it ran on the main
     * thread nothing was drawn, so the saved picture never showed. Now the window shows the last picture (or the ground colour) at once, the WebView is created right after
     * that first frame was drawn, goes under the picture, and the picture fades when the page has painted. One line of the log (tag UrTVSaver) says what happened.
     */
    private void show(boolean start) {
        final long t0 = android.os.SystemClock.elapsedRealtime();
        shownUrl = new Prefs(this).screenUrl(this, false);
        if (getWindow() != null) getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF040E09));   // the page's ground, under the view until it has painted
        final FrameLayout box = new FrameLayout(this);
        box.setBackgroundColor(0xFF040E09);
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        android.graphics.Bitmap last = SaverSnapshot.load(this, shownUrl, dm.widthPixels, dm.heightPixels);
        final ImageView cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.FIT_XY);
        if (last != null) { cover.setImageBitmap(last); box.addView(cover, new FrameLayout.LayoutParams(-1, -1)); }
        android.util.Log.i("UrTVSaver", "snapshot: " + SaverSnapshot.why(this, shownUrl, dm.widthPixels, dm.heightPixels) + (last != null ? ", decoded " + last.getWidth() + "x" + last.getHeight() : "") + ", ready at " + (android.os.SystemClock.elapsedRealtime() - t0) + " ms");
        setContentView(box);
        final boolean coverOn = last != null;
        box.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                box.getViewTreeObserver().removeOnPreDrawListener(this);
                android.util.Log.i("UrTVSaver", "snapshot: first frame (" + (coverOn ? "the picture" : "the ground colour") + ") at " + (android.os.SystemClock.elapsedRealtime() - t0) + " ms; the page's view is created next");
                box.post(() -> addPage(box, cover, coverOn, t0, start));   // after the frame, not before it
                return true;
            }
        });
        snapHandler.removeCallbacks(snapTick);
        snapHandler.postDelayed(snapTick, SaverSnapshot.FIRST_MS);   // the picture for the next start, taken from the window while the page shows
    }

    private void addPage(final FrameLayout box, final ImageView cover, final boolean coverOn, final long t0, boolean start) {
        if (!attached) return;
        final long c0 = android.os.SystemClock.elapsedRealtime();
        web = ScreenView.create(this, this::gone, start);
        final WebView page = web;
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

    /** The page's renderer died: the dead view is thrown away; a new one at most twice in ten minutes, else the dream ends (the system starts it again later). */
    private void gone(WebView dead) {
        guard.gone(android.os.SystemClock.elapsedRealtime());
        ScreenView.release(dead);
        if (dead != web) return;
        web = null;
        if (attached && guard.mayCreate(android.os.SystemClock.elapsedRealtime())) show(false);
        else finish();
    }

    @Override
    public void onDetachedFromWindow() {
        NowPlayingBridge.clearCard();
        if (attached) Showing.leave();
        attached = false;
        try { unregisterReceiver(changed); } catch (IllegalArgumentException ignored) { }
        snapHandler.removeCallbacks(snapTick);   // (the picture the next start draws first was taken while the page showed: SaverSnapshot.capture)
        ScreenView.release(web);
        web = null;
        super.onDetachedFromWindow();
    }
}
