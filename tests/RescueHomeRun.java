package sa.urtv.home.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

/** For tests/rescue_home_test.py (4.4, R7): the rescue's fallback home (RescueHome) and the engine around it, on SetupEngineRun's fake TV. One line per scenario. */
public final class RescueHomeRun {
    static final String STOCK = "com.google.android.apps.tv.launcherx", TVL = "com.google.android.tvlauncher";

    public static void main(String[] a) throws Exception {
        // the pure rules
        System.out.println("ARMED " + RescueHome.armed("  sa.urtv.home/.MainActivity\n    " + RescueHome.ALIAS + "\r\n") + " " + RescueHome.armed("  sa.urtv.home/.MainActivity\n") + " " + RescueHome.armed(null));
        System.out.println("STOCKON " + RescueHome.stockHomeOn(Arrays.asList("sa.urtv.home", "sa.urtv.rescue", "com.android.tv.settings", "com.google.android.tungsten.setupwraith"))
            + " " + RescueHome.stockHomeOn(Arrays.asList("sa.urtv.rescue", STOCK)) + " " + RescueHome.stockHomeOn(Collections.<String>emptyList()) + " " + RescueHome.stockHomeOn(null));
        System.out.println("PKGS " + RescueHome.packages("2 activities found:\n  Activity #0:\n    priority=0\n    " + STOCK + "/.home.HomeActivity\n    sa.urtv.rescue/sa.urtv.home.setup.RescueHome\n"));
        LinkedHashSet<String> both = new LinkedHashSet<>(Arrays.asList(STOCK, TVL));
        System.out.println("GOOGLE " + StockMigration.googleStockOff(both, both, STOCK, TVL) + " " + StockMigration.googleStockOff(both, new LinkedHashSet<>(Collections.singletonList(TVL)), STOCK, TVL)
            + " " + StockMigration.googleStockOff(both, new LinkedHashSet<String>(), STOCK, TVL) + " " + StockMigration.googleStockOff(new LinkedHashSet<>(Collections.singletonList("com.tcl.home")), new LinkedHashSet<>(Collections.singletonList("com.tcl.home")), STOCK, TVL));

        // R1: a TV where it is needed and the rescue app is there: the alias is armed and read back BEFORE the stock home goes off; Home stays ours
        SetupEngineRun.FakeTv t1 = SetupEngineRun.factoryTv(); SetupEngineRun.Mem m1 = SetupEngineRun.factoryMem();
        SetupEngine e1 = SetupEngineRun.engine(t1, m1, new SetupEngineRun.Dead(), new ArrayList<>());
        SetupEngine.Result r1 = e1.factoryOff();
        int arm = t1.log.indexOf(RescueHome.ARM), off = t1.log.indexOf("pm disable-user --user 0 " + STOCK);
        System.out.println("R1 " + r1.status + " armed=" + t1.rescueArmed + " armBeforeStockOff=" + (arm >= 0 && off > arm) + " stockOff=" + t1.off.contains(STOCK) + " homeOurs=" + t1.home().startsWith("sa.urtv.home/"));
        // R2: the one-press restore: the stock home is back and the alias is disarmed
        SetupEngine.Result r2 = e1.restoreFactory();
        System.out.println("R2 " + r2.status + " armed=" + t1.rescueArmed + " stockOn=" + !t1.off.contains(STOCK));
        // R3: no rescue app: refused with the reason, nothing changed
        SetupEngineRun.FakeTv t3 = SetupEngineRun.tv(); SetupEngine e3 = SetupEngineRun.engine(t3, SetupEngineRun.factoryMem(), new SetupEngineRun.Dead(), new ArrayList<>());
        SetupEngine.FactoryOffer o3 = e3.factoryOffer();
        SetupEngine.Result r3 = e3.factoryOff();
        System.out.println("R3 offer=" + RescueHome.NEED.equals(o3.refusal) + " " + r3.status + " changes=" + (t3.count("pm disable-user") + t3.count("pm enable") + t3.count("cmd role add")));
        // R4: the TV refuses to turn the alias on: nothing turned off, the alias left off
        SetupEngineRun.FakeTv t4 = SetupEngineRun.factoryTv(); t4.rescueRefuses = true;
        SetupEngine.Result r4 = SetupEngineRun.engine(t4, SetupEngineRun.factoryMem(), new SetupEngineRun.Dead(), new ArrayList<>()).factoryOff();
        System.out.println("R4 " + r4.status + " said=" + RescueHome.NOT_ARMED.equals(r4.message) + " disables=" + t4.count("pm disable-user") + " armed=" + t4.rescueArmed + " disarmSent=" + (t4.count("pm disable " + RescueHome.ALIAS) > 0));
        // R5: the switch never failed here (Home opens ours with the role and the add-on): not offered
        SetupEngineRun.FakeTv t5 = SetupEngineRun.factoryTv();
        SetupEngine.FactoryOffer o5 = SetupEngineRun.engine(t5, new SetupEngineRun.Mem(), new SetupEngineRun.Dead(), new ArrayList<>()).factoryOffer();
        System.out.println("R5 notNeeded=" + RescueHome.NOT_NEEDED.equals(o5.refusal) + " changes=" + (t5.count("pm disable-user") + t5.count("pm enable")));
        // R6: a TV switched the old way from the PC (no journal here): Google's stock home off, nothing else answers Home: turned back on, ours stays Home, once
        SetupEngineRun.FakeTv t6 = SetupEngineRun.tv(); t6.off.add(STOCK); t6.roleHolder = "sa.urtv.home"; t6.stockPkg = "sa.urtv.home"; SetupEngineRun.Mem m6 = new SetupEngineRun.Mem();
        SetupEngine e6 = SetupEngineRun.engine(t6, m6, new SetupEngineRun.Dead(), new ArrayList<>());
        SetupEngine.Result r6 = e6.migrateStockNoJournal();
        SetupEngine.Result r6b = e6.migrateStockNoJournal();
        System.out.println("R6 " + (r6 == null ? "null" : r6.status) + " stockOn=" + !t6.off.contains(STOCK) + " role=" + t6.roleHolder + " mark=" + String.valueOf(m6.get(StockMigration.MARK)).startsWith("done") + " again=" + (r6b == null));
        // R7: a stock home already answers Home: nothing is done, the mark says none
        SetupEngineRun.FakeTv t7 = SetupEngineRun.tv(); SetupEngineRun.Mem m7 = new SetupEngineRun.Mem();
        SetupEngine.Result r7 = SetupEngineRun.engine(t7, m7, new SetupEngineRun.Dead(), new ArrayList<>()).migrateStockNoJournal();
        System.out.println("R7 " + (r7 == null ? "null" : r7.status) + " enables=" + t7.count("pm enable") + " mark=" + String.valueOf(m7.get(StockMigration.MARK)).startsWith("none"));
        // R8: the person chose «عطّل واجهة المصنع»: never undone at start
        SetupEngineRun.FakeTv t8 = SetupEngineRun.tv(); t8.off.add(STOCK); t8.stockPkg = "sa.urtv.home"; SetupEngineRun.Mem m8 = new SetupEngineRun.Mem(); m8.put(SetupEngine.FACTORY, "on x\n");
        SetupEngine.Result r8 = SetupEngineRun.engine(t8, m8, new SetupEngineRun.Dead(), new ArrayList<>()).migrateStockNoJournal();
        System.out.println("R8 " + (r8 == null ? "null" : r8.status) + " enables=" + t8.count("pm enable") + " stockOff=" + t8.off.contains(STOCK));
    }
}
