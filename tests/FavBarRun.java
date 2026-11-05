package sa.urtv.home;

/** Prints FavBar's geometry at 1080p and 720p for tests/fav_bar_test.py. */
public class FavBarRun {
    public static void main(String[] a) {
        StringBuilder o = new StringBuilder();
        for (int h : new int[] {1080, 720}) {
            int w = h * 16 / 9;
            int x0 = FavBar.px(FavBar.SIDE, h), x1 = w - FavBar.px(FavBar.SIDE, h);
            int bottom = h - FavBar.px(FavBar.REST_BOTTOM, h), top = bottom - FavBar.px(FavBar.TRAY_H, h);
            int tileW = FavBar.tileW(h), tileH = FavBar.px(FavBar.TILE_H, h), gap = FavBar.px(FavBar.GAP, h);
            int padS = FavBar.px(FavBar.PAD_SIDE, h), padT = FavBar.px(FavBar.PAD_TOP, h);
            int five = FavBar.SHOWN * tileW + (FavBar.SHOWN - 1) * gap + 2 * padS;
            // the focused tile grown from its centre, lifted, with its ring: how far its top is from the tray's top, its side from the tray's side
            float grown = (FavBar.FOCUS_SCALE - 1) / 2f;
            float overTop = tileH * grown + FavBar.px(FavBar.LIFT, h) + FavBar.px(FavBar.RING, h);
            float overSide = tileW * grown + FavBar.px(FavBar.RING, h);
            float lifted = (FavBar.LIFTED_SCALE - 1) / 2f;
            o.append(h).append(" tray ").append(x0).append(' ').append(x1).append(' ').append(top).append(' ').append(bottom)
             .append(" fits ").append(padT * 2 + tileH <= FavBar.px(FavBar.TRAY_H, h) + 1 ? 1 : 0)
             .append(" five ").append(five).append(" width ").append(x1 - x0).append(" padS ").append(padS).append(" padT ").append(padT)
             .append(" topRoom ").append(padT - overTop).append(" sideRoom ").append(padS - overSide)
             .append(" liftedTopRoom ").append(padT - (tileH * lifted + FavBar.px(FavBar.LIFT, h)))
             .append(" dropped ").append(bottom + FavBar.px(FavBar.WIDGET_DROP, h)).append(" safe ").append(h - FavBar.px(FavBar.BOTTOM, h)).append(" screen ").append(h).append('\n');
        }
        o.append("tile1080 ").append(FavBar.tileW(1080)).append('\n');
        // «تطبيقات في الشريط»: N whole tiles with 24 gaps and 24 insets, at 1080p and 720p: width, the row they make, the ring room of the focused one
        for (int h : new int[] {1080, 720}) {
            for (int n : new int[] {5, 6, 7, 8, 9, 10}) {
                int tw = FavBar.tileW(h, n), th = tw * 9 / 16, inner = FavBar.px(1920 - 2 * FavBar.SIDE, h) - 2 * FavBar.px(FavBar.PAD_SIDE, h);
                int row = n * tw + (n - 1) * FavBar.px(FavBar.GAP, h);
                float over = tw * (FavBar.FOCUS_SCALE - 1) / 2f + FavBar.px(FavBar.RING, h);
                o.append("bar ").append(h).append(' ').append(n).append(" tile ").append(tw).append(" row ").append(row).append(" inner ").append(inner)
                 .append(" slack ").append(inner - row).append(" ratio ").append(Math.abs(tw * 9 / 16f - th) < 1 ? 1 : 0)
                 .append(" ringRoom ").append(FavBar.px(FavBar.PAD_SIDE, h) - over >= 0 ? 1 : 0);
                int sum = 0;
                for (int i = 0; i < n; i++) sum += FavBar.tileW(h, n, i);
                o.append(" fill ").append(inner - sum - (n - 1) * FavBar.px(FavBar.GAP, h)).append('\n');   // 0: the cells with their gaps are exactly the inner width, no sliver of the next tile
            }
        }
        o.append("trayh ").append(FavBar.trayH(1080, 5)).append(' ').append(FavBar.trayH(1080, 7)).append(' ').append(FavBar.trayH(1080, 8)).append(" lift ")
         .append(FavBar.lift(1080, 5)).append(' ').append(FavBar.lift(1080, 6)).append(' ').append(FavBar.lift(1080, 7)).append('\n');   // step 28: the tray is as high as its tiles; the lift only for 5 and 6
        o.append("shown ").append(FavBar.shown("5", 7)).append(' ').append(FavBar.shown("7", 3)).append(' ').append(FavBar.shown("10", 10)).append(' ').append(FavBar.shown("all", 8)).append(' ')
         .append(FavBar.shown("all", 3)).append(' ').append(FavBar.shown("all", 12)).append(' ').append(FavBar.shown("12", 12)).append(' ').append(FavBar.shown("4", 12)).append(' ').append(FavBar.shown("x", 12)).append(' ')
         .append(FavBar.shown(null, 12)).append('\n');
        o.append("nameX ").append(FavBar.nameX(500, 0, 200, 1680)).append(' ').append(FavBar.nameX(50, 0, 200, 1680)).append(' ')
         .append(FavBar.nameX(1670, 0, 200, 1680)).append('\n');
        // tester step 20: Arabic (drawn right to left) ▶ goes to the item before, English (left to right) to the next; the edges stay
        o.append("side ").append(FavBar.sideStep(0, 5, true, false)).append(' ').append(FavBar.sideStep(0, 5, false, false)).append(' ')
         .append(FavBar.sideStep(2, 5, true, true)).append(' ').append(FavBar.sideStep(0, 5, true, true)).append(' ')
         .append(FavBar.sideStep(0, 5, false, true)).append(' ').append(FavBar.sideStep(4, 5, true, false)).append('\n');
        // build 179 (tester step 23): seven favourites, tile 304 / gap 24 / inset 24, tray 1664 wide, content 24 + 7*304 + 6*24 + 24 = 2320
        o.append("scroll ").append(FavBar.scrollDelta(1000, 1304, 656, 1664, 24, 2320)).append(' ')   // whole inside already: stays
         .append(FavBar.scrollDelta(656 + 1664 - 24, 656 + 1664 + 280, 656, 1664, 24, 2320)).append(' ')   // clamped at the end
         .append(FavBar.scrollDelta(328 - 328, 304, 656, 1664, 24, 2320)).append(' ')   // the left end
         .append(FavBar.scrollDelta(680 - 328, 680 - 24, 656, 1664, 24, 2320)).append(' ')   // the next tile to the left: one pitch (328) back
         .append(FavBar.scrollDelta(0, 304, 0, 1664, 24, 1664)).append('\n');   // five or fewer: nothing scrolls
        System.out.print(o);
    }
}
