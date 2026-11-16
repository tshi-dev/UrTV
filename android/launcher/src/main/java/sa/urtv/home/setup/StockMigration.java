package sa.urtv.home.setup;

/**
 * 4.3.2: an earlier home switch turned the stock home, the setup wizard and the stock home's helpers OFF (the journal home-switch.json says what the TV had
 * before). A TV in that state can fail to start (a blank screen and a restart loop on some Google TVs); from 4.3.2 the switch leaves them on, and this runs ONCE
 * on a TV switched the old way: it turns on what the journal says we turned off (never what the TV had off itself, never what «تخلّص من واجهة المصنع» turned off
 * on purpose) and tells the person in one line. No Android in here (tests/StockMigrationRun.java); SetupEngine.migrateStockHome does the work.
 */
final class StockMigration {
    private StockMigration() { }

    static final String MARK = "stock-migrated.txt";
    static final String NOTICE = "رجّعنا تشغيل الشاشة الأصلية ووسيط الإعداد اللذين أوقفهما الإعداد القديم (لأمان إقلاع التلفزيون)؛ تلفزيونك يبقى الشاشة الرئيسية.";

    /**
     * Is there anything to look at (no TV question asked yet): a finished switch (state done), by us, not already migrated, and the person did not ask for the
     * factory interface to be off. {@code state}, {@code stockBefore}, {@code wizardBefore}: the journal's words; {@code factoryMark}: factory-off.txt; {@code migrated}: the mark.
     */
    static boolean due(String state, String stockBefore, String wizardBefore, String factoryMark, String migrated) {
        if (migrated != null && !migrated.trim().isEmpty()) return false;
        if (factoryMark != null && factoryMark.startsWith("on")) return false;   // the person's own choice, kept until «رجّع واجهة المصنع»
        if (!"done".equals(state)) return false;                                  // «switching» belongs to the recovery; nothing else is ours
        return "enabled".equals(stockBefore) || "enabled".equals(wizardBefore);
    }

    /** The packages to turn on: each one the journal says was on before and the TV says is off now (a package the TV had off itself is never touched). */
    static java.util.List<String> toEnable(String stockBefore, String stockPkg, String wizardBefore, String wizardPkg, java.util.Set<String> off, java.util.List<String> helpers, java.util.Set<String> installed) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if ("enabled".equals(stockBefore) && stockPkg != null && off.contains(stockPkg)) out.add(stockPkg);
        if ("enabled".equals(wizardBefore) && off.contains(wizardPkg)) out.add(wizardPkg);
        if ("enabled".equals(stockBefore)) for (String h : helpers) if (installed.contains(h) && off.contains(h) && !out.contains(h)) out.add(h);   // the helpers went off only after the switch
        return out;
    }
}
