package sa.urtv.home;

import android.view.View;
import android.view.ViewTreeObserver;

/**
 * G4 (4.3.3): work that the screen does not need for its first frame runs right after that frame is drawn, not before it (a return's
 * `am start -W` time is until that frame). The frame's pre-draw lets it go once (the frame is not held: it returns true) and the work is
 * posted, so it runs after the draw; when no frame comes within LATE_MS (the screen did not change) it runs then. Once, either way.
 */
final class AfterFrame {
    private AfterFrame() { }

    static final long LATE_MS = 1000;

    static void run(final View v, final Runnable work) {
        final boolean[] done = {false};
        final Runnable once = () -> { if (done[0]) return; done[0] = true; work.run(); };
        final ViewTreeObserver.OnPreDrawListener[] l = new ViewTreeObserver.OnPreDrawListener[1];
        l[0] = () -> {
            ViewTreeObserver o = v.getViewTreeObserver();
            if (o.isAlive()) o.removeOnPreDrawListener(l[0]);
            v.post(once);
            return true;
        };
        v.getViewTreeObserver().addOnPreDrawListener(l[0]);
        v.postDelayed(() -> {
            ViewTreeObserver o = v.getViewTreeObserver();
            if (o.isAlive()) o.removeOnPreDrawListener(l[0]);
            once.run();
        }, LATE_MS);
    }
}
