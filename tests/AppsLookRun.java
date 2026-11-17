package sa.urtv.home;

/** Prints the apps row's three ways (FavBar.look, cellW, rowH, centredStart) at 1080p and 720p for tests/apps_look_test.py. */
public class AppsLookRun {
    public static void main(String[] a) {
        StringBuilder o = new StringBuilder();
        for (String v : new String[] {"banners", "icons", "tiles", "frame", "zoom", "", "nonsense"})
            o.append("look ").append(v.isEmpty() ? "-" : v).append(' ').append(FavBar.look(v)).append(' ').append(FavBar.zoom(v) ? 1 : 0).append('\n');
        for (int h : new int[] {1080, 720}) {
            int w = h * 16 / 9, tray = FavBar.px(1920 - 2 * FavBar.SIDE, h);
            for (String way : new String[] {"banners", "icons", "tiles"}) {
                o.append(h).append(' ').append(way).append(" cell ").append(FavBar.cellW(way, h, 5, 0)).append(" aspect ").append(FavBar.aspect(way))
                 .append(" gap ").append(FavBar.gap(way, h)).append(" radius ").append(FavBar.radius(way, h)).append(" row ").append(FavBar.rowH(way, h, 5));
                for (int n : new int[] {3, 6, 9, 14}) o.append(" start").append(n).append(' ').append(FavBar.centredStart(way, h, n, tray));
                o.append(" tray ").append(tray).append(" screen ").append(w).append('\n');
            }
        }
        System.out.print(o);
    }
}
