package sa.urtv.screensaver;

/**
 * The glass look of every UrTV screen (the owner's choice, «د · زجاج»): flat translucent panels over ONE small, blurred copy of the
 * home's photo, made once when the photo changes, never a live blur. Dark is smoked glass, light frosted white glass with dark ink.
 *
 * <p>The veil over the photo (the scrim) is as strong as THIS photo needs: {@link #scrimAlpha} finds the lightest veil under which
 * every text of every look reads at {@link #MIN_CONTRAST} or more, over the photo's brightest and darkest colours, so a photo of
 * the owner's own reads as well as ours. No Android in here: tests/GlassRun.java runs it over every photo we ship.
 *
 * <p>This file is the SAME in both apps (sa.urtv.home and sa.urtv.screensaver, only the package line differs):
 * tests/glass_test.py fails when the two copies differ.
 */
public final class GlassStyle {
    private GlassStyle() { }

    /** the backdrop's size: the photo at 1/8 of 1920x1080 */
    static final int W = 240, H = 135;
    /** the box blur's radius at that size, and its passes (three boxes come close to a Gaussian) */
    static final int RADIUS = 5, PASSES = 3;
    /** WCAG AA for ordinary text, asked of every text here, the small lines too */
    static final double MIN_CONTRAST = 4.5;
    /** a little more veil than the least that passes: the backdrop on the TV is scaled by Android, here by an average */
    static final float MARGIN = 0.04f;

    /** One look's colours (ARGB). */
    public static final class Look {
        public final boolean light;
        public final int base;        // the page with no photo yet: solid
        public final int scrim;       // the veil's colour over the photo (its alpha comes from scrimAlpha)
        public final float scrimMin;  // the veil is never lighter than this, whatever the photo
        public final int text, sub, accent;
        public final int warn;        // a word that warns: delete, an error
        public final int panel, line; // a tile, a row's panel, a list
        public final int dialog;      // a dialog's own fill
        public final int focus, onFocus, onFocusSub;   // the focused tile: a solid fill and its text
        public final int hint;        // the hint line's pill

        Look(boolean light, int base, int scrim, float scrimMin, int text, int sub, int accent, int warn, int panel, int line, int dialog,
             int focus, int onFocus, int onFocusSub, int hint) {
            this.light = light; this.base = base; this.scrim = scrim; this.scrimMin = scrimMin; this.text = text; this.sub = sub;
            this.accent = accent; this.warn = warn; this.panel = panel; this.line = line; this.dialog = dialog; this.focus = focus;
            this.onFocus = onFocus; this.onFocusSub = onFocusSub; this.hint = hint;
        }
    }

    /** smoked glass: white text over a dark veil and dark panels; the focused tile is solid white */
    public static final Look DARK = new Look(false, 0xFF17181B, 0x000000, 0.30f, 0xFFFFFFFF, 0xFFDADCE0, 0xFFFFFFFF, 0xFFFFC2B3,
            0x6B14161A, 0x24FFFFFF, 0xB318191D, 0xF2FFFFFF, 0xFF14161A, 0xFF3A3E46, 0xCC14161A);

    /** frosted white glass: dark ink over a white veil and white panels; the focused tile is solid ink */
    public static final Look LIGHT = new Look(true, 0xFFECEDEF, 0xF5F6F8, 0.40f, 0xFF14161A, 0xFF3A3E46, 0xFF14161A, 0xFF8E2A10,
            0x9EFFFFFF, 0x1F14161A, 0xD9FFFFFF, 0xF214161A, 0xFFFFFFFF, 0xFFDADCE0, 0xE6FFFFFF);

    public static Look of(boolean light) { return light ? LIGHT : DARK; }

    // ---------------------------------------------------------------- the other looks («المظهر ← الشكل»; the mockups in SPEC.md)
    // Each keeps the same backdrop under a strong veil of its own colour, so every look reads by the same rules over every photo.

    /** أ · فحمي: charcoal with an amber accent; the focused tile is solid amber */
    public static final Look GRAPHITE_DARK = new Look(false, 0xFF121314, 0x121314, 0.82f, 0xFFF2F0EC, 0xFFB9B6B0, 0xFFF0A64A, 0xFFFFB4A0,
            0xE6202124, 0x14FFFFFF, 0xFF1E1F22, 0xFFF0A64A, 0xFF1A1206, 0xFF3D2A0E, 0xF2202124);
    public static final Look GRAPHITE_LIGHT = new Look(true, 0xFFF3F1EE, 0xF3F1EE, 0.85f, 0xFF1C1B1A, 0xFF55514B, 0xFF8A4F00, 0xFF8E2A10,
            0xF2FFFFFF, 0x1F1C1B1A, 0xFFFFFFFF, 0xFF8A4F00, 0xFFFFFFFF, 0xFFF3E2CC, 0xF2FFFFFF);

    /** ب · كحلي: the home's night sky, deep navy with gold; the focused tile is solid gold */
    public static final Look NAVY_DARK = new Look(false, 0xFF0B1630, 0x0B1630, 0.80f, 0xFFF3EEE2, 0xFFB7BFD0, 0xFFE0BA74, 0xFFFFC2B3,
            0xD9142247, 0x1FE0BA74, 0xFF101D3D, 0xFFE0BA74, 0xFF0B1630, 0xFF2A3350, 0xF2101D3D);
    public static final Look NAVY_LIGHT = new Look(true, 0xFFEEF1F7, 0xEEF1F7, 0.85f, 0xFF14213D, 0xFF46516B, 0xFF6E5017, 0xFF8E2A10,
            0xF2FFFFFF, 0x1F14213D, 0xFFFFFFFF, 0xFF14213D, 0xFFFFFFFF, 0xFFD5DBE8, 0xF2FFFFFF);

    /** ج · نظيف: white tiles on soft grey, a deep-teal solid focus; light only: its dark is graphite's (the mockup's note) */
    public static final Look CLEAN_LIGHT = new Look(true, 0xFFECECE8, 0xF4F4F1, 0.88f, 0xFF1B1D21, 0xFF50555E, 0xFF0B5E51, 0xFF8E2A10,
            0xFAFFFFFF, 0x1414181E, 0xFFFFFFFF, 0xFF0B5E51, 0xFFFFFFFF, 0xFFCFE6E1, 0xFAFFFFFF);

    /** the looks «المظهر ← الشكل» offers, in its order; the first is the default */
    public static final String[] LOOKS = {"glass", "graphite", "navy", "clean"};
    public static final String[] LOOK_NAMES = {"زجاجي", "فحمي", "كحلي", "نظيف"};

    /** A look's key as kept; anything else (an older app, nothing set) is the glass look. */
    public static String look(String raw) {
        for (String k : LOOKS) if (k.equals(raw)) return k;
        return LOOKS[0];
    }

    /** The colours of a look in the dark or the light. */
    public static Look of(String look, boolean light) {
        switch (look(look)) {
            case "graphite": return light ? GRAPHITE_LIGHT : GRAPHITE_DARK;
            case "navy": return light ? NAVY_LIGHT : NAVY_DARK;
            case "clean": return light ? CLEAN_LIGHT : GRAPHITE_DARK;
            default: return light ? LIGHT : DARK;
        }
    }

    /** The veil's colour at this strength. */
    static int scrimColor(Look l, float alpha) {
        return (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255) << 24) | (l.scrim & 0xFFFFFF);
    }

    // ---------------------------------------------------------------- the backdrop

    /** The photo (any size, ARGB) averaged down to W x H, each pixel the mean of its block. */
    static int[] downscale(int[] src, int sw, int sh) {
        int[] out = new int[W * H];
        for (int y = 0; y < H; y++) {
            int y0 = y * sh / H, y1 = Math.max(y0 + 1, (y + 1) * sh / H);
            for (int x = 0; x < W; x++) {
                int x0 = x * sw / W, x1 = Math.max(x0 + 1, (x + 1) * sw / W);
                long r = 0, g = 0, b = 0, n = 0;
                for (int yy = y0; yy < y1; yy++) for (int xx = x0; xx < x1; xx++) {
                    int p = src[yy * sw + xx];
                    r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255; n++;
                }
                out[y * W + x] = 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
            }
        }
        return out;
    }

    /** A box blur, PASSES times each way, in place; the edges repeat their last pixel. Small: W x H only. */
    static int[] blur(int[] px, int w, int h) {
        int[] tmp = new int[px.length];
        for (int pass = 0; pass < PASSES; pass++) {
            box(px, tmp, w, h, 1, w);        // along the rows
            box(tmp, px, h, w, w, 1);        // along the columns
        }
        return px;
    }

    /** one box along lines of {@code len} pixels, {@code step} apart, {@code lines} of them, {@code lineStep} apart */
    private static void box(int[] in, int[] out, int len, int lines, int step, int lineStep) {
        int span = 2 * RADIUS + 1;
        for (int l = 0; l < lines; l++) {
            int start = l * lineStep;
            int r = 0, g = 0, b = 0;
            for (int i = -RADIUS; i <= RADIUS; i++) {
                int p = in[start + clamp(i, len) * step];
                r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255;
            }
            for (int i = 0; i < len; i++) {
                out[start + i * step] = 0xFF000000 | (r / span) << 16 | (g / span) << 8 | (b / span);
                int add = in[start + clamp(i + RADIUS + 1, len) * step], drop = in[start + clamp(i - RADIUS, len) * step];
                r += ((add >> 16) & 255) - ((drop >> 16) & 255);
                g += ((add >> 8) & 255) - ((drop >> 8) & 255);
                b += (add & 255) - (drop & 255);
            }
        }
    }

    private static int clamp(int i, int len) { return i < 0 ? 0 : i >= len ? len - 1 : i; }

    // ---------------------------------------------------------------- reading

    /**
     * The lightest veil (from the look's least, up to 0.95) under which every text reads at MIN_CONTRAST over this backdrop, plus
     * MARGIN. Checked against two colours that bound every pixel: the brightest and the darkest of each channel (a pixel under the
     * veil and a panel is never lighter than the first nor darker than the second, so this holds for every pixel of the photo).
     */
    static float scrimAlpha(int[] px, Look l) {
        int[] b = bounds(px);
        return scrimAlpha(b[0], b[1], l);
    }

    /**
     * The two colours that bound every pixel of the backdrop, {brightest, darkest} channel by channel. Kept with the backdrop (two
     * numbers), so any look's veil is worked out from them when it is drawn (scrimAlpha(hi, lo, look)), not stored once per look.
     */
    static int[] bounds(int[] px) {
        int hi = 0xFF000000, lo = 0xFFFFFFFF;
        for (int p : px) {
            hi = 0xFF000000 | Math.max(hi & 0xFF0000, p & 0xFF0000) | Math.max(hi & 0xFF00, p & 0xFF00) | Math.max(hi & 0xFF, p & 0xFF);
            lo = 0xFF000000 | Math.min(lo & 0xFF0000, p & 0xFF0000) | Math.min(lo & 0xFF00, p & 0xFF00) | Math.min(lo & 0xFF, p & 0xFF);
        }
        return new int[] {hi, lo};
    }

    /** The lightest veil for a backdrop bounded by {@code hi} and {@code lo} (bounds), plus MARGIN; the strongest, 0.95, if none reads. */
    public static float scrimAlpha(int hi, int lo, Look l) {
        for (int step = Math.round(l.scrimMin * 100); step <= 95; step++) {
            float a = step / 100f;
            if (worst(l, a, hi, lo) >= MIN_CONTRAST) return Math.min(0.95f, a + MARGIN);
        }
        return 0.95f;
    }

    /**
     * The lowest contrast of any text of the look over every pixel between {@code lo} and {@code hi} (channel by channel) under a veil of
     * this strength: the page, a panel, a dialog, the hint, the focused tile. Each background is laid over both bounds; a text whose
     * lightness falls between the two (a pixel in between could match it) counts as 1.
     */
    static double worst(Look l, float alpha, int hi, int lo) {
        double w = Double.MAX_VALUE;
        for (int[] bgs : backgrounds(l, alpha, hi, lo)) {
            int[] texts = bgs[2] == 1 ? new int[] {l.onFocus, l.onFocusSub} : new int[] {l.text, l.sub, l.accent, l.warn};
            for (int t : texts) w = Math.min(w, between(t, bgs[0], bgs[1]));
        }
        return w;
    }

    /** One pixel's worst. */
    static double worst(Look l, float alpha, int pixel) { return worst(l, alpha, pixel, pixel); }

    /** each background as {over hi, over lo, 1 for the focused tile} */
    private static int[][] backgrounds(Look l, float alpha, int hi, int lo) {
        int s = scrimColor(l, alpha);
        int pageHi = over(s, hi), pageLo = over(s, lo);
        int panelHi = over(l.panel, pageHi), panelLo = over(l.panel, pageLo);
        int dialogHi = over(l.dialog, pageHi), dialogLo = over(l.dialog, pageLo);
        return new int[][] {
            {pageHi, pageLo, 0}, {panelHi, panelLo, 0}, {dialogHi, dialogLo, 0}, {over(l.hint, pageHi), over(l.hint, pageLo), 0},
            {over(l.panel, dialogHi), over(l.panel, dialogLo), 0},   // a tile on a dialog (the Control Center's)
            {over(l.focus, panelHi), over(l.focus, panelLo), 1}, {over(l.focus, dialogHi), over(l.focus, dialogLo), 1},
            {over(l.focus, over(l.panel, dialogHi)), over(l.focus, over(l.panel, dialogLo)), 1},
        };
    }

    /** The contrast of a text over every background from {@code a} to {@code b}: 1 when the text's own lightness is between them. */
    private static double between(int text, int a, int b) {
        double lt = luminance(text), la = luminance(a), lb = luminance(b);
        if ((lt - la) * (lt - lb) < 0) return 1;
        return Math.min(contrast(text, a), contrast(text, b));
    }

    /** The solid page with no photo: its texts read too. */
    static double worstSolid(Look l) {
        return worst(l, 0f, l.base);
    }

    /** A colour laid over another (both ARGB; the result is opaque when the one under it is). */
    static int over(int top, int under) {
        float a = (top >>> 24) / 255f;
        int r = Math.round(((top >> 16) & 255) * a + ((under >> 16) & 255) * (1 - a));
        int g = Math.round(((top >> 8) & 255) * a + ((under >> 8) & 255) * (1 - a));
        int b = Math.round((top & 255) * a + (under & 255) * (1 - a));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** The WCAG contrast ratio of two opaque colours (1 to 21). */
    static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(int c) {
        double r = lin(((c >> 16) & 255) / 255.0), g = lin(((c >> 8) & 255) / 255.0), b = lin((c & 255) / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double lin(double v) { return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }
}
