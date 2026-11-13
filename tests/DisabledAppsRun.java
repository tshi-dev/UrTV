package sa.urtv.home.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** For tests/disabled_apps_test.py: «التطبيقات المعطّلة» (DisabledApps.rows, SetupEngine.enableApps) on SetupEngineRun's fake TV. One line per scenario: name TAB ok|FAIL. */
public final class DisabledAppsRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] x) throws Exception {
        // the rows: off by hand (2, 3) only; «disabled until used» (4) and on (1) never; our own apps and Android never; UrTV's first, then by name
        List<DisabledApps.Row> rows = DisabledApps.rows(Arrays.asList("com.b.app", "com.a.app", "com.c.app", "sa.urtv.home", "android", "com.d.app", "com.e.app"),
            Arrays.asList("Beta", "alpha", "", "Home", "Android", "Delta", "Echo"), Arrays.asList(3, 2, 3, 3, 3, 4, 1), Arrays.asList("com.b.app"));
        check("rows: only apps off by hand, no waiting-for-first-use, no running ones, none of ours, UrTV's first then by name",
            rows.size() == 3 && rows.get(0).pkg.equals("com.b.app") && rows.get(0).byUrtv && rows.get(1).pkg.equals("com.a.app") && !rows.get(1).byUrtv && rows.get(2).name.equals("com.c.app")
            && DisabledApps.byUrtv(rows) == 1);
        // the engine: two of three come back; the third is refused by the TV; the ledger keeps only the refused one and the community record likewise
        SetupEngineRun.FakeTv t = SetupEngineRun.tv(); SetupEngineRun.Mem m = new SetupEngineRun.Mem();
        t.installed.addAll(Arrays.asList("com.a.app", "com.b.app", "com.c.app")); t.off.addAll(Arrays.asList("com.a.app", "com.b.app", "com.c.app")); t.stuck.add("com.c.app");
        m.put("disabled-by-us.txt", "com.a.app\ncom.c.app\n"); m.put("community-applied.txt", "com.a.app\ncom.b.app\n");
        SetupEngine e = SetupEngineRun.engine(t, m, new SetupEngineRun.Dead(), new ArrayList<>());
        List<SetupEngine.AppResult> r = e.enableApps(Arrays.asList("com.a.app", "com.b.app", "com.c.app", "not a package; rm -rf /"));
        check("enableApps: each asked app answered (a stranger's text dropped), the worked ones ok, the refused one with a reason",
            r.size() == 3 && r.get(0).ok && r.get(1).ok && !r.get(2).ok && !r.get(2).why.isEmpty() && !t.off.contains("com.a.app") && t.off.contains("com.c.app"));
        check("enableApps: the ledger keeps only what is still off (the refused one), the community record the same",
            SetupEngine.ledgerNames(m.get("disabled-by-us.txt")).equals(Arrays.asList("com.c.app")) && SetupEngine.ledgerNames(m.get("community-applied.txt")).isEmpty());
        check("enableApps only ever turns on: no disable, uninstall, clear or settings change", t.count("pm disable") == 0 && t.count("pm uninstall") == 0 && t.count("pm clear") == 0 && t.count("settings put") == 0 && t.count("pm enable --user 0") == 3);
        check("the reasons are plain Arabic words, never the TV's raw text", SetupEngine.enableWhy("java.lang.SecurityException: Neither user 2000 nor current process has permission").equals("التلفزيون ما سمح بتشغيله")
            && SetupEngine.enableWhy("Error: Unknown package: x").equals("التطبيق ما عاد موجود على التلفزيون") && SetupEngine.enableWhy("").equals("التلفزيون ما شغّله"));
        System.exit(0);
    }
}
