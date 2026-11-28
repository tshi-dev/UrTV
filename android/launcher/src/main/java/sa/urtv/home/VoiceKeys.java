package sa.urtv.home;

/**
 * 4.4 (R-404): the remote's voice / search key stopped working after a change of the favourites. The key went through the home's own key path (the
 * remote buttons' table, the shortcut keys, the edit modes) and through the add-on's key filter, and any of them could keep it. Now SEARCH, ASSIST
 * and VOICE_ASSIST are never ours: the home hands them straight to Android (after the adhan's any-key and a reminder's card, which are the person's
 * own choices), and the add-on gives them back at once, before anything is looked at. The same file is in the add-on (android/extras/build.gradle).
 * KeyEvent's numbers are written out: no Android in here (tests/VoiceKeysRun.java).
 */
final class VoiceKeys {
    private VoiceKeys() { }

    /** KeyEvent.KEYCODE_SEARCH, KEYCODE_ASSIST, KEYCODE_VOICE_ASSIST */
    static final int SEARCH = 84, ASSIST = 219, VOICE_ASSIST = 231;

    static boolean is(int keyCode) { return keyCode == SEARCH || keyCode == ASSIST || keyCode == VOICE_ASSIST; }
}
