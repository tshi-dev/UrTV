package sa.urtv.screensaver;

/**
 * The score table of a remote-only game (no Android, tests/GamesKitTest.java): 1 to 10 sides (teams or players), points, and the places. Sides on one score
 * share the place (1, 1, 3), as in «الأسرع». Nothing here is ever stored: a game's names and scores live only while its screen does.
 */
final class GameScore {
    static final int MAX = 10;
    /** the default team names (games/COMMON.md: the first four are the mockups'); a game shows them through S.tv */
    static final String[] TEAMS = {"الصقور", "النجوم", "الأسود", "الواحة", "النخيل", "الفرسان", "الأمواج", "القمم", "الشهب", "الروّاد"};

    final int n; final int[] points, last;

    GameScore(int sides) { n = Math.max(1, Math.min(MAX, sides)); points = new int[n]; last = new int[n]; }

    /** points to a side; `last` keeps what each side won since newRound() (the «+10» beside the total) */
    void add(int side, int pts) { if (side < 0 || side >= n) return; points[side] += pts; last[side] += pts; }
    void newRound() { for (int i = 0; i < n; i++) last[i] = 0; }

    /** the sides from first to last; equal scores keep their order */
    int[] ranked() {
        int[] v = new int[n];
        for (int i = 0; i < n; i++) v[i] = i;
        for (int i = 1; i < n; i++) { int x = v[i], j = i - 1; while (j >= 0 && points[v[j]] < points[x]) { v[j + 1] = v[j]; j--; } v[j + 1] = x; }
        return v;
    }
    /** the place of a side, 1 = first; sides on one score share it */
    int rank(int side) { int r = 1; for (int i = 0; i < n; i++) if (points[i] > points[side]) r++; return r; }
    int best() { int b = points[0]; for (int p : points) b = Math.max(b, p); return b; }
    /** how many sides hold the best score */
    int leaders() { int b = best(), c = 0; for (int p : points) if (p == b) c++; return c; }
    /** more than one side on the best score (with one side there is never a tie) */
    boolean tie() { return n > 1 && leaders() > 1; }
    /** nobody won a point */
    boolean nobody() { return best() <= 0; }
    /** the side whose turn follows `side` */
    int after(int side) { return (side + 1) % n; }
}
