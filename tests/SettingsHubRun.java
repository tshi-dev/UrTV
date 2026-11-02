package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * For tests/settings_hub_test.py: SettingsHub and FocusKey on a plain JVM. Reads a file of lines "row<TAB>label<TAB>header" or
 * "header<TAB>text" (what the test found in SettingsActivity) and prints one line each: "key<TAB>category" for a row named in
 * SettingsHub.ROWS (or "key<TAB>MISSING"), "text<TAB>category" for a header (or MISSING); then each screen of buttons and its ways
 * ("screen KEY n wide", "next KEY i n dir = j"), then the categories.
 */
public final class SettingsHubRun {
    public static void main(String[] args) throws Exception {
        StringBuilder out = new StringBuilder();
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            String[] f = line.split("\t", -1);
            if (f[0].equals("row")) {
                String key = FocusKey.key(f[1]);
                boolean named = false;
                for (String[] r : SettingsHub.ROWS) named |= r[0].equals(key);
                out.append(key).append('\t').append(named ? SettingsHub.of(key, null) : "MISSING").append('\t')
                   .append(SettingsHub.of(key, SettingsHub.ofHeader(f[2]))).append('\n');
            } else if (f[0].equals("header")) {
                String c = SettingsHub.ofHeader(f[1]);
                out.append(f[1]).append('\t').append(c == null ? "MISSING" : c).append('\n');
            }
        }
        // each screen of buttons (null: the first): "screen KEY n wide" then its ways "next KEY i n dir = j"
        for (String sc : new String[] {null, SettingsHub.CARDS, SettingsHub.MORE}) {
            String[] t = SettingsHub.tiles(sc);
            boolean wide = SettingsHub.wide(sc);
            String name = sc == null ? "-" : sc;
            out.append("screen ").append(name).append(' ').append(t.length).append(' ').append(wide).append('\n');
            for (int i = 0; i < t.length; i++) for (int dir = 0; dir < 4; dir++)
                out.append("next ").append(name).append(' ').append(i).append(' ').append(t.length).append(' ').append(dir).append(" = ")
                   .append(SettingsHub.next(i, t.length, dir, wide)).append('\n');
        }
        out.append("next - 6 7 1 = ").append(SettingsHub.next(6, 7, 1)).append('\n').append("next - 6 7 2 = ").append(SettingsHub.next(6, 7, 2)).append('\n');
        // each category: key, name, its index on its screen, the screen ("-" the first), its direct row ("-" none), tiles (a screen of buttons)
        for (String[] c : SettingsHub.CATEGORIES) {
            String par = SettingsHub.parent(c[0]), dir = SettingsHub.direct(c[0]);
            out.append("category ").append(c[0]).append('\t').append(c[1]).append('\t').append(SettingsHub.index(c[0])).append('\t')
               .append(par == null ? "-" : par).append('\t').append(dir == null ? "-" : dir).append('\t').append(SettingsHub.hasTiles(c[0])).append('\n');
        }
        System.out.print(out);
    }
}
