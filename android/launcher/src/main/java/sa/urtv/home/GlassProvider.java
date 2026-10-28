package sa.urtv.home;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * The glass backdrop for the screensaver app's screens (its settings): read only, and only by an app holding
 * sa.urtv.home.READ_GLASS, a signature permission (our two apps share the key): no other app can read the home's photo.
 * content://sa.urtv.home.glass/backdrop: a query gives stamp (0: none yet), the glass look's two veils, the look the home shows and the
 * backdrop's bounds (any look's veil comes from them: GlassStyle.scrimAlpha);
 * opening it gives the small PNG (GlassSource.FILE).
 */
public final class GlassProvider extends ContentProvider {
    static final String AUTHORITY = "sa.urtv.home.glass";

    @Override public boolean onCreate() { return true; }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (!"/backdrop".equals(uri.getPath())) return null;
        File f = GlassSource.file(getContext());
        MatrixCursor c = new MatrixCursor(new String[] {"stamp", "scrim_dark", "scrim_light", "home_light", "glass_hi", "glass_lo"});
        int[] bounds = GlassSource.bounds(getContext());
        c.addRow(new Object[] {f.isFile() ? f.lastModified() : 0L,   // the glass look's two veils first, for a settings app from before «الشكل»
                GlassStyle.scrimAlpha(bounds[0], bounds[1], GlassStyle.DARK), GlassStyle.scrimAlpha(bounds[0], bounds[1], GlassStyle.LIGHT),
                Dock.prefs(getContext()).contains("glass_home_light") ? (Dock.prefs(getContext()).getBoolean("glass_home_light", false) ? 1 : 0) : -1,
                bounds[0], bounds[1]});
        return c;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"/backdrop".equals(uri.getPath()) || !"r".equals(mode)) throw new FileNotFoundException(uri.toString());
        File f = GlassSource.file(getContext());
        if (!f.isFile()) throw new FileNotFoundException("no backdrop yet");
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public String getType(Uri uri) { return "image/png"; }
    @Override public Uri insert(Uri uri, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
