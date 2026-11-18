package sa.urtv.home.setup;

import java.io.IOException;
import java.util.Arrays;

/**
 * 4.4 (step 100): the safe page's «افتح الشاشة الأصلية» where an earlier setup turned the stock home off: that one package (a known stock home, never the setup
 * wizard) is turned back on, then the TV is asked which packages are off. No Android in here (tests/SafeStockRun.java on SetupEngineRun's fake TV).
 */
final class StockEnable {
    private StockEnable() { }

    static final String WRAITH = "com.google.android.tungsten.setupwraith";

    static boolean run(SetupEngine.Shell sh, String pkg) throws IOException {
        if (pkg == null || pkg.equals(WRAITH) || !Arrays.asList(sa.urtv.home.StockHome.known()).contains(pkg)) return false;
        sh.run("pm enable " + pkg);
        String off = sh.run("pm list packages -d; echo END_OF_LIST").output;
        return off != null && off.contains("END_OF_LIST") && !Arrays.asList(off.replace("\r", "").split("\n")).contains("package:" + pkg);
    }
}
