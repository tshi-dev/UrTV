package sa.urtv.home.setup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How the setup went, in exactly the form optimize/share.py writes it for the owner to send if they want to: only share.py's allowlist of lines
 * (the TV, the steps and their times, memory before and after, the checks), and nothing personal (no city, team, apps of theirs, names, addresses or
 * serials: none of these is ever read here). Saved as files/setup/report.md. shareUrl() is the link share.py puts in its QR code: docs/share/ (GitHub
 * Pages) with the report after the #, so the done screen can show it and the person sends it from the phone if they want. Plain Java.
 */
final class SetupReport {
    static final String FILE = "report.md", STEPS = "steps.log";
    /** share.py's PAGE: the QR code opens this page with the report after the # (docs/share/index.html reads location.hash). */
    static final String PAGE = "https://tshi-dev.github.io/UrTV/share/";
    /** The longest link a QR code holds (version 40, level L, byte mode): share.py's segno refuses a longer one. */
    static final int QR_MAX_BYTES = 2953;
    /** share.py's ALLOWED_LINE, to the letter: a line that does not look like one of its fields is dropped. */
    static final Pattern ALLOWED_LINE = Pattern.compile(
        "UrTV (problem|setup) report, |TV: |Computer: |Steps, minutes:seconds:$|Memory:$|Checks:$|Setup results: |ADB: "
        + "|Protected features on: |(Device|WebView|Home|Home switch|Screensaver|Versions|Extras):|Auto-start|Optional picks|!! |- |  (off|on): "
        + "|home hides|[\\w-]+: \\d+ off, |\\(no steps\\.log: an older run\\.sh\\)$|\\(not measured\\)$|\\(no report\\.md yet: the setup didn't finish\\)$|\\d+ min in all$|\\[\\d+:\\d\\d until it was run again\\]$"
        + "|[A-Za-z0-9_.-]+ \\d+:\\d\\d( STOPPED.*)?(, [A-Za-z0-9_.-]+ \\d+:\\d\\d( STOPPED.*)?)*(, \\[.*\\])?$");
    static final String[] PROTECTED = {"com.tcl.airplay2", "com.google.android.apps.mediashell", "com.tcl.miracast", "com.google.android.katniss", "com.tcl.tvinput", "com.android.vending", "com.google.android.gms"};
    static final String[] OFF_BY_DESIGN = {"com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher", "com.google.android.tungsten.setupwraith", "com.tcl.keycustomfunctionservice"};

    private final SetupTv tv;
    private final SetupEngine.Store store;

    SetupReport(SetupEngine.Shell sh, SetupEngine.Store store) {
        this.tv = new SetupTv(sh);
        this.store = store;
    }

    /** share.py's restrict(): lines that are not one of its fields are dropped (free text has no place in this report), each cut at 400 characters. */
    static String restrict(String text) {
        List<String> out = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        int n = text.endsWith("\n") ? lines.length - 1 : lines.length;   // Python's splitlines(): no empty last line
        for (int i = 0; i < n; i++) {
            String l = lines[i];
            if (!l.trim().isEmpty() && !ALLOWED_LINE.matcher(l).lookingAt()) continue;
            out.add(l.length() > 400 ? l.substring(0, 400) : l);
        }
        return String.join("\n", out) + (text.endsWith("\n") ? "\n" : "");
    }

    /** Python's urllib.parse.quote(text): UTF-8, everything but letters, digits, _ . - ~ and / as %XX (upper case). */
    static String quote(String text) {
        StringBuilder b = new StringBuilder();
        for (byte x : text.getBytes(StandardCharsets.UTF_8)) {
            int c = x & 0xff;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-' || c == '~' || c == '/') b.append((char) c);
            else b.append('%').append(String.format(Locale.ROOT, "%02X", c));
        }
        return b.toString();
    }

    /** The link share.py's QR code carries: the page, then the report after the #. Null when it is too long for a QR code. */
    static String shareUrl(String report) {
        String url = PAGE + "#" + quote(report);
        return url.getBytes(StandardCharsets.UTF_8).length <= QR_MAX_BYTES ? url : null;
    }

    // ---- the steps' times (share.py's steps(), from steps.log) -----------------------------------------------------------------------

    static List<String> steps(String log) {
        List<String> out = new ArrayList<>();
        if (log == null || log.isEmpty()) { out.add("(no steps.log: an older run.sh)"); return out; }
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        Pattern row = Pattern.compile("(\\S+ \\S+) (start|done|stopped) (\\S+)(.*)");
        List<String> lines = new ArrayList<>();
        Map<String, Long> started = new LinkedHashMap<>();
        Long first = null, last = null, stoppedAt = null;
        for (String r : log.split("\n")) {
            Matcher m = row.matcher(r);
            if (!m.matches()) continue;
            long t;
            try { t = f.parse(m.group(1)).getTime() / 1000; } catch (java.text.ParseException e) { continue; }
            first = first == null ? t : Math.min(first, t);
            last = last == null ? t : Math.max(last, t);
            String what = m.group(2), step = m.group(3);
            if (what.equals("start")) {
                started.put(step, t);
                if (stoppedAt != null) {
                    long gap = t - stoppedAt;
                    if (gap >= 0) lines.add("[" + gap / 60 + ":" + two(gap % 60) + " until it was run again]");
                    stoppedAt = null;
                }
            } else if (started.containsKey(step)) {
                long took = Math.max(0, t - started.remove(step));
                lines.add(step + " " + took / 60 + ":" + two(took % 60) + (what.equals("done") ? "" : " STOPPED" + m.group(4)));
                stoppedAt = what.equals("stopped") ? t : null;
            }
        }
        String total = first != null ? (last - first) / 60 + " min in all" : "";
        if (!lines.isEmpty()) { out.add(String.join(", ", lines)); out.add(total); }
        else out.add(total);
        return out;
    }

    private static String two(long n) { return n < 10 ? "0" + n : String.valueOf(n); }

    // ---- memory (share.py's memory(), from summary.md) -------------------------------------------------------------------------------

    /** kinds: «before restart», «after now» lines (SetupMeasure): a reading taken without the restart says so after its label. */
    static List<String> memory(String summary, String kinds) {
        List<String> out = new ArrayList<>();
        if (summary == null || summary.isEmpty()) { out.add("(not measured)"); return out; }
        List<String[]> rows = new ArrayList<>();
        for (String l : summary.split("\n")) {
            if (!l.startsWith("|") || l.startsWith("|---")) continue;
            String inner = l.trim();
            inner = inner.replaceAll("^\\|+", "").replaceAll("\\|+$", "");
            String[] c = inner.split("\\|", -1);
            for (int i = 0; i < c.length; i++) c[i] = c[i].trim();
            rows.add(c);
        }
        if (rows.isEmpty()) { out.add("(not measured)"); return out; }
        String[] head = rows.get(0);
        String total = head.length > 1 ? head[1].replaceAll("[^0-9]", "") : "";
        for (int i = 1; i < rows.size(); i++) {
            String[] r = rows.get(i);
            String label = r[0] + (kinds != null && kinds.contains(r[0] + " now") ? " (" + SetupMeasure.NO_RESTART + ")" : "");
            if (r.length < 6 || !r[1].matches("[0-9]+")) { out.add("- " + label + ": " + (r.length > 1 ? r[1] : "")); continue; }
            out.add("- " + label + ": " + r[1] + " MB free" + (total.isEmpty() ? "" : " of " + total) + ", " + r[2] + " MB swap, " + r[3] + " processes, "
                + (r[4].isEmpty() ? "?" : r[4]) + " CPU idle, " + r[5] + " disabled");
        }
        return out;
    }

    // ---- the checks, read from the TV now ------------------------------------------------------------------------------------------

    private String appver(String pkg) throws IOException {
        String o = tv.text("dumpsys package " + pkg + " 2> /dev/null | grep -e versionName -e versionCode= | sed -n 1,2p");
        Matcher n = Pattern.compile("versionName=(\\S+)").matcher(o), c = Pattern.compile("versionCode=([0-9]+)").matcher(o);
        if (!n.find()) return "";
        return n.group(1) + (c.find() ? " (build " + c.group(1) + ")" : "");
    }

    /** The whole report. appVersion: this app's version as the screen says it («4.0 (build 151)»). */
    String build(String appVersion, long nowMs) throws IOException {
        Map<String, String> p = new LinkedHashMap<>();
        for (String k : new String[] {"ro.product.manufacturer", "ro.product.model", "ro.product.device", "ro.build.version.release", "ro.build.version.sdk", "ro.build.display.id"}) {
            String v = tv.tail1("getprop " + k).trim();
            p.put(k, v.isEmpty() ? "?" : v);
        }
        List<String> parts = new ArrayList<>();
        parts.add("UrTV setup report, " + new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(nowMs)) + ", UrTV " + appVersion);
        parts.add("");
        parts.add("TV: " + p.get("ro.product.manufacturer") + " " + p.get("ro.product.model") + " (" + p.get("ro.product.device") + "), Android " + p.get("ro.build.version.release")
            + " (SDK " + p.get("ro.build.version.sdk") + "), firmware " + p.get("ro.build.display.id"));
        parts.add("Computer: none (set up by the TV itself, over its own adb)");
        parts.add("");
        parts.add("Steps, minutes:seconds:");
        parts.addAll(steps(store.get(STEPS)));
        parts.add("");
        parts.add("Memory:");
        parts.addAll(memory(store.get(SetupMeasure.SUMMARY), store.get(SetupMeasure.KINDS)));
        parts.add("");
        parts.add("Checks:");
        parts.addAll(checks());
        String text = restrict(String.join("\n", parts)) + "\n";
        store.put(FILE, text);
        return text;
    }

    private List<String> checks() throws IOException {
        List<String> out = new ArrayList<>(), bad = new ArrayList<>();
        Set<String> installed = tv.packages("pm list packages"), disabled = tv.packages("pm list packages -d");
        int ok = 0;
        for (String pk : PROTECTED) {
            if (installed == null || disabled == null) break;
            if (disabled.contains(pk)) bad.add("!! " + pk + " is DISABLED");
            else if (installed.contains(pk)) ok++;
        }
        out.add("Protected features on: " + ok + "/" + (ok + bad.size()));
        out.addAll(bad);
        String home = tv.tail1(SetupEngine.RESOLVE_HOME).trim();
        out.add("Home: " + (home.matches("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+") ? home : "?"));
        String saver = tv.tail1("settings get secure screensaver_components").trim();
        out.add("Screensaver: " + (saver.matches("[A-Za-z0-9_.$/,]+") ? saver : "?"));
        String vs = appver(SetupEngine.SAVER), vh = appver(SetupEngine.SELF);
        out.add("Versions: screensaver " + vs + ", home " + vh);
        if (installed != null) out.add("Extras: TV Bro " + (installed.contains(SetupInstall.TVBRO) ? "yes" : "no") + ", SmartTube " + (installed.contains(SetupInstall.SMARTTUBE) ? "yes" : "no"));
        SetupPrep prep = new SetupPrep(tv.sh, store, t -> { });
        if (!prep.isTcl()) out.add("Auto-start: not a TCL, nothing to allow");
        else {
            List<String> st = new ArrayList<>();
            for (String pk : SetupPrep.OURS) if (installed != null && installed.contains(pk)) st.add(pk + " " + prep.state(pk));
            out.add("Auto-start (TCL): " + (st.isEmpty() ? "our apps are not installed" : String.join(", ", st)));
        }
        if (installed != null && disabled != null) {
            Map<String, List<String>> lists = new LinkedHashMap<>();
            lists.put("tcl-safe", java.util.Arrays.asList(SetupLists.SAFE));
            lists.put("community", SetupCommunity.lines(store.get(SetupCommunity.APPLIED)));
            lists.put("home-replaced", java.util.Arrays.asList(SetupLists.HOME_HELPERS));
            for (Map.Entry<String, List<String>> e : lists.entrySet()) {
                List<String> off = new ArrayList<>(), on = new ArrayList<>();
                int absent = 0;
                for (String pk : e.getValue()) {
                    if (disabled.contains(pk)) { if (!off.contains(pk)) off.add(pk); }
                    else if (installed.contains(pk)) { if (!on.contains(pk)) on.add(pk); }
                    else absent++;
                }
                if (off.isEmpty() && on.isEmpty() && absent == 0) continue;
                out.add(e.getKey() + ": " + off.size() + " off, " + on.size() + " still on, " + absent + " not on this TV");
                if (!off.isEmpty()) out.add("  off: " + String.join(", ", off));
                if (!on.isEmpty()) out.add("  on: " + String.join(", ", on));
            }
            Set<String> before = SetupPrep.section(store.get(SetupPrep.BACKUP), "disabled packages");
            List<String> prot = new ArrayList<>();
            for (String pk : SetupLists.KEEP) if (disabled.contains(pk) && !java.util.Arrays.asList(OFF_BY_DESIGN).contains(pk) && !before.contains(pk)) prot.add(pk);
            if (!prot.isEmpty()) out.add("!! protected but off: " + String.join(", ", prot));
        }
        return out;
    }
}
