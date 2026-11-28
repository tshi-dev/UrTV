package sa.urtv.home;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** The TV's inputs and the name shown for each: the one the owner gave it (InputNames), else its HDMI-CEC device's own (InputDisplay), else the TV's own. One place for the popup, «أسماء المصادر» and ADB. */
final class InputLabels {
    private static final String KEY = "input_names";

    private InputLabels() {}

    /** The inputs the sources popup lists (HDMI, AV...), in the TV's own names' order. */
    static List<TvInputInfo> reported(Context c) {
        List<TvInputInfo> inputs = new ArrayList<>();
        try {
            TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
            if (tim != null) for (TvInputInfo t : tim.getTvInputList()) {
                if (listed(t.isPassthroughInput(), t.getType()) && !t.isHidden(c)) inputs.add(t);
            }
            Collections.sort(inputs, (x, y) -> own(c, x).compareToIgnoreCase(own(c, y)));
            inputs = once(c, inputs);
        } catch (RuntimeException e) {   // 4.4 beta 3: a TV's input service that throws: no rows, and the popup offers the TV's own way (MainActivity.showInputsNow)
            inputs.clear();
        }
        return inputs;
    }

    /** 4.4 beta 4: each socket once (InputDisplay.distinct): a port and its CEC device, or one socket the TV reports twice under the same name. */
    private static List<TvInputInfo> once(Context c, List<TvInputInfo> inputs) {
        int n = inputs.size();
        String[] key = new String[n], same = new String[n];
        boolean[] port = new boolean[n], pass = new boolean[n];
        for (int i = 0; i < n; i++) {
            TvInputInfo t = inputs.get(i);
            key[i] = key(t);
            same[i] = t.getType() + ":" + own(c, t);
            port[i] = t.getParentId() == null || t.getParentId().isEmpty();
            pass[i] = t.isPassthroughInput();
        }
        List<TvInputInfo> out = new ArrayList<>();
        for (int i : InputDisplay.distinct(key, same, port, pass)) out.add(inputs.get(i));
        return out;
    }

    /**
     * 4.4 beta 3 (a beta user: no HDMI 1, HDMI 2 in «المصادر»): a passthrough input, as before, or a physical socket a TV reports without the passthrough mark (some
     * vendors' HDMI and AV inputs carry channels of their own). The tuner stays out: «البث التلفزيوني» is its row.
     */
    static boolean listed(boolean passthrough, int type) {
        if (passthrough) return true;
        switch (type) {
            case TvInputInfo.TYPE_HDMI: case TvInputInfo.TYPE_COMPOSITE: case TvInputInfo.TYPE_COMPONENT: case TvInputInfo.TYPE_SVIDEO:
            case TvInputInfo.TYPE_SCART: case TvInputInfo.TYPE_VGA: case TvInputInfo.TYPE_DVI: case TvInputInfo.TYPE_DISPLAY_PORT:
                return true;
            default:
                return false;
        }
    }

    /** The TV's own name for it. */
    static String own(Context c, TvInputInfo t) {
        CharSequence s = t.loadCustomLabel(c);
        if (s == null || s.length() == 0) s = t.loadLabel(c);
        return String.valueOf(s);
    }

    /**
     * What a name is kept under: the HDMI port (the parent) when the input has one, else its own id. The id of a device the TV found over CEC changes
     * and goes away with the device; the port stays, so the name belongs to the port.
     */
    static String key(TvInputInfo t) {
        String parent = t.getParentId();
        return parent != null && !parent.isEmpty() ? parent : t.getId();
    }

    /**
     * The name the HDMI-CEC device behind this input gives itself (raw, InputDisplay cleans it), or null when there is none: the input is the
     * device (it has a parent and is an HDMI input), or a device is listed with this port as its parent. The TV lists such a device as an input
     * of its own, labelled with the name it reports over CEC. Only public API (getParentId, getType, loadLabel), and no permission: the same
     * TvInputManager reads as the sources list always made. (HdmiDeviceInfo itself is @SystemApi, not in the SDK the apps build against.)
     */
    static String cecRaw(Context c, TvInputInfo t) {
        TvInputInfo d = cecDevice(c, t);
        if (d == null) return null;
        CharSequence l = d.loadCustomLabel(c);
        if (l == null || l.length() == 0) l = d.loadLabel(c);
        return InputDisplay.cecName(l == null ? null : l.toString()).isEmpty() ? null : l.toString();
    }

    private static boolean behindPort(TvInputInfo d) {
        return d.getParentId() != null && d.getType() == TvInputInfo.TYPE_HDMI;
    }

    private static TvInputInfo cecDevice(Context c, TvInputInfo t) {
        if (behindPort(t)) return t;
        TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return null;
        TvInputInfo first = null;
        for (TvInputInfo d : tim.getTvInputList()) {
            if (!t.getId().equals(d.getParentId()) || !behindPort(d)) continue;
            if (first == null) first = d;
            CharSequence l = d.loadLabel(c);
            if (!InputDisplay.cecName(l == null ? null : l.toString()).isEmpty()) return d;   // with a hub, the first device that has a name
        }
        return first;
    }

    /** The name an input has when the owner gave none: its CEC device's, else the TV's own. */
    static String fallback(Context c, TvInputInfo t) {
        return InputDisplay.name(null, cecRaw(c, t), own(c, t));
    }

    /** The input's state as the TV reports it: TvInputManager.INPUT_STATE_*, DISCONNECTED when it can't be read. The port and its CEC device both count. */
    static int state(Context c, TvInputInfo t) {
        TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return TvInputManager.INPUT_STATE_DISCONNECTED;
        TvInputInfo d = cecDevice(c, t);
        int a = stateOf(tim, t.getId()), b = d == null ? TvInputManager.INPUT_STATE_DISCONNECTED : stateOf(tim, d.getId());
        if (a == TvInputManager.INPUT_STATE_CONNECTED_STANDBY || b == TvInputManager.INPUT_STATE_CONNECTED_STANDBY) return TvInputManager.INPUT_STATE_CONNECTED_STANDBY;
        return a == TvInputManager.INPUT_STATE_CONNECTED || b == TvInputManager.INPUT_STATE_CONNECTED ? TvInputManager.INPUT_STATE_CONNECTED : TvInputManager.INPUT_STATE_DISCONNECTED;
    }

    /** For «input-names»: connected, standby or disconnected (what the TV says when it has nothing to say). */
    static String stateWord(int state) {
        return state == TvInputManager.INPUT_STATE_CONNECTED_STANDBY ? "standby" : state == TvInputManager.INPUT_STATE_CONNECTED ? "connected" : "disconnected";
    }

    private static int stateOf(TvInputManager tim, String id) {
        try { return tim.getInputState(id); } catch (RuntimeException e) { return TvInputManager.INPUT_STATE_DISCONNECTED; }
    }

    /** The name shown for it, without the state: the owner's, else the CEC device's, else the TV's own. */
    static String label(Context c, TvInputInfo t, Map<String, String> given) {
        return InputDisplay.name(given.get(key(t)), cecRaw(c, t), own(c, t));
    }

    /** What the sources list shows: label() and « · وضع الاستعداد» when the input is in standby. */
    static String rowLabel(Context c, TvInputInfo t, Map<String, String> given) {
        return InputDisplay.label(given.get(key(t)), cecRaw(c, t), own(c, t), state(c, t) == TvInputManager.INPUT_STATE_CONNECTED_STANDBY);
    }

    static Map<String, String> given(Context c) {
        return InputNames.parse(Dock.prefs(c).getString(KEY, ""));
    }

    static void set(Context c, String id, String name) {
        Dock.prefs(c).edit().putString(KEY, InputNames.serialize(InputNames.with(given(c), id, name))).apply();
    }
}
