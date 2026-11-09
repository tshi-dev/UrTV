package sa.urtv.screensaver;

import java.util.Calendar;
import java.util.TimeZone;

/** Tadawul's hours as a plain rule for «تلفزيونك»'s line under «الأسواق»: Sunday to Thursday, 10:00 to 15:00, Asia/Riyadh. The holidays are not known here. No Android in it. */
final class MarketHours {
    private MarketHours() { }

    static boolean open(long nowMillis) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        c.setTimeInMillis(nowMillis);
        int d = c.get(Calendar.DAY_OF_WEEK), m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        return d != Calendar.FRIDAY && d != Calendar.SATURDAY && m >= 10 * 60 && m < 15 * 60;
    }
}
