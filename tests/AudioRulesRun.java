package sa.urtv.home.tilawah;

/** tests/audio_library_test.py runs this: one line per case, «name<TAB>ok|bad». */
public class AudioRulesRun {
    static void out(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "bad")); }
    static byte[] b(int... v) { byte[] r = new byte[v.length]; for (int i = 0; i < v.length; i++) r[i] = (byte) v[i]; return r; }

    public static void main(String[] a) {
        long MB = AudioRules.MB;
        out("the caps: 20 MB a file and 100 MB in all; «سعة أكبر» 50 and 500", AudioRules.fileMax(false) == 20 * MB && AudioRules.totalMax(false) == 100 * MB
                && AudioRules.fileMax(true) == 50 * MB && AudioRules.totalMax(true) == 500 * MB);
        out("a file of 20 MB comes in, one byte more does not", AudioRules.refuse(20 * MB, 0, 0, false).isEmpty() && AudioRules.refuse(20 * MB + 1, 0, 0, false).equals("too_big"));
        out("with «سعة أكبر» a file of 50 MB comes in", AudioRules.refuse(50 * MB, 0, 0, true).isEmpty() && AudioRules.refuse(50 * MB + 1, 0, 0, true).equals("too_big"));
        out("all together stay under the total", AudioRules.refuse(10 * MB, 95 * MB, 5, false).equals("full") && AudioRules.refuse(5 * MB, 95 * MB, 5, false).isEmpty());
        out("an empty file is refused", AudioRules.refuse(0, 0, 0, false).equals("empty"));
        out("at most 60 files", AudioRules.refuse(MB, 0, 60, true).equals("too_many") && AudioRules.refuse(MB, 0, 59, true).isEmpty());
        out("«سعة أكبر» off only while what is kept fits the usual caps", AudioRules.mayShrink(100 * MB, 20 * MB) && !AudioRules.mayShrink(101 * MB, MB) && !AudioRules.mayShrink(MB, 21 * MB));

        out("mp3 by its ID3 tag", AudioRules.kind(b('I', 'D', '3', 4, 0, 0, 0, 0, 0, 0, 0, 0), 12).equals("mp3"));
        out("mp3 by an MPEG frame", AudioRules.kind(b(0xFF, 0xFB, 0x90, 0x64, 0, 0, 0, 0, 0, 0, 0, 0), 12).equals("mp3"));
        out("m4a by its ftyp box", AudioRules.kind(b(0, 0, 0, 0x20, 'f', 't', 'y', 'p', 'M', '4', 'A', ' '), 12).equals("m4a"));
        out("ogg", AudioRules.kind(b('O', 'g', 'g', 'S', 0, 2, 0, 0, 0, 0, 0, 0), 12).equals("ogg"));
        out("wav", AudioRules.kind(b('R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'A', 'V', 'E'), 12).equals("wav"));
        out("a JPEG, a ZIP, a text named .mp3 are not sounds", AudioRules.kind(b(0xFF, 0xD8, 0xFF, 0xE0, 0, 0, 0, 0, 0, 0, 0, 0), 12).isEmpty()
                && AudioRules.kind(b('P', 'K', 3, 4, 0, 0, 0, 0, 0, 0, 0, 0), 12).isEmpty() && AudioRules.kind("hello world!".getBytes(), 12).isEmpty());
        out("too few bytes to tell", AudioRules.kind(b('I', 'D'), 2).isEmpty() && AudioRules.kind(null, 0).isEmpty());

        out("the stick lists mp3, m4a, ogg and wav only, in any case", AudioRules.listed("دعاء.MP3") && AudioRules.listed("a.m4a") && AudioRules.listed("b.Ogg") && AudioRules.listed("c.wav")
                && !AudioRules.listed("photo.jpg") && !AudioRules.listed("x.mp3.exe") && !AudioRules.listed(null));
        out("hidden files (._ copies of a Mac) are not listed", !AudioRules.listed("._دعاء.mp3") && !AudioRules.listed(".hidden.mp3"));

        out("the shown name: no folder, no extension", AudioRules.name("/storage/AB12/أدعية/دعاء الصباح.mp3").equals("دعاء الصباح"));
        out("underscores read as spaces, spaces folded", AudioRules.name("surat_al__kahf.m4a").equals("surat al kahf"));
        out("no control or direction-override characters", AudioRules.name("a‮bc\u0007.mp3").equals("abc"));
        StringBuilder longName = new StringBuilder(); for (int i = 0; i < 60; i++) longName.append('س');
        out("at most 40 letters", AudioRules.name(longName + ".mp3").length() == 40);
        out("nothing left: empty (the store names it)", AudioRules.name(".mp3").isEmpty() && AudioRules.name("___.wav").isEmpty());

        out("pieces come in order and inside the size", AudioRules.nextChunk(0, 0, AudioRules.CHUNK, 10 * MB) && AudioRules.nextChunk(AudioRules.CHUNK, AudioRules.CHUNK, 100, 10 * MB));
        out("a piece out of order, too big, empty or past the end is refused", !AudioRules.nextChunk(0, 10, 100, MB) && !AudioRules.nextChunk(0, 0, AudioRules.CHUNK + 1, 10 * MB)
                && !AudioRules.nextChunk(0, 0, 0, MB) && !AudioRules.nextChunk(MB - 10, MB - 10, 20, MB));
        out("a piece is 512 KB", AudioRules.CHUNK == 512 * 1024);
    }
}
