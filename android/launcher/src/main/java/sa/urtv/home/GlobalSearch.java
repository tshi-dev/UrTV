package sa.urtv.home;

import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * 4.3.3 «بحث» on the home (the owner's list): the TV's own search, the one that looks inside the apps (Android TV's global search: films,
 * series and channels the installed apps offer to it). Off by default: the screensaver's settings «زر البحث» (or `saver.sh searchpill on`)
 * puts #searchpill=1 in the settings; then the round button sits before «الجوال» on any TV that answers one of these actions. Never in kids
 * mode (a search result opens any app) or on «UrTV النظيف»; behind the lock of the app that answers. Nothing is searched or sent by us.
 */
final class GlobalSearch {
    private GlobalSearch() { }

    /** SearchManager.INTENT_ACTION_GLOBAL_SEARCH, then the assistant's search (Google TV answers this one) */
    static final String[] ACTIONS = {"android.search.action.GLOBAL_SEARCH", Intent.ACTION_ASSIST};

    /** the settings ask for the button (#searchpill=1), and kids mode and the clean home do not keep it out */
    static boolean wanted(String fragment, boolean kids, boolean clean) {
        return !kids && !clean && fragment != null && ("&" + fragment + "&").contains("&searchpill=1&");
    }

    /** the first of ACTIONS this TV opens, or null (then there is no button) */
    static Intent intent(PackageManager pm) {
        for (String a : ACTIONS) {
            Intent i = new Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { if (pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY) != null) return i; } catch (RuntimeException ignored) { }
        }
        return null;
    }
}
