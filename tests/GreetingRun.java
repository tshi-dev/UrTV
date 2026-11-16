package sa.urtv.home;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** For tests/greeting_test.py: GreetingText.java on the cases in tests/greeting_cases.tsv.
 *  Arg: the cases file. One answer per case line. */
public final class GreetingRun {
    public static void main(String[] args) throws IOException {
        StringBuilder out = new StringBuilder();
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t", -1);
            String r;
            switch (f[0]) {
                case "part": {   // part  now  fajr  dhuhr  asr  isha  (minutes of the day)
                    long[] m = new long[5];
                    for (int i = 0; i < 5; i++) m[i] = Long.parseLong(f[i + 1]) * 60000L;
                    r = GreetingText.greeting(GreetingText.part(m[0], m[1], m[2], m[3], m[4]));
                    break;
                }
                case "weather": r = GreetingText.weather(Integer.parseInt(f[1]), Double.parseDouble(f[2]), f[3], Double.parseDouble(f[4]), "latin".equals(f[5])); break;
                case "seen": r = String.valueOf((int) GreetingText.seenDust(Double.parseDouble(f[1]), Double.parseDouble(f[2]))); break;
                case "icon": r = String.valueOf(GreetingText.icon(Integer.parseInt(f[1]), "night".equals(f[2]))); break;   // 4.3.3: the drawing beside the line
                case "sun": r = GreetingText.sun(f[1], f[2], "latin".equals(f[3]), false); break;
                default: r = "?";
            }
            out.append(r).append('\n');
        }
        System.out.write(out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.flush();
    }
}
