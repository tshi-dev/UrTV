package sa.urtv.home.phone;

/** For tests/phone_qr_test.py: prints the modules of the QR code for each text given, as lines of 0 and 1 after a "version size" line. A text
 * that starts with "L:" is encoded with encodeLarge (level L, up to version 40), without those two characters. */
public final class QrDumpRun {
    public static void main(String[] args) throws Exception {
        java.io.PrintStream out = new java.io.PrintStream(System.out, true, "UTF-8");
        for (String t : args) {
            QrCode q = t.startsWith("L:") ? QrCode.encodeLarge(t.substring(2)) : QrCode.encode(t);
            out.println(q.version + " " + q.size);
            for (int y = 0; y < q.size; y++) {
                StringBuilder b = new StringBuilder();
                for (int x = 0; x < q.size; x++) b.append(q.dark(x, y) ? '1' : '0');
                out.println(b);
            }
        }
    }
}
