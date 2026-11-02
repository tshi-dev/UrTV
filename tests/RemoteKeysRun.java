package sa.urtv.home;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/** For tests/remote_keys_test.py: RemoteKeys on tests/remote_keys_cases.tsv, one answer line per case line.
 *  "reset": a new RemoteKeys (it remembers the key that acted until its release). "key": kind keyCode scanCode action repeat installed
 *  (comma list of YouTube, Netflix, Prime, or empty), answered PASS, EAT, APP:package, SETTINGS or INPUTS. */
public final class RemoteKeysRun {
    public static void main(String[] args) throws IOException {
        StringBuilder out = new StringBuilder();
        RemoteKeys keys = new RemoteKeys();
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t", -1);
            if (f[0].equals("reset")) { keys = new RemoteKeys(); out.append("-\n"); continue; }
            Set<String> installed = new HashSet<>();
            for (String n : f[5].split(",")) {
                if (n.equals("YouTube")) installed.add(RemoteKeys.YOUTUBE);
                if (n.equals("Netflix")) installed.add(RemoteKeys.NETFLIX);
                if (n.equals("Prime")) installed.add(RemoteKeys.PRIME);
            }
            RemoteKeys.Decision d = keys.decide(Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4]), installed);
            String r;
            switch (d.kind) {
                case RemoteKeys.EAT: r = "EAT"; break;
                case RemoteKeys.OPEN_APP: r = "APP:" + d.pkg; break;
                case RemoteKeys.OPEN_OUR_SETTINGS: r = "OUR_SETTINGS"; break;
                case RemoteKeys.OPEN_SETTINGS: r = "TV_SETTINGS"; break;
                case RemoteKeys.OPEN_INPUTS: r = "INPUTS"; break;
                default: r = "PASS";
            }
            out.append(r).append('\n');
        }
        System.out.write(out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.flush();
    }
}
