package sa.urtv.home;

import android.os.Build;
import android.view.InputDevice;
import android.view.KeyEvent;

/**
 * «أزرار الريموت»: which of this TV's remote app buttons a key event is (RemoteButtons' table), read the same way by the home (MainActivity, on our
 * screen) and by «إضافات تلفزيونك» (ExtrasA11yService, over other apps; android/extras/build.gradle copies this file). The device's ids are read only
 * where the table checks the remote (onn, the Chromecast); nothing of the key is kept or logged.
 */
final class KeyButton {
    private KeyButton() { }

    /** this TV, as RemoteButtons knows it (by maker, brand, model and Android version) */
    static final int TV = RemoteButtons.tv(Build.MANUFACTURER, Build.BRAND, Build.MODEL, Build.DEVICE, Build.VERSION.SDK_INT);

    /** the button's id (RemoteButtons.idOf), or null: any other key, a gamepad's, or another remote's on onn and the Chromecast */
    static String of(KeyEvent e) {
        int src = e.getSource();
        boolean pad = (src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD || (src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
        int vendor = -1, product = -1;
        if (TV == RemoteButtons.ONN || TV == RemoteButtons.SABRINA) {
            InputDevice d = InputDevice.getDevice(e.getDeviceId());
            if (d != null && Build.VERSION.SDK_INT >= 19) { vendor = d.getVendorId(); product = d.getProductId(); }
        }
        return RemoteButtons.idOf(TV, e.getKeyCode(), e.getScanCode(), pad, vendor, product);
    }

    /** Xiaomi's burst window (one press arrives as 5 to 8), 0 elsewhere */
    static long burstMs() { return TV == RemoteButtons.XIAOMI ? RemoteButtons.XIAOMI_BURST_MS : 0; }

    /** a key filter is safe here (not TCL's Android 11, where any filter doubles the volume steps: Free My TV #325) */
    static boolean filterSafe() { return RemoteButtons.filterSafe(TV, Build.VERSION.SDK_INT); }
}
