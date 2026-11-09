package sa.urtv.home.tilawah;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the phone control (sa.urtv.home.phone) may reach inside the «تلاوة» package, which keeps its classes to itself: the adhan's settings
 * and the Quran player. The adhan goes through AdhanSettings, the same code the ADB receiver uses; the player gets the same intents the
 * ADB receiver and the screens send. The phone's values were already checked by phone.Schema; they are checked again here.
 */
public final class PhoneBridge {
    private PhoneBridge() { }

    /** The adhan, the voices, the reciters and the player, for the phone page. */
    public static Map<String, Object> state(Context c) {
        Prefs prefs = new Prefs(c);
        Catalog cat = Catalog.get(c);
        Map<String, Object> adhan = new LinkedHashMap<>();
        adhan.put("on", prefs.adhanOn());
        adhan.put("voice", (long) (prefs.playVoice(c) == Prefs.OWN ? 0 : prefs.voice()));
        adhan.put("fajr_voice", (long) (OwnAdhan.isOwn(prefs.playFajrVoice(c)) ? 0 : prefs.fajrVoice()));
        adhan.put("short", prefs.adhanShort());
        adhan.put("stopkey", prefs.adhanStopKey());
        adhan.put("volume", (long) prefs.adhanVolume());
        List<Object> voices = new ArrayList<>();
        for (int v = 1; v <= Adhan.VOICES; v++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", (long) v);
            m.put("name", Adhan.name(v));
            m.put("fajr", AdhanVoices.isFajr(v));
            voices.add(m);
        }
        List<Object> reciters = new ArrayList<>();
        for (Catalog.Reciter r : cat.reciters) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.id);
            m.put("name", r.name);
            reciters.add(m);
        }
        List<Object> surahs = new ArrayList<>();
        for (int n = 1; n <= 114; n++) surahs.add(cat.surahName(n));
        int source = MusicService.currentSource();
        long sleepAt = MusicService.sleepAt();
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("playing", MusicService.isPlaying());
        player.put("source", source == Prefs.SOURCE_SURAH ? "surah" : source == Prefs.SOURCE_RADIO ? "radio" : source == Prefs.SOURCE_ADHAN ? "adhan" : "none");
        int reciter = MusicService.reciterIndex();
        player.put("reciter", reciter >= 0 && reciter < cat.reciters.size() ? cat.reciters.get(reciter).id : "");
        player.put("surah", (long) (source == Prefs.SOURCE_SURAH ? MusicService.currentSurah() : 0));
        player.put("sleep_left", sleepAt > 0 ? Math.max(0L, (sleepAt - System.currentTimeMillis()) / 1000) : 0L);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("adhan", adhan);
        out.put("voices", voices);
        out.put("reciters", reciters);
        out.put("surahs", surahs);
        out.put("player", player);
        return out;
    }

    /** Adds what the phone's «يعمل الآن» shows about the adhan: adhan_live, adhan_prayer (while it plays) and next_adhan {prayer, in_seconds}. */
    public static void addAdhanLive(Context c, Map<String, Object> out) {
        sa.urtv.home.S.init(c);   // «اللغة»: the prayer's name as the TV says it (English with English)
        boolean live = MusicService.adhanLive();
        out.put("adhan_live", live);
        int p = MusicService.adhanPrayer();
        out.put("adhan_prayer", live && p >= 0 && p < PrayerTimes.NAMES.length ? sa.urtv.home.S.tv(PrayerTimes.NAMES[p]) : "");
        try {
            PrayerTimes.Next n = PrayerTimes.nextAlarm(c);
            if (n != null) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("prayer", sa.urtv.home.S.tv(n.name()));
                m.put("in_seconds", Math.max(0L, (n.at - System.currentTimeMillis()) / 1000));
                out.put("next_adhan", m);
            }
        } catch (RuntimeException ignored) {
            // no place or times yet: the page just shows no next adhan
        }
    }

    /** «أوقف الأذان الآن»: stops the adhan that is playing and nothing else; false when none is. */
    public static boolean stopAdhan(Context c) {
        if (!MusicService.adhanLive()) return false;
        MusicService.stopAdhan(c);
        return true;
    }

    /** Applies the adhan keys of a checked "set" (adhan_on, adhan_voice, adhan_fajr_voice, adhan_short, adhan_stopkey, adhan_volume). Null when saved, else a fixed code. */
    public static String adhan(Context c, Map<String, Object> v) {
        Prefs prefs = new Prefs(c);
        // all or nothing: the values are looked at first
        if (v.containsKey("adhan_voice")) {
            int n = ((Long) v.get("adhan_voice")).intValue();
            if (!AdhanVoices.valid(n) || AdhanVoices.isFajr(n)) return "refused";
        }
        if (v.containsKey("adhan_fajr_voice") && !AdhanVoices.valid(((Long) v.get("adhan_fajr_voice")).intValue())) return "refused";
        if (v.containsKey("adhan_volume")) {
            long n = (Long) v.get("adhan_volume");
            if (n < 10 || n > 100) return "refused";
        }
        if (v.containsKey("adhan_voice") && AdhanSettings.voice(c, prefs, ((Long) v.get("adhan_voice")).intValue()) != null) return "refused";
        if (v.containsKey("adhan_fajr_voice") && AdhanSettings.fajrVoice(c, prefs, ((Long) v.get("adhan_fajr_voice")).intValue()) != null) return "refused";
        if (v.containsKey("adhan_volume")) AdhanSettings.volume(prefs, ((Long) v.get("adhan_volume")).intValue());
        if (v.containsKey("adhan_short")) prefs.setAdhanShort((Boolean) v.get("adhan_short"));
        if (v.containsKey("adhan_stopkey")) prefs.setAdhanStopKey((Boolean) v.get("adhan_stopkey"));
        if (v.containsKey("adhan_on")) AdhanSettings.on(c, prefs, (Boolean) v.get("adhan_on"));
        return null;
    }

    /** The Quran player: play (reciter, surah), pause, resume, toggle, next, prev, stop, sleep (minutes, 0 cancels). Null when sent, else a fixed code. */
    public static String quran(Context c, Map<String, Object> v) {
        String op = (String) v.get("op");
        switch (op) {
            case "play": {
                String reciter = (String) v.get("reciter");
                long surah = (Long) v.get("surah");
                if (Catalog.get(c).reciterIndex(reciter) < 0 || surah < 1 || surah > 114) return "refused";
                MusicService.playSurah(c, reciter, (int) surah);
                return null;
            }
            case "pause": MusicService.pause(c); return null;
            case "resume": MusicService.resume(c); return null;
            case "toggle": MusicService.toggle(c); return null;
            case "next": MusicService.nextAction(c); return null;
            case "prev": MusicService.prevAction(c); return null;
            case "stop": MusicService.stopAction(c); return null;
            case "sleep": {
                long m = (Long) v.get("minutes");
                if (m < 0 || m > 180) return "refused";
                MusicService.sleep(c, (int) m);
                return null;
            }
            default: return "refused";
        }
    }

    /** After the city changed from the phone: the alarm for the new prayer times, as the shell's ADHAN_RESCHEDULE does. */
    public static void rescheduleAdhan(Context c) { AdhanSync.reschedule(c); }

    /** The phone's play/pause: pauses our player when it is the one playing; false when it isn't, and the caller sends the media key to whatever is. */
    public static boolean pauseOurs(Context c) {
        if (!MusicService.isPlaying() && !MusicService.isBuffering()) return false;
        MusicService.toggle(c);
        return true;
    }
}
