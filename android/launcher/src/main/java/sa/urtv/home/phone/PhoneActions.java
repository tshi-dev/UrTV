package sa.urtv.home.phone;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import sa.urtv.home.PhoneApps;
import sa.urtv.home.tilawah.PhoneBridge;

/**
 * What the phone does on the TV, on this TV's own screens and through the same code as the TV's settings:
 *  - keys: delivered to our own screen that is in front (Android allows an app to send keys to its own windows only); volume, mute, play/pause
 *    and home go through the system's own calls. Up/down/left/right/OK/back while another app is in front are refused ("other_app").
 *  - text: into the focused text field of our own screen, or as a task when «المهام» is open; otherwise copied to the TV's clipboard.
 *  - settings: the city and the home cards through the screensaver app's provider (SaverReceiver.set), the adhan and the Quran player
 *    through tilawah.PhoneBridge (the same code as optimize/music.sh).
 */
final class PhoneActions implements Actions {
    private static final Uri SAVER = Uri.parse("content://sa.urtv.screensaver.config");

    private final Context app;
    private final Handler main = new Handler(Looper.getMainLooper());

    PhoneActions(Context app) { this.app = app.getApplicationContext(); }

    // ---- keys

    @Override public Result key(String key) {
        AudioManager audio = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        switch (key) {
            case "vol_up": audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); return Result.ok("done");
            case "vol_down": audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); return Result.ok("done");
            case "mute": audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI); return Result.ok("done");
            case "home":
                try {
                    app.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    return Result.ok("done");
                } catch (RuntimeException e) {
                    return Result.fail("failed");
                }
            case "play_pause":
                if (PhoneBridge.pauseOurs(app)) return Result.ok("done");
                audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                return Result.ok("sent");   // to whatever app is playing; the system decides
            default:
                break;
        }
        final int code;
        switch (key) {
            case "up": code = KeyEvent.KEYCODE_DPAD_UP; break;
            case "down": code = KeyEvent.KEYCODE_DPAD_DOWN; break;
            case "left": code = KeyEvent.KEYCODE_DPAD_LEFT; break;
            case "right": code = KeyEvent.KEYCODE_DPAD_RIGHT; break;
            case "ok": code = KeyEvent.KEYCODE_DPAD_CENTER; break;
            case "back": code = KeyEvent.KEYCODE_BACK; break;
            default: return Result.fail("failed");
        }
        final Activity a = Foreground.current();
        if (a == null) {   // no screen of ours in front: Android lets an app send keys only to its own windows; Back goes through the accessibility service when it is on
            if (key.equals("back") && PhoneApps.globalBack()) return Result.ok("done");
            return Result.fail("other_app");
        }
        if (a instanceof PrivateScreen) return Result.fail("private_screen");
        main.post(new Runnable() {
            @Override public void run() {
                long t = SystemClock.uptimeMillis();
                a.dispatchKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0));
                a.dispatchKeyEvent(new KeyEvent(t, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, code, 0));
            }
        });
        return Result.ok("done");
    }

    // ---- text

    @Override public Result text(final String text, String mode, boolean search) {
        // the typing log (tag UrTVType): fixed words and the length only, never the text or a package name
        android.util.Log.i("UrTVType", "text request: mode=" + ("append".equals(mode) ? "append" : "replace") + " search=" + search + " len=" + (text == null ? 0 : text.length()));
        android.util.Log.i("UrTVType", Foreground.current() == null ? "foreground=other" : "foreground=own");
        final boolean[] otherNoField = {false};
        if (Foreground.current() == null) {   // another app in front: through the accessibility service, when the owner turned it on
            String r = PhoneApps.typeText(app, text, "append".equals(mode), search);
            switch (r) {
                case "typed_other": case "typed_other_nosearch": case "searched": return Result.ok(r);
                case "no_field": otherNoField[0] = true; break;   // the app has no text field on screen: the text goes to the TV's clipboard, as it always did
                default: return Result.fail(r);   // service_off, locked, password_field, failed
            }
        }
        FutureTask<String> job = new FutureTask<>(new java.util.concurrent.Callable<String>() {
            @Override public String call() {
                Activity a = Foreground.current();
                if (a != null && !(a instanceof PrivateScreen)) {
                    if (a instanceof TextTarget && ((TextTarget) a).acceptText(text)) return "task";
                    View f = a.getCurrentFocus();
                    if (f instanceof EditText) {
                        EditText e = (EditText) f;
                        Editable ed = e.getText();
                        int s = Math.max(0, e.getSelectionStart()), t = Math.max(0, e.getSelectionEnd());
                        ed.replace(Math.min(s, t), Math.max(s, t), text);
                        return "typed";
                    }
                }
                try {
                    ClipboardManager cm = (ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("urtv", text));
                    return "copied";
                } catch (RuntimeException e) {
                    return null;
                }
            }
        });
        main.post(job);
        try {
            String r = job.get(2, TimeUnit.SECONDS);
            return r == null ? Result.fail("failed") : Result.ok(otherNoField[0] && "copied".equals(r) ? "copied_other" : r);
        } catch (Exception e) {
            return Result.fail("failed");
        }
    }

    // ---- state and settings

    private Bundle saver(String method, String arg, Bundle extras) {
        try {
            return app.getContentResolver().call(SAVER, method, arg, extras);
        } catch (RuntimeException e) {
            return null;   // the screensaver app is missing, older, or not signed with our key
        }
    }

    @Override public Map<String, Object> state() {
        Map<String, Object> m = new LinkedHashMap<>();
        Bundle b = saver("phone_state", null, null);
        m.put("settings", b != null);
        if (b != null) {
            m.put("city", b.getString("city", ""));
            m.put("home_cards", b.getStringArrayList("home_cards") == null ? new ArrayList<String>() : b.getStringArrayList("home_cards"));
            List<Object> panels = new ArrayList<>();
            List<String> codes = b.getStringArrayList("panels"), names = b.getStringArrayList("panel_names");
            for (int i = 0; codes != null && names != null && i < codes.size() && i < names.size(); i++) {
                if ("none".equals(codes.get(i))) continue;
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("id", codes.get(i));
                p.put("name", names.get(i));
                panels.add(p);
            }
            m.put("panels", panels);
        }
        m.putAll(PhoneBridge.state(app));
        PhoneBridge.addAdhanLive(app, m);
        m.put("sleep_timer", PhoneApps.sleep(app));
        m.putAll(sa.urtv.home.GlassSource.phoneState(app));   // the glass look: which backdrop, and the veils it needs (thousandths)
        if (b != null && b.getString("idle") != null) {
            m.put("saver_idle", b.getString("idle"));
            List<Object> options = new ArrayList<>();
            for (int minutes : Schema.SAVER_IDLE) options.add(String.valueOf(minutes));
            m.put("saver_idle_options", options);
        }
        m.put("keys_everywhere", PhoneApps.keysEverywhere());
        m.put("typing_service", PhoneApps.typingEnabled());
        m.put("family", PhoneApps.family(app));
        m.put("foreground", Foreground.current() != null ? "own" : "other");
        m.put("update", PhoneUpdater.status());
        return m;
    }

    @Override public Result set(Map<String, Object> v) {
        Bundle extras = new Bundle();
        boolean city = false;
        if (v.containsKey("city")) { extras.putString("city", (String) v.get("city")); city = true; }
        if (v.containsKey("home_cards")) {
            StringBuilder cards = new StringBuilder();
            for (Object x : (List<?>) v.get("home_cards")) cards.append(cards.length() > 0 ? "," : "").append((String) x);
            extras.putString("home_panel", cards.length() == 0 ? "none" : cards.toString());
        }
        if (v.containsKey("saver_idle")) extras.putString("idle", String.valueOf((Long) v.get("saver_idle")));
        if (v.containsKey("sleep_minutes") && !PhoneApps.setSleep(app, ((Long) v.get("sleep_minutes")).intValue())) return Result.fail("refused");
        if (!extras.isEmpty()) {
            Bundle r = saver("phone_set", null, extras);
            if (r == null) return Result.fail("saver_missing");
            if (!r.getBoolean("ok")) return Result.fail(r.getBoolean("idle_denied") ? "needs_permission" : "refused");
            if (city) PhoneBridge.rescheduleAdhan(app);
            if (v.containsKey("home_cards")) PhoneApps.cardsChanged(app);
        }
        String why = PhoneBridge.adhan(app, v);
        return why == null ? Result.ok("saved") : Result.fail(why);
    }

    @Override public Result quran(Map<String, Object> v) {
        String why = PhoneBridge.quran(app, v);
        return why == null ? Result.ok("sent") : Result.fail(why);
    }

    @Override public List<String> cities(String query) {
        Bundle r = saver("phone_cities", query, null);
        List<String> out = r == null ? null : r.getStringArrayList("cities");
        return out == null ? new ArrayList<String>() : out;
    }

    @Override public Map<String, Object> apps() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Object> apps = new ArrayList<>(), tools = new ArrayList<>();
        for (String[] a : PhoneApps.apps(app)) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("pkg", a[0]);
            x.put("name", a[1]);
            x.put("locked", "1".equals(a[2]));
            apps.add(x);
        }
        for (String[] t : PhoneApps.tools(app)) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", t[0]);
            x.put("name", t[1]);
            x.put("locked", "1".equals(t[2]));
            tools.add(x);
        }
        m.put("apps", apps);
        m.put("tools", tools);
        return m;
    }

    @Override public byte[] icon(String pkg) { return PhoneApps.icon(app, pkg); }

    @Override public byte[] backdrop() { return sa.urtv.home.GlassSource.png(app); }

    @Override public Result launch(String target) {
        String r = PhoneApps.launch(app, target);
        return r.equals("opened") || r.equals("pin_on_tv") ? Result.ok(r) : Result.fail(r);
    }

    @Override public Result adhanStop() { return Result.ok(PhoneBridge.stopAdhan(app) ? "done" : "nothing"); }

    @Override public Result doOp(String op) {
        if (op.equals("sleep_cancel")) return PhoneApps.setSleep(app, 0) ? Result.ok("done") : Result.fail("failed");
        Result r = launch("@saver");   // the home's own «شاشة التوقف»: the same family lock as the pill
        return r.ok ? Result.ok(r.code.equals("pin_on_tv") ? "pin_on_tv" : "done") : r;
    }

    @Override public Map<String, Object> family() { return PhoneApps.family(app); }

    @Override public Map<String, Object> update(String op) {
        return "install".equals(op) ? PhoneUpdater.install(app, true) : PhoneUpdater.checkVersion(app);   // «افحص التحديث» reads version.txt only; the files come with «ثبّت»
    }
}
