package sa.urtv.home.tilawah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;

/**
 * ADB control for agents and optimize/music.sh: GET_PLAYER state, play, pause, next, sleep, and the
 * adhan (play now, switch it on or off, pick the voice, the person's own recording, set its volume).
 *
 *   ADHAN_OWN --es op info | add --ei size BYTES [--es path /data/local/tmp/urtv-own/in.m4a] | clear
 *   VOICE --ei voice 1..21 (a recording of ours, not a Fajr one) or 0 (the person's own, when there is one): the other prayers' muezzin
 *   FAJR_VOICE --ei voice 1..21 or 0: «مؤذن الفجر», the one for Fajr (the four Fajr recordings and the normal ones)
 *   ADHAN_SHORT --ez on true|false: «أذان مختصر»
 *   ADHAN_TAKBIR --ez on true|false: «تكبير فقط» (turns «أذان مختصر» off)
 *   ADHAN_STOPKEY --ez on true|false: «إيقاف الأذان بأي زر»
 *   ADHAN_SAVERONLY --ez on true|false: «الأذان على شاشة التوقف فقط»
 *   ADHAN_NOTICE --ez on true|false: «تنبيه عند الأذان»
 *   ADHAN_SAVERAT --ez on true|false: «شاشة التوقف وقت الأذان»
 *   STOP_ADHAN: stop the adhan that is playing (nothing else)
 */
public class MusicReceiver extends BroadcastReceiver {

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        if ("sa.urtv.home.GET_PLAYER".equals(action)) {
            setResultData(status(context));
            return;
        }

        Prefs prefs = new Prefs(context);
        switch (action) {
            case "sa.urtv.home.ADHAN": {
                int voice = intent.getIntExtra(MusicService.EXTRA_VOICE, prefs.playVoiceFor(context, intent.getBooleanExtra("fajr", false)));
                if (voice == Prefs.OWN && !OwnAdhan.exists(context)) return;   // nothing of theirs to play
                MusicService.adhan(context, voice, null, 0, intent.getBooleanExtra("fajr", false) ? PrayerTimes.FAJR : -1);
                return;
            }
            case "sa.urtv.home.RECITER_FAV": {   // optimize/saver.sh reciter-fav add|remove <id>: a reciter ★ or not (an id the catalogue has)
                String id = intent.getStringExtra("id"), op = intent.getStringExtra("op");
                if (id == null || catalogIndex(context, id) < 0) { setResultData("error: unknown reciter"); return; }
                if (!"add".equals(op) && !"remove".equals(op)) { setResultData("error: op is add or remove"); return; }
                prefs.setReciterFavs(ReciterOrder.toggle(prefs.reciterFavs(), id, "add".equals(op)));
                setResultData("saved\nreciter_favs=" + prefs.reciterFavs());
                return;
            }
            case "sa.urtv.home.RECITER_FAVS": {   // settings import: the whole list at once ("-" = none); all ids must be in the catalogue, or nothing changes
                String ids = intent.getStringExtra("ids");
                List<String> want = "-".equals(ids) ? new ArrayList<String>() : ReciterOrder.parse(ids);
                for (String id : want) if (catalogIndex(context, id) < 0) { setResultData("error: unknown reciter"); return; }
                prefs.setReciterFavs(String.join(",", want));
                setResultData("saved\nreciter_favs=" + prefs.reciterFavs());
                return;
            }
            case "sa.urtv.home.ADHAN_RESCHEDULE":   // optimize/saver.sh prayeradj: the alarm for the minutes as they are now
                AdhanSync.reschedule(context);
                setResultData("rescheduled");
                return;
            case "sa.urtv.home.ADHAN_ON":
            case "sa.urtv.home.ADHAN_OFF":
                AdhanSettings.on(context, prefs, "sa.urtv.home.ADHAN_ON".equals(action));
                return;
            case "sa.urtv.home.ADHAN_VOLUME":   // optimize/music.sh adhan volume 60
                AdhanSettings.volume(prefs, intent.getIntExtra("percent", -1));
                return;
            case "sa.urtv.home.VOICE": {
                String error = AdhanSettings.voice(context, prefs, intent.getIntExtra(MusicService.EXTRA_VOICE, prefs.voice()));
                setResultData(error != null ? error : "saved\n" + ownListing(context, prefs));
                return;
            }
            case "sa.urtv.home.FAJR_VOICE": {
                String error = AdhanSettings.fajrVoice(context, prefs, intent.getIntExtra(MusicService.EXTRA_VOICE, prefs.fajrVoice()));
                setResultData(error != null ? error : "saved\n" + ownListing(context, prefs));
                return;
            }
            case "sa.urtv.home.ADHAN_SHORT":   // optimize/music.sh adhan short on|off
                prefs.setAdhanShort(intent.getBooleanExtra("on", false));
                setResultData("saved\nshort=" + (prefs.adhanShort() ? "on" : "off"));
                return;
            case "sa.urtv.home.ADHAN_TAKBIR":   // optimize/music.sh adhan takbir on|off
                prefs.setAdhanTakbir(intent.getBooleanExtra("on", false));
                setResultData("saved\ntakbir=" + (prefs.adhanTakbir() ? "on" : "off") + "\nshort=" + (prefs.adhanShort() ? "on" : "off"));
                return;
            case "sa.urtv.home.ADHAN_STOPKEY":   // optimize/music.sh adhan stopkey on|off
                prefs.setAdhanStopKey(intent.getBooleanExtra("on", false));
                setResultData("saved\nstopkey=" + (prefs.adhanStopKey() ? "on" : "off"));
                return;
            case "sa.urtv.home.ADHAN_SAVERONLY":   // optimize/music.sh adhan saver-only on|off
                prefs.setAdhanSaverOnly(intent.getBooleanExtra("on", false));
                setResultData("saved\nsaver_only=" + (prefs.adhanSaverOnly() ? "on" : "off"));
                return;
            case "sa.urtv.home.ADHAN_NOTICE":   // optimize/music.sh adhan notice on|off
                prefs.setAdhanNotice(intent.getBooleanExtra("on", false));
                AdhanScheduler.schedule(context);   // the alarm is wanted for the notice alone, too
                setResultData("saved\nnotice=" + (prefs.adhanNotice() ? "on" : "off"));
                return;
            case "sa.urtv.home.ADHAN_SAVERAT":   // optimize/music.sh adhan saver-at on|off
                prefs.setAdhanSaverAt(intent.getBooleanExtra("on", false));
                AdhanScheduler.schedule(context);   // the alarm is wanted for the screensaver alone, too
                setResultData("saved\nsaver_at=" + (prefs.adhanSaverAt() ? "on" : "off"));
                return;
            case "sa.urtv.home.ADHAN_OWN":
                setResultData(own(context, prefs, intent));
                return;
            default:
                break;
        }

        Catalog cat = Catalog.get(context);
        Intent service = new Intent(context, MusicService.class).setAction(action);
        switch (action) {
            case MusicService.ACTION_PLAY_SURAH:
                service.putExtra(MusicService.EXTRA_RECITER, intent.getStringExtra("reciter"));
                service.putExtra(MusicService.EXTRA_SURAH, intent.getIntExtra("surah", 1));
                break;
            case MusicService.ACTION_PLAY_RADIO: {
                int index = intent.getIntExtra("station", -1);
                String name = intent.getStringExtra("station");
                if (index < 0 && name != null) index = cat.stationIndex(name);
                if (index < 0) return;
                service.putExtra(MusicService.EXTRA_STATION, index);
                break;
            }
            case MusicService.ACTION_SLEEP:
                service.putExtra(MusicService.EXTRA_MINUTES, intent.getIntExtra("minutes", 0));
                break;
            case MusicService.ACTION_SEEK_BY:
                service.putExtra(MusicService.EXTRA_DELTA, intent.getIntExtra("delta", 0));
                break;
            default:
                break;
        }
        MusicService.start(context, service);
    }

    /** «أذان من ملفي», for optimize/music.sh: info, add (the shell has just written the file, or it is in /data/local/tmp/urtv-own/), clear. */
    private static String own(Context context, Prefs prefs, Intent intent) {
        String op = String.valueOf(intent.getStringExtra("op"));
        switch (op) {
            case "info":
                return ownListing(context, prefs);
            case "add": {
                String path = intent.getStringExtra("path");
                java.io.File src = OwnAdhan.incoming(context);
                if (path != null) {
                    if (!path.matches("/data/local/tmp/urtv-own/[A-Za-z0-9_.-]+") || path.contains(".."))
                        return "error: path must be a file in /data/local/tmp/urtv-own/";
                    src = new java.io.File(path);
                }
                int size = intent.getIntExtra("size", -1);   // what the shell sent: a short or stale file is refused
                boolean forFajr = intent.getBooleanExtra("fajr", false);   // 4.3 C16: the second recording, for Fajr only
                String why = size >= 0 && src.length() != size
                        ? "the file is not the one sent (" + src.length() + " of " + size + " bytes)"
                        : OwnAdhan.keep(context, src, forFajr);
                if (why != null && src.equals(OwnAdhan.incoming(context))) src.delete();
                if (why != null) return "error: " + why;
                if (forFajr) {   // only Fajr changes: the other prayers keep what they had
                    prefs.setFajrOwn(true);
                    prefs.setFajrOwnSecond(true);
                } else {
                    prefs.setOwnChosen(true);   // a recording just added is the one the person wants, at Fajr too (unless a Fajr one is chosen)
                    if (!prefs.fajrOwnSecond()) prefs.setFajrOwn(true);
                }
                return "saved\n" + ownListing(context, prefs);
            }
            case "clear":
                OwnAdhan.clear(context);   // both recordings
                prefs.setOwnChosen(false);
                prefs.setFajrOwn(false);
                return "saved\n" + ownListing(context, prefs);
            case "clear-fajr":   // 4.3 C16: only the Fajr one; Fajr goes back to the first recording if it had it, else its voice
                OwnAdhan.clearFajr(context);
                prefs.setFajrOwnSecond(false);
                return "saved\n" + ownListing(context, prefs);
            default:
                return "error: op is one of info add clear clear-fajr";
        }
    }

    private static String ownListing(Context context, Prefs prefs) {
        return (OwnAdhan.exists(context)
                ? "own: " + OwnAdhanText.size(OwnAdhan.bytes(context)) + " (" + inUse(context, prefs) + ")"
                : "own: none")
                + (OwnAdhan.fajrExists(context) ? "\nown_fajr: " + OwnAdhanText.size(OwnAdhan.fajrFile(context).length())
                        + (prefs.playFajrVoice(context) == Prefs.OWN_FAJR ? " (in use at Fajr)" : " (kept, not in use)") : "");
    }

    private static String inUse(Context context, Prefs prefs) {
        boolean normal = prefs.playVoice(context) == Prefs.OWN, fajr = prefs.playFajrVoice(context) == Prefs.OWN;
        return normal && fajr ? "in use" : normal ? "in use, not at Fajr" : fajr ? "in use at Fajr only" : "kept, not in use";
    }

    private static int catalogIndex(Context c, String id) { return Catalog.get(c).reciterIndex(id); }

    private String status(Context context) {
        Catalog cat = Catalog.get(context);
        Prefs prefs = new Prefs(context);
        int source = MusicService.currentSource();
        boolean playing = MusicService.isPlaying();
        int reciter = MusicService.reciterIndex();
        int surah = MusicService.currentSurah();
        int station = MusicService.currentStation();
        int position = MusicService.currentPosition();
        int duration = MusicService.currentDuration();
        String error = MusicService.currentError();
        long sleepAt = MusicService.sleepAt();

        if (source == Prefs.SOURCE_NONE) {
            source = prefs.source();
            surah = prefs.surah();
            station = prefs.station();
            position = prefs.position();
            reciter = cat.reciterIndex(prefs.reciter());
            duration = 0;
            error = null;
            sleepAt = 0;
        }

        String reciterId = reciter >= 0 && reciter < cat.reciters.size() ? cat.reciters.get(reciter).id : "";
        String reciterName = reciter >= 0 && reciter < cat.reciters.size() ? cat.reciters.get(reciter).name : "";
        String stationName = station >= 0 && station < cat.stations.size() ? cat.stations.get(station).name : "";
        long sleepLeft = sleepAt > 0 ? Math.max(0, (sleepAt - System.currentTimeMillis()) / 1000) : 0;
        PrayerTimes.Next next = PrayerTimes.next(context);

        return "playing=" + (playing ? 1 : 0)
                + "|source=" + (source == Prefs.SOURCE_SURAH ? "surah"
                        : source == Prefs.SOURCE_RADIO ? "radio"
                        : source == Prefs.SOURCE_ADHAN ? "adhan" : "none")
                + "|reciter=" + reciterId
                + "|reciter_name=" + reciterName
                + "|surah=" + (source == Prefs.SOURCE_SURAH ? surah : 0)
                + "|surah_name=" + (source == Prefs.SOURCE_SURAH ? cat.surahName(surah) : "")
                + "|station=" + (source == Prefs.SOURCE_RADIO ? station : -1)
                + "|station_name=" + (source == Prefs.SOURCE_RADIO ? stationName : "")
                + "|pos=" + position
                + "|dur=" + duration
                + "|sleep=" + sleepLeft
                + "|adhan=" + (prefs.adhanOn() ? "on" : "off")
                + "|voice=" + (prefs.playVoice(context) == Prefs.OWN ? "own" : String.valueOf(prefs.voice()))
                + "|fajr_voice=" + (prefs.playFajrVoice(context) == Prefs.OWN_FAJR ? "own_fajr" : prefs.playFajrVoice(context) == Prefs.OWN ? "own" : String.valueOf(prefs.fajrVoice()))
                + "|adhan_short=" + (prefs.adhanShort() ? "on" : "off")
                + "|adhan_takbir=" + (prefs.adhanTakbir() ? "on" : "off")
                + "|adhan_stopkey=" + (prefs.adhanStopKey() ? "on" : "off")
                + "|adhan_saver_only=" + (prefs.adhanSaverOnly() ? "on" : "off")
                + "|adhan_saver_at=" + (prefs.adhanSaverAt() ? "on" : "off")
                + "|adhan_notice=" + (prefs.adhanNotice() ? "on" : "off")
                + "|adhan_alarm_last=" + prefs.lastAlarmNoteAt() + (prefs.lastAlarmNote().isEmpty() ? "" : " " + prefs.lastAlarmNote().replace('|', '/'))
                + "|adhan_live=" + (MusicService.adhanLive() ? 1 : 0)
                + "|adhan_volume=" + prefs.adhanVolume()
                + "|adhan_exact=" + (AdhanScheduler.exactAllowed(context) ? 1 : 0)
                + "|own_bytes=" + (OwnAdhan.exists(context) ? OwnAdhan.bytes(context) : 0)
                + "|reciter_favs=" + prefs.reciterFavs()
                + "|adhan_next=" + (next == null ? 0 : next.at)
                + "|adhan_next_name=" + (next == null ? "" : next.name())
                + "|adhan_next_clock=" + (next == null ? "" : next.clock)
                + "|adhan_last=" + prefs.lastAdhan()
                + "|error=" + (error == null ? "" : error)
                + "|rebuffers=" + MusicService.rebufferCount()
                + "|rebuffer_ms=" + MusicService.rebufferMillis()
                + "|errors=" + MusicService.errorCount()
                + "|last_error=" + MusicService.lastErrorCode()
                + "|last_error_at=" + MusicService.lastErrorAt()
                + "|buffering=" + (MusicService.isBuffering() ? 1 : 0)
                + "|player=" + MusicService.playerKind()
                + "|stall_restarts=" + MusicService.stallRestartCount();
    }
}
