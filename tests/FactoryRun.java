package sa.urtv.home.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/** For tests/factory_test.py: «تخلّص من واجهة المصنع» (SetupEngine.factoryOffer / factoryOff / restoreFactory) on SetupEngineRun's fake TV. One line per scenario. */
public final class FactoryRun {
    static String joined(List<String> l) { return String.join(",", new TreeSet<>(l)); }

    public static void main(String[] a) throws Exception {
        String stock = "com.google.android.apps.tv.launcherx";
        // F1: a healthy TV: it may go on; the plan is the stock home first, then the safe list installed and not off already; nothing changed; the safety switch tested and let go
        SetupEngineRun.FakeTv t1 = SetupEngineRun.tv(); SetupEngineRun.Mem m1 = new SetupEngineRun.Mem(); SetupEngineRun.Dead d1 = new SetupEngineRun.Dead();
        SetupEngine.FactoryOffer o1 = SetupEngineRun.engine(t1, m1, d1, new ArrayList<>()).factoryOffer();
        List<String> safeInstalled = new ArrayList<>();
        for (String p : SetupEngine.filterSafe(SetupLists.SAFE)) if (t1.installed.contains(p) && !t1.off.contains(p)) safeInstalled.add(p);
        System.out.println("F1 " + (o1.refusal == null) + " first=" + o1.packages.get(0) + " plan=" + joined(o1.packages).equals(joined(add(stock, safeInstalled))) + " changed=" + (t1.count("pm disable-user") + t1.count("pm enable"))
                + " armed=" + d1.armed + " disarmed=" + d1.disarmed);
        // F2: the setup wizard owns the saved Wi-Fi: refused with the reason, nothing changed
        SetupEngineRun.FakeTv t2 = SetupEngineRun.tv(); t2.wizardAnswersHome = true; t2.wizardOwnsWifi = true; t2.roleHolder = stock;
        SetupEngine.FactoryOffer o2 = SetupEngineRun.engine(t2, new SetupEngineRun.Mem(), new SetupEngineRun.Dead(), new ArrayList<>()).factoryOffer();
        System.out.println("F2 " + (o2.refusal != null && o2.refusal.contains("الواي فاي")) + " changed=" + (t2.count("pm disable-user") + t2.count("cmd role")));
        // F3: the safety switch cannot be set: refused with the reason
        SetupEngineRun.FakeTv t3 = SetupEngineRun.tv(); SetupEngineRun.Dead d3 = new SetupEngineRun.Dead(); d3.works = false;
        SetupEngine.FactoryOffer o3 = SetupEngineRun.engine(t3, new SetupEngineRun.Mem(), d3, new ArrayList<>()).factoryOffer();
        System.out.println("F3 " + (o3.refusal != null && o3.refusal.contains("مفتاح الأمان")) + " changed=" + t3.count("pm disable-user"));
        // F4: it turns the stock interface off: the stock home and the safe list, written in the ledger first; the protected features are never in it; nothing uninstalled
        SetupEngineRun.FakeTv t4 = SetupEngineRun.tv(); SetupEngineRun.Mem m4 = new SetupEngineRun.Mem(); SetupEngineRun.Dead d4 = new SetupEngineRun.Dead();
        String before = SetupEngineRun.snapshot(t4);
        SetupEngine e4 = SetupEngineRun.engine(t4, m4, d4, new ArrayList<>());
        SetupEngine.Result r4 = e4.factoryOff();
        List<String> ledger4 = SetupEngine.ledgerNames(m4.get("disabled-by-us.txt"));
        boolean keepTouched = false;
        for (String k : SetupLists.KEEP) if (t4.off.contains(k) && !k.equals(SetupEngine.WRAITH) && !k.equals(stock)) { keepTouched = true; System.out.println("KEEPOFF " + k); }
        System.out.println("F4 " + r4.status + " stockOff=" + t4.off.contains(stock) + " ledgerHasStock=" + ledger4.contains(stock) + " ledgerHasSafe=" + ledger4.containsAll(safeInstalled)
                + " keepTouched=" + keepTouched + " uninstall=" + (t4.count("pm uninstall") + t4.count("pm clear")) + " mark=" + m4.get(SetupEngine.FACTORY).startsWith("on") + " isOff=" + e4.factoryIsOff());
        // F5: the one-press restore: exactly the ledger and the stock home come back; the TV is as it was; the ledger is empty; only the ledger, the stock home, the wizard and the home's helpers were enabled
        SetupEngine.Result u5 = e4.restoreFactory();
        List<String> enabled = new ArrayList<>();
        for (String l : t4.log) if (l.startsWith("pm enable ")) enabled.add(l.substring("pm enable ".length()).replace("--user 0 ", "").trim());
        TreeSet<String> allowed = new TreeSet<>(ledger4);
        allowed.add(stock); allowed.add(SetupEngine.WRAITH); allowed.addAll(Arrays.asList(SetupLists.HOME_HELPERS));
        TreeSet<String> extra = new TreeSet<>(enabled); extra.removeAll(allowed);
        System.out.println("F5 " + u5.status + " same=" + SetupEngineRun.snapshot(t4).equals(before) + " home=" + t4.home().startsWith(stock) + " ledgerEmpty=" + m4.get("disabled-by-us.txt").trim().isEmpty()
                + " enabledAllLedger=" + enabled.containsAll(ledger4) + " extraEnabled=[" + String.join(",", extra) + "] mark=" + m4.get(SetupEngine.FACTORY).startsWith("off") + " isOff=" + e4.factoryIsOff());
        // F6: the restore does not run the full undo: nothing of ours is uninstalled, the screensaver and the tweaks are left, the apps the setup installed stay
        System.out.println("F6 uninstall=" + (t4.count("pm uninstall")) + " settingsPut=" + (t4.count("settings put") + t4.count("settings delete")) + " appops=" + t4.count("appops set"));
        // F7: a package that does not come back stays in the ledger for the next try
        SetupEngineRun.FakeTv t7 = SetupEngineRun.tv(); SetupEngineRun.Mem m7 = new SetupEngineRun.Mem();
        SetupEngine e7 = SetupEngineRun.engine(t7, m7, new SetupEngineRun.Dead(), new ArrayList<>());
        e7.factoryOff();
        String stuck = SetupEngine.ledgerNames(m7.get("disabled-by-us.txt")).get(1);
        t7.stuck.add(stuck);
        SetupEngine.Result r7 = e7.restoreFactory();
        System.out.println("F7 " + r7.status + " ledger=" + SetupEngine.ledgerNames(m7.get("disabled-by-us.txt")).equals(Arrays.asList(stuck)) + " mark=" + m7.get(SetupEngine.FACTORY).startsWith("on"));
    }

    static List<String> add(String first, List<String> rest) { List<String> l = new ArrayList<>(); l.add(first); l.addAll(rest); return l; }
}
