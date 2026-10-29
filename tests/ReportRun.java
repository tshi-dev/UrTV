package sa.urtv.home.setup;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** SetupReport's pieces for tests/setup_report_test.py: `quote <file>`, `url <file>`, `restrict <file>`, `steps <file>`, `memory <summary> <kinds>`. */
public class ReportRun {
    public static void main(String[] a) throws Exception {
        String t = new String(Files.readAllBytes(Paths.get(a[1])), StandardCharsets.UTF_8);
        switch (a[0]) {
            case "quote": System.out.print(SetupReport.quote(t)); break;
            case "url": System.out.print(String.valueOf(SetupReport.shareUrl(t))); break;
            case "restrict": System.out.print(SetupReport.restrict(t)); break;
            case "steps": System.out.print(String.join("\n", SetupReport.steps(t))); break;
            case "memory": System.out.print(String.join("\n", SetupReport.memory(t, new String(Files.readAllBytes(Paths.get(a[2])), StandardCharsets.UTF_8)))); break;
            default: throw new IllegalArgumentException(a[0]);
        }
    }
}
