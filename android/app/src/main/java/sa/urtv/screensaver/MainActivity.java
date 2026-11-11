package sa.urtv.screensaver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;

/** Full-screen preview of the screensaver; any remote key closes it. */
public class MainActivity extends Activity {
    private WebView web;
    private final RenderGuard guard = new RenderGuard();
    private boolean alive;
    private final AdhanKeys adhanKeys = new AdhanKeys();
    /** settings were saved while this preview is open: it starts over with them (the page reads its settings once) */
    private final BroadcastReceiver changed = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (!alive || web == null) return;
            WebView old = web;
            web = null;
            ScreenView.release(old);
            show();
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // brought up by the sleep timer (sleeping): no wake lock, so the TV's own idle timer carries on to sleep from here
        if (!getIntent().getBooleanExtra("sleeping", false)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        alive = true;
        show();
        InternalReceivers.register(this, changed, new IntentFilter(Prefs.CHANGED));
    }

    private String shownUrl;
    private final android.os.Handler snapHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable snapTick = new Runnable() {
        @Override public void run() {
            if (!alive) return;
            if (web != null && !NowPlayingBridge.reminderCard()) SaverSnapshot.capture(MainActivity.this, getWindow(), shownUrl, web);   // the picture for the next start (the dream's too: one page)
            snapHandler.postDelayed(this, SaverSnapshot.EVERY_MS);
        }
    };

    /** the same start as the dream's: the last picture of the page (or the ground colour) at once, the page's view after the first frame (SaverStart) */
    private void show() {
        shownUrl = new Prefs(this).screenUrl(this, false);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF040E09));   // the page's ground, as the dream's window: not black until the page has painted
        SaverStart.begin(new SaverStart.Host() {
            @Override public Context context() { return MainActivity.this; }
            @Override public void setContent(View v) {
                setContentView(v);
                v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
            @Override public WebView createPage() { web = ScreenView.create(MainActivity.this, MainActivity.this::gone, false); return web; }
            @Override public boolean alive() { return alive && !isFinishing(); }
        }, shownUrl, () -> {
            snapHandler.removeCallbacks(snapTick);
            snapHandler.postDelayed(snapTick, SaverSnapshot.FIRST_MS);
        });
    }

    /** The page's renderer died: the dead view is thrown away; a new one at most twice in ten minutes, else the preview closes. */
    private void gone(WebView dead) {
        guard.gone(android.os.SystemClock.elapsedRealtime());
        ScreenView.release(dead);
        if (dead != web) return;
        web = null;
        if (alive && guard.mayCreate(android.os.SystemClock.elapsedRealtime())) show();
        else finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        // as in the dream: while the adhan plays, OK (or any key, with «إيقاف الأذان بأي زر») stops it and the preview stays
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
        org.json.JSONObject adhan = down ? NowPlayingBridge.adhanState(this) : null;
        boolean live = adhan != null && adhan.optBoolean("live");
        int d = adhanKeys.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), live, new Prefs(this).adhanBar(), true, live && adhan.optBoolean("any"));
        if (d == AdhanKeys.STOP) NowPlayingBridge.stopAdhan(this);
        if (d != AdhanKeys.PASS) return true;
        if (e.getAction() == KeyEvent.ACTION_UP) finish();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Showing.enter();   // the home's «الأذان على شاشة التوقف فقط»: the preview counts as the screensaver
    }

    @Override
    protected void onStop() {
        Showing.leave();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        alive = false;
        snapHandler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(changed); } catch (IllegalArgumentException ignored) { }
        ScreenView.release(web);
        web = null;
        super.onDestroy();
    }
}
