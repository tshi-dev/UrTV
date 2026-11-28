package sa.urtv.home.setup;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import java.util.List;

/**
 * The home's side of the ledger copy (LedgerKeep): at each start of the home, on a thread of its own, the ledger of what the setup turned off (disabled-by-us.txt and
 * community-applied.txt) is sent to the rescue app when it changed since the last send and the rescue app is installed. Only the rescue app (it holds the same signature
 * permission) is heard; nothing else is shared.
 */
public final class LedgerPush {
    private LedgerPush() { }

    private static final String PREFS = "ledger_push", KEY = "last";

    public static void maybe(Context c) {
        final Context app = c.getApplicationContext();
        Thread t = new Thread(() -> {
            try {
                FileStore store = new FileStore(app);
                List<String> names = SetupEngine.ledgerNames(store.get("disabled-by-us.txt"));
                for (String p : SetupEngine.ledgerNames(store.get("community-applied.txt"))) if (!names.contains(p)) names.add(p);
                String now = LedgerKeep.clean(String.join("\n", names));
                SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                if (!LedgerKeep.worthPushing(sp.getString(KEY, ""), now)) return;
                try { app.getPackageManager().getPackageInfo(LedgerKeep.RESCUE_PACKAGE, 0); } catch (PackageManager.NameNotFoundException e) { return; }   // no rescue app: asked again at the next start
                app.sendBroadcast(new Intent(LedgerKeep.ACTION).setComponent(new ComponentName(LedgerKeep.RESCUE_PACKAGE, LedgerKeep.RECEIVER)).putExtra(LedgerKeep.EXTRA, now), LedgerKeep.PERMISSION);
                sp.edit().putString(KEY, now).apply();
            } catch (Exception ignored) { /* the copy is a help, never a failure of the home */ }
        }, "ledger-push");
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();
    }
}
