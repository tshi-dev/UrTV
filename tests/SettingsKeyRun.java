package sa.urtv.home;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

/** For tests/settings_key_test.py: the remote's Settings key, in front of our home and of another app, on tests/settings_key_cases.tsv.
 *  "reset": a new SettingsKey and a new RemoteKeys. "key": keyCode action repeat front (home | other) stockHome (on | off); the answer is
 *  what happens to the key: with our home in front SettingsKey must PASS and the home's own RemoteKeys decides (OUR_SETTINGS, PASS, ...);
 *  in another app SettingsKey answers PASS, EAT or TV_SETTINGS. "filter": front stockHome, answered on / off (does the service ask for keys).
 *  "stock": a comma list of the enabled HOME packages, answered yes / no. */
public final class SettingsKeyRun {
    public static void main(String[] args) throws IOException {
        StringBuilder out = new StringBuilder();
        SettingsKey sk = new SettingsKey();
        RemoteKeys home = new RemoteKeys();
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t", -1);
            String r;
            switch (f[0]) {
                case "reset": sk = new SettingsKey(); home = new RemoteKeys(); r = "-"; break;
                case "filter": r = SettingsKey.filterWanted(f[1].equals("home"), f[2].equals("on")) ? "on" : "off"; break;
                case "stock": r = SettingsKey.stockHomeOn(f[1].isEmpty() ? Collections.<String>emptyList() : Arrays.asList(f[1].split(",")), "sa.urtv.home") ? "yes" : "no"; break;
                default: {
                    int code = Integer.parseInt(f[1]), action = Integer.parseInt(f[2]), repeat = Integer.parseInt(f[3]);
                    boolean front = f[4].equals("home"), stock = f[5].equals("on");
                    int d = sk.decide(code, action, repeat, front, stock);
                    if (d == SettingsKey.EAT) r = "EAT";
                    else if (d == SettingsKey.OPEN_TV_SETTINGS) r = "TV_SETTINGS";
                    else if (!front) r = "PASS";
                    else {   // the key went on to our home: what its own key path does with it
                        RemoteKeys.Decision h = home.decide(code, 0, action, repeat, Collections.<String>emptySet());
                        r = h.kind == RemoteKeys.OPEN_OUR_SETTINGS ? "OUR_SETTINGS" : h.kind == RemoteKeys.EAT ? "HOME_EAT" : h.kind == RemoteKeys.PASS ? "PASS" : "HOME_OTHER";
                    }
                }
            }
            out.append(r).append('\n');
        }
        System.out.write(out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.flush();
    }
}
