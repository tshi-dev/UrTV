package sa.urtv.home;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/** For tests/player_update_test.py: PlayerUpdate on made-up version files, caches and callers. One line per check: name TAB ok|FAIL. */
public final class PlayerUpdateRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] x) {
        String cert = SuggestedApps.PLAYER_CERT, other = "00" + cert.substring(2);
        PlayerUpdate.Version v = PlayerUpdate.parse("4302 4.3.2\n");
        check("version file: «<versionCode> <versionName>», no notes", v != null && v.code == 4302 && "4.3.2".equals(v.name) && "".equals(v.notes));
        PlayerUpdate.Version w = PlayerUpdate.parse("\uFEFF  7\t1.0-beta 2 \r\nanything after the first line\n");
        check("a BOM, CRLF, a tab, a name with a space; only the first line counts", w != null && w.code == 7 && "1.0-beta 2".equals(w.name));
        check("a code alone is enough (no name)", PlayerUpdate.parse("12") != null && PlayerUpdate.parse("12").code == 12 && "".equals(PlayerUpdate.parse("12").name));
        check("no or a bad versionCode is nothing known", PlayerUpdate.parse("1.0 12") == null && PlayerUpdate.parse("0 0.0") == null
            && PlayerUpdate.parse("-3 x") == null && PlayerUpdate.parse("12a 1.0") == null && PlayerUpdate.parse("2100000001 x") == null
            && PlayerUpdate.parse("99999999999 x") == null && PlayerUpdate.parse(null) == null && PlayerUpdate.parse("") == null && PlayerUpdate.parse("version_code=7") == null);
        PlayerUpdate.Version n = PlayerUpdate.parse("9 " + new String(new char[60]).replace('\0', 'z') + "\u0007");
        check("a name at most 40 characters, no control characters", n != null && n.name.length() == 40 && !n.name.contains("\u0007"));
        long now = 1_800_000_000_000L, h = 3600_000L;
        String cache = PlayerUpdate.encode(now, v);
        check("the cache keeps what was read", PlayerUpdate.decode(cache) != null && PlayerUpdate.decode(cache).code == 4302 && "4.3.2".equals(PlayerUpdate.decode(cache).name));
        check("a | in the name does not break the cache", PlayerUpdate.decode(PlayerUpdate.encode(now, PlayerUpdate.parse("5 a|b"))).name.equals("a b"));
        check("row: a newer version than the installed one", PlayerUpdate.row(cache, 4301) != null && PlayerUpdate.row(cache, 4301).code == 4302);
        check("no row: the same or an older version, nothing known, the Player not installed, an odd cache", PlayerUpdate.row(cache, 4302) == null && PlayerUpdate.row(cache, 5000) == null
            && PlayerUpdate.row(PlayerUpdate.encode(now, null), 1) == null && PlayerUpdate.row(cache, -1) == null && PlayerUpdate.row("", 1) == null
            && PlayerUpdate.row("x|9|a|b", 1) == null && PlayerUpdate.row(null, 1) == null);
        check("refreshed at most every 6 hours (never asked, an odd cache or a clock set back: now)", !PlayerUpdate.refreshDue(PlayerUpdate.encode(now - 5 * h, v), now)
            && PlayerUpdate.refreshDue(PlayerUpdate.encode(now - 6 * h, v), now) && PlayerUpdate.refreshDue("", now) && PlayerUpdate.refreshDue("junk", now)
            && PlayerUpdate.refreshDue(PlayerUpdate.encode(now + h, v), now));
        check("caller: com.urtv.player with exactly the pinned certificate", PlayerUpdate.callerOk("com.urtv.player", Collections.singleton(cert))
            && PlayerUpdate.callerOk("com.urtv.player", Collections.singleton(cert.toUpperCase())));
        check("caller refused: another package with the pinned certificate, the Player with another key or two signers, no caller, no signers",
            !PlayerUpdate.callerOk("com.evil.player", Collections.singleton(cert)) && !PlayerUpdate.callerOk("com.urtv.player", Collections.singleton(other))
            && !PlayerUpdate.callerOk("com.urtv.player", new HashSet<>(Arrays.asList(cert, other))) && !PlayerUpdate.callerOk(null, Collections.singleton(cert))
            && !PlayerUpdate.callerOk("com.urtv.player", Collections.<String>emptySet()) && !PlayerUpdate.callerOk("com.urtv.player", null)
            && !PlayerUpdate.callerOk("sa.urtv.home", Collections.singleton(cert)));
        check("install: the version asked is the release found, newer than installed", PlayerUpdate.installOk(4302, v, 4301));
        check("install refused: no version asked, another version, nothing found, not newer, Player unknown", !PlayerUpdate.installOk(0, v, 4301)
            && !PlayerUpdate.installOk(4303, v, 4301) && !PlayerUpdate.installOk(4302, null, 4301) && !PlayerUpdate.installOk(4302, v, 4302)
            && !PlayerUpdate.installOk(4302, v, -1));
        SuggestedApps.App p = SuggestedApps.PLAYER_APP;
        String rel = "https://github.com/tshi-dev/UrTV-Player/releases/download/";
        String r132 = "{\"tag_name\": \"v1.3.2\", \"draft\": false, \"prerelease\": false, \"assets\": [{\"browser_download_url\": \"" + rel + "v1.3.2/urtv-player.apk\"},"
            + "{\"browser_download_url\": \"" + rel + "v1.3.2/urtv-player.apk.sha256\"}, {\"browser_download_url\": \"" + rel + "v1.3.2/urtv-player.version\"}]}";
        String r140b = "{\"tag_name\": \"v1.4.0-beta1\", \"draft\": false, \"prerelease\": true, \"assets\": [{\"browser_download_url\": \"" + rel + "v1.4.0-beta1/urtv-player.apk\"},"
            + "{\"browser_download_url\": \"" + rel + "v1.4.0-beta1/urtv-player.apk.sha256\"}, {\"browser_download_url\": \"" + rel + "v1.4.0-beta1/urtv-player.version\"}]}";
        String r09 = "{\"tag_name\": \"0.9.3\", \"draft\": false, \"prerelease\": true, \"assets\": [{\"browser_download_url\": \"" + rel + "0.9.3/urtv-player.apk\"},"
            + "{\"browser_download_url\": \"" + rel + "0.9.3/urtv-player.apk.sha256\"}, {\"browser_download_url\": \"" + rel + "0.9.3/urtv-player.version\"}]}";
        String r08 = r09.replace("0.9.3", "0.8.0");
        check("the Player's own repository", "tshi-dev/UrTV-Player".equals(p.repo) && p.address().startsWith("https://api.github.com/repos/tshi-dev/UrTV-Player/releases"));
        check("a full release wins over a newer pre-release", (rel + "v1.3.2/urtv-player.version").equals(PlayerUpdate.versionUrl(p, "[" + r140b + "," + r132 + "]")));
        check("before 1.0: the newest pre-release counts", (rel + "0.9.3/urtv-player.version").equals(PlayerUpdate.versionUrl(p, "[" + r09 + "," + r08 + "]"))
            && (rel + "0.9.3/urtv-player.apk").equals(SuggestedApps.pickMirror(p, "[" + r09 + "," + r08 + "]")[0]));
        String noVersion = r132.replace(", {\"browser_download_url\": \"" + rel + "v1.3.2/urtv-player.version\"}", "");
        check("the release taken has no version file: nothing known (not an older release's version)", PlayerUpdate.versionUrl(p, "[" + noVersion + "," + r09 + "]") == null);
        String noSha = r132.replace("{\"browser_download_url\": \"" + rel + "v1.3.2/urtv-player.apk.sha256\"}, ", "");
        check("a release without its .sha256 is skipped whole", (rel + "0.9.3/urtv-player.version").equals(PlayerUpdate.versionUrl(p, "[" + noSha + "," + r09 + "]")));
        check("a draft never counts, a private (unreadable) list gives nothing", PlayerUpdate.versionUrl(p, "[" + r132.replace("\"draft\": false", "\"draft\": true") + "]") == null
            && PlayerUpdate.versionUrl(p, "{\"message\": \"Not Found\"}") == null && SuggestedApps.pickMirror(p, "{\"message\": \"Not Found\"}") == null);
        check("only the Player repository's own release files", SuggestedApps.mirrorOk(p, rel + "v1.3.2/urtv-player.apk") && SuggestedApps.mirrorOk(p, rel + "0.9.3/urtv-player.apk.sha256")
            && !SuggestedApps.mirrorOk(p, "https://github.com/tshi-dev/UrTV/releases/download/v1.3.2/urtv-player.apk") && !SuggestedApps.mirrorOk(p, rel + "v../urtv-player.apk"));
        check("the release file's redirect only to https githubusercontent.com", PlayerUpdate.redirectOk("https://objects.githubusercontent.com/github-production-release-asset/x")
            && PlayerUpdate.redirectOk("https://release-assets.githubusercontent.com/a?b=c") && !PlayerUpdate.redirectOk("http://objects.githubusercontent.com/x")
            && !PlayerUpdate.redirectOk("https://githubusercontent.com.evil.example/x") && !PlayerUpdate.redirectOk("https://evil.example/x?githubusercontent.com/")
            && !PlayerUpdate.redirectOk(null) && "urtv-player.version".equals(PlayerUpdate.VERSION_FILE));
    }
}
