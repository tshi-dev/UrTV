package sa.urtv.screensaver;

/** The mode row of the older games («كيف تلعبون؟»): first, kept per game, and the rows that do not apply with phones are skipped by the focus (no Android: GameChips only). */
public class GameChipsFlowTest {
    static int failed;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }

    static GameChips game() {
        GameChips c = new GameChips();
        c.add("rounds", "r", new String[] {"3", "5"}, 1).add("players", "p", new String[] {"3", "4", "5"}, 1).add("laps", "l", new String[] {"1", "2"}, 0).add("phones", "m", new String[] {"remote", "phones"}, 0);
        return c;
    }
    public static void main(String[] a) {
        GameChips c = game();
        c.first("phones");
        check("the mode row moves to the top", c.rows.get(0).key.equals("phones") && c.rows.size() == 4);
        check("the save is keyed, so the order does not matter: an old save loads into the new order", cLoads());
        c.row = c.rows.size();
        check("with the remote all rows are shown", c.shown().size() == 4);
        c.hide("players", true);
        check("a hidden row is not shown", c.shown().size() == 3 && !c.shown().contains(c.rows.indexOf(c.get("players"))));
        c.row = 0; c.col = 0;
        check("down from the mode row skips the hidden row", c.move(0, 1) && c.rows.get(c.row).key.equals("rounds") && c.move(0, 1) && c.rows.get(c.row).key.equals("laps"));
        check("up from the row after the hidden one skips it too", c.move(0, -1) && c.rows.get(c.row).key.equals("rounds") && c.move(0, -1) && c.row == 0);
        c.row = c.rows.size() - 1; c.move(0, 1);
        check("the start button is always reached", c.onButton());
        c.hide("players", false);
        check("shown again with the remote", c.shown().size() == 4);
        c.hasHelp = true; c.row = 0; c.hide("phones", false);
        check("up from the first shown row reaches the «!» when the first row is the mode row", c.move(0, -1) && c.help);
        GameChips d = game(); d.first("phones"); d.hide("phones", true); d.row = 1; d.hasHelp = true;
        check("a hidden first row: up from the first shown row reaches the «!»", d.move(0, -1) && d.help);
        System.out.println(failed == 0 ? "ok" : "failed");
        System.exit(failed == 0 ? 0 : 1);
    }
    static boolean cLoads() {
        GameChips c = game(); c.first("phones"); c.load("rounds=0;players=2;laps=1;phones=1");
        return c.picked("rounds") == 0 && c.picked("players") == 2 && c.picked("laps") == 1 && c.picked("phones") == 1 && c.save().startsWith("phones=1;");
    }
}
