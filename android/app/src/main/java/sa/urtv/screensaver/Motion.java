package sa.urtv.screensaver;

import android.content.Context;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/**
 * 4.4 the owner: «the same easing everywhere». Every move of our screens takes MS on couch-tv's curve, cubic-bezier(.2,.8,.2,1); with «الوضع الخفيف»
 * (on, or auto on a weak TV) a move is instant and only a fade runs. The home app has its twin (sa.urtv.home.Motion).
 */
final class Motion {
    private Motion() { }

    static final long MS = 200;

    static Interpolator ease() { return new PathInterpolator(0.2f, 0.8f, 0.2f, 1f); }

    static boolean lite(Context c) { Prefs p = new Prefs(c); return "on".equals(p.lite()) || ("auto".equals(p.lite()) && Prefs.lowRam(c)); }

    /** a move's time: MS, or none with «الوضع الخفيف» (a simple fade instead) */
    static long move(Context c) { return lite(c) ? 0 : MS; }
}
