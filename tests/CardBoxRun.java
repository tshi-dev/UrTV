package sa.urtv.home;

/** Prints CardBox's answers for the cases tests/card_box_test.py reads: one line "name w h" in pixels each. */
public class CardBoxRun {
    public static void main(String[] a) {
        // the same 1920 x 1080 pixels as a 960 dp screen at density 2, a 1280 dp one at 1.5 and a 1920 dp one at 1
        float[] density = {2f, 1.5f, 1f};
        for (float d : density) {
            int w = 1920, h = 1080;
            // the model of the home screen: the clock block 300 dp wide at the right margin; the dock's tile is a fifth of the
            // screen wide plus 12%, 16:9, in a band with 22 dp padding; the title 62 dp above it; 30 dp under the dock
            int clockLeft = Math.round(w - 48 * d - 300 * d);
            int tile = Math.round(w / 5f * 1.12f), band = Math.round(tile * 9 / 16f + 44 * d);
            int block = Math.round(62 * d) + band, titleTop = h - Math.round(30 * d) - block, dockTop = titleTop + Math.round(62 * d);
            for (int n = 1; n <= 3; n++) {
                int[] early = CardBox.box(n, w, h, d, 0, 0, 0, 0);
                int[] short_ = CardBox.box(n, w, h, d, clockLeft, titleTop, dockTop, Math.round(200 * d));
                int[] long_ = CardBox.box(n, w, h, d, clockLeft, titleTop, dockTop, Math.round(420 * d));
                System.out.println("early " + d + " " + n + " " + early[0] + " " + early[1]);
                System.out.println("short " + d + " " + n + " " + short_[0] + " " + short_[1] + " " + clockLeft + " " + titleTop + " " + dockTop);
                System.out.println("long " + d + " " + n + " " + long_[0] + " " + long_[1] + " " + clockLeft + " " + titleTop + " " + dockTop);
            }
        }
        // 4.4 the strip with the big glass clock (its digits end at 542 of 1080): the owner's TV (the title at 683 px) and the model's
        for (int titleTop : new int[] {683, 0}) {
            int[] plain = CardBox.row(1920, 1080, 1.5f, titleTop), clock = CardBox.row(1920, 1080, 1.5f, titleTop, 542);
            int endP = 1080 - plain[2], endC = 1080 - clock[2];
            int[] stripP = {Math.round(48 * 1.5f), endP - plain[1], 1920 - Math.round(48 * 1.5f), endP}, stripC = {Math.round(48 * 1.5f), endC - clock[1], 1920 - Math.round(48 * 1.5f), endC};
            int[] box = {601, 266, 1319, 542};
            System.out.println("strip " + titleTop + " " + plain[1] + " " + clock[1] + " " + CardBox.overClock(stripP, box) + " " + CardBox.overClock(stripC, box) + " " + stripC[1]);
        }
    }
}
