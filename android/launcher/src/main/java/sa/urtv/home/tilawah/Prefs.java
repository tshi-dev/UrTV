package sa.urtv.home.tilawah;

import android.content.Context;
import android.content.SharedPreferences;

/** The last thing played, so «متابعة» works after a reboot too, and the adhan settings. */
final class Prefs {
    static final int SOURCE_NONE = 0, SOURCE_SURAH = 1, SOURCE_RADIO = 2, SOURCE_ADHAN = 3;

    private final SharedPreferences sp;

    Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("tilawah", Context.MODE_PRIVATE);
    }

    int source() { return sp.getInt("source", SOURCE_NONE); }
    String reciter() { return sp.getString("reciter", ""); }
    int surah() { return sp.getInt("surah", 1); }
    int station() { return sp.getInt("station", 0); }
    int position() { return sp.getInt("position", 0); }

    void save(int source, String reciter, int surah, int station, int position) {
        sp.edit().putInt("source", source).putString("reciter", reciter).putInt("surah", surah)
                .putInt("station", station).putInt("position", position).apply();
    }

    // ---- the adhan ----

    /** Off until the user turns it on; there is a switch row for it in «تلاوة». */
    boolean adhanOn() { return sp.getBoolean("adhan_on", false); }
    void setAdhanOn(boolean on) { sp.edit().putBoolean("adhan_on", on).apply(); }

    /** The adhan's own volume, in percent; the TV's volume comes back after it. */
    int adhanVolume() { int v = sp.getInt("adhan_volume", 30); return v < 10 || v > 100 ? 30 : v; }
    void setAdhanVolume(int percent) { sp.edit().putInt("adhan_volume", percent).apply(); }
    /** The TV's own volume while an adhan plays, kept on disk so it comes back even if the player was stopped mid-adhan (-1: none). */
    int savedTvVolume() { return sp.getInt("tv_volume_saved", -1); }
    void setSavedTvVolume(int v) { sp.edit().putInt("tv_volume_saved", v).commit(); }
    /** the TV was muted when the adhan raised its volume: muted again at the end */
    boolean savedTvMuted() { return sp.getBoolean("tv_volume_saved_muted", false); }
    void setSavedTvMuted(boolean v) { sp.edit().putBoolean("tv_volume_saved_muted", v).commit(); }
    /** The level the adhan set the volume to (-1: not known), so a volume the person changed during the adhan is left alone afterwards. */
    int adhanLevel() { return sp.getInt("tv_volume_adhan_level", -1); }
    void setAdhanLevel(int v) { sp.edit().putInt("tv_volume_adhan_level", v).commit(); }

    /** The voice number that stands for the person's own recording (OwnAdhan); the numbered ones are 1..21. */
    static final int OWN = 0;
    boolean ownChosen() { return sp.getBoolean("adhan_own", false); }
    void setOwnChosen(boolean on) { freezeFajr(); sp.edit().putBoolean("adhan_own", on).apply(); }
    /** What plays at the prayers other than Fajr: OWN when the recording is chosen and still on the TV, else the numbered voice. */
    int playVoice(Context c) { return ownChosen() && OwnAdhan.exists(c) ? OWN : voice(); }

    /** The numbered voice for Dhuhr, Asr, Maghrib and Isha (never a Fajr recording: a TV that had chosen one keeps it for Fajr). */
    int voice() { return AdhanVoices.normalVoice(sp.getInt("adhan_voice", AdhanVoices.DEFAULT)); }
    void setVoice(int voice) { freezeFajr(); sp.edit().putInt("adhan_voice", voice).apply(); }

    /** «مؤذن الفجر»: the numbered voice for Fajr. A TV whose one voice was a Fajr recording keeps it here; the others start with فجر الحرم المكي. */
    int fajrVoice() {
        return AdhanVoices.fajrVoice(sp.contains("adhan_fajr_voice") ? sp.getInt("adhan_fajr_voice", -1) : -1,
                sp.getInt("adhan_voice", AdhanVoices.DEFAULT));
    }
    void setFajrVoice(int voice) { freezeFajr(); sp.edit().putInt("adhan_fajr_voice", voice).putBoolean("adhan_fajr_own", false).apply(); }
    /** The person's own recording at Fajr: until it is chosen apart it follows the one for the other prayers (as it did before there was a Fajr voice). */
    boolean fajrOwn() { return sp.contains("adhan_fajr_own") ? sp.getBoolean("adhan_fajr_own", false) : ownChosen(); }
    void setFajrOwn(boolean on) { freezeFajr(); sp.edit().putBoolean("adhan_fajr_own", on).apply(); }
    /** What plays at Fajr: OWN when the recording is chosen for it and still on the TV, else the Fajr voice. */
    int playFajrVoice(Context c) { return fajrOwn() && OwnAdhan.exists(c) ? OWN : fajrVoice(); }
    /** What plays at a prayer: Fajr has its own voice. */
    int playVoiceFor(Context c, boolean fajr) { return fajr ? playFajrVoice(c) : playVoice(c); }

    /**
     * Writes down what Fajr is using now before one of the two choices changes, so a change to the other prayers' voice never moves the one
     * for Fajr (nothing is written when the TV starts; the old single voice is read as it is until then).
     */
    private void freezeFajr() {
        SharedPreferences.Editor e = sp.edit();
        if (!sp.contains("adhan_fajr_voice")) e.putInt("adhan_fajr_voice", fajrVoice());
        if (!sp.contains("adhan_fajr_own")) e.putBoolean("adhan_fajr_own", ownChosen());
        e.apply();
    }

    /** «أذان مختصر»: the adhan stops after its first minute with a short fade (not the few seconds of a preview). Off until it is turned on. */
    boolean adhanShort() { return sp.getBoolean("adhan_short", false); }
    void setAdhanShort(boolean on) { sp.edit().putBoolean("adhan_short", on).apply(); }

    /** «إيقاف الأذان بأي زر»: while the adhan plays, any key stops it (once) and is consumed; ON by default (the owner's decision), off only when someone turns it off. The screensaver asks for it. */
    boolean adhanStopKey() { return sp.getBoolean("adhan_stopkey", true); }
    void setAdhanStopKey(boolean on) { sp.edit().putBoolean("adhan_stopkey", on).apply(); }

    /** The last adhan that played (a preview too), for `music.sh adhan`. */
    long lastAdhan() { return sp.getLong("adhan_at", 0); }
    void setLastAdhan(long at) { sp.edit().putLong("adhan_at", at).apply(); }

    /**
     * The prayer time of the last adhan an alarm played, so the same adhan never plays twice (a clock change can set
     * the alarm again). It is the alarm's own time, not the moment something played: a preview must not count.
     */
    long lastDue() { return sp.getLong("adhan_due", 0); }
    void setLastDue(long due) { sp.edit().putLong("adhan_due", due).apply(); }
    /** An alarm that came without its time (one set by an older version): when it played, for a five-minute guard. */
    long lastAlarmAt() { return sp.getLong("adhan_alarm_at", 0); }
    void setLastAlarmAt(long at) { sp.edit().putLong("adhan_alarm_at", at).apply(); }

    /** When the chosen recording was last tried and when all of them were last asked for (a day, yyyyMMdd), so neither repeats at once. */
    long prefetchTried() { return sp.getLong("adhan_prefetch_try", 0); }
    void setPrefetchTried(long at) { sp.edit().putLong("adhan_prefetch_try", at).apply(); }
    String prefetchDay() { return sp.getString("adhan_prefetch_day", ""); }
    void setPrefetchDay(String day) { sp.edit().putString("adhan_prefetch_day", day).apply(); }
}
