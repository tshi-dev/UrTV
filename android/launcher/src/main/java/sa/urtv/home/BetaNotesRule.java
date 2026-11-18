package sa.urtv.home;

/** BetaNotes' rule, no Android in it (tests/BetaNotesRun.java runs it). */
final class BetaNotesRule {
    private BetaNotesRule() { }

    /** "crash" first, then "update" (a build newer than the one last seen on beta), else "" (off: never anything) */
    static String what(boolean beta, String crash, long seen, long code) {
        if (!beta) return "";
        if (crash != null && !crash.isEmpty()) return "crash";
        return seen > 0 && code > seen ? "update" : "";
    }
}
