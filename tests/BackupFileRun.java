package sa.urtv.home;

import java.util.*;

/** For tests/backup_file_test.py: BackupFile (the home's part of «نسخة الإعدادات», schema 2). Prints PASS/FAIL lines. */
public final class BackupFileRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        Map<String, Object> home = new LinkedHashMap<>();
        home.put("dock", Arrays.asList("com.netflix.ninja", "net.mbc.shahid"));
        home.put("dock_exact", true);
        home.put("hidden", Collections.singletonList("com.example.ads"));
        Map<String, Object> names = new LinkedHashMap<>(); names.put("net.mbc.shahid", "شاهد");
        home.put("app_names", names);
        Map<String, Object> inames = new LinkedHashMap<>(); inames.put("com.tcl.tvinput/.TvPassThroughService/HW15", "PS5");
        home.put("input_names", inames);
        home.put("input_hidden", Collections.singletonList("com.tcl.tvinput/.TvPassThroughService/HW16"));
        Map<String, Object> keys = new LinkedHashMap<>(); keys.put("youtube", "com.google.android.youtube.tv"); keys.put("youtube.2", "@switcher");
        home.put("keys", keys);
        List<String> notes = new ArrayList<>();
        check("a good home part: no problems", BackupFile.check(home, notes).isEmpty() && notes.isEmpty());
        Map<String, Object> extra = new LinkedHashMap<>(home); extra.put("pin", "1234"); extra.put("wifi", "x");
        notes.clear();
        check("a key not in the allow-list (the PIN, Wi-Fi) is never applied, only noted", BackupFile.check(extra, notes).isEmpty() && notes.size() == 2
            && !BackupFile.allowed(extra).containsKey("pin") && !BackupFile.allowed(extra).containsKey("wifi"));
        String[][] bad = {{"dock", "list"}, {"dock_exact", "yes"}, {"app_names", "latin"}, {"input_hidden", "space"}, {"keys", "shell"}};
        for (String[] b : bad) {
            Map<String, Object> h = new LinkedHashMap<>(home);
            switch (b[0]) {
                case "dock": h.put("dock", Arrays.asList("ok.pkg", "rm -rf /")); break;
                case "dock_exact": h.put("dock_exact", "yes"); break;
                case "app_names": Map<String, Object> m = new LinkedHashMap<>(); m.put("net.mbc.shahid", "Shahid"); h.put("app_names", m); break;
                case "input_hidden": h.put("input_hidden", Collections.singletonList("a b")); break;
                default: Map<String, Object> k = new LinkedHashMap<>(); k.put("youtube", "@shell"); h.put("keys", k);
            }
            check("refused: " + b[0] + " (" + b[1] + ")", BackupFile.check(h, null).size() == 1);
        }
        Map<String, Object> now = new LinkedHashMap<>();
        now.put("dock", Arrays.asList("a.b", "c.d", "e.f", "g.h", "i.j"));
        now.put("dock_exact", false);
        List<String> d = BackupFile.diff(now, home);
        check("the preview: lists by their count, a changed switch, a missing one shown as —", d.contains("dock: 5 → 2") && d.contains("dock_exact: false → true") && d.contains("keys: — → 2"));
        check("an unchanged value is not in the preview", BackupFile.diff(home, home).isEmpty());
        check("the app-name rule: Arabic, one line, up to 30, no | or ;", BackupFile.appName("شاهد") && !BackupFile.appName("Shahid") && !BackupFile.appName("شاهد|x")
            && !BackupFile.appName(new String(new char[31]).replace('\0', 'ش')));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
