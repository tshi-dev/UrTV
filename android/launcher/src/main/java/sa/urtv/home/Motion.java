package sa.urtv.home;

import android.content.Context;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/**
 * 4.4 the owner: «the same easing everywhere». Every move of our screens (a focus, a label, a pill opening, the control centre coming in) takes
 * MS on couch-tv's curve, cubic-bezier(.2,.8,.2,1); with «الوضع الخفيف» (Ram.low) a move is instant and only a fade runs. Not the content's own
 * timing: a photo's or a scene's cross-fade, the home's first arrival, a game. The settings app has its twin (sa.urtv.screensaver.Motion).
 */
final class Motion {
    private Motion() { }

    static final long MS = 200;

    static Interpolator ease() { return new PathInterpolator(0.2f, 0.8f, 0.2f, 1f); }

    /** a move's time: MS, or none on a weak TV («الوضع الخفيف»: a simple fade instead) */
    static long move(Context c) { return Ram.low(c) ? 0 : MS; }
}
