package sa.urtv.home.phone;

import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * «تحكم من الجوال», the owner's switch. Off is the whole state of an app that has not been asked: no socket, no thread, nothing listening.
 * start() (the owner pressing «تشغيل») opens the server on the Wi-Fi address and a random high port; stop() closes it and forgets every
 * pairing offer and session. It stops by itself when the last phone's session ends, when nobody paired within 15 minutes, and when the pairing
 * is locked by five wrong tries. Nothing is written to disk. No Android in it: the Binder, Actions and Page come in from outside.
 */
public final class PhoneControl {
    public interface Clock { long now(); }

    public interface Listener { void changed(); }

    /** The page to serve; the text carries __NONCE__ where the script's nonce goes. font() gives one of PhoneApi.FONTS, or null when the APK has none. */
    public interface Page {
        String html();
        byte[] font(String name);
        /** «محفظتي»'s own page (/m), in the language of «اللغة»; null when this build has none */
        default String portfolioHtml() { return null; }
        /** 4.3: «صوري» from the phone (assets/phone/photos.html) */
        default String photosHtml() { return null; }
    }

    public enum Start { OK, NO_WIFI, FAILED, ALREADY_ON }

    public static final long PENDING_MS = 60_000, START_WINDOW_MS = 15 * 60_000L, TICK_MS = 5_000;
    private static final int PORT_LOW = 49152, PORT_HIGH = 65535;

    private final PhoneServer.Binder binder;
    private final Clock clock;
    private final Actions actions;
    private final Page page;
    private final Predicate<InetAddress> clientOk;
    private final SecureRandom rnd;
    private volatile PinGate pin = PinGate.NONE;
    private volatile Listener listener;

    // all below exist only while it is on
    private PhoneServer server;
    private ScheduledExecutorService timer;
    private Pairing pairing;
    private Sessions sessions;
    private Pending pending;
    private String ip, lastStop = "";
    private int port;
    private long startedAt;
    private boolean hadSession;
    private int lastLive;
    private final List<String[]> pinFails = new ArrayList<>();   // session id, count

    public PhoneControl(PhoneServer.Binder binder, Clock clock, Actions actions, Page page, Predicate<InetAddress> clientOk, SecureRandom rnd) {
        this.binder = binder;
        this.clock = clock;
        this.actions = actions;
        this.page = page;
        this.clientOk = clientOk;
        this.rnd = rnd;
    }

    public void setPinGate(PinGate gate) { pin = gate == null ? PinGate.NONE : gate; }

    PinGate pinGate() { return pin; }

    public void setListener(Listener l) { listener = l; }

    Actions actions() { return actions; }

    Page page() { return page; }

    long now() { return clock.now(); }

    void changed() {
        Listener l = listener;
        if (l != null) l.changed();
    }

    // ---- the switch

    public synchronized boolean isOn() { return server != null; }

    /** Why it last stopped: "", "owner", "ended" (the last phone left), "unused", "locked", "restart". */
    public synchronized String lastStop() { return lastStop; }

    /** Turns it on, listening on this IPv4 address (the Wi-Fi one). */
    public Start start(byte[] wifi) {
        synchronized (this) {
            if (server != null) return Start.ALREADY_ON;
            if (wifi == null || wifi.length != 4) return Start.NO_WIFI;
            PhoneServer s = null;
            InetAddress addr;
            try {
                addr = InetAddress.getByAddress(wifi);
            } catch (IOException e) {
                return Start.NO_WIFI;
            }
            ip = Guard.text(wifi);
            for (int attempt = 0; attempt < 20 && s == null; attempt++) {
                int p = PORT_LOW + rnd.nextInt(PORT_HIGH - PORT_LOW + 1);
                try {
                    s = new PhoneServer(binder.bind(addr, p), new PhoneApi(this, ip, p), clientOk);
                    port = p;
                } catch (IOException e) {
                    s = null;   // taken: another port
                }
            }
            if (s == null) return Start.FAILED;
            server = s;
            pairing = new Pairing(rnd);
            sessions = new Sessions(rnd);
            pending = null;
            hadSession = false;
            lastLive = 0;
            lastStop = "";
            startedAt = clock.now();
            pinFails.clear();
            pairing.offer(startedAt);
            timer = Executors.newSingleThreadScheduledExecutor(new java.util.concurrent.ThreadFactory() {
                @Override public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "phone-timer");
                    t.setDaemon(true);
                    return t;
                }
            });
            timer.scheduleWithFixedDelay(new Runnable() { @Override public void run() { tick(); } }, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
            s.start();
        }
        changed();
        return Start.OK;
    }

    private volatile Runnable onSessionEnd;

    /** Told when a phone's session ends here (revoked, logged out, its fifth wrong PIN) or control stops: «محفظتي»'s window closes. */
    public void setOnSessionEnd(Runnable r) { onSessionEnd = r; }

    private void sessionEnded() {
        Runnable r = onSessionEnd;
        if (r == null) return;
        try {
            r.run();
        } catch (RuntimeException e) {   // 4.3 S2: whatever the screen does with it, the server's own teardown is never stopped by it
            System.err.println("UrTVPhone: session end callback failed: " + e);
        }
    }

    /** 4.3 S2: the server is torn down FIRST, then the callback (inside try/catch): the LAN server never stays up because a callback threw. */
    public void stop(String reason) {
        synchronized (this) {
            if (server == null) return;
            server.close();
            server = null;
            if (timer != null) timer.shutdownNow();
            timer = null;
            if (sessions != null) sessions.clear();
            if (pairing != null) pairing.withdraw();
            pairing = null;
            sessions = null;
            pending = null;
            pinFails.clear();
            lastStop = reason;
        }
        sessionEnded();
        changed();
    }

    /** Stops a moment from now, so the answer being written to the phone gets out first. */
    synchronized void stopSoon(final String reason) {
        if (timer == null) return;
        try {
            timer.schedule(new Runnable() { @Override public void run() { stop(reason); } }, 300, TimeUnit.MILLISECONDS);
        } catch (RuntimeException ignored) {
            // the timer was shut down meanwhile: it is stopping anyway
        }
    }

    /** What the timer does every few seconds (public so a test can run it at a chosen time). 4.3 S2: anything it throws stops the server. */
    public void tick() {
        try {
            tickOnce();
        } catch (RuntimeException e) {
            System.err.println("UrTVPhone: tick failed, phone control stops: " + e);
            stop("error");
        }
    }

    private void tickOnce() {
        String stop = null;
        boolean redraw = false;
        synchronized (this) {
            if (server == null) return;
            long now = clock.now();
            int live = sessions.count(now);
            if (live != lastLive) { lastLive = live; redraw = true; }
            if (live > 0) hadSession = true;
            if (pending != null && pending.expired(now)) { pending = null; redraw = true; }
            if (hadSession && live == 0) stop = "ended";
            else if (!hadSession && now - startedAt >= START_WINDOW_MS) stop = "unused";
        }
        if (stop != null) stop(stop);
        else if (redraw) changed();
    }

    // ---- what the screen shows

    public synchronized String address() { return server == null ? null : "http://" + ip + ":" + port; }

    public synchronized String ip() { return ip; }

    public synchronized int port() { return port; }

    /** The offer to show: the current one, or a new one when it ran out or was used. Null when off or locked. */
    public synchronized Pairing.Offer offer() {
        if (server == null) return null;
        long now = clock.now();
        Pairing.Offer o = pairing.current(now);
        return o != null ? o : pairing.offer(now);
    }

    public synchronized String pairUrl(Pairing.Offer o) { return o == null || server == null ? null : address() + "/p#" + o.token; }

    /** Drops the offer on show (the screen with the code was left); a new one is made when it is shown again. */
    public synchronized void withdrawOffer() {
        if (pairing != null) pairing.withdraw();
    }

    public synchronized Pending pending() {
        if (pending != null && pending.expired(clock.now())) pending = null;
        return pending;
    }

    public synchronized void approve() {
        if (pending != null && pending.state == Pending.WAITING) { pending.state = Pending.APPROVED; pending.answeredAt = clock.now(); }
        changed();
    }

    public synchronized void deny() {
        if (pending != null && pending.state == Pending.WAITING) { pending.state = Pending.DENIED; pending.answeredAt = clock.now(); }
        changed();
    }

    public synchronized List<Sessions.Info> phones() {
        return sessions == null ? new ArrayList<Sessions.Info>() : sessions.list(clock.now());
    }

    public void revoke(String id) {
        synchronized (this) {
            if (sessions == null) return;
            sessions.revoke(id);
        }
        sessionEnded();
        tick();
    }

    // ---- what the API uses

    /** A waiting or answered request from a phone that showed a good token. */
    public static final class Pending {
        static final int WAITING = 0, APPROVED = 1, DENIED = 2;
        public final String id, ip;
        final long createdAt;
        int state = WAITING;
        /** 4.3 S11: when the TV answered; an answer the phone never comes to read goes after PENDING_MS (it no longer blocks a new phone) */
        long answeredAt = -1;
        Pending(String id, String ip, long createdAt) { this.id = id; this.ip = ip; this.createdAt = createdAt; }
        public boolean waiting() { return state == WAITING; }
        boolean expired(long now) { return state == WAITING ? now - createdAt >= PENDING_MS : answeredAt >= 0 && now - answeredAt >= PENDING_MS; }
    }

    enum Pair { OK, WRONG, LOCKED, BUSY, OFF }

    /** A phone presents a token or a code. OK carries the request id for pairStatus. */
    synchronized Object[] pair(String given, boolean isCode, String clientIp) {
        if (server == null) return new Object[] {Pair.OFF, null};
        long now = clock.now();
        if (pending != null && pending.expired(now)) pending = null;   // 4.3 S11: an unread answer expires too
        if (pending != null) return new Object[] {Pair.BUSY, null};
        Pairing.Result r = pairing.redeem(given, isCode, now);
        if (r == Pairing.Result.LOCKED) { stopSoon("locked"); return new Object[] {Pair.LOCKED, null}; }
        if (r != Pairing.Result.OK) return new Object[] {Pair.WRONG, null};
        StringBuilder id = new StringBuilder();
        byte[] b = new byte[16];
        rnd.nextBytes(b);
        for (byte x : b) id.append(String.format(java.util.Locale.ROOT, "%02x", x & 0xff));
        pending = new Pending(id.toString(), clientIp, now);
        return new Object[] {Pair.OK, pending.id};
    }

    /** The answer for a request id: "waiting", "denied", "gone", or "approved" with the new session's secret in [1]. */
    synchronized String[] pairStatus(String id, String clientIp) {
        if (server == null || pending == null || !Pairing.same(pending.id, id) || !pending.ip.equals(clientIp)) return new String[] {"gone", null};
        long now = clock.now();
        if (pending.expired(now)) { pending = null; changed(); return new String[] {"gone", null}; }   // 4.3 S11: an old approval issues no session
        if (pending.state == Pending.WAITING) return new String[] {"waiting", null};
        boolean approved = pending.state == Pending.APPROVED;
        pending = null;   // an answer is read once
        if (!approved) { changed(); return new String[] {"denied", null}; }
        String secret = sessions.issue(clientIp, now);
        if (secret == null) return new String[] {"full", null};
        hadSession = true;
        changed();
        return new String[] {"approved", secret};
    }

    synchronized boolean sessionOk(String secret, String clientIp) {
        return sessions != null && sessions.checkFrom(secret, clientIp, clock.now());
    }

    synchronized long secondsLeft(String secret) {
        if (sessions == null) return 0;
        long now = clock.now();
        for (Sessions.Info i : sessions.list(now)) {
            String id = sessions.idOf(secret, now);
            if (i.id.equals(id)) return Math.max(0, Math.min(Sessions.IDLE_MS - (now - i.lastUsed), Sessions.MAX_MS - (now - i.since)) / 1000);
        }
        return 0;
    }

    /** Ends the session this secret belongs to (the phone's own «خروج»). */
    void logout(String secret) {
        synchronized (this) {
            if (sessions == null) return;
            String id = sessions.idOf(secret, clock.now());
            if (id != null) sessions.revoke(id);
        }
        sessionEnded();
        tick();
    }

    /** A wrong PIN from this session; at the fifth the session ends. Returns true when it ended. */
    boolean pinFailed(String secret) {
        boolean ended = false;
        synchronized (this) {
            if (sessions == null) return false;
            String id = sessions.idOf(secret, clock.now());
            if (id == null) return false;
            String[] row = null;
            for (String[] r : pinFails) if (r[0].equals(id)) row = r;
            if (row == null) { row = new String[] {id, "0"}; pinFails.add(row); }
            row[1] = String.valueOf(Integer.parseInt(row[1]) + 1);
            if (Integer.parseInt(row[1]) >= 5) { sessions.revoke(id); ended = true; }
        }
        if (ended) {
            sessionEnded();
            tick();
        }
        return ended;
    }
}
