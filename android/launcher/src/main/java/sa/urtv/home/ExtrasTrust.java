package sa.urtv.home;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;

/**
 * 4.4 beta 4 (review F88, security): every call into the add-on's provider goes through here. The authority {@link ExtrasWords#EXTRAS_AUTHORITY} carries no permission on the
 * home's side, so ANY installed app could declare it, answer «available» and receive the pairing code the person types on the TV and the shell commands the setup sends.
 * The add-on checks its caller (ExtrasCallProvider, SignDigest.callerIs); this checks the other way: the app that owns the authority must be «sa.urtv.extras» and signed
 * with the certificate this home is signed with, else the call is not made (null: «no add-on»).
 */
public final class ExtrasTrust {
    private ExtrasTrust() { }

    /** the rule alone (no Android): the owner of the authority is the add-on's package, with a certificate, and it is OURS */
    public static boolean trusted(String providerPackage, String providerDigest, String ownDigest) {
        return ExtrasWords.PACKAGE.equals(providerPackage) && providerDigest != null && !providerDigest.isEmpty() && providerDigest.equals(ownDigest);
    }

    /** the app that answers the add-on's authority now is the add-on, signed as this home is */
    public static boolean ok(Context c) {
        try {
            ProviderInfo pi = c.getPackageManager().resolveContentProvider(ExtrasWords.EXTRAS_AUTHORITY, 0);
            String pkg = pi == null ? null : pi.packageName;
            return trusted(pkg, pkg == null ? null : SignDigest.of(c, pkg), SignDigest.of(c, c.getPackageName()));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** one call into the add-on's provider, only when its owner is trusted; null when it is not installed, not trusted, or does not answer */
    public static Bundle call(Context c, String method, String arg, Bundle extras) {
        try {
            if (!ok(c)) return null;
            return c.getContentResolver().call(Uri.parse("content://" + ExtrasWords.EXTRAS_AUTHORITY), method, arg, extras);
        } catch (RuntimeException e) {
            return null;   // not installed (unknown authority) or the add-on refused
        }
    }
}
