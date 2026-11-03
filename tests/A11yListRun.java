package sa.urtv.home;

import java.util.Arrays;

/** For tests/a11y_heal_test.py: A11yList (the TV's list of accessibility services, and when A11yHeal heals). Prints PASS/FAIL lines. */
public final class A11yListRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        String type = "sa.urtv.home/sa.urtv.home.TypeService", sleep = "sa.urtv.home/sa.urtv.home.SleepService";
        String km = "io.github.sds100.keymapper/io.github.sds100.keymapper.system.accessibility.MyAccessibilityService";
        String list = km + ":" + type;
        check("our service is found in the list, in its full or its short form", A11yList.has(list, type) && A11yList.has(km + ":sa.urtv.home/.TypeService", type)
            && !A11yList.has(km, type) && !A11yList.has(null, type) && !A11yList.has("null", type) && !A11yList.has("", type));
        check("the list without ours keeps the others exactly as written, in their order",
            A11yList.without(km + ":" + sleep + ":com.x/.Svc$Inner:" + type, type, sleep).equals(km + ":com.x/.Svc$Inner"));
        check("ours alone: nothing is left", A11yList.without(type, type, sleep).isEmpty());
        check("an entry another app's list had and lost is reported (never re-added)", A11yList.lost(list, type).equals(Arrays.asList(km)));
        check("nothing lost, or nothing seen before: nothing reported", A11yList.lost(list, list).isEmpty() && A11yList.lost(null, list).isEmpty() && A11yList.lost("", type).isEmpty());
        check("heal: listed, the switch 0 (the TV test after the update)", A11yList.decide(true, "0", false, true).equals(A11yList.HEAL));
        check("heal: listed, the switch 1 but Android runs none of it", A11yList.decide(true, "1", false, true).equals(A11yList.HEAL));
        check("heal: running but the switch not set", A11yList.decide(true, null, true, true).equals(A11yList.HEAL));
        check("nothing to do when listed, on and running", A11yList.decide(true, "1", true, true).equals(A11yList.OK));
        check("never when our service is not in the list (the owner turned it off: we do not turn it on)", A11yList.decide(false, "0", false, true).equals(A11yList.NOT_LISTED));
        check("never without WRITE_SECURE_SETTINGS", A11yList.decide(true, "0", false, false).equals(A11yList.NO_PERMISSION));
        check("after our update: our entry the list had before the install and lost is the one to add back (tester: not_listed after 168 -> 174)",
            A11yList.ownLost(list, km, type, sleep).equals(Arrays.asList(type)));
        check("after our update: never another app's entry, never one the owner had removed before the update, nothing when nothing was lost",
            A11yList.ownLost(km, "", type, sleep).isEmpty() && A11yList.ownLost(list, list, type, sleep).isEmpty() && A11yList.ownLost(null, null, type).isEmpty()
            && A11yList.ownLost(km + ":" + type, type, type, sleep).isEmpty());
        check("added back at the end, the others exactly as written; an entry already there is not doubled",
            A11yList.with(km, Arrays.asList(type)).equals(km + ":" + type) && A11yList.with("", Arrays.asList(type)).equals(type)
            && A11yList.with(null, Arrays.asList(type)).equals(type) && A11yList.with(km + ":sa.urtv.home/.TypeService", Arrays.asList(type)).equals(km + ":sa.urtv.home/.TypeService"));
        carry(type, km);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    /** A fake TV for the carry-over: the state of the sound, the list and the clock; every alarm and log is recorded. */
    static final class FakeTv implements A11yList.Tv {
        int version = 179, vol = 7;
        boolean muted, adhan, ours, music, switchOn = true, bound, canWrite = true, writeWorks = true, muteHolds = true;
        String list = "";
        int mutes, pauses, toasts;
        final java.util.List<String> logs = new java.util.ArrayList<>(), alarms = new java.util.ArrayList<>();
        public int versionCode() { return version; }
        public int volume() { return vol; }
        public boolean muted() { return muted; }
        public boolean adhanActive() { return adhan; }
        public boolean ourPlaying() { return ours; }
        public boolean musicActive() { return music; }
        public boolean mute() { mutes++; muted = muteHolds; return muted; }
        public void pauseKey() { pauses++; music = false; }
        public String list() { return list; }
        public boolean switchOn() { return switchOn; }
        public boolean bound() { return bound; }
        public boolean canWrite() { return canWrite; }
        public boolean write(String l) { if (!writeWorks) return false; list = l; switchOn = true; bound = true; return true; }
        public void alarm(long d, int slot) { alarms.add(slot + ":" + d); }
        public void toast() { toasts++; }
        public void log(String st, String r, String m) { logs.add(st + "=" + r); }
        boolean front;
        final java.util.List<Integer> fronts = new java.util.ArrayList<>();
        public boolean homeInFront() { return front; }
        public String bringFront(int a) { fronts.add(a); return a == 0 ? "home_intent_sent" : a == 1 ? "a11y_not_bound" : "notification_posted"; }
        boolean said(String x) { return logs.contains(x); }
    }

    static A11yList.Carry rec(boolean muted, String savedList, boolean hadOurs) {
        A11yList.Carry c = new A11yList.Carry();
        c.gen = 1000; c.at = 1000; c.expected = 179; c.ui = false; c.code = 178; c.vol = 7; c.muted = muted; c.saved = savedList; c.hadOurs = hadOurs;
        return c;
    }

    static void carry(String type, String km) {
        long t0 = 100_000, fresh = 30 * 60_000L;
        // 1. the TV unmuted itself after the replace: muted again at once, again at +10 s and +30 s when it unmutes again (three times at most), never past the OEM window
        FakeTv tv = new FakeTv();
        A11yList.Carry c = rec(true, "", false);
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        check("carry: the TV unmuted by the update is muted again at the replace, and two alarms (+10 s, +30 s) are set", tv.mutes == 1 && tv.muted && tv.alarms.containsAll(Arrays.asList("1:10000", "2:30000")));
        tv.muted = false;
        A11yList.carryStep(c, tv, "alarm", 1000, t0 + 10_000, fresh);
        tv.muted = false;
        A11yList.carryStep(c, tv, "alarm", 1000, t0 + 30_000, fresh);
        check("carry: unmuted again by the TV at +10 s and +30 s: muted again each time (3 in all)", tv.mutes == 3 && tv.muted);
        tv.muted = false;
        A11yList.carryStep(c, tv, "alarm", 1000, t0 + 40_000, fresh);
        check("carry: no more than three times", tv.mutes == 3 && !tv.muted && tv.said("audio=enough"));
        // 2. the owner unmutes later (past the OEM window): theirs
        tv = new FakeTv(); c = rec(true, "", false);
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        tv.muted = false;
        A11yList.carryStep(c, tv, "start", 0, t0 + 70_000, fresh);
        check("carry: an unmute after the OEM window (60 s) is the owner's: not muted again, the stage ends", tv.mutes == 1 && !tv.muted && c.audioDone && tv.said("audio=owner_unmute"));
        // 3. the owner moved the volume since the snapshot: theirs, whatever the mute says
        tv = new FakeTv(); c = rec(true, "", false); tv.vol = 9;
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        check("carry: the owner changed the volume since the snapshot: no mute", tv.mutes == 0 && tv.said("audio=owner_volume") && c.audioDone);
        // 4. the adhan starts in the window: never muted over it, and the stage is over (the adhan puts the mute back itself)
        tv = new FakeTv(); c = rec(true, "", false);
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        tv.muted = false; tv.adhan = true;
        A11yList.carryStep(c, tv, "alarm", 1000, t0 + 10_000, fresh);
        A11yList.carryStep(c, tv, "alarm", 1000, t0 + 30_000, fresh);
        check("carry: the adhan sounding mid-window is never muted, at either check", tv.mutes == 1 && !tv.muted && tv.said("audio=adhan"));
        // 5. not muted before the update: nothing is muted
        tv = new FakeTv(); c = rec(false, "", false);
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        check("carry: a TV that was not muted before is left alone", tv.mutes == 0 && tv.said("audio=not_muted_before") && c.audioDone);
        // 6. a stale alarm (another generation) does nothing
        tv = new FakeTv(); c = rec(true, "", false);
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        tv.muted = false; int before = tv.mutes;
        A11yList.carryStep(c, tv, "alarm", 999, t0 + 10_000, fresh);
        check("carry: an alarm of an older generation is stale and changes nothing", tv.mutes == before && !tv.muted && tv.said("step=stale"));
        // 7. the first start before the replace event (order reversed), and the replace event after: one operation, no double work
        tv = new FakeTv(); c = rec(true, "", false); tv.muted = false;
        A11yList.carryStep(c, tv, "start", 0, t0, fresh);
        A11yList.carryStep(c, tv, "replaced", 0, t0 + 200, fresh);
        check("carry: the home's first start before the replace event: it starts the operation; the replace event after it is the same step (no second arming)",
            tv.mutes == 1 && tv.alarms.stream().filter(a -> a.startsWith("1:")).count() == 1 && c.replacedAt == t0);
        // 8. before the new build is installed nothing happens (the expected version is not there yet)
        tv = new FakeTv(); tv.version = 178; c = rec(true, "", false);
        A11yList.carryStep(c, tv, "start", 0, t0, fresh);
        check("carry: while the old build is still installed nothing is done and nothing is armed", tv.mutes == 0 && tv.alarms.isEmpty() && c.replacedAt == 0);
        // 9. process death: the record is saved between steps and loaded again, the alarms still find it
        tv = new FakeTv(); c = rec(true, "", false);
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        A11yList.Carry reborn = new A11yList.Carry();
        reborn.gen = c.gen; reborn.at = c.at; reborn.replacedAt = c.replacedAt; reborn.expected = c.expected; reborn.code = c.code; reborn.vol = c.vol; reborn.mutes = c.mutes;
        reborn.muted = c.muted; reborn.saved = c.saved; reborn.hadOurs = c.hadOurs; reborn.audioDone = c.audioDone; reborn.a11yDone = c.a11yDone;
        tv.muted = false;
        A11yList.carryStep(reborn, tv, "alarm", 1000, t0 + 10_000, fresh);
        check("carry: after the process died the alarm's step works from the saved record", tv.mutes == 2 && tv.muted);
        // 10. the last app resumed by the OEM plays over a TV that was muted: one pause key, inside the window only, never over the adhan or our playback
        tv = new FakeTv(); c = rec(true, "", false); tv.music = true;
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        check("carry: another app playing in the window over a TV that was muted gets one pause key", tv.pauses == 1);
        tv = new FakeTv(); c = rec(true, "", false); tv.music = true; tv.adhan = true;
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        FakeTv tv2 = new FakeTv(); A11yList.Carry c2 = rec(true, "", false); tv2.music = true; tv2.ours = true;
        A11yList.carryStep(c2, tv2, "replaced", 0, t0, fresh);
        FakeTv tv3 = new FakeTv(); A11yList.Carry c3 = rec(false, "", false); tv3.music = true;
        A11yList.carryStep(c3, tv3, "replaced", 0, t0, fresh);
        FakeTv tv4 = new FakeTv(); A11yList.Carry c4 = rec(true, "", false); tv4.music = true;
        A11yList.carryStep(c4, tv4, "replaced", 0, t0, fresh);   // arms
        c4.paused = false; tv4.music = true;
        A11yList.carryStep(c4, tv4, "alarm", 1000, t0 + 90_000, fresh);
        check("carry: no pause key over the adhan, over our own playback, when the TV was not muted before, or after the OEM window", tv.pauses == 0 && tv2.pauses == 0 && tv3.pauses == 0 && tv4.pauses == 1);   // tv4: the first (inside the window) paused once, the one at +90 s did not
        // 11. accessibility: a failed write keeps the entries and is tried again; done only when listed, switched on and bound
        String saved = km + ":" + type;
        tv = new FakeTv(); c = rec(false, saved, true); tv.list = km; tv.writeWorks = false;
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        check("carry: a failed accessibility write is not done: the saved entries stay, a retry alarm is set with a backoff", !c.a11yDone && c.a11yTries == 1 && c.saved.equals(saved) && tv.said("a11y=write_failed") && tv.alarms.contains("3:" + A11yList.backoffMs(1)));
        tv.writeWorks = true;
        A11yList.carryStep(c, tv, "alarm", 1000, t0 + 30_000, fresh);
        check("carry: the retry writes our entry back (ours only, the others as they were)", tv.list.equals(km + ":" + type) && tv.said("a11y=written") && !c.a11yDone);
        A11yList.carryStep(c, tv, "start", 0, t0 + 31_000, fresh);
        check("carry: done only after the next look reads it back listed, switched on and bound", c.a11yDone && tv.said("a11y=done"));
        tv = new FakeTv(); c = rec(false, saved, true); tv.list = saved; tv.bound = false;
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        check("carry: listed but Android has not bound it: not done (A11yHeal nudges it), looked at again later", !c.a11yDone && tv.said("a11y=listed_not_bound"));
        tv = new FakeTv(); c = rec(false, saved, true); tv.list = km; tv.canWrite = false;
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        A11yList.carryStep(c, tv, "alarm", 1000, t0 + 10_000, fresh);
        check("carry: without the permission: the notice once, the entries kept for a later try", tv.toasts == 1 && !c.a11yDone && c.saved.equals(saved));
        tv = new FakeTv(); c = rec(false, saved, true); tv.list = km; tv.writeWorks = false;
        for (int i = 0; i < 9; i++) A11yList.carryStep(c, tv, "alarm", 1000, t0 + i * 10_000, fresh);
        check("carry: a write that never works ends after the bounded number of tries", c.a11yDone && c.a11yTries <= A11yList.MAX_A11Y_TRIES && tv.said("a11y=gave_up"));
        tv = new FakeTv(); c = rec(false, "", false);
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        check("carry: no entry of ours before the update: nothing to put back, and the operation finishes", c.finished && tv.said("a11y=no_a11y_entry"));
        // 11b. our home back in front after the install took its task (A2): HOME intent, then the accessibility HOME, then the notification, within 45 s
        tv = new FakeTv(); c = rec(false, "", false); c.ui = true;
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        A11yList.carryStep(c, tv, "alarm", 1000, t0 + 10_000, fresh);
        A11yList.carryStep(c, tv, "alarm", 1000, t0 + 30_000, fresh);
        check("carry: our home is brought to the front in three ways in turn (intent, accessibility HOME, notification), one per call, each logged", tv.fronts.equals(Arrays.asList(0, 1, 2)) && tv.said("front=home_intent_sent") && tv.said("front=notification_posted"));
        A11yList.carryStep(c, tv, "alarm", 1000, t0 + 40_000, fresh);
        check("carry: no fourth try, the stage ends (gave up)", tv.fronts.size() == 3 && c.frontDone && tv.said("front=gave_up"));
        tv = new FakeTv(); c = rec(false, "", false); c.ui = true;
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        tv.front = true;
        A11yList.carryStep(c, tv, "alarm", 1000, t0 + 10_000, fresh);
        check("carry: once our home is in front nothing more is tried", tv.fronts.size() == 1 && c.frontDone && tv.said("front=home_in_front"));
        tv = new FakeTv(); c = rec(false, "", false); c.ui = true;
        A11yList.carryStep(c, tv, "replaced", 0, t0 + 50_000 - 50_000, fresh);
        A11yList.carryStep(c, tv, "start", 0, t0 + 46_000, fresh);
        check("carry: later than 45 s after the replace our home is no longer pulled to the front (the owner may have opened something)", tv.fronts.size() == 1 && tv.said("front=gave_up"));
        tv = new FakeTv(); c = rec(false, "", false);
        A11yList.carryStep(c, tv, "replaced", 0, t0, fresh);
        check("carry: an update not started from our UpdateActivity never brings the home forward", tv.fronts.isEmpty());
        tv = new FakeTv(); c = rec(true, "", false);
        A11yList.Carry old = new A11yList.Carry(); old.gen = 55; old.at = 55; old.code = 178; old.expected = -1; old.muted = true; old.vol = -1; old.ui = true;
        old.hadOurs = false;
        tv.version = 179; tv.muted = false;
        A11yList.carryStep(old, tv, "replaced", 0, 55 + 1000, fresh);
        check("carry: a record the build before wrote (no volume, no expected build) is still carried over: muted again, every step logged", tv.mutes == 1 && tv.said("audio=muted") && tv.said("a11y=no_a11y_entry") && tv.said("step=armed"));
        // 12. a record from long ago is dropped, not applied to an unrelated replace
        tv = new FakeTv(); c = rec(true, "", false);
        A11yList.carryStep(c, tv, "replaced", 0, 1000 + fresh + 1, fresh);
        check("carry: a replace more than 30 minutes after the snapshot is not ours to carry over", c.finished && tv.mutes == 0 && tv.said("step=expired"));
        check("carry: the stages run in the lifetimes: 3 minutes for the sound, 30 for the accessibility; the backoff grows to 5 minutes",
            A11yList.AUDIO_LIFETIME_MS == 180_000 && A11yList.A11Y_LIFETIME_MS == 30 * 60_000L && A11yList.backoffMs(0) == 10_000 && A11yList.backoffMs(2) == 60_000 && A11yList.backoffMs(40) == 300_000);
    }
}
