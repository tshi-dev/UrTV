package sa.urtv.home;

/**
 * 4.4 beta 4 (reports: on weak TVs our screensaver used more processor and memory than the TV's own): the processor's class, beside the memory and
 * the core count of the tier (Ram.tier). Four cores or fewer that run at WEAK_KHZ or less at their fastest (the TVs that struggle: Cortex-A53 and
 * A55 sets at 1.1 to 1.4 GHz; a 1.5 GHz Amlogic S905X and up stay as they are) make the TV LOW, so «الوضع الخفيف» comes on by itself on
 * «تلقائي» («إيقاف» still wins). The fastest core is read from the kernel (cpufreq's cpuinfo_max_freq, no permission); 0 when it cannot be:
 * nothing changes then. Both apps carry the same file. No Android in it (tests/CpuClassRun.java).
 */
public final class CpuClass {
    private CpuClass() { }

    public static final long WEAK_KHZ = 1_400_000;
    public static final int WEAK_CORES = 4;

    /** the rule: a known core count and fastest clock, both at the weak class or under */
    public static boolean weak(int cores, long maxKhz) {
        return cores > 0 && cores <= WEAK_CORES && maxKhz > 0 && maxKhz <= WEAK_KHZ;
    }

    /** the share of one core used between two readings (Process.getElapsedCpuTime, ms; the wall clock, ms), in whole percent; -1 when it cannot be told */
    public static int percent(long cpuMs, long wallMs) {
        if (cpuMs < 0 || wallMs <= 0) return -1;
        return (int) Math.min(1000, Math.round(cpuMs * 100.0 / wallMs));
    }

    private static volatile long fastest = -1;

    /** the fastest core's top clock in kHz, read once; 0 when the kernel does not say */
    public static long maxKhz() {
        long f = fastest;
        if (f >= 0) return f;
        f = 0;
        for (int i = 0; i < 16; i++) {
            java.io.File file = new java.io.File("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq");
            if (!file.isFile()) { if (i > 0) break; else continue; }
            f = Math.max(f, parse(file));
        }
        fastest = f;
        return f;
    }

    static long parse(java.io.File file) {
        try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String s = r.readLine();
            return s == null ? 0 : Math.max(0, Long.parseLong(s.trim()));
        } catch (java.io.IOException | RuntimeException e) {
            return 0;
        }
    }
}
