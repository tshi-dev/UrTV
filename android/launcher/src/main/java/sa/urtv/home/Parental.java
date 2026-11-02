package sa.urtv.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import sa.urtv.home.tilawah.NowPlaying;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * «الرقابة الأبوية»: the PIN, the locks and kids mode as the home screen, its settings screen and ADB (DockReceiver) see them. All of it
 * is OFF until the owner sets a PIN and turns something on. The decisions are ParentalPolicy's, the PIN's are PinStore's; this is the
 * Android side: where they are kept (their own preferences file), the keypad, and a worker thread for the slow PIN check so the
 * screen, and the adhan on top of it, never wait for it.
 *
 * A family lock on what the UrTV home screen opens. It does not lock the TV: its own settings, the Play Store opened another way, the
 * remote's assistant button, ADB and another launcher all go around it.
 */
final class Parental {
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
    /** What was opened with the PIN since the home screen last came to the front. */
    static final ParentalPolicy.Session SESSION = new ParentalPolicy.Session();

    private Parental() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("Parental", Context.MODE_PRIVATE);
    }

    private static PinStore store;   // one for the process, so the check on the worker thread and ADB's change share its lock

    static synchronized PinStore pin(Context c) {
        if (store == null) {
            SharedPreferences p = prefs(c);
            store = new PinStore(new PinStore.Backing() {
                @Override public String get(String key) { return p.getString(key, null); }
                @Override public void put(String key, String value) { p.edit().putString(key, value).commit(); }
                @Override public void remove(String key) { p.edit().remove(key).commit(); }
            });
        }
        return store;
    }

    static ParentalPolicy.State state(Context c) {
        SharedPreferences p = prefs(c);
        return ParentalPolicy.stateFrom(pin(c).isSet(), p.getBoolean("kids", false), p.getString("locked", ""), p.getString("allowed", ""));
    }

    static boolean needsPin(Context c, String... targets) {
        return ParentalPolicy.needsPin(state(c), SESSION, targets);
    }

    /** Tell a running home screen to redraw (kids mode changed what it shows). */
    static void changed(Context c) {
        PhoneApps.invalidate();   // the phone remote's list of favourites and locks follows at once
        c.sendBroadcast(new Intent(Dock.CHANGED).setPackage(c.getPackageName()));
    }

    // ---------------------------------------------------------------- what the owner set

    static void setLocked(Context c, String target, boolean on) { toggle(c, "locked", target, on); }

    static void setAllowed(Context c, String pkg, boolean on) {
        toggle(c, "allowed", pkg, on);
        if (prefs(c).getBoolean("kids", false)) changed(c);   // only kids mode shows by this list
    }

    static void setKids(Context c, boolean on) {
        PhoneApps.invalidate();
        prefs(c).edit().putBoolean("kids", on).commit();
        changed(c);
    }

    private static void toggle(Context c, String key, String item, boolean on) {
        PhoneApps.invalidate();
        Set<String> set = ParentalPolicy.parse(prefs(c).getString(key, ""));
        if (on) set.add(item); else set.remove(item);
        prefs(c).edit().putString(key, ParentalPolicy.join(set)).commit();
    }

    // ---------------------------------------------------------------- from the computer (DockReceiver)

    /** {@code dock.sh pin set 1234}: from the computer a PIN is set or changed without asking for the old one (ADB is the owner's). */
    static String adbSet(Context c, String pin) {
        if (!PinCrypto.valid(pin)) return "error: a PIN is exactly 4 digits";
        pin(c).set(pin);
        SESSION.clear();
        return status(c);
    }

    /** «إيقاف الرقابة الأبوية» and {@code dock.sh pin off}: no PIN, so nothing asks and kids mode is over; the apps chosen stay for the next PIN. */
    static String turnOff(Context c) {
        pin(c).clear();
        prefs(c).edit().putBoolean("kids", false).commit();
        SESSION.clear();
        changed(c);
        return status(c);
    }

    /** {@code dock.sh pin reset}: the PIN was forgotten. Everything goes: the PIN, its tries, the locks, the allowed apps, kids mode. */
    static String adbReset(Context c) {
        pin(c).clear();
        prefs(c).edit().clear().commit();
        SESSION.clear();
        changed(c);
        return status(c);
    }

    /** {@code dock.sh pin lock|unlock TARGET}: one app (its package), a pill (@wifi...) or «قفل الإعدادات» (@urtv); from the computer, like the PIN itself. */
    static String adbLock(Context c, String target, boolean on) {
        if (!ParentalPolicy.validTarget(target)) return "error: name an app's package (com.example.app), a pill (" + String.join(" ", ParentalPolicy.PILLS) + ") or " + ParentalPolicy.OWN_SETTINGS;
        setLocked(c, target, on);
        return status(c);
    }

    /** What is set, never the PIN. */
    static String status(Context c) {
        ParentalPolicy.State s = state(c);
        PinStore store = pin(c);
        long wait = store.waitLeft(System.currentTimeMillis());
        return "pin: " + (s.pinSet ? "set" : "not set")
                + "\nlocked: " + ParentalPolicy.lockedLine(s)
                + "\nkids mode: " + (s.kids ? "on" : "off") + (s.allowed.isEmpty() ? "" : " (" + s.allowed.size() + " apps allowed" + (s.pinSet ? "" : ", kept, nothing asks while there is no PIN") + ")")
                + "\nwrong tries in a row: " + store.fails() + (wait > 0 ? ", locked for " + (wait + 999) / 1000 + " more seconds" : "");
    }

    // ---------------------------------------------------------------- asking for the PIN

    /**
     * Runs {@code allowed} now when none of the targets is locked (or all were opened with the PIN since the home screen last came to
     * the front), else after the right PIN. Back or «إلغاء» runs {@code cancelled}.
     */
    static void guard(Activity a, PinPrompt.Before before, String name, String[] targets, Runnable allowed, Runnable cancelled) {
        if (!needsPin(a, targets)) {
            allowed.run();
            return;
        }
        ask(a, before, name, targets, allowed, cancelled);
    }

    /** Asks for the PIN whatever was opened before (leaving kids mode, turning the whole lock off). */
    static void confirm(Activity a, PinPrompt.Before before, String name, Runnable ok, Runnable cancelled) {
        ask(a, before, name, new String[0], ok, cancelled);
    }

    /**
     * For a screen with no adhan rule of its own: while the adhan plays, the key that stops it (any key, with «إيقاف الأذان بأي زر») works
     * over the PIN keypad too. The home screen hands its own AdhanKeys in instead, so one press is never counted twice.
     */
    static PinPrompt.Before adhanFirst(Context c) {
        AdhanKeys keys = new AdhanKeys();
        return e -> {
            boolean adhan = NowPlaying.adhanLive();
            int stop = keys.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), adhan, false, false, adhan && NowPlaying.adhanStopKey(c));
            if (stop == AdhanKeys.STOP) NowPlaying.stopAdhan(c);
            return stop != AdhanKeys.PASS;
        };
    }

    private static void ask(Activity a, PinPrompt.Before before, String name, String[] targets, Runnable allowed, Runnable cancelled) {
        long wait = pin(a).waitLeft(System.currentTimeMillis());
        if (wait > 0) {
            toast(a, tooMany(wait));
            cancelled.run();
            return;
        }
        // name: a screen's or an app's name; the screens pass their Arabic words (S.tv says them in «اللغة»), an app its own label
        // a build with a recovery code (PinRecovery, AGENTS.md) lets the keypad listen past four digits; the screen is the same
        String recovery = BuildConfig.PIN_RECOVERY;
        boolean longer = PinRecovery.on(recovery);
        String[] probed = {null};
        boolean[] probedRight = {false};
        PinPrompt.Probe probe = !longer ? null : (four, answer) -> WORK.execute(() -> {
            boolean right = pin(a).matches(four, System.currentTimeMillis());
            a.runOnUiThread(() -> {
                probed[0] = four;
                probedRight[0] = right;
                answer.accept(right);
            });
        });
        PinPrompt.ask(a, S.t("أدخل الرمز"), name == null || name.isEmpty() ? S.t("هذا مقفل بالرمز الأبوي") : S.t("«%s» مقفل بالرمز الأبوي", S.tv(name)), before,
                PinRecovery.LONGEST, probe, code ->
                WORK.execute(() -> {
                    PinStore.Result r = PinGate.check(pin(a), code, recovery, probed[0], probedRight[0], System.currentTimeMillis());
                    a.runOnUiThread(() -> {
                        if (a.isFinishing() || a.isDestroyed()) return;
                        if (r.kind == PinGate.RECOVER) {
                            // straight to «رمز جديد»: the locks, the allowed apps and kids mode stay, only the PIN is replaced
                            choose(a, before, () -> {
                                SESSION.unlock(targets);
                                allowed.run();
                            }, cancelled);
                        } else if (r.kind == PinStore.OK || r.kind == PinStore.NOT_SET) {
                            SESSION.unlock(targets);
                            allowed.run();
                        } else if (r.waitMs > 0) {
                            toast(a, tooMany(r.waitMs));
                            cancelled.run();
                        } else {
                            toast(a, r.triesLeft > 0 && r.triesLeft < PinStore.TRIES ? S.t("الرمز غير صحيح، بقي %s محاولات", Digits.of(String.valueOf(r.triesLeft))) : S.t("الرمز غير صحيح"));
                            ask(a, before, name, targets, allowed, cancelled);
                        }
                    });
                }), cancelled);
    }

    /** A new PIN, typed twice. */
    static void choose(Activity a, PinPrompt.Before before, Runnable saved, Runnable cancelled) {
        PinPrompt.ask(a, S.t("رمز جديد"), S.t("اختر رمزاً من أربعة أرقام"), before, first ->
                PinPrompt.ask(a, S.t("تأكيد الرمز"), S.t("أعد إدخال الرمز نفسه"), before, second -> {
                    if (!first.equals(second)) {
                        toast(a, S.t("الرمزان غير متطابقين، لم يتغير شيء"));
                        cancelled.run();
                        return;
                    }
                    WORK.execute(() -> {
                        pin(a).set(first);
                        a.runOnUiThread(() -> {
                            if (a.isFinishing() || a.isDestroyed()) return;
                            SESSION.clear();
                            saved.run();
                        });
                    });
                }, cancelled), cancelled);
    }

    /** Wrong or too many: «انتظر دقيقة». */
    static String tooMany(long waitMs) {
        long s = (waitMs + 999) / 1000;
        String t = s >= 60 ? S.t("%s دقيقة", Digits.of(String.valueOf((s + 59) / 60))) : S.t("%s ثانية", Digits.of(String.valueOf(s)));
        return S.t("محاولات كثيرة، انتظر %s", t);
    }

    static void toast(Context c, String s) { Toast.makeText(c, s, Toast.LENGTH_LONG).show(); }
}
