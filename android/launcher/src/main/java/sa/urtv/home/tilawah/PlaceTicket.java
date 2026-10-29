package sa.urtv.home.tilawah;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The order in which reads of the screensaver's settings were STARTED. A ticket is taken before a read begins and travels with what it
 * returned (ConfigSnapshot keeps it, a cached or old copy keeps the one it was read with), so a read started earlier can never replace the place
 * a later one gave (PlaceMemory), however late it is used.
 */
public final class PlaceTicket {
    private static final AtomicLong TICKETS = new AtomicLong();

    private PlaceTicket() { }

    public static long next() { return TICKETS.incrementAndGet(); }
}
