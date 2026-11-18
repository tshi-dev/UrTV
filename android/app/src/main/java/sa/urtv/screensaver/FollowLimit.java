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

    static int check(String kind, int count, boolean warned) {
        return LEAGUES.equals(kind) ? check(count, SOFT_LEAGUES, MAX_LEAGUES, warned) : check(count, SOFT_TEAMS, MAX_TEAMS, warned);
    }

    /** the owner's words; the number is the soft limit of that kind */
    static String warning(String kind) {
        return LEAGUES.equals(kind) ? "إضافة أكثر من ٨ قد تبطّئ التلفزيون إذا كان ضعيف" : "إضافة أكثر من ٢٠ قد تبطّئ التلفزيون إذا كان ضعيف";
    }

    static String full(String kind) {
        return LEAGUES.equals(kind) ? "هذا الحد الأعلى: ٢٤ دوريًا حتى يبقى التلفزيون سريعًا. أزل دوريًا لتضيف غيره"
            : "هذا الحد الأعلى: ٤٠ فريقًا حتى يبقى التلفزيون سريعًا. أزل فريقًا لتضيف غيره";
    }

    /** the state as words, for the launcher's page (ConfigProvider's phone_follow_check) */
    static String word(int state) { return state == FULL ? "full" : state == WARN ? "warn" : "add"; }
}
