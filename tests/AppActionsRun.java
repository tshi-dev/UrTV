package sa.urtv.home;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** For tests/app_actions_test.py: AppActions on tests/app_actions_cases.tsv, one answer line per case line.
 *  "buttons": package, system (0/1), kids (0/1) -> the buttons right to left (DETAILS, DELETE; "-" for none). */
public final class AppActionsRun {
    public static void main(String[] args) throws IOException {
        StringBuilder out = new StringBuilder();
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t", -1);
            List<String> names = new ArrayList<>();
            for (int k : AppActions.buttons(f[1].isEmpty() ? null : f[1], f[2].equals("1"), f[3].equals("1")))
                names.add(k == AppActions.OPEN_DETAILS ? "DETAILS" : k == AppActions.OPEN_DELETE ? "DELETE" : "?");
            out.append(names.isEmpty() ? "-" : String.join(",", names)).append('\n');
        }
        System.out.write(out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.flush();
    }
}
