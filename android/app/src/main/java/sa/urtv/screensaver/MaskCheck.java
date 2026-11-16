package sa.urtv.screensaver;

/**
 * 4.4 the checks of a person's mask for the glass clock (docs/OWN-PHOTO-MASK.md), plain Java (tests/MaskCheckRun.java runs them): the photo's shape
 * within 2 %, and mostly black and white with some of each (a grey or coloured picture, or a blank one, is not a mask); the same gate our own cut-outs
 * pass in CI (.github/tools/photo_masks.py), in a light form: the scenery covers a fair share with sky along the top (cover), and its top edge is
 * not ragged (smooth). Run on the mask made small (about 120 px high), once, when it arrives.
 */
final class MaskCheck {
    private MaskCheck() { }

    /** w x h is the shape of pw x ph within 2 % */
    static boolean sameShape(int w, int h, int pw, int ph) {
        if (w <= 0 || h <= 0 || pw <= 0 || ph <= 0) return false;
        double a = w / (double) h, b = pw / (double) ph;
        return Math.abs(a - b) <= 0.02 * b;
    }

    /** ARGB pixels: at most 15 % between dark and light (luma 60 to 195), and at least 3 % of each */
    static boolean twoTone(int[] px) {
        if (px.length == 0) return false;
        int dark = 0, light = 0, mid = 0;
        for (int c : px) {
            int y = (((c >> 16) & 255) * 299 + ((c >> 8) & 255) * 587 + (c & 255) * 114) / 1000;
            if (y < 60) dark++; else if (y > 195) light++; else mid++;
        }
        return mid <= px.length * 0.15 && dark >= px.length * 0.03 && light >= px.length * 0.03;
    }

    private static boolean light(int c) { return (((c >> 16) & 255) * 299 + ((c >> 8) & 255) * 587 + (c & 255) * 114) / 1000 > 127; }

    /** w x h ARGB pixels (light = in front of the clock): the scenery 8 % to 85 % of it, and the top row at least 80 % sky */
    static boolean cover(int[] px, int w, int h) {
        if (w <= 0 || h <= 0 || px.length != w * h) return false;
        int front = 0, topSky = 0;
        for (int i = 0; i < px.length; i++) if (light(px[i])) front++;
        for (int x = 0; x < w; x++) if (!light(px[x])) topSky++;
        return front >= px.length * 0.08 && front <= px.length * 0.85 && topSky >= w * 0.8;
    }

    /** the scenery's top, column by column, has few big jumps (at most 8 % of the columns jump by more than a fifth of the height): not ragged */
    static boolean smooth(int[] px, int w, int h) {
        if (w < 2 || h <= 0 || px.length != w * h) return false;
        int prev = -1, jumps = 0;
        for (int x = 0; x < w; x++) {
            int y = 0;
            while (y < h && !light(px[y * w + x])) y++;
            if (prev >= 0 && Math.abs(y - prev) > h / 5) jumps++;
            prev = y;
        }
        return jumps <= (w - 1) * 0.08;
    }
}
