package sa.urtv.home;

import java.util.HashSet;
import java.util.Set;

/** For tests/install_saver_test.py: SaverInstallRule's verdicts, one per line. S.t is not used here (refusal() is not called). */
public class SaverInstallRuleRun {
    static Set<String> set(String... d) { Set<String> s = new HashSet<>(); for (String x : d) s.add(x); return s; }

    public static void main(String[] a) {
        String k1 = "a".repeat(64), k2 = "b".repeat(64);
        System.out.println("missing " + SaverInstallRule.needsInstall(false) + " here " + SaverInstallRule.needsInstall(true));
        System.out.println("ok " + SaverInstallRule.verify("sa.urtv.screensaver", set(k1), set(k1)));
        System.out.println("other_app " + SaverInstallRule.verify("com.evil.app", set(k1), set(k1)));
        System.out.println("other_key " + SaverInstallRule.verify("sa.urtv.screensaver", set(k1), set(k2)));
        System.out.println("unsigned " + SaverInstallRule.verify("sa.urtv.screensaver", set(k1), set()));
        System.out.println("unsigned2 " + SaverInstallRule.verify("sa.urtv.screensaver", set(), set(k1)));
        System.out.println("badhash " + SaverInstallRule.verify("sa.urtv.screensaver", set("xyz"), set("xyz")));
        System.out.println("nullpkg " + SaverInstallRule.verify(null, set(k1), set(k1)));
        System.out.println("url " + SaverInstallRule.URL + " code " + SaverInstallRule.DOWNLOADER_CODE);
    }
}
