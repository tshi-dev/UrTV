package sa.urtv.home;

/** «إلغاء التثبيت» and «صفحته في المتجر» of the app options box (the Android side of AppActions' rules). */
final class AppStore {
    private AppStore() { }

    /** «إلغاء التثبيت» is offered when the app is one the person installed (deletable), read from the package manager */
    static boolean canUninstall(android.content.Context c, String pkg) {
        try {
            android.content.pm.ApplicationInfo a = c.getPackageManager().getApplicationInfo(pkg, 0);
            boolean system = (a.flags & (android.content.pm.ApplicationInfo.FLAG_SYSTEM | android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            return AppActions.deletable(pkg, system);
        } catch (android.content.pm.PackageManager.NameNotFoundException | RuntimeException e) {
            return false;
        }
    }

    static final String PLAY = "com.android.vending";

    /** «صفحته في المتجر»: the app's page in the Play Store, or null when there is no Play Store on this TV (or nothing to ask for) */
    static android.content.Intent storeIntent(android.content.Context c, String pkg) {
        if (pkg == null || pkg.isEmpty()) return null;
        try {
            c.getPackageManager().getPackageInfo(PLAY, 0);
        } catch (android.content.pm.PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }
        return new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=" + pkg)).setPackage(PLAY);
    }
}
