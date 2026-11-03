package sa.urtv.home;

import java.nio.charset.StandardCharsets;

/** For tests/portfolio_window_test.py: «محفظتي»'s phone window (PortfolioWindow). Prints "name=value" lines. */
public final class PortfolioWindowRun {
    static StringBuilder out = new StringBuilder();
    static void say(String k, Object v) { out.append(k).append('=').append(v).append('\n'); }

    public static void main(String[] a) {
        long ms = 10 * 60_000L, t = 5_000_000L;
        PortfolioWindow w = new PortfolioWindow();
        say("closed_at_start", w.open(t));
        w.open(t, ms);
        say("open_now", w.open(t));
        // a phone's no-op save just before the end renews nothing (PortfolioPhone.save calls no open): the end is where the TV put it
        say("before_end", w.open(t + ms - 1));
        say("read_after_end", w.open(t + ms));
        say("read_long_after", w.open(t + 3 * ms));
        // only the TV renews it
        w.open(t + ms - 1, ms);
        say("tv_renews", w.open(t + 2 * ms - 2));
        // a time before the opening (a wall clock set back, if one were ever passed) reads as closed, never as an extension
        w.open(t, ms);
        say("clock_back", w.open(t - 60_000L));
        say("clock_forward", w.open(t + 24 * 3600_000L));
        // lock, wipe or a phone session ended: closed until the TV opens it again
        w.close();
        say("closed", w.open(t + 1));
        // the pairing screen asked for «محفظتي» (its extra): what it may do
        say("entry_open_window", PortfolioWindow.entry(true, true, true));     // the owner opened it on the TV: show it
        say("entry_locked", PortfolioWindow.entry(false, true, true));         // an outside caller, «اقفل محفظتي»: the PIN first
        say("entry_locked_no_pin", PortfolioWindow.entry(false, true, false)); // no PIN: the lock asks nothing, as the tab
        say("entry_unlocked", PortfolioWindow.entry(false, false, true));
        System.out.write(out.toString().getBytes(StandardCharsets.UTF_8), 0, out.toString().getBytes(StandardCharsets.UTF_8).length);
        System.out.flush();
    }
}
