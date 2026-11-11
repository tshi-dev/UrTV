package sa.urtv.home;

import android.app.Activity;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Debug only (`optimize/dock.sh debug keytrace on`): one log line per key press on the home, tag UrTVKeyTrace, so the TV names the stage that is slow:
 *   queue    ms between the key's own time and our dispatch (the system, an accessibility key filter and a busy main thread all show here)
 *   dispatch ms the home spent in dispatchKeyEvent (the focus search, the focus listeners, the animations started)
 *   focus@   ms from the start of dispatch to the focus change
 *   layout@  ms from the start of dispatch to the next layout pass finishing (the first pre-draw)
 *   draw@    ms from the start of dispatch to the next draw being issued on the main thread
 *   frames   the next six frames' intervals in ms (16.7 is smooth; a long one is a janky frame, and the number of them is counted)
 * Nothing runs unless the switch is on.
 */
final class KeyTrace {
    private static KeyTrace live;

    private final long t0 = SystemClock.elapsedRealtimeNanos();
    private final int code;
    private final double queueMs;
    private double dispatchMs = -1, focusMs = -1, layoutMs = -1, drawMs = -1;
    private final double[] frames = new double[6];
    private int n;
    private long lastFrame;

    private KeyTrace(int code, double queueMs) { this.code = code; this.queueMs = queueMs; }

    static KeyTrace begin(Activity a, KeyEvent e) {
        double q = (SystemClock.uptimeMillis() - e.getEventTime());
        final KeyTrace t = new KeyTrace(e.getKeyCode(), q);
        live = t;
        final View decor = a.getWindow().getDecorView();
        decor.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                ViewTreeObserver o = decor.getViewTreeObserver();
                if (o.isAlive()) o.removeOnPreDrawListener(this);
                if (t.layoutMs < 0) t.layoutMs = t.since();
                return true;
            }
        });
        decor.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
            @Override public void onDraw() {
                if (t.drawMs < 0) t.drawMs = t.since();
                final ViewTreeObserver.OnDrawListener self = this;
                decor.post(() -> { ViewTreeObserver o = decor.getViewTreeObserver(); if (o.isAlive()) o.removeOnDrawListener(self); });
            }
        });
        Choreographer.getInstance().postFrameCallback(t::frame);
        return t;
    }

    private double since() { return (SystemClock.elapsedRealtimeNanos() - t0) / 1e6; }

    void dispatched() { dispatchMs = since(); }

    /** the focus moved (called from the home's global focus listener) */
    static void focusChanged() { KeyTrace t = live; if (t != null && t.focusMs < 0) t.focusMs = t.since(); }

    private void frame(long frameTimeNanos) {
        if (lastFrame != 0 && n < frames.length) frames[n++] = (frameTimeNanos - lastFrame) / 1e6;
        lastFrame = frameTimeNanos;
        if (n < frames.length) { Choreographer.getInstance().postFrameCallback(this::frame); return; }
        int janky = 0;
        StringBuilder f = new StringBuilder();
        for (double d : frames) { f.append(f.length() > 0 ? "," : "").append(String.format(java.util.Locale.US, "%.1f", d)); if (d > 25) janky++; }
        android.util.Log.i("UrTVKeyTrace", String.format(java.util.Locale.US, "key=%d queue=%.1fms dispatch=%.1fms focus@=%.1fms layout@=%.1fms draw@=%.1fms frames=[%s] janky=%d",
            code, queueMs, dispatchMs, focusMs, layoutMs, drawMs, f, janky));
        if (live == this) live = null;
    }
}
