package sa.urtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * For tests/city_keep_test.py (R-403): CityKeep across a restart, with a fake storage. A Disk keeps only what commit() wrote; apply() stays in memory,
 * and a power cut (a new Context on the same Disk) loses it. The Android classes are the test's own stubs (only what CityKeep uses).
 */
public final class CityKeepRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    static final class Disk { final Map<String, Map<String, String>> files = new HashMap<>(); }

    static final class Prefs implements SharedPreferences {
        final Disk disk; final String name; final Map<String, String> mem;
        Prefs(Disk d, String n) { disk = d; name = n; Map<String, String> f = d.files.get(n); mem = f == null ? new HashMap<>() : new HashMap<>(f); }
        @Override public boolean contains(String k) { return mem.containsKey(k); }
        @Override public String getString(String k, String def) { return mem.containsKey(k) ? mem.get(k) : def; }
        @Override public SharedPreferences.Editor edit() {
            final Map<String, String> pending = new HashMap<>();
            return new SharedPreferences.Editor() {
                @Override public SharedPreferences.Editor putString(String k, String v) { pending.put(k, v); return this; }
                @Override public boolean commit() { mem.putAll(pending); disk.files.put(name, new HashMap<>(mem)); return true; }
                @Override public void apply() { mem.putAll(pending); }   // never reaches the disk before the power cut
            };
        }
    }

    static final class Ctx extends Context {
        final Disk disk; final Map<String, Prefs> open = new HashMap<>();
        Ctx(Disk d) { disk = d; }
        @Override public SharedPreferences getSharedPreferences(String n, int mode) { return open.computeIfAbsent(n, k -> new Prefs(disk, k)); }
    }

    public static void main(String[] args) {
        Disk disk = new Disk();
        Ctx before = new Ctx(disk);
        // the person chose Dammam: the settings file written the way Prefs.setCity writes it now (commit), and CityKeep.save
        before.getSharedPreferences("settings", 0).edit().putString("cityName", "الدمام").putString("cityLat", "26.43").putString("cityLon", "50.10")
            .putString("cityTz", "Asia/Riyadh").putString("cityCountry", "SA").commit();
        check("the kept copy is written", CityKeep.save(before, "الدمام", "26.43", "50.10", "Asia/Riyadh", "SA"));
        Ctx after = new Ctx(disk);   // a power cycle
        check("after the restart the city is there (it was committed)", "الدمام".equals(after.getSharedPreferences("settings", 0).getString("cityName", "الرياض")));
        check("a city that is there is never overwritten by the kept copy", !CityKeep.restore(after, after.getSharedPreferences("settings", 0)));

        // the TV lost the settings file (or wrote back one without the city): the kept copy brings the whole city back
        disk.files.remove("settings");
        Ctx lost = new Ctx(disk);
        SharedPreferences s = lost.getSharedPreferences("settings", 0);
        check("a settings file without its city gets the kept one", CityKeep.restore(lost, s) && "الدمام".equals(s.getString("cityName", "الرياض"))
              && "26.43".equals(s.getString("cityLat", "")) && "50.10".equals(s.getString("cityLon", "")) && "Asia/Riyadh".equals(s.getString("cityTz", ""))
              && "SA".equals(s.getString("cityCountry", "")));
        Ctx again = new Ctx(disk);   // and it was written to the disk at once
        check("...written at once: it outlives the next power cut too", "الدمام".equals(again.getSharedPreferences("settings", 0).getString("cityName", "الرياض")));

        // the old way (apply) is what lost it: shown here so the fake disk is honest
        Disk old = new Disk();
        Ctx o = new Ctx(old);
        o.getSharedPreferences("settings", 0).edit().putString("cityName", "جدة").apply();
        check("(the fake disk) an apply() is lost by a power cut", !new Ctx(old).getSharedPreferences("settings", 0).contains("cityName"));

        Disk none = new Disk();
        Ctx n = new Ctx(none);
        check("nothing kept, nothing restored (a new TV keeps Riyadh as the default)", !CityKeep.restore(n, n.getSharedPreferences("settings", 0)));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
