package sa.urtv.home;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/** For tests/player_update_test.py: PlayerUpdate on made-up version files, caches and callers. One line per check: name TAB ok|FAIL. */
public final class PlayerUpdateRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] x) {
        String cert = SuggestedApps.PLAYER_CERT, other = "00" + cert.substring(2);
        PlayerUpdate.Version v = PlayerUpdate.parse("version_code=4302\nversion_name=4.3.2\nnotes=يشغّل القوائم أسرع\n");
        check("version file: code, name and notes", v != null && v.code == 4302 && "4.3.2".equals(v.name) && "يشغّل القوائم أسرع".equals(v.notes));
        PlayerUpdate.Version w = PlayerUpdate.parse("﻿notes=\r\nversion_name = 1.0 \r\nversion_code = 7\r\nextra=ignored\r\n");
        check("any order, CRLF, a BOM, spaces, unknown keys ignored, empty notes", w != null && w.code == 7 && "1.0".equals(w.name) && "".equals(w.notes));
        check("no or a bad version_code is nothing known", PlayerUpdate.parse("version_name=1.0\nnotes=x") == null && PlayerUpdate.parse("version_code=0") == null
            && PlayerUpdate.parse("version_code=-3") == null && PlayerUpdate.parse("version_code=12a") == null && PlayerUpdate.parse("version_code=2100000001") == null
            && PlayerUpdate.parse("version_code=99999999999") == null && PlayerUpdate.parse(null) == null && PlayerUpdate.parse("") == null);
        String longNotes = new String(new char[200]).replace('\0', 'ب');
        PlayerUpdate.Version n = PlayerUpdate.parse("version_code=9\nversion_name=" + new String(new char[60]).replace('\0', 'z') + "\nnotes=" + longNotes + "\u0007x");
        check("notes at most 120 characters, a name at most 40, no control characters", n != null && n.notes.length() == 120 && n.name.length() == 40 && !n.notes.contains("\u0007"));
        long now = 1_800_000_000_000L, h = 3600_000L;
        String cache = PlayerUpdate.encode(now, v);
        check("the cache keeps what was read", PlayerUpdate.decode(cache) != null && PlayerUpdate.decode(cache).code == 4302 && "يشغّل القوائم أسرع".equals(PlayerUpdate.decode(cache).notes));
        check("a | in the notes does not break the cache", PlayerUpdate.decode(PlayerUpdate.encode(now, PlayerUpdate.parse("version_code=5\nnotes=a|b"))).notes.equals("a b"));
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
        String rel = "https://github.com/tshi-dev/UrTV/releases/download/";
        String json = "[{\"tag_name\": \"player-4.3.2\", \"draft\": false, \"prerelease\": false, \"assets\": [{\"browser_download_url\": \"" + rel + "player-4.3.2/urtv-player.apk\"},"
            + "{\"browser_download_url\": \"" + rel + "player-4.3.2/urtv-player.apk.sha256\"}, {\"browser_download_url\": \"" + rel + "player-4.3.2/urtv-player.version\"}]},"
            + "{\"tag_name\": \"player-4.3.1\", \"draft\": false, \"prerelease\": false, \"assets\": [{\"browser_download_url\": \"" + rel + "player-4.3.1/urtv-player.apk\"},"
            + "{\"browser_download_url\": \"" + rel + "player-4.3.1/urtv-player.apk.sha256\"}, {\"browser_download_url\": \"" + rel + "player-4.3.1/urtv-player.version\"}]}]";
        check("the version file of the very release the install would take", (rel + "player-4.3.2/urtv-player.version").equals(PlayerUpdate.versionUrl(p, json)));
        String noVersion = json.replace(", {\"browser_download_url\": \"" + rel + "player-4.3.2/urtv-player.version\"}", "");
        check("a newest release without its version file: nothing known (not an older release's version)", PlayerUpdate.versionUrl(p, noVersion) == null);
        String noSha = json.replace("{\"browser_download_url\": \"" + rel + "player-4.3.2/urtv-player.apk.sha256\"}, ", "");
        check("a release without its .sha256 is skipped whole: the older verified one's version", (rel + "player-4.3.1/urtv-player.version").equals(PlayerUpdate.versionUrl(p, noSha)));
        check("the release file's redirect only to https githubusercontent.com", PlayerUpdate.redirectOk("https://objects.githubusercontent.com/github-production-release-asset/x")
            && PlayerUpdate.redirectOk("https://release-assets.githubusercontent.com/a?b=c") && !PlayerUpdate.redirectOk("http://objects.githubusercontent.com/x")
            && !PlayerUpdate.redirectOk("https://githubusercontent.com.evil.example/x") && !PlayerUpdate.redirectOk("https://evil.example/x?githubusercontent.com/")
            && !PlayerUpdate.redirectOk(null) && "urtv-player.version".equals(PlayerUpdate.VERSION_FILE));
    }
}
