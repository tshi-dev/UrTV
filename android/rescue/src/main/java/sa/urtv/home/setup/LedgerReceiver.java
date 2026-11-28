package sa.urtv.home.setup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The rescue app keeps a copy of the home's ledger (LedgerKeep): sent by the home on the signature permission sa.urtv.permission.EXTRAS (the manifest only lets a holder of it
 * broadcast here), written to the rescue's own file, used by «إنقاذ» when «تلفزيونك» does not answer (after a reinstall that wiped its data).
 */
public final class LedgerReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (i == null || !LedgerKeep.ACTION.equals(i.getAction())) return;
        try {
            String text = LedgerKeep.keep(read(c), i.getStringExtra(LedgerKeep.EXTRA));
            File dir = c.getFilesDir(), tmp = new File(dir, LedgerKeep.FILE + ".tmp");
            try (FileOutputStream o = new FileOutputStream(tmp)) { o.write(text.getBytes(StandardCharsets.UTF_8)); }
            if (!tmp.renameTo(new File(dir, LedgerKeep.FILE))) tmp.delete();
        } catch (IOException | RuntimeException ignored) { /* the kept copy stays */ }
    }

    /** the kept copy (empty when there is none) */
    public static String read(Context c) {
        File f = new File(c.getFilesDir(), LedgerKeep.FILE);
        if (!f.isFile() || f.length() > 64 * 1024) return "";
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            byte[] b = new byte[(int) f.length()];
            int n = 0;
            while (n < b.length) { int r = in.read(b, n, b.length - n); if (r < 0) break; n += r; }
            return new String(b, 0, n, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
