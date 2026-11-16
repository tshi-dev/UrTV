package sa.urtv.screensaver;

/**
 * 4.4 the checks of a person's mask for the glass clock (docs/OWN-PHOTO-MASK.md), plain Java (tests/MaskCheckRun.java runs them): the photo's shape
 * within 2 %, and mostly black and white with some of each (a grey or coloured picture, or a blank one, is not a mask).
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
}
