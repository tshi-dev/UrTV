package sa.urtv.home;

/** For tests/tile_art_test.py: TileArt's answers, one line each. */
public final class TileArtRun {
    public static void main(String[] a) {
        StringBuilder o = new StringBuilder();
        o.append("kind\t").append(TileArt.kind(true, true, 100, 100)).append('\t').append(TileArt.kind(false, true, 108, 108)).append('\t')
         .append(TileArt.kind(false, false, 48, 48)).append('\t').append(TileArt.kind(false, false, 400, 100)).append('\n');
        int tw = 340, th = 191;
        int[] tiny = TileArt.box(TileArt.ICON, tw, th, 32, 32), big = TileArt.box(TileArt.ICON, tw, th, 512, 512), wide = TileArt.box(TileArt.WIDE, tw, th, 400, 100),
              ad = TileArt.box(TileArt.ADAPTIVE, tw, th, 108, 108), none = TileArt.box(TileArt.ICON, tw, th, -1, -1);
        o.append("box\t").append(tiny[0]).append('x').append(tiny[1]).append('\t').append(big[0]).append('x').append(big[1]).append('\t')
         .append(wide[0]).append('x').append(wide[1]).append('\t').append(ad[0]).append('x').append(ad[1]).append('\t').append(none[0]).append('x').append(none[1]).append('\n');
        o.append("backing\t").append(Integer.toHexString(TileArt.backing(0.98f, 0.9f))).append('\t')   // a white icon (white on white)
         .append(Integer.toHexString(TileArt.backing(0.10f, 0.15f))).append('\t')                      // a transparent icon, a dark glyph
         .append(Integer.toHexString(TileArt.backing(0.95f, 0.12f))).append('\t')                      // a transparent icon, a white glyph
         .append(Integer.toHexString(TileArt.backing(0.35f, 0.9f))).append('\t')                       // a strong red icon
         .append(Integer.toHexString(TileArt.backing(-1f, 0f))).append('\n');                          // nothing read
        o.append("lum\t").append(Math.round(TileArt.lum(0xFFFFFFFF) * 100)).append('\t').append(Math.round(TileArt.lum(0xFF000000) * 100)).append('\n');
        int[] t1 = TileArt.trim(200, 200, 60, 60, 139, 139), t2 = TileArt.trim(200, 200, 4, 4, 195, 195), t3 = TileArt.trim(200, 200, 0, 0, -1, -1);
        o.append("trim\t").append(t1 == null ? "null" : t1[0] + "," + t1[1] + "," + t1[2] + "," + t1[3]).append('\t').append(t2 == null ? "null" : "cut").append('\t').append(t3 == null ? "null" : "cut").append('\n');
        int[] tops = {0, 250, 500, 750, 1000, 1250}, bottoms = {220, 470, 720, 970, 1220, 1470};   // rows of 220 with 30 between
        int safeTop = 108, safeEnd = 1080 - 86;   // a 1080 screen
        o.append("grid\t").append(GridWindow.first(tops, bottoms, 1, 0, safeTop, safeEnd)).append('\t')   // row 1 fits with row 0 first
         .append(GridWindow.first(tops, bottoms, 3, 0, safeTop, safeEnd)).append('\t')                      // row 3: first moves to 1
         .append(GridWindow.first(tops, bottoms, 4, 1, safeTop, safeEnd)).append('\t')                      // row 4 from first 1: 2
         .append(GridWindow.first(tops, bottoms, 1, 2, safeTop, safeEnd)).append('\t')                      // back up to 1: it is first
         .append(GridWindow.first(tops, bottoms, 2, 1, safeTop, safeEnd)).append('\n');                     // inside the window: no move
        System.out.print(o);
    }
}
