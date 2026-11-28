package sa.urtv.home.setup;

/**
 * 4.4 (R-409, about 40 reports): on some TCL and Xiaomi TVs (Android 11 and 14) the TV's setup wizard (setupwraith) keeps the Home key: it owns the saved
 * Wi-Fi, so it is never turned off (SetupEngine.KEEP_STOCK_ON; AGENTS.md), and Home brought the wizard's stock screen back. The add-on's window watch
 * looks at stock homes only, never at the wizard, so the setup now turns «إضافات تلفزيونك»'s Home key catch on there by default (the remote's Home opens
 * ours before the wizard sees it), the «تلفزيونك» tile stays the way in, and the result and the report say so plainly. adb's own Home key never reaches
 * the catch, so the setup cannot see it work: the person's remote can. No Android in here (tests/WizardHomeRun.java).
 */
final class WizardHome {
    private WizardHome() { }

    static final String WRAITH = SetupEngine.WRAITH;
    /** the home step's word on the add-on's Home key catch («on» / «off»), for the report */
    static final String FILE = "home-catch.txt";

    /** the wizard keeps Home: the Home key's resumed line names it, or HOME resolves to it */
    static boolean holds(String homeKeyFront, String resolved) {
        return homeKeyFront != null && homeKeyFront.contains(WRAITH + "/") || resolved != null && resolved.trim().startsWith(WRAITH + "/");
    }

    static final String SAID_CATCH = "وسيط إعداد التلفزيون ماسك زر الرئيسية في هذا التلفزيون (معه الواي فاي، فما نوقفه). شغّلنا في «إضافات تلفزيونك» إن زر الرئيسية في الريموت يفتح تلفزيونك، ومربع «تلفزيونك» في صف التطبيقات يفتحه بعد. خلّ «إضافات تلفزيونك» مفعّلة، واضغط زر الرئيسية في الريموت للتأكد.";
    static final String SAID_NO_CATCH = "وسيط إعداد التلفزيون ماسك زر الرئيسية في هذا التلفزيون (معه الواي فاي، فما نوقفه)، و«إضافات تلفزيونك» ما قدرت تمسك الزر هنا. افتح تلفزيونك من مربع «تلفزيونك» في صف التطبيقات. تلفزيونك بقي الشاشة الرئيسية، و«تراجع» يرجّع الأصلية.";

    /** the report's line (English, for an agent): how Home works on a TV whose wizard keeps it; null when the wizard does not */
    static String reportLine(String homeOpens, boolean catchOn) {
        if (homeOpens == null || !homeOpens.startsWith(WRAITH + "/")) return null;
        return catchOn
            ? "Setup wizard: setupwraith keeps the Home key on this TV (it owns the saved Wi-Fi, so it stays on). Home opens UrTV through the add-on's Home key catch (on), and the «تلفزيونك» tile; adb's own Home key never reaches the catch, so «Home:» above names the wizard. Check with the remote; never turn the wizard off"
            : "Setup wizard: setupwraith keeps the Home key on this TV (it owns the saved Wi-Fi, so it stays on), and the add-on's Home key catch is OFF here: Home opens the wizard's screen, UrTV opens from its «تلفزيونك» tile. Never turn the wizard off";
    }
}
