package sa.urtv.home;

import java.util.Set;

/**
 * 4.3 UrTV Player's updates from inside the Player (the owner never uninstalls or reinstalls): what a newer Player is, read from our own mirror
 * (SuggestedApps' MIRROR entry: the newest «player-<version>» release of this repository whose urtv-player.apk and .sha256 are both there), and
 * who may ask. Plain Java: tests/player_update_test.py runs it on a JVM.
 *
 * The release asset (the Player's release job writes it beside the two files; docs/SETTINGS.md says the same):
 *   urtv-player.version   UTF-8 text, one "key=value" per line, in any order, nothing else needed:
 *     version_code=<the APK's versionCode, a whole number 1..2100000000>
 *     version_name=<its versionName, at most 40 characters>
 *     notes=<what is new, Arabic, one line, at most 120 characters; may be empty or missing>
 *   A release without it, or with an unreadable version_code, is "nothing known": no row, no «تحديث متوفر».
 *
 * Who may ask (PlayerUpdateProvider, InstallPlayerActivity): the calling package is com.urtv.player AND it is signed by exactly one certificate,
 * the pinned one (SuggestedApps.PLAYER_CERT).
 */
public final class PlayerUpdate {
    private PlayerUpdate() { }

    public static final String VERSION_FILE = SuggestedApps.PLAYER_FILE.replace(".apk", ".version");   // urtv-player.version
    public static final int NAME_MAX = 40, NOTES_MAX = 120, VERSION_MAX_BYTES = 4096;
    /** the cached answer is asked again, in the background, at most this often */
    public static final long REFRESH_MS = 6 * 3600_000L;

    /** One known release: its versionCode, versionName and notes. */
    public static final class Version {
        public final int code;
        public final String name, notes;
        Version(int code, String name, String notes) { this.code = code; this.name = name; this.notes = notes; }
    }

    /** the version file's answer; null when version_code is missing or not 1..2100000000 */
    public static Version parse(String body) {
        if (body == null) return null;
        Integer code = null;
        String name = "", notes = "";
        for (String line : body.replace("\r", "").split("\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String k = line.substring(0, eq).trim(), v = line.substring(eq + 1).trim();
            if (k.startsWith("﻿")) k = k.substring(1);
            switch (k) {
                case "version_code":
                    if (v.matches("[0-9]{1,10}")) { long n = Long.parseLong(v); if (n >= 1 && n <= 2_100_000_000L) code = (int) n; }
                    break;
                case "version_name": name = clean(v, NAME_MAX); break;
                case "notes": notes = clean(v, NOTES_MAX); break;
                default: break;
            }
        }
        return code == null ? null : new Version(code, name, notes);
    }

    /** one line of printable text, at most max characters (a control character or a line break goes) */
    static String clean(String s, int max) {
        String v = s.replaceAll("[\\p{Cc}\\p{Zl}\\p{Zp}]+", " ").replaceAll("\\s+", " ").trim();
        return v.length() > max ? v.substring(0, max).trim() : v;
    }

    /**
     * MIRROR: the version file's address in the very release pickMirror picks (the newest one with both files), or null when that release has none.
     */
    public static String versionUrl(SuggestedApps.App a, String releasesJson) {
        String[] urls = SuggestedApps.pickMirror(a, releasesJson);
        if (urls == null) return null;
        String v = urls[0].substring(0, urls[0].lastIndexOf('/') + 1) + VERSION_FILE;
        return releasesJson.contains("\"" + v + "\"") && v.startsWith("https://github.com/" + a.repo + "/releases/download/" + a.tagPrefix) ? v : null;
    }

    /** a release file's redirect may go to GitHub's file host only: https, a host of githubusercontent.com */
    public static boolean redirectOk(String location) {
        if (location == null) return false;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^https://([A-Za-z0-9.-]+)(?::443)?/").matcher(location);
        return m.find() && (m.group(1).equals("githubusercontent.com") || m.group(1).endsWith(".githubusercontent.com")) && !location.contains("..");
    }

    /** the cache: "<time>|<code>|<name>|<notes>" a verified release with its version, "<time>|0||" nothing known; the time is when it was asked */
    public static String encode(long at, Version v) {
        return at + "|" + (v == null ? "0||" : v.code + "|" + v.name.replace("|", " ") + "|" + v.notes.replace("|", " "));
    }

    /** the cached version, null when nothing (or nothing readable) is known */
    public static Version decode(String cache) {
        if (cache == null) return null;
        String[] p = cache.split("\\|", 4);
        if (p.length < 4 || !p[0].matches("\\d{1,15}") || !p[1].matches("[0-9]{1,10}")) return null;
        long code = Long.parseLong(p[1]);
        if (code < 1 || code > 2_100_000_000L) return null;
        return new Version((int) code, clean(p[2], NAME_MAX), clean(p[3], NOTES_MAX));
    }

    /** ask again: never asked, an unreadable cache, older than REFRESH_MS, or a clock set back */
    public static boolean refreshDue(String cache, long now) {
        if (cache == null || !cache.matches("\\d{1,15}\\|.*")) return true;
        long at = Long.parseLong(cache.substring(0, cache.indexOf('|')));
        return now < at || now - at >= REFRESH_MS;
    }

    /** the provider's one row, or null: a known version newer than the installed Player's versionCode (installed < 0: unknown, no row) */
    public static Version row(String cache, long installed) {
        Version v = decode(cache);
        return v != null && installed >= 0 && v.code > installed ? v : null;
    }

    /** the caller may ask: com.urtv.player, signed by exactly one certificate, the pinned one */
    public static boolean callerOk(String pkg, Set<String> signers) {
        return SuggestedApps.PLAYER.equals(pkg) && signers != null && signers.size() == 1
            && SuggestedApps.PLAYER_CERT.equalsIgnoreCase(signers.iterator().next()) && SuggestedApps.mirrorReady(SuggestedApps.PLAYER_APP);
    }

    /** INSTALL_PLAYER's request: a version_code given (> 0), the same as the release found now, and newer than the installed Player */
    public static boolean installOk(int asked, Version found, long installed) {
        return asked > 0 && found != null && found.code == asked && installed >= 0 && found.code > installed;
    }
}
