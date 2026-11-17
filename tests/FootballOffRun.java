package sa.urtv.home;

/** tests/football_off_test.py: HomeCardChoice.football on a plain JVM. One line per case: «ok …» or «BAD …». */
public final class FootballOffRun {
    static int bad;
    static void check(String n, boolean ok) { System.out.println((ok ? "ok " : "BAD ") + n); if (!ok) bad++; }

    public static void main(String[] a) {
        String[] cards = {"city=Riyadh", "prayers,hourly"}, none = {"city=Riyadh", "none"}, empty = {"city=Riyadh", ""}, nul = {"city=Riyadh", null};
        check("football layout with cards: the football home, as before", HomeCardChoice.football("football", cards));
        check("football layout with «بدون»: off (the widgets are the cards)", !HomeCardChoice.football("football", none));
        check("football layout with an empty pick: off, as HomeCardChoice.url says for it", !HomeCardChoice.football("football", empty) && HomeCardChoice.url(empty) == null);
        check("football layout with no card word: off, never a crash", !HomeCardChoice.football("football", nul));
        check("nothing known (null): not the football home, the prayers card as before", !HomeCardChoice.football("football", null) && HomeCardChoice.url(null) != null);
        check("the other layouts never are the football home", !HomeCardChoice.football("classic", cards) && !HomeCardChoice.football("couch", cards) && !HomeCardChoice.football(null, cards));
        check("«بدون» is still no card page on every layout", HomeCardChoice.url(none) == null);
        System.out.println(bad == 0 ? "ALL PASS" : bad + " FAILED");
        System.exit(bad == 0 ? 0 : 1);
    }
}
