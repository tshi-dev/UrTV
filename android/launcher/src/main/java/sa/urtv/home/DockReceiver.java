package sa.urtv.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The favourites row from ADB, for agents and scripts. Only the shell can send these: the manifest requires
 * android.permission.DUMP, which apps can't hold.
 *
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.GET_DOCK
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.LIST_APPS
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.SET_DOCK --es apps pkg1,pkg2,...
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.RESET_DOCK
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.HIDDEN                 left out, and why
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.HIDE --es apps pkg1,pkg2,...
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.SHOW --es apps pkg1,pkg2,...
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.NAMES                  pkg | shown | own | from
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.SET_NAMES --es names 'pkg|الاسم;pkg2|'   (empty: back)
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.CLEAR_NAMES
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.INPUT_NAMES            id | shown | the TV's name | ours   («أسماء المصادر»)
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.SET_INPUT_NAME --es id com.x/.Svc/HDMI1 --es name 'PS5'   (empty: the TV's name)
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.DEBUG --es flag traynoglass|nopillpanel|nowifimark|nosnapshot|nocardglass|nokeyfilter|keytrace|nolift|noglow|noshine|noscale|notitle|plainfocus|nobackdrop|all|show --es value on|off   (a tester's switch: one piece of the home's drawing off)
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.RAM_TIER --es tier low|mid|normal|auto   (a test switch: the memory tier, from the next start)
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.PIN_SET --es pin 1234   «الرقابة الأبوية»: the master PIN (never printed back)
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.PIN_LOCK --es target com.example.app   (PIN_UNLOCK to open it again; a pill: @wifi...)
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.PIN_STATUS            what is set
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.PIN_OFF               no PIN: nothing asks, kids mode ends, the choices stay
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.PIN_RESET             a forgotten PIN: everything parental back to nothing
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.BACK_SAVER --es mode on|off|status   «الرجوع يفتح شاشة التوقف» (off by default)
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.KEY_APP [--es key youtube|netflix|prime --es app PKG|default]   «أزرار الريموت»:
 *      the app a remote key opens (on our home and, with the typing service on, over other apps); answers "key-apps: youtube=…, netflix=…, prime=…"
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.SLEEP_TV [--ei minutes 30 | 0 = cancel]   «مؤقت النوم» for the whole TV
 *   am broadcast -n sa.urtv.home/.DockReceiver -a sa.urtv.home.FAMILY_DNS --es mode on|off|status   «حماية العائلة (DNS)»: on/off
 *      answers (the shell script depends on these exact lines):
 *        off     runs here, at once (no network call): "family-dns: RESTORED | NOTHING_TO_RESTORE | RESTORE_FAILED | CHANGED_BY_SOMEONE_ELSE"
 *                (or "family-dns: busy" while an "on" check runs)
 *        on      "family-dns: started (on)" and the ~20 s check runs on a background thread; or "family-dns: busy",
 *                "family-dns: NEEDS_GRANT", "family-dns: UNSUPPORTED"
 *        status  "family-dns: <on|off|unknown> (<us|other|none>); grant=<yes|no>; last=<RESULT or none>[; working]; supported=<yes|no>"
 *
 * The answer is in the "data" of am's output. optimize/dock.sh wraps them all. People use «ترتيب المفضلة»
 * (DockActivity) instead.
 */
public class DockReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String action = String.valueOf(i.getAction());
        Map<String, String> apps = Dock.launchable(c);
        switch (action) {
            case "sa.urtv.home.LIST_APPS": {
                StringBuilder b = new StringBuilder();
                for (Map.Entry<String, String> e : apps.entrySet()) b.append(e.getKey()).append(" | ").append(e.getValue()).append('\n');
                setResultData(b.toString().trim());
                return;
            }
            case "sa.urtv.home.NAMES": {   // for the owner's agent: which names are still not Arabic, and their own name
                Map<String, String> given = AppNames.given(c);
                StringBuilder b = new StringBuilder();
                for (ResolveInfo ri : Dock.catalogue(c)) {
                    String own = String.valueOf(ri.loadLabel(c.getPackageManager()));
                    b.append(ri.activityInfo.packageName).append(" | ").append(AppNames.label(c, ri, given)).append(" | ")
                     .append(own).append(" | ").append(AppNames.source(c, ri, given)).append('\n');
                }
                setResultData(b.toString().trim());
                return;
            }
            case "sa.urtv.home.SET_NAMES": {
                String raw = i.getStringExtra("names");
                if (raw == null) { setResultData("error: add --es names 'pkg|الاسم;pkg2|الاسم'"); return; }
                Map<String, String> given = AppNames.given(c);
                List<String> done = new ArrayList<>(), skipped = new ArrayList<>();
                for (String pair : raw.split(";")) {
                    int bar = pair.indexOf('|');
                    if (bar <= 0) { if (!pair.trim().isEmpty()) skipped.add(pair.trim()); continue; }
                    String pkg = pair.substring(0, bar).trim(), name = pair.substring(bar + 1).trim();
                    if (!apps.containsKey(pkg)) { skipped.add(pkg + " (not on the home screen)"); continue; }
                    if (name.isEmpty()) { given.remove(pkg); done.add(pkg + " (its usual name again)"); continue; }
                    if (!AppNames.valid(name)) { skipped.add(pkg + " (Arabic, one line, up to " + AppNames.MAX_LEN + " characters)"); continue; }
                    given.put(pkg, name);
                    done.add(pkg + " = " + name);
                }
                AppNames.save(c, given);
                c.sendBroadcast(new Intent(Dock.CHANGED).setPackage(c.getPackageName()));
                setResultData("named: " + (done.isEmpty() ? "none" : String.join(", ", done))
                    + (skipped.isEmpty() ? "" : "\nskipped: " + String.join(", ", skipped)));
                return;
            }
            case "sa.urtv.home.CLEAR_NAMES":
                AppNames.save(c, new java.util.HashMap<String, String>());
                c.sendBroadcast(new Intent(Dock.CHANGED).setPackage(c.getPackageName()));
                setResultData("names: back to each app's usual name");
                return;
            case "sa.urtv.home.INPUT_NAMES": {   // only the inputs the TV reports now; the names are the owner's and are never logged
                Map<String, String> given = InputLabels.given(c);
                StringBuilder b = new StringBuilder();
                for (android.media.tv.TvInputInfo t : InputLabels.reported(c)) {
                    String ours = given.get(InputLabels.key(t));
                    b.append(InputLabels.key(t)).append(" | ").append(InputLabels.label(c, t, given)).append(" | ").append(InputLabels.own(c, t))
                     .append(" | ").append(ours == null ? "" : ours)
                     .append(" | ").append(InputDisplay.cecName(InputLabels.cecRaw(c, t)))   // the CEC device's own name, "" when none
                     .append(" | ").append(InputLabels.stateWord(InputLabels.state(c, t))).append('\n');
                }
                setResultData(b.length() == 0 ? "no inputs reported" : b.toString().trim());
                return;
            }
            case "sa.urtv.home.SET_INPUT_NAME": {
                String id = i.getStringExtra("id"), raw = i.getStringExtra("name");
                if (id == null || raw == null) { setResultData("error: add --es id <input id> --es name 'PS5' ('' = the TV's own name)"); return; }
                if (!InputNames.validId(id)) { setResultData("error: not an input id"); return; }
                String name = InputNames.clean(raw);
                if (name == null) { setResultData("error: one line, up to " + InputNames.MAX_LEN + " characters"); return; }
                if (!name.isEmpty()) {   // a typo in the id is refused; clearing a name of an input that is gone is always allowed
                    boolean known = false;
                    for (android.media.tv.TvInputInfo t : InputLabels.reported(c)) known |= id.equals(InputLabels.key(t));
                    if (!known) { setResultData("error: this TV does not report that input (optimize/dock.sh input-names)"); return; }
                }
                InputLabels.set(c, id, name);
                setResultData(name.isEmpty() ? "input-name: the TV's own name again" : "input-name: saved");
                return;
            }
            case "sa.urtv.home.KEY_APP": {   // «أزرار الريموت»: --es key youtube|netflix|prime --es app PKG|default; none = how they stand
                String k = i.getStringExtra("key"), a = i.getStringExtra("app");
                if (k != null || a != null) {
                    if (RemoteButtons.byId(Dock.remoteTv(), k) == null) { setResultData("error: this TV's remote has no " + k + " button we know (optimize/dock.sh key lists them)"); return; }
                    String pkg = a == null || a.equals("default") ? "" : a;
                    if (!KeyApps.validChoice(pkg)) { setResultData("error: --es app is a package name or default"); return; }
                    if (!pkg.isEmpty() && !Dock.installed(c, pkg)) { setResultData("error: " + pkg + " is not installed"); return; }
                    Dock.setKeyApp(c, k, pkg);
                }
                StringBuilder b = new StringBuilder("key-apps:");
                java.util.List<RemoteButtons.Button> all = RemoteButtons.buttons(Dock.remoteTv());
                if (all.isEmpty()) b.append(" none (no app buttons are known for this TV's remote)");
                for (RemoteButtons.Button x : all) {
                    String v = Dock.keyApp(c, x.id);
                    b.append(' ').append(x.id).append('=').append(v.isEmpty() ? "default" : v).append(x == all.get(all.size() - 1) ? "" : ",");
                }
                setResultData(b.toString());
                return;
            }
            case "sa.urtv.home.BACK_SAVER": {   // «الرجوع يفتح شاشة التوقف»: on | off | status; read at each Back, nothing to redraw
                String m = String.valueOf(i.getStringExtra("mode"));
                if (!m.equals("on") && !m.equals("off") && !m.equals("status")) { setResultData("error: add --es mode on|off|status"); return; }
                if (!m.equals("status")) Dock.setBackSaver(c, m.equals("on"));
                setResultData("back-saver: " + (Dock.backSaver(c) ? "on" : "off"));
                return;
            }
            case "sa.urtv.home.PHONE_MESSAGE": {   // 4.3 #53 «رسائل الجوال»: --es mode on|off|status, --es overapps on|off
                String m = String.valueOf(i.getStringExtra("mode")), o = i.getStringExtra("overapps");
                if (!m.equals("on") && !m.equals("off") && !m.equals("status")) { setResultData("error: add --es mode on|off|status"); return; }
                if (o != null && !o.equals("on") && !o.equals("off")) { setResultData("error: overapps is on or off"); return; }
                if (!m.equals("status")) MessageActivity.setOn(c, m.equals("on"));
                if (o != null) MessageActivity.setOverApps(c, o.equals("on"));
                setResultData("phone-message: " + (MessageActivity.on(c) ? "on" : "off") + ", over apps: " + (MessageActivity.overApps(c) ? "on" : "off"));
                return;
            }
            case "sa.urtv.home.SLEEP_TV": {   // the sleep timer: no extra = how it stands; minutes = start it; 0 = cancel
                if (i.hasExtra("minutes")) {
                    int m = i.getIntExtra("minutes", -1);
                    if (m < 0 || m > 600) { setResultData("error: minutes is 1 to 600, or 0 to cancel"); return; }
                    SleepTimer.start(c, m);
                }
                setResultData(SleepTimer.status(c));
                return;
            }
            case "sa.urtv.home.FAMILY_DNS": {   // Private DNS -> AdGuard Family, see FamilyDns. mode: on | off | status
                String m = String.valueOf(i.getStringExtra("mode"));
                if (!m.equals("on") && !m.equals("off") && !m.equals("status")) { setResultData("error: add --es mode on|off|status"); return; }
                if (m.equals("off")) {
                    if (FamilyDnsAndroid.busy()) { setResultData("family-dns: busy"); return; }
                    setResultData("family-dns: " + FamilyDnsAndroid.disableNow(c).name());
                    return;
                }
                boolean grant = c.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED;
                boolean supported = FamilyDnsAndroid.supported(c);
                if (m.equals("on")) {
                    if (!supported) { setResultData("family-dns: UNSUPPORTED"); return; }
                    if (!grant) { setResultData("family-dns: NEEDS_GRANT"); return; }
                    setResultData(FamilyDnsAndroid.run(c, true, null) ? "family-dns: started (on)" : "family-dns: busy");
                    return;
                }
                FamilyDns.Status st = FamilyDnsAndroid.status(c);
                String last = FamilyDnsAndroid.lastResult(c);
                setResultData("family-dns: " + st + "; grant=" + (grant ? "yes" : "no") + "; last=" + (last.isEmpty() ? "none" : last)
                    + (FamilyDnsAndroid.busy() ? "; working" : "") + "; supported=" + (supported ? "yes" : "no"));
                return;
            }
            case "sa.urtv.home.PIN_SET": {   // from here only: the shell's DUMP is needed to send it, and this is where a forgotten PIN is put right
                String pin = i.getStringExtra("pin");
                setResultData(pin == null ? "error: add --es pin 1234 (4 digits)" : Parental.adbSet(c, pin));
                return;
            }
            case "sa.urtv.home.PIN_LOCK":
            case "sa.urtv.home.PIN_UNLOCK": {   // one app, pill or «قفل الإعدادات»; from here only, like the PIN
                String target = i.getStringExtra("target");
                setResultData(target == null ? "error: add --es target com.example.app" : Parental.adbLock(c, target, "sa.urtv.home.PIN_LOCK".equals(action)));
                return;
            }
            case "sa.urtv.home.PIN_STATUS":
                setResultData(Parental.status(c));
                return;
            case "sa.urtv.home.PIN_OFF":
                setResultData(Parental.turnOff(c));
                return;
            case "sa.urtv.home.PIN_RESET":
                setResultData(Parental.adbReset(c));
                return;
            case "sa.urtv.home.GET_DOCK":
                setResultData("dock: " + String.join(",", Dock.shown(c, apps)));
                return;
            case "sa.urtv.home.DEBUG": {
                String f = i.getStringExtra("flag"), v = i.getStringExtra("value");
                if ("show".equals(f)) { setResultData("debug: " + DebugFlags.describe(c)); return; }
                if (f == null || !DebugFlags.valid(f) || (v != null && !v.equals("on") && !v.equals("off"))) { setResultData("error: add --es flag traynoglass|nopillpanel|nowifimark|nosnapshot|nocardglass|nokeyfilter|keytrace|nolift|noglow|noshine|noscale|notitle|plainfocus|nobackdrop|all|show and --es value on|off"); return; }
                if (!DebugFlags.set(c, f, !"off".equals(v))) { setResultData("error: all can only be turned off"); return; }
                c.sendBroadcast(new Intent(DebugFlags.CHANGED).setPackage(c.getPackageName()));
                setResultData("debug: " + DebugFlags.describe(c) + " (the home starts over)");
                return;
            }
            case "sa.urtv.home.RAM_TIER": {
                String tn = i.getStringExtra("tier");
                if (tn == null || !Ram.force(c, tn)) { setResultData("error: add --es tier low|mid|normal|auto"); return; }
                setResultData("ramtier: " + tn + " (in use now: " + new String[] {"low", "mid", "normal"}[Ram.tier(c)] + "; a forced tier counts from the next start of the home)");
                return;
            }
            case "sa.urtv.home.RESET_DOCK":
                Dock.reset(c);
                setResultData("dock: " + String.join(",", Dock.shown(c, apps)) + " (defaults)");
                return;
            case "sa.urtv.home.SET_DOCK": {
                String raw = i.getStringExtra("apps");
                if (raw == null) { setResultData("error: add --es apps pkg1,pkg2,..."); return; }
                List<String> row = new ArrayList<>(), skipped = new ArrayList<>();
                for (String p : raw.split(",")) {
                    p = p.trim();
                    if (p.isEmpty() || row.contains(p)) continue;
                    if (apps.containsKey(p) && row.size() < Dock.MAX) row.add(p); else skipped.add(p);
                }
                if (row.isEmpty()) { setResultData("error: none of these apps are on this TV's home screen"); return; }
                Dock.save(c, row);
                setResultData("dock: " + String.join(",", Dock.shown(c, apps))
                    + (skipped.isEmpty() ? "" : "  skipped (not installed, hidden or over " + Dock.MAX + "): "
                       + String.join(",", skipped)));
                return;
            }
            case "sa.urtv.home.HIDDEN": {
                StringBuilder b = new StringBuilder();
                for (Map.Entry<String, String> e : Dock.leftOut(c).entrySet()) b.append(e.getKey()).append(" | ").append(e.getValue()).append('\n');
                setResultData(b.length() == 0 ? "nothing left out" : b.toString().trim());
                return;
            }
            case "sa.urtv.home.HIDE":
            case "sa.urtv.home.SHOW": {
                String raw = i.getStringExtra("apps");
                if (raw == null) { setResultData("error: add --es apps pkg1,pkg2,..."); return; }
                List<String> pkgs = new ArrayList<>();
                for (String p : raw.split(",")) if (!p.trim().isEmpty()) pkgs.add(p.trim());
                boolean hide = action.endsWith("HIDE");
                Dock.hideOrShow(c, pkgs, hide);
                setResultData((hide ? "hidden: " : "shown: ") + String.join(",", pkgs));
                return;
            }
            default:
                setResultData("error: unknown action " + action);
        }
    }
}
