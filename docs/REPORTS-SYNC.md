# مزامنة التقارير كل ليلة

كل تقرير يرسله الإعداد (`optimize/share.py` على الكمبيوتر، أو `SetupReport.java` على التلفزيون) يصير صفّاً في جدول ردود نموذج Google.
السكربت `apps-script/sync.gs` يشتغل كل ليلة داخل هذا الجدول، وينسخ الصفوف الجديدة فقط إلى هذا المستودع الخاص في ملف
`reports/YYYY-MM-DD.csv` (بتوقيت الرياض). لا يخرج من الجدول شيء غير ذلك.

> **المفتاح (التوكن) لا يُكتب أبداً في محادثة ولا في ملف ولا في رسالة.** مكانه الوحيد «خصائص السكربت» (Script Properties) داخل Apps Script.

## ١. أنشئ المفتاح في GitHub (مرة واحدة)

1. افتح github.com وأنت داخل بحساب **tshi-dev**.
2. اضغط صورتك أعلى اليمين ← **Settings** ← في آخر القائمة **Developer settings** ← **Personal access tokens** ← **Fine-grained tokens** ← **Generate new token**.
3. **Token name**: `urtv-reports-sync`.
4. **Expiration**: اختر مدة (مثلاً 90 يوماً أو سنة). اكتب في تقويمك موعد انتهائه لتجدّده.
5. **Resource owner**: `tshi-dev`.
6. **Repository access**: اختر **Only select repositories** ثم اختر **tshi-dev/urtv-reports** فقط.
7. **Permissions** ← **Repository permissions** ← **Contents**: اختر **Read and write**. لا تغيّر أي صلاحية أخرى (صلاحية Metadata للقراءة تُضاف وحدها، وهذا طبيعي).
8. اضغط **Generate token**. يظهر المفتاح مرة واحدة فقط: انسخه والصقه مباشرة في الخطوة ٣ أدناه، ولا تحفظه في أي مكان آخر.

## ٢. ضع السكربت في الجدول

1. افتح جدول ردود النموذج (الجدول الذي فيه عمود Timestamp وعمود التقرير).
2. من القائمة: **Extensions** (الإضافات) ← **Apps Script**.
3. اضغط رمز الترس **Project Settings** (إعدادات المشروع) وفعّل **Show "appsscript.json" manifest file in editor**.
4. ارجع إلى **Editor** (المحرر):
   - افتح `Code.gs`، احذف كل ما فيه، والصق محتوى الملف `apps-script/sync.gs` من هذا المستودع.
   - افتح `appsscript.json`، احذف كل ما فيه، والصق محتوى `apps-script/appsscript.json`.
   - اضغط **Save** (حفظ).

## ٣. ضع المفتاح في خصائص السكربت

1. **Project Settings** (الترس) ← انزل إلى **Script Properties** ← **Add script property**.
2. **Property**: `GITHUB_TOKEN` — **Value**: الصق المفتاح من الخطوة ١. ← **Save script properties**.
3. (اختياري) إذا كان في الجدول أكثر من ورقة: أضف `SHEET_NAME` وقيمتها اسم ورقة الردود بالضبط (مثل `Form Responses 1`).
4. (اختياري) أول تشغيل يرسل كل الصفوف من الصف ٢. إذا أردت البدء من الآن فقط: أضف `LAST_ROW` وقيمتها رقم آخر صف موجود الآن.

## ٤. جرّب بدون إرسال

1. في المحرر اختر الدالة **dryRun** من القائمة أعلى الصفحة واضغط **Run**.
2. أول مرة يطلب Google الإذن. يجب أن ترى **صلاحيتين فقط**: رؤية وتعديل هذا الجدول، والاتصال بخدمة خارجية. إذا طلب غيرهما فتوقّف وتحقق من `appsscript.json`.
3. افتح **Execution log** (سجل التنفيذ): يقول كم صفاً سيُرسل وإلى أي ملف، وهل المفتاح موجود. لا يُرسل شيء ولا يتغيّر شيء.

## ٥. أول إرسال حقيقي

1. اختر **syncReports** واضغط **Run**.
2. افتح المستودع `tshi-dev/urtv-reports` في GitHub: يجب أن يظهر ملف في مجلد `reports/` بتاريخ اليوم.

## ٦. التشغيل كل ليلة

1. في Apps Script اضغط رمز الساعة **Triggers** (المشغّلات) ← **Add Trigger**.
2. **Choose which function to run**: `syncReports` — **deployment**: `Head` — **event source**: `Time-driven` — **type**: `Day timer` — **time**: مثلاً `2am to 3am`.
3. **Failure notification settings**: `Notify me daily` (يصلك بريد إذا فشل التشغيل، مثلاً عند انتهاء المفتاح).
4. **Save**.

## إذا انتهى المفتاح أو أردت إيقاف المزامنة

- **انتهى المفتاح**: أنشئ مفتاحاً جديداً بالخطوة ١ نفسها، وضعه مكان القيمة القديمة لـ `GITHUB_TOKEN`. الصفوف التي فاتت تُرسل في الليلة التالية تلقائياً (السكربت لا يتقدّم إلا بعد أن يؤكد GitHub الحفظ).
- **الإيقاف**: احذف المشغّل من Triggers، واحذف المفتاح من GitHub (Fine-grained tokens ← المفتاح ← Delete).

---

# Nightly reports sync (English)

Every report the setup sends (`optimize/share.py` on a computer, `SetupReport.java` on the TV) becomes a row of the Google Form's responses
sheet. `apps-script/sync.gs` runs in that sheet each night and commits only the rows added since its last run to this private repository as
`reports/YYYY-MM-DD.csv` (Riyadh's day). Nothing else leaves the sheet. **The token is never in a chat, a file or a message**: only in the
script's Script Properties.

1. **Token** (once): GitHub as tshi-dev → Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token.
   Name `urtv-reports-sync`; an expiry (note it); owner `tshi-dev`; **Only select repositories → tshi-dev/urtv-reports**; Repository
   permissions → **Contents: Read and write**, nothing else. Copy it once, straight into step 3.
2. **Script**: the responses sheet → Extensions → Apps Script. Project Settings → show `appsscript.json`. Replace `Code.gs` with
   `apps-script/sync.gs` and `appsscript.json` with `apps-script/appsscript.json`. Save.
3. **Properties**: Project Settings → Script Properties: `GITHUB_TOKEN` = the token. Optional: `SHEET_NAME` (the responses sheet's name
   when there are several), `LAST_ROW` (the last row already present, to start from now; otherwise the first run sends everything from row 2).
4. **Dry run**: run `dryRun`. Google asks for exactly two permissions: this spreadsheet, and connecting to an external service. The log says
   what would be sent; nothing is sent or changed.
5. **First run**: run `syncReports`; a file appears in `reports/`.
6. **Nightly**: Triggers → Add Trigger → `syncReports`, Head, Time-driven, Day timer, e.g. 2am–3am; failure notifications daily.

How it stays safe: `@OnlyCurrentDoc` and the manifest's two scopes (`spreadsheets.currentonly`, `script.external_request`); no trigger
management in code (the trigger is added by hand); `LAST_ROW` moves only after GitHub confirms the save, so a failed night is retried; a
second run on the same day appends to that day's file; at most 2000 rows per run. To stop: delete the trigger and the token.
