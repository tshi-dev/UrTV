package sa.urtv.home.tilawah;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import sa.urtv.home.LastGood;

/**
 * For tests/place_choice_test.py: the adhan alarm when the screensaver's settings cannot be read (the TV kills its process and refuses to start
 * it from the background). A small device made of the SAME pure parts the home uses (LastGood for the settings read, PlaceMemory for the place,
 * SchedIdentity for what the alarm was set from), driven through the cases of the review: a failed read never moves the alarm, an old copy never
 * becomes the place, the alarm is recorded from where it was really set (so an upgrade or a boot with no read is corrected by the first good one).
 */
public final class PlaceMemoryRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    static final String CAIRO = "city=c&lat=30.0444&lon=31.2357&tz=Africa%2FCairo&adj=2,0,0,0,0";
    static final String KUWAIT = "city=k&lat=29.3759&lon=47.9774&tz=Asia%2FKuwait";

    static final class FakeStore implements PlaceMemory.Store {
        String saved;
        @Override public String load() { return saved; }
        @Override public void save(String f) { saved = f; }
    }

    /** the home, as Place/AdhanSync/ConfigSnapshot/AdhanScheduler wire the pure parts */
    static final class Device {
        final FakeStore store;
        PlaceMemory mem = new PlaceMemory();
        final LastGood<String> snapshot = new LastGood<>();
        boolean providerUp = true;
        String providerFragment;
        String recorded = "";        // sched_identity
        String scheduledTimes = "";  // what the alarm is for
        long ticket, now;

        Device(FakeStore store) { this.store = store; }

        String answer() { return providerUp ? providerFragment : null; }

        /** Place.get(c): a fresh read of the provider */
        PlaceChoice.Pick placeGet() { return mem.resolve(store, answer(), ++ticket); }

        static String identity(PlaceChoice.Pick p) {
            if (p.fragment == null) return SchedIdentity.of(true, "");
            PlaceFragment f = PlaceFragment.parse(p.fragment);
            return SchedIdentity.of(false, PrayerMath.identity(f.lat, f.lng, f.tz, PrayerAdjust.text(f.adj), PrayerMath.methodFor(f.tz, f.pm)));
        }

        static String times(PlaceChoice.Pick p) {
            if (p.fragment == null) return "bundled";
            PlaceFragment f = PlaceFragment.parse(p.fragment);
            long[] t = PrayerMath.compute(2026, 10, 29, f.lat, f.lng, f.tz, false, PrayerMath.methodFor(f.tz, f.pm));
            PrayerAdjust.shift(t, f.adj);
            return Arrays.toString(t);
        }

        /** AdhanScheduler.schedule(c, place): the one writer of the identity */
        void schedule(PlaceChoice.Pick p) { recorded = identity(p); scheduledTimes = times(p); }

        /** AdhanSync.reschedule (the shell, the phone) and every other schedule(c): the place read now */
        void scheduleNow() { schedule(placeGet()); }

        /** AdhanSync.onConfig(c, fragment) */
        void onConfig(String fragment) {
            PlaceChoice.Pick p = mem.resolve(store, fragment, ++ticket);
            if (fragment == null) return;
            if (SchedIdentity.due(recorded, identity(p))) schedule(p);
        }

        /** the home's minute tick: ConfigSnapshot.read, then placeFragment() (null for an old copy) into onConfig; `passStale` is the bug */
        void tick(boolean passStale) {
            now += 10_000;
            LastGood.Read<String> r = snapshot.read(now, 3000, () -> answer());
            String fragment = r.value;
            if (r.stale && !passStale) fragment = null;
            onConfig(fragment);
        }
    }

    public static void main(String[] args) {
        // LastGood itself
        LastGood<String> lg = new LastGood<>();
        String[] box = {"a"};
        LastGood.Read<String> r1 = lg.read(0, 3000, () -> box[0]);
        box[0] = null;
        LastGood.Read<String> r2 = lg.read(1000, 3000, () -> box[0]);
        LastGood.Read<String> r3 = lg.read(5000, 3000, () -> box[0]);
        check("LastGood: a fresh answer is not stale; inside the share window the same one is handed out", "a".equals(r1.value) && !r1.stale && "a".equals(r2.value) && !r2.stale);
        check("LastGood: a failed read hands out the last good value, marked stale", "a".equals(r3.value) && r3.stale);
        check("LastGood: with nothing ever read the value is null (the caller has its defaults)", new LastGood<String>().read(0, 3000, () -> null).value == null);
        box[0] = "b";
        LastGood.Read<String> r4 = lg.read(9000, 3000, () -> box[0]);
        check("LastGood: the next answer replaces it, fresh again", "b".equals(r4.value) && !r4.stale);

        // 1 (review, high): a stale copy never becomes the place or moves the alarm
        Device d = new Device(new FakeStore());
        d.providerFragment = CAIRO;
        d.tick(false);                                   // the tick reads Cairo: alarm and place Cairo
        String cairoTimes = d.scheduledTimes;
        d.providerFragment = KUWAIT;                     // the owner sets Kuwait from the phone: reschedule reads Kuwait
        d.scheduleNow();
        String kuwaitTimes = d.scheduledTimes, kuwaitIdentity = d.recorded;
        d.providerUp = false;                            // the TV refuses the next read
        d.tick(false);
        d.tick(false);
        check("the alarm for Cairo was Cairo's and then Kuwait's (the two differ)", !cairoTimes.equals(kuwaitTimes) && !cairoTimes.equals("bundled"));
        check("a failed tick after the owner's new city leaves the alarm for Kuwait (an old Cairo copy is not taken for an answer)",
            d.scheduledTimes.equals(kuwaitTimes) && d.recorded.equals(kuwaitIdentity) && KUWAIT.equals(d.store.saved));
        Device broken = new Device(new FakeStore());
        broken.providerFragment = CAIRO; broken.tick(true);
        broken.providerFragment = KUWAIT; broken.scheduleNow();
        broken.providerUp = false; broken.tick(true);
        check("(the same with the old copy passed on as an answer: the alarm goes back to Cairo and the place is overwritten: what the review found)", !broken.scheduledTimes.equals(kuwaitTimes) && CAIRO.equals(broken.store.saved));
        d.providerUp = true;
        d.tick(false);
        check("when the screensaver answers again nothing moves for the same city", d.scheduledTimes.equals(kuwaitTimes) && KUWAIT.equals(d.store.saved));

        // 2 (review, medium): an upgrade or a boot with no good read yet
        FakeStore empty = new FakeStore();
        Device up = new Device(empty);
        up.recorded = PrayerMath.identity(30.0444, 31.2357, "Africa/Cairo", "2,0,0,0,0", PrayerMath.methodFor("Africa/Cairo", null));   // an older version set it for Cairo
        up.providerFragment = CAIRO;
        up.providerUp = false;                           // MY_PACKAGE_REPLACED in the background: the TV refuses; the new place prefs are empty
        up.scheduleNow();
        check("no read ever and a schedule: the bundled place, recorded as '' (so no real place matches it)", "bundled".equals(up.scheduledTimes) && "".equals(up.recorded));
        up.providerUp = true;
        up.tick(false);
        check("the first good read of Cairo sees the mismatch and sets the alarm for Cairo, recording it", up.scheduledTimes.equals(cairoTimes) && !up.recorded.isEmpty() && CAIRO.equals(empty.saved));
        String before = up.scheduledTimes;
        up.tick(false);
        check("and then it is left alone", up.scheduledTimes.equals(before));
        // a TV already stuck (identity says Cairo, the alarm is not): a schedule from any place rewrites the identity, so it is repaired by the next good read
        Device stuck = new Device(new FakeStore());
        stuck.providerFragment = CAIRO; stuck.providerUp = true; stuck.tick(false);
        stuck.providerUp = false; stuck.mem = new PlaceMemory(); stuck.store.saved = null;    // a restart with no stored place
        stuck.scheduleNow();                                                                  // an alarm fired: scheduled from the bundled place
        stuck.providerUp = true; stuck.tick(false);
        check("an alarm scheduled from the bundled place is put right by the next good read", stuck.scheduledTimes.equals(cairoTimes));

        // 3 (review, low): reads end out of order
        PlaceMemory m = new PlaceMemory();
        FakeStore fs = new FakeStore();
        PlaceChoice.Pick newer = m.resolve(fs, KUWAIT, 5);
        PlaceChoice.Pick older = m.resolve(fs, CAIRO, 3);   // started earlier, ended later
        check("an older read that ends after a newer one does not put its city back", !newer.remembered && KUWAIT.equals(newer.fragment) && older.remembered && KUWAIT.equals(older.fragment) && KUWAIT.equals(fs.saved));
        check("a later read replaces it", CAIRO.equals(m.resolve(fs, CAIRO, 6).fragment) && CAIRO.equals(fs.saved));

        // the rest
        FakeStore keep = new FakeStore();
        new PlaceMemory().resolve(keep, CAIRO, 1);
        PlaceMemory restarted = new PlaceMemory();
        check("after a restart (a new process) a failed read uses the place that was kept", CAIRO.equals(restarted.resolve(keep, null, 1).fragment) && restarted.resolve(keep, null, 2).remembered);
        Device fresh = new Device(new FakeStore());
        fresh.providerUp = false;
        fresh.scheduleNow();
        check("the very first install with nothing read and no provider uses the bundled place", fresh.placeGet().fragment == null && "bundled".equals(fresh.scheduledTimes));
        PlaceMemory log = new PlaceMemory();
        check("the log line is for the start of a streak of failed reads only", log.streakStarted(true, true) && !log.streakStarted(true, true) && !log.streakStarted(false, false) && log.streakStarted(true, true));
        check("a failed read of a never-read install is no streak (nothing remembered to use)", !new PlaceMemory().streakStarted(true, false));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
