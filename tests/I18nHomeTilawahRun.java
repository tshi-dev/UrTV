package sa.urtv.home.tilawah;

import sa.urtv.home.S;

/** The words of «تلاوة وأذان»'s plain-Java classes, for tests/I18nHomeRun.java (the same «key<TAB>value» lines). */
public final class I18nHomeTilawahRun {
    private static void p(String k, String v) { System.out.println(k + "\t" + (v == null ? "null" : v.replace("\n", "\\n"))); }

    public static void main(String[] args) {
        p("now.title", NowPlayingText.title(false, "Al-Fatihah", null) + " / " + NowPlayingText.title(true, null, "Station"));
        p("now.who", NowPlayingText.who(true, null) + " / " + NowPlayingText.who(false, "Reciter"));
        p("now.sub", NowPlayingText.sub(true, "Reciter", 23) + " / " + NowPlayingText.sub(false, "Reciter", 0, NowPlayingText.note(null, true)));
        p("now.hint", NowPlayingText.hint(true) + " / " + NowPlayingText.hint(false));
        for (int i = -1; i <= 6; i++) p("notice." + i, AdhanNoticeText.text(i));
        int[][] faults = {{1, PlayerFault.IO}, {1, PlayerFault.TIMED_OUT}, {1, PlayerFault.MALFORMED}, {PlayerFault.SERVER_DIED, 0}, {1, 0}};
        for (int[] f : faults) p("fault." + PlayerFault.code(f[0], f[1]), PlayerFault.message(true, f[0], f[1]) + " / " + PlayerFault.message(false, f[0], f[1]));
        p("own", S.tv(OwnAdhanText.NAME) + " / " + S.tv(OwnAdhanText.PLACE));
    }
}
