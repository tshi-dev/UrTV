package sa.urtv.home.setup;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** InstallRules and SetupInstall.pickAsset for tests/setup_install_test.py: `rules`, or `pick <pattern> <abis> <release.json>`. */
public class InstallRun {
    static Set<String> s(String... a) { return new HashSet<>(Arrays.asList(a)); }

    public static void main(String[] a) throws Exception {
        if (a[0].equals("pick")) {
            System.out.print(String.valueOf(SetupInstall.pickAsset(a[1], a[2], new String(Files.readAllBytes(Paths.get(a[3])), StandardCharsets.UTF_8))));
            return;
        }
        String me = "ab".repeat(32), other = "cd".repeat(32), third = "ef".repeat(32);
        String P = "sa.urtv.screensaver";
        System.out.println("same " + InstallRules.fresh(P, P, s(me), s(me)));
        System.out.println("sameTwo " + InstallRules.fresh(P, P, s(me, third), s(third, me)));
        System.out.println("other " + InstallRules.fresh(P, P, s(me), s(other)));
        System.out.println("subset " + InstallRules.fresh(P, P, s(me, third), s(me)));
        System.out.println("superset " + InstallRules.fresh(P, P, s(me), s(me, other)));
        System.out.println("unsigned " + InstallRules.fresh(P, P, s(me), Collections.<String>emptySet()));
        System.out.println("unsignedNull " + InstallRules.fresh(P, P, s(me), null));
        System.out.println("selfUnknown " + InstallRules.fresh(P, P, Collections.<String>emptySet(), s(me)));
        System.out.println("notHex " + InstallRules.fresh(P, P, s("x"), s("x")));
        System.out.println("otherApp " + InstallRules.fresh(P, "com.evil.app", s(me), s(me)));
        System.out.println("noApp " + InstallRules.fresh(P, null, s(me), s(me)));
    }
}
