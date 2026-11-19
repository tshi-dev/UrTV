package sa.urtv.home;

/**
 * 4.4 beta 2, the owner (build 525: «the icons need to be 100% perfect»; then: «no outline, no small icons»): how an app with no TV banner fills its
 * tile. No Android in the decisions (tests/tile_art_test.py runs them on a JVM); MainActivity.composeTile draws them, off the main thread, once per
 * app, from the icon at the highest density the app has (xxxhdpi), so nothing is blurry.
 *
 * A banner fills the tile (its own art, unchanged). Every other app fills its tile too, with no frame and no tint of a colour of ours:
 *  - an adaptive icon: its BACKGROUND layer covers the whole tile and its FOREGROUND layer is drawn large in the middle (ADAPTIVE_FG of the tile's
 *    height, so the logo, the middle 72 of its 108, is about 80 % of the height): the app's own colours, edge to edge;
 *  - a legacy icon: its transparent padding trimmed first, then fitted to LEGACY_W x LEGACY_H of the tile (a tiny one enlarged at most MAX_UP),
 *    on the colour of its own edge pixels (so it looks native); an icon whose edge is transparent (a bare glyph) gets LIGHT or DARK, whichever
 *    differs more from its own lightness (so white on white never happens).
 */
final class TileArt {
    private TileArt() { }

    static final String BANNER = "banner", ADAPTIVE = "adaptive", ICON = "icon", WIDE = "wide";
    /** the two backings for a bare glyph: a warm light grey and a dark slate (never a tint of the app's colour) */
    static final int LIGHT = 0xFFECE9E3, DARK = 0xFF2B2E36;
    static final float LIGHT_LUM = 0.91f, DARK_LUM = 0.17f;
    /** an adaptive icon's foreground, as a share of the tile's height (its middle 72 of 108, the logo, about 80 % of it) */
    static final float ADAPTIVE_FG = 1.2f;
    /** a legacy icon's box on the tile: 85 % of the height (a square icon), 80 % of the width (a wide logo) */
    static final float LEGACY_W = 0.8f, LEGACY_H = 0.85f, WIDE_ASPECT = 1.3f;
    /** a small bitmap icon is enlarged at most this much */
    static final float MAX_UP = 4f;
    /** an edge with fewer opaque pixels than this share is a bare glyph's (no colour of its own to take) */
    static final float EDGE_OPAQUE = 0.6f;
    /** the highest density asked for an app's icon and banner (DisplayMetrics.DENSITY_XXXHIGH) */
    static final int DENSITY = 640;

    /** what the app shows: its banner, else its icon by its kind and shape */
    static String kind(boolean hasBanner, boolean adaptive, int iw, int ih) {
        if (hasBanner) return BANNER;
        if (adaptive) return ADAPTIVE;
        if (iw > 0 && ih > 0 && iw > ih * WIDE_ASPECT) return WIDE;
        return ICON;
    }

    /** the drawn size {w, h} on a tile of tw x th: an adaptive icon's foreground square, or a legacy icon fitted to its box */
    static int[] box(String kind, int tw, int th, int iw, int ih) {
        if (tw <= 0 || th <= 0) return new int[] {0, 0};
        if (ADAPTIVE.equals(kind)) { int s = Math.round(th * ADAPTIVE_FG); return new int[] {s, s}; }
        float maxW = tw * LEGACY_W, maxH = th * LEGACY_H;
        if (iw <= 0 || ih <= 0) { int s = Math.round(maxH); return new int[] {s, s}; }
        float f = Math.min(Math.min(maxW / iw, maxH / ih), MAX_UP);
        return new int[] {Math.max(1, Math.round(iw * f)), Math.max(1, Math.round(ih * f))};
    }

    /**
     * a legacy icon's tile colour from its edge pixels (its outermost opaque ring: the mean of those with alpha >= 128), or 0 when too few are
     * opaque (a bare glyph: then backing() decides)
     */
    static int edgeColour(int[] edge) {
        if (edge == null || edge.length == 0) return 0;
        long r = 0, g = 0, b = 0; int n = 0;
        for (int p : edge) {
            if (((p >>> 24) & 0xFF) < 128) continue;
            r += (p >> 16) & 0xFF; g += (p >> 8) & 0xFF; b += p & 0xFF; n++;
        }
        if (n < edge.length * EDGE_OPAQUE) return 0;
        return 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    /** a bare glyph's backing: whichever of LIGHT or DARK differs more from its opaque pixels' mean lightness; none read: LIGHT */
    static int backing(float meanLum, float opaqueShare) {
        if (opaqueShare <= 0f || meanLum < 0f) return LIGHT;
        return Math.abs(meanLum - LIGHT_LUM) >= Math.abs(meanLum - DARK_LUM) ? LIGHT : DARK;
    }

    /** a margin share under which an icon is drawn as it is; over it, its opaque part only */
    static final float TRIM_MIN = 0.06f;

    /**
     * the part of a w x h icon to keep, {x, y, w, h}, from its opaque pixels' bounds (l, t, r, b inclusive): null when nothing is opaque or the
     * empty margin is small (under TRIM_MIN of the side on every side)
     */
    static int[] trim(int w, int h, int l, int t, int r, int b) {
        if (r < l || b < t || w <= 0 || h <= 0) return null;
        int ml = l, mt = t, mr = w - 1 - r, mb = h - 1 - b;
        if (Math.max(Math.max(ml, mr) / (float) w, Math.max(mt, mb) / (float) h) < TRIM_MIN) return null;
        return new int[] {l, t, r - l + 1, b - t + 1};
    }

    /** a pixel's lightness (Rec. 709 luma of its sRGB channels, 0..1) */
    static float lum(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }
}
