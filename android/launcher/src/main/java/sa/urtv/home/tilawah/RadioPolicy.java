package sa.urtv.home.tilawah;

/**
 * 4.3 (a user report on 4.2: «إذاعة القرآن تشتغل كذا ثواني بعدين يكتب صيغة لا تدعم بعدين يكتب تعذّر التشغيل»): what a live station does after
 * a failure, with no Android in it (tests/radio_policy_test.py runs it, with a scripted local server for StreamUrl).
 *
 * - A station that played and then stopped (its stream dropped, a server hiccup mid-stream, a segment that failed, the end of an Icecast
 *   stream) is joined again quietly, after StreamUrl's growing wait, up to RECONNECTS times in WINDOW_MS: «جارٍ إعادة المحاولة…», no fault.
 * - A station that does not start is tried TRIES times in all (StreamUrl.TRIES), then its second source when it has one (stations.json «alt»),
 *   tried the same way.
 * - Only then is a fault said, and it is «تعذّر الاتصال بالبث» unless the station never played and every try said its format is not one the
 *   TV can play («صيغة البث غير مدعومة»). The «server died» of the platform player stays its own words.
 */
final class RadioPolicy {
    private RadioPolicy() { }

    static final int RECONNECTS = 5;
    static final long WINDOW_MS = 5 * 60_000L;

    enum Step { RECONNECT, RETRY, ALT, FAIL }

    /** The station's state for one pick: what has happened since the person chose it. */
    static final class State {
        boolean played;           // it gave sound at least once since the pick
        int tries;                // failed starts on the current source
        boolean onAlt;            // the second source is playing now
        long[] reconnectsAt = new long[RECONNECTS];
        int reconnects;
        boolean formatEveryTime = true;   // every failure so far said malformed or unsupported
        int failures;             // every failure since the pick, on both sources (the line «توقف البث بعد N محاولات» and the waits)
        int patient;              // failures since the policy began to keep trying past the old limit (the second source is swapped every StreamUrl.TRIES of them)
        boolean spent;            // the reconnects of the window are used up: a short sound no longer clears the tries
    }

    /** Started with sound: a later failure is a reconnect, not a failed start. */
    static void started(State s) {
        s.played = true;
        if (!s.spent) s.tries = 0;   // a stream that keeps playing a moment and dropping does not get its tries back (it would loop for ever)
    }

    /** What to do after a failure (what, extra: MediaPlayer's numbers, ExoPlayback maps its own to them); hasAlt: stations.json has «alt». */
    static Step after(State s, int what, int extra, boolean hasAlt, long now) {
        s.failures++;
        String code = PlayerFault.code(what, extra);
        if (!code.equals("malformed") && !code.equals("unsupported")) s.formatEveryTime = false;
        if (s.played) {
            int recent = 0;
            for (int i = 0; i < Math.min(s.reconnects, RECONNECTS); i++) if (now - s.reconnectsAt[i] < WINDOW_MS) recent++;
            if (recent < RECONNECTS) {
                s.spent = false;
                s.reconnectsAt[s.reconnects % RECONNECTS] = now;
                s.reconnects++;
                return Step.RECONNECT;
            }
            s.spent = true;
        }
        s.tries++;
        if (StreamUrl.again(s.tries)) return Step.RETRY;
        if (hasAlt && !s.onAlt) {
            s.onAlt = true;
            s.tries = 0;
            return Step.ALT;
        }
        return Step.FAIL;
    }

    /** 4.3.1 a station is never given up while the person wants it: the waits are 2, 5, 10, 20 s, then every 30 s (failures: counted since the pick) */
    static long retryMs(int failures) {
        long[] steps = {2_000, 5_000, 10_000, 20_000};
        return failures <= 0 ? steps[0] : failures <= steps.length ? steps[failures - 1] : 30_000;
    }

    /** a fault is final only for a stream that never played and whose every answer said its format is not playable; every other failure is tried again */
    static boolean finalFault(State s, int what) { return what == PlayerFault.SERVER_DIED || (!s.played && s.formatEveryTime); }

    /** after RadioPolicy.after said FAIL for a fault that is not final: try again, and every StreamUrl.TRIES failures take the other source (back and forth) when there is one */
    static Step patient(State s, boolean hasAlt) {
        s.patient++;
        return hasAlt && s.patient % StreamUrl.TRIES == 0 ? Step.ALT : Step.RETRY;
    }

    /** The wait before the next try (the schedule above). */
    static long waitMs(State s) { return retryMs(s.failures); }

    /** The words of a station's fault, said only at Step.FAIL. */
    static String message(State s, int what, int extra) {
        String code = PlayerFault.code(what, extra);
        if (code.equals("server_died")) return sa.urtv.home.S.t("توقف المشغّل");
        if (!s.played && s.formatEveryTime && (code.equals("malformed") || code.equals("unsupported"))) return sa.urtv.home.S.t("صيغة البث غير مدعومة");
        return sa.urtv.home.S.t("تعذّر الاتصال بالبث، تحقق من الإنترنت");
    }

    /**
     * ExoPlayer's error code as MediaPlayer's «extra» (ExoPlayback hands MusicService MediaPlayer-shaped errors): 2xxx input/output (2002 the
     * timeout), 3xxx the stream's format could not be read, 4xxx it could not be decoded, anything else unknown (0).
     */
    static int extraForExo(int code) {
        if (code == 2002) return PlayerFault.TIMED_OUT;
        if (code >= 2000 && code < 3000) return PlayerFault.IO;
        if (code >= 3000 && code < 4000) return PlayerFault.MALFORMED;
        if (code >= 4000 && code < 5000) return PlayerFault.UNSUPPORTED;
        return 0;
    }
}
