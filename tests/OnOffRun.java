import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * For tests/on_off_test.py: both apps' OnOff on a plain JVM (compiled twice, once per package, with -cp of each). Reads values, one a line,
 * and prints "state<TAB>wordEnd" for each; then the greens on a white, a black and the focus fills.
 */
public final class OnOffRun {
    public static void main(String[] args) throws Exception {
        Class<?> c = Class.forName(args[0]);
        java.lang.reflect.Method state = c.getDeclaredMethod("state", String.class), end = c.getDeclaredMethod("wordEnd", String.class),
                green = c.getDeclaredMethod("green", int.class);
        state.setAccessible(true); end.setAccessible(true); green.setAccessible(true);
        StringBuilder out = new StringBuilder();
        for (String v : Files.readAllLines(Paths.get(args[1]), StandardCharsets.UTF_8))
            out.append(state.invoke(null, v)).append('\t').append(end.invoke(null, v)).append('\n');
        for (int bg : new int[] {0xFFFFFFFF, 0xFF000000, 0xF2FFFFFF, 0xF214161A, 0xFF17181B, 0xFFF0A64A})
            out.append(String.format("green %08X = %08X%n", bg, (Integer) green.invoke(null, bg)));
        System.out.print(out);
    }
}
