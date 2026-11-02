package sa.urtv.screensaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.dreams.DreamService;
import android.view.KeyEvent;
import android.webkit.WebView;

/** The system screensaver: starts on the TV's own idle timer, any key wakes it. */
public class ScreenDream extends DreamService {
    private WebView web;
    private final RenderGuard guard = new RenderGuard();
    private boolean attached;
    private final AdhanKeys adhanKeys = new AdhanKeys();
    /** settings were saved while this dream is on (an agent's `saver.sh city`): start the page over with them, not at the next dream */
    private final BroadcastReceiver changed = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (!attached || web == null) return;
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
        return super.dispatchKeyEvent(e);
    }

    /** start: the dream starting (counted for the first-run note), not a renderer's re-creation */
    private void show(boolean start) {
        web = ScreenView.create(this, this::gone, start);
        setContentView(web);
    }

    /** The page's renderer died: the dead view is thrown away; a new one at most twice in ten minutes, else the dream ends (the system starts it again later). */
    private void gone(WebView dead) {
        guard.gone(System.currentTimeMillis());
        ScreenView.release(dead);
        if (dead != web) return;
        web = null;
        if (attached && guard.mayCreate(System.currentTimeMillis())) show(false);
        else finish();
    }

    @Override
    public void onDetachedFromWindow() {
        if (attached) Showing.leave();
        attached = false;
        try { unregisterReceiver(changed); } catch (IllegalArgumentException ignored) { }
        ScreenView.release(web);
        web = null;
        super.onDetachedFromWindow();
    }
}
