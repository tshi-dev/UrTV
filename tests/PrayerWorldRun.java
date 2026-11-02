package sa.urtv.home.tilawah;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

/** Local calculations against published Aladhan fixtures, including night rollover. */
public final class PrayerWorldRun {
    public static void main(String[] args) throws Exception {
        int bad = 0, count = 0, worst = 0;
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            String[] f = line.split("\t"), date = f[4].split("-");
            int id = Integer.parseInt(f[5]);
            int[] raw = new int[6];
            for (int i = 0; i < 6; i++) raw[i] = Integer.parseInt(f[6 + i].substring(0, 2)) * 60 + Integer.parseInt(f[6 + i].substring(3));
            int[] ordered = PrayerMath.orderedClockMinutes(raw, true);
            for (int i = 1; i < 6; i++) if (ordered[i] <= ordered[i - 1]) throw new AssertionError("published night rollover " + f[0]);
            if (PrayerMath.methodFor(f[3], Integer.toString(id)) != id) throw new AssertionError("numeric method " + id);
            long[] at = PrayerMath.compute(Integer.parseInt(date[2]), Integer.parseInt(date[1]), Integer.parseInt(date[0]),
                    Double.parseDouble(f[1]), Double.parseDouble(f[2]), f[3], false, id);
            for (int i = 0; i < 6; i++) {
                String clock = PrayerMath.clockOf(at[i], f[3]);
                int got = Integer.parseInt(clock.substring(0, 2)) * 60 + Integer.parseInt(clock.substring(3));
                int want = Integer.parseInt(f[6 + i].substring(0, 2)) * 60 + Integer.parseInt(f[6 + i].substring(3));
                int delta = Math.abs(got - want); delta = Math.min(delta, 1440 - delta);
                worst = Math.max(worst, delta);
                if (delta > 2) { bad++; System.out.println("FAIL " + f[0] + " " + f[4] + " m" + id + " prayer " + i + ": " + clock + " vs " + f[6 + i]); }
                if (i > 0 && at[i] <= at[i - 1]) { bad++; System.out.println("FAIL prayer order " + f[0] + " " + f[4]); }
            }
            count++;
            if (f[0].equals("Oslo") && f[4].equals("21-06-2026")) {
                long afterMidnight = PrayerMath.wall(2026, 6, 22, 1, f[3]);
                if (PrayerMath.firstAfter(at, new int[] {0, 2, 3, 4, 5}, afterMidnight) != 5)
                    throw new AssertionError("last night's Isha remains due after midnight");
            }
        }
        // At the poles an unreachable angle must never turn into midnight/NaN.
        for (int month : new int[] {6, 12}) {
            long[] at = PrayerMath.compute(2026, month, 21, Double.parseDouble(args[1]), Double.parseDouble(args[2]), args[3], false, 3);
            for (int i = 1; i < 6; i++) if (at[i] <= at[i - 1]) throw new AssertionError("polar prayer order");
        }
        System.out.println((bad == 0 ? "PASS " : "FAIL ") + count + " published Aladhan city-days, worst " + worst + " minutes; polar fallback ordered");
        System.exit(bad == 0 ? 0 : 1);
    }
}
