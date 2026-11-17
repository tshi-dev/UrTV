package sa.urtv.screensaver;

/**
 * 4.4: one photo of «صوري» picked to delete, from the phone page or the TV's grid. Photos are numbered 1.jpg, 2.jpg … with no gaps, so a number
 * alone could name another photo after one before it was deleted: a pick is its number AND its file's time («3:1763300000000»), and a pick whose
 * photo moved or went is refused, never applied to its neighbour. No Android in it (tests/OwnPickRun.java runs it).
 */
final class OwnPick {
    final int n;
    final long stamp;

    private OwnPick(int n, long stamp) { this.n = n; this.stamp = stamp; }

    static String id(int n, long stamp) { return n + ":" + stamp; }

    /** null when it is not one of ours: «n:stamp», n 1..999, a stamp of 1 to 14 digits */
    static OwnPick parse(String id) {
        if (id == null || !id.matches("[1-9][0-9]{0,2}:[0-9]{1,14}")) return null;
        int c = id.indexOf(':');
        return new OwnPick(Integer.parseInt(id.substring(0, c)), Long.parseLong(id.substring(c + 1)));
    }

    /** the pick still names the same photo: its number is there and its file has the same time */
    boolean same(int count, long stampNow) { return n >= 1 && n <= count && stampNow == stamp; }
}
