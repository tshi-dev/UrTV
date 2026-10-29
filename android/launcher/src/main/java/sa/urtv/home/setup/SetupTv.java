package sa.urtv.home.setup;

import java.io.IOException;
import java.util.Set;

/**
 * The TV's answers read the way optimize/'s scripts read them (`tr -d '\r' | tail -n 1`, `head -n 1`, a list with its end marker), for the steps
 * that are ports of those scripts (SetupPrep, SetupTweaks, SetupCommunity, SetupMeasure). Plain Java.
 */
final class SetupTv {
    final SetupEngine.Shell sh;

    SetupTv(SetupEngine.Shell sh) { this.sh = sh; }

    /** The whole answer, without carriage returns. */
    String text(String command) throws IOException { return sh.run(command).output.replace("\r", ""); }

    AdbClient.Result run(String command) throws IOException { return sh.run(command); }

    /** `… | tr -d '\r' | tail -n 1`: the last line ("" when the answer is empty, or ends with an empty line). */
    String tail1(String command) throws IOException { return tail1of(text(command)); }

    static String tail1of(String t) {
        if (t == null || t.isEmpty()) return "";
        String s = t.endsWith("\n") ? t.substring(0, t.length() - 1) : t;
        int i = s.lastIndexOf('\n');
        return i < 0 ? s : s.substring(i + 1);
    }

    /** `… | tr -d '\r' | head -n 1`. */
    static String head1of(String t) {
        if (t == null || t.isEmpty()) return "";
        int i = t.indexOf('\n');
        return i < 0 ? t : t.substring(0, i);
    }

    /** `pm list packages …` with an end marker; null when the TV did not say (SetupEngine.packagesOf; the full list must hold «android»). */
    Set<String> packages(String command) throws IOException {
        Set<String> p = SetupEngine.packagesOf(text(command + "; echo END_OF_LIST"));
        if (p == null) return null;
        return command.startsWith("pm list packages -d") || p.contains("android") ? p : null;
    }

    /** The Android SDK number, 0 when the TV did not say. */
    int sdk() throws IOException {
        String s = tail1("getprop ro.build.version.sdk").replaceAll("[^0-9]", "");
        try { return s.isEmpty() ? 0 : Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    /** Is the package installed (`pm list packages <p>` has exactly its line)? */
    boolean installed(String pkg) throws IOException {
        for (String l : text("pm list packages " + pkg).split("\n")) if (l.equals("package:" + pkg)) return true;
        return false;
    }
}
