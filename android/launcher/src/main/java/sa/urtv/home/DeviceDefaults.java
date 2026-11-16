package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/** 4.3.3 DeviceRule's defaults written on this TV: at the home's start, from the kept copy, only where nothing is set yet; never throws. */
final class DeviceDefaults {
    private DeviceDefaults() { }

    static void apply(Context c) {
        try {
            ContentFile.refresh(c, DeviceRule.PATH, DeviceRule.NAME, 24 * 3600_000L);
            Map<String, Object> d = DeviceRule.defaults(ContentFile.read(c, DeviceRule.NAME), android.os.Build.BRAND, android.os.Build.MODEL,
                android.os.Build.DEVICE, android.os.Build.VERSION.SDK_INT);
            if (Boolean.TRUE.equals(d.get("sound_opens_general_settings"))) {
                SharedPreferences sp = c.getSharedPreferences("control", Context.MODE_PRIVATE);   // ControlCenter's own file
                if (!sp.contains("sound_bad")) sp.edit().putBoolean("sound_bad", true).apply();
            }
        } catch (RuntimeException ignored) {
            // a default is a help, never a reason for the home not to start
        }
    }
}
