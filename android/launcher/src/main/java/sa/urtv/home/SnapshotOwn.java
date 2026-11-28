package sa.urtv.home;

/**
 * 4.4 beta 4 (a user: the picture the home shows at a cold start was another app's screen): PixelCopy copies the window so that the WebView cards
 * are in the picture, but on some TVs the copy taken as the home leaves for an app already holds that app. Each copy is now held against a
 * small drawing of the home's own views (the wallpaper, the bar, the tiles: View.draw, which never sees another window): on a grid of
 * GRID x GRID/16*9 cells, the copy is the home's when its light differs from the drawing's by at most MAX_DIFF on average. A page card the
 * drawing leaves empty is a small part of the grid, so the home's own copy passes; another app's screen does not. No Android in here
 * (tests/SnapshotOwnRun.java).
 */
final class SnapshotOwn {
    private SnapshotOwn() { }

    static final int GRID = 32, ROWS = 18;
    /** the mean difference in light (0 to 255) a copy may have from the home's own drawing */
    static final int MAX_DIFF = 40;

    /** light (0 to 255) of an ARGB colour */
    static int light(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    /** the copy and the drawing, each GRID x ROWS ARGB samples in the same order: the copy is the home's own */
    static boolean same(int[] copy, int[] own) {
        if (copy == null || own == null || copy.length != own.length || copy.length == 0) return false;
        long sum = 0;
        for (int i = 0; i < copy.length; i++) sum += Math.abs(light(copy[i]) - light(own[i]));
        return sum / copy.length <= MAX_DIFF;
    }
}
