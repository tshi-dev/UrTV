package sa.urtv.home.setup;

import java.util.Arrays;
import java.util.LinkedHashSet;

/** For tests/safe_stock_test.py (4.4, step 100): the safe page's «افتح الشاشة الأصلية» rules (StockHome) and the turn-on (StockEnable) on SetupEngineRun's fake TV. */
public final class SafeStockRun {
    static final String LX = "com.google.android.apps.tv.launcherx";
    public static void main(String[] a) throws Exception {
        LinkedHashSet<String> off = new LinkedHashSet<>(Arrays.asList("com.google.android.tvlauncher", LX, "com.tcl.bi"));
        System.out.println("PICK " + sa.urtv.home.StockHome.disabledKnown(off) + " " + sa.urtv.home.StockHome.disabledKnown(new LinkedHashSet<>(Arrays.asList("com.tcl.bi", StockEnable.WRAITH)))
            + " " + sa.urtv.home.StockHome.disabledKnown(new LinkedHashSet<>(Arrays.asList("com.tcl.cyberui"))) + " " + sa.urtv.home.StockHome.disabledKnown(null));
        System.out.println("DECIDE " + sa.urtv.home.StockHome.decide(LX, true, true) + " " + sa.urtv.home.StockHome.decide(LX, false, false) + " " + sa.urtv.home.StockHome.decide(null, true, false)
            + " " + sa.urtv.home.StockHome.decide(null, false, true));
        // the owner's TCL G08: launcherx turned off by an earlier setup: turned back on, the TV says so
        SetupEngineRun.FakeTv t = SetupEngineRun.tv(); t.off.add(LX);
        boolean on = StockEnable.run(t, LX, sa.urtv.home.StockHome.known());
        System.out.println("ENABLE " + on + " off=" + t.off.contains(LX) + " cmds=" + t.count("pm enable " + LX));
        // the TV will not turn it on: said (false), so the page shows the way by hand
        SetupEngineRun.FakeTv t2 = SetupEngineRun.tv(); t2.off.add(LX); t2.stuck.add(LX);
        System.out.println("STUCK " + StockEnable.run(t2, LX, sa.urtv.home.StockHome.known()));
        // never the setup wizard, never a package that is not a known stock home
        SetupEngineRun.FakeTv t3 = SetupEngineRun.tv(); t3.off.add(StockEnable.WRAITH); t3.off.add("com.tcl.bi");
        System.out.println("NEVER " + StockEnable.run(t3, StockEnable.WRAITH, sa.urtv.home.StockHome.known()) + " " + StockEnable.run(t3, "com.tcl.bi", sa.urtv.home.StockHome.known()) + " cmds=" + t3.count("pm enable"));
    }
}
