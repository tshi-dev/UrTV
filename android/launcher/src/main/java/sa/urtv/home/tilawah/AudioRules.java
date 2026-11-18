package sa.urtv.home.tilawah;

/**
 * 4.4 «صوتياتي»: the person's own sound files (a dua, a reminder, their adhan), with no Android in it (tests/AudioRulesRun.java runs it).
 * What a file may be (judged by its first bytes, never by its name alone), how big one file and all of them together may be, and the name it
 * is shown by. The files come from a USB stick (UsbActivity) or the phone page (PhoneApi /api/audio); both copy them into the app, nothing
 * plays from the stick or from the phone.
 */
final class AudioRules {
    private AudioRules() { }

    static final long MB = 1L << 20;
    /** the usual caps: 20 MB a file, 100 MB for all */
    static final long FILE_MAX = 20 * MB, TOTAL_MAX = 100 * MB;
    /** «سعة أكبر» (behind a red warning about the TV's storage): 50 MB a file, 500 MB for all */
    static final long BIG_FILE_MAX = 50 * MB, BIG_TOTAL_MAX = 500 * MB;
    /** at most this many files, whatever their size (the list stays usable with the remote) */
    static final int MAX_FILES = 60;
    /** the phone sends a file in pieces of at most this (PhoneServer reads one piece into memory, never the whole file) */
    static final int CHUNK = 512 * 1024;

    static final String[] EXTENSIONS = {"mp3", "m4a", "ogg", "wav"};

    static long fileMax(boolean big) { return big ? BIG_FILE_MAX : FILE_MAX; }
    static long totalMax(boolean big) { return big ? BIG_TOTAL_MAX : TOTAL_MAX; }

    /** "mp3", "m4a", "ogg", "wav" from the first bytes (at least 12), or "" for anything else */
    static String kind(byte[] h, int n) {
        if (h == null || n < 4) return "";
        if (n >= 8 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p') return "m4a";
        if (h[0] == 'O' && h[1] == 'g' && h[2] == 'g' && h[3] == 'S') return "ogg";
        if (n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'A' && h[10] == 'V' && h[11] == 'E') return "wav";
        if (h[0] == 'I' && h[1] == 'D' && h[2] == '3') return "mp3";
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xE0) == 0xE0 && (h[1] & 0x06) != 0) return "mp3";   // an MPEG audio frame (layer bits not 00)
        return "";
    }

    /** the file name ends with one of ours (the USB list shows only these) */
    static boolean listed(String fileName) {
        if (fileName == null) return false;
        String n = fileName.toLowerCase(java.util.Locale.ROOT);
        if (n.startsWith(".")) return false;   // hidden files (macOS leaves ._ copies on sticks)
        for (String e : EXTENSIONS) if (n.endsWith("." + e)) return true;
        return false;
    }

    /** "" when a file of this size may be added beside `used` bytes in `count` files; else "empty", "too_big", "full" or "too_many" */
    static String refuse(long size, long used, int count, boolean big) {
        if (size <= 0) return "empty";
        if (size > fileMax(big)) return "too_big";
        if (count >= MAX_FILES) return "too_many";
        if (used + size > totalMax(big)) return "full";
        return "";
    }

    /** turning «سعة أكبر» off is refused while what is kept is already over the usual caps (nothing is deleted for it) */
    static boolean mayShrink(long used, long largest) { return used <= TOTAL_MAX && largest <= FILE_MAX; }

    /** the name it is shown by: the file's name without its folder and its extension, no control characters, at most 40 letters */
    static String name(String fileName) {
        String n = fileName == null ? "" : fileName;
        int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        if (slash >= 0) n = n.substring(slash + 1);
        int dot = n.lastIndexOf('.');
        if (dot >= 0) n = n.substring(0, dot);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            char ch = n.charAt(i);
            if (Character.isISOControl(ch) || ch == '‮' || ch == '‭') continue;   // no control or direction-override characters
            b.append(ch == '_' ? ' ' : ch);
        }
        String out = b.toString().trim().replaceAll("\\s+", " ");
        if (out.length() > 40) out = out.substring(0, 40).trim();
        return out;   // "" when nothing is left: the caller names it (AudioStore: «صوت»)
    }

    /** a piece of a phone upload is the next one: it starts where the last ended and stays inside the size given at the start */
    static boolean nextChunk(long have, long offset, int length, long total) {
        return offset == have && length > 0 && length <= CHUNK && offset + length <= total;
    }

    /** "12.4 MB", "340 KB" with Arabic digits as the screens say sizes */
    static String size(long bytes) {
        if (bytes >= MB) return (Math.round(bytes * 10.0 / MB) / 10.0) + " MB";
        return Math.max(1, (bytes + 1023) / 1024) + " KB";
    }

    /** 4.4: another upload is still coming: it sent a piece in the last BUSY_MS */
    static final long BUSY_MS = 30_000L;

    static boolean busy(long total, long lastPieceAt, long now) { return total > 0 && now - lastPieceAt < BUSY_MS; }

    /** 4.4: a file of the library's folder that is a leftover (an unfinished phone upload, a copy that was not kept), never a kept sound */
    static boolean leftover(String name) {
        return name != null && (name.equals("phone.part") || name.matches("phone-[0-9]+\\.done") || name.matches("in-[0-9]+\\.tmp"));
    }
}
