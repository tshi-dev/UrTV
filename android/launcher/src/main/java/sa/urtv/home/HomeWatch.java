package sa.urtv.home;

import java.util.Set;

/**
 * 4.3.3 «إضافات تلفزيونك»: the Home button on a Google TV. From 4.3.2 the stock home stays on (with it off some Google TVs cannot start), and on many of
 * them the Home key still opens the stock home even with ours the HOME role. The add-on's service sees which window comes to the front (only its package
 * and class, nothing in it): when the stock home's HOME screen comes up while the person chose our home, ours is brought back in front (couch-tv's «Home
 * button watch», github.com/Seagey/couch-tv, MIT). Never when ours is not the chosen home (the person's choice: 4.4, Android's default home, or the
 * setup's word that the person chose ours when the TV took the role back by itself), and it steps aside when the stock home
 * keeps coming back (three times within 20 s: five minutes off, the stock home stays). No Android in here (tests/HomeWatchRun.java runs it).
 */
final class HomeWatch {
    static final long WINDOW_MS = 4_000, PAUSE_MS = 30_000, BURST_MS = 700;   // tester-exp (step 88 R2d)
    static final int MAX_BRINGS = 6;

    private final long[] brings = new long[MAX_BRINGS];
    private long pausedUntil, lastBring, lastBurst;
    static final long SAME_PRESS_MS = 1_500;

    /**
     * A window came to the front: bring ours? {@code on}: the watch is on; {@code pkg}/{@code cls}: the window's package and class; {@code stockHomes}:
     * the stock homes' HOME activities as "package/class" (ours, the setup wizard and the settings apps never in it); {@code oursChosen}: Android's
     * default home is ours.
     */
    boolean decide(boolean on, String pkg, String cls, Set<String> stockHomes, boolean oursChosen, long now) {
        return decide(on, pkg, cls, stockHomes, oursChosen, now, true);
    }

    /** tester-exp (step 88): {@code fight} false = the stock home came over another app (the person pressed Home there): brought, never counted;
     *  only the stock home replacing OUR home (or nothing known) counts toward the loop guard */
    boolean decide(boolean on, String pkg, String cls, Set<String> stockHomes, boolean oursChosen, long now, boolean fight) {
        if (!on || !oursChosen || pkg == null || cls == null || stockHomes == null) return false;
        if (!homeScreen(pkg, cls, stockHomes)) return false;
        if (!fight) { lastBring = now; return true; }   // tester-exp (R2e): a Home press over another app is always honoured, even in a pause
        if (now < pausedUntil) return false;
        // tester-exp (step 88 R2f): one Home press makes the stock home show 2-3 windows within ~0.5 s: one burst = one count. A person mashing
        // Home (about one a second) makes ~4 bursts in 4 s; a firmware that keeps putting its home back makes more: 6 bursts in 4 s = a fight
        if (lastBurst > 0 && now - lastBurst < BURST_MS) { lastBring = now; return true; }
        lastBurst = now;
        int recent = 0, oldest = 0;
        for (int i = 0; i < brings.length; i++) {
            if (brings[i] > 0 && now - brings[i] < WINDOW_MS) recent++;
            if (brings[i] < brings[oldest]) oldest = i;
        }
        if (recent >= MAX_BRINGS) {   // ours keeps losing to the stock home (a firmware that pins it): step aside, the TV's home stays
            pausedUntil = now + PAUSE_MS;
            java.util.Arrays.fill(brings, 0);
            return false;
        }
        brings[oldest] = now;
        lastBring = now;
        return true;
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
