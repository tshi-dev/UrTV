package sa.urtv.home;

import java.util.TimeZone;

/** For tests/ZoneFixRun.java: ZoneFix's package-private hooks (a fake TV tz database) and ReminderPlan.step. */
public final class ZoneFixPeek {
    public interface P { long offset(String id, long t); }
    public static void platform(final P p) { ZoneFix.platform = (id, t) -> p.offset(id, t); }
    public static void forget() { ZoneFix.forget(); }
    public static long step(long due, int days, TimeZone tz) { return ReminderPlan.step(due, days, tz); }
}
