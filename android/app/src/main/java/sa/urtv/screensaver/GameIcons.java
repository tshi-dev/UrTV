package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * The three little pictures of the games list (docs/GAMES-KIT.md): a phone (the «بالجوالات» filter, and the tag on a game that plays with phones), a palette (the night colours:
 * as many dots filled as the palette's number) and a moon (the night mode). The state is in the picture itself: a filled shape is on, an outline is off. They are drawn on a
 * 100 x 100 square with the colours the caller gives from the look (no colour of its own).
 */
final class GameIcons extends View {
    static final int PHONE = 0, PALETTE = 1, MOON = 2;
    private final int kind; private boolean on; private int level, fg, accent;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    GameIcons(Context c, int kind) { super(c); this.kind = kind; p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); }
    /** `on`: filled (the filter is on, the night is on); `level`: for the palette, 1 to 3 dots filled; `fg`: the shape; `accent`: the details */
    GameIcons set(boolean on, int level, int fg, int accent) { this.on = on; this.level = level; this.fg = fg; this.accent = accent; invalidate(); return this; }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth() - getPaddingLeft() - getPaddingRight(), h = getHeight() - getPaddingTop() - getPaddingBottom(), s = Math.min(w, h) / 100f; if (s <= 0) return;
        c.save(); c.translate(getPaddingLeft() + (w - 100 * s) / 2f, getPaddingTop() + (h - 100 * s) / 2f); c.scale(s, s);
        p.setStrokeWidth(7);
        if (kind == PHONE) {
            RectF body = new RectF(30, 12, 70, 88);
            p.setColor(fg); p.setStyle(on ? Paint.Style.FILL : Paint.Style.STROKE); c.drawRoundRect(body, 9, 9, p);
            p.setColor(on ? accent : fg); p.setStyle(Paint.Style.FILL); c.drawCircle(50, 77, 4, p);                      // the home dot
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5); c.drawLine(43, 22, 57, 22, p);                          // the speaker
        } else if (kind == PALETTE) {
            p.setColor(fg); p.setStyle(Paint.Style.STROKE); c.drawCircle(50, 50, 38, p);
            float[][] dots = {{32, 52}, {50, 30}, {68, 52}};
            for (int i = 0; i < 3; i++) {
                boolean filled = i < Math.max(1, Math.min(3, level));
                p.setColor(filled ? accent : fg); p.setStyle(filled ? Paint.Style.FILL : Paint.Style.STROKE); p.setStrokeWidth(5);
                c.drawCircle(dots[i][0], dots[i][1], 8, p);
            }
        } else {
            Path moon = new Path(); moon.addCircle(46, 52, 34, Path.Direction.CW);
            Path cut = new Path(); cut.addCircle(64, 40, 29, Path.Direction.CW);
            moon.op(cut, Path.Op.DIFFERENCE);
            p.setColor(fg); p.setStyle(on ? Paint.Style.FILL : Paint.Style.STROKE); c.drawPath(moon, p);
            if (on) { p.setColor(accent); p.setStyle(Paint.Style.FILL); c.drawCircle(76, 62, 4.5f, p); c.drawCircle(64, 80, 3.2f, p); }   // two stars when the night is on
        }
        c.restore();
    }
}
