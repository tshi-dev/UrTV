package sa.urtv.home.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** For tests/home_role_test.py (4.5): the protected way to the HOME role (HomeRole, SetupEngine.protectedRole) on SetupEngineRun's fake TV. One line per scenario. */
public final class HomeRoleRun {
    static final String STOCK = "com.google.android.apps.tv.launcherx";

    static String changes(SetupEngineRun.FakeTv t) {
        int dis = 0, uninst = 0;
        for (String l : t.log) { if ((l.startsWith("pm disable") && !l.startsWith("pm disable " + RescueHome.ALIAS)) || l.startsWith("pm hide")) dis++; if (l.startsWith("pm uninstall") || l.startsWith("cmd package uninstall")) uninst++; }
        return "disables=" + dis + " uninstalls=" + uninst;
    }

    static int idx(SetupEngineRun.FakeTv t, String prefix) { for (int i = 0; i < t.log.size(); i++) if (t.log.get(i).startsWith(prefix)) return i; return -1; }

    static SetupEngineRun.FakeTv sticky() { SetupEngineRun.FakeTv t = SetupEngineRun.tv(); t.stickyRole = true; t.roleHolder = STOCK; t.sdk = "30"; return t; }

    public static void main(String[] a) throws Exception {
        // the pure rules
        System.out.println("BRAND " + HomeRole.brandOf(STOCK) + " " + HomeRole.brandOf("com.tcl.cyberui") + " " + HomeRole.brandOf("org.droidtv.ui.home") + " " + HomeRole.brandOf("com.example.app").isEmpty()
            + " " + HomeRole.isStockHome("com.sony.dtv.homemenu"));
        System.out.println("REMOVABLE " + HomeRole.removable(STOCK) + " " + HomeRole.removable("com.google.android.tungsten.setupwraith") + " " + HomeRole.removable("com.android.tv.settings")
            + " " + HomeRole.removable("sa.urtv.home") + " " + HomeRole.removable("bad name") + " " + HomeRole.removable(""));
        boolean threw = false;
        try { HomeRole.record("x; rm -rf /"); } catch (IllegalArgumentException e) { threw = true; }
        System.out.println("RECORD " + HomeRole.record(STOCK).equals("printf '%s\\n' " + STOCK + " > " + HomeRole.JOURNAL) + " rejects=" + threw
            + " parse=" + HomeRole.parse("\n  " + STOCK + "\r\n") + " wizard=[" + HomeRole.parse("com.google.android.tungsten.setupwraith\n") + "] junk=[" + HomeRole.parse("a b c") + "]");
        System.out.println("ALONE " + HomeRole.addAloneFailed(30, 0, STOCK, "sa.urtv.home") + " " + HomeRole.addAloneFailed(30, 0, "sa.urtv.home", "sa.urtv.home")
            + " " + HomeRole.addAloneFailed(28, 1, STOCK, "sa.urtv.home") + " " + HomeRole.addAloneFailed(29, 1, "sa.urtv.home", "sa.urtv.home"));

        // H1: add alone does not take (Android 11): recorded outside the app's data, then taken off, ours added, a real Home press opens ours; nothing disabled
        SetupEngineRun.FakeTv t1 = sticky(); SetupEngineRun.Mem m1 = new SetupEngineRun.Mem(); SetupEngineRun.Dead d1 = new SetupEngineRun.Dead(); List<String> s1 = new ArrayList<>();
        SetupEngine e1 = SetupEngineRun.engine(t1, m1, d1, s1);
        SetupEngine.Result r1 = e1.home();
        int rec = idx(t1, "printf '%s\\n' " + STOCK), rem = idx(t1, "cmd role remove-role-holder"), addOurs = -1;
        for (int i = rem + 1; i < t1.log.size() && rem >= 0; i++) if (t1.log.get(i).equals(HomeRole.add("sa.urtv.home"))) { addOurs = i; break; }
        System.out.println("H1 " + r1.status + " role=" + t1.roleHolder + " file=" + t1.roleFile + " order=" + (rec >= 0 && rec < rem && rem < addOurs) + " armed=" + d1.armed + " disarmed=" + (d1.disarmed > 0)
            + " " + changes(t1) + " stockOn=" + !t1.off.contains(STOCK) + " said=" + HomeRole.DONE.equals(r1.message));
        // H1u: «تراجع» gives the role back to the recorded holder and lets the record go
        SetupEngine.Result u1 = e1.undo();
        System.out.println("H1u " + u1.status + " role=" + t1.roleHolder + " file=" + t1.roleFile);
        // H2: the record cannot be written: nothing is taken off the role
        SetupEngineRun.FakeTv t2 = sticky(); t2.recordFails = true;
        SetupEngine.Result r2 = SetupEngineRun.engine(t2, new SetupEngineRun.Mem(), new SetupEngineRun.Dead(), new ArrayList<>()).home();
        System.out.println("H2 " + r2.status + " removes=" + t2.count("cmd role remove-role-holder") + " role=" + t2.roleHolder + " " + changes(t2));
        // H3: after the holder is taken off, ours is refused: the recorded holder gets the role back at once, said; the record let go; the safety switch let go
        SetupEngineRun.FakeTv t3 = sticky(); t3.addFailsAfterRemove = true; SetupEngineRun.Dead d3 = new SetupEngineRun.Dead(); List<String> s3 = new ArrayList<>();
        SetupEngine.Result r3 = SetupEngineRun.engine(t3, new SetupEngineRun.Mem(), d3, s3).home();
        System.out.println("H3 " + r3.status + " role=" + t3.roleHolder + " file=" + t3.roleFile + " said=" + String.join("|", s3).contains(HomeRole.ADD_FAILED + HomeRole.PUT_BACK_OK)
            + " disarmed=" + (d3.disarmed > 0) + " " + changes(t3));
        // H4: ours has the role but the Home key still opens the stock home: put back at once, said
        SetupEngineRun.FakeTv t4 = sticky(); t4.refuseSwitch = true; List<String> s4 = new ArrayList<>();
        SetupEngine.Result r4 = SetupEngineRun.engine(t4, new SetupEngineRun.Mem(), new SetupEngineRun.Dead(), s4).home();
        System.out.println("H4 " + r4.status + " role=" + t4.roleHolder + " file=" + t4.roleFile + " said=" + String.join("|", s4).contains(HomeRole.VERIFY_FAILED) + " " + changes(t4));
        // H5: the TV ignores the take-off: ours never gets the role: put back (the stock holder never lost it), said
        SetupEngineRun.FakeTv t5 = sticky(); t5.removeIgnored = true; List<String> s5 = new ArrayList<>();
        SetupEngine.Result r5 = SetupEngineRun.engine(t5, new SetupEngineRun.Mem(), new SetupEngineRun.Dead(), s5).home();
        System.out.println("H5 " + r5.status + " role=" + t5.roleHolder + " said=" + String.join("|", s5).contains(HomeRole.ADD_FAILED));
        // H6: the role's holder is the setup wizard: never taken off
        SetupEngineRun.FakeTv t6 = sticky(); t6.roleHolder = "com.google.android.tungsten.setupwraith";
        SetupEngine.Result r6 = SetupEngineRun.engine(t6, new SetupEngineRun.Mem(), new SetupEngineRun.Dead(), new ArrayList<>()).home();
        System.out.println("H6 " + r6.status + " removes=" + t6.count("cmd role remove-role-holder") + " records=" + t6.count("printf"));
        // H7: Android 9 (no role): the protected way is never taken
        SetupEngineRun.FakeTv t7 = sticky(); t7.sdk = "28";
        SetupEngineRun.engine(t7, new SetupEngineRun.Mem(), new SetupEngineRun.Dead(), new ArrayList<>()).home();
        System.out.println("H7 removes=" + t7.count("cmd role remove-role-holder") + " records=" + t7.count("printf"));
        // H8: add alone works (the usual TV): nothing is recorded or taken off
        SetupEngineRun.FakeTv t8 = SetupEngineRun.tv(); t8.sdk = "30";
        SetupEngineRun.engine(t8, new SetupEngineRun.Mem(), new SetupEngineRun.Dead(), new ArrayList<>()).home();
        System.out.println("H8 removes=" + t8.count("cmd role remove-role-holder") + " records=" + t8.count("printf"));

        // the rescue app: the recorded holder gets the role back, then the record is let go
        Set<String> inst = new LinkedHashSet<>(Arrays.asList("com.tcl.cyberui", "sa.urtv.home"));
        RescuePlan.Plan p = RescuePlan.plan(inst, new LinkedHashSet<String>(), null, "", "com.tcl.cyberui\n");
        Set<String> inst2 = new LinkedHashSet<>(Arrays.asList(STOCK, "sa.urtv.home"));
        RescuePlan.Plan q = RescuePlan.plan(inst2, new LinkedHashSet<String>(), null, "", STOCK + "\n");
        RescuePlan.Plan n = RescuePlan.plan(inst2, new LinkedHashSet<String>(), null, "", "");
        System.out.println("RESCUE " + p.home + " " + p.commands.contains(HomeRole.add("com.tcl.cyberui")) + " clear=" + p.commands.contains(HomeRole.CLEAR)
            + " google=" + q.home + " clear=" + q.commands.contains(HomeRole.CLEAR) + " none=" + !n.commands.contains(HomeRole.CLEAR));
    }
}
