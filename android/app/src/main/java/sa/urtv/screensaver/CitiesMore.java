package sa.urtv.screensaver;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 4.3.3: the offline search list (assets/cities-more.txt, .github/tools/cities-more/build.py): about ten thousand places the built-in list
 * (Cities.java) lacks, and the English name, other spellings and population of the built-in ones. Read once, on the first search, never at
 * start: the settings, the home and the receiver do not pay for it until a person searches. A place found here is saved as a searched place
 * (its name, coordinates, zone and country), so a later update of the file never changes a saved city. No Android in here (the tests).
 */
final class CitiesMore {
    private CitiesMore() {}

    static final String FILE = "cities-more.txt";

    /** Opens the file: set by Prefs (the app's assets), or by a test. */
    interface Opener { InputStream open() throws Exception; }
    static volatile Opener opener;

    /** One place: its row as Cities.java's world rows (Arabic, latitude, longitude, zone, English, country), its stable id, population, other spellings. */
    static final class Place {
        final String[] row;
        final String id;
        final int pop;
        final String[] aliases;
        final boolean builtin;
        Place(String[] row, String id, int pop, String[] aliases, boolean builtin) {
            this.row = row; this.id = id; this.pop = pop; this.aliases = aliases; this.builtin = builtin;
        }
    }

    private static volatile List<Place> all;

    /** Every place: the built-in ones first (in the list's order, with what the file adds to them), then the file's own. Empty without the file. */
    static List<Place> all() {
        List<Place> a = all;
        if (a != null) return a;
        synchronized (CitiesMore.class) {
            if (all == null) all = load();
            return all;
        }
    }

    /** The file's own places only (not in Cities.java). */
    static List<Place> extra() {
        List<Place> out = new ArrayList<>();
        for (Place p : all()) if (!p.builtin) out.add(p);
        return out;
    }

    /** For a test: read again, from the opener now set. */
    static void reset() { synchronized (CitiesMore.class) { all = null; } }

    private static List<Place> load() {
        Map<String, String[]> more = new HashMap<>();   // "lat|lon" of a built-in row -> population, English, aliases
        List<Place> extra = new ArrayList<>();
        Opener o = opener;
        if (o != null) {
            try (InputStream in = o.open(); BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 16)) {
                String[] zones = new String[0];
                for (String line; (line = r.readLine()) != null; ) {
                    if (line.isEmpty() || line.charAt(0) == '#') continue;
                    if (line.charAt(0) == '@') { zones = line.substring(1).split("\t"); continue; }
                    String[] f = line.split("\t", -1);
                    if (line.charAt(0) == '=') {
                        if (f.length >= 5) more.put(f[0].substring(1) + "|" + f[1], new String[] {f[2], f[3], f[4]});
                        continue;
                    }
                    if (f.length < 9) continue;
                    int z = Integer.parseInt(f[2]);
                    String[] row = {f[6], f[3], f[4], zones[z], f[7], f[1]};
                    extra.add(new Place(row, f[0], Integer.parseInt(f[5]), split(f[8]), false));
                }
            } catch (Exception e) {
                extra.clear();   // a broken file: the built-in list alone, as before it existed
                more.clear();
            }
        }
        List<Place> out = new ArrayList<>();
        for (Cities.Region reg : Cities.REGIONS) for (String[] c : reg.cities) {
            String[] m = more.get(c[1] + "|" + c[2]);
            String[] row = c.length > 5 ? c : new String[] {c[0], c[1], c[2], CityMatch.zone(c), m != null ? m[1] : "", reg.country};
            out.add(new Place(row, c[0] + "|" + c[1] + "|" + c[2], m != null ? parse(m[0]) : 0, m != null ? split(m[2]) : new String[0], true));
        }
        out.addAll(extra);
        return java.util.Collections.unmodifiableList(out);
    }

    private static int parse(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private static String[] split(String s) { return s.isEmpty() ? new String[0] : s.split("\\|"); }
}
