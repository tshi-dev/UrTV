package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** For tests/game_bundle_test.py: opens a bundle made by games/tools/pack_bundle.py (args: name, file, expected text file). Prints PASS/FAIL lines. */
public final class GameBundleTest {
    static int failed = 0;
    static void check(String what, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + what); if (!ok) failed++; }
    public static void main(String[] a) throws Exception {
        byte[] file = Files.readAllBytes(Paths.get(a[1])), want = Files.readAllBytes(Paths.get(a[2]));
        byte[] got = GameBundle.open(a[0], file);
        check("a bundle made by the Python tool opens and gives the same text, Arabic included", got != null && java.util.Arrays.equals(got, want));
        check("a bundle opened under another name is refused", GameBundle.open("other", file) == null);
        byte[] bad = file.clone(); bad[40] ^= 1;
        check("one changed byte is refused (the check comes before the decryption)", GameBundle.open(a[0], bad) == null);
        check("a file that is not a bundle, an empty one and null are refused, none throws", GameBundle.open(a[0], "hello".getBytes(StandardCharsets.UTF_8)) == null && GameBundle.open(a[0], new byte[0]) == null && GameBundle.open(a[0], null) == null);
        System.exit(failed == 0 ? 0 : 1);
    }
}
