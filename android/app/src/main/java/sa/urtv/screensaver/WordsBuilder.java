package sa.urtv.screensaver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * The settings' dialogs in «اللغة» (Words.of): every title, message, list and button the code gives in Arabic is shown in English when the
 * setting is English, and as it is with Arabic, the default (Words.of gives the text itself). own(): a list whose choices are each in their
 * own language («اللغة»: العربية, English), never translated. SettingsActivity.Dlg is one of these.
 */
class WordsBuilder extends AlertDialog.Builder {
    private boolean own;

    WordsBuilder(Context c, int theme) { super(c, theme); }

    WordsBuilder own() { own = true; return this; }

    private CharSequence w(CharSequence t) { return own || t == null ? t : Words.of(t.toString()); }
    private CharSequence[] w(CharSequence[] t) { return own ? t : Words.of(t); }

    @Override public AlertDialog.Builder setTitle(CharSequence t) { return super.setTitle(w(t)); }
    @Override public AlertDialog.Builder setMessage(CharSequence t) { return super.setMessage(w(t)); }
    @Override public AlertDialog.Builder setPositiveButton(CharSequence t, DialogInterface.OnClickListener l) { return super.setPositiveButton(w(t), l); }
    @Override public AlertDialog.Builder setNegativeButton(CharSequence t, DialogInterface.OnClickListener l) { return super.setNegativeButton(w(t), l); }
    @Override public AlertDialog.Builder setNeutralButton(CharSequence t, DialogInterface.OnClickListener l) { return super.setNeutralButton(w(t), l); }
    @Override public AlertDialog.Builder setItems(CharSequence[] t, DialogInterface.OnClickListener l) { return super.setItems(w(t), l); }
    @Override public AlertDialog.Builder setSingleChoiceItems(CharSequence[] t, int checked, DialogInterface.OnClickListener l) {
        return super.setSingleChoiceItems(w(t), checked, l);
    }
    @Override public AlertDialog.Builder setMultiChoiceItems(CharSequence[] t, boolean[] checked, DialogInterface.OnMultiChoiceClickListener l) {
        return super.setMultiChoiceItems(w(t), checked, l);
    }
}
