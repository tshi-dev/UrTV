package sa.urtv.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import sa.urtv.home.phone.PhoneUpdater;
import sa.urtv.home.phone.UpdateChannel;

/**
 * 4.3.3, «النسخ التجريبية»: while the beta channel is on, the home asks ONCE for a note, and only in two moments: after a beta update («انت على النسخة
 * التجريبية…») and after the app died and restarted («صار خطأ، تبي ترسل تقرير؟»). Both buttons open «أرسل تقرير» (ReportActivity: its few allowed lines, the QR
 * code, nothing is sent from the TV). The crash is remembered as a class name only (the same words Report.name allows); nothing else of it is kept. Off, none
 * of this shows (the crash note is dropped at the next start).
 */
final class BetaNotes {
    private BetaNotes() { }

    static final String PREFS = "beta_notes";

    /** the process of the home died on an exception: its class name is kept (apply may be lost with the process, so commit), then the old handler does what it did */
    static void hookCrashes(final Context app) {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("crash", Report.name(e.getClass().getName())).commit();
            } catch (Throwable ignored) { }
            if (prev != null) prev.uncaughtException(t, e);
        });
    }

    /** from MainActivity.onResume: one question at most, once */
    static void maybeAsk(final Activity a) {
        try {
            SharedPreferences sp = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String crash = sp.getString("crash", null);
            boolean beta = UpdateChannel.beta(a);
            long code = PhoneUpdater.installedCode(a, a.getPackageName());
            String what = BetaNotesRule.what(beta, crash, sp.getLong("seen", 0), code);
            SharedPreferences.Editor e = sp.edit().remove("crash");   // asked, or dropped (off): never twice
            if (beta) e.putLong("seen", code);
            e.apply();
            if (what.isEmpty() || a.isFinishing()) return;
            final boolean crashed = "crash".equals(what);
            final String error = crash;
            new AlertDialog.Builder(a)
                .setMessage(S.t(crashed ? "صار خطأ، تبي ترسل تقرير؟" : "انت على النسخة التجريبية، إذا شفت شي غريب أرسل ملاحظة"))
                .setPositiveButton(S.t(crashed ? "أرسل تقرير" : "أرسل ملاحظة"), (d, w) -> {
                    Intent i = new Intent(a, ReportActivity.class);
                    if (crashed) i.putExtra("error", error);
                    a.startActivity(i);
                })
                .setNegativeButton(S.t(crashed ? "لا" : "تمام"), null)
                .show();
        } catch (RuntimeException ignored) { }
    }
}
