package sa.urtv.home.tilawah;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.4 beta 4 (Xiaomi A9: a surah stuttered or sounded slowed, the stations fine): the order ExoPlayer tries the TV's MP3 decoders in, for a
 * surah. The platform's own software decoder (c2.android, OMX.google, c2.google) first, then the TV maker's, as the TV listed them: a vendor
 * MP3 decoder that reports the wrong output sample rate plays a 44.1 kHz file slowed down and in bursts, and the software one decodes MP3 on
 * any TV with little work. Another type (AAC, the stations' HLS) keeps the TV's own order. No Android in here (tests/DecoderOrderRun.java).
 */
final class DecoderOrder {
    private DecoderOrder() { }

    static final String MP3 = "audio/mpeg";

    /** the platform's software decoder, by its name */
    static boolean platform(String name) {
        if (name == null) return false;
        String n = name.toLowerCase(java.util.Locale.ROOT);
        return n.startsWith("c2.android.") || n.startsWith("omx.google.") || n.startsWith("c2.google.");
    }

    /** the indexes of `names` in the order to try them for `mime` */
    static List<Integer> order(String mime, List<String> names) {
        List<Integer> first = new ArrayList<>(), rest = new ArrayList<>();
        boolean mp3 = MP3.equals(mime);
        for (int i = 0; i < names.size(); i++) (mp3 && platform(names.get(i)) ? first : rest).add(i);
        first.addAll(rest);
        return first;
    }
}
