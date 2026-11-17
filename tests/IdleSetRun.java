package sa.urtv.home.setup;

import java.util.Arrays;
import java.util.List;

/** For tests/idle_rule_test.py: IdleSetRule (the commands the home runs over its adb for «بدء شاشة التوقف بعد»). Prints PASS/FAIL lines. */
public final class IdleSetRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("the screensaver's minutes, as ms", IdleSetRule.ms("1") == 60_000 && IdleSetRule.ms("30") == 1_800_000 && IdleSetRule.ms(" 5 ") == 300_000);
        check("anything else is refused before a command is made", IdleSetRule.ms("2147483") == -1 && IdleSetRule.ms("0") == -1 && IdleSetRule.ms("5; reboot") == -1
            && IdleSetRule.ms(null) == -1 && IdleSetRule.commands(-1, "").isEmpty());
        List<String> plain = IdleSetRule.commands(300_000, "null\n1\n");
        check("a TV with no timer of its own: the system delay only", plain.equals(Arrays.asList("settings put system screen_off_timeout 300000")));
        List<String> off = IdleSetRule.commands(600_000, "2147483647\r\n0\r\n");
        check("the user's TV (its own timer at 2147483647, the screensaver off): both delays, and the screensaver on", off.equals(Arrays.asList(
            "settings put system screen_off_timeout 600000", "settings put secure screensaver_timeout 600000", "settings put secure screensaver_enabled 1")));
        check("the TV's sleep (sleep_timeout) is never touched", !String.join(" ", off).contains("sleep_timeout"));
        check("a probe that said nothing: the system delay only", IdleSetRule.commands(60_000, null).size() == 1 && IdleSetRule.commands(60_000, "").size() == 1);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
