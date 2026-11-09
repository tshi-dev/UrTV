package sa.urtv.home.tilawah;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The 21 adhan recordings, named after the muezzin or the mosque that recorded them. They come from
 * the «Azaan of World Mosques» collection on Archive.org, released under the Public Domain Mark 1.0
 * (https://archive.org/details/azaan-of-world-mosques_202608). The file is kept in the cache the
 * first time it is needed, so an adhan still plays when the connection is down.
 */
final class Adhan {
    static final int VOICES = AdhanVoices.COUNT;

    /** Where the recordings come from and under which licence. */
    static final String SOURCE = "archive.org/details/azaan-of-world-mosques_202608 (Public Domain Mark 1.0)";

    private static final String[] NAMES = {
            "عبدالملك الملا",
            "عبدالرحمن خاشقجي",
            "عبدالعزيز بخاري",
            "عبدالمطلب نجدي",
            "سامي ديولي",
            "ماجد حكيم",
            "محمد ماجد حكيم",
            "ناجي قزاز",
            "الحرم المكي",
            "فجر الحرم المكي",
            "الحرم المكي ٢",
            "المسجد الأقصى",
            "أذان القاهرة",
            "أذان القاهرة ٢",
            "أذان مصر",
            "أذان عجمان",
            "أذان أم القيوين",
            "أذان عُمان",
            "أذان تونس",
            "أذان ماليزيا",
            "أذان المالديف",
    };

    private static final String[] PLACES = {
            "مكة المكرمة",
            "مكة المكرمة",
            "مكة المكرمة",
            "مكة المكرمة",
            "مكة المكرمة",
            "مكة المكرمة",
            "مكة المكرمة",
            "القدس",
            "مكة المكرمة",
            "مكة المكرمة",
            "مكة المكرمة",
            "القدس",
            "مصر",
            "مصر",
            "مصر",
            "الإمارات",
            "الإمارات",
            "عُمان",
            "تونس",
            "ماليزيا",
            "المالديف",
    };

    private static final String[] URLS = {
            "https://archive.org/download/azaan-of-world-mosques_202608/%D8%B9%D8%A8%D8%AF%D8%A7%D9%84%D9%85%D9%84%D9%83%20%D8%A7%D9%84%D9%85%D9%84%D8%A7_%D9%85%D9%83%D8%A9%20%28www.alqurantranslation.com%29.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/%D8%B9%D8%A8%D8%AF%D8%A7%D9%84%D8%B1%D8%AD%D9%85%D9%86%20%D8%AE%D8%A7%D8%B4%D9%82%D8%AC%D9%8A%20%28www.alqurantranslation.com%29.MP3",
            "https://archive.org/download/azaan-of-world-mosques_202608/%D8%B9%D8%A8%D8%AF%D8%A7%D9%84%D8%B9%D8%B2%D9%8A%D8%B2%20%D8%A8%D8%AE%D8%A7%D8%B1%D9%8A%20%28www.alqurantranslation.com%29.MP3",
            "https://archive.org/download/azaan-of-world-mosques_202608/%D8%B9%D8%A8%D8%AF%D8%A7%D9%84%D9%85%D8%B7%D9%84%D8%A8%20%D9%86%D8%AC%D8%AF%D9%8A%20%28www.alqurantranslation.com%29.MP3",
            "https://archive.org/download/azaan-of-world-mosques_202608/%D8%B3%D8%A7%D9%85%D9%8A%20%D8%AF%D9%8A%D9%88%D9%84%D9%8A1%20%28www.alqurantranslation.com%29.MP3",
            "https://archive.org/download/azaan-of-world-mosques_202608/%D9%85%D8%A7%D8%AC%D8%AF%20%D8%AD%D9%83%D9%8A%D9%85%20%28www.alqurantranslation.com%29.MP3",
            "https://archive.org/download/azaan-of-world-mosques_202608/%D9%85%D8%AD%D9%85%D8%AF%20%D9%85%D8%A7%D8%AC%D8%AF%20%D8%AD%D9%83%D9%8A%D9%85%20%28www.alqurantranslation.com%29.MP3",
            "https://archive.org/download/azaan-of-world-mosques_202608/Al-Aqsa%20Masjid%20Naaji%20Qazzaaz.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/1st%20Haram%20Makkah%20best.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/2nd%20Fajr%20Haram%20Makkah%20best.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/3rd%20Haram%20Makkah.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/Al-Aqsa%20Masjid%20Naaji%20Qazzaaz%202.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/Cairo%20AlFajr.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/Cairo%20Shuaisha.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/Egypt.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/Ajman%20of%20UAE.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/AlFajr%20Umm%20alQaiwain.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/Oman.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/Tunisia.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/Malaysia%20Al%20Fajr.mp3",
            "https://archive.org/download/azaan-of-world-mosques_202608/Maldive%20Islands.mp3",
    };

    /**
     * The same in English (data, not the table: docs/ENGLISH.md rule 2), shown when «اللغة» is English. The names as the recordings' own
     * file names and the usual Latin spellings write them; the places in English.
     */
    private static final String[] NAMES_EN = {
            "Abdulmalik Al-Mulla",
            "Abdulrahman Khashoggi",
            "Abdulaziz Bukhari",
            "Abdulmuttalib Najdi",
            "Sami Diwali",
            "Majed Hakim",
            "Muhammad Majed Hakim",
            "Naji Qazzaz",
            "Masjid al-Haram",
            "Fajr at Masjid al-Haram",
            "Masjid al-Haram 2",
            "Al-Aqsa Mosque",
            "Cairo adhan",
            "Cairo adhan 2",
            "Egypt adhan",
            "Ajman adhan",
            "Umm Al Quwain adhan",
            "Oman adhan",
            "Tunisia adhan",
            "Malaysia adhan",
            "Maldives adhan",
    };

    private static final String[] PLACES_EN = {
            "Makkah", "Makkah", "Makkah", "Makkah", "Makkah", "Makkah", "Makkah", "Al-Quds", "Makkah", "Makkah", "Makkah", "Al-Quds",
            "Egypt", "Egypt", "Egypt", "UAE", "UAE", "Oman", "Tunisia", "Malaysia", "Maldives",
    };

    private Adhan() {}

    static String url(int voice) { return URLS[clamp(voice) - 1]; }

    /** The muezzin, e.g. «فاروق حضراوي». */
    static String name(int voice) { return voice == Prefs.OWN_FAJR ? sa.urtv.home.S.tv(OwnAdhanText.NAME_FAJR) : voice == Prefs.OWN ? sa.urtv.home.S.tv(OwnAdhanText.NAME) : (sa.urtv.home.S.en() ? NAMES_EN : NAMES)[clamp(voice) - 1]; }

    /** Where it was recorded, e.g. «الحرم المكي». */
    static String place(int voice) { return OwnAdhan.isOwn(voice) ? sa.urtv.home.S.tv(OwnAdhanText.PLACE) : (sa.urtv.home.S.en() ? PLACES_EN : PLACES)[clamp(voice) - 1]; }

    /** Both, for the row that opens this list: «فاروق حضراوي — الحرم المكي». */
    static String label(int voice) { return name(voice) + " — " + place(voice); }

    static File cached(Context c, int voice) {
        return new File(new File(c.getCacheDir(), "adhan"), "adhan-" + clamp(voice) + ".mp3");
    }

    private static int clamp(int voice) { return voice < 1 || voice > VOICES ? 1 : voice; }

    /** a recording shorter than this is a failed or cut-off download, not an adhan */
    private static final long MIN_BYTES = 100_000;
    private static final long TRY_AGAIN_MS = 60 * 60_000L;
    /** the voices being downloaded now, so a stream and a prefetch never fetch the same file twice */
    private static final Set<Integer> LOADING = new HashSet<>();

    static boolean isCached(Context c, int voice) { return cached(c, voice).length() > MIN_BYTES; }

    static volatile boolean skippedInStandby;

    /** The home screen's tick: a prefetch that was skipped in standby is made now (once; nothing is asked when none was skipped). */
    static void prefetchIfSkipped(Context c) {
        if (skippedInStandby) prefetchChosen(c);
    }

    /**
     * The chosen voices (the usual one and the one for Fajr), on their own thread, unless they are on the TV already: asked for by the alarm
     * scheduler and the receiver, so the adhan plays from the cache at its prayer time and the screen «تلاوة» need not have been opened.
     * A failed try is not repeated for an hour.
     */
    static void prefetchChosen(final Context c) {
        if (!sa.urtv.home.NetGate.backgroundOk(sa.urtv.home.NetGateAndroid.displayOn(c))) { skippedInStandby = true; return; }   // not now; AdhanSync asks once when the home is back
        skippedInStandby = false;
        final Prefs prefs = new Prefs(c);
        final int chosen = prefs.voice(), fajr = prefs.fajrVoice();
        long now = System.currentTimeMillis();
        if ((isCached(c, chosen) && isCached(c, fajr)) || now - prefs.prefetchTried() < TRY_AGAIN_MS) return;
        prefs.setPrefetchTried(now);
        new Thread(() -> {
            download(c, chosen);
            if (fajr != chosen) download(c, fajr);
        }, "adhan-chosen").start();
    }

    /**
     * Downloads every voice that is not in the cache yet, one at a time, on its own thread: the chosen voice first,
     * then the Fajr one, then the others, and a voice that fails is skipped, not the end of the round. At most one round a day. The
     * numbered files of the old source are removed once.
     */
    static void prefetchAll(final Context c) {
        final Prefs prefs = new Prefs(c);
        final int chosen = prefs.voice(), fajr = prefs.fajrVoice();
        final String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        if (today.equals(prefs.prefetchDay())) return;
        prefs.setPrefetchDay(today);
        new Thread(() -> {
            File dir = new File(c.getCacheDir(), "adhan");
            File[] old = dir.listFiles((d, n) -> n.startsWith("azan") && n.endsWith(".mp3"));
            if (old != null) for (File f : old) f.delete();
            download(c, chosen);
            if (fajr != chosen) download(c, fajr);
            for (int voice = 1; voice <= VOICES; voice++) {
                if (voice == chosen || voice == fajr) continue;
                download(c, voice);   // a failure here is that voice's: the next one is still asked for
                try { Thread.sleep(2000); } catch (InterruptedException e) { return; }   // gently: this is not urgent
            }
        }, "adhan-cache").start();
    }

    /** Downloads one voice and keeps it only if it came whole; false when the network is not there or the file is short. */
    static boolean download(Context c, int voice) {
        File file = cached(c, voice);
        if (file.length() > MIN_BYTES) return true;
        synchronized (LOADING) { if (!LOADING.add(voice)) return false; }   // another thread is fetching it
        try {
            return fetch(c, voice, file);
        } finally {
            synchronized (LOADING) { LOADING.remove(voice); }
        }
    }

    private static boolean fetch(Context c, int voice, File file) {
        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) return false;
        File tmp = new File(dir, file.getName() + ".part");
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url(voice)).openConnection();
            con.setConnectTimeout(8000);
            con.setReadTimeout(20000);
            con.setRequestProperty("User-Agent", "urtv-home");
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) return false;
            long expected = con.getContentLength();   // -1 when the server does not say
            try (InputStream in = con.getInputStream(); OutputStream out = new FileOutputStream(tmp)) {
                byte[] buffer = new byte[16 * 1024];
                int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            }
            if (tmp.length() <= MIN_BYTES || (expected > 0 && tmp.length() != expected)) {   // cut off: not kept as an adhan
                tmp.delete();
                return false;
            }
            return tmp.renameTo(file);
        } catch (Exception e) {
            tmp.delete();
            return false;
        } finally {
            if (con != null) con.disconnect();
        }
    }
}
