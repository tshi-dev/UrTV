package sa.urtv.home;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * The home's S (docs/ENGLISH.md) for a test that runs the plain-Java classes on a JVM (ControlText, GreetingText, AdhanBarText…): the real
 * one needs Android. The same calls, the same Tr underneath: Arabic by default, so every Arabic case runs as it always did; with
 * -Durtv.lang=en the English of i18n/en/*.tsv (-Durtv.i18n=<that folder>), read as the app reads it (tests/i18n_home_test.py).
 */
public final class S {
    private S() { }

    private static final String LANG = Tr.norm(System.getProperty("urtv.lang"));
    private static Map<String, String> table;

    public static String t(String ar, Object... args) { return Tr.t(LANG, Tr.EN.equals(LANG) ? table() : null, ar, args); }
    public static String n(String ar) { return ar; }
    public static String tv(String ar, Object... args) { return t(ar, args); }
    public static String lang() { return LANG; }
    public static boolean en() { return Tr.EN.equals(LANG); }

    private static synchronized Map<String, String> table() {
        if (table != null) return table;
        table = new HashMap<>();
        String dir = System.getProperty("urtv.i18n");
        if (dir == null) return table;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(dir), "*.tsv")) {
            for (Path f : files) Tr.parse(new String(Files.readAllBytes(f), StandardCharsets.UTF_8), table);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("no table at " + dir, e);
        }
        return table;
    }
}
