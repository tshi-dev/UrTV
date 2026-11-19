package sa.urtv.home;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Binder;
import android.os.Build;

import java.security.MessageDigest;

/**
 * 4.4 beta 3, the paired route's caller checks (PairGuard): the SHA-256 of a package's signing certificate, and the packages of the app calling us. Shared by the home
 * and the add-on (android/extras/build.gradle, def shared). Null when it cannot be read: the caller is then refused.
 */
public final class SignDigest {
    private SignDigest() { }

    /** the packages of the uid that is calling this provider right now */
    public static String[] callingPackages(Context c) {
        try { return c.getPackageManager().getPackagesForUid(Binder.getCallingUid()); } catch (RuntimeException e) { return null; }
    }

    /** SHA-256 (hex) of the package's one signing certificate; null when it has none, several, or cannot be read */
    @SuppressWarnings("deprecation")
    public static String of(Context c, String pkg) {
        try {
            Signature[] sigs;
            if (Build.VERSION.SDK_INT >= 28) {
                PackageInfo pi = c.getPackageManager().getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES);
                sigs = pi.signingInfo == null || pi.signingInfo.hasMultipleSigners() ? null : pi.signingInfo.getSigningCertificateHistory();
                if (sigs != null && sigs.length > 0) sigs = new Signature[] {sigs[sigs.length - 1]};   // the current certificate
            } else {
                sigs = c.getPackageManager().getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures;
            }
            if (sigs == null || sigs.length != 1) return null;
            byte[] d = MessageDigest.getInstance("SHA-256").digest(sigs[0].toByteArray());
            StringBuilder b = new StringBuilder();
            for (byte x : d) b.append(String.format(java.util.Locale.ROOT, "%02x", x & 0xff));
            return b.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** the app calling this provider is {@code pkg} (its uid's only package) and signed with OUR certificate */
    public static boolean callerIs(Context c, String pkg) {
        String[] pkgs = callingPackages(c);
        return PairGuard.callerIs(pkgs, pkg, pkgs != null && pkgs.length == 1 ? of(c, pkgs[0]) : null, of(c, c.getPackageName()));
    }
}
