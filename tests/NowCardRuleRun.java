package sa.urtv.home;

/**
 * For tests/now_card_close_test.py: each argument «show,playing,was,now» printed as «pausedSince visible» (NowCardRule), and the argument «hint»
 * as the paused card's hint (NowPlayingText.hintPaused).
 */
public final class NowCardRuleRun {
    public static void main(String[] args) {
        for (String a : args) {
            if (a.equals("hint")) { System.out.println(sa.urtv.home.tilawah.NowCardHint.paused()); continue; }
            String[] f = a.split(",");
            boolean show = f[0].equals("1"), playing = f[1].equals("1");
            long now = Long.parseLong(f[3]), since = NowCardRule.pausedSince(show, playing, Long.parseLong(f[2]), now);
            System.out.println(since + " " + (NowCardRule.visible(show, playing, since, now) ? 1 : 0));
        }
    }
}
