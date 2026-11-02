package sa.urtv.home;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * «المهام»: the tasks kept by the screensaver app (its ConfigProvider, same signing key), listed with OK to mark
 * one done, «حذف» beside each, a text field with «إضافة» (the TV keyboard's own mic dictates into it, so it works where
 * the system's speech window does not, e.g. Google TV), and a mic that starts Android's speech recognizer when the TV has one. Said in Arabic:
 * «أضف مهمة …» adds, «خلصت …» finishes, «احذف …» deletes (TaskVoice). Nothing changes until «نعم» is pressed.
 * No dialogs, like DockActivity: rows are built right-aligned by hand. `am start -n sa.urtv.home/.TasksActivity`.
 */
public class TasksActivity extends Activity implements sa.urtv.home.phone.TextTarget {
    /** «اللغة» (docs/ENGLISH.md): with Arabic the context itself, with English an English one */
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private static final Uri TASKS = Uri.parse("content://sa.urtv.screensaver.config/tasks");
    private static final int SPEECH = 41;
    /** from the home screen's «المهام» card: the list opens with the focus on «🎤 تكلّم», so one more OK starts the voice adding */
    static final String EXTRA_MIC = "mic";
    private int GOLD, INK, TEXT, DIM;   // from the glass look (glass(), before anything is drawn)
    private GlassStyle.Look look;
    private static final int MAX_CHARS = 60;   // as the screensaver keeps them (Prefs.TASK_CHARS)

    private final List<String> texts = new ArrayList<>();
    private final List<Boolean> dones = new ArrayList<>();
    /** each task's own id, which stays with it when the list moves or two tasks say the same (0: an older screensaver app that gives none) */
    private final List<Long> ids = new ArrayList<>();
    private LinearLayout list, confirm;
    private TextView status, question;
    private View mic, yes, no, addBtn;
    private EditText field;   // the task typed (or dictated by the keyboard's mic): «إضافة» or the keyboard's action key adds it
    private long listenedAt;   // when the speech window was opened (SystemClock.elapsedRealtime), to tell one that never worked
    private Runnable onYes;
    private boolean micFirst;   // the first render puts the focus on the mic (EXTRA_MIC), later ones on the list as always

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        micFirst = getIntent().getBooleanExtra(EXTRA_MIC, false);
        ScrollView scroll = new ScrollView(this);
        glass(scroll);   // the glass look: the home's photo, small and blurred once, under its veil
        scroll.setVerticalScrollBarEnabled(false);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(36), 0, dp(48));
        frame.addView(col, new FrameLayout.LayoutParams(dp(600), -2, Gravity.CENTER_HORIZONTAL));

        col.addView(text(S.t("المهام"), 26, "Medium", TEXT), right(0));
        col.addView(text(S.t("اختيار على مهمة لإنجازها. اكتب مهمة جديدة في الخانة ثم «إضافة». أو اضغط «تكلّم» وقل: أضف مهمة … / خلصت … / احذف …"), 14, "Regular", DIM), right(dp(10)));

        field = new EditText(this);
        field.setId(View.generateViewId());
        field.setHint(S.t("اكتب المهمة هنا أو أملها بميكروفون لوحة المفاتيح"));
        field.setHintTextColor(DIM);
        field.setTextColor(TEXT);
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT);   // plain text: the keyboard's dictation writes here
        field.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);   // the action key is «إضافة», never a search
        field.setImeActionLabel(S.t("إضافة"), EditorInfo.IME_ACTION_DONE);
        field.setFilters(new InputFilter[] { new InputFilter.LengthFilter(MAX_CHARS) });
        field.setTextDirection(Dir.text());
        field.setGravity(Dir.start() | Gravity.CENTER_VERTICAL);
        field.setPadding(dp(16), dp(10), dp(16), dp(10));
        field.setFocusable(true);
        field.setFocusableInTouchMode(true);
        try { field.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-Medium.otf")); }
        catch (RuntimeException ignored) { }
        field.setOnFocusChangeListener((v, has) -> field.setHintTextColor(has ? look.onFocusSub : DIM));   // before card(): it keeps this one
        GlassPaint.card(field, look, 14);   // the same panel and solid focus fill as every button: the focus is clear
        field.setOnEditorActionListener((v, action, e) -> {
            boolean enter = e != null && e.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (action == EditorInfo.IME_ACTION_DONE || (enter && e.getAction() == KeyEvent.ACTION_DOWN)) { addTyped(); return true; }
            return enter;   // the key-up of an Enter we already took
        });
        col.addView(field, right(dp(8)));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Dir.start());
        addBtn = pill("إضافة", GOLD, v -> addTyped());
        actions.addView(addBtn, new LinearLayout.LayoutParams(dp(110), dp(40)));
        mic = pill(S.t("🎤 تكلّم"), GOLD, v -> listen());
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(140), dp(40));
        mp.setMarginEnd(dp(10));
        actions.addView(mic, mp);   // to the left of «إضافة» (the right in English: the screen is mirrored, below)
        col.addView(actions, new LinearLayout.LayoutParams(-1, -2));
        field.setNextFocusDownId(addBtn.getId());
        addBtn.setNextFocusUpId(field.getId());
        mic.setNextFocusUpId(field.getId());
        if (Dir.rtl()) addBtn.setNextFocusLeftId(mic.getId()); else addBtn.setNextFocusRightId(mic.getId());
        if (Dir.rtl()) mic.setNextFocusRightId(addBtn.getId()); else mic.setNextFocusLeftId(addBtn.getId());
        status = text("", 14, "Medium", GOLD);
        col.addView(status, right(dp(8)));

        confirm = new LinearLayout(this);
        confirm.setOrientation(LinearLayout.VERTICAL);
        confirm.setVisibility(View.GONE);
        confirm.setPadding(dp(16), dp(12), dp(16), dp(12));
        confirm.setBackground(GlassPaint.dialog(this, look));   // the question: a glass panel of its own
        question = text("", 17, "Medium", TEXT);
        confirm.addView(question, right(dp(10)));
        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Dir.start());
        yes = pill(S.t("نعم"), GOLD, v -> answer(true));
        no = pill(S.t("لا"), DIM, v -> answer(false));
        buttons.addView(yes, new LinearLayout.LayoutParams(dp(90), dp(38)));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(dp(90), dp(38));
        np.setMarginEnd(dp(10));
        buttons.addView(no, np);
        confirm.addView(buttons, new LinearLayout.LayoutParams(-1, -2));
        col.addView(confirm, right(dp(12)));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        col.addView(list, new LinearLayout.LayoutParams(-1, -2));
        // the D-pad ids of these rows are left-to-right: pinned, so a TV set to Arabic does not mirror the rows under them. English is the
        // mirror of the Arabic screen: the same rows laid out right to left, their margins at the end and their ◀▶ ids swapped (Dir.rtl())
        scroll.setLayoutDirection(Dir.layout(!Dir.rtl()));
        setContentView(scroll);

        if (!hasRecognizer()) noMic(S.t("الإدخال الصوتي غير متاح في هذا التلفزيون. اكتب المهمة في الخانة، أو استعمل ميكروفون لوحة المفاتيح"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        render(0);
    }

    // ---------------------------------------------------------------- list

    /** false when the screensaver app is missing, too old or refuses us */
    private boolean load() {
        texts.clear();
        dones.clear();
        ids.clear();
        try (Cursor c = getContentResolver().query(TASKS, null, null, null, null)) {
            if (c == null) return false;
            while (c.moveToNext()) {
                texts.add(c.getString(1));
                dones.add(c.getInt(2) != 0);
                ids.add(c.getColumnCount() > 4 ? c.getLong(4) : 0L);
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void render(int focus) {
        boolean ok = load();
        list.removeAllViews();
        if (!ok) status.setText(S.t("تطبيق شاشة التوقف غير مثبّت أو قديم"));
        else if (texts.isEmpty()) list.addView(text(S.t("لا مهام اليوم"), 17, "Medium", DIM), right(0));
        View first = null, wanted = null;
        for (int i = 0; i < texts.size(); i++) {
            final String t = texts.get(i);
            final boolean done = dones.get(i);
            final long id = ids.get(i);
            LinearLayout r = new LinearLayout(this);
            View del = pill(S.t("حذف"), look.warn, v -> ask(S.t("أحذف «%s»؟", t), () -> delete(t, id)));
            r.addView(del, new LinearLayout.LayoutParams(dp(84), -1));
            View card = card((done ? "✓  " : "●  ") + t, done, v -> {
                if (done) toast(S.t("هذي المهمة منجزة"));
                else markDone(t, id);
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
            cp.setMarginStart(dp(10));
            r.addView(card, cp);
            if (Dir.rtl()) card.setNextFocusLeftId(del.getId()); else card.setNextFocusRightId(del.getId());
            if (Dir.rtl()) del.setNextFocusRightId(card.getId()); else del.setNextFocusLeftId(card.getId());
            list.addView(r, rowParams());
            if (first == null) first = card;
            if (i == focus) wanted = card;
        }
        View f = wanted != null ? wanted : first != null ? first : mic.getVisibility() == View.VISIBLE ? mic : field;
        if (micFirst && mic.getVisibility() == View.VISIBLE) f = mic;   // from the home card: the mic first
        micFirst = false;
        if (f != null && confirm.getVisibility() != View.VISIBLE) f.post(f::requestFocus);
    }

    // ---------------------------------------------------------------- changes (the list itself lives in the screensaver app)

    /** id is the task the person pointed at (0 when it is not known: found again by its text, the first open one) */
    private void markDone(String t, long id) {
        ContentValues v = new ContentValues();
        long which = id > 0 ? id : idOf(t, true);
        int n = which > 0 ? 0 : numberOf(t, true);
        if (which > 0) v.put("id", which); else v.put("i", n);
        if ((which > 0 || n > 0) && write(() -> getContentResolver().update(TASKS, v, null, null) > 0)) toast(S.t("تم: %s", t));
        else failed();
    }

    private void delete(String t, long id) {
        long which = id > 0 ? id : idOf(t, false);
        int n = which > 0 ? 0 : numberOf(t, false);
        Uri uri = which > 0 ? TASKS.buildUpon().appendPath("id").appendPath(String.valueOf(which)).build()
                : TASKS.buildUpon().appendPath(String.valueOf(n)).build();
        if ((which > 0 || n > 0) && write(() -> getContentResolver().delete(uri, null, null) > 0)) toast(S.t("حُذفت: %s", t));
        else failed();
    }

    /** Text typed on the phone («تحكم من الجوال») becomes a task, like a spoken one. */
    @Override public boolean acceptText(String t) {
        String text = TaskVoice.typed(t, MAX_CHARS);
        if (text.isEmpty()) return false;
        add(text);
        return true;
    }

    /** «إضافة» / the keyboard's action key / Enter: the field's text becomes a task, the same insert as `saver.sh tasks add` (ConfigProvider -> Prefs.addTask). */
    private void addTyped() {
        String t = TaskVoice.typed(field.getText().toString(), MAX_CHARS);
        if (t.isEmpty()) { toast(S.t("اكتب المهمة أولاً")); return; }
        if (add(t)) field.setText("");   // a refused one (full list) keeps its words
        hideKeyboard();
        field.post(field::requestFocus);   // ready for the next one
    }

    private void hideKeyboard() {
        InputMethodManager m = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (m != null) m.hideSoftInputFromWindow(field.getWindowToken(), 0);
    }

    private boolean add(String t) {
        ContentValues v = new ContentValues();
        v.put("t", t);
        boolean ok = write(() -> getContentResolver().insert(TASKS, v) != null);
        if (ok) toast(S.t("أُضيفت: %s", t));
        else toast(S.t("ما أضفت المهمة: القائمة ممتلئة (%s) أو النص فاضي", ar(12)));
        return ok;
    }

    private interface Change { boolean run(); }

    private boolean write(Change c) {
        try {
            boolean ok = c.run();
            render(0);
            return ok;
        } catch (RuntimeException e) {   // SecurityException: not the same signing key; or the app is gone
            return false;
        }
    }

    private void failed() {
        toast(S.t("تعذّر التعديل، افتح المهام من جديد"));
        render(0);
    }

    /** the id of the first task with this text (an open one when openOnly); 0 when there is none, or the app gives no ids */
    private long idOf(String t, boolean openOnly) {
        if (!load()) return 0;
        for (int i = 0; i < texts.size(); i++)
            if (texts.get(i).equals(t) && !(openOnly && dones.get(i))) return ids.get(i);
        return 0;
    }

    /** the number the screensaver gives it now (from 1), found again by its text: for an older screensaver app that has no ids */
    private int numberOf(String t, boolean openOnly) {
        if (!load()) return 0;
        for (int i = 0; i < texts.size(); i++)
            if (texts.get(i).equals(t) && !(openOnly && dones.get(i))) return i + 1;
        return 0;
    }

    // ---------------------------------------------------------------- confirm

    private void ask(String q, Runnable yesAction) {
        onYes = yesAction;
        question.setText(q);
        confirm.setVisibility(View.VISIBLE);
        yes.post(yes::requestFocus);
    }

    private void answer(boolean ok) {
        Runnable r = onYes;
        onYes = null;
        confirm.setVisibility(View.GONE);
        if (ok && r != null) r.run();
        else render(0);
    }

    @Override
    public void onBackPressed() {
        if (confirm.getVisibility() == View.VISIBLE) answer(false);
        else super.onBackPressed();
    }

    // ---------------------------------------------------------------- voice

    private Intent speechIntent() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "أضف مهمة … / خلصت … / احذف …");
        return i;
    }

    /** the TV offers a recognizer: a RecognitionService (SpeechRecognizer) or an activity for RECOGNIZE_SPEECH (both visible through the manifest's queries) */
    private boolean hasRecognizer() {
        return SpeechRecognizer.isRecognitionAvailable(this) || speechIntent().resolveActivity(getPackageManager()) != null;
    }

    /** no (working) voice here: no dead button, the typed field is the way */
    private void noMic(String why) {
        mic.setVisibility(View.GONE);
        if (Dir.rtl()) addBtn.setNextFocusLeftId(addBtn.getId()); else addBtn.setNextFocusRightId(addBtn.getId());
        status.setText(why);
    }

    private void listen() {
        status.setText("");
        onYes = null;
        confirm.setVisibility(View.GONE);
        listenedAt = SystemClock.elapsedRealtime();
        try { startActivityForResult(speechIntent(), SPEECH); }
        catch (ActivityNotFoundException | SecurityException e) {
            noMic(S.t("الإدخال الصوتي غير متاح في هذا التلفزيون. اكتب المهمة في الخانة"));
            field.post(field::requestFocus);
        }
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == SPEECH && TaskVoice.micDead(result == RESULT_CANCELED && data == null, SystemClock.elapsedRealtime() - listenedAt)) {
            // a window that opened and closed at once with nothing (Google TV's search): the button would stay dead, so it goes
            noMic(S.t("الإدخال الصوتي غير متاح في هذا التلفزيون (فتح وأغلق بلا صوت). اكتب المهمة في الخانة، أو استعمل ميكروفون لوحة المفاتيح"));
            field.post(field::requestFocus);
            return;
        }
        if (code != SPEECH || result != RESULT_OK || data == null) return;
        ArrayList<String> heard = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (heard == null || heard.isEmpty()) return;
        load();
        TaskVoice.Result r = null;
        for (String h : heard) {   // the recognizer's best guess first; the first one that is a command
            r = TaskVoice.understand(h, texts, dones);
            if (r.kind != TaskVoice.Kind.NONE) break;
        }
        status.setText(S.t("سمعت: %s", heard.get(0)));
        switch (r.kind) {
            case ADD: {
                final String t = clip(r.text);
                ask(S.t("أضيف «%s»؟", t), () -> add(t));
                break;
            }
            case DONE: {
                final String t = r.text;
                ask(S.t("أعلّم «%s» منجزة؟", t), () -> markDone(t, 0));
                break;
            }
            case DELETE: {
                final String t = r.text;
                ask(S.t("أحذف «%s»؟", t), () -> delete(t, 0));
                break;
            }
            case NO_MATCH:
                status.setText(S.t("ما لقيت مهمة تطابق «%s»", r.text));
                break;
            case AMBIGUOUS:
                status.setText(S.t("أكثر من مهمة تطابق «%s»، قل جزءاً أوضح", r.text));
                break;
            default:
                status.setText(S.t("ما فهمت. قل: أضف مهمة … أو خلصت … أو احذف …"));
        }
    }

    private static String clip(String s) {
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() > MAX_CHARS ? s.substring(0, MAX_CHARS).trim() : s;
    }

    // ---------------------------------------------------------------- views

    private View card(String label, boolean done, View.OnClickListener click) {
        TextView c = text(label, 17, "Medium", done ? DIM : TEXT);
        c.setPadding(dp(16), dp(12), dp(16), dp(12));
        c.setFocusable(true);
        c.setClickable(true);
        c.setId(View.generateViewId());
        GlassPaint.card(c, look, 14);   // the panel, the solid focus fill and its ink while focused
        c.setOnClickListener(click);
        return c;
    }

    /** a small button: its word in {@code accent} (the warning colour for «حذف») at rest, the solid focus fill while focused */
    private View pill(String label, final int accent, View.OnClickListener click) {
        final TextView p = text(label, 14, "Medium", accent == look.warn ? look.warn : DIM);
        p.setGravity(Gravity.CENTER);
        p.setFocusable(true);
        p.setClickable(true);
        p.setId(View.generateViewId());
        GlassPaint.card(p, look, 14);
        p.setOnClickListener(click);
        return p;
    }

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(S.tv(s));   // the words of the screen given as Arabic (pill("إضافة")) are said in «اللغة»; one already said stays
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Dir.start());
        try { t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + weight + ".otf")); }
        catch (RuntimeException ignored) { }
        return t;
    }

    private LinearLayout.LayoutParams right(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = bottom;
        return p;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(8);
        return p;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private static String ar(int n) {
        return Digits.of(String.valueOf(n));
    }

    /** The glass look on this page (GlassSource: the home's photo under its veil, or the look's solid colour), and this screen's colours from it. */
    private void glass(View page) {
        look = GlassSource.page(this, page);
        GOLD = look.accent; INK = look.onFocus; TEXT = look.text; DIM = look.sub;
    }
}
