package sa.urtv.home.tilawah;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.4 radio reliability: a station's addresses in the order they are tried: its own, its «alt» (4.3), then its «mirrors» (stations.json, and the
 * ones the stations check publishes in urtv-data, StationStatus). https only (StreamUrl follows a redirect to a cleartext host it allows), no
 * repeats, at most MAX. When one source has failed every try (RadioPolicy.Step.ALT) the next one is taken, round and round while the person wants
 * the station. No Android in it (tests/StationSourcesRun.java runs it).
 */
final class StationSources {
    private StationSources() { }

    static final int MAX = 6;

    static List<String> of(String url, String alt, List<String> mirrors, List<String> published) {
        List<String> out = new ArrayList<>();
        add(out, url);
        add(out, alt);
        if (mirrors != null) for (String m : mirrors) add(out, m);
        if (published != null) for (String m : published) add(out, m);
        return out;
    }

    private static void add(List<String> out, String u) {
        if (u == null || out.size() >= MAX || !u.startsWith("https://") || u.length() > 400 || u.contains(" ") || out.contains(u)) return;
        out.add(u);
    }

    /** the source after i, round and round */
    static int next(int i, int n) { return n <= 0 ? 0 : (i + 1) % n; }
}
