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

    /** "own", "own_fajr", "dua" (a dua or reminder plays it), "iqama" (4.4: «صوت الإقامة») or "" */
    static String of(Context c, int id) {
        String adhan = adhanOf(c, id);
        if (!adhan.isEmpty()) return adhan;
        for (DuaStore.Dua d : DuaStore.list(c)) if (("a:" + id).equals(d.sound)) return "dua";
        if (("a" + id).equals(new Prefs(c).iqamaSound())) return "iqama";
        return "";
    }

    private static String adhanOf(Context c, int id) {
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
            AdhanScheduler.scheduleSoon(c);   // the place is a question to the screensaver: never on the screen's thread (review F93)
            return null;
        }
        if ("dua".equals(as)) {   // a new dua for this sound, off until its time is set on the TV («الأدعية والتذكيرات»)
            for (DuaStore.Dua d : DuaStore.list(c)) if (("a:" + it.id).equals(d.sound)) return null;   // one is there already
            DuaStore.Dua d = new DuaStore.Dua();
            d.name = it.name;
            d.sound = "a:" + it.id;
            d.on = false;
            d.fresh = true;
            d.rule.at = DuaRule.AFTER; d.rule.prayer = PrayerTimes.FAJR; d.rule.offset = 15;
            return DuaStore.put(c, d) ? null : "duas_full";   // 4.4: the duas' list is full (not the files' «too_many»)
        }
        if ("none".equals(as)) {   // the adhan goes back to the muezzin it had; the recording itself stays in the list
            String now = adhanOf(c, it.id);
            if (now.isEmpty() && !of(c, it.id).isEmpty()) return "on_tv";   // 4.4: a dua or the iqama plays it: changed from the TV, where its time is
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
