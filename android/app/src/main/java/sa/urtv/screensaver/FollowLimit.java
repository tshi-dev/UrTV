package sa.urtv.screensaver;

/**
 * 4.4 how many leagues and clubs can be followed: past the soft limit (8 leagues, 20 clubs) one warning, once, that a weak TV may slow
 * down («أضف» / «لا»; after «أضف» never again); at the ceiling (24 leagues, 40 clubs) a plain «this is the most», never a silent no.
 * No Android in the decision (tests/FollowLimitRun.java); the words are here so every place says the same.
 */
final class FollowLimit {
    private FollowLimit() { }

    static final int SOFT_LEAGUES = 8, MAX_LEAGUES = 24, SOFT_TEAMS = 20, MAX_TEAMS = 40;
    /** what one more follow means: go ahead, ask once first, or the ceiling */
    static final int ADD = 0, WARN = 1, FULL = 2;
    static final String LEAGUES = "leagues", TEAMS = "teams";

    static int check(int count, int soft, int max, boolean warned) {
        if (count >= max) return FULL;
        if (count >= soft && !warned) return WARN;
        return ADD;
    }

    static int check(String kind, int count, boolean warned) { return check(kind, count, warned, MAX_TEAMS); }

    /** the same with the clubs' ceiling the person chose (teamCap: 40, 80, or 0 for none) */
    static int check(String kind, int count, boolean warned, int teamCap) {
        return LEAGUES.equals(kind) ? check(count, SOFT_LEAGUES, MAX_LEAGUES, warned) : check(count, SOFT_TEAMS, teamCeiling(teamCap), warned);
    }

    /**
     * 4.4 the owner (R-472): the clubs' ceiling stays 40 by default; the person may raise it to 80, then to none, only after a clear warning
     * (TEAM_CAP_WARNING). Lowering it never removes a followed club: no more are added until the list is under the ceiling again.
     */
    static final int[] TEAM_CAPS = {MAX_TEAMS, 80, 0};
    static final String[] TEAM_CAP_NAMES = {"٤٠ فريقًا", "٨٠ فريقًا", "بلا حد"};
    static final String TEAM_CAP_WARNING = "كثرة الفرق ممكن تبطّئ التلفزيون";
    /** a stored or given value as one of TEAM_CAPS (anything else: the default 40) */
    static int teamCap(int v) { return v == 80 || v == 0 ? v : MAX_TEAMS; }
    /** how many clubs at most (none: Integer.MAX_VALUE) */
    static int teamCeiling(int cap) { int c = teamCap(cap); return c == 0 ? Integer.MAX_VALUE : c; }
    static String teamCapName(int cap) { int c = teamCap(cap); return TEAM_CAP_NAMES[c == 80 ? 1 : c == 0 ? 2 : 0]; }
    /** the warning is due only when the ceiling goes up (never when it goes back down) */
    static boolean raises(int from, int to) { return teamCeiling(to) > teamCeiling(from); }

    /** the owner's words; the number is the soft limit of that kind */
    static String warning(String kind) {
        return LEAGUES.equals(kind) ? "إضافة أكثر من ٨ قد تبطّئ التلفزيون إذا كان ضعيف" : "إضافة أكثر من ٢٠ قد تبطّئ التلفزيون إذا كان ضعيف";
    }

    static String full(String kind) { return full(kind, MAX_TEAMS); }

    /** the clubs' words name the ceiling the person chose, and where to raise it */
    static String full(String kind, int teamCap) {
        if (LEAGUES.equals(kind)) return "هذا الحد الأعلى: ٢٤ دوريًا حتى يبقى التلفزيون سريعًا. أزل دوريًا لتضيف غيره";
        int c = teamCap(teamCap);
        return "هذا الحد الأعلى: " + (c == 80 ? "٨٠" : "٤٠") + " فريقًا حتى يبقى التلفزيون سريعًا. أزل فريقًا لتضيف غيره\nأو ارفع الحد من «حد الفرق»";
    }

    /** the state as words, for the launcher's page (ConfigProvider's phone_follow_check) */
    static String word(int state) { return state == FULL ? "full" : state == WARN ? "warn" : "add"; }
}
