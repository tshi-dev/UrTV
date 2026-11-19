package sa.urtv.home;

import java.util.HashSet;
import java.util.Set;

/**
 * 4.4 beta 3, the security of the paired route (the supervisor's hard condition): the add-on's PAIR_* calls give an adb shell on the TV, so
 * 1. only our home may call them: the calling uid's one package is sa.urtv.home AND its signing certificate's SHA-256 is the add-on's own ({@link #caller});
 * 2. only the setup's own commands pass ({@link #allowed}): a command's SHAPE (package-like names become P, a run of them P+, numbers N; nothing else may vary)
 *    must be one of {@link PairCommandShapes#SHAPES}, made from every command the setup's scenarios send (tests/pair_commands_test.py keeps the list exact).
 *    A placeholder matches letters, digits, «_», «.» and «/» only, so no quote, «;», «|», «$(», «`», «&», «>» or space can be slipped in through it.
 * Shared by the home (it refuses before sending) and the add-on (it enforces). No Android in here (tests/PairGuardRun.java).
 */
public final class PairGuard {
    private PairGuard() { }

    public static final String HOME = "sa.urtv.home";

    /** a package or component name: dotted words, optionally /Class or /.Class */
    private static final String NAME = "\\b[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+(/\\.?[A-Za-z0-9_.]+)?";

    /** the command's shape; null for null */
    public static String shape(String command) {
        if (command == null) return null;
        String s = command.replaceAll(NAME, "P");
        s = s.replaceAll("\\b[0-9]+\\b", "N");
        s = s.replaceAll("P( P)+", "P+");
        return s;
    }

    private static volatile Set<String> shapes;

    private static Set<String> shapes() {
        Set<String> s = shapes;
        if (s == null) { s = new HashSet<>(); for (String x : PairCommandShapes.SHAPES) s.add(x); shapes = s; }
        return s;
    }

    /** the command is one of the setup's own (by shape); anything else is refused */
    public static boolean allowed(String command) {
        return command != null && command.indexOf('\n') < 0 && command.indexOf('\0') < 0 && shapes().contains(shape(command));
    }

    /**
     * the caller may use the paired route: its uid has exactly one package, our home's, and that package's signing certificate digest (SHA-256, hex) equals
     * the add-on's own. {@code packages}: getPackagesForUid(callingUid); {@code callerDigest}/{@code ownDigest}: null when they could not be read (refused).
     */
    public static boolean caller(String[] packages, String callerDigest, String ownDigest) { return callerIs(packages, HOME, callerDigest, ownDigest); }

    /** the same for any one expected package (the home's PairFiles lets only the add-on, sa.urtv.extras, read) */
    public static boolean callerIs(String[] packages, String expected, String callerDigest, String ownDigest) {
        return packages != null && packages.length == 1 && expected != null && expected.equals(packages[0])
            && callerDigest != null && ownDigest != null && !callerDigest.isEmpty() && callerDigest.equalsIgnoreCase(ownDigest);
    }
}
