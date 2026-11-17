package sa.urtv.home;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import sa.urtv.home.phone.PhoneUpdater;
import sa.urtv.home.phone.UpdateChannel;
import sa.urtv.home.phone.UpdateRules;

/**
 * The home says on the TV that an update is worth opening «تحديث تلفزيوني» for (4.3; 4.4: always on, all three apps): the home, the screensaver app or «إضافات تلفزيونك»
 * (where it is installed) is older than another of them or than the newest build of the person's channel (version.txt of apk-latest, or apk-next for a tester), read at
 * start and at most every 6 hours, off the main thread, only its first 64 bytes, quietly when offline, never while our recitation or the adhan plays. Two ways say it: the
 * «تحديث» pill in the top row (while anything is behind), and once the card «في تحديث لـ …» (UpdateNoticeRule.cardDue: again only for a newer build or after 3 days); OK
 * opens the update screen, which does everything (nothing is installed from here). The rules are plain Java (tests/UpdateNoticeRun.java).
 */
final class UpdateNotice {
    private UpdateNotice() { }

    static final String PREFS = "update_notice";
    static final String SAVER = "sa.urtv.screensaver";

    /** the card: its line and the build it asks for */
    static final class Card {
        final String line;
        final long target;
        Card(String line, long target) { this.line = line; this.target = target; }
    }

    private static long code(Context c, String pkg) { long k = PhoneUpdater.installedCode(c, pkg); return k < 0 ? 0 : k; }

    private static String channel(Context c) {
        return UpdateChannel.cached();   // 4.3.3 hotfix: this runs on the UI thread (the pill, the card, the minute tick): the kept word, never a query to the screensaver
    }

    private static int newest(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("newest_" + channel(c), 0); }

    private static int behindNow(Context c) {
        return UpdateNoticeRule.behind(code(c, c.getPackageName()), code(c, SAVER), code(c, ExtrasWords.PACKAGE), newest(c));
    }

    static boolean wanted(Context c) { return behindNow(c) != 0; }

    /** «الشاشة الرئيسية وشاشة التوقف»: the apps that are behind, in the person's language */
    static String names(int behind) {
        List<String> n = new ArrayList<>();
        if ((behind & UpdateNoticeRule.HOME) != 0) n.add(S.t("الشاشة الرئيسية"));
        if ((behind & UpdateNoticeRule.SAVER) != 0) n.add(S.t("شاشة التوقف"));
        if ((behind & UpdateNoticeRule.EXTRAS) != 0) n.add(S.t("إضافات تلفزيونك"));
        String out = n.isEmpty() ? "" : n.get(0);
        for (int i = 1; i < n.size(); i++) out = i == n.size() - 1 ? S.t("%s و%s", out, n.get(i)) : S.t("%s، %s", out, n.get(i));
        return out;
    }

    /** the card due now, or null; {@code onScreen}: the target of the card this visit shows already (-1: none) */
    static Card card(Context c, long onScreen, long now) {
        int b = behindNow(c);
        long t = UpdateNoticeRule.target(code(c, c.getPackageName()), code(c, SAVER), code(c, ExtrasWords.PACKAGE), newest(c));
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!UpdateNoticeRule.cardDue(b, t, sp.getLong("card_target", 0), sp.getLong("card_at", 0), onScreen, now)) return null;
        return new Card(S.t("في تحديث لـ %s", names(b)), t);
    }

    /** the card appeared: it is not shown again for this build until 3 days pass */
    static void shown(Context c, long target, long now) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("card_target", target).putLong("card_at", now).apply();
    }

    /** reads version.txt of the person's channel when one is due (off the main thread; {@code done} runs on that thread after a good read). Offline: nothing said. */
    static void checkIfDue(final Context app, final Runnable done) {
        final SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final String ch = channel(app);
        if (!UpdateNoticeRule.due(sp.getLong("at_" + ch, 0), System.currentTimeMillis())) return;   // per channel: switching to the beta asks at once, not in 6 hours
        if (sa.urtv.home.tilawah.MusicService.carryOurPlaying() || sa.urtv.home.tilawah.NowPlaying.adhanLive()) return;   // never during playback or the adhan: the next start asks
        sp.edit().putLong("at_" + ch, System.currentTimeMillis()).apply();   // asked: the next try is in 6 hours, a failure included
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Net.Reply r = Net.get(UpdateRules.baseFor(ch) + "version.txt", null, 10000, 12000, 64);   // IPv4 first, 4 s per address, 12 s in all (Net)
                    if (r.status != 200) return;
                    int build = UpdateRules.buildOf(r.text());
                    if (build > 0) {
                        sp.edit().putInt("newest_" + ch, build).apply();
                        if (done != null) done.run();
                    }
                } catch (java.io.IOException | RuntimeException ignored) {
                    // offline or the release did not answer: quietly, the next try in 6 hours
                }
            }
        }, "update-notice").start();
    }
}
