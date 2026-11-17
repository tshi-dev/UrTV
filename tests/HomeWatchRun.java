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
        // 4.4 R2g (step 88): a fight over OUR home: one burst (0.7 s) counts once; 6 bursts within 4 s pause the watch for 30 s
        HomeWatch k = new HomeWatch();
        boolean five = true;
        for (int i = 0; i < 5; i++) five &= k.decide(true, lx, home, stock, true, 10_000 + i * 800L);
        boolean sixth = k.decide(true, lx, home, stock, true, 14_000);
        check("a firmware fighting over ours: five bursts brought, the sixth within 6 s pauses the watch (the TV's home stays)", five && !sixth && k.paused(14_000));
        check("while paused (30 s) a fight over ours is left", !k.decide(true, lx, home, stock, true, 14_000 + 20_000));
        check("while paused, the person's press over ANOTHER app is still brought (never counted)", k.decide(true, lx, home, stock, true, 14_000 + 20_000, false));
        check("after the 30 s: ours again", k.decide(true, lx, home, stock, true, 14_000 + HomeWatch.PAUSE_MS + 1));
        HomeWatch m = new HomeWatch();
        boolean mash = true;
        for (int i = 0; i < 4; i++) mash &= m.decide(true, lx, home, stock, true, 50_000 + i * 1_000L);
        check("a person pressing Home over ours once a second, four times: all brought, no pause", mash && !m.paused(54_000));
        HomeWatch s1 = new HomeWatch();
        boolean spaced = s1.decide(true, lx, home, stock, true, 1) && s1.decide(true, lx, home, stock, true, 30_000) && s1.decide(true, lx, home, stock, true, 60_000) && s1.decide(true, lx, home, stock, true, 90_000);
        check("brings spaced over minutes (the person pressing Home now and then): every one goes", spaced);
        // 4.4 (step 86 B): Google TV's Home through another activity, or an alias; the stock home's other screens are left alone
        Set<String> alias = new java.util.HashSet<>(java.util.Arrays.asList(lx + "/" + lx + ".StartupAlias"));
        check("the HOME answers through an alias, the window is named by its real …HomeActivity: it is the home screen", HomeWatch.homeScreen(lx, home, alias) && new HomeWatch().decide(true, lx, home, alias, true, 1_000));
        check("another …HomeActivity of the stock home (launcherx shows Home through more than one): the home screen", HomeWatch.homeScreen(lx, lx + ".main.MainHomeActivity", stock));
        check("«Your apps», its search or its settings: not its home screen", !HomeWatch.homeScreen(lx, lx + ".apps.AppsActivity", stock) && !HomeWatch.homeScreen(lx, lx + ".settings.SettingsActivity", stock));
        check("a …HomeActivity of ANOTHER app is never a stock home screen", !HomeWatch.homeScreen("com.example.tv", "com.example.tv.HomeActivity", stock));
        check("a view's class (not an activity of the stock home) is not its home screen", !HomeWatch.homeScreen(lx, "android.widget.FrameLayout", stock));
        check("the stock homes' packages", HomeWatch.packages(alias).equals(Collections.singleton(lx)));
        // 4.4 R2g (step 88): one Home press = 2-3 stock windows within 0.5 s; presses over apps never counted (64 of 64 on the owner's Google TV)
        HomeWatch p = new HomeWatch();
        boolean all = true;
        for (int i = 0; i < 64; i++) { long t = 100_000 + i * 900L; all &= p.decide(true, lx, home, stock, true, t, false) && p.decide(true, lx, home, stock, true, t + 300, false) && p.decide(true, lx, home, stock, true, t + 500, false); }
        check("64 Home presses over apps, three stock windows each, 0.9 s apart: all brought, never paused", all && !p.paused(200_000));
        HomeWatch q = new HomeWatch();
        boolean b1 = q.decide(true, lx, home, stock, true, 1_000), b2 = q.decide(true, lx, home, stock, true, 1_300), b3 = q.decide(true, lx, home, stock, true, 1_600);
        check("a burst over ours (three windows within 0.7 s) is brought each time and counts once", b1 && b2 && b3);
        HomeWatch loop = new HomeWatch();
        boolean tripped = false;
        for (int i = 0; i < 40 && !tripped; i++) { loop.decide(true, lx, home, stock, true, 1_000 + i * 500L); tripped = loop.paused(1_000 + i * 500L); }
        check("a true loop over ours (back every 0.5 s) still trips the guard within seconds: the burst window never extends itself", tripped);
        check("R2g: the app Home was pressed over comes back within 1.5 s of our bring: ours once more, only once, only that app",
              HomeWatch.bringAgain("com.google.android.youtube.tv", "com.google.android.youtube.tv", 10_000, false, 10_700)
              && !HomeWatch.bringAgain("com.google.android.youtube.tv", "com.google.android.youtube.tv", 10_000, true, 10_700)
              && !HomeWatch.bringAgain("com.google.android.youtube.tv", "com.google.android.youtube.tv", 10_000, false, 11_600)
              && !HomeWatch.bringAgain("com.netflix.ninja", "com.google.android.youtube.tv", 10_000, false, 10_700)
              && !HomeWatch.bringAgain("com.google.android.youtube.tv", null, 10_000, false, 10_700));
        System.out.println(bad == 0 ? "ALL PASS" : bad + " FAILED");
        System.exit(bad == 0 ? 0 : 1);
    }
}
