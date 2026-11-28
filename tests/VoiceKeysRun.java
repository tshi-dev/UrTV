package sa.urtv.home;

/** For tests/voice_keys_test.py (R-404): which keys are the voice / search keys that are never ours. */
public final class VoiceKeysRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("KEYCODE_SEARCH (84)", VoiceKeys.is(84));
        check("KEYCODE_ASSIST (219)", VoiceKeys.is(219));
        check("KEYCODE_VOICE_ASSIST (231)", VoiceKeys.is(231));
        check("not OK, Back, Home, Menu, the arrows, Settings or an app button", !VoiceKeys.is(23) && !VoiceKeys.is(4) && !VoiceKeys.is(3) && !VoiceKeys.is(82)
              && !VoiceKeys.is(19) && !VoiceKeys.is(20) && !VoiceKeys.is(21) && !VoiceKeys.is(22) && !VoiceKeys.is(176) && !VoiceKeys.is(190) && !VoiceKeys.is(4065));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
