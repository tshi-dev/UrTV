package sa.urtv.screensaver;

/** For tests/page_push_test.py: arguments in pairs (shown, now); for each pair PagePush.script's answer, «-» for null, one per line. */
public final class PagePushRun {
    public static void main(String[] args) {
        for (int i = 0; i + 1 < args.length; i += 2) {
            String s = PagePush.script(args[i], args[i + 1]);
            System.out.println(s == null ? "-" : s);
        }
    }
}
