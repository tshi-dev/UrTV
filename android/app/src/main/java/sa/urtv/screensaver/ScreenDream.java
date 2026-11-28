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
            if (!attached) return;
            if (gate.changedWhileStarting()) return;   // 4.4 review F1: the page is not created yet; it is looked at again when it is (createPage)
            rebuildIfChanged(c);
        }
    };
    private final StartGate gate = new StartGate();
    /** 4.4 beta 4: every 10 minutes while it shows, one log line with this app's share of one core (the page's renderer is its own process) and the tier */
    static final long CPU_EVERY_MS = 10 * 60_000L;
    private long cpuAt, cpuMs;
    private final Runnable cpuTick = new Runnable() {
        @Override public void run() {
            if (!attached) return;
            long now = android.os.SystemClock.elapsedRealtime(), cpu = android.os.Process.getElapsedCpuTime();
            android.util.Log.i("UrTVSaver", "cpu: " + CpuClass.percent(cpu - cpuMs, now - cpuAt) + "% of one core over " + (now - cpuAt) / 60_000 + " min (this app), tier "
                    + (Prefs.lowRam(ScreenDream.this) ? "low" : Prefs.midRam(ScreenDream.this) ? "mid" : "normal") + ", " + Runtime.getRuntime().availableProcessors() + " cores at " + CpuClass.maxKhz() / 1000 + " MHz");
            cpuAt = now; cpuMs = cpu;
            snapHandler.postDelayed(this, CPU_EVERY_MS);
        }
    };

    /** S6 (P4): the page is built again only when what it is opened with changed; a setting that does not reach it (the phone's, the home's) left the dream as it was */
    private void rebuildIfChanged(Context c) {
        if (!attached || web == null) return;
        if (new Prefs(c).screenUrl(c, false).equals(shownUrl)) return;
        final String now = new Prefs(c).screenUrl(c, false);
        final String push = PagePush.script(shownUrl, now);   // 4.4 beta 4: the markets line on or off reaches the page as it runs
        if (push != null) {
            final WebView w = web;
            shownUrl = now;
            w.evaluateJavascript(push, r -> { if (!"true".equals(r) && w == web) rebuild(); });   // an older page without it: built again, as before
            return;
        }
        rebuild();
    }

    private void rebuild() {
        if (!attached || web == null) return;
        WebView old = web;
        web = null;
        ScreenView.release(old);
        show(false);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        attached = true;
        started = false;
        if (getWindow() != null) getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF040E09));   // the page's ground while the launcher answers
        // «إعداد تلفزيوني» waits for «السماح بتصحيح USB؟»: never cover it (it would stay stuck). Asked on a thread (DreamHold): with the launcher not running the
        // query waits for its process to start, and on the main thread that was an ANR
        final Context app = getApplicationContext();
        snapHandler.postDelayed(holdWait, DreamHold.WAIT_MS);
        Thread t = new Thread(() -> {
            final boolean held = NowPlayingBridge.setupWaiting(app);
            snapHandler.post(() -> holdAnswer(true, held));
        }, "dream-hold");
        t.setDaemon(true);
        t.start();
    }

    private boolean started, entered;
    private final Runnable holdWait = () -> holdAnswer(false, false);

    /** the launcher's answer (or the wait's end, DreamHold.WAIT_MS): start the page once, or end the dream while the setup holds it */
    private void holdAnswer(boolean answered, boolean held) {
        int d = DreamHold.decide(attached, started, answered, held);
        if (answered) snapHandler.removeCallbacks(holdWait);
        if (d == DreamHold.END) {
            android.util.Log.i("UrTVSaver", "not started: the setup is waiting for the debugging prompt");
            finish();
            return;
        }
        if (d != DreamHold.START) return;
        started = true;
        entered = true;
        Showing.enter();
        snapHandler.post(adhanWatch);
        ClockCheck.start(this);   // 4.4 beta 4: the TV's clock against the network's, for the page's date line
        cpuAt = android.os.SystemClock.elapsedRealtime(); cpuMs = android.os.Process.getElapsedCpuTime();
        snapHandler.postDelayed(cpuTick, CPU_EVERY_MS);   // 4.4 beta 4: what this app's processor use is, in the log (a weak TV's report)
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
        org.json.JSONObject adhan = down ? NowPlayingBridge.adhanKnown(this) : null;   // asked on a press only; a release belongs to the one before
        boolean live = adhan != null && adhan.optBoolean("live");
        int d = adhanKeys.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), live, new Prefs(this).adhanBar(), true, live && adhan.optBoolean("any"));
        if (d == AdhanKeys.STOP) NowPlayingBridge.stopAdhanSoon(this);
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
        final Context app = getApplicationContext();
        Thread t = new Thread(() -> {   // on a thread, as setupWaiting (DreamHold): the launcher may have to start first
            if (!NowPlayingBridge.setupRunning(app)) return;
            snapHandler.post(() -> {
                if (!attached) return;
                if (android.os.Build.VERSION.SDK_INT >= 29) wakeUp(); else finish();
            });
        }, "dream-setup");
        t.setDaemon(true);
        t.start();
    }

    /** with the adhan bar off the page does not ask the launcher about the adhan: the dream does, every 3 s, so «إيقاف الأذان بأي زر» still finds it playing (on a thread, never on the UI thread) */
    private final Runnable adhanWatch = new Runnable() {
        @Override public void run() {
            if (!attached) return;
            if (!new Prefs(ScreenDream.this).adhanBar()) NowPlayingBridge.refreshAdhan(ScreenDream.this);
            snapHandler.postDelayed(this, 3000);
        }
    };

    private final android.os.Handler snapHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable snapTick = new Runnable() {
        @Override public void run() {
            // not while a reminder's card is up (it would come back with the next start's first frame)
            if (attached && web != null && !NowPlayingBridge.reminderCard()) SaverSnapshot.capture(ScreenDream.this, getWindow(), shownUrl, web);
            if (attached) snapHandler.postDelayed(this, SaverSnapshot.EVERY_MS);
        }
    };

    /** start: the dream starting (counted for the first-run note), not a renderer's re-creation: the picture first, the page after the first frame (SaverStart) */
    private void show(final boolean start) {
        shownUrl = new Prefs(this).screenUrl(this, false);
        final int token = gate.begin();   // a start that a newer one has replaced creates nothing
        if (getWindow() != null) getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF040E09));   // the page's ground, under the view until it has painted
        SaverStart.begin(new SaverStart.Host() {
            @Override public Context context() { return ScreenDream.this; }
            @Override public void setContent(android.view.View v) { setContentView(v); }
            @Override public WebView createPage() {
                web = ScreenView.create(ScreenDream.this, ScreenDream.this::gone, start);
                gate.created(token);
                if (gate.takeDirty()) snapHandler.post(() -> rebuildIfChanged(ScreenDream.this));   // a change came in during the start
                return web;
            }
            @Override public boolean alive() { return attached && gate.current(token); }
        }, shownUrl, () -> {
            snapHandler.removeCallbacks(snapTick);
            snapHandler.postDelayed(snapTick, SaverSnapshot.FIRST_MS);   // the picture for the next start, taken from the window while the page shows
        });
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
        if (entered) Showing.leave();
        entered = false;
        attached = false;
        snapHandler.removeCallbacks(holdWait);
        gate.cancel();
        try { unregisterReceiver(changed); } catch (IllegalArgumentException ignored) { }
        snapHandler.removeCallbacks(adhanWatch);
        snapHandler.removeCallbacks(snapTick);   // (the picture the next start draws first was taken while the page showed: SaverSnapshot.capture)
        snapHandler.removeCallbacks(cpuTick);
        ScreenView.release(web);
        web = null;
        super.onDetachedFromWindow();
    }
}
