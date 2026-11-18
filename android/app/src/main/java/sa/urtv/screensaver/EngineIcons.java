package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * The built-in picture set of the engine's games (GameDef.ICONS): twelve plain shapes drawn from numbers, so a game's file names a picture and never carries one. Square corners and straight
 * lines only, like the rest of the kit; the picture is `ink` on whatever the view's background is.
 */
final class EngineIcons extends View {
    private final String name; private final int ink; private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG);

    EngineIcons(Context c, String name, int ink) {
        super(c); this.name = name; this.ink = ink;
        fill.setStyle(Paint.Style.FILL); fill.setColor(ink); line.setStyle(Paint.Style.STROKE); line.setColor(ink); line.setStrokeCap(Paint.Cap.SQUARE); line.setStrokeJoin(Paint.Join.MITER);
    }

    /** the points of a shape on a 0 to 100 square, as x,y pairs (null for the ones drawn another way) */
    static float[] points(String n) {
        switch (n) {
            case "star": return new float[] {50, 6, 62, 38, 96, 38, 69, 58, 79, 92, 50, 72, 21, 92, 31, 58, 4, 38, 38, 38};
            case "triangle": return new float[] {50, 10, 92, 88, 8, 88};
            case "diamond": return new float[] {50, 6, 92, 50, 50, 94, 8, 50};
            case "square": return new float[] {12, 12, 88, 12, 88, 88, 12, 88};
            case "flag": return new float[] {20, 8, 28, 8, 28, 20, 86, 20, 70, 42, 86, 64, 28, 64, 28, 94, 20, 94};
            case "drop": return new float[] {50, 6, 76, 52, 80, 70, 70, 86, 50, 94, 30, 86, 20, 70, 24, 52};
            case "heart": return new float[] {50, 90, 10, 50, 10, 28, 28, 14, 50, 32, 72, 14, 90, 28, 90, 50};
            case "bulb": return new float[] {50, 6, 76, 18, 82, 44, 68, 66, 66, 78, 34, 78, 32, 66, 18, 44, 24, 18};
            default: return null;
        }
    }

    @Override protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight()) / 100f, ox = (getWidth() - 100 * s) / 2, oy = (getHeight() - 100 * s) / 2;
        line.setStrokeWidth(Math.max(2f, 7 * s));
        float[] p = points(name);
        if (p != null) {
            Path path = new Path(); path.moveTo(ox + p[0] * s, oy + p[1] * s);
            for (int i = 2; i + 1 < p.length; i += 2) path.lineTo(ox + p[i] * s, oy + p[i + 1] * s);
            path.close(); c.drawPath(path, fill);
            if (name.equals("bulb")) c.drawRect(ox + 36 * s, oy + 84 * s, ox + 64 * s, oy + 94 * s, fill);
            return;
        }
        switch (name) {
            case "circle": c.drawCircle(ox + 50 * s, oy + 50 * s, 40 * s, fill); break;
            case "check": { Path k = new Path(); k.moveTo(ox + 12 * s, oy + 54 * s); k.lineTo(ox + 38 * s, oy + 80 * s); k.lineTo(ox + 90 * s, oy + 20 * s); c.drawPath(k, line); line.setStrokeWidth(Math.max(3f, 12 * s)); c.drawPath(k, line); break; }
            case "clock": { c.drawRect(ox + 10 * s, oy + 10 * s, ox + 90 * s, oy + 90 * s, line); c.drawLine(ox + 50 * s, oy + 50 * s, ox + 50 * s, oy + 22 * s, line); c.drawLine(ox + 50 * s, oy + 50 * s, ox + 72 * s, oy + 50 * s, line); break; }
            default: { android.graphics.Paint t = new Paint(Paint.ANTI_ALIAS_FLAG); t.setColor(ink); t.setTextSize(90 * s); t.setTextAlign(Paint.Align.CENTER); t.setFakeBoldText(true); c.drawText("؟", ox + 50 * s, oy + 82 * s, t); }   // question
        }
    }
}
