package sa.urtv.screensaver;

/** tests/team_cap_test.py: «حد الفرق» (FollowLimit.teamCap, 4.4 R-472) on a plain JVM. One line per case: «ok …» or «BAD …». */
public class TeamCapRun {
    static int bad;
    static void check(String n, boolean ok) { System.out.println((ok ? "ok " : "BAD ") + n); if (!ok) bad++; }

    public static void main(String[] a) {
        String T = FollowLimit.TEAMS, L = FollowLimit.LEAGUES;
        check("the default stays 40: the 41st club is refused, as before", FollowLimit.check(T, 40, true) == FollowLimit.FULL
            && FollowLimit.check(T, 40, true, 40) == FollowLimit.FULL && FollowLimit.check(T, 39, true, 40) == FollowLimit.ADD);
        check("80: up to 80, the 81st refused", FollowLimit.check(T, 40, true, 80) == FollowLimit.ADD && FollowLimit.check(T, 79, true, 80) == FollowLimit.ADD
            && FollowLimit.check(T, 80, true, 80) == FollowLimit.FULL);
        check("none (0): never full", FollowLimit.check(T, 500, true, 0) == FollowLimit.ADD && FollowLimit.teamCeiling(0) == Integer.MAX_VALUE);
        check("the soft limit's one warning is still asked past 20, whatever the ceiling", FollowLimit.check(T, 20, false, 80) == FollowLimit.WARN
            && FollowLimit.check(T, 20, false, 0) == FollowLimit.WARN);
        check("the leagues keep their own 24", FollowLimit.check(L, 24, true, 0) == FollowLimit.FULL && FollowLimit.check(L, 23, true, 0) == FollowLimit.ADD);
        check("only 40, 80 and 0; anything else is the default 40", FollowLimit.teamCap(80) == 80 && FollowLimit.teamCap(0) == 0 && FollowLimit.teamCap(60) == 40
            && FollowLimit.teamCap(-1) == 40 && FollowLimit.TEAM_CAPS.length == 3 && FollowLimit.TEAM_CAPS[0] == 40);
        check("the warning only when the ceiling goes up", FollowLimit.raises(40, 80) && FollowLimit.raises(80, 0) && FollowLimit.raises(40, 0)
            && !FollowLimit.raises(80, 40) && !FollowLimit.raises(0, 80) && !FollowLimit.raises(40, 40));
        check("the owner's words", FollowLimit.TEAM_CAP_WARNING.equals("كثرة الفرق ممكن تبطّئ التلفزيون")
            && FollowLimit.teamCapName(40).equals("٤٠ فريقًا") && FollowLimit.teamCapName(80).equals("٨٠ فريقًا") && FollowLimit.teamCapName(0).equals("بلا حد"));
        check("the ceiling's words name the chosen number and where to raise it", FollowLimit.full(T, 80).contains("٨٠") && FollowLimit.full(T, 40).contains("٤٠")
            && FollowLimit.full(T, 80).contains("«حد الفرق»") && FollowLimit.full(T).equals(FollowLimit.full(T, 40)));
        System.exit(bad == 0 ? 0 : 1);
    }
}
