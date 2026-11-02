package sa.urtv.home;

import java.util.Calendar;
import java.util.TimeZone;

/** The words of a reminder's card on the home and of its notice over another app. The screensaver's page says the same (remSub in template.html). tests/ReminderPlanRun.java runs it. */
final class ReminderText {
    private ReminderText() { }

    /** «الساعة ٥:٠٠ م، بعد ٣٠ دقيقة», and «الساعة ٥:٠٠ م، الآن» from the appointment on; the time of day in the zone */
    static String when(ReminderPlan.Row r, long now, TimeZone tz, boolean clock24, boolean latin) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(r.due);
        String at = S.t("الساعة %s", ClockText.time(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), clock24, latin));
        if (now >= r.due) return at + S.t("، الآن");
        return at + S.t("، بعد %s", AdhanBarText.minutes(ReminderPlan.minutesLeft(r, now), latin));
    }

    /** one line for the notice over another app: the task, then when */
    static String line(ReminderPlan.Row r, long now, TimeZone tz, boolean clock24, boolean latin) {
        return r.text + "  ·  " + when(r, now, tz, clock24, latin);
    }

    static String snooze(boolean latin) {
        return S.t("ذكّرني بعد %s", AdhanBarText.minutes(ReminderCard.SNOOZE_MIN, latin));
    }
}
