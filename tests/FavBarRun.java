package sa.urtv.home;

/** Prints FavBar's geometry at 1080p and 720p for tests/fav_bar_test.py. */
public class FavBarRun {
    public static void main(String[] a) {
        StringBuilder o = new StringBuilder();
        for (int h : new int[] {1080, 720}) {
            int w = h * 16 / 9;
            int x0 = FavBar.px(FavBar.SIDE, h), x1 = w - FavBar.px(FavBar.SIDE, h);
            int bottom = h - FavBar.px(FavBar.BOTTOM, h), top = bottom - FavBar.px(FavBar.TRAY_H, h);
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
             .append(" five ").append(five).append(" width ").append(x1 - x0)
             .append(" topRoom ").append(padT - overTop).append(" sideRoom ").append(padS - overSide)
             .append(" liftedTopRoom ").append(padT - (tileH * lifted + FavBar.px(FavBar.LIFT, h)))
             .append(" dropped ").append(bottom + FavBar.px(FavBar.WIDGET_DROP, h)).append(" screen ").append(h).append('\n');
        }
        o.append("tile1080 ").append(FavBar.tileW(1080)).append('\n');
        o.append("nameX ").append(FavBar.nameX(500, 0, 200, 1680)).append(' ').append(FavBar.nameX(50, 0, 200, 1680)).append(' ')
         .append(FavBar.nameX(1670, 0, 200, 1680)).append('\n');
        // tester step 20: Arabic (drawn right to left) ▶ goes to the item before, English (left to right) to the next; the edges stay
        o.append("side ").append(FavBar.sideStep(0, 5, true, false)).append(' ').append(FavBar.sideStep(0, 5, false, false)).append(' ')
         .append(FavBar.sideStep(2, 5, true, true)).append(' ').append(FavBar.sideStep(0, 5, true, true)).append(' ')
         .append(FavBar.sideStep(0, 5, false, true)).append(' ').append(FavBar.sideStep(4, 5, true, false)).append('\n');
        System.out.print(o);
    }
}
