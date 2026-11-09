package sa.urtv.home.tilawah;

/**
 * The words and the plain checks of «أذان من ملفي», with no Android in them (tests/OwnAdhanRun.java runs them):
 * what a file may be, and how its size is said.
 */
final class OwnAdhanText {
    /** «أذان من ملفي»: the row in the voice list and the name shown while it plays. */
    static final String NAME = sa.urtv.home.S.n("أذان من ملفي");
    static final String PLACE = sa.urtv.home.S.n("تسجيلك على هذا التلفزيون فقط");
    /** 4.3 C16: the second recording, for Fajr */
    static final String NAME_FAJR = sa.urtv.home.S.n("أذاني للفجر");

    /** 5 minutes of 48 kbps mono AAC is 1.8 MB; the TV keeps nothing larger than this. */
    static final long MAX_BYTES = 3L << 20;
    /** the adhan is at most 5 minutes (a few seconds over are the encoder's rounding) */
    static final long MAX_MS = (5 * 60 + 5) * 1000L;

    private OwnAdhanText() {}

    /** "mp4" (an m4a: AAC), "ogg" (Opus or Vorbis) or "" for anything else, from the first bytes of the file. */
    static String container(byte[] head, int n) {
        if (n >= 8 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') return "mp4";
        if (n >= 4 && head[0] == 'O' && head[1] == 'g' && head[2] == 'g' && head[3] == 'S') return "ogg";
        return "";
    }

    /** "" when a file of that size and length may be kept, else why not. */
    static String refuse(long bytes, long ms) {
        if (bytes <= 0) return "no such file";
        if (bytes > MAX_BYTES) return "larger than " + (MAX_BYTES >> 20) + " MB (optimize/music.sh adhan add makes it small first)";
        if (ms <= 0) return "not a readable recording";
        if (ms > MAX_MS) return "longer than 5 minutes";
        return "";
    }

    /** "1.7 MB", "340 KB": the size in the words of the status line. */
    static String size(long bytes) {
        if (bytes >= 1L << 20) return (Math.round(bytes * 10.0 / (1L << 20)) / 10.0) + " MB";
        return Math.max(1, (bytes + 1023) / 1024) + " KB";
    }
}
