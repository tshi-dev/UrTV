package sa.urtv.home;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/**
 * 4.3: «اللغة» for our other app (UrTV Player, com.urtv.player, signed with another key) so it can follow it. content://sa.urtv.home.lang/lang
 * gives one row with one column, «lang»: "ar" or "en" (S.lang, the screensaver's setting as this app keeps it). Nothing else: no other column,
 * no other path, no write. Behind the normal-level permission sa.urtv.permission.READ_LANG, declared here; the player asks for it.
 */
public final class LangProvider extends ContentProvider {
    public static final String AUTHORITY = "sa.urtv.home.lang", PATH = "lang", COLUMN = "lang";

    @Override public boolean onCreate() { return true; }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (!LangRule.isLangUri(uri == null ? null : uri.getAuthority(), uri == null ? null : uri.getPath())) return null;
        S.init(getContext());
        MatrixCursor c = new MatrixCursor(new String[] {COLUMN}, 1);
        c.addRow(new Object[] {LangRule.code(S.lang())});
        return c;
    }

    @Override public String getType(Uri uri) { return null; }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("read only"); }

    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException("read only"); }

    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException("read only"); }
}
