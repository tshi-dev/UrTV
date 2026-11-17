package sa.urtv.home.setup;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** tests/rescue_test.py runs this: what «إنقاذ تلفزيونك» sends (RescuePlan). One line per case: «ok …» or «BAD …». */
public final class RescuePlanRun {
    static int bad;
    static void check(String n, boolean ok) { System.out.println((ok ? "ok " : "BAD ") + n); if (!ok) bad++; }
    static Set<String> set(String... a) { return new LinkedHashSet<>(Arrays.asList(a)); }

    public static void main(String[] a) {
        String lx = RescuePlan.LAUNCHERX, wz = RescuePlan.WRAITH;
        Set<String> inst = set(lx, wz, "com.tcl.tv", "com.tcl.bi", "com.tcl.tv.tclhome_passive", "sa.urtv.home");
        RescuePlan.Plan p = RescuePlan.plan(inst, set(lx, wz, "com.tcl.bi", "com.tcl.tv.tclhome_passive"), null);
        check("no ledger (the home gone): the TV's own home, the wizard and the helpers on; a package the TV had off itself (com.tcl.bi) untouched",
            p.enabled.equals(Arrays.asList(lx, wz, "com.tcl.tv.tclhome_passive")) && !p.commands.contains("pm enable com.tcl.bi"));
        check("Home to the stock home, by role and home activity", p.commands.contains("cmd role add-role-holder --user 0 android.app.role.HOME " + lx)
            && p.commands.contains("cmd package set-home-activity --user 0 " + RescuePlan.LAUNCHERX_HOME) && p.home.equals(lx));
        check("the Home key last; never a removal or a disable, and the accessibility list is never touched", p.commands.get(p.commands.size() - 1).equals("input keyevent KEYCODE_HOME")
            && p.commands.stream().noneMatch(c -> c.contains("uninstall") || c.contains("disable") || c.contains("accessibility")));
        RescuePlan.Plan q = RescuePlan.plan(inst, set("com.tcl.bi", "com.netflix.ninja"), RescuePlan.ledger("com.netflix.ninja\n# x\nbad name\n"));
        check("with the home's ledger: what our setup turned off comes back too (and only valid names)", q.enabled.equals(Arrays.asList("com.netflix.ninja")));
        check("the TV did not say its packages: nothing at all", RescuePlan.plan(null, set(), null).commands.isEmpty() && RescuePlan.plan(inst, null, null).commands.isEmpty());
        check("an Android TV with tvlauncher: Home to it", RescuePlan.plan(set(RescuePlan.TVLAUNCHER), set(), null).home.equals(RescuePlan.TVLAUNCHER));
        check("packages(): the end marker is required", RescuePlan.packages("package:a\npackage:b\n") == null && RescuePlan.packages("package:a\r\nEND_OF_LIST").equals(set("a")));
        System.out.println(bad == 0 ? "ALL PASS" : bad + " FAILED");
        System.exit(bad == 0 ? 0 : 1);
    }
}
