package sa.urtv.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The top row's one bar (design/43): the buttons stand on a single panel of the look's glass (its fill and hairline, as thin as «شفافية الشريط» says), not each in a round
 * shape of its own. The bar is drawn behind the Pills and the bar's members (the network icon), not behind the «يُتلى الآن» card, 4 dp wider than the first and last of them.
 *
 * Collapsed by default (design/43): only the main buttons (phone control, the control center) and the members (the network status) show; the other Pills have no width, no margin, no
 * alpha and no focus. When the focus comes into the bar it opens (about 220 ms, ease-out: width, margin and fade), and it closes again when the focus has left. Nothing is removed or
 * hidden (the row's index-based adding and removing is as it was); a Pill added while it is closed starts closed.
 */
final class PillBar extends LinearLayout {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float dp;
    private final Set<View> mains = new HashSet<>(), members = new HashSet<>();
    private final Map<View, int[]> margins = new HashMap<>();   // a collapsed Pill's own start and end margins, put back as it opens
    private float open;   // 0 closed .. 1 open
    private ValueAnimator anim;
    static final long OPEN_MS = 220, CLOSE_DELAY_MS = 160;

    PillBar(Context c) {
        super(c);
        dp = c.getResources().getDisplayMetrics().density;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Math.max(1f, dp));
        setWillNotDraw(false);
        setClipChildren(false);
    }

    /** the buttons that stay when the row is closed */
    void mains(View... v) { for (View x : v) if (x != null) mains.add(x); }

    /** a part of the bar that is not a Pill (the network status): drawn on the bar, always shown */
    void member(View v) { members.add(v); }

    boolean isOpen() { return open >= 1f; }

    /** the focus came into the bar (true) or went out of it (false) */
    void focusInside(boolean inside) {
        removeCallbacks(closer);
        if (inside) animateTo(1f); else postDelayed(closer, CLOSE_DELAY_MS);   // a short wait: moving from one button to the next is not leaving
    }

    private final Runnable closer = new Runnable() { @Override public void run() { animateTo(0f); } };

    private void animateTo(float to) {
        if (anim != null) anim.cancel();
        if (open == to) { apply(); return; }
        anim = ValueAnimator.ofFloat(open, to);
        anim.setDuration(Math.max(40, Math.round(OPEN_MS * Math.abs(to - open))));
        anim.setInterpolator(new DecelerateInterpolator(2f));
        anim.addUpdateListener(a -> { open = (float) a.getAnimatedValue(); apply(); });
        anim.start();
    }

    private void apply() { for (int i = 0; i < getChildCount(); i++) shape(getChildAt(i)); requestLayout(); invalidate(); }

    /** one Pill at the openness of now: not a main one, not a member */
    private void shape(View v) {
        if (!(v instanceof Pill) || mains.contains(v) || members.contains(v)) return;
        ViewGroup.LayoutParams raw = v.getLayoutParams();
        if (!(raw instanceof LinearLayout.LayoutParams)) return;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) raw;
        int[] m = margins.get(v);
        if (m == null) { m = new int[] {lp.getMarginStart(), lp.getMarginEnd()}; margins.put(v, m); }
        if (open >= 1f) { lp.width = ViewGroup.LayoutParams.WRAP_CONTENT; lp.setMarginStart(m[0]); lp.setMarginEnd(m[1]); v.setAlpha(1f); v.setFocusable(true); return; }
        lp.width = Math.round(dp * Pill.HEIGHT * open);
        lp.setMarginStart(Math.round(m[0] * open));
        lp.setMarginEnd(Math.round(m[1] * open));
        v.setAlpha(open);
        v.setFocusable(open > 0.5f);   // closed it cannot take the focus; opening, it can once it is mostly there
    }

    @Override public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (open < 1f) child.post(() -> { shape(child); child.requestLayout(); });   // a Pill added to a closed row starts closed (its margins are read from its params first)
    }

    @Override public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        margins.remove(child);
    }

    /** the glass of the home now; the strength is the owner's «شفافية الشريط» (100: the look as it is) */
    void look(GlassStyle.Look l) {
        int pct = GlassStyle.barPercent();
        fill.setColor(scaled(l.panel, pct));
        line.setColor(scaled(l.line, pct));
        invalidate();
    }

    private static int scaled(int argb, int pct) { return (Math.round((argb >>> 24) * pct / 100f) << 24) | (argb & 0xFFFFFF); }

    @Override protected void dispatchDraw(Canvas c) {
        float l = Float.MAX_VALUE, t = Float.MAX_VALUE, rt = -Float.MAX_VALUE, b = -Float.MAX_VALUE;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (!(v instanceof Pill || members.contains(v)) || v.getVisibility() != VISIBLE || v.getWidth() == 0) continue;
            l = Math.min(l, v.getLeft()); t = Math.min(t, v.getTop()); rt = Math.max(rt, v.getRight()); b = Math.max(b, v.getBottom());
        }
        if (l < rt) {
            r.set(l - 4 * dp, t - 4 * dp, rt + 4 * dp, b + 4 * dp);
            c.drawRoundRect(r, 12 * dp, 12 * dp, fill);
            c.drawRoundRect(r, 12 * dp, 12 * dp, line);
        }
        super.dispatchDraw(c);
    }
}
