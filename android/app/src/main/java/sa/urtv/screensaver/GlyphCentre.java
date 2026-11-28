package sa.urtv.screensaver;

/**
 * Where to draw one glyph so that the middle of its INK is the middle of its tile (no Android; tests/GlyphCentreTest.java). A TextView centres the font's line box, which
 * puts a tall letter (ك ط ظ ل ا) high and a letter with a tail (ج ح خ ع غ م ر ز و) low in a square; here `inkL..inkB` are the bounds `Paint.getTextBounds` gives for the text
 * drawn at the origin (0, 0) on its baseline, and the result is the point to draw it at inside a `w` x `h` view.
 */
final class GlyphCentre {
    private GlyphCentre() {}

    /** {x, y} to pass to `Canvas.drawText` (left-aligned paint) */
    static float[] origin(float inkL, float inkT, float inkR, float inkB, float w, float h) {
        return new float[] {w / 2f - (inkL + inkR) / 2f, h / 2f - (inkT + inkB) / 2f};
    }
}
