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
        // the undo's backup check (tester step 18): a real list without our screensaver yes; with it, a short list, no backup: no
        StringBuilder bk = new StringBuilder("$ echo '== disabled packages'; pm list packages -d\n== disabled packages\npackage:sa.urtv.screensaver\n$ echo '== all packages'; pm list packages\n== all packages\npackage:android\n");
        for (int i = 0; i < 25; i++) bk.append("package:com.tv.app").append(i).append('\n');
        String withSaver = bk.toString() + "package:sa.urtv.screensaver\n";
        System.out.println("notBefore " + SetupInstall.notBefore(bk.toString(), "sa.urtv.screensaver") + " " + SetupInstall.notBefore(withSaver, "sa.urtv.screensaver")
            + " " + SetupInstall.notBefore("== all packages\npackage:android\n", "sa.urtv.screensaver") + " " + SetupInstall.notBefore(null, "sa.urtv.screensaver")
            + " " + SetupInstall.notBefore("", "sa.urtv.screensaver"));
        System.out.println("otherApp " + InstallRules.fresh(P, "com.evil.app", s(me), s(me)));
        System.out.println("noApp " + InstallRules.fresh(P, null, s(me), s(me)));
    }
}
