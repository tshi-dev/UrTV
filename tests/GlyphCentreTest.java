package sa.urtv.screensaver;

/** GlyphCentre: the point a glyph is drawn at puts the middle of its ink on the middle of the tile, for any ink box (tall, tailed, wide, thin) and any tile (no Android). */
public class GlyphCentreTest {
    static int failed;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    public static void main(String[] a) {
        java.util.Random rnd = new java.util.Random(7);
        boolean centred = true;
        for (int n = 0; n < 20000; n++) {
            float w = 40 + rnd.nextInt(200), h = 40 + rnd.nextInt(200);
            float l = -rnd.nextInt(10), r = l + 3 + rnd.nextInt(90), t = -(5 + rnd.nextInt(90)), b = t + 3 + rnd.nextInt(120);   // an ink box around the baseline origin
            float[] at = GlyphCentre.origin(l, t, r, b, w, h);
            float cx = at[0] + (l + r) / 2f, cy = at[1] + (t + b) / 2f;   // where the ink's middle lands in the tile
            centred &= Math.abs(cx - w / 2f) < 0.001f && Math.abs(cy - h / 2f) < 0.001f;
        }
        check("20,000 random ink boxes and tiles: the ink's middle lands on the tile's middle (tall, tailed, wide and thin glyphs alike)", centred);
        float[] tall = GlyphCentre.origin(0, -80, 20, 0, 100, 100), tail = GlyphCentre.origin(0, -50, 60, 40, 100, 100);
        check("a tall letter (ا, ink from -80 to 0 above the baseline) is drawn lower than the line box would put it; a tailed one (ink -50 to 40) is drawn higher than its baseline", tall[1] == 90 && tail[1] == 55);
        System.out.println(failed == 0 ? "ok" : "failed");
        System.exit(failed == 0 ? 0 : 1);
    }
}
