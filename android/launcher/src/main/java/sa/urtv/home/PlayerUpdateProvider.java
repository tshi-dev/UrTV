package sa.urtv.home;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;

/**
 * 4.3 UrTV Player asks whether a newer Player is published, so it can offer «تحديث» from inside (the owner never uninstalls or reinstalls).
 * content://sa.urtv.home.player/update, read only, behind the normal permission sa.urtv.permission.READ_LANG (as LangProvider), and answered
 * ONLY when the caller is com.urtv.player signed by exactly its pinned certificate (PlayerUpdate.callerOk); anyone else gets null.
 * One row {version_code INTEGER, version_name TEXT, notes TEXT} when the newest «player-<version>» release of our repository that passed the
 * mirror checks says, in its urtv-player.version, a versionCode newer than the installed Player's; no row when nothing newer is known or nothing
 * is verified. The answer is the cached one (MirrorInstall); a cache older than 6 h is refreshed in the background for next time, never waited for.
 */
public final class PlayerUpdateProvider extends ContentProvider {
    public static final String AUTHORITY = "sa.urtv.home.player", PATH = "update";
    public static final String[] COLUMNS = {"version_code", "version_name", "notes"};

    @Override public boolean onCreate() { return true; }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (uri == null || !AUTHORITY.equals(uri.getAuthority()) || !("/" + PATH).equals(uri.getPath())) return null;
        String caller = getCallingPackage();
        long id = Binder.clearCallingIdentity();   // our own package manager and preferences from here
        try {
            if (!PlayerUpdate.callerOk(caller, MirrorInstall.signers(getContext().getPackageManager(), caller))) return null;
            MirrorInstall.refreshAsync(getContext(), null);
            MatrixCursor c = new MatrixCursor(COLUMNS, 1);
            PlayerUpdate.Version v = MirrorInstall.known(getContext());
            if (v != null) c.addRow(new Object[] {v.code, v.name, v.notes});
            return c;
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    @Override public String getType(Uri uri) { return null; }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("read only"); }

    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException("read only"); }

    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException("read only"); }
}
