package sa.urtv.home.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Runs SetupEngine against a fake TV (packages, settings, the HOME role) on a plain JVM. Prints one line per scenario for tests/setup_engine_test.py. */
public class SetupEngineRun {
    /** A fake Android TV: the commands the engine sends, with the answers the real ones give. */
    static final class FakeTv implements SetupEngine.Shell {
        final Set<String> installed = new LinkedHashSet<>(), off = new LinkedHashSet<>();
        final Map<String, String> settings = new HashMap<>(), appops = new HashMap<>();
        String roleHolder = "com.google.android.apps.tv.launcherx";
        String stockPkg = "com.google.android.apps.tv.launcherx";
        String webview = "Current WebView package (name, version): (com.google.android.webview, 120.0.6099.144)";
        String sdk = "34";
        boolean silent, dashFailsMarker, wizardAnswersHome, wizardOwnsWifi, nokia, refuseSwitch, noRoleManager, emptyResolve;
        boolean roleRejected, homeRejected, ignoreRoleBack, appopRefused;
        String exactAlarm = "default";
        String maker = "Google", adbPort = "", bucket = "10", bootId = "boot-1";
        int reboots;
        long memAvailKb = 1024000;
        boolean autoStartOps;
        long dfTotal = 16777216, dfAvail = 12582912;
        final List<String> log = new ArrayList<>();

        String home() {
            if (emptyResolve) return "";
            String h = roleHolder;
            if (ignoreRoleBack && h.equals("sa.urtv.home") && refuseSwitch) return "com.google.android.tungsten.setupwraith/.RecoveryActivity";   // stuck on the setup wizard's recovery screen
            if (h.equals("sa.urtv.home") && !off.contains("sa.urtv.home")) return refuseSwitch ? stockPkg + "/.home.HomeActivity" : "sa.urtv.home/.MainActivity";
            if (off.contains(h)) return "com.google.android.tungsten.setupwraith/.RecoveryActivity";
            return h + "/.home.HomeActivity";
        }

        public AdbClient.Result run(String command) {
            log.add(command);
            String body = command.contains("\necho URTV_RC=") ? command.substring(0, command.indexOf("\necho URTV_RC=")) : command;
            if (silent) return new AdbClient.Result("", 0);
            String out = answer(body);
            int code = 0;
            if (roleRejected && body.startsWith("cmd role add-role-holder") && body.endsWith("sa.urtv.home")) code = 1;
            if (homeRejected && body.startsWith("cmd package set-home-activity") && body.contains("sa.urtv.home")) code = 1;
            return new AdbClient.Result(out, code);
        }

        String answer(String c) {
            if (!c.endsWith("; echo END_OF_LIST") && c.contains("; ")) {   // a line of several commands (the backup's, autostart's): each one's answer in turn
                StringBuilder b = new StringBuilder();
                for (String part : c.split("; ")) b.append(answer(part));
                return b.toString();
            }
            c = c.replace(" 2> /dev/null", "").replace(" 2>&1", "");
            if (c.startsWith("echo '")) return c.substring(6, c.lastIndexOf('\'')) + "\n";
            if (c.equals("true") || c.equals("appops write-settings") || c.startsWith("pm trim-caches")) return "";
            if (c.startsWith("dumpsys activity settings")) return "";
            if (c.startsWith("getprop ro.product.model")) return "Test TV\n";
            if (c.startsWith("getprop ro.product.manufacturer") || c.startsWith("getprop ro.product.brand")) return maker + "\n";
            if (c.startsWith("getprop ro.product.device")) return "testdevice\n";
            if (c.startsWith("getprop ro.build.version.release")) return "14\n";
            if (c.startsWith("getprop ro.build.type")) return "user\n";
            if (c.startsWith("getprop persist.adb.tcp.port")) return adbPort + "\n";
            if (c.startsWith("device_config get activity_manager ")) return settings.getOrDefault("dc/" + c.substring(c.lastIndexOf(' ') + 1), "null") + "\n";
            if (c.startsWith("device_config put activity_manager ")) { String[] w = c.split(" "); settings.put("dc/" + w[3], w[4]); return ""; }
            if (c.startsWith("device_config delete activity_manager ")) { settings.remove("dc/" + c.substring(c.lastIndexOf(' ') + 1)); return ""; }
            if (c.startsWith("am get-standby-bucket ")) return bucket + "\n";
            if (c.startsWith("am set-standby-bucket ")) { String b = c.substring(c.lastIndexOf(' ') + 1); bucket = b.equals("restricted") ? "45" : b.equals("rare") ? "40" : b.equals("active") ? "10" : bucket; return ""; }
            if (c.startsWith("appops get com.android.vending ")) { String op = c.substring(c.lastIndexOf(' ') + 1); return appops.containsKey("ps/" + op) ? op + ": " + appops.get("ps/" + op) + "\n" : "No operations.\n"; }
            if (c.startsWith("cmd appops set com.android.vending ")) { String[] w = c.split(" "); if (w[5].equals("default")) appops.remove("ps/" + w[4]); else appops.put("ps/" + w[4], w[5]); return ""; }
            if (c.matches("(cmd )?appops (get|set) sa\\.urtv\\.[a-z]+ (APP_)?AUTO_START.*")) {
                String[] w = c.replaceFirst("^cmd ", "").split(" ");
                if (!autoStartOps) return "Error: Unknown operation string: " + w[3] + "\n";
                if (w[1].equals("set")) { appops.put(w[2] + "/" + w[3], w[4]); return ""; }
                return appops.containsKey(w[2] + "/" + w[3]) ? w[3] + ": " + appops.get(w[2] + "/" + w[3]) + "\n" : "No operations.\n";
            }
            if (c.startsWith("df -k /data")) return "/dev/block/dm-5 " + dfTotal + " 1 " + dfAvail + " 25% /data\n";
            if (c.equals("pm list packages") || c.equals("pm list packages -d")) {
                String a = answer(c + "; echo END_OF_LIST");
                return a.endsWith("END_OF_LIST\n") ? a.substring(0, a.length() - "END_OF_LIST\n".length()) : a;
            }
            if (c.matches("pm list packages [A-Za-z0-9_.]+")) {
                String q = c.substring(c.lastIndexOf(' ') + 1);
                StringBuilder b = new StringBuilder();
                for (String p : installed) if (p.contains(q)) b.append("package:").append(p).append('\n');
                return b.toString();
            }
            if (c.equals("cat /proc/sys/kernel/random/boot_id")) return bootId + "\n";
            if (c.equals("reboot")) { reboots++; return ""; }
            if (c.equals("sync") || c.startsWith("input keyevent")) return "";
            if (c.startsWith("grep -E 'MemTotal")) return "MemTotal:        2048000 kB\nMemAvailable:     " + memAvailKb + " kB\nSwapTotal:        512000 kB\nSwapFree:         500000 kB\n";
            if (c.startsWith("dumpsys meminfo")) return "Total RAM: 2,048,000K\nFree RAM: 900,000K\n";
            if (c.startsWith("ps -A")) return "312\n";
            if (c.startsWith("top -b")) return "Tasks: 300 total\n400%cpu  10%user   0%nice  10%sys 380%idle\n";
            if (c.startsWith("pm list packages -d | wc -l")) return off.size() + "\n";
            if (c.startsWith("dumpsys package ")) {
                String p = c.split(" ")[2];
                return installed.contains(p) ? "    versionCode=151 minSdk=26 targetSdk=30\n    versionName=4.0\n" : "";
            }
            if (c.startsWith("pm uninstall ")) { String p = c.substring(c.lastIndexOf(' ') + 1); installed.remove(p); off.remove(p); return "Success\n"; }
            if (c.startsWith("getprop ro.product.cpu.abi")) return "arm64-v8a,armeabi-v7a,armeabi\n";
            if (c.startsWith("getprop ro.build.display.id")) return "TEST.1234\n";
            if (c.startsWith("getprop ro.build.version.sdk")) return sdk + "\n";
            if (c.equals("pm list features")) return "feature:android.hardware.touchscreen\nfeature:android.software.leanback\n";
            if (c.equals("pm list packages; echo END_OF_LIST")) {
                StringBuilder b = new StringBuilder("package:android\n");
                for (String p : installed) b.append("package:").append(p).append('\n');
                return b.append("END_OF_LIST\n").toString();
            }
            if (c.equals("pm list packages -d; echo END_OF_LIST")) {
                StringBuilder b = new StringBuilder();
                for (String p : off) b.append("package:").append(p).append('\n');
                return dashFailsMarker ? b.toString() : b.append("END_OF_LIST\n").toString();
            }
            if (c.startsWith("pm disable-user --user 0 ")) {
                String p = c.substring(c.lastIndexOf(' ') + 1);
                if (nokia && p.equals(stockPkg)) return "java.lang.SecurityException: This command is illegal\n";
                if (installed.contains(p) && !p.equals("sa.urtv.home")) off.add(p);
                return "Package " + p + " new state: disabled-user\n";
            }
            if (c.startsWith("pm enable ")) { String p = c.substring(c.lastIndexOf(' ') + 1); off.remove(p); return "Package " + p + " new state: enabled\n"; }
            if (c.startsWith("pm grant ")) return "";
            if (c.startsWith("appops get sa.urtv.home SCHEDULE_EXACT_ALARM")) return "SCHEDULE_EXACT_ALARM: " + exactAlarm + "; time=+1d\n";
            if (c.startsWith("appops set sa.urtv.home SCHEDULE_EXACT_ALARM ")) { if (!appopRefused) exactAlarm = c.substring(c.lastIndexOf(' ') + 1); return ""; }
            if (c.startsWith("appops get sa.urtv.home REQUEST_INSTALL_PACKAGES")) return appops.containsKey("REQUEST_INSTALL_PACKAGES") ? "REQUEST_INSTALL_PACKAGES: " + appops.get("REQUEST_INSTALL_PACKAGES") + "\n" : "No operations.\n";
            if (c.startsWith("appops set sa.urtv.home REQUEST_INSTALL_PACKAGES ")) { if (!appopRefused) appops.put("REQUEST_INSTALL_PACKAGES", c.substring(c.lastIndexOf(' ') + 1)); return ""; }
            if (c.startsWith("dumpsys webviewupdate")) return webview + "\n";
            if (c.startsWith("settings get ")) { String[] w = c.split(" "); return settings.getOrDefault(w[2] + "/" + w[3], "null") + "\n"; }
            if (c.startsWith("settings put ")) { String[] w = c.split(" "); settings.put(w[2] + "/" + w[3], w[4].replace("'", "")); return ""; }
            if (c.startsWith("settings delete ")) { String[] w = c.split(" "); settings.remove(w[2] + "/" + w[3]); return ""; }
            if (c.startsWith("cmd role get-role-holders")) return noRoleManager ? "" : roleHolder + "\n";
            if (c.startsWith("cmd role add-role-holder")) {
                String who = c.substring(c.lastIndexOf(' ') + 1);
                if (roleRejected && who.equals("sa.urtv.home")) return "Error\n";
                if (ignoreRoleBack && !who.equals("sa.urtv.home")) return "";
                roleHolder = who;
                return "";
            }
            if (c.startsWith("cmd package set-home-activity")) return "";
            if (c.startsWith("cmd package resolve-activity")) return home() + "\n";
            if (c.startsWith("cmd package query-activities")) return "2 activities found:\n  Activity #0:\n    priority=0\n    " + stockPkg + "/.home.HomeActivity\n"
                + (wizardAnswersHome ? "  Activity #1:\n    com.google.android.tungsten.setupwraith/.RecoveryActivity\n" : "");
            if (c.startsWith("dumpsys wifi")) return wizardOwnsWifi ? "  cname=com.google.android.tungsten.setupwraith\n" : "  cname=android.uid.system\n";
            if (c.startsWith("dumpsys activity")) return "  mResumedActivity: ActivityRecord{abc u0 " + home() + " t5}\n";
            if (c.startsWith("input keyevent") || c.startsWith("am force-stop") || c.startsWith("cmd notification")) return "";
            return "unknown command: " + c + "\n";
        }

        int count(String prefix) { int n = 0; for (String l : log) if (l.startsWith(prefix)) n++; return n; }
    }

    static final class Mem implements SetupEngine.Store {
        final Map<String, String> files = new HashMap<>();
        public String get(String name) { return files.get(name); }
        public void put(String name, String text) { files.put(name, text); }
    }

    static final class Dead implements SetupEngine.Deadman {
        int armed = 0, disarmed = 0;
        boolean works = true, exact = true;
        public boolean arm(int seconds) { if (works) armed++; return works; }
        public void disarm() { disarmed++; }
        public boolean exact() { return exact; }
    }

    static FakeTv tv() {
        FakeTv t = new FakeTv();
        t.installed.addAll(Arrays.asList(SetupLists.SAFE));
        t.installed.addAll(Arrays.asList("com.google.android.apps.tv.launcherx", "com.android.vending", "com.tcl.airplay2", "sa.urtv.home", "sa.urtv.screensaver",
            "com.google.android.tungsten.setupwraith", "com.google.android.leanbacklauncher.recommendations", "com.tcl.tv.tclhome_passive"));
        t.off.add("com.tcl.bi");            // already off before UrTV: never ours
        t.settings.put("secure/screensaver_components", "com.google.android.apps.tv.dreamx/.service.Dream");
        t.settings.put("secure/screensaver_enabled", "1");
        return t;
    }

    static SetupEngine engine(FakeTv t, Mem m, Dead d, List<String> said) {
        return new SetupEngine(t, m, said::add, d, ms -> { }, "2026-10-27 12:00:00");
    }

    static String snapshot(FakeTv t) {
        return new TreeSet<>(t.off) + "|" + roleHolderOf(t) + "|" + new TreeSet<>(t.settings.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(java.util.stream.Collectors.toList()))
            + "|" + t.exactAlarm + "|" + (t.appops.getOrDefault("REQUEST_INSTALL_PACKAGES", "default").equals("default") ? "" : t.appops);   // Android's own default is the same as never set
    }

    static String roleHolderOf(FakeTv t) { return t.roleHolder; }

    static void line(String s) { System.out.println(s); }

    public static void main(String[] a) throws Exception {
        // A: a healthy TV, the whole run, then the undo puts everything back
        FakeTv t = tv(); Mem m = new Mem(); Dead d = new Dead(); List<String> said = new ArrayList<>();
        String before = snapshot(t);
        SetupEngine e = engine(t, m, d, said);
        SetupEngine.Result pre = e.precheck(), gr = e.grants(), de = e.debloat(), ss = e.screensaver(), ho = e.home();
        line("A_steps " + pre.status + " " + gr.status + " " + de.status + " " + ss.status + " " + ho.status);
        TreeSet<String> ledger = new TreeSet<>(SetupEngine.ledgerNames(m.get("disabled-by-us.txt")));
        line("A_ledger " + String.join(",", ledger));
        line("A_journal " + m.get("home-switch.json").replace("\n", "|").replaceAll("\"written\": \"[^\"]*\"", "\"written\": \"W\""));
        line("A_home " + t.roleHolder + " " + t.home() + " armed=" + d.armed + " disarmed=" + d.disarmed + " helpersOff=" + (t.off.contains("com.tcl.tv.tclhome_passive") && t.off.contains("com.google.android.leanbacklauncher.recommendations")));
        line("A_after_off_stock " + t.off.contains("com.google.android.apps.tv.launcherx"));
        line("A_screensaver " + t.settings.get("secure/screensaver_components"));
        SetupEngine.Result un = e.undo();
        line("A_undo " + un.status + " " + t.roleHolder + " " + t.home() + " " + t.settings.get("secure/screensaver_components") + " ledger=[" + m.get("disabled-by-us.txt").trim() + "]");
        line("A_same " + (snapshot(t).equals(before)) + " " + new TreeSet<>(t.off));
        line("A_journal_state " + e.journal("state"));
        // A2: running it twice changes nothing more (the ledger keeps once each; Home is already ours)
        FakeTv t2 = tv(); Mem m2 = new Mem(); Dead d2 = new Dead(); List<String> said2 = new ArrayList<>();
        SetupEngine e2 = engine(t2, m2, d2, said2);
        e2.precheck(); e2.grants(); e2.debloat(); e2.screensaver(); e2.home();
        String ledger1 = m2.get("disabled-by-us.txt");
        e2.precheck(); e2.grants(); e2.debloat(); e2.screensaver(); SetupEngine.Result again = e2.home();
        line("A2 " + ledger1.equals(m2.get("disabled-by-us.txt")) + " " + again.status + " " + again.message.startsWith("تلفزيونك هو الشاشة الرئيسية أصلاً"));
        // B: the TV does not end the list of disabled packages: nothing is turned off, nothing remembered
        FakeTv tb = tv(); tb.dashFailsMarker = true; Mem mb = new Mem();
        SetupEngine.Result rb = engine(tb, mb, new Dead(), new ArrayList<>()).debloat();
        line("B " + rb.status + " disables=" + tb.count("pm disable-user") + " ledger=" + mb.get("disabled-by-us.txt"));
        // C: a TV that answers nothing at all: nothing counts as done, nothing is changed
        FakeTv tc = tv(); tc.silent = true; Mem mc = new Mem(); Dead dc = new Dead();
        SetupEngine ec = engine(tc, mc, dc, new ArrayList<>());
        line("C " + ec.precheck().status + " " + ec.debloat().status + " " + ec.screensaver().status + " " + ec.home().status + " " + ec.undo().status
            + " changes=" + (tc.count("pm disable-user") + tc.count("settings put") + tc.count("cmd role add") + tc.count("pm enable")) + " files=" + mc.files.keySet()
            + " armed=" + dc.armed);
        // D: Home does not open ours: the stock home is put back, the safety switch is disarmed, the journal says restored
        FakeTv td = tv(); td.refuseSwitch = true; Mem md = new Mem(); Dead dd = new Dead();
        SetupEngine ed = engine(td, md, dd, new ArrayList<>());
        SetupEngine.Result rd = ed.home();
        line("D " + rd.status + " " + td.roleHolder + " stockOff=" + td.off.contains(td.stockPkg) + " wizOff=" + td.off.contains("com.google.android.tungsten.setupwraith")
            + " armed=" + dd.armed + " disarmed=" + dd.disarmed + " state=" + ed.journal("state"));
        // E: a box that answers «illegal» to disabling its own home (Nokia): skipped, as before
        FakeTv te = tv(); te.nokia = true; Mem me = new Mem(); Dead de2 = new Dead();
        SetupEngine ee = engine(te, me, de2, new ArrayList<>());
        SetupEngine.Result re = ee.home();
        line("E " + re.status + " " + te.roleHolder + " stockOff=" + te.off.contains(te.stockPkg) + " state=" + ee.journal("state") + " disarmed=" + de2.disarmed);
        // F: the WebView is too old, or unknown: the screensaver is left alone
        FakeTv tf = tv(); tf.webview = "Current WebView package (name, version): (com.google.android.webview, 66.0.3359.158)"; Mem mf = new Mem();
        SetupEngine.Result rf = engine(tf, mf, new Dead(), new ArrayList<>()).screensaver();
        FakeTv tg = tv(); tg.webview = ""; SetupEngine.Result rg = engine(tg, new Mem(), new Dead(), new ArrayList<>()).screensaver();
        FakeTv th = tv(); th.installed.remove("sa.urtv.screensaver"); SetupEngine.Result rh = engine(th, new Mem(), new Dead(), new ArrayList<>()).screensaver();
        line("F " + rf.status + " " + rg.status + " " + rh.status + " puts=" + (tf.count("settings put") + tg.count("settings put") + th.count("settings put")));
        // G: the wizard answers Home and owns the saved Wi-Fi: it is left on
        FakeTv tw = tv(); tw.wizardAnswersHome = true; tw.wizardOwnsWifi = true;
        SetupEngine.Result rw = engine(tw, new Mem(), new Dead(), new ArrayList<>()).home();
        line("G " + rw.status + " wizardDisabled=" + tw.count("pm disable-user --user 0 com.google.android.tungsten.setupwraith"));
        FakeTv tx = tv(); tx.wizardAnswersHome = true; tx.wizardOwnsWifi = false;
        engine(tx, new Mem(), new Dead(), new ArrayList<>()).home();
        line("G2 wizardDisabled=" + tx.count("pm disable-user --user 0 com.google.android.tungsten.setupwraith"));
        // L: no alarm could be set: the home switch does not start, nothing on the TV changes
        FakeTv tl = tv(); Mem ml = new Mem(); Dead dl = new Dead(); dl.works = false;
        SetupEngine el = engine(tl, ml, dl, new ArrayList<>());
        SetupEngine.Result rl = el.home();
        line("L " + rl.status + " state=" + el.journal("state") + " changes=" + (tl.count("cmd role add") + tl.count("pm disable-user") + tl.count("cmd package set-home")) + " deadline=[" + ml.get("home-deadline.txt") + "]");
        // M: the TV rejects the role (or the home activity): the stock home is never disabled, and it is put back and checked
        FakeTv tm = tv(); tm.roleRejected = true; Mem mm = new Mem(); Dead dm = new Dead();
        SetupEngine em = engine(tm, mm, dm, new ArrayList<>());
        SetupEngine.Result rm = em.home();
        line("M " + rm.status + " stockDisabled=" + tm.count("pm disable-user --user 0 com.google.android.apps.tv.launcherx") + " " + tm.roleHolder + " state=" + em.journal("state") + " disarmed=" + dm.disarmed);
        FakeTv tm2 = tv(); tm2.homeRejected = true; Mem mm2 = new Mem();
        SetupEngine.Result rm2 = engine(tm2, mm2, new Dead(), new ArrayList<>()).home();
        line("M2 " + rm2.status + " stockDisabled=" + tm2.count("pm disable-user --user 0 com.google.android.apps.tv.launcherx") + " " + tm2.roleHolder);
        // N: the stock home cannot be CONFIRMED back: not «restored», the safety switch is not disarmed, the journal stays «switching»; a later recovery finishes it
        FakeTv tn = tv(); tn.refuseSwitch = true; tn.ignoreRoleBack = true; Mem mn = new Mem(); Dead dn = new Dead();
        SetupEngine en = engine(tn, mn, dn, new ArrayList<>());
        SetupEngine.Result rn = en.home();
        line("N " + rn.status + " state=" + en.journal("state") + " disarmed=" + dn.disarmed + " deadlineSet=" + (en.deadline() > 0));
        tn.ignoreRoleBack = false; tn.refuseSwitch = false; tn.roleHolder = "com.google.android.tungsten.setupwraith";
        SetupEngine.Result rn2 = en.recoverIfCutOff();
        line("N2 " + (rn2 == null ? "null" : rn2.status) + " state=" + en.journal("state") + " disarmed=" + dn.disarmed + " deadline=" + en.deadline() + " " + tn.roleHolder);
        // O: a journal value is never a shell word: a holder that is not a package name falls back to the stock home; a `$Inner` component is quoted
        FakeTv to = tv(); Mem mo = new Mem(); Dead dno = new Dead(); SetupEngine eo = engine(to, mo, dno, new ArrayList<>());
        eo.home();
        mo.put("home-switch.json", mo.get("home-switch.json").replace("\"role_holder_before\": \"com.google.android.apps.tv.launcherx\"", "\"role_holder_before\": \"x; reboot #\""));
        to.log.clear();
        eo.undo();
        boolean evil = false; for (String l : to.log) if (l.contains("reboot")) evil = true;
        line("O holderOk=" + !evil + " " + to.roleHolder);
        FakeTv tq = tv(); Mem mq = new Mem(); mq.put("screensaver-before.txt", "a.b/.C$Inner\n1\nnull\n");
        engine(tq, mq, new Dead(), new ArrayList<>()).undo();
        boolean quoted = false; for (String l : tq.log) if (l.contains("screensaver_components 'a.b/.C$Inner'")) quoted = true;
        line("O2 quoted=" + quoted);
        // Q: the permissions are read back; the exact-alarm mode is given back only when it was changed, as it was
        FakeTv tg1 = tv(); tg1.appopRefused = true; Mem mg1 = new Mem();
        SetupEngine eg1 = engine(tg1, mg1, new Dead(), new ArrayList<>());
        eg1.precheck();
        SetupEngine.Result rg1 = eg1.grants();
        FakeTv tg2 = tv(); tg2.exactAlarm = "allow"; Mem mg2 = new Mem(); SetupEngine eg2 = engine(tg2, mg2, new Dead(), new ArrayList<>());
        eg2.precheck(); eg2.grants(); eg2.undo();
        FakeTv tg3 = tv(); tg3.exactAlarm = "ignore"; Mem mg3 = new Mem(); SetupEngine eg3 = engine(tg3, mg3, new Dead(), new ArrayList<>());
        eg3.precheck(); eg3.grants(); String during = tg3.exactAlarm; eg3.undo();
        line("Q " + rg1.status + " alreadyAllow=" + tg2.exactAlarm + "/" + tg2.count("appops set sa.urtv.home SCHEDULE_EXACT_ALARM") + " ignore->" + during + "->" + tg3.exactAlarm);
        // R: Android 12+ without the exact-alarm permission: the screen is told it can be late
        FakeTv tx2 = tv(); Dead dx = new Dead(); dx.exact = false; List<String> saidX = new ArrayList<>();
        engine(tx2, new Mem(), dx, saidX).home();
        boolean late = false; for (String l : saidX) if (l.contains("قد يتأخر")) late = true;
        line("R late=" + late);
        // H: a switch cut off in the middle (the journal says switching, Home is the stock one): recovered; ours is home: left alone
        FakeTv tr = tv(); Mem mr = new Mem(); Dead dr = new Dead();
        SetupEngine er = engine(tr, mr, dr, new ArrayList<>());
        tr.settings.put("secure/x", "1");
        er.home();      // a good switch first
        tr.roleHolder = tr.stockPkg; tr.off.remove(tr.stockPkg);                   // ... then pretend the TV went back to the stock home with the journal at «switching»
        mr.put("home-switch.json", mr.get("home-switch.json").replaceAll("(?m)^  \"state\": \".*\",", "  \"state\": \"switching\","));
        SetupEngine.Result rr = er.recoverIfCutOff();
        line("H " + (rr == null ? "null" : rr.status) + " " + er.journal("state") + " " + tr.roleHolder);
        FakeTv ts = tv(); Mem ms = new Mem(); SetupEngine es = engine(ts, ms, new Dead(), new ArrayList<>());
        line("H2 " + (es.recoverIfCutOff() == null));
        // H3: the journal says switching but ours is Home (it took): confirmed done, the alarm is cancelled
        FakeTv t3 = tv(); Mem m3 = new Mem(); Dead d3 = new Dead(); SetupEngine e3 = engine(t3, m3, d3, new ArrayList<>());
        e3.home();
        m3.put("home-switch.json", m3.get("home-switch.json").replaceAll("(?m)^  \"state\": \".*\",", "  \"state\": \"switching\","));
        SetupEngine.Result r3 = e3.recoverIfCutOff();
        line("H3 " + r3.status + " " + e3.journal("state") + " disarmed=" + d3.disarmed);
        // I: no role holder named (an older Android): the app that answers Home is the «before» only when it is the stock home; nothing readable stops the switch
        FakeTv ti = tv(); ti.noRoleManager = true; Mem mi = new Mem();
        engine(ti, mi, new Dead(), new ArrayList<>()).home();
        FakeTv tj = tv(); tj.noRoleManager = true; tj.emptyResolve = true; Mem mj = new Mem();
        SetupEngine.Result rj = engine(tj, mj, new Dead(), new ArrayList<>()).home();
        line("I " + new SetupEngine(ti, mi, s -> { }, new Dead(), ms2 -> { }, "x").journal("role_holder_before") + " " + rj.status + " changes=" + (tj.count("cmd role add") + tj.count("pm disable-user")) + " files=" + mj.files.keySet());
        line("S " + SetupEngine.backoffSeconds(0) + " " + SetupEngine.backoffSeconds(1) + " " + SetupEngine.backoffSeconds(3) + " " + SetupEngine.backoffSeconds(5) + " " + SetupEngine.backoffSeconds(50));
        // T: a quick TV is checked at once: putStockBack waits for nothing when Home is already the stock one
        final int[] sleeps = {0};
        FakeTv tt = tv(); tt.roleRejected = true;
        new SetupEngine(tt, new Mem(), x -> { }, new Dead(), waitMs -> sleeps[0]++, "x").home();
        line("T sleeps=" + sleeps[0]);
        // J: the lists: the safe list never holds a package of keep.txt or core.txt
        int bad = 0;
        for (String p : SetupEngine.filterSafe(SetupLists.SAFE)) {
            for (String k : SetupLists.KEEP) if (k.equals(p)) bad++;
            for (String re2 : SetupLists.CORE) if (p.matches(re2)) bad++;
        }
        line("J " + bad + " " + SetupEngine.filterSafe(SetupLists.SAFE).size() + " " + SetupLists.SAFE.length);
        // K: parsing
        line("K " + (SetupEngine.packagesOf("package:a\npackage:b\n") == null) + " " + (SetupEngine.packagesOf("package:a\r\nEND_OF_LIST\r\n").size()) + " " + (SetupEngine.packagesOf("") == null)
            + " " + SetupEngine.webviewMajor("Current WebView package (name, version): (com.google.android.webview, 120.0.6099.144)") + " " + SetupEngine.webviewMajor("nothing") + " " + SetupEngine.ledgerNames("a.b\n# no\na.b\n\nc d\ne.f\r\n"));
    }
}
