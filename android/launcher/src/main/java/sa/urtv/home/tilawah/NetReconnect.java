package sa.urtv.home.tilawah;

/**
 * 4.4 radio reliability: what the player does when the TV's network comes back (a Wi-Fi drop, a router restart, a switch to Ethernet). No Android in
 * it (tests/NetReconnectRun.java runs it). Before, a station that lost its network waited for RadioPolicy's next timed try, up to a minute of
 * silence after the network was back; now it joins again at once, saying «يعيد الاتصال…», and a surah that failed for the same reason goes on from
 * its place. Nothing happens when nothing of ours is wanted, or when it plays fine (a change of network under a playing stream is the player's).
 */
final class NetReconnect {
    private NetReconnect() { }

    enum Act { NOTHING, RADIO, SURAH }

    /** a network that comes back this soon after the last reconnect is the same flap: one reconnect, not one per callback */
    static final long MIN_GAP_MS = 5_000;

    /**
     * @param source    Prefs.SOURCE_* now
     * @param wanted    the person wants it playing (no pause, no stop)
     * @param playing   it is playing now
     * @param trouble   an error is shown, or it is loading, or stalled
     * @param sinceLast ms since the last reconnect made here (Long.MAX_VALUE when none)
     */
    static Act onNetworkBack(int source, boolean wanted, boolean playing, boolean trouble, long sinceLast) {
        if (!wanted || sinceLast < MIN_GAP_MS) return Act.NOTHING;
        if (playing && !trouble) return Act.NOTHING;
        if (source == Prefs.SOURCE_RADIO) return Act.RADIO;
        if (source == Prefs.SOURCE_SURAH && trouble) return Act.SURAH;
        return Act.NOTHING;
    }
}
