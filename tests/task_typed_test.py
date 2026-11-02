#!/usr/bin/env python3
"""«المهام» by typing (issue #48: on Google TV the speech window gives nothing and its search box never reaches us): a text field with
«إضافة» inside the screen, the keyboard's action key adds, the mic only where the TV has a recognizer, and the add is the same one
`saver.sh tasks add` makes. The text rules run for real (TaskVoice.typed / micDead on tests/TaskTypedRun.java, needs javac); the wiring
is read from the source.   Run: python3 tests/task_typed_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

ui = (HOME / "TasksActivity.java").read_text(encoding="utf-8")
manifest = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
prov = (APP / "ConfigProvider.java").read_text(encoding="utf-8")
recv = (APP / "SaverReceiver.java").read_text(encoding="utf-8")

check("the screen has its own EditText", "field = new EditText(this)" in ui)
check("the field is plain text, one line, kept to the 60 characters", "InputType.TYPE_CLASS_TEXT" in ui and "field.setSingleLine(true)" in ui and "LengthFilter(MAX_CHARS)" in ui and "MAX_CHARS = 60" in ui)
check("the action key is «إضافة» (done), not search", "IME_ACTION_DONE" in ui and 'setImeActionLabel(S.t("إضافة"), EditorInfo.IME_ACTION_DONE)' in ui and "IME_ACTION_SEARCH" not in ui)
check("the keyboard's action key and Enter add the task", "setOnEditorActionListener" in ui and "action == EditorInfo.IME_ACTION_DONE" in ui and "KEYCODE_ENTER" in ui and "addTyped(); return true;" in ui)
check("an «إضافة» button adds it too", 'addBtn = pill("إضافة", GOLD, v -> addTyped())' in ui)
check("the field is focusable with the D-pad and has the glass focus", "field.setFocusable(true)" in ui and "GlassPaint.card(field, look, 14)" in ui)
check("the field's focus listener is set before GlassPaint.card (which keeps it)", ui.index("field.setOnFocusChangeListener") < ui.index("GlassPaint.card(field"))
check("▼ from the field reaches «إضافة», ▲ comes back", "field.setNextFocusDownId(addBtn.getId())" in ui and "addBtn.setNextFocusUpId(field.getId())" in ui)
check("right to left: the text, its gravity and the hint (Dir: English left to right)", "field.setTextDirection(Dir.text());" in ui and "Dir.start() | Gravity.CENTER_VERTICAL" in ui)
check("the screen's direction is still pinned (the D-pad ids; English: the mirror)", "scroll.setLayoutDirection(Dir.layout(!Dir.rtl()))" in ui)
check("an empty field adds nothing and says so", 't.isEmpty()) { toast(S.t("اكتب المهمة أولاً"))' in ui)
check("a refused add (full list) keeps the words; an accepted one clears the field", "if (add(t)) field.setText(\"\")" in ui)
check("the typed add is the provider's insert, as `saver.sh tasks add` (Prefs.addTask)",
      "getContentResolver().insert(TASKS, v)" in ui and "p.addTask(v.getAsString(\"t\"))" in prov and "p.addTask(i.getStringExtra(\"text\"))" in recv)
check("a phone-typed text and the field share one cleaning", ui.count("TaskVoice.typed(") == 2)
# the mic
check("the mic needs a recognizer: SpeechRecognizer or a resolved RecognizerIntent", "SpeechRecognizer.isRecognitionAvailable(this)" in ui and "speechIntent().resolveActivity(getPackageManager()) != null" in ui)
check("no recognizer: the mic is hidden at creation, with the reason", "if (!hasRecognizer()) noMic(" in ui and re.search(r"private void noMic\(String why\) \{\s*mic\.setVisibility\(View\.GONE\)", ui))
check("a speech window that closes at once with nothing hides the mic", "TaskVoice.micDead(result == RESULT_CANCELED && data == null" in ui)
check("no activity for the intent hides it too", "catch (ActivityNotFoundException | SecurityException e)" in ui)
check("the manifest lets us see recognizers (Android 11 package visibility)", "android.speech.action.RECOGNIZE_SPEECH" in manifest and "android.speech.RecognitionService" in manifest)
check("without a mic the focus still lands somewhere (the field)", ": field;" in ui)
check("the phone page was not touched for this (it adds tasks through TextTarget)", "acceptText" in ui)

if not shutil.which("javac"):
    print("SKIP typed task rules: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "TaskVoice.java"), str(ROOT / "tests/TaskTypedRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.TaskTypedRun"], check=True, capture_output=True).stdout.decode("utf-8")
rows = [l.split("\t") for l in out.split("\n") if l]
for name, res in rows:
    check("typed task: " + name, res == "ok", res)
check("every rule ran", len(rows) == 7, len(rows))
sys.exit(1 if failed else 0)
