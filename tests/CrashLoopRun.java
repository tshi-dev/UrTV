package sa.urtv.home;

/** tests/crash_loop_test.py: steps "s t" (the process started), "c t" (it crashed), "o t" (the home screen opened), "d t" (it drew its first frame), "q t" (safe now? prints safe or home), "x" (cleared), "p TEXT" (parse); "w" prints the file */
public final class CrashLoopRun {
    public static void main(String[] args) {
        CrashLoop s = CrashLoop.parse(null);
        for (String a : args) {
            String[] p = a.split(" ", 2);
            switch (p[0]) {
                case "s": s = s.started(Long.parseLong(p[1])); break;
                case "c": s = s.crashed(Long.parseLong(p[1])); break;
                case "o": s = s.opened(Long.parseLong(p[1])); break;
                case "d": s = s.drewFrame(Long.parseLong(p[1])); break;
                case "q": System.out.println(s.safe(Long.parseLong(p[1])) ? "safe" : "home"); break;
                case "x": s = s.cleared(); break;
                case "p": s = CrashLoop.parse(p[1].replace("|", "\n")); break;
                default: System.out.println(s.text().replace("\n", "|"));
            }
        }
    }
}
