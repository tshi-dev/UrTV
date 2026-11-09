package sa.urtv.home;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 4.3 B8 (issue #50, «the flash drive is not read»): the TVs' own viewers of a USB stick. The setup never turns them off (optimize/lists/keep.txt
 * holds the same names; «الميزات المحمية» turns them back on), and the home's apps row always shows them, even where only an ordinary launcher
 * activity opens them next to a Google home (Dock.scan): they are how a USB stick is opened. tests/usb_test.py keeps this list and keep.txt equal.
 * Plain Java.
 */
final class UsbViewers {
    static final Set<String> PACKAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "com.android.documentsui", "com.google.android.documentsui", "com.tcl.ui_mediaCenter", "com.xiaomi.mitv.mediaexplorer",
        "com.mediatek.wwtv.mediaplayer", "com.gdch.wwtv.mediaplayer", "com.homwee.mmp.fileexplorer", "com.homwee.mmp.imageplayer",
        "com.softwinner.TvdFileManager", "com.cxinventor.file.explorer", "com.alphainventor.filemanager", "com.apps.usbmediaplayer",
        "com.rtk.mediabrowser", "com.tianci.localmedia", "com.jamdeo.tv.mediacenter", "com.vt.tv.mediacenter", "com.heytap.tv.filebrowser",
        "com.xgimi.filemanager")));

    private UsbViewers() { }

    static boolean is(String pkg) { return pkg != null && PACKAGES.contains(pkg); }
}
