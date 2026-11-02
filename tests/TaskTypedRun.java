package sa.urtv.home;

/** For tests/task_typed_test.py: TaskVoice.typed and TaskVoice.micDead. Prints name TAB ok|got. */
public final class TaskTypedRun {
    static void t(String name, boolean ok, Object got) { System.out.println(name + "\t" + (ok ? "ok" : String.valueOf(got))); }

    public static void main(String[] a) {
        t("trims", "اشتري خبز".equals(TaskVoice.typed("  اشتري خبز \n", 60)), TaskVoice.typed("  اشتري خبز \n", 60));
        t("spaces tidied", "موعد الطبيب".equals(TaskVoice.typed("موعد   \t الطبيب", 60)), TaskVoice.typed("موعد   \t الطبيب", 60));
        t("empty", TaskVoice.typed("   ", 60).isEmpty() && TaskVoice.typed(null, 60).isEmpty(), "");
        String long70 = "ب".repeat(70);
        t("cut to 60", TaskVoice.typed(long70, 60).length() == 60, TaskVoice.typed(long70, 60).length());
        t("mic dead: cancelled at once", TaskVoice.micDead(true, 300), "");
        t("mic alive: cancelled after a while (the person pressed Back)", !TaskVoice.micDead(true, 4000), "");
        t("mic alive: a result", !TaskVoice.micDead(false, 100), "");
    }
}
