package sa.urtv.home;

/**
 * For tests/clean_prayer_card_test.py: PrayerCardBox on the home's model at 1920x1080 and 1280x720, both directions, three places. One line each:
 * "pos rtl w h d l t r b clockL clockT clockR clockB titleTop quickL quickT quickR quickB adhanB".
 * The model: the clock block (greeting with the Wi-Fi mark, clock, date, the prayer line) 300 dp wide, 150 dp high, 48 dp from the start edge and 36 dp from the top; the quick buttons
 * (clean mode: the Wi-Fi mark and the settings button) 160 dp wide, 56 dp high, 48 dp from the end edge, 36 dp from the top; the adhan bar 26 dp over the top edge; the title 62 dp above
 * the dock band; 30 dp under the dock.
 */
public class PrayerCardBoxRun {
    public static void main(String[] a) {
        int[][] screens = {{1920, 1080}, {1280, 720}};
        float[][] dens = {{2f, 1.5f, 1f}, {1.3333f, 1f, 0.75f}};
        for (int s = 0; s < 2; s++) for (float d : dens[s]) for (int rtl = 0; rtl < 2; rtl++) {
            int w = screens[s][0], h = screens[s][1];
            int cl = rtl == 1 ? w - Math.round(48 * d) - Math.round(300 * d) : Math.round(48 * d), ct = Math.round(36 * d);
            int[] clock = {cl, ct, cl + Math.round(300 * d), ct + Math.round(150 * d)};
            int ql = rtl == 1 ? Math.round(48 * d) : w - Math.round(48 * d) - Math.round(160 * d);
            int[] quick = {ql, ct, ql + Math.round(160 * d), ct + Math.round(56 * d)};
            int[] adhan = {0, 0, w, Math.round(26 * d)};
            int tile = Math.round(w / 5f * 1.12f), band = Math.round(tile * 9 / 16f + 44 * d);
            int titleTop = h - Math.round(30 * d) - Math.round(62 * d) - band;
            for (String p : new String[] {"center", "below", "opposite"}) {
                int[] r = PrayerCardBox.place(p, rtl == 1, w, h, d, clock, new int[][] {quick, adhan}, titleTop);
                System.out.println(p + " " + rtl + " " + w + " " + h + " " + d + " " + r[0] + " " + r[1] + " " + r[2] + " " + r[3] + " " + clock[0] + " " + clock[1] + " " + clock[2] + " " + clock[3] + " " + titleTop
                    + " " + quick[0] + " " + quick[1] + " " + quick[2] + " " + quick[3] + " " + adhan[3]);
            }
        }
    }
}
