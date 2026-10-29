package sa.urtv.home;

import java.util.Locale;

/**
 * What «مركز التحكم» says and decides, with no Android in it (tests/ControlTextRun.java runs it). Numbers are written with western
 * digits here and the screen passes them through Digits.of, like every other number of the home screen.
 */
final class ControlText {
    private ControlText() {}

    /** The panel's name: on the pill, over the panel, and in the family lock's list. */
    static final String NAME = "مركز التحكم";

    /**
     * «انعكاس الشاشة»: the TV's own screen-mirroring (Miracast) receiver, which our cleaning keeps on (optimize/lists/keep.txt) but people
     * cannot find, and which our home may hide. The packages are the ones our lists name (keep.txt and names-ar.txt: com.tcl.miracast; the
     * community lists of the TVs that have them: Allwinner projectors, Homwee/CHiQ, Xiaomi, MediaTek), in this order; the tile opens the
     * first one installed that has a screen to open, and is not there when none does. Each one is in the manifest's <queries> too.
     */
    static final String[] MIRROR_APPS = {
        "com.tcl.miracast",                  // TCL: «عرض الشاشة (Miracast)», names-ar.txt, keep.txt
        "com.softwinner.miracastReceiver",   // Allwinner (projectors such as the HY300 Pro)
        "com.homwee.mmp.screenshare",        // Homwee boards (CHiQ, RCA, Aiwa, OK)
        "com.xiaomi.mitv.smartshare",        // Xiaomi Mi TV
        "com.mediatek.wfd",                  // MediaTek's Wi-Fi Display (Miracast) receiver
    };
    static final String MIRROR = "انعكاس الشاشة";
    /** the one line under the tile's name: what the person does on the phone */
    static final String MIRROR_HINT = "من الجوال: شارك الشاشة واختر التلفزيون";

    /** Whether this TV can open a package (it is installed, enabled, and has a screen to open). */
    interface Opens { boolean opens(String pkg); }

    /** The mirroring app the tile opens: the first of MIRROR_APPS this TV can open, or null (then there is no tile). */
    static String mirrorApp(Opens tv) {
        for (String p : MIRROR_APPS) if (tv.opens(p)) return p;
        return null;
    }

    /** Mbps for the person: one decimal below 100, whole above; «٫» is the decimal mark (Digits.of turns it into «.» for latin digits) */
    static String mbps(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0) v = 0;
        String n = v < 100 ? String.format(Locale.ROOT, "%.1f", v) : String.format(Locale.ROOT, "%.0f", v);
        return n.replace('.', '٫') + " ميجابت/ث";
    }

    /** the Wi-Fi's signal as 0..4 bars from its dBm; below -100 is 0, from -55 up is 4 (the system's own five-step scale) */
    static int signalLevel(int rssi) {
        if (rssi <= -100) return 0;
        if (rssi >= -55) return 4;
        return (int) Math.round((rssi + 100) * 4.0 / 45);
    }

    static String signal(int level) {
        switch (Math.max(0, Math.min(4, level))) {
            case 0: return "إشارة ضعيفة جدًا";
            case 1: return "إشارة ضعيفة";
            case 2: return "إشارة متوسطة";
            case 3: return "إشارة جيدة";
            default: return "إشارة ممتازة";
        }
    }

    /** the first line of the network tile: the cable, the Wi-Fi by name, or not connected (the same words as PillText.network) */
    static String network(boolean wired, boolean wifi, String ssid) { return PillText.network(wired, wifi, ssid); }

    /** the second line: the signal when it is Wi-Fi and the TV says it (rssi 0 or more = unknown), else nothing */
    static String networkDetail(boolean wired, boolean wifi, int rssi) {
        if (wired) return "اتصال ثابت";
        if (!wifi) return "افتح إعدادات الشبكة";
        return rssi >= 0 ? "افتح إعدادات الشبكة" : signal(signalLevel(rssi));
    }

    // The values of android.media.AudioDeviceInfo.TYPE_*, so the names can be tested without Android.
    static final int BUILTIN_SPEAKER = 2, WIRED_HEADSET = 3, WIRED_HEADPHONES = 4, LINE_ANALOG = 5, LINE_DIGITAL = 6, BLUETOOTH_SCO = 7,
            BLUETOOTH_A2DP = 8, HDMI = 9, HDMI_ARC = 10, USB_DEVICE = 11, USB_ACCESSORY = 12, USB_HEADSET = 22, HDMI_EARC = 29;

    /** the name of an output the TV reports, or null for one the person would not call an output (telephony, the bus, and the like) */
    static String outputName(int type) {
        switch (type) {
            case BUILTIN_SPEAKER: return "سماعات التلفزيون";
            case WIRED_HEADSET:
            case WIRED_HEADPHONES: return "سماعة بسلك";
            case LINE_ANALOG:
            case LINE_DIGITAL: return "مخرج صوت خارجي";
            case BLUETOOTH_SCO:
            case BLUETOOTH_A2DP: return "بلوتوث";
            case HDMI: return "HDMI";
            case HDMI_ARC:
            case HDMI_EARC: return "HDMI ARC (مضخم أو ساوند بار)";
            case USB_DEVICE:
            case USB_ACCESSORY:
            case USB_HEADSET: return "USB";
            default: return null;
        }
    }

    /** an output that is a device the person connected: when one is there the TV's own speakers are not what the sound goes to */
    private static boolean external(int type) {
        switch (type) {
            case WIRED_HEADSET: case WIRED_HEADPHONES: case LINE_ANALOG: case LINE_DIGITAL: case BLUETOOTH_SCO: case BLUETOOTH_A2DP:
            case HDMI_ARC: case HDMI_EARC: case USB_DEVICE: case USB_ACCESSORY: case USB_HEADSET:
                return true;
            default:
                return false;
        }
    }

    /**
     * One output as the tile says it: its kind, then the product name the device reports («بلوتوث: JBL Flip») when it has a real one.
     * A blank name, or one that only repeats the kind («HDMI», «USB») or is the TV's own model (the speakers report it), is left out.
     */
    static String outputLabel(int type, String product) {
        String kind = outputName(type);
        if (kind == null) return null;
        if (type == BUILTIN_SPEAKER || product == null) return kind;
        String p = product.trim();
        String lower = p.toLowerCase(Locale.ROOT);
        if (p.isEmpty() || lower.equals("hdmi") || lower.equals("usb") || lower.equals("bluetooth") || lower.equals("hdmi arc")
                || lower.equals("hdmi earc") || lower.equals("unknown") || kind.startsWith(p)) return kind;
        if (p.length() > 24) p = p.substring(0, 24).trim() + "…";
        return kind + ": " + p;
    }

    /**
     * What the tile shows: the outputs the TV reports that have a name, each once, with its product name; the TV's own speakers only
     * when nothing was connected, and a plain HDMI output only when it is all there is. products[i] is the name of types[i] (null, or
     * shorter, when the TV gave none). None = the TV did not say.
     */
    static String outputs(int[] types, String[] products) {
        boolean ext = false;
        for (int t : types) if (external(t)) ext = true;
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (int i = 0; i < types.length; i++) {
            int t = types[i];
            if (ext && !external(t)) continue;
            if (!ext && t == HDMI && hasSpeaker(types)) continue;
            String n = outputLabel(t, products != null && i < products.length ? products[i] : null);
            if (n != null) seen.add(n);
        }
        return seen.isEmpty() ? "لا يُعرف" : String.join("، ", seen);
    }

    private static boolean hasSpeaker(int[] types) {
        for (int t : types) if (t == BUILTIN_SPEAKER) return true;
        return false;
    }

    // ---- «أطفئ الآن»

    /** What the panel says before it does the fallback, when the sleep service is off (shown for {@link #NOTICE_MS}). */
    static final String SLEEP_OFF_NOTICE = "خدمة الإطفاء مو شغّالة: نوقف الصوت ونشغّل شاشة التوقف. لإطفاء الشاشة فعّلها: اضغط مطوّلًا على «أطفئ الآن»";
    static final long NOTICE_MS = 2200;
}
