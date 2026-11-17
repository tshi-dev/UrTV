package sa.urtv.home;

import java.util.*;

/** For tests/switcher_rule_test.py: SwitcherRule (4.5 «التطبيقات المفتوحة»). Prints PASS/FAIL lines. */
public final class SwitcherRuleRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        List<String> homes = Arrays.asList("com.google.android.apps.tv.launcherx"), keep = Arrays.asList("com.tcl.tvinput"), imes = Arrays.asList("com.google.android.leanback.ime");
        List<String> recent = Arrays.asList("com.google.android.youtube.tv", "sa.urtv.home", "com.netflix.ninja", "com.android.tv.settings", "com.google.android.apps.tv.launcherx",
            "com.tcl.tvinput", "com.google.android.leanback.ime", "com.google.android.gms", "com.netflix.ninja", "net.mbc.shahid", "com.phlox.tvwebbrowser");
        List<String> cards = SwitcherRule.cards(recent, homes, keep, imes);
        check("the cards: newest first, never ours, the system, the TV's home, an input, a keyboard, Play services; each once",
            cards.equals(Arrays.asList("com.google.android.youtube.tv", "com.netflix.ninja", "net.mbc.shahid", "com.phlox.tvwebbrowser")));
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 20; i++) many.add("com.example.app" + i);
        check("at most eight cards", SwitcherRule.cards(many, homes, keep, imes).size() == SwitcherRule.MAX);
        check("the stop commands are force-stop of stoppable apps only, nothing else reaches the shell",
            SwitcherRule.stopCommands(Arrays.asList("com.netflix.ninja", "sa.urtv.screensaver", "x; reboot", "com.tcl.tvinput"), homes, keep, imes).equals(Collections.singletonList("am force-stop com.netflix.ninja")));
        String[] m = SwitcherRule.memory(900L << 20, 2L << 30);
        check("memory: used of total in GB, one decimal", m != null && m[0].equals("1.1") && m[1].equals("2.0") && SwitcherRule.percent(900L << 20, 2L << 30) == 56);
        check("memory the TV did not say: no line", SwitcherRule.memory(5, 0) == null && SwitcherRule.memory(-1, 10) == null && SwitcherRule.memory(11, 10) == null && SwitcherRule.percent(1, 0) == 0);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
