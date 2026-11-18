package sa.urtv.home.setup;

import java.util.ArrayList;
import java.util.List;

/**
 * «بدء شاشة التوقف بعد» from the screensaver's settings, over this app's own adb (the key is kept): the TV's idle delay is the system
 * screen_off_timeout, and TVs that have their own screensaver timer keep it in secure screensaver_timeout too. A user's TV said 2147483647 (its
 * screensaver turned off), and the screensaver app may not write system settings on most TVs, so the home writes it here. Only the minutes the
 * screensaver offers; sleep_timeout (the TV turning itself off) is never touched. No Android in here (tests/idle_set_test.py runs it).
 */
final class IdleSetRule {
    private IdleSetRule() { }

    static final String[] MINUTES = {"1", "2", "5", "10", "15", "30"};

    /** what the TV keeps now, read first: two lines (the TV's own timer, or null; the screensaver's switch) */
    static final String PROBE = "settings get secure screensaver_timeout; settings get secure screensaver_enabled";

    /** the ms for one of MINUTES, -1 for anything else */
    static long ms(String minutes) {
        if (minutes == null) return -1;
        for (String m : MINUTES) if (m.equals(minutes.trim())) return Long.parseLong(m) * 60000L;
        return -1;
    }

    /** the commands for {@code ms} after PROBE said {@code probe}: the system delay always; the TV's own timer only where it has one; the switch on when it was off */
    static List<String> commands(long ms, String probe) {
        List<String> out = new ArrayList<>();
        if (ms <= 0) return out;
        out.add("settings put system screen_off_timeout " + ms);
        String[] lines = (probe == null ? "" : probe).trim().split("\\s*\\n\\s*");
        String own = lines.length > 0 ? lines[0].trim() : "";
        String enabled = lines.length > 1 ? lines[1].trim() : "";
        if (own.matches("[0-9]+")) out.add("settings put secure screensaver_timeout " + ms);
        if ("0".equals(enabled)) out.add("settings put secure screensaver_enabled 1");
        return out;
    }
}
