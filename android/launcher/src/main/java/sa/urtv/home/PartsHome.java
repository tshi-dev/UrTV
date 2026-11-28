package sa.urtv.home;

import android.content.Context;

/**
 * 4.4 «أجزاء UrTV» on the home's side, for what runs with no screen (the adhan's, the duas' and the tasks' alarms): the last settings fragment
 * the home read from the screensaver (HomeCard's «homecard» file) says which parts are off («&off=a,b»). No provider call, no network: a local read.
 * A part switched off in the settings is seen here from the home's next read of them (its return to the front or its minute).
 */
public final class PartsHome {
    private PartsHome() { }

    public static boolean off(Context c, String id) {
        String frag = c.getSharedPreferences("homecard", Context.MODE_PRIVATE).getString("frag", "");
        return offIn(frag, id);
    }

    static boolean offIn(String frag, String id) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:^|&)off=([a-z,]*)").matcher(frag == null ? "" : frag);
        if (!m.find()) return false;
        for (String k : m.group(1).split(",")) if (k.equals(id)) return true;
        return false;
    }
}
