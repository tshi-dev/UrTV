package sa.urtv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/**
 * 4.3.3 «الآفاق»: the big glass clock of a scene (docs/mockups). The digits are filled with the scene's glass copy (blurred and brightened, the
 * same trick as couch-tv's CSS background-clip: text, here a BitmapShader), frosted (less colour, a white veil, a little see-through: frost()), with a
 * thin bright rim and a soft dark shadow so they read on any scene; a pale scene has a dark fill instead. The view covers the whole screen, so the glass lines up with the photo under it. It draws only
 * when the time changes (once a minute); nothing animates.
 */
final class SceneClock extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), edge = new Paint(Paint.ANTI_ALIAS_FLAG), sheen = new Paint(Paint.ANTI_ALIAS_FLAG),
            shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect bounds = new Rect();
    private String text = "";
    private int[] box;            // the digits' box: left, top, right, bottom (ScenePlan.clockBox)
    private Bitmap glass;
    private boolean dark;
    private final float density;

    SceneClock(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(0.8f * density);   // 1.6 px at 1080p: thin and bright
        shadow.setColor(0x00000000);
        setWillNotDraw(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);   // the header's clock still says the time to a screen reader
    }

    private static Typeface glyphsOf;
    private static ScenePlan.Glyphs glyphs;

    /**
     * 4.4 the clock's glyphs for the ink rule (ScenePlan.inkLost): each digit (Arabic and Latin) and the colon drawn with the clock's typeface, a
     * digit ScenePlan.GLYPH_H px tall («٤٥٨»'s ink), its ink alpha 128 or more; made once per typeface; null when they cannot be drawn
     */
    static synchronized ScenePlan.Glyphs glyphs(Typeface t) {
        if (glyphs != null && glyphsOf == t) return glyphs;
        try {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setTypeface(t);
            p.setTextSize(100);
            Rect r = new Rect();
            p.getTextBounds("٤٥٨", 0, 3, r);
            if (r.height() <= 0) return null;
            p.setTextSize(100f * ScenePlan.GLYPH_H / r.height());
            ScenePlan.Glyphs g = new ScenePlan.Glyphs();
            for (char c : "٠١٢٣٤٥٦٧٨٩0123456789:".toCharArray()) {
                String s = String.valueOf(c);
                p.getTextBounds(s, 0, 1, r);
                int gw = Math.max(1, r.width() + 2), gh = Math.max(1, r.height() + 2);
                Bitmap b = Bitmap.createBitmap(gw, gh, Bitmap.Config.ARGB_8888);
                new Canvas(b).drawText(s, 1 - r.left, 1 - r.top, p);
                int[] px = new int[gw * gh];
                b.getPixels(px, 0, gw, 0, 0, gw, gh);
                b.recycle();
                boolean[] in = new boolean[px.length];
                for (int i = 0; i < px.length; i++) in[i] = (px[i] >>> 24) >= 128;
                g.put(c, r.left - 1, r.top - 1, gw, gh, p.measureText(s), in);
            }
            glyphsOf = t;
            glyphs = g;
            return g;
        } catch (RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }

    void setTypeface(Typeface t) { fill.setTypeface(t); edge.setTypeface(t); sheen.setTypeface(t); shadow.setTypeface(t); invalidate(); }

    void setTime(CharSequence t) {
        String s = t == null ? "" : t.toString();
        if (s.equals(text)) return;
        text = s;
        invalidate();
    }

    /** 4.4 the picture behind is zoomed by k about the screen's top centre (ScenePlan.zoom): the glass in the digits follows it */
    private float zoom = 1f;

    void setZoom(float k) { if (k == zoom) return; zoom = k; shaderFor(getWidth(), getHeight()); invalidate(); }

    void setScene(Bitmap glassCopy, int[] digits, boolean darkClock) {
        glass = glassCopy;
        box = digits;
        dark = darkClock;
        shaderFor(getWidth(), getHeight());
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { shaderFor(w, h); }

    private void shaderFor(int w, int h) {
        if (glass == null || w == 0 || h == 0) { fill.setShader(null); return; }
        BitmapShader sh = new BitmapShader(glass, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix m = new Matrix();
        m.setScale(w / (float) glass.getWidth(), h / (float) glass.getHeight());   // the glass copy is half size: stretched over the screen
        m.postScale(zoom, zoom, w / 2f, 0);
        sh.setLocalMatrix(m);
        fill.setShader(sh);
        fill.setColorFilter(new ColorMatrixColorFilter(frost(dark)));
        fill.setAlpha(dark ? 0xF0 : 0xE0);   // a little see-through: the scene shows faintly through the frost
        edge.setColor(0xE6FFFFFF);   // a thin bright rim, on a dark clock too (the fog scene's)
        shadow.setColor(0x33000000);
        shadow.setShadowLayer(12 * density, 0, 3 * density, 0x59000000);   // soft: the digits read on a bright sky and a dark one
    }

    /**
     * 4.4 the frost (the owner: lighter, crisp, never a pink or washed cast): the glass copy's colour taken 40 % toward grey, then a white veil
     * (out = in x 0.72 + 30 %); a pale scene's dark clock: the copy darkened instead.
     */
    static ColorMatrix frost(boolean dark) {
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.6f);
        ColorMatrix lift = new ColorMatrix();
        if (dark) lift.setScale(0.42f, 0.42f, 0.42f, 1f);
        else lift.set(new float[] {0.72f, 0, 0, 0, 76, 0, 0.72f, 0, 0, 76, 0, 0, 0.72f, 0, 76, 0, 0, 0, 1, 0});
        cm.postConcat(lift);
        return cm;
    }

    @Override protected void onDraw(Canvas c) {
        if (box == null || text.isEmpty() || glass == null) return;
        int boxH = box[3] - box[1], boxW = box[2] - box[0];
        fill.setTextSize(100);
        fill.getTextBounds(text, 0, text.length(), bounds);
        if (bounds.height() <= 0 || bounds.width() <= 0) return;
        float size = 100f * boxH / bounds.height();
        if (bounds.width() * size / 100f > boxW) size = 100f * boxW / bounds.width();   // a long time («١٢:٣٠ م») narrows, never spills
        for (Paint p : new Paint[] {fill, edge, sheen, shadow}) p.setTextSize(size);
        fill.getTextBounds(text, 0, text.length(), bounds);
        float x = box[0] + (boxW - bounds.width()) / 2f - bounds.left;
        float y = box[3] - bounds.bottom;   // the digits stand on the box's bottom line, where the scenery crosses them
        sheen.setShader(new LinearGradient(x, box[1], x + bounds.width() * 0.6f, box[3],
                new int[] {0x8CFFFFFF, 0x00FFFFFF, 0x00FFFFFF, 0x38FFFFFF}, new float[] {0f, 0.38f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        c.drawText(text, x, y, shadow);   // the soft dark shadow under the glass
        c.drawText(text, x, y, fill);
        c.drawText(text, x, y, sheen);
        c.drawText(text, x, y, edge);
    }
}
