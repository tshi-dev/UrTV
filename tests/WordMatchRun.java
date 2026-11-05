package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The screensaver's English words on a JVM, for tests/i18n_saver_test.py: WordMatch with the real table (every i18n/en/*.tsv, read with
 * Tr.parse as the app reads it), Words' own patterns, and Places.
 *   java sa.urtv.screensaver.WordMatchRun <i18n/en dir> <patterns file> <in file> <out file>
 * in: one text per line (\n written as \\n); a line "city\tname\tlat\tlon" asks Places.city, "country\tcode" Places.country,
 * "name\tarabic" Places.name, "regions" every region's English (one line each), "cities" every built-in city without one.
 * out: one line per in line.
 */
public final class WordMatchRun {
    public static void main(String[] a) throws Exception {
        final Map<String, String> table = new HashMap<>();
        try (java.util.stream.Stream<Path> files = Files.list(Paths.get(a[0]))) {
            for (Path f : (Iterable<Path>) files.sorted()::iterator)
                if (f.toString().endsWith(".tsv")) Tr.parse(new String(Files.readAllBytes(f), StandardCharsets.UTF_8), table);
        }
        List<String> patterns = new ArrayList<>();
        for (String l : Files.readAllLines(Paths.get(a[1]), StandardCharsets.UTF_8)) if (!l.isEmpty()) patterns.add(l);
        WordMatch m = new WordMatch(new WordMatch.Table() {
            @Override public String en(String ar, Object... args) {
                if (!table.containsKey(ar) || table.get(ar).equals(ar)) return null;
                return Tr.t(Tr.EN, table, ar, args);
            }
            @Override public String name(String ar) { return Places.name(ar); }
        }, patterns.toArray(new String[0]));
        StringBuilder out = new StringBuilder();
        for (String line : Files.readAllLines(Paths.get(a[2]), StandardCharsets.UTF_8)) {
            String[] p = line.split("\t", -1);
            String r;
            if (p[0].equals("city") && p.length == 4) r = String.valueOf(Places.city(p[1], p[2], p[3]));
            else if (p[0].equals("country") && p.length == 2) r = String.valueOf(Places.country(p[1]));
            else if (p[0].equals("name") && p.length == 2) r = String.valueOf(Places.name(p[1]));
            else if (p[0].equals("regions")) {
                StringBuilder b = new StringBuilder();
                for (Cities.Region g : Cities.REGIONS) b.append(b.length() > 0 ? "|" : "").append(g.name).append('=').append(Places.region(g));
                r = b.toString();
            } else if (p[0].equals("cities")) {
                StringBuilder b = new StringBuilder();
                for (Cities.Region g : Cities.REGIONS) for (String[] c : g.cities) if (Places.city(c) == null) b.append(c[0]).append(' ');
                r = b.toString().trim();
            } else r = m.of(line.replace("\\n", "\n"));
            out.append(r.replace("\n", "\\n")).append('\n');
        }
        Files.write(Paths.get(a[3]), out.toString().getBytes(StandardCharsets.UTF_8));
    }
}
