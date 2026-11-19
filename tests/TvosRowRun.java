package sa.urtv.home;

/** Prints CardBox.tvosRow and tvosSlots at 1080p and 720p, plain and with the large font, for tests/tvos_home_test.py. */
public class TvosRowRun {
    public static void main(String[] a) {
        StringBuilder o = new StringBuilder();
        for (int[] s : new int[][] {{1920, 1080}, {1280, 720}}) {
            for (float f : new float[] {1f, 1.3f}) {
                int titleTop = Math.round(s[1] * 0.80f);   // the name and the bar's block starts at 80 % of the height
                int[] r = CardBox.tvosRow(s[0], s[1], titleTop, f);
                o.append("row ").append(s[1]).append(' ').append(f).append(' ').append(r[0]).append(' ').append(r[1]).append(' ').append(r[2])
                 .append(' ').append(titleTop).append(' ').append(FavBar.px(FavBar.SIDE, s[1])).append(' ').append(FavBar.px(CardBox.TVOS_GAP, s[1]))
                 .append(' ').append(CardBox.tvosSlots(s[1], f)).append('\n');
            }
        }
        System.out.print(o);
    }
}
