package sa.urtv.home;

/**
 * For tests/snapshot_own_test.py: made-up 32 x 18 grids, SnapshotOwn.same printed for each case (1 or 0), one per line.
 *   home: a wallpaper of soft greens and the bar (the drawing); the copies: the same, the same with a page card the drawing left empty,
 *   the same a little brighter (the copy's colours), another app's white page, another app's dark video, nothing drawn.
 */
public final class SnapshotOwnRun {
    static int rgb(int r, int g, int b) { return 0xFF000000 | (r << 16) | (g << 8) | b; }
    public static void main(String[] args) {
        int n = SnapshotOwn.GRID * SnapshotOwn.ROWS;
        int[] home = new int[n], card = new int[n], bright = new int[n], white = new int[n], video = new int[n];
        for (int y = 0; y < SnapshotOwn.ROWS; y++) for (int x = 0; x < SnapshotOwn.GRID; x++) {
            int i = y * SnapshotOwn.GRID + x;
            home[i] = y >= 14 ? rgb(20, 20, 20) : rgb(40 + x * 3, 90 + y * 4, 60);   // the photo above, the bar below
            card[i] = (x >= 20 && x < 30 && y >= 2 && y < 7) ? rgb(250, 250, 250) : home[i];   // a card the drawing has empty: white in the copy
            bright[i] = rgb(Math.min(255, ((home[i] >> 16) & 0xFF) + 15), Math.min(255, ((home[i] >> 8) & 0xFF) + 15), Math.min(255, (home[i] & 0xFF) + 15));
            white[i] = y < 2 ? rgb(30, 30, 30) : rgb(245, 245, 245);
            video[i] = rgb((x * 37 + y * 11) % 256, (x * 13 + y * 29) % 256, (x * 7 + y * 53) % 256);
        }
        System.out.println(SnapshotOwn.same(home, home) ? 1 : 0);
        System.out.println(SnapshotOwn.same(card, home) ? 1 : 0);
        System.out.println(SnapshotOwn.same(bright, home) ? 1 : 0);
        System.out.println(SnapshotOwn.same(white, home) ? 1 : 0);
        System.out.println(SnapshotOwn.same(video, home) ? 1 : 0);
        System.out.println(SnapshotOwn.same(new int[n], null) ? 1 : 0);
    }
}
