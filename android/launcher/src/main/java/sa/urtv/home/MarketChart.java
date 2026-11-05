package sa.urtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * The markets page's drawings, drawn once per change (no animation, no timer):
 * <ul>
 *   <li>a day's line ({@link #line}): the 15-minute closes against the close before (a dashed line), filled softly under it; large on a card,
 *       small as a row's sparkline;</li>
 *   <li>a bar of parts ({@link #bar}): «اتجاه السوق» (rising, unchanged, falling), the session's progress, and the portfolio's allocation.</li>
 * </ul>
 * The line runs left to right in both languages: time reads that way on every chart, as on the mockups.
 */
final class MarketChart extends View {
    private double[] closes = new double[0];
    private double base = Double.NaN;
    private int color, dim;
    private boolean fill;
    private float stroke;

    private float[] parts = new float[0];
    private int[] colors = new int[0];
    private boolean isBar;
    private int track;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();

    MarketChart(Context c) {
        super(c);
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
    }

    /** The day's line: {@code closes} (oldest first), the close before as {@code base} (NaN: none), in {@code color}; the dashed line in {@code dim}. */
    MarketChart line(double[] closes, double base, int color, int dim, boolean fill, float strokeDp) {
        this.closes = closes == null ? new double[0] : closes;
        this.base = base;
        this.color = color;
        this.dim = dim;
        this.fill = fill;
        this.stroke = strokeDp * getResources().getDisplayMetrics().density;
        isBar = false;
        invalidate();
        return this;
    }

    /** A bar of parts, each its weight and colour, in reading order (from the start of the line); {@code track} under them (0: none). */
    MarketChart bar(float[] weights, int[] colors, int track) {
        this.parts = weights;
        this.colors = colors;
        this.track = track;
        isBar = true;
        invalidate();
        return this;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (isBar) drawBar(c); else drawLine(c);
    }

    private void drawBar(Canvas c) {
        float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
        float rad = h / 2, gap = parts.length > 1 ? 3 * d : 0;
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);
        if (track != 0) {
            p.setColor(track);
            r.set(0, 0, w, h);
            c.drawRoundRect(r, rad, rad, p);
        }
        float sum = 0;
        int shown = 0;
        for (float x : parts) if (x > 0) { sum += x; shown++; }
        if (sum <= 0) return;
        float room = w - gap * Math.max(0, shown - 1);
        boolean rtl = Dir.rtl();
        float at = rtl ? w : 0;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] <= 0) continue;
            float len = room * parts[i] / sum;
            if (rtl) r.set(at - len, 0, at, h); else r.set(at, 0, at + len, h);
            p.setColor(colors[i]);
            c.drawRoundRect(r, rad, rad, p);
            at = rtl ? at - len - gap : at + len + gap;
        }
    }

    private void drawLine(Canvas c) {
        float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
        if (closes.length < 2 || w <= 0 || h <= 0) return;
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (double x : closes) { lo = Math.min(lo, x); hi = Math.max(hi, x); }
        if (!Double.isNaN(base)) { lo = Math.min(lo, base); hi = Math.max(hi, base); }
        if (hi - lo < 1e-9) { hi += 1; lo -= 1; }
        float pad = stroke + d;
        float top = pad, bottom = h - pad;
        path.reset();
        for (int i = 0; i < closes.length; i++) {
            float x = closes.length == 1 ? 0 : w * i / (closes.length - 1);
            float y = (float) (bottom - (closes[i] - lo) / (hi - lo) * (bottom - top));
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        if (fill) {
            Path area = new Path(path);
            area.lineTo(w, h);
            area.lineTo(0, h);
            area.close();
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, 0, 0, h, (color & 0x00FFFFFF) | 0x38000000, (color & 0x00FFFFFF) | 0x08000000, Shader.TileMode.CLAMP));
            c.drawPath(area, p);
            p.setShader(null);
        }
        if (!Double.isNaN(base)) {
            float y = (float) (bottom - (base - lo) / (hi - lo) * (bottom - top));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1, d));
            p.setColor(dim);
            p.setPathEffect(new DashPathEffect(new float[] {4 * d, 4 * d}, 0));
            c.drawLine(0, y, w, y, p);
            p.setPathEffect(null);
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(stroke);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(color);
        c.drawPath(path, p);
    }
}
