package sa.urtv.home.setup;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import java.util.List;

/**
 * 4.3.3 «إنقاذ تلفزيونك» (the rescue app, android/rescue): answers what our setup turned off (the ledger disabled-by-us.txt and the community list's
 * community-applied.txt: package names, one per line), so the rescue can turn those back on. Only an app signed with our key can ask
 * (the signature permission sa.urtv.permission.RESCUE); nothing else is shared, nothing is written.
 */
public final class RescueLedger extends ContentProvider {
    // the words the 4.3.3 rescue app (the one in apk-latest) asks with; the 4.4 rescue app asks the home's extras bridge instead (ExtrasWords.LEDGER)
    private static final String CALL = "ledger", RESULT = "result";

    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        if (!CALL.equals(method) || getContext() == null) return null;
        FileStore store = new FileStore(getContext());
        List<String> names = SetupEngine.ledgerNames(store.get("disabled-by-us.txt"));
        for (String p : SetupEngine.ledgerNames(store.get("community-applied.txt"))) if (!names.contains(p)) names.add(p);
        Bundle b = new Bundle();
        b.putString(RESULT, String.join("\n", names));
        return b;
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
