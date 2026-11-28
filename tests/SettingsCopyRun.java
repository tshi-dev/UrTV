package sa.urtv.screensaver;

import java.util.*;

/** For tests/settings_copy_test.py: SettingsCopyRule.java on a plain JVM. Prints PASS/FAIL lines. */
public final class SettingsCopyRun {
    static int failed;
    static void check(String name, boolean ok, Object d) { System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + d)); if (!ok) failed++; }
    public static void main(String[] a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cityName", "الرياض\tحي\nالملز \\ ٢"); m.put("clock24", true); m.put("photoMinutes", "5"); m.put("adhanLead", 15); m.put("stamp", 1700000000000L);
        m.put("wallDim", 0.25f); m.put("teams", new LinkedHashSet<>(Arrays.asList("133602:Al Hilal", "x\ty"))); m.put("updateChannel", "next"); m.put("ramtier", "low");
        String t = SettingsCopyRule.encode(m);
        Map<String, Object> back = SettingsCopyRule.decode(t);
        Map<String, Object> want = new LinkedHashMap<>(m); want.remove("updateChannel"); want.remove("ramtier");
        check("every value comes back with its type (text with tabs, newlines and backslashes, a set, numbers, a switch)", back != null && back.equals(want), back);
        check("the update channel and the memory-tier debug switch are never in the copy", !t.contains("updateChannel") && !t.contains("ramtier"), t);
        check("…and never taken from one", SettingsCopyRule.decode("s\tupdateChannel\tnext\nb\tclock24\ttrue\n").keySet().equals(Collections.singleton("clock24")), "");
        check("a broken copy is never taken in part", SettingsCopyRule.decode("b\tclock24\ttrue\ni\tadhanLead\tmany\n") == null && SettingsCopyRule.decode("x\tk\tv\n") == null
            && SettingsCopyRule.decode("s\tk\tbad\\q\n") == null && SettingsCopyRule.decode("b\tk\tmaybe\n") == null, "");
        check("an empty or missing copy is not a copy", SettingsCopyRule.decode("") == null && SettingsCopyRule.decode(null) == null && SettingsCopyRule.decode("\n") == null, "");
        StringBuilder big = new StringBuilder(); while (big.length() <= SettingsCopyRule.MAX_CHARS) big.append("s\tk").append(big.length()).append("\tvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv\n");
        check("a copy over the size limit is refused", SettingsCopyRule.decode(big.toString()) == null, "");
        check("a fresh install takes the copy back (nothing saved, or only the key the first start writes)",
            SettingsCopyRule.shouldRestore(new HashSet<String>(), false) && SettingsCopyRule.shouldRestore(new HashSet<>(Collections.singleton("tournaments")), false), "");
        check("…but never over settings of its own, and only once", !SettingsCopyRule.shouldRestore(new HashSet<>(Arrays.asList("tournaments", "cityName")), false)
            && !SettingsCopyRule.shouldRestore(new HashSet<String>(), true), "");
        check("the home keeps a real copy that differs, never an empty or broken one over the kept copy",
            SettingsCopyRule.worthKeeping(t, "old") && !SettingsCopyRule.worthKeeping(t, t) && !SettingsCopyRule.worthKeeping("", t) && !SettingsCopyRule.worthKeeping(null, t), "");
        System.exit(failed == 0 ? 0 : 1);
    }
}
