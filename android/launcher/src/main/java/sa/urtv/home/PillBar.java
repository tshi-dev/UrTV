package sa.urtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.LinearLayout;
import android.view.View;

/**
 * The top row's one bar (design/43): the buttons stand on a single panel of the look's glass (its fill and hairline, as thin as «شفافية الشريط» says), not each in a round
 * shape of its own. The bar is drawn behind the Pills only (not behind the «يُتلى الآن» card or the network icon), 4 dp wider than the first and last of them.
 */
final class PillBar extends LinearLayout {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float dp;

    PillBar(Context c) {
        super(c);
        dp = c.getResources().getDisplayMetrics().density;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Math.max(1f, dp));
        setWillNotDraw(false);
        setClipChildren(false);
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
            if (!(v instanceof Pill) || v.getVisibility() != VISIBLE) continue;
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
