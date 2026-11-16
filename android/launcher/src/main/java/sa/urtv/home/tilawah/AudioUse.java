package sa.urtv.home.tilawah;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 4.4: what a file of «صوتياتي» is used for: «أذاني الخاص», «أذان الفجر الخاص», or nothing yet (a dua uses it by its own record, DuaStore).
 * The adhan uses a copy (OwnAdhan's two files, as `music.sh adhan add` makes them), so deleting the file from the list never silences an adhan.
 */
final class AudioUse {
    private AudioUse() { }

    private static SharedPreferences sp(Context c) { return c.getApplicationContext().getSharedPreferences("audio_lib", Context.MODE_PRIVATE); }

    /** "own", "own_fajr" or "" */
    static String of(Context c, int id) {
        if (sp(c).getInt("own_from", -1) == id && OwnAdhan.exists(c)) return "own";
        if (sp(c).getInt("own_fajr_from", -1) == id && OwnAdhan.fajrExists(c)) return "own_fajr";
        return "";
    }

    /** null when done, else a code: "too_long" (an adhan is at most 5 minutes), "copy" */
    static String set(Context c, AudioStore.Item it, String as) {
        Prefs prefs = new Prefs(c);
        if ("own".equals(as) || "own_fajr".equals(as)) {
            boolean fajr = "own_fajr".equals(as);
            if (it.ms > OwnAdhanText.MAX_MS) return "too_long";
            String why = OwnAdhan.keepFromLibrary(c, AudioStore.file(c, it), fajr);
            if (why != null) return "copy";
            if (fajr) { prefs.setFajrOwn(true); prefs.setFajrOwnSecond(true); }
            else { prefs.setOwnChosen(true); if (!prefs.fajrOwnSecond()) prefs.setFajrOwn(true); }   // as music.sh adhan add does
            sp(c).edit().putInt(fajr ? "own_fajr_from" : "own_from", it.id).apply();
            AdhanScheduler.schedule(c);
            return null;
        }
        if ("none".equals(as)) {   // the adhan goes back to the muezzin it had; the recording itself stays in the list
            String now = of(c, it.id);
            if ("own".equals(now)) { OwnAdhan.clear(c); prefs.setOwnChosen(false); prefs.setFajrOwn(false); prefs.setFajrOwnSecond(false); }
            if ("own_fajr".equals(now)) { OwnAdhan.clearFajr(c); prefs.setFajrOwnSecond(false); }
            forget(c, it.id);
            return null;
        }
        return "bad_use";
    }

    /** the list no longer points at this file (the adhan keeps its own copy) */
    static void forget(Context c, int id) {
        SharedPreferences.Editor e = sp(c).edit();
        if (sp(c).getInt("own_from", -1) == id) e.remove("own_from");
        if (sp(c).getInt("own_fajr_from", -1) == id) e.remove("own_fajr_from");
        e.apply();
    }
}
