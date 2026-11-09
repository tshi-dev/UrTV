package sa.urtv.home;

import java.util.LinkedHashSet;
import java.util.Set;

/** For tests/clean_mode_test.py: CleanKeep on a plain JVM. One line per case. */
public final class CleanKeepRun {
    public static void main(String[] a) {
        System.out.println(CleanKeep.join(CleanKeep.parse(null)));                 // never set: the default
        System.out.println(CleanKeep.join(CleanKeep.parse("")));                   // set to nothing: nothing stays but the clock, the date and the bar
        System.out.println(CleanKeep.join(CleanKeep.parse("settings,card,junk,card")));   // unknown names go, order is the sheet's, no repeats
        Set<String> s = new LinkedHashSet<>();
        s.add("greeting"); s.add("wifi");
        System.out.println(CleanKeep.join(s));
    }
}
