package sa.urtv.home;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** For tests/adhan_bar_test.py: AdhanBarText.java on the cases in tests/adhan_bar_cases.tsv. One answer per case line
 *  ("-" when the bar is not showing). Fields: now, then the five adhan times (Fajr Dhuhr Asr Maghrib Isha), friday, latin
 *  (all times as minutes since midnight, a fraction allowed), then what is expected and a note, then the lead in minutes (4.4 beta 4: 10 when empty, or 15). */
public final class AdhanBarRun {
    public static void main(String[] args) throws IOException {
        StringBuilder out = new StringBuilder();
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t", -1);
            long now = Math.round(Double.parseDouble(f[0]) * 60000);
            long[] at = new long[5];
            for (int i = 0; i < 5; i++) at[i] = Math.round(Double.parseDouble(f[i + 1]) * 60000);
            String r = AdhanBarText.text(now, at, "1".equals(f[6]), "latin".equals(f[7]), f.length > 10 && !f[10].isEmpty() ? Integer.parseInt(f[10]) : 10);
            out.append(r == null ? "-" : r).append('\n');
        }
        System.out.write(out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.flush();
    }
}
