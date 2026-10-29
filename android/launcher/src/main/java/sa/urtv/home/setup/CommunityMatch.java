package sa.urtv.home.setup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The community lists (CleanYourTV's tested per-model lists, MIT; optimize/lists/community/) in the app: a port of optimize/community.py, `match` and
 * `packages`, line for line. A list is used only when the TV's maker or brand is one of its brands AND its model equals one of the list's models
 * (5 characters or more, not a generic one), or else its device code equals one of the list's devices (3 or more, not generic), and exactly one
 * list says so: two that fit mean none (a list for another model must never be applied). The lists come from the app's asset setup/community.txt,
 * which optimize/gen_setup_lists.py writes from the folder (tests/setup_community_test.py compares this with the Python on every list). Plain Java.
 */
final class CommunityMatch {
    static final Set<String> GENERIC_MODELS = set("SMARTTV", "ANDROIDTV", "GOOGLETV", "TV", "ATV", "TVBOX", "ANDROIDTVBOX", "GENERIC", "ANDROID", "UNKNOWN",
        "SMARTTV4K", "ANDROIDTV4K", "LEDTV", "LCDTV", "SMARTLEDTV", "TELEVISION");
    static final Set<String> GENERIC_DEVICES = set("TV", "ATV", "GTV", "DEVICE", "GENERIC", "ANDROID", "UNKNOWN", "TVBOX", "BOX");
    /** what a home switch of ours must never find in a list: launchers, setup wizards, the provisioning app */
    static final Pattern NOT_FOR_US = Pattern.compile("(launcher|tvhome|setupwraith|setupwizard|provision|\\.home$|\\.setup$)", Pattern.CASE_INSENSITIVE);

    /** One list: its name, the brands, models and devices it is for (normalised), and its packages. */
    static final class Meta {
        final String name;
        final Set<String> brands = new LinkedHashSet<>(), models = new LinkedHashSet<>(), devices = new LinkedHashSet<>();
        final List<String> packages = new ArrayList<>();
        Meta(String name) { this.name = name; }
    }

    /** The lists, the packages that owners often want (community-ask.txt), and the TCL packages our own lists know. */
    static final class Lists {
        final List<Meta> metas = new ArrayList<>();
        final List<Pattern> ask = new ArrayList<>();
        final List<String> knownTcl = new ArrayList<>();
    }

    /** The choice: the list, or none and why (in English, for the log; the screen says it in Arabic). */
    static final class Choice {
        final Meta meta;
        final String why;
        Choice(Meta meta, String why) { this.meta = meta; this.why = why; }
    }

    private CommunityMatch() { }

    private static Set<String> set(String... a) { Set<String> s = new LinkedHashSet<>(); for (String x : a) s.add(x); return s; }

    /** community.py's norm(): letters and digits only, upper case. */
    static String norm(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toUpperCase(Locale.ROOT).toCharArray()) if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) b.append(c);
        return b.toString();
    }

    /** The asset's lines: `P <package>` (numbered from 0), `L name|brands|models|devices|<package numbers, base 36>`, `A <ask regex>`, `K <tcl package>`. */
    static Lists parse(String asset) {
        Lists out = new Lists();
        if (asset == null) return out;
        List<String> pkgs = new ArrayList<>();
        for (String l : asset.split("\n")) {
            if (l.startsWith("P ")) pkgs.add(l.substring(2).trim());
            else if (l.startsWith("A ")) out.ask.add(Pattern.compile(l.substring(2)));
            else if (l.startsWith("K ")) out.knownTcl.add(l.substring(2).trim());
            else if (l.startsWith("L ")) {
                String[] f = l.substring(2).split("\\|", -1);
                if (f.length != 5) continue;
                Meta m = new Meta(f[0]);
                words(f[1], m.brands); words(f[2], m.models); words(f[3], m.devices);
                for (String n : f[4].split(" ")) if (!n.isEmpty()) m.packages.add(pkgs.get(Integer.parseInt(n, 36)));
                out.metas.add(m);
            }
        }
        return out;
    }

    private static void words(String s, Set<String> into) { for (String w : s.split(" ")) if (!norm(w).isEmpty()) into.add(norm(w)); }

    /** community.py's choose(): props are getprop's ro.product.manufacturer, ro.product.brand, ro.product.model and ro.product.device. */
    static Choice choose(Map<String, String> props, List<Meta> metas) {
        Set<String> makers = new LinkedHashSet<>();
        makers.add(norm(props.get("ro.product.manufacturer")));
        makers.add(norm(props.get("ro.product.brand")));
        makers.remove("");
        String model = norm(props.get("ro.product.model")), device = norm(props.get("ro.product.device"));
        List<Meta> fits = new ArrayList<>();
        for (Meta m : metas) for (String b : makers) if (m.brands.contains(b)) { fits.add(m); break; }
        if (fits.isEmpty()) return new Choice(null, "no community list is for this brand");
        List<Meta> byModel = new ArrayList<>();
        for (Meta m : fits) if (!model.isEmpty() && model.length() >= 5 && !GENERIC_MODELS.contains(model) && m.models.contains(model)) byModel.add(m);
        if (byModel.size() == 1) return new Choice(byModel.get(0), "the model matches");
        if (byModel.size() > 1) return new Choice(null, "more than one list fits this model: none is used");
        List<Meta> byDevice = new ArrayList<>();
        for (Meta m : fits) if (!device.isEmpty() && device.length() >= 3 && !GENERIC_DEVICES.contains(device) && m.devices.contains(device)) byDevice.add(m);
        if (byDevice.size() == 1) return new Choice(byDevice.get(0), "the device code matches");
        if (byDevice.size() > 1) return new Choice(null, "the device code is shared by several lists (sizes or models): none is used");
        return new Choice(null, "no community list is for this model or device code");
    }

    /**
     * community.py's packages(): the list's packages without the protected ones (keep), without the apps that answer HOME on this TV, without launchers
     * and setup wizards, once each, and (installed not null) only those on the TV.
     */
    static List<String> packages(Meta m, Collection<String> keep, Collection<String> homes, Collection<String> installed) {
        List<String> out = new ArrayList<>();
        for (String p : m.packages) {
            if (keep.contains(p) || homes.contains(p) || NOT_FOR_US.matcher(p).find() || out.contains(p)) continue;
            if (installed != null && !installed.contains(p)) continue;
            out.add(p);
        }
        return out;
    }

    /** Never Android's own core (optimize/lists/core.txt, regular expressions over the whole name), whatever a list says. */
    static boolean core(String p) {
        for (String re : SetupLists.CORE) if (p.matches(re)) return true;
        return false;
    }

    /** community.sh's `grep -E -f community-ask.txt`: a package whose name holds one of the expressions is left on and asked about. */
    static boolean ask(String p, List<Pattern> ask) {
        for (Pattern a : ask) if (a.matcher(p).find()) return true;
        return false;
    }

    /**
     * The «removal» rating of every package in UAD-ng's uad_lists.json (Recommended, Advanced, Expert, Unsafe), read with a small exact JSON walker:
     * strings with escapes, nested arrays and objects; only the top-level keys' own «removal» counts. Null when the text is not that JSON.
     */
    static Map<String, String> removals(String json) {
        if (json == null) return null;
        Map<String, String> out = new HashMap<>();
        int[] at = {skip(json, 0)};
        try {
            if (at[0] >= json.length() || json.charAt(at[0]) != '{') return null;
            at[0]++;
            while (true) {
                at[0] = skip(json, at[0]);
                if (json.charAt(at[0]) == '}') return out;
                String key = string(json, at);
                at[0] = skip(json, at[0]);
                if (json.charAt(at[0]++) != ':') return null;
                at[0] = skip(json, at[0]);
                if (json.charAt(at[0]) == '{') {
                    at[0]++;
                    while (true) {
                        at[0] = skip(json, at[0]);
                        if (json.charAt(at[0]) == '}') { at[0]++; break; }
                        String k = string(json, at);
                        at[0] = skip(json, at[0]);
                        if (json.charAt(at[0]++) != ':') return null;
                        at[0] = skip(json, at[0]);
                        if (k.equals("removal") && json.charAt(at[0]) == '"') out.put(key, string(json, at));
                        else value(json, at);
                        at[0] = skip(json, at[0]);
                        if (json.charAt(at[0]) == ',') at[0]++;
                    }
                } else {
                    value(json, at);
                }
                at[0] = skip(json, at[0]);
                if (json.charAt(at[0]) == ',') at[0]++;
            }
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int skip(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static String string(String s, int[] at) {
        if (s.charAt(at[0]) != '"') throw new IllegalArgumentException("not a string");
        StringBuilder b = new StringBuilder();
        int i = at[0] + 1;
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') break;
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n': b.append('\n'); break;
                    case 't': b.append('\t'); break;
                    case 'r': b.append('\r'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                    default: b.append(e);
                }
            } else {
                b.append(c);
            }
        }
        at[0] = i;
        return b.toString();
    }

    /** Skips one value of any kind. */
    private static void value(String s, int[] at) {
        char c = s.charAt(at[0]);
        if (c == '"') { string(s, at); return; }
        if (c == '{' || c == '[') {
            int depth = 0;
            while (true) {
                char d = s.charAt(at[0]);
                if (d == '"') { string(s, at); continue; }
                at[0]++;
                if (d == '{' || d == '[') depth++;
                else if ((d == '}' || d == ']') && --depth == 0) return;
            }
        }
        while (at[0] < s.length() && ",}] \n\r\t".indexOf(s.charAt(at[0])) < 0) at[0]++;   // a number, true, false, null
    }
}
