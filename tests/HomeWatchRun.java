package sa.urtv.home;

import java.util.Collections;
import java.util.Set;

/** tests/extras_test.py runs this: HomeWatch, the Home button on a Google TV («إضافات تلفزيونك»). Prints one line per case: «ok …» or «BAD …». */
public final class HomeWatchRun {
    static int bad;
    static void check(String name, boolean ok) { System.out.println((ok ? "ok " : "BAD ") + name); if (!ok) bad++; }

    public static void main(String[] a) {
        String lx = "com.google.android.apps.tv.launcherx", home = lx + ".home.HomeActivity";
        Set<String> stock = Collections.singleton(lx + "/" + home);
        HomeWatch w = new HomeWatch();
        check("the stock home's HOME screen comes up while ours is the chosen home: ours is brought back", w.decide(true, lx, home, stock, true, 1_000));
        check("the watch is off: nothing", !new HomeWatch().decide(false, lx, home, stock, true, 1_000));
        check("ours is not the chosen home (the person's choice): nothing", !new HomeWatch().decide(true, lx, home, stock, false, 1_000));
        check("another screen of the stock home (not its HOME activity): nothing", !new HomeWatch().decide(true, lx, lx + ".settings.SettingsActivity", stock, true, 1_000));
        check("another app: nothing", !new HomeWatch().decide(true, "com.netflix.ninja", "com.netflix.ninja.MainActivity", stock, true, 1_000));
        check("the window names no package or class: nothing", !new HomeWatch().decide(true, null, home, stock, true, 1_000) && !new HomeWatch().decide(true, lx, null, stock, true, 1_000));
        HomeWatch k = new HomeWatch();
        boolean b1 = k.decide(true, lx, home, stock, true, 10_000), b2 = k.decide(true, lx, home, stock, true, 14_000), b3 = k.decide(true, lx, home, stock, true, 18_000);
        boolean b4 = k.decide(true, lx, home, stock, true, 22_000);
        check("three brings within 20 s, the stock home back a fourth time: it steps aside (the TV's home stays)", b1 && b2 && b3 && !b4 && k.paused(22_000));
        check("while it steps aside (five minutes): nothing", !k.decide(true, lx, home, stock, true, 22_000 + 4 * 60_000));
        check("after the five minutes: it brings ours again", k.decide(true, lx, home, stock, true, 22_000 + HomeWatch.PAUSE_MS + 1));
        HomeWatch s = new HomeWatch();
        boolean spaced = s.decide(true, lx, home, stock, true, 0 + 1) && s.decide(true, lx, home, stock, true, 30_000) && s.decide(true, lx, home, stock, true, 60_000) && s.decide(true, lx, home, stock, true, 90_000);
        check("brings spaced over minutes (the person pressing Home now and then): every one goes", spaced);
        // 4.4 (step 86 B): Google TV's Home through another activity, or an alias; the stock home's other screens are left alone
        Set<String> alias = new java.util.HashSet<>(java.util.Arrays.asList(lx + "/" + lx + ".StartupAlias"));
        check("the HOME answers through an alias, the window is named by its real …HomeActivity: it is the home screen", HomeWatch.homeScreen(lx, home, alias) && new HomeWatch().decide(true, lx, home, alias, true, 1_000));
        check("another …HomeActivity of the stock home (launcherx shows Home through more than one): the home screen", HomeWatch.homeScreen(lx, lx + ".main.MainHomeActivity", stock));
        check("«Your apps», its search or its settings: not its home screen", !HomeWatch.homeScreen(lx, lx + ".apps.AppsActivity", stock) && !HomeWatch.homeScreen(lx, lx + ".settings.SettingsActivity", stock));
        check("a …HomeActivity of ANOTHER app is never a stock home screen", !HomeWatch.homeScreen("com.example.tv", "com.example.tv.HomeActivity", stock));
        check("a view's class (not an activity of the stock home) is not its home screen", !HomeWatch.homeScreen(lx, "android.widget.FrameLayout", stock));
        check("the stock homes' packages", HomeWatch.packages(alias).equals(Collections.singleton(lx)));
        System.out.println(bad == 0 ? "ALL PASS" : bad + " FAILED");
        System.exit(bad == 0 ? 0 : 1);
    }
}
