package sa.urtv.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.security.MessageDigest;

/**
 * 4.3 «تطبيق المباريات» and «شاهد المباراة». The setting (the screensaver's Prefs.matchApp, through ConfigSnapshot): "auto" = UrTV Player when it is
 * installed and signed with its pinned certificate (SuggestedApps.PLAYER_CERT), "none", or any app's package. UrTV Player is told which game with
 * an explicit intent, the contract below; any other app is only opened. We send no channel, no stream and no link: the Player searches the
 * owner's own lists.
 *
 * The contract (to com.urtv.player only, explicit, never broadcast):
 *   action  sa.urtv.player.action.FIND_MATCH
 *   extras  home_ar, home_en, away_ar, away_en, competition_ar, competition_en (String; "" when unknown), kickoff (long, epoch ms),
 *           channels (String[]; the broadcaster's channel names we know, else empty)
 *   sent    with startActivityForResult from an Activity of ours (sa.urtv.home), no FLAG_ACTIVITY_NEW_TASK, never from a service or the
 *           dream: the Player knows its caller by getCallingPackage(), which only a for-result start from an activity gives
 *   result  RESULT_OK something played, RESULT_CANCELED nothing (the Player says so itself: nothing for us to show)
 */
final class MatchApp {
    static final String ACTION = "sa.urtv.player.action.FIND_MATCH";
    /** the request code of the Player's start (its result needs nothing from us) */
    static final int REQUEST = 0x4d41;

    private MatchApp() { }

    /** the package «شاهد المباراة» goes to now, or null (none chosen, not installed, or a Player that is not ours) */
    static String chosen(Context c, String setting) {
        PackageManager pm = c.getPackageManager();
        String v = setting == null ? "auto" : setting;
        if ("none".equals(v)) return null;
        String pkg = "auto".equals(v) ? SuggestedApps.PLAYER : v;
        if (SuggestedApps.PLAYER.equals(pkg)) return playerOk(pm) ? pkg : null;
        if (!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) return null;
        return pm.getLeanbackLaunchIntentForPackage(pkg) != null || pm.getLaunchIntentForPackage(pkg) != null ? pkg : null;
    }

    /** UrTV Player is installed, signed by exactly its pinned certificate, and takes FIND_MATCH */
    static boolean playerOk(PackageManager pm) {
        try {
            int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
            PackageInfo info = pm.getPackageInfo(SuggestedApps.PLAYER, flags);
            Signature[] sigs = Build.VERSION.SDK_INT >= 28 && info.signingInfo != null
                ? (info.signingInfo.hasMultipleSigners() ? info.signingInfo.getApkContentsSigners() : info.signingInfo.getSigningCertificateHistory()) : info.signatures;
            if (sigs == null || sigs.length != 1) return false;
            String sha = SuggestedApps.hex(MessageDigest.getInstance("SHA-256").digest(sigs[0].toByteArray()));
            if (!sha.equalsIgnoreCase(SuggestedApps.PLAYER_CERT)) return false;
            return pm.resolveActivity(new Intent(ACTION).setPackage(SuggestedApps.PLAYER), 0) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * «شاهد المباراة», from the activity on screen (on its UI thread): the Player with the game (the checked facts), for a result, so it sees
     * who asks; another app just opened, as before. False when nothing could be started.
     */
    static boolean open(Activity c, String setting, MatchFacts f) {
        String pkg = chosen(c, setting);
        if (pkg == null || f == null) return false;
        Intent i;
        if (SuggestedApps.PLAYER.equals(pkg)) {
            i = new Intent(ACTION).setPackage(pkg);
            for (int k = 0; k < MatchFacts.TEXTS.length; k++) i.putExtra(MatchFacts.TEXTS[k], f.texts[k]);
            i.putExtra("kickoff", f.kickoff);
            i.putExtra("channels", f.channels.toArray(new String[0]));
        } else {
            PackageManager pm = c.getPackageManager();
            i = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = pm.getLaunchIntentForPackage(pkg);
            if (i == null) return false;
        }
        try {
            if (SuggestedApps.PLAYER.equals(pkg)) c.startActivityForResult(i, REQUEST);   // no NEW_TASK: that would drop the caller
            else c.startActivity(i);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
