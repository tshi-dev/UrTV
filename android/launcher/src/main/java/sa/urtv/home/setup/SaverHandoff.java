package sa.urtv.home.setup;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

/**
 * 4.4 «أجزاء UrTV» (the owner): «شاشة التوقف» off gives the TV back its own screensaver; on again, ours. The TV's own is what the setup kept
 * before it chose ours (SetupEngine: screensaver-before.txt, its first line); with nothing kept, the TV's setting is left as it is and only our
 * screensaver's rows and tile go. Needs WRITE_SECURE_SETTINGS (granted by the setup); without it, nothing changes and the log says so. Called off
 * the UI thread, only when the part changed.
 */
public final class SaverHandoff {
    private SaverHandoff() { }

    /** @return what was done, for the log: "theirs", "ours", "kept" (nothing to change, or nothing kept), "denied" */
    public static String apply(Context c, boolean off) {
        ContentResolver r = c.getContentResolver();
        android.content.SharedPreferences sp = c.getSharedPreferences("parts", Context.MODE_PRIVATE);
        try {
            String now = Settings.Secure.getString(r, "screensaver_components");
            boolean ours = now != null && now.contains(SetupEngine.SAVER + "/");
            if (off) {
                String theirs = theirs(new FileStore(c).get("screensaver-before.txt"));
                if (!ours || theirs == null) return "kept";
                Settings.Secure.putString(r, "screensaver_components", theirs);
                sp.edit().putBoolean("handedOff", true).apply();   // ours goes back only when it was ours that was handed off
                return "theirs";
            }
            // on: back to ours only after our own hand-off; a TV whose person chose the TV's screensaver keeps it (stable first)
            if (!sp.getBoolean("handedOff", false)) return "kept";
            sp.edit().remove("handedOff").apply();
            if (ours) return "kept";
            Settings.Secure.putString(r, "screensaver_components", SetupEngine.OURS_DREAM);
            return "ours";
        } catch (SecurityException e) {
            Log.i("UrTVParts", "screensaver handoff: no permission to change the TV's screensaver");
            return "denied";
        }
    }

    /** the TV's own screensaver from the setup's note (components, enabled, default: one a line); null when there is none, or it was already ours */
    static String theirs(String before) {
        if (before == null) return null;
        String first = before.split("\n", 2)[0].trim();
        if (first.isEmpty() || "null".equals(first) || first.contains(SetupEngine.SAVER + "/")) return null;
        return first;
    }
}
