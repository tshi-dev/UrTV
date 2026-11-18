package sa.urtv.home;

/** Prints «زجاجي»'s geometry and grid steps (FavBar.glass*) at 1080p and 720p for tests/glass_dock_test.py. */
public class GlassDockRun {
    public static void main(String[] a) {
        StringBuilder o = new StringBuilder();
        o.append("look ").append(FavBar.look("glass")).append(' ').append(FavBar.zoom("glass") ? 1 : 0).append('\n');
        for (String v : new String[] {"50", "70", "100", "150", "40", "160", "x", ""}) o.append("k ").append(v.isEmpty() ? "-" : v).append(' ').append(FavBar.glassK(v)).append('\n');
        for (String v : new String[] {"1", "2", "3", "0", "4", "x"}) o.append("rows ").append(v).append(' ').append(FavBar.glassRows(v)).append('\n');
        for (int h : new int[] {1080, 720}) {
            int tray = FavBar.px(1920 - 2 * FavBar.SIDE, h);
            for (String pct : new String[] {"50", "70", "100", "150"}) {
                float k = FavBar.glassK(pct);
                for (int rows = 1; rows <= 3; rows++) {
                    o.append("geo ").append(h).append(' ').append(pct).append(' ').append(rows)
                     .append(" cell ").append(FavBar.glassCell(h, k)).append(" gap ").append(FavBar.glassGap(h, k)).append(" pad ").append(FavBar.glassPad(h, k))
                     .append(" radius ").append(FavBar.glassRadius(h, k)).append(" grow ").append(FavBar.glassGrow(h, k))
                     .append(" row11 ").append(FavBar.glassRowH(h, k, rows, 11)).append(" w11 ").append(FavBar.glassBarW(h, k, rows, 11))
                     .append(" w6 ").append(FavBar.glassBarW(h, k, rows, 6)).append(" tray ").append(tray).append(" bottom ").append(FavBar.px(FavBar.REST_BOTTOM, h)).append('\n');
                }
            }
        }
        // the grid: 6 in 2 rows (3 columns), 5 in 2 rows (the last row shorter), 11 in 3 rows
        o.append("side ").append(FavBar.glassSideStep(2, 6, 2, true, false)).append(' ').append(FavBar.glassSideStep(1, 6, 2, true, false)).append(' ')
         .append(FavBar.glassSideStep(3, 6, 2, true, true)).append(' ').append(FavBar.glassSideStep(3, 6, 2, false, true)).append(' ').append(FavBar.glassSideStep(4, 6, 1, true, false)).append('\n');
        o.append("updown ").append(FavBar.glassUpDown(1, 5, 2, true)).append(' ').append(FavBar.glassUpDown(2, 5, 2, true)).append(' ').append(FavBar.glassUpDown(4, 5, 2, false)).append(' ')
         .append(FavBar.glassUpDown(4, 5, 2, true)).append(' ').append(FavBar.glassUpDown(0, 5, 2, false)).append(' ').append(FavBar.glassUpDown(0, 3, 1, true)).append('\n');
        o.append("cols ").append(FavBar.glassCols(11, 1)).append(' ').append(FavBar.glassCols(11, 2)).append(' ').append(FavBar.glassCols(11, 3)).append(' ').append(FavBar.glassCols(2, 3))
         .append(' ').append(FavBar.glassRowsUsed(2, 3)).append(' ').append(FavBar.glassCellAt(7, 11, 2)[0]).append(' ').append(FavBar.glassCellAt(7, 11, 2)[1]).append('\n');
        System.out.print(o);
    }
}
