package sa.urtv.home;

import java.util.ArrayList;
import java.util.List;

/**
 * The TV's list of accessibility services that are on (Settings.Secure enabled_accessibility_services: «pkg/cls:pkg/.Cls:…»), read with no
 * Android in it (tests/A11yListRun.java runs it). What A11yHeal decides from it: whether our service is in the list, what the list is without
 * ours (the nudge that makes Android bind the list again), which entries a list lost since a list seen before, and whether to heal.
 * Other apps' entries are never changed, re-ordered or re-added: their text goes back exactly as it was.
 */
public final class A11yList {
    private A11yList() { }

    /** What check() decides: nothing to do, heal (switch on and nudge), our service is not in the list, or we may not write the setting. */
    public static final String OK = "ok", HEAL = "heal", NOT_LISTED = "not_listed", NO_PERMISSION = "no_permission";

    /** The entries in order, as written (empty for null, "null" or ""). */
    static List<String> entries(String list) {
        List<String> out = new ArrayList<>();
        if (list == null || list.equals("null")) return out;
        for (String e : list.split(":")) if (!e.trim().isEmpty()) out.add(e.trim());
        return out;
    }

    /** «pkg/.Cls» and «pkg/pkg.Cls» are the same service. */
    static String full(String component) {
        int slash = component.indexOf('/');
        if (slash <= 0 || slash == component.length() - 1) return component;
        String pkg = component.substring(0, slash), cls = component.substring(slash + 1);
        return cls.startsWith(".") ? pkg + "/" + pkg + cls : component;
    }

    static boolean has(String list, String component) {
        String want = full(component);
        for (String e : entries(list)) if (full(e).equals(want)) return true;
        return false;
    }

    // ---- the carry over our own update (UpdateCarry): its decisions, pure, so tests/A11yListRun.java can walk them through every order of events

    /** how long after the replace the OEM may still unmute the TV by itself; after it, an unmute is the owner's */
    public static final long OEM_WINDOW_MS = 60_000, AUDIO_LIFETIME_MS = 180_000, A11Y_LIFETIME_MS = 30 * 60_000L;
    public static final int MAX_MUTES = 3, MAX_A11Y_TRIES = 8;
    public static final long FRONT_WINDOW_MS = 45_000;

    /**
     * The mute stage: "mute" (do it now) or why not. `live` = the record's generation is the current one and it is inside the lifetime. The first answer that
     * is not "mute"/"muted_ok" ends the stage, except "already" (muted now: nothing to do, the TV may still unmute inside the window) and "wait".
     */
    public static String audioStep(boolean live, boolean wasMuted, boolean adhanActive, int snapVol, int nowVol, boolean nowMuted, int mutesDone, long sinceReplaceMs) {
        if (!live || sinceReplaceMs > AUDIO_LIFETIME_MS) return "expired";
        if (!wasMuted) return "not_muted_before";
        if (adhanActive) return "adhan";   // the adhan unmutes the music stream on purpose, and puts the mute back itself
        if (snapVol >= 0 && nowVol != snapVol) return "owner_volume";   // the owner turned the volume since: theirs
        if (nowMuted) return "already";
        if (sinceReplaceMs > OEM_WINDOW_MS) return "owner_unmute";   // past the window an unmute is not the OEM's
        if (mutesDone >= MAX_MUTES) return "enough";
        return "mute";
    }

    /** A media key to pause what the OEM resumed (the last app): only inside the window, only when the TV was muted before, never over the adhan or our own playback. */
    public static boolean pauseOthers(boolean live, boolean wasMuted, boolean adhanActive, boolean ourPlaying, boolean otherMusic, boolean alreadyPaused, long sinceReplaceMs) {
        return live && wasMuted && !adhanActive && !ourPlaying && otherMusic && !alreadyPaused && sinceReplaceMs <= AUDIO_LIFETIME_MS;
    }

    /** The accessibility stage from what was read back: "none" (we had no entry), "done" (listed, switch on, bound), "verify" (listed, not yet bound: A11yHeal nudges), "write" (put it back). */
    public static String a11yStep(boolean hadOurs, List<String> lost, boolean switchOn, boolean bound) {
        if (!hadOurs) return "none";
        if (!lost.isEmpty()) return "write";
        return switchOn && bound ? "done" : "verify";
    }

    /** the wait before the next try of a stage that did not finish: 10, 30, 60, 120, 240 s, then 300 s */
    public static long backoffMs(int tries) {
        long[] w = {10_000, 30_000, 60_000, 120_000, 240_000};
        return tries < 0 ? w[0] : tries < w.length ? w[tries] : 300_000;
    }

    /** The record of one carry-over operation (what UpdateCarry keeps on disk). A new generation is a new record. */
    public static final class Carry {
        public long gen, at, replacedAt;
        public int expected = -1, code, vol = -1, mutes, a11yTries;
        public boolean muted, hadOurs, audioDone, paused, a11yDone, toasted, finished, ui, frontDone;
        public int frontTries;
        public String saved = "";
    }

    /** What the carry reads and does on the TV (UpdateCarry implements it on Android, tests/A11yListRun.java with a fake TV). */
    public interface Tv {
        int versionCode();
        int volume();
        boolean muted();
        boolean adhanActive();
        boolean ourPlaying();
        boolean musicActive();
        boolean homeInFront();                // our home is the resumed screen
        String bringFront(int attempt);       // 0: the HOME intent, 1: HOME through the accessibility service, 2: a full-screen notification; answers a fixed word
        boolean mute();                       // asks for the mute, answers whether the TV is muted after it
        void pauseKey();
        String list();                        // enabled_accessibility_services now
        boolean switchOn();
        boolean bound();                      // our service is listed and Android runs it
        boolean canWrite();
        boolean write(String list);           // writes list + the switch; answers whether the setting reads back with ours in it
        void alarm(long delayMs, int slot);   // a durable check for the same generation
        void toast();
        void log(String stage, String result, String more);
    }

    /** True when a record of an install in the last `freshMs` is there. */
    public static boolean fresh(long at, long now, long freshMs) { return at > 0 && now >= at && now - at <= freshMs; }

    /**
     * One idempotent step of the carry-over, for whatever event (replace, 8 s later, first start, an alarm) in whatever order, in a new process or the old.
     * `gen` is the generation an alarm was set for (0: an event, not an alarm); another than the record's is stale. Sets c.finished when nothing is open.
     */
    public static void carryStep(Carry c, Tv tv, String why, long gen, long now, long freshMs) {
        if (c.gen == 0 || c.finished) return;
        if (gen != 0 && gen != c.gen) { tv.log("step", "stale", "why=" + why); return; }
        boolean replaced = c.expected > 0 ? tv.versionCode() == c.expected : tv.versionCode() != c.code;
        if (!replaced) { tv.log("step", "not_replaced_yet", "why=" + why); return; }
        if (c.replacedAt == 0) {
            if (!fresh(c.at, now, freshMs)) { c.finished = true; tv.log("step", "expired", "why=" + why); return; }
            c.replacedAt = now;
            tv.alarm(10_000, 1);
            tv.alarm(30_000, 2);
            tv.log("step", "armed", "why=" + why);
        }
        long since = now - c.replacedAt;
        boolean adhan = tv.adhanActive();
        // our home back in front: the install removed its task, so Android resumed the app used before (YouTube on 179, Jellyfin on 180). Only after an update
        // started from our UpdateActivity, only in the first 45 s, at most three ways, each tried once and logged.
        if (c.ui && !c.frontDone) {
            if (tv.homeInFront()) { c.frontDone = true; tv.log("front", "home_in_front", "why=" + why); }
            else if (since > FRONT_WINDOW_MS || c.frontTries >= 3) { c.frontDone = true; tv.log("front", "gave_up", "why=" + why + " tries=" + c.frontTries); }
            else { String how = tv.bringFront(c.frontTries); c.frontTries++; tv.log("front", how, "why=" + why + " attempt=" + c.frontTries); }
        }
        if (!c.audioDone) {
            String r = audioStep(true, c.muted, adhan, c.vol, tv.volume(), tv.muted(), c.mutes, since);   // every outcome is logged, "do nothing" ones too
            if (r.equals("mute")) {
                boolean ok = tv.mute();
                c.mutes++;
                tv.log("audio", ok ? "muted" : "mute_not_held", "why=" + why + " now_muted=" + ok);
            } else {
                tv.log("audio", r, "why=" + why);
                if (!r.equals("already")) c.audioDone = true;
            }
            if (!c.paused && pauseOthers(true, c.muted, adhan, tv.ourPlaying(), tv.musicActive(), false, since) && since <= OEM_WINDOW_MS) {
                tv.pauseKey();
                c.paused = true;
                tv.log("audio", "paused_other_app", "why=" + why);
            }
            if (since > AUDIO_LIFETIME_MS) c.audioDone = true;
        }
        if (!c.a11yDone) {
            String now_ = tv.list();
            List<String> lost = ownLost(c.saved, now_, "sa.urtv.home/sa.urtv.home.TypeService", "sa.urtv.home/sa.urtv.home.SleepService");
            String r = a11yStep(c.hadOurs, lost, tv.switchOn(), tv.bound());
            if (r.equals("write")) {
                c.a11yTries++;
                boolean can = tv.canWrite(), wrote = can && tv.write(with(now_, lost));
                if (!can && !c.toasted) { c.toasted = true; tv.toast(); }
                tv.log("a11y", wrote ? "written" : can ? "write_failed" : "no_permission", "why=" + why + " tries=" + c.a11yTries);   // failed: the saved entries stay
            } else if (r.equals("verify")) {
                c.a11yTries++;
                tv.log("a11y", "listed_not_bound", "why=" + why + " tries=" + c.a11yTries);
            } else {
                c.a11yDone = true;
                tv.log("a11y", r.equals("none") ? "no_a11y_entry" : r, "why=" + why);
            }
            if (!c.a11yDone && (c.a11yTries >= MAX_A11Y_TRIES || since > A11Y_LIFETIME_MS)) { c.a11yDone = true; tv.log("a11y", "gave_up", "why=" + why + " tries=" + c.a11yTries); }
            if (!c.a11yDone) tv.alarm(backoffMs(c.a11yTries), 3);
        }
        if (c.audioDone && c.a11yDone && (c.frontDone || !c.ui)) { c.finished = true; tv.log("step", "finished", "why=" + why); }
    }

    /** The list with every entry of these components taken out; the others as written, in their order. Empty when nothing else is left. */
    static String without(String list, String... components) {
        StringBuilder sb = new StringBuilder();
        for (String e : entries(list)) {
            boolean ours = false;
            for (String c : components) ours |= full(e).equals(full(c));
            if (ours) continue;
            if (sb.length() > 0) sb.append(':');
            sb.append(e);
        }
        return sb.toString();
    }

    /** The entries of `before` that `now` no longer has (reported only: never put back by us). */
    static List<String> lost(String before, String now) {
        List<String> out = new ArrayList<>();
        for (String e : entries(before)) if (!has(now, e) && !out.contains(e)) out.add(e);
        return out;
    }

    /**
     * Heal only when our service is in the list but Android does not run it, or the master switch (accessibility_enabled) is not 1, and only
     * with WRITE_SECURE_SETTINGS. `switchValue` is the setting as read (null when it is not set).
     */
    static String decide(boolean listed, String switchValue, boolean running, boolean canWrite) {
        if (!listed) return NOT_LISTED;
        if (running && "1".equals(switchValue)) return OK;
        return canWrite ? HEAL : NO_PERMISSION;
    }

    /**
     * After our own update (UpdateCarry): which of OUR components the list had right before the install and no longer has. Only these may be
     * added back, and only when the install is what lost them; another app's entry is never added, and an entry the owner removed before the
     * update is not in `before`, so it stays off.
     */
    static List<String> ownLost(String before, String now, String... ours) {
        List<String> out = new ArrayList<>();
        for (String c : ours) if (has(before, c) && !has(now, c) && !out.contains(c)) out.add(c);
        return out;
    }

    /** The list as written with these entries appended (each once); the others untouched, in their order. */
    static String with(String list, List<String> add) {
        StringBuilder sb = new StringBuilder();
        for (String e : entries(list)) {
            if (sb.length() > 0) sb.append(':');
            sb.append(e);
        }
        for (String c : add) {
            if (has(sb.toString(), c)) continue;
            if (sb.length() > 0) sb.append(':');
            sb.append(c);
        }
        return sb.toString();
    }
}
