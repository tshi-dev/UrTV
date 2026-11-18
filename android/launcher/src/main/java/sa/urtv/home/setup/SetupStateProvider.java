package sa.urtv.home.setup;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/**
 * 4.3.3: what the computer's home switch (optimize/install.sh launcher) asks before it turns back on what an older switch turned off:
 * content://sa.urtv.home.setupstate/factory, one row {off: 1|0}, 1 while «تخلّص من واجهة المصنع» is on (the person's own choice: the stock
 * home stays off then). Read only; DUMP in the manifest = only the shell (adb) may ask. Nothing here changes anything.
 */
public class SetupStateProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        MatrixCursor c = new MatrixCursor(new String[]{"off"});
        if (getContext() != null && uri != null && "factory".equals(uri.getLastPathSegment())) c.addRow(new Object[]{SetupHook.factoryOff(getContext()) ? 1 : 0});
        return c;
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
