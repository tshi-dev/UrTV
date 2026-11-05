package sa.urtv.screensaver;

/**
 * Whether the screensaver is on the screen right now (the dream, or its full-screen preview), for the home's «الأذان على شاشة التوقف فقط»:
 * ConfigProvider answers it, in this same process, so a process that is gone is a screensaver that is not showing.
 */
final class Showing {
    private static int shown;

    private Showing() { }

    static synchronized void enter() { shown++; }

    static synchronized void leave() { if (shown > 0) shown--; }

    static synchronized boolean now() { return shown > 0; }
}
