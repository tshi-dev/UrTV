package sa.urtv.home;

/** tests/side_cards_test.py: SideTopics.beside for the options beside the strips people choose; prints «wanted|strip -> result» */
public class SideTopicsRun {
    public static void main(String[] a) {
        String[][] cases = {
            {"nextprayer,hourly", "none"}, {"nextprayer,hourly,nextmatch", "none"}, {"nextprayer,hourly", "stocks"},
            {"nextprayer,hourly,nextmatch", "matches,prayerrow,hourly"}, {"nextprayer,hourly", "prayerrow"}, {"nextprayer,hourly,nextmatch", "prayers,verse"},
            {"nextprayer,hourly", "smart"}, {"nextprayer,hourly,nextmatch", "today,stocks"},
        };
        for (String[] c : cases) System.out.println(c[0] + "|" + c[1] + " -> " + SideTopics.beside(c[0], c[1]));
    }
}
