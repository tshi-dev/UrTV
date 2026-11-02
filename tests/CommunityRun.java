package sa.urtv.home.setup;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CommunityMatch on the app's asset, for tests/setup_community_test.py.
 *   choose <asset> <cases>     one case per line «maker|brand|model|device»: prints the chosen list's name, or «-»
 *   packages <asset> <keep> <homes>   every list's packages without keep and homes, «name: p1 p2 …»
 *   removals <json>            the «removal» ratings, «pkg rating» per line (sorted)
 */
public class CommunityRun {
    static String read(String p) throws Exception { return new String(Files.readAllBytes(Paths.get(p)), StandardCharsets.UTF_8); }

    public static void main(String[] a) throws Exception {
        if (a[0].equals("removals")) {
            Map<String, String> r = CommunityMatch.removals(read(a[1]));
            if (r == null) { System.out.println("NULL"); return; }
            List<String> keys = new ArrayList<>(r.keySet());
            java.util.Collections.sort(keys);
            for (String k : keys) System.out.println(k + " " + r.get(k));
            return;
        }
        CommunityMatch.Lists lists = CommunityMatch.parse(read(a[1]));
        if (a[0].equals("choose")) {
            StringBuilder out = new StringBuilder();
            for (String l : read(a[2]).split("\n", -1)) {
                if (l.isEmpty()) continue;
                String[] f = l.split("\\|", -1);
                Map<String, String> props = new HashMap<>();
                props.put("ro.product.manufacturer", f[0]); props.put("ro.product.brand", f[1]); props.put("ro.product.model", f[2]); props.put("ro.product.device", f[3]);
                CommunityMatch.Choice c = CommunityMatch.choose(props, lists.metas);
                out.append(c.meta == null ? "-" : c.meta.name).append(" ").append(c.why).append('\n');
            }
            System.out.print(out);
        } else if (a[0].equals("packages")) {
            List<String> keep = Arrays.asList(read(a[2]).split("\n")), homes = Arrays.asList(read(a[3]).split("\n"));
            for (CommunityMatch.Meta m : lists.metas) System.out.println(m.name + ": " + String.join(" ", CommunityMatch.packages(m, keep, homes, null)));
            System.out.println("ASK " + lists.ask.size() + " KNOWN " + lists.knownTcl.size());
        }
    }
}
