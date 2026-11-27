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
    /** the station's number in stations.json; 4.3.1 put Egypt's Quran Radio in at 1: a number saved before that moves up by one, once */
    int station() {
        int s = sp.getInt("station", 0);
        if (sp.getInt("station_order", 1) < STATION_ORDER) {
            if (sp.contains("station") && s >= 1) s++;
            sp.edit().putInt("station", s).putInt("station_order", STATION_ORDER).apply();
        }
        return s;
    }

    /** 2: the order of stations.json with Egypt's Quran Radio at 1 (4.3.1) */
    static final int STATION_ORDER = 2;
    int position() { return sp.getInt("position", 0); }

    /** 4.4 «متابعة السورة»: each surah's place (SurahPlaces) */
    String surahPlaces() { return sp.getString("surah_places", ""); }
    void setSurahPlaces(String r) { sp.edit().putString("surah_places", r).apply(); }

    void save(int source, String reciter, int surah, int station, int position) {
        sp.edit().putInt("source", source).putString("reciter", reciter).putInt("surah", surah)
                .putInt("station", station).putInt("station_order", STATION_ORDER).putInt("position", position).apply();
    }

    // ---- the adhan ----

    /** Off until the user turns it on; there is a switch row for it in «تلاوة». */
    boolean adhanOn() { return sp.getBoolean("adhan_on", false); }
    void setAdhanOn(boolean on) { sp.edit().putBoolean("adhan_on", on).apply(); }

    /** The adhan's own volume, in percent; the TV's volume comes back after it. */
    int adhanVolume() { int v = sp.getInt("adhan_volume", 30); return v < 0 || v > 100 ? 30 : v; }   // 4.4: 0..100 (an older app reads under 10 as its 30)
    void setAdhanVolume(int percent) { sp.edit().putInt("adhan_volume", percent).putBoolean("adhan_volume_tv", false).apply(); }
    /** 4.4 «مثل صوت التلفزيون»: the adhan leaves the TV's volume as it is */
    boolean adhanVolumeTv() { return sp.getBoolean("adhan_volume_tv", false); }
    void setAdhanVolumeTv() { sp.edit().putBoolean("adhan_volume_tv", true).apply(); }
    /** the general level (AdhanLevel: TV or a percent) */
    int adhanVolumeLevel() { return AdhanLevel.general(adhanVolume(), adhanVolumeTv()); }
    void setAdhanVolumeLevel(int level) { if (level == AdhanLevel.TV) setAdhanVolumeTv(); else setAdhanVolume(AdhanLevel.clamp(level)); }
    /** 4.4: a prayer's own level ("" the general one, "tv", "0".."100") */
    String prayerVolume(int prayer) { String c = sp.getString("pv_" + prayer, ""); return AdhanLevel.valid(c) ? c : ""; }
    void setPrayerVolume(int prayer, String choice) { sp.edit().putString("pv_" + prayer, AdhanLevel.valid(choice) ? choice : "").apply(); }
    /** the level of the adhan for `prayer` (-1: no prayer, the general one) */
    int volumeFor(int prayer) { return prayer < 0 ? adhanVolumeLevel() : AdhanLevel.forPrayer(prayerVolume(prayer), adhanVolumeLevel()); }
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
    /** 4.3 C16: the person's second recording, for Fajr only (OwnAdhan.fajrFile); never a numbered voice (those are 1..21) */
    static final int OWN_FAJR = 100;
    boolean fajrOwnSecond() { return sp.getBoolean("adhan_fajr_own_second", false); }
    void setFajrOwnSecond(boolean on) { sp.edit().putBoolean("adhan_fajr_own_second", on).apply(); }
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
    void setFajrVoice(int voice) { freezeFajr(); sp.edit().putInt("adhan_fajr_voice", voice).putBoolean("adhan_fajr_own", false).putBoolean("adhan_fajr_own_second", false).apply(); }
    /** The person's own recording at Fajr: until it is chosen apart it follows the one for the other prayers (as it did before there was a Fajr voice). */
    boolean fajrOwn() { return sp.contains("adhan_fajr_own") ? sp.getBoolean("adhan_fajr_own", false) : ownChosen(); }
    void setFajrOwn(boolean on) { freezeFajr(); sp.edit().putBoolean("adhan_fajr_own", on).putBoolean("adhan_fajr_own_second", false).apply(); }
    /** What plays at Fajr: OWN when the recording is chosen for it and still on the TV, else the Fajr voice. */
    int playFajrVoice(Context c) {
        if (fajrOwnSecond() && OwnAdhan.fajrExists(c)) return OWN_FAJR;   // 4.3 C16: the second recording, chosen for Fajr
        return fajrOwn() && OwnAdhan.exists(c) ? OWN : fajrVoice();
    }
    /** What plays at a prayer: Fajr has its own voice. */
    int playVoiceFor(Context c, boolean fajr) { return fajr ? playFajrVoice(c) : playVoice(c); }

    /** 4.4 «أذان كل صلاة»: this prayer's choice (PrayerSound), "" when the general adhan plays as before */
    String prayerSound(int prayer) { String c = sp.getString("pp_" + prayer, ""); return PrayerSound.valid(c) ? c : ""; }
    void setPrayerSound(int prayer, String choice) { sp.edit().putString("pp_" + prayer, PrayerSound.valid(choice) ? choice : "").apply(); }

    /** 4.4 beta 4: does this prayer show on the screen (the bar, «تنبيه عند الأذان»)? On unless its flag is off; a prayer of no number (an older alarm) shows. */
    boolean screenOn(int prayer) { return prayer < 0 || prayer > 5 || sp.getBoolean("ps_" + prayer, true); }
    void setScreenOn(int prayer, boolean on) {
        if (prayer < 0 || prayer > 5) return;
        sp.edit().putBoolean("ps_" + prayer, on).apply();
        PrayerScreen.cache(hideMask());
    }
    /** the hidden prayers as PrayerScreen.hidden reads them */
    int hideMask() { int m = 0; for (int p : PrayerScreen.IDS) if (!screenOn(p)) m |= 1 << p; return m; }
    /** the prayer's sound is off (PrayerSound «silent») */
    boolean soundOff(int prayer) { return "silent".equals(prayerSound(prayer)); }
    /** the four states at once: a prayer's sound and its screen (PrayerScreen) */
    int prayerState(int prayer) { return PrayerScreen.state(!soundOff(prayer), screenOn(prayer)); }
    void setPrayerState(int prayer, int state) {
        boolean wantSound = PrayerScreen.sound(state);
        if (wantSound && soundOff(prayer)) setPrayerSound(prayer, "");   // heard again: the general adhan, as before
        else if (!wantSound && !soundOff(prayer)) setPrayerSound(prayer, "silent");
        setScreenOn(prayer, PrayerScreen.screen(state));
    }

    /** 4.4 «ساعات الهدوء»: off by default; 23:00 to 05:00 when first turned on; the adhan in it heard by default */
    boolean quietOn() { return sp.getBoolean("quiet_on", false); }
    int quietFrom() { return Math.floorMod(sp.getInt("quiet_from", 23 * 60), 1440); }
    int quietTo() { return Math.floorMod(sp.getInt("quiet_to", 5 * 60), 1440); }
    int quietAdhan() { int v = sp.getInt("quiet_adhan", QuietHours.ADHAN_HEARD); return v < 0 || v > 2 ? QuietHours.ADHAN_HEARD : v; }
    void setQuiet(boolean on, int from, int to, int adhan) {
        sp.edit().putBoolean("quiet_on", on).putInt("quiet_from", Math.floorMod(from, 1440)).putInt("quiet_to", Math.floorMod(to, 1440)).putInt("quiet_adhan", adhan).apply();
    }

    /** the quiet hours hold now, in the city's own time */
    boolean quietNow(Context c) {
        if (!quietOn()) return false;
        java.util.Calendar now = java.util.Calendar.getInstance(sa.urtv.home.ZoneFix.zone(Place.get(c).tz), java.util.Locale.US);
        return QuietHours.inside(now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE), quietFrom(), quietTo());
    }

    /** 4.4: what plays at this prayer now: a voice (Prefs.OWN…, 1..COUNT) or PrayerSound.SILENT */
    int playFor(Context c, int prayer) {
        boolean friday = fridayDhuhr()
                && java.util.Calendar.getInstance(sa.urtv.home.ZoneFix.zone(Place.get(c).tz), java.util.Locale.US).get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY;
        return PrayerSound.voice(prayerSound(prayer), prayer, playVoiceFor(c, prayer == PrayerTimes.FAJR), OwnAdhan.exists(c), OwnAdhan.fajrExists(c), friday);
    }

    /** 4.4 touch «أذان الجمعة»: on by default: a Dhuhr set silent is still heard on Fridays (it matters only when Dhuhr is silent) */
    boolean fridayDhuhr() { return sp.getBoolean("friday_dhuhr", true); }
    void setFridayDhuhr(boolean on) { sp.edit().putBoolean("friday_dhuhr", on).apply(); }

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

    /** the reciters marked ★ (ids, comma separated, in the order they were marked): shown first on the reciters tab under «المفضلون» */
    String reciterFavs() { return sp.getString("reciter_favs", ""); }
    /** 4.4: the stations' troubles of the last seven days (RadioWeek) */
    String radioWeek() { return sp.getString("radio_week", ""); }
    void radioWeek(String r) { sp.edit().putString("radio_week", r).apply(); }
    void setReciterFavs(String ids) { sp.edit().putString("reciter_favs", ids == null ? "" : ids).apply(); }

    /** «أذان مختصر»: the adhan stops after its first minute with a short fade (not the few seconds of a preview). Off until it is turned on. */
    boolean adhanShort() { return sp.getBoolean("adhan_short", false); }
    void setAdhanShort(boolean on) {
        SharedPreferences.Editor e = sp.edit().putBoolean("adhan_short", on);
        if (on) e.putBoolean("adhan_takbir", false);   // one length at a time
        e.apply();
    }

    /** «تكبير فقط»: the third length, next to «أذان مختصر»: only the opening takbir plays, then a fade (not a preview). Off until it is turned on; it turns «أذان مختصر» off and the other way round. */
    boolean adhanTakbir() { return sp.getBoolean("adhan_takbir", false); }
    void setAdhanTakbir(boolean on) {
        SharedPreferences.Editor e = sp.edit().putBoolean("adhan_takbir", on);
        if (on) e.putBoolean("adhan_short", false);
        e.apply();
    }

    /** «الأذان على شاشة التوقف فقط»: the adhan plays only over the screensaver or our home; another app or an input in front gets none. Off until it is turned on. */
    boolean adhanSaverOnly() { return sp.getBoolean("adhan_saver_only", false); }
    void setAdhanSaverOnly(boolean on) { sp.edit().putBoolean("adhan_saver_only", on).apply(); }

    /** «تنبيه عند الأذان»: a 5-second notice with the prayer's name when its time comes, whether or not the adhan plays. Off until it is turned on. */
    boolean adhanNotice() { return sp.getBoolean("adhan_notice", false); }
    void setAdhanNotice(boolean on) { sp.edit().putBoolean("adhan_notice", on).apply(); }

    /** The alarm goes off for the adhan or for any option that acts at the prayer's time (the notice). */
    boolean alarmWanted() { return adhanOn() || adhanNotice() || adhanSaverAt() || IqamaPlan.wanted(iqamaSound(), iqamaNotice()); }   // 4.4: the iqama's alarm is set by the adhan's

    /** 4.4 «الإقامة»: its sound (IqamaPlan: "" none, the default; "chime"; "a<id>" one of «صوتياتي»), its volume (10-100, 30) and «تنبيه الإقامة» (off) */
    String iqamaSound() { String s = sp.getString("iqama_sound", ""); return IqamaPlan.validSound(s) ? s : ""; }
    void setIqamaSound(String s) { sp.edit().putString("iqama_sound", IqamaPlan.validSound(s) ? s : "").apply(); }
    int iqamaVolume() { int v = sp.getInt("iqama_volume", 30); return v < 10 || v > 100 ? 30 : v; }
    void setIqamaVolume(int v) { sp.edit().putInt("iqama_volume", Math.max(10, Math.min(100, v))).apply(); }
    boolean iqamaNotice() { return sp.getBoolean("iqama_notice", false); }
    void setIqamaNotice(boolean on) { sp.edit().putBoolean("iqama_notice", on).apply(); }

    /** «شاشة التوقف وقت الأذان»: at the prayer's time the screensaver starts, if our home is in front (never over another app). Off until it is turned on. */
    boolean adhanSaverAt() { return sp.getBoolean("adhan_saver_at", false); }
    void setAdhanSaverAt(boolean on) { sp.edit().putBoolean("adhan_saver_at", on).apply(); }

    /** What the last alarm did (AdhanPlan.note) and when, for `music.sh adhan`: a tester with the sound at 0 reads this. */
    String lastAlarmNote() { return sp.getString("adhan_alarm_note", ""); }
    long lastAlarmNoteAt() { return sp.getLong("adhan_alarm_note_at", 0); }
    void setLastAlarmNote(String note, long at) { sp.edit().putString("adhan_alarm_note", note).putLong("adhan_alarm_note_at", at).apply(); }

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

    /** 4.4 beta 3: the voices on the TV («3,10», written by Adhan.keepNow off the UI thread), the next adhan's time (the alarm's), and when an adhan could not play */
    String adhanSaved() { return sp.getString("adhan_saved", ""); }
    void setAdhanSaved(String text) { sp.edit().putString("adhan_saved", text).apply(); }
    long nextAdhanAt() { return sp.getLong("adhan_next_at", 0); }
    void setNextAdhanAt(long at) { sp.edit().putLong("adhan_next_at", at).apply(); }
    long adhanMissed() { return sp.getLong("adhan_missed", 0); }
    void setAdhanMissed(long at) { sp.edit().putLong("adhan_missed", at).apply(); }
}
