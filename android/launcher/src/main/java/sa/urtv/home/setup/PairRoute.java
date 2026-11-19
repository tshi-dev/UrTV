package sa.urtv.home.setup;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import sa.urtv.home.ExtrasWords;

/**
 * 4.4 beta 3, the ONE place the home's setup reaches wireless debugging. The libraries (libadb-android, Conscrypt, BouncyCastle, the LGPL SPAKE2) live in the ADD-ON only
 * (the owner's rule: never in the home or the screensaver), so this only calls «إضافات تلفزيونك» (ExtrasCallProvider, the signature permission): the pairing state,
 * then a connection over its TLS, one shell command or one install at a time. Every call blocks the caller's thread: the setup's own threads only, never a UI thread.
 * Fails closed: no add-on, an old Android, no pairing or wireless debugging off each come back as a short word ({@link #why}), never as a silent nothing.
 */
final class PairRoute {
    private PairRoute() { }

    private static final Uri ADDON = Uri.parse("content://" + ExtrasWords.EXTRAS_AUTHORITY);

    private static Bundle call(Context c, String method, String arg, Bundle extras) {
        try { return c.getContentResolver().call(ADDON, method, arg, extras); }
        catch (RuntimeException e) { return null; }   // the add-on is not installed (unknown authority) or not ours
    }

    /**
     * Where the paired route stands: "paired" (a connection may be tried), "no_addon" (the add-on is not installed or does not answer), "old_addon" (an add-on
     * without the pairing), "old_android" (no wireless debugging before Android 11), "not_paired".
     */
    static String why(Context c) {
        if (Build.VERSION.SDK_INT < 30) return "old_android";
        Bundle b = call(c, ExtrasWords.PAIR_STATUS, null, null);
        if (b == null) return "no_addon";
        if (!b.containsKey(ExtrasWords.AVAILABLE)) return "old_addon";
        if (!b.getBoolean(ExtrasWords.AVAILABLE)) return "old_android";
        return b.getBoolean(ExtrasWords.PAIRED) ? "paired" : "not_paired";
    }

    static boolean paired(Context c) { return "paired".equals(why(c)); }

    /** the factory page's word: "off", "armed", "pairing", "paired" or "failed"; "" when the add-on did not answer */
    static String state(Context c) {
        Bundle b = call(c, ExtrasWords.PAIR_STATUS, null, null);
        return b == null ? "" : b.getString(ExtrasWords.STATE, "");
    }

    /** why the last pairing failed ("no_dialog", "wrong", "failed") */
    static String failure(Context c) {
        Bundle b = call(c, ExtrasWords.PAIR_STATUS, null, null);
        return b == null ? "failed" : b.getString(ExtrasWords.WHY, "failed");
    }

    static boolean arm(Context c) { return call(c, ExtrasWords.PAIR_ARM, null, null) != null; }

    static void disarm(Context c) { call(c, ExtrasWords.PAIR_DISARM, null, null); }

    static void retry(Context c) { call(c, ExtrasWords.PAIR_RETRY, null, null); }

    /** the phone page's six digits, handed on (never logged): "ok", "not_armed", "busy", "bad_code" or "no_addon" */
    static String submit(Context c, String typed) {
        Bundle b = call(c, ExtrasWords.PAIR_CODE, typed, null);
        return b == null ? "no_addon" : b.getString(ExtrasWords.RESULT, "no_addon");
    }

    /** the add-on's pairing key goes with ours (SetupSession.dropKey) */
    static void forget(Context c) { call(c, ExtrasWords.PAIR_FORGET, null, null); }

    /** a session over the add-on's TLS connection; throws with the add-on's short word ("no_port": wireless debugging is off, "not_paired", "refused", "no_addon") */
    static SetupSession open(final Context c, SetupEngine.Out out) throws IOException {
        Bundle b = call(c, ExtrasWords.PAIR_OPEN, null, null);
        if (b == null) throw new IOException("no_addon");
        final String id = b.getString(ExtrasWords.RESULT);
        if (id == null) throw new IOException(b.getString(ExtrasWords.ERROR, "refused"));
        SetupEngine.Shell shell = command -> {
            if (!sa.urtv.home.PairGuard.allowed(command)) {   // never a generic shell, even for our own code: only the setup's own commands
                android.util.Log.w("UrTVFactory", "paired route: a command outside the setup's own was refused");
                throw new IOException("not_allowed");
            }
            Bundle in = new Bundle();
            in.putString(ExtrasWords.PAIR_ID, id);
            in.putString(ExtrasWords.COMMAND, command);
            Bundle r = call(c, ExtrasWords.PAIR_RUN, null, in);
            String text = r == null ? null : r.getString(ExtrasWords.RESULT);
            if (text == null) throw new IOException("the paired connection failed");
            return AdbClient.withCode(text);
        };
        SetupSession.Exec exec = (command, data, size) -> {
            if (!sa.urtv.home.PairGuard.allowed(command)) throw new IOException("not_allowed");
            String token = PairFiles.put(c, data, size);
            try {
                Bundle in = new Bundle();
                in.putString(ExtrasWords.PAIR_ID, id);
                in.putString(ExtrasWords.COMMAND, command);
                in.putString(ExtrasWords.URI, PairFiles.uri(token).toString());
                in.putLong(ExtrasWords.SIZE, size);
                Bundle r = call(c, ExtrasWords.PAIR_EXEC, null, in);
                String text = r == null ? null : r.getString(ExtrasWords.RESULT);
                if (text == null) throw new IOException("the paired install failed");
                return text;
            } finally {
                PairFiles.drop(c, token);
            }
        };
        android.util.Log.i("UrTVFactory", "paired route: open");
        return SetupSession.wrap(c, shell, exec, () -> {
            Bundle in = new Bundle();
            in.putString(ExtrasWords.PAIR_ID, id);
            call(c, ExtrasWords.PAIR_CLOSE, null, in);
        }, out);
    }
}
