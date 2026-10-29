package sa.urtv.home.setup;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sa.urtv.home.phone.UpdateRules;

/**
 * Installing from the app, over the TV's own adb (optimize/install.sh's push_install, pm_install and from_github, ported): the screensaver from our
 * release (apk-latest, the same file install.sh and «تحديث تلفزيونك» use) and, unless the person unticked them, TV Bro and SmartTube from their own
 * GitHub releases with install.sh's asset patterns. The APK's bytes are streamed to `cmd package install -S` (AdbClient.exec): no file on the TV.
 * Before anything is installed it is checked: an update by UpdateRules.verify (same app, newer, the same signer), a fresh install of our app by
 * InstallRules.fresh (only this launcher's own signer), an extra by its package name. A package the run installs is journaled BEFORE the install
 * (installed-by-us.txt), and the undo removes only those. Play Protect's ADB check is switched off for one install only when it refuses, journaled
 * first (verifier-before.txt) and put back whatever happens. Never uninstalls anything to make room for an install. Plain Java.
 */
final class SetupInstall {
    static final String INSTALLED = "installed-by-us.txt", VERIFIER_BEFORE = "verifier-before.txt", VERIFIER = "verifier_verify_adb_installs";
    static final String TVBRO = "com.phlox.tvwebbrowser", SMARTTUBE = "org.smarttube.stable";
    /** install.sh's extras: owner/repo, package, asset pattern ({abi}: the TV's processor types, best first). */
    static final String[][] EXTRAS = {
        {"truefedex/tv-bro", TVBRO, "tvbro-[0-9.]+-generic-geckoExcluded\\.apk"},
        {"yuliskov/SmartTube", SMARTTUBE, "SmartTube_stable_[0-9.]+_{abi}\\.apk"},
    };

    /** A downloaded APK as the TV's package manager reads it. */
    static final class Apk {
        final String pkg;
        final long code, size;
        final Set<String> signers;
        final Opener opener;
        final Runnable delete;
        Apk(String pkg, long code, Set<String> signers, long size, Opener opener, Runnable delete) {
            this.pkg = pkg; this.code = code; this.signers = signers; this.size = size; this.opener = opener; this.delete = delete;
        }
    }

    interface Opener { InputStream open() throws IOException; }

    /** The platform: downloads and reads APKs, and knows this app's own signer and the installed apps (PackageManager on the TV). */
    interface Apks {
        /** Downloads a file and reads it as an APK; null when it could not be downloaded or read. */
        Apk download(String url) throws IOException;
        Set<String> selfSigners();
        long installedCode(String pkg);
        Set<String> installedSigners(String pkg);
    }

    /** `exec:` with a stream (AdbClient.exec on the TV). */
    interface Exec { String exec(String command, InputStream data, long size) throws IOException; }

    private final SetupTv tv;
    private final Exec exec;
    private final Apks apks;
    private final SetupEngine.Store store;
    private final SetupEngine.Out out;
    private final SetupEngine.Sleeper sleeper;

    SetupInstall(SetupEngine.Shell sh, Exec exec, Apks apks, SetupEngine.Store store, SetupEngine.Out out, SetupEngine.Sleeper sleeper) {
        this.tv = new SetupTv(sh);
        this.exec = exec;
        this.apks = apks;
        this.store = store;
        this.out = out;
        this.sleeper = sleeper;
    }

    private SetupEngine.Result done(String m) { out.line(m); return new SetupEngine.Result(SetupEngine.Status.DONE, m); }
    private SetupEngine.Result skipped(String m) { out.line(m); return new SetupEngine.Result(SetupEngine.Status.SKIPPED, m); }
    private SetupEngine.Result failed(String m) { out.line(m); return new SetupEngine.Result(SetupEngine.Status.FAILED, m); }

    private void remember(String pkg) throws IOException {
        List<String> all = SetupEngine.ledgerNames(store.get(INSTALLED));
        if (all.contains(pkg)) return;
        all.add(pkg);
        StringBuilder b = new StringBuilder();
        for (String p : all) b.append(p).append('\n');
        store.put(INSTALLED, b.toString());
    }

    /** The bytes to `cmd package install`, from the start of the file each time. */
    private String stream(Apk apk, String flags) throws IOException {
        try (InputStream in = apk.opener.open()) {
            return exec.exec("cmd package install " + flags + " -S " + apk.size, in, apk.size);
        }
    }

    /**
     * install.sh's pm_install: installed, then still there 3 s later? Some operator TVs remove apps installed over ADB at once: then once more with the
     * Play Store as the installer.
     */
    private String install(Apk apk, String pkg) throws IOException {
        String said = stream(apk, "-r");
        if (said.contains("Success")) {
            sleeper.sleep(3000);
            if (!tv.installed(pkg)) {
                out.line("التلفزيون حذف " + pkg + " بعد تثبيته مباشرة (خدمة فيه تفعل هذا بما يُثبَّت من ADB): نثبّته مرة ثانية كأنه من متجر Play.");
                said = stream(apk, "-i com.android.vending -r");
                sleeper.sleep(3000);
                if (said.contains("Success") && !tv.installed(pkg)) said = "Failure [" + pkg + " is removed by this TV right after every install]";
            }
        }
        return said;
    }

    /** install.sh's push_install, with Play Protect's ADB check switched off for this install only when it refused (journaled first, always put back). */
    private String installWithVerifier(Apk apk, String pkg) throws IOException {
        String said = install(apk, pkg);
        if (!said.contains("INSTALL_FAILED_VERIFICATION_FAILURE")) return said;
        String old = tv.tail1("settings get global " + VERIFIER).trim();
        if (old.isEmpty() || !old.matches("null|[0-9]+")) return "Failure [Play Protect refused, and the TV did not say what its ADB check is]";
        store.put(VERIFIER_BEFORE, old + "\n");   // before the change: the undo puts it back if this run is cut off
        out.line("Play Protect رفض التثبيت من ADB: أوقفنا فحصه لهذا التثبيت فقط، ونرجعه بعده مباشرة. إن ظهرت على التلفزيون رسالة Play Protect فاضغط «تثبيت على أي حال».");
        try {
            tv.run("settings put global " + VERIFIER + " 0");
            said = install(apk, pkg);
        } finally {
            putVerifierBack();
        }
        return said;
    }

    /** Play Protect's ADB check as it was (null: deleted); the record goes once it is back. */
    void putVerifierBack() throws IOException {
        String old = store.get(VERIFIER_BEFORE);
        if (old == null || old.trim().isEmpty()) return;
        old = old.trim();
        if (old.equals("null")) tv.run("settings delete global " + VERIFIER);
        else if (old.matches("[0-9]+")) tv.run("settings put global " + VERIFIER + " " + old);
        store.put(VERIFIER_BEFORE, "");
    }

    // ---- the screensaver from our release -------------------------------------------------------------------------------------------

    SetupEngine.Result screensaver() throws IOException {
        String pkg = SetupEngine.SAVER, url = UpdateRules.BASE + "urtv-screensaver.apk";
        boolean installed = tv.installed(pkg);
        Apk apk = apks.download(url);
        if (apk == null) {
            if (installed) return skipped("ما قدرنا ننزّل آخر نسخة من شاشة التوقف (الإنترنت؟)، فبقيت النسخة المثبّتة.");
            return failed("ما قدرنا ننزّل شاشة التوقف (الإنترنت؟). تأكد من الاتصال ثم اضغط «ابدأ» مرة ثانية.");
        }
        try {
            if (installed) {
                String why = UpdateRules.verify(pkg, apk.pkg, apks.installedCode(pkg), apk.code, apks.installedSigners(pkg), apk.signers);
                if ("not_newer".equals(why)) return done("شاشة التوقف مثبّتة وبآخر نسخة.");
                if ("other_key".equals(why)) return failed("عندك نسخة من شاشة التوقف غير الرسمية (موقّعة بمفتاح آخر)، فما ثبّتنا فوقها ولا أزلناها: إزالتها تحذف إعداداتها. اسأل من ساعدك في الإعداد.");
                if (why != null) return failed("الملف الذي نزل ليس شاشة التوقف الصحيحة، فما ثبّتناه (" + why + ").");
            } else {
                String why = InstallRules.fresh(pkg, apk.pkg, apks.selfSigners(), apk.signers);
                if (why != null) return failed("الملف الذي نزل غير موقّع بمفتاح تطبيقنا، فما ثبّتناه (" + why + ").");
                remember(pkg);   // before the install: a run cut half way is still undone
            }
            String said = installWithVerifier(apk, pkg);
            if (said.contains("Success")) return done("ثبّتنا شاشة التوقف (آخر نسخة من إصداراتنا).");
            if (said.matches("(?s).*(UPDATE_INCOMPATIBLE|signatures do not match).*")) return failed("عندك نسخة من شاشة التوقف موقّعة بمفتاح آخر، فما ثبّتنا فوقها ولا أزلناها.");
            return failed("لم يقبل التلفزيون تثبيت شاشة التوقف: " + firstLine(said));
        } finally {
            apk.delete.run();
        }
    }

    static String firstLine(String s) {
        for (String l : s.replace("\r", "").split("\n")) if (!l.trim().isEmpty()) return l.trim().length() > 120 ? l.trim().substring(0, 120) : l.trim();
        return "(لا جواب)";
    }

    // ---- TV Bro and SmartTube -------------------------------------------------------------------------------------------------------

    /** install.sh's pick_asset: the first file (by name) of the best processor type that fits the pattern, then «universal», then any. */
    static String pickAsset(String pattern, String abis, String releaseJson) {
        Map<String, String> assets = new LinkedHashMap<>();
        Matcher m = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"").matcher(releaseJson == null ? "" : releaseJson);
        while (m.find()) assets.put(m.group(1).substring(m.group(1).lastIndexOf('/') + 1), m.group(1));
        List<String> order = new ArrayList<>();
        for (String a : abis.split(",")) if (!a.isEmpty()) order.add(a);
        order.add("universal");
        order.add("");
        for (String abi : order) {
            Pattern p = Pattern.compile(pattern.replace("{abi}", Pattern.quote(abi)) + "$");
            List<String> hit = new ArrayList<>();
            for (String n : assets.keySet()) if (p.matcher(n).find()) hit.add(n);
            if (!hit.isEmpty()) {
                java.util.Collections.sort(hit);
                return assets.get(hit.get(0));
            }
        }
        return null;
    }

    /** The extras, as install.sh extras does them: one that is already on the TV is left, one that fails never holds the setup up. */
    SetupEngine.Result extras(boolean wanted, SetupRunner.Host host) throws IOException {
        if (!wanted) return skipped("تركنا TV Bro و SmartTube كما طلبت (تقدر تضيفهما لاحقاً).");
        boolean bad = false;
        List<String> got = new ArrayList<>();
        for (String[] x : EXTRAS) {
            String repo = x[0], pkg = x[1], pattern = x[2], name = pkg.equals(TVBRO) ? "TV Bro" : "SmartTube";
            if (tv.installed(pkg)) { out.line(name + " مثبّت من قبل: تركناه كما هو (يتحدّث وحده)."); got.add(name); continue; }
            String body = host.fetch("https://api.github.com/repos/" + repo + "/releases/latest", 2 * 1024 * 1024);
            if (body == null) { out.line("لم تعطنا GitHub قائمة إصدارات " + name + " (الإنترنت أو حدّ الطلبات): تركناه. جرّب لاحقاً."); bad = true; continue; }
            String url = null;
            String abis = "";
            for (int attempt = 0; attempt < 3 && url == null; attempt++) {   // a flaky TV answered nothing about its processor once
                for (String prop : new String[] {"ro.product.cpu.abilist", "ro.product.cpu.abilist32", "ro.product.cpu.abi"}) {
                    abis = tv.text("getprop " + prop).replaceAll("[\\s]", "");
                    if (!abis.isEmpty()) break;
                }
                url = pickAsset(pattern, abis, body);
                if (url == null && attempt < 2) sleeper.sleep(3000);
            }
            if (url == null) { out.line("لا نسخة من " + name + " تناسب معالج هذا التلفزيون: تركناه."); continue; }
            if (!url.startsWith("https://github.com/" + repo + "/releases/download/")) { out.line("رابط " + name + " ليس من إصداراته على GitHub، فما نزّلناه."); bad = true; continue; }
            Apk apk = apks.download(url);
            if (apk == null) { out.line("ما قدرنا ننزّل " + name + ": تركناه."); bad = true; continue; }
            try {
                if (!pkg.equals(apk.pkg)) { out.line("الملف الذي نزل ليس " + name + "، فما ثبّتناه."); bad = true; continue; }
                remember(pkg);   // before the install
                String said;
                try {
                    said = installWithVerifier(apk, pkg);
                } catch (IOException e) {
                    if (!tv.installed(pkg)) { out.line("انقطع الاتصال أثناء تثبيت " + name + ": تركناه."); bad = true; continue; }
                    said = "Success";   // the install finished while the TV was silent
                }
                if (said.contains("Success")) { out.line("ثبّتنا " + name + "."); got.add(name); }
                else if (said.matches("(?s).*(UPDATE_INCOMPATIBLE|signatures do not match).*")) out.line(name + " مثبّت من مصدر آخر: تركناه كما هو مع إعداداته.");
                else { out.line("لم يقبل التلفزيون تثبيت " + name + ": " + firstLine(said)); bad = true; }
            } finally {
                apk.delete.run();
            }
        }
        if (bad) return new SetupEngine.Result(SetupEngine.Status.FAILED, "لم نثبّت كل التطبيقات الإضافية (التفاصيل فوق). بقية الإعداد تمّت.");
        return new SetupEngine.Result(SetupEngine.Status.DONE, got.isEmpty() ? "لا تطبيقات إضافية تناسب هذا التلفزيون." : "التطبيقات الإضافية: " + String.join(" و", got) + ".");
    }

    // ---- the undo -------------------------------------------------------------------------------------------------------------------

    /** What this setup installed (installed-by-us.txt) is removed; nothing else ever is. Play Protect's ADB check back if a run was cut off. */
    List<String> undo() throws IOException {
        putVerifierBack();
        List<String> left = new ArrayList<>(), ours = SetupEngine.ledgerNames(store.get(INSTALLED));
        ours.remove(SetupEngine.SELF);   // never this app itself
        if (ours.isEmpty()) return left;
        for (String p : ours) tv.run("pm uninstall " + p);
        Set<String> now = tv.packages("pm list packages");   // with its end marker: a TV that said nothing keeps the record
        for (String p : ours) {
            if (now == null || now.contains(p)) left.add(p); else out.line("أزلنا " + p + " (ثبّته الإعداد).");
        }
        StringBuilder b = new StringBuilder();
        for (String p : left) b.append(p).append('\n');
        if (store.get(INSTALLED) != null) store.put(INSTALLED, b.toString());
        return left;
    }
}
