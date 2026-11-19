package sa.urtv.home;

/**
 * 4.4 beta 2, the owner's test of build 525 («the icons need to be 100% perfect»): how an app with no TV banner is drawn on a tile. No Android in
 * the decisions (tests/tile_art_test.py runs them on a JVM); MainActivity.composeTile draws them, off the main thread, once per app.
 *
 * A banner fills the tile (its own art, unchanged). Every other app: its own icon, large, on a flat neutral backing, never a tint of its colour:
 *  - an adaptive icon (Android 8+) is masked to a rounded square as a launcher shows it (its outer safe zone cropped), ICON_H of the tile's height;
 *  - a square legacy icon fits the same box; a tiny one is enlarged up to MAX_UP times (never more: past that it blurs);
 *  - a wide logo (wider than WIDE_ASPECT) fits WIDE_W of the width and WIDE_H of the height;
 *  - the backing is whichever of LIGHT or DARK differs more from the icon's own mean lightness, so a white icon (white on white) or a
 *    transparent glyph (only its strokes count) always stands out.
 */
final class TileArt {
    private TileArt() { }

    static final String BANNER = "banner", ADAPTIVE = "adaptive", ICON = "icon", WIDE = "wide";
    /** the two backings: a warm light grey, and a dark slate (no colour of the app's own, never muddy) */
    static final int LIGHT = 0xFFECE9E3, DARK = 0xFF2B2E36;
    static final float LIGHT_LUM = 0.91f, DARK_LUM = 0.17f;
    /** a square icon's height on the tile, as a share of the tile's height */
    static final float ICON_H = 0.62f;
    /** a wide logo's box on the tile */
    static final float WIDE_W = 0.72f, WIDE_H = 0.5f, WIDE_ASPECT = 1.3f;
    /** a small bitmap icon is enlarged at most this much */
    static final float MAX_UP = 3f;
    /** an adaptive icon's visible part (the launcher's mask) is the middle 72 of its 108: drawn larger by 108/72 and clipped to the box */
    static final float ADAPTIVE_BLEED = 108f / 72f;
    /** the rounded square's corner, as a share of its side */
    static final float CORNER = 0.22f;

    /** what the app shows: its banner, else its icon by its shape */
    static String kind(boolean hasBanner, boolean adaptive, int iw, int ih) {
        if (hasBanner) return BANNER;
        if (adaptive) return ADAPTIVE;
        if (iw > 0 && ih > 0 && iw > ih * WIDE_ASPECT) return WIDE;
        return ICON;
    }

    /** the icon's drawn size {w, h} on a tile of tw x th (an adaptive icon: the box it is clipped to) */
    static int[] box(String kind, int tw, int th, int iw, int ih) {
        if (tw <= 0 || th <= 0) return new int[] {0, 0};
        if (ADAPTIVE.equals(kind)) { int s = Math.round(th * ICON_H); return new int[] {s, s}; }
        float maxW = WIDE.equals(kind) ? tw * WIDE_W : th * ICON_H, maxH = WIDE.equals(kind) ? th * WIDE_H : th * ICON_H;
        if (iw <= 0 || ih <= 0) { int s = Math.round(th * ICON_H); return new int[] {s, s}; }
        float f = Math.min(maxW / iw, maxH / ih);
        f = Math.min(f, MAX_UP);
        return new int[] {Math.max(1, Math.round(iw * f)), Math.max(1, Math.round(ih * f))};
    }

    /** the backing that stands out from an icon whose opaque pixels have this mean lightness (0 black, 1 white); none read: LIGHT */
    static int backing(float meanLum, float opaqueShare) {
        if (opaqueShare <= 0f || meanLum < 0f) return LIGHT;
        return Math.abs(meanLum - LIGHT_LUM) >= Math.abs(meanLum - DARK_LUM) ? LIGHT : DARK;
    }

    /** a margin share under which an icon is drawn as it is (most icons); over it, its opaque part only */
    static final float TRIM_MIN = 0.12f;

    /**
     * the part of a w x h icon to keep, {x, y, w, h}, from its opaque pixels' bounds (l, t, r, b inclusive): null when nothing is opaque or the
     * empty margin is small (under TRIM_MIN of the side on every side); a small gap is kept around the opaque part so it never touches the box
     */
    static int[] trim(int w, int h, int l, int t, int r, int b) {
        if (r < l || b < t || w <= 0 || h <= 0) return null;
        int ml = l, mt = t, mr = w - 1 - r, mb = h - 1 - b;
        if (Math.max(Math.max(ml, mr) / (float) w, Math.max(mt, mb) / (float) h) < TRIM_MIN) return null;
        int gap = Math.max(1, Math.round(Math.max(r - l + 1, b - t + 1) * 0.04f));
        int x = Math.max(0, l - gap), y = Math.max(0, t - gap), x2 = Math.min(w - 1, r + gap), y2 = Math.min(h - 1, b + gap);
        return new int[] {x, y, x2 - x + 1, y2 - y + 1};
    }

    /** a pixel's lightness (Rec. 709 luma of its sRGB channels, 0..1) */
    static float lum(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }
}
