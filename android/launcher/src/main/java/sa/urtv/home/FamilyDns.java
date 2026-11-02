package sa.urtv.home;

/**
 * «تصفية المواقع (DNS)»: Android's Private DNS pointed at AdGuard Family, and put back exactly as it was. Pure logic (no Android
 * imports) so tests/family_dns_test.py can run it with a fake {@link Ports}; FamilyDnsAndroid is the real one.
 *
 * Rules, each one a way this could leave a TV without internet or with a lost setting:
 *  - nothing changes until HOST:PORT answers, and the two original values are recorded BEFORE the first write;
 *  - a record already there is never overwritten (it would then hold our own values, and "off" would keep them);
 *  - "was not set" (null) is not the same as "could not be read" (UNREADABLE): an unreadable value is never recorded;
 *  - "off" puts back exactly what was recorded; a null original is deleted, not written as a default;
 *  - after the change, in this order: Private DNS must show active for HOST (VALIDATE_MS), then a FRESH random name must get an answer
 *    from the resolver in time (up to FRESH_TRIES tries of FRESH_MS each, a new name every time: a freshly set strict Private DNS fails
 *    lookups until its TLS check is done; see {@link #resolverAnswers}), then the internet must still work (HTTP_MS), together
 *    INTERNET_MS (40 s at most, each step ends as soon as it succeeds), else the originals go back at once;
 *  - the check is crash safe: a flag (checkingSince) is saved before the first write and cleared only after ENABLED or a revert;
 *    a process that starts (or a boot) with the flag set calls recover(), which puts the originals back;
 *  - "off" puts back only while the value is still ours; if it was changed from elsewhere the record is dropped and nothing is written;
 *  - Private DNS exists only from API 28: below it enable() is UNSUPPORTED and the state is never ON.
 */
public final class FamilyDns {
    public static final String HOST = "family.adguard-dns.com";
    public static final int PORT = 853;
    public static final int PROBE_MS = 5000;
    public static final int INTERNET_MS = 40000;
    /** INTERNET_MS = waiting for Private DNS to show active + up to FRESH_TRIES fresh names (FRESH_MS each) + fresh https requests. */
    public static final int VALIDATE_MS = 12000, FRESH_MS = 6000, FRESH_TRIES = 3, HTTP_MS = INTERNET_MS - VALIDATE_MS - FRESH_TRIES * FRESH_MS;
    /** A "no such name" answer that comes back faster than this means the resolver answered (DNS works); slower, or none, is broken.
     *  Wide on purpose: the first query through a new DNS-over-TLS connection pays the TLS handshake. */
    public static final int FAST_ANSWER_MS = 6000;
    public static final int MIN_SDK = 28;
    public static final String KEY_MODE = "private_dns_mode";
    public static final String KEY_SPEC = "private_dns_specifier";
    public static final String MODE_ON = "hostname";
    /** getGlobal answer for "the value exists but could not be read" (null means "not set"). */
    public static final String UNREADABLE = "\u0000unreadable";

    /** The originals; a null field = it was not set. */
    public static final class Record {
        public final String mode, specifier;
        public Record(String mode, String specifier) { this.mode = mode; this.specifier = specifier; }
    }

    public interface Ports {
        boolean canWriteSecure();
        /** TCP connect to host:port within timeoutMs. */
        boolean probe(String host, int port, int timeoutMs);
        /** The value, null if not set, UNREADABLE if it could not be read. */
        String getGlobal(String key);
        boolean putGlobal(String key, String value);
        boolean deleteGlobal(String key);
        /** The platform level (Build.VERSION.SDK_INT). */
        int sdkInt();
        /** Within ms the active network shows Private DNS active with server name host (not a cached answer). */
        boolean privateDnsValidated(String host, int ms);
        /** Sleeps ms (a port so the tests do not wait). */
        void sleep(int ms);
        /** Resolves a unique random name (nothing can have cached it) through the system resolver, waiting at most waitMs. */
        Lookup lookupFreshName(int waitMs);
        /** A fresh, uncached request to connectivitycheck.gstatic.com succeeds within totalMs. */
        boolean internetWorks(int totalMs);
        /** When the check began (ms), 0 = no check in progress. Saved before the first write. */
        long checkingSince();
        boolean markChecking();
        void clearChecking();
        /** null = nothing recorded. */
        Record loadRecord();
        boolean saveRecord(Record r);
        void clearRecord();
    }

    /** How one lookup of a fresh name ended; elapsedMs = how long it took (or the wait, for TIMEOUT). */
    public static final class Lookup {
        public enum Kind { RESOLVED, NOT_FOUND, FAILED, TIMEOUT }
        public final Kind kind;
        public final long elapsedMs;
        public Lookup(Kind kind, long elapsedMs) { this.kind = kind; this.elapsedMs = elapsedMs; }
    }

    /**
     * The decision on a fresh-name lookup. An answer (an address, or "no such name" = NXDOMAIN) that arrives in time means the resolver
     * answered; a timeout, a slow failure or any other error means DNS is broken. This proves the resolver answers names it has never
     * seen through the new setting. It cannot prove the filtering is AdGuard's: an NXDOMAIN looks the same from any resolver.
     */
    public static boolean resolverAnswers(Lookup l) {
        if (l == null) return false;
        switch (l.kind) {
            case RESOLVED: return true;
            case NOT_FOUND: return l.elapsedMs < FAST_ANSWER_MS;
            default: return false;
        }
    }

    /**
     * Up to FRESH_TRIES lookups of new random names, each given FRESH_MS (a try that failed early waits out the rest of its slot, so the
     * tries span the time Private DNS needs to validate); true on the first answer. Still false when the resolver never answers.
     */
    private boolean freshNameAnswers() {
        for (int i = 0; i < FRESH_TRIES; i++) {
            Lookup l = p.lookupFreshName(FRESH_MS);
            if (resolverAnswers(l)) return true;
            long used = l == null ? 0 : l.elapsedMs;
            if (i < FRESH_TRIES - 1 && used < FRESH_MS) p.sleep((int) (FRESH_MS - used));
        }
        return false;
    }

    /**
     * The validated Private DNS servers of the active network, when the TV can say how many (LinkProperties.getValidatedPrivateDnsServers, hidden
     * API on some versions, reached by reflection): none validated means the strict setting is not working, even if the setting propagated.
     * null = this TV cannot say, which is not a reason to refuse.
     */
    public static boolean validatedServersOk(Integer count) {
        return count == null || count > 0;
    }

    /**
     * What an UnknownHostException means. Only the resolver's own «no such name / no data» (a GaiException with EAI_NODATA or EAI_NONAME as the
     * cause) is a real answer; any other cause (a failed or refused connection, a bad certificate, a system error) is a broken resolver.
     */
    public static Lookup.Kind unknownHostKind(boolean causeIsGai, int gaiError, int eaiNodata, int eaiNoname) {
        return causeIsGai && (gaiError == eaiNodata || gaiError == eaiNoname) ? Lookup.Kind.NOT_FOUND : Lookup.Kind.FAILED;
    }

    /**
     * Fallback for a TV that does not let the app read GaiException.error (hidden field): the resolver's own «no such name / no data» text.
     * A timeout or a refused connection has other texts («Temporary failure in name resolution», …), so those stay failures.
     */
    public static boolean messageIsNoSuchName(String m) {
        if (m == null) return false;
        String s = m.toLowerCase(java.util.Locale.ROOT);
        return s.contains("no address associated with hostname") || s.contains("name or service not known") || s.contains("eai_nodata") || s.contains("eai_noname");
    }

    public enum Result { ENABLED, ALREADY_ON_BY_OTHER, NEEDS_GRANT, NO_ROUTE, UNREADABLE_VALUE, NOT_RECORDED, NOT_TAKEN, REVERTED, RESTORED, NOTHING_TO_RESTORE, RESTORE_FAILED, UNSUPPORTED, CHANGED_BY_SOMEONE_ELSE }
    public enum State { ON, OFF, UNKNOWN }

    public static final class Status {
        public final State state;
        public final String by;   // "us" (we recorded originals), "other" (on, but not set by us), "none"
        Status(State s, String by) { this.state = s; this.by = by; }
        @Override public String toString() { return state.name().toLowerCase() + " (" + by + ")"; }
    }

    private final Ports p;
    public FamilyDns(Ports ports) { p = ports; }

    public boolean supported() { return p.sdkInt() >= MIN_SDK; }

    public Status status() {
        if (!supported()) return new Status(State.OFF, "none");
        String mode = p.getGlobal(KEY_MODE), spec = p.getGlobal(KEY_SPEC);
        if (UNREADABLE.equals(mode) || UNREADABLE.equals(spec)) return new Status(State.UNKNOWN, p.loadRecord() != null ? "us" : "none");
        boolean on = MODE_ON.equals(mode) && HOST.equals(spec);
        boolean ours = p.loadRecord() != null;
        return on ? new Status(State.ON, ours ? "us" : "other") : new Status(State.OFF, "none");
    }

    public Result enable() {
        if (!supported()) return Result.UNSUPPORTED;
        if (!p.canWriteSecure()) return Result.NEEDS_GRANT;
        if (!p.probe(HOST, PORT, PROBE_MS)) return Result.NO_ROUTE;
        Record rec = p.loadRecord();
        boolean madeRecord = false;
        if (rec == null) {
            String mode = p.getGlobal(KEY_MODE), spec = p.getGlobal(KEY_SPEC);
            if (UNREADABLE.equals(mode) || UNREADABLE.equals(spec)) return Result.UNREADABLE_VALUE;
            if (MODE_ON.equals(mode) && HOST.equals(spec)) return Result.ALREADY_ON_BY_OTHER;   // nothing of ours to put back
            rec = new Record(mode, spec);
            if (!p.saveRecord(rec)) return Result.NOT_RECORDED;
            madeRecord = true;
        }
        if (!p.markChecking()) {   // no flag, no write: a crash could not be undone
            if (madeRecord) p.clearRecord();
            return Result.NOT_RECORDED;
        }
        boolean wrote = p.putGlobal(KEY_SPEC, HOST) && p.putGlobal(KEY_MODE, MODE_ON);
        if (!wrote || !MODE_ON.equals(p.getGlobal(KEY_MODE)) || !HOST.equals(p.getGlobal(KEY_SPEC))) {
            if (!restore(rec)) return Result.RESTORE_FAILED;   // the old values are not back: the record and the flag stay (recover() and «off» try again)
            p.clearRecord(); p.clearChecking();
            return Result.NOT_TAKEN;
        }
        if (!p.privateDnsValidated(HOST, VALIDATE_MS) || !freshNameAnswers() || !p.internetWorks(HTTP_MS)) {
            if (!restore(rec)) return Result.RESTORE_FAILED;   // REVERTED only once the old values were read back
            p.clearRecord(); p.clearChecking();
            return Result.REVERTED;
        }
        p.clearChecking();
        return Result.ENABLED;
    }

    public Result disable() {
        Record rec = p.loadRecord();
        if (rec == null) return Result.NOTHING_TO_RESTORE;
        String mode = p.getGlobal(KEY_MODE), spec = p.getGlobal(KEY_SPEC);
        if (UNREADABLE.equals(mode) || UNREADABLE.equals(spec)) return Result.RESTORE_FAILED;   // cannot tell: keep the record
        if (!MODE_ON.equals(mode) || !HOST.equals(spec)) {   // no longer ours: leave it as it is
            p.clearRecord();
            p.clearChecking();
            return Result.CHANGED_BY_SOMEONE_ELSE;
        }
        if (!restore(rec)) return Result.RESTORE_FAILED;   // the record stays: "off" can be tried again
        p.clearRecord();
        p.clearChecking();
        return Result.RESTORED;
    }

    /** A check was cut short (crash, reboot): checkingSince != 0 -> the recorded originals go back at once. */
    public Result recover(long checkingSince) {
        if (checkingSince == 0) return Result.NOTHING_TO_RESTORE;
        Record rec = p.loadRecord();
        if (rec == null) { p.clearChecking(); return Result.NOTHING_TO_RESTORE; }
        if (!restore(rec)) return Result.RESTORE_FAILED;   // record and flag stay: tried again at the next start
        p.clearRecord();
        p.clearChecking();
        return Result.RESTORED;
    }

    public Result recover() { return recover(p.checkingSince()); }

    /** Exactly the recorded values, mode first (the specifier alone does nothing); true only if read back equal. */
    private boolean restore(Record r) {
        boolean ok = put(KEY_MODE, r.mode);
        ok &= put(KEY_SPEC, r.specifier);
        return ok && same(p.getGlobal(KEY_MODE), r.mode) && same(p.getGlobal(KEY_SPEC), r.specifier);
    }

    private boolean put(String key, String v) { return v == null ? p.deleteGlobal(key) : p.putGlobal(key, v); }
    private static boolean same(String a, String b) { return a == null ? b == null : a.equals(b); }

    /** Plain Arabic for the person (the settings row); the screen says it in «اللغة» with S.tv (the words are marked for the table). */
    public static String arabic(Result r) {
        switch (r) {
            case ENABLED: return S.t("تصفية المواقع مفعّلة");
            case ALREADY_ON_BY_OTHER: return S.t("الحماية شغّالة من قبل بإعداد ثاني، ما غيّرنا شي");
            case NEEDS_GRANT: return S.t("يحتاج تفعيل من الكمبيوتر");
            case NO_ROUTE: return S.t("ما قدرنا نوصل لخدمة الحماية، ما تغيّر شي");
            case UNREADABLE_VALUE: return S.t("ما قدرنا نقرأ إعداد التلفزيون الحالي، ما تغيّر شي");
            case NOT_RECORDED: return S.t("ما قدرنا نحفظ الإعداد القديم، ما تغيّر شي");
            case NOT_TAKEN: return S.t("الإعداد ما انحفظ فرجّعنا القديم");
            case REVERTED: return S.t("الإنترنت وقف فرجّعنا الإعداد القديم");
            case RESTORED: return S.t("تصفية المواقع متوقفة، رجع الإعداد القديم");
            case NOTHING_TO_RESTORE: return S.t("تصفية المواقع متوقفة");
            case UNSUPPORTED: return S.t("غير مدعومة في هذا التلفزيون (أندرويد قديم)");
            case CHANGED_BY_SOMEONE_ELSE: return S.t("الإعداد تغيّر من مكان آخر، تركناه كما هو");
            default: return S.t("ما قدرنا نرجّع الإعداد القديم، جرّب مرة ثانية");
        }
    }

    public static String arabic(Status s) {
        if (s.state == State.UNKNOWN) return S.n("غير معروفة");
        return s.state == State.ON ? S.n("مفعّلة") : S.n("متوقفة");
    }
}
