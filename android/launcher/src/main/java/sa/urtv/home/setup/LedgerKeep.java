package sa.urtv.home.setup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Known bug (tester step 92): a reinstall of UrTV loses the setup record (the ledger of what the setup turned off lives in the home's app data), so «إنقاذ تلفزيونك» and «تراجع» left
 * 18 apps off. The home sends a copy of the ledger (package names, one per line) to the rescue app whenever it changed (LedgerPush); the rescue keeps it in its own file
 * (LedgerReceiver) and uses it when the home does not answer. Only a sender holding the signature permission sa.urtv.permission.EXTRAS is heard. No Android in here, and the rescue
 * app compiles this same file (android/rescue/build.gradle): tests/ledger_keep_test.py runs it.
 */
public final class LedgerKeep {
    private LedgerKeep() { }

    public static final String RESCUE_PACKAGE = "sa.urtv.rescue", RECEIVER = "sa.urtv.home.setup.LedgerReceiver", ACTION = "sa.urtv.rescue.LEDGER_COPY",
        EXTRA = "ledger", FILE = "ledger-copy.txt", PERMISSION = "sa.urtv.permission.EXTRAS";

    /** the valid package names of a ledger text, in order, each once */
    public static List<String> names(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text != null) for (String l : text.split("\n")) { l = l.trim(); if (l.matches("[A-Za-z0-9_.]+") && l.length() <= 200) out.add(l); }
        return new ArrayList<>(out);
    }

    /** the text sent and kept: the names, one per line (empty when there are none) */
    public static String clean(String text) {
        StringBuilder b = new StringBuilder();
        for (String n : names(text)) b.append(n).append('\n');
        return b.toString();
    }

    /** what the rescue keeps after hearing {@code incoming}: the new list, except that an empty or unreadable one never wipes a kept one */
    public static String keep(String kept, String incoming) {
        String now = clean(incoming);
        return now.isEmpty() ? clean(kept) : now;
    }

    /** «إنقاذ» uses the kept copy when the home gives it nothing: no answer, or an empty ledger (a reinstalled home answers «\n», never null: review F120) and the copy has names */
    public static boolean useCopy(String fromHome, String copy) {
        return names(fromHome).isEmpty() && !names(copy).isEmpty();
    }

    /** the home pushes only a list that is not empty and not the one it pushed last */
    public static boolean worthPushing(String lastPushed, String now) {
        String n = clean(now);
        return !n.isEmpty() && !n.equals(lastPushed == null ? "" : lastPushed);
    }
}
