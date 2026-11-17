package sa.urtv.home;

import java.util.Set;

/**
 * 4.3.3 «إضافات تلفزيونك»: the Home button on a Google TV. From 4.3.2 the stock home stays on (with it off some Google TVs cannot start), and on many of
 * them the Home key still opens the stock home even with ours the HOME role. The add-on's service sees which window comes to the front (only its package
 * and class, nothing in it): when the stock home's HOME screen comes up while the person chose our home, ours is brought back in front (couch-tv's «Home
 * button watch», github.com/Seagey/couch-tv, MIT). Never when ours is not the chosen home (the person's choice: 4.4, Android's default home, or the
 * setup's word that the person chose ours when the TV took the role back by itself). No Android in here (tests/HomeWatchRun.java runs it).
 *
 * 4.4 (step 88, the tester's R2g on the owner's Google TV: 64 of 64 Home presses, 3 of 3 boots, 30 minutes idle):
 * - the stock home over ANOTHER app is the person's Home press: always brought, never counted, even while paused;
 * - the stock home over OUR home is a fight: the 2-3 windows of one press (within 0.7 s) count once, and the 6th burst within 6 s pauses the
 *   watch, for 30 s. (The tester's 6 in 4 s could never trip: with one count per 0.7 s burst, a firmware bouncing its home back every 0.3-0.5 s
 *   counts about once a second, 4 in 4 s, and the fight would go on for ever; 6 in 6 s catches it. Presses over apps are never counted, so the
 *   64 of 64 is unchanged.)
 */
final class HomeWatch {
    static final long WINDOW_MS = 6_000, PAUSE_MS = 30_000, BURST_MS = 700;
    static final int MAX_BRINGS = 6;

    private final long[] brings = new long[MAX_BRINGS];
    private long pausedUntil, lastBurst;

    /** the stock home replaced ours (a fight), as {@link #decide(boolean, String, String, Set, boolean, long, boolean)} with {@code fight} true */
    boolean decide(boolean on, String pkg, String cls, Set<String> stockHomes, boolean oursChosen, long now) {
        return decide(on, pkg, cls, stockHomes, oursChosen, now, true);
    }

    /**
     * A window came to the front: bring ours? {@code on}: the watch is on; {@code pkg}/{@code cls}: the window's package and class; {@code stockHomes}:
     * the stock homes' HOME activities as "package/class" (ours, the setup wizard and the settings apps never in it); {@code oursChosen}: the person's
     * home is ours; {@code fight}: the stock home came over OUR home (false: over another app, the person pressed Home there).
     */
    boolean decide(boolean on, String pkg, String cls, Set<String> stockHomes, boolean oursChosen, long now, boolean fight) {
        if (!on || !oursChosen || pkg == null || cls == null || stockHomes == null) return false;
        if (!homeScreen(pkg, cls, stockHomes)) return false;
        if (!fight) return true;   // the person's press over an app: always, even while paused
        if (now < pausedUntil) return false;
        if (lastBurst > 0 && now - lastBurst < BURST_MS) return true;   // the same press's other windows: anchored to the burst's first (never extends itself)
        lastBurst = now;
        int recent = 0, oldest = 0;
        for (int i = 0; i < brings.length; i++) {
            if (brings[i] > 0 && now - brings[i] < WINDOW_MS) recent++;
            if (brings[i] < brings[oldest]) oldest = i;
        }
        if (recent >= MAX_BRINGS - 1) {   // this is the 6th burst within 6 s: ours keeps losing to the stock home (a firmware that pins it): step aside
            pausedUntil = now + PAUSE_MS;
            java.util.Arrays.fill(brings, 0);
            return false;
        }
        brings[oldest] = now;
        return true;
    }

    /** 4.4 R2g: the app Home was pressed over came back this soon after our bring (YouTube puts its screen back about 0.7 s later): ours once more */
    static final long COME_BACK_MS = 1_500;

    /** the app ({@code pkg}) is the one Home was pressed over, within COME_BACK_MS of our bring, and ours was not brought again yet */
    static boolean bringAgain(String pkg, String pressedOver, long broughtAt, boolean already, long now) {
        return pkg != null && pkg.equals(pressedOver) && broughtAt > 0 && now - broughtAt < COME_BACK_MS && !already;
    }

    boolean paused(long now) { return now < pausedUntil; }

    /** the packages of the stock homes' HOME activities ("package/class") */
    static Set<String> packages(Set<String> stockHomes) {
        Set<String> out = new java.util.HashSet<>();
        if (stockHomes != null) for (String s : stockHomes) { int i = s.indexOf('/'); if (i > 0) out.add(s.substring(0, i)); }
        return out;
    }

    /**
     * 4.4 (step 86 B): is this window of a stock home its HOME screen? Its HOME activity (or the alias's real one), or, on Google TV, any of its
     * activities named …HomeActivity (launcherx shows Home through more than one: the role went back to it and the add-on never answered). A window of
     * the stock home that is not a home screen («Your apps», its search, its settings) is left alone.
     */
    static boolean homeScreen(String pkg, String cls, Set<String> stockHomes) {
        if (pkg == null || cls == null || stockHomes == null) return false;
        if (stockHomes.contains(pkg + "/" + cls)) return true;
        if (!packages(stockHomes).contains(pkg)) return false;
        String simple = cls.substring(cls.lastIndexOf('.') + 1);
        return cls.startsWith(pkg + ".") && simple.endsWith("HomeActivity");
    }
}
