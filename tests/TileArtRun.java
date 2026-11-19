package sa.urtv.home;

/** For tests/tile_art_test.py: TileArt's and GridWindow's answers, one line each. */
public final class TileArtRun {
    public static void main(String[] a) {
        StringBuilder o = new StringBuilder();
        o.append("kind\t").append(TileArt.kind(true, true, 100, 100)).append('\t').append(TileArt.kind(false, true, 108, 108)).append('\t')
         .append(TileArt.kind(false, false, 48, 48)).append('\t').append(TileArt.kind(false, false, 400, 100)).append('\n');
        int tw = 340, th = 191;
        int[][] b = {TileArt.box(TileArt.ICON, tw, th, 32, 32), TileArt.box(TileArt.ICON, tw, th, 512, 512), TileArt.box(TileArt.WIDE, tw, th, 400, 100),
                     TileArt.box(TileArt.ADAPTIVE, tw, th, 108, 108), TileArt.box(TileArt.ICON, tw, th, -1, -1)};
        o.append("box");
        for (int[] x : b) o.append('\t').append(x[0]).append('x').append(x[1]);
        o.append('\n');
        int[] white = new int[40], half = new int[40], red = new int[40];
        for (int i = 0; i < 40; i++) { white[i] = 0xFFFFFFFF; half[i] = i % 2 == 0 ? 0xFF102030 : 0x00000000; red[i] = 0xFFE50914; }
        o.append("edge\t").append(Integer.toHexString(TileArt.edgeColour(white))).append('\t').append(Integer.toHexString(TileArt.edgeColour(half))).append('\t')
         .append(Integer.toHexString(TileArt.edgeColour(red))).append('\n');
        o.append("backing\t").append(Integer.toHexString(TileArt.backing(0.98f, 0.9f))).append('\t').append(Integer.toHexString(TileArt.backing(0.10f, 0.15f))).append('\t')
         .append(Integer.toHexString(TileArt.backing(0.95f, 0.12f))).append('\t').append(Integer.toHexString(TileArt.backing(-1f, 0f))).append('\n');
        int[] t1 = TileArt.trim(200, 200, 60, 60, 139, 139), t2 = TileArt.trim(200, 200, 4, 4, 195, 195), t3 = TileArt.trim(200, 200, 0, 0, -1, -1);
        o.append("trim\t").append(t1 == null ? "null" : t1[0] + "," + t1[1] + "," + t1[2] + "," + t1[3]).append('\t').append(t2 == null ? "null" : "cut").append('\t').append(t3 == null ? "null" : "cut").append('\n');
        o.append("lum\t").append(Math.round(TileArt.lum(0xFFFFFFFF) * 100)).append('\t').append(Math.round(TileArt.lum(0xFF000000) * 100)).append('\n');
        int[] tops = {0, 250, 500, 750, 1000, 1250}, bottoms = {220, 470, 720, 970, 1220, 1470};
        int safeTop = 108, safeEnd = 1080 - 86;
        o.append("grid\t").append(GridWindow.first(tops, bottoms, 1, 0, safeTop, safeEnd)).append('\t').append(GridWindow.first(tops, bottoms, 3, 0, safeTop, safeEnd)).append('\t')
         .append(GridWindow.first(tops, bottoms, 4, 1, safeTop, safeEnd)).append('\t').append(GridWindow.first(tops, bottoms, 1, 2, safeTop, safeEnd)).append('\t')
         .append(GridWindow.first(tops, bottoms, 2, 1, safeTop, safeEnd)).append('\n');
        System.out.print(o);
    }
}
