package sa.urtv.home;

import android.app.Activity;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.os.Handler;
import android.os.Looper;
import android.view.FrameMetrics;
import android.view.View;
import android.view.Window;
import android.view.ViewTreeObserver;

/**
 * Debug only (`optimize/dock.sh debug keytrace on`): one log line per key press on the home, tag UrTVKeyTrace, so the TV names the stage that is slow:
 *   queue    ms between the key's own time and our dispatch (the system, an accessibility key filter and a busy main thread all show here)
 *   dispatch ms the home spent in dispatchKeyEvent (the focus search, the focus listeners, the animations started)
 *   focus@   ms from the start of dispatch to the focus change
 *   layout@  ms from the start of dispatch to the next layout pass finishing (the first pre-draw)
 *   draw@    ms from the start of dispatch to the next draw being issued on the main thread
 *   main     what the main thread was running in the 80 ms before our dispatch (messages over 2 ms, with the time they ended before the key was dispatched): the frame callback that
 *            held a key back (installed only while the switch is on)
 *   frame i  one line per frame of the next six, from FrameMetrics: total, the wait before the frame began (unknown delay), input, animation, measure, draw, sync, command issue,
 *            swap buffers, GPU and the deadline where the TV reports them, and the gap since the frame before: which stage made the doubled frame
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

    private final long tNano = System.nanoTime();   // the clock the frames' vsync stamps use
    private final java.util.List<String> metrics = new java.util.ArrayList<>();
    private long prevVsync;
    private String mainBusy = "";
    private Window.OnFrameMetricsAvailableListener fml;
    private Window window;

    private KeyTrace(int code, double queueMs) { this.code = code; this.queueMs = queueMs; }

    // ---- what the main thread ran just before (a Looper message logger, only while the switch is on)
    private static final int RING = 24;
    private static final String[] names = new String[RING];
    private static final long[] starts = new long[RING], ends = new long[RING];
    private static int ringAt;
    private static boolean installed;

    /** called when the home is built with `keytrace` on */
    static void install() {
        if (installed) return;
        installed = true;
        Looper.getMainLooper().setMessageLogging(new android.util.Printer() {
            @Override public void println(String x) {
                if (x.startsWith(">>>>> Dispatching to")) { int i = ringAt % RING; names[i] = shortName(x); starts[i] = System.nanoTime(); ends[i] = 0; }
                else if (x.startsWith("<<<<< Finished to")) { ends[ringAt % RING] = System.nanoTime(); ringAt++; }
            }
        });
    }

    static void release() {
        if (!installed) return;
        installed = false;
        Looper.getMainLooper().setMessageLogging(null);
    }

    private static String shortName(String x) {
        int h = x.indexOf("Handler (");
        String t = h >= 0 ? x.substring(h + 9, Math.max(h + 9, x.indexOf(')', h))) : x;
        int c = x.lastIndexOf("} ");
        String cb = c >= 0 ? x.substring(c + 2) : "";
        int sp = cb.indexOf('@');
        if (sp > 0) cb = cb.substring(0, sp);
        int dot = t.lastIndexOf('.');
        return (dot >= 0 ? t.substring(dot + 1) : t) + (cb.isEmpty() ? "" : " " + cb.substring(cb.lastIndexOf('.') + 1));
    }

    private String busyBefore() {
        StringBuilder b = new StringBuilder();
        long now = tNano;
        for (int k = 1; k <= RING; k++) {
            int i = ((ringAt - k) % RING + RING) % RING;
            if (names[i] == null || ends[i] == 0) continue;
            double ms = (ends[i] - starts[i]) / 1e6, ago = (now - ends[i]) / 1e6;
            if (ago > 80) break;
            if (ms >= 2) b.append(b.length() > 0 ? "; " : "").append(names[i]).append(' ').append(String.format(java.util.Locale.US, "%.1fms", ms)).append(String.format(java.util.Locale.US, "@-%.0fms", Math.max(0, ago)));
        }
        return b.length() == 0 ? "-" : b.toString();
    }

    private void metricsOn(Window w) {
        window = w;
        if (android.os.Build.VERSION.SDK_INT < 24) return;
        fml = (win, fm, dropped) -> {
            long intended = fm.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP);
            if (intended < tNano - 20_000_000L || metrics.size() >= 6) return;   // a frame from before the key
            long vs = fm.getMetric(FrameMetrics.VSYNC_TIMESTAMP);
            StringBuilder b = new StringBuilder();
            b.append(String.format(java.util.Locale.US, "frame %d of key %d: total=%.1f", metrics.size() + 1, code, fm.getMetric(FrameMetrics.TOTAL_DURATION) / 1e6));
            b.append(String.format(java.util.Locale.US, " unknownDelay=%.1f input=%.1f anim=%.1f measure=%.1f draw=%.1f sync=%.1f issue=%.1f swap=%.1f",
                fm.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION) / 1e6, fm.getMetric(FrameMetrics.INPUT_HANDLING_DURATION) / 1e6, fm.getMetric(FrameMetrics.ANIMATION_DURATION) / 1e6,
                fm.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION) / 1e6, fm.getMetric(FrameMetrics.DRAW_DURATION) / 1e6, fm.getMetric(FrameMetrics.SYNC_DURATION) / 1e6,
                fm.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION) / 1e6, fm.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION) / 1e6));
            if (android.os.Build.VERSION.SDK_INT >= 31) b.append(String.format(java.util.Locale.US, " gpu=%.1f deadline=%.1f", fm.getMetric(FrameMetrics.GPU_DURATION) / 1e6, fm.getMetric(FrameMetrics.DEADLINE) / 1e6));
            b.append(String.format(java.util.Locale.US, " sinceKey=%.1f vsyncGap=%.1f firstDraw=%d", (vs - tNano) / 1e6, prevVsync == 0 ? 0 : (vs - prevVsync) / 1e6, fm.getMetric(FrameMetrics.FIRST_DRAW_FRAME)));
            prevVsync = vs;
            metrics.add(b.toString());
        };
        w.addOnFrameMetricsAvailableListener(fml, new Handler(Looper.getMainLooper()));
    }

    private void metricsOff() {
        if (fml != null && window != null) { try { window.removeOnFrameMetricsAvailableListener(fml); } catch (RuntimeException ignored) { } }
        fml = null;
    }

    static KeyTrace begin(Activity a, KeyEvent e) {
        double q = (SystemClock.uptimeMillis() - e.getEventTime());
        final KeyTrace t = new KeyTrace(e.getKeyCode(), q);
        live = t;
        t.mainBusy = t.busyBefore();
        t.metricsOn(a.getWindow());
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
        android.util.Log.i("UrTVKeyTrace", String.format(java.util.Locale.US, "key=%d queue=%.1fms dispatch=%.1fms focus@=%.1fms layout@=%.1fms draw@=%.1fms frames=[%s] janky=%d main=[%s]",
            code, queueMs, dispatchMs, focusMs, layoutMs, drawMs, f, janky, mainBusy));
        // the frames' own numbers arrive a little later (after the frames are on the screen): one line each, then the listener goes
        final KeyTrace self = this;
        new Handler(Looper.getMainLooper()).postDelayed(() -> { self.metricsOff(); for (String l : self.metrics) android.util.Log.i("UrTVKeyTrace", l); }, 400);
        if (live == this) live = null;
    }
}
