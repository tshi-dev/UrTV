package sa.urtv.home.tilawah;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sa.urtv.home.S;

/**
 * 4.4 «صوتياتي»: the person's sound files on the TV. Added from a USB stick (its folders and its sound files, browsed here; the file is
 * copied in, never played from the stick) or from the phone page («صوتياتي»). Each one can be heard, used as «أذاني الخاص» or «أذان الفجر
 * الخاص», or deleted. «سعة أكبر» raises the caps behind a red warning about the TV's storage. With EXTRA_PICK the screen hands back the file
 * the person chose (a dua's sound).
 */
public class AudioActivity extends RowScreen {
    /** pick a file and give its id back (RESULT_OK, "id") */
    static final String EXTRA_PICK = "pick";
    /** 4.4 beta 4: «أذاني من ملف» in «المؤذن»: "own" or "own_fajr": the file added here becomes that adhan at once (nothing else is asked) and the screen closes */
    static final String EXTRA_USE = "use";
    private static final int ASK_READ = 7;

    private boolean pick;
    private String useAs;
    private MediaPlayer preview;

    @Override protected void onCreate(android.os.Bundle state) {
        pick = getIntent().getBooleanExtra(EXTRA_PICK, false);
        String u = getIntent().getStringExtra(EXTRA_USE);
        useAs = "own".equals(u) || "own_fajr".equals(u) ? u : null;
        super.onCreate(state);
    }

    @Override protected void onPause() {
        super.onPause();
        stopPreview();
    }

    @Override Page first() {
        return new Page() {
            @Override public String title() { return S.t("صوتياتي"); }
            @Override public String tagline() { return pick ? S.t("اختر الصوت") : S.t("أدعية وتذكيرات وأذانك الخاص، محفوظة في التلفزيون"); }
            @Override public void rows(List<Row> out) {
                out.add(Row.head(S.t("إضافة")));
                out.add(Row.of(S.t("أضف من فلاشة أو التنزيلات"), S.t("يُنسخ الملف إلى التلفزيون ولا يُشغَّل من الفلاشة"), AudioActivity.this::openUsb));
                out.add(Row.of(S.t("من الجوال"), S.t("صفحة الجوال، ثم «صوتياتي»"),
                        () -> toast(S.t("افتح صفحة الجوال من زر «الجوال» في الشاشة الرئيسية، ثم «صوتياتي»، واختر الملف"))));
                List<AudioStore.Item> items = AudioStore.list(AudioActivity.this);
                boolean big = AudioStore.big(AudioActivity.this);
                out.add(Row.head(S.t("الملفات: %1$s من %2$s", AudioRules.size(AudioStore.used(AudioActivity.this)), AudioRules.size(AudioRules.totalMax(big)))));
                for (AudioStore.Item it : items) {
                    Row r = Row.of(it.name, line(it), () -> { if (pick) hand(it); else openItem(it); });
                    r.onLong(() -> listen(it));
                    out.add(r);
                }
                if (items.isEmpty()) out.add(Row.of(S.t("لا ملفات بعد"), S.t("أضف من الفلاشة أو من الجوال"), null));
                if (!pick) {
                    out.add(Row.head(S.t("السعة")));
                    out.add(Row.of(S.t("سعة أكبر"), big ? S.t("مفعّل: %1$s للملف و%2$s للكل", AudioRules.size(AudioRules.BIG_FILE_MAX), AudioRules.size(AudioRules.BIG_TOTAL_MAX))
                            : S.t("متوقف: %1$s للملف و%2$s للكل", AudioRules.size(AudioRules.FILE_MAX), AudioRules.size(AudioRules.TOTAL_MAX)), AudioActivity.this::toggleBig));
                }
            }
        };
    }

    @Override String hintText() { return S.t("حسنًا للفتح، ومطوّلًا للاستماع، ورجوع للعودة"); }

    private String line(AudioStore.Item it) {
        String use = AudioUse.of(this, it.id);
        String what = "own".equals(use) ? S.t("أذاني الخاص") : "own_fajr".equals(use) ? S.t("أذان الفجر الخاص") : "iqama".equals(use) ? S.t("صوت الإقامة") : "";
        String len = AudioText.length(it.ms);
        return what.isEmpty() ? S.t("%1$s، %2$s", AudioRules.size(it.bytes), len) : S.t("%1$s، %2$s، %3$s", AudioRules.size(it.bytes), len, what);
    }

    // ---- one file

    private void openItem(AudioStore.Item it) {
        open(new Page() {
            @Override public String title() { return it.name; }
            @Override public String tagline() { return line(it); }
            @Override public void rows(List<Row> out) {
                out.add(Row.of(S.t("استمع"), S.t("أول ١٥ ثانية"), () -> listen(it)));
                out.add(Row.of(S.t("استخدمه لأذاني الخاص"), S.t("لكل الصلوات، والفجر إن لم يكن له تسجيل"), () -> use(it, "own")));
                out.add(Row.of(S.t("استخدمه لأذان الفجر الخاص"), S.t("للفجر فقط"), () -> use(it, "own_fajr")));
                String use = AudioUse.of(AudioActivity.this, it.id);
                if ("own".equals(use) || "own_fajr".equals(use)) out.add(Row.of(S.t("أوقف استخدامه للأذان"), S.t("يرجع الأذان لمؤذنه"), () -> use(it, "none")));
                out.add(Row.red(S.t("حذف"), S.t("من التلفزيون"), () -> ask(S.t("تحذف «%s»؟", it.name), S.t("ما يُحذف من الفلاشة ولا من الجوال"), false,
                        new String[]{S.t("حذف"), S.t("إلغاء")},
                        new Runnable[]{() -> { stopPreview(); AudioUse.forget(AudioActivity.this, it.id); AudioStore.remove(AudioActivity.this, it.id); back(); }, null})));
            }
        });
    }

    private void use(AudioStore.Item it, String as) {
        String why = AudioUse.set(this, it, as);
        if ("too_long".equals(why)) toast(S.t("الأذان خمس دقائق على الأكثر"));
        else if (why != null) toast(S.t("تعذّر نسخ الملف"));
        else toast("none".equals(as) ? S.t("رجع الأذان لمؤذنه") : S.t("صار أذانك ✓"));
        refresh();
    }

    private void hand(AudioStore.Item it) {
        setResult(RESULT_OK, new Intent().putExtra("id", it.id));
        finish();
    }

    // ---- listening: a short preview here, never through the player (nothing else stops for it)

    private void listen(AudioStore.Item it) {
        stopPreview();
        try {
            preview = new MediaPlayer();
            preview.setDataSource(AudioStore.file(this, it).getPath());
            preview.setOnPreparedListener(MediaPlayer::start);
            preview.setOnCompletionListener(mp -> stopPreview());
            preview.prepareAsync();
            sa.urtv.home.S.init(this);
            new android.os.Handler(getMainLooper()).postDelayed(this::stopPreview, 15_000);
        } catch (Exception e) {
            stopPreview();
            toast(S.t("هذا ليس ملفًا صوتيًا يشتغل على التلفزيون"));
        }
    }

    private void stopPreview() {
        if (preview == null) return;
        try { preview.release(); } catch (RuntimeException ignored) { }
        preview = null;
    }

    // ---- «سعة أكبر»: behind a red warning

    private void toggleBig() {
        if (AudioStore.big(this)) {
            if (!AudioStore.setBig(this, false)) toast(S.t("احذف بعض الملفات أولًا: المحفوظ أكبر من السعة العادية"));
            refresh();
            return;
        }
        ask(S.t("سعة أكبر"), S.t("تنبيه: الملفات الكبيرة تأخذ من مساحة التلفزيون نفسها. إذا امتلأت قد تبطؤ التطبيقات أو يتوقف تحديثها. فعّلها فقط إذا كان في التلفزيون مساحة فارغة كافية."), true,
                new String[]{S.t("فعّل: %1$s للملف و%2$s للكل", AudioRules.size(AudioRules.BIG_FILE_MAX), AudioRules.size(AudioRules.BIG_TOTAL_MAX)), S.t("إلغاء")},
                new Runnable[]{() -> { AudioStore.setBig(this, true); refresh(); }, null});
    }

    // ---- the USB stick: its folders and sound files

    private void openUsb() {
        if (!mayRead()) { askRead(); return; }
        List<File> roots = usbRoots();
        if (roots.isEmpty()) { toast(S.t("ما وجدنا فلاشة ولا ملفات في التنزيلات: ركّب الفلاشة وانتظر ثواني")); return; }
        if (roots.size() == 1) { openFolder(roots.get(0), roots.get(0)); return; }
        open(new Page() {
            @Override public String title() { return S.t("أضف من فلاشة أو التنزيلات"); }
            @Override public String tagline() { return S.t("اختر الفلاشة"); }
            @Override public void rows(List<Row> out) { for (File r : roots) out.add(Row.of(r.equals(downloads()) ? S.t("التنزيلات") : r.getName(), "", () -> openFolder(r, r))); }
        });
    }

    private void openFolder(File root, File dir) {
        open(new Page() {
            @Override public String title() { return S.t("أضف من فلاشة أو التنزيلات"); }
            @Override public String tagline() { return dir.equals(root) ? S.t("يُنسخ الملف إلى التلفزيون ولا يُشغَّل من الفلاشة") : dir.getName(); }
            @Override public void rows(List<Row> out) {
                File[] all = dir.listFiles();
                if (all == null) all = new File[0];
                Arrays.sort(all, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                long max = AudioRules.fileMax(AudioStore.big(AudioActivity.this));
                int shown = 0;
                for (File f : all) {
                    if (f.isHidden() || f.getName().startsWith(".")) continue;
                    if (f.isDirectory()) { out.add(Row.of(f.getName(), S.t("مجلد"), () -> openFolder(root, f))); shown++; }
                }
                for (File f : all) {
                    if (!f.isFile() || !AudioRules.listed(f.getName())) continue;
                    if (f.length() > max) out.add(new Row(false, false, f.getName(), S.t("أكبر من %s: لا يُنسخ", AudioRules.size(max))));
                    else out.add(Row.of(f.getName(), AudioRules.size(f.length()), () -> copy(f)));
                    shown++;
                }
                if (shown == 0) out.add(Row.of(S.t("لا ملفات صوتية هنا"), S.t("mp3 أو m4a أو ogg أو wav"), null));
            }
        });
    }

    /** the copy runs off the screen's thread; the screen says what happened and then asks what the file is for */
    private void copy(File f) {
        toast(S.t("ينسخ «%s»…", f.getName()));
        new Thread(() -> {
            AudioStore.Added a = AudioStore.add(this, f, f.getName());
            // «أذاني من ملف»: the copy into the adhan and what follows it run here, off the screen's thread (review F93); the screen only says the result
            final String why = a.item != null && !pick && useAs != null ? AudioUse.set(this, a.item, useAs) : null;
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if (a.item == null) { toast(AudioStore.refusal(this, a.why)); return; }
                while (pagesAboveFirst()) back();
                refresh();
                if (pick) { hand(a.item); return; }
                AudioStore.Item it = a.item;
                if (useAs != null) {   // «أذاني من ملف»: it is that adhan now, no question
                    if ("too_long".equals(why)) toast(S.t("الأذان خمس دقائق على الأكثر"));
                    else if (why != null) toast(S.t("تعذّر نسخ الملف"));
                    else { toast(S.t("صار أذانك ✓")); finish(); }
                    return;
                }
                ask(it.name, S.t("وصل للتلفزيون ✓ استخدمه لـ"), false,
                        new String[]{S.t("أذاني الخاص"), S.t("أذان الفجر الخاص"), S.t("احفظه فقط")},
                        new Runnable[]{() -> use(it, "own"), () -> use(it, "own_fajr"), null});
            });
        }, "audio-copy").start();
    }

    private int depth;
    @Override void open(Page p) { depth++; super.open(p); }
    @Override void back() { if (depth > 0) depth--; super.back(); }
    private boolean pagesAboveFirst() { return depth > 0; }

    /** the TV's «التنزيلات» folder, or null when it is not there */
    private static File downloads() {
        try {
            File d = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            return d != null && d.isDirectory() ? d : null;
        } catch (RuntimeException e) { return null; }
    }

    /** the places to add from: «التنزيلات» of the TV, and the removable volumes Android sees now (a USB stick, an SD card) */
    private List<File> usbRoots() {
        List<File> out = new ArrayList<>();
        File dl = downloads();
        if (dl != null) out.add(dl);
        try {
            StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);
            for (StorageVolume v : sm.getStorageVolumes()) {
                if (!v.isRemovable() || !android.os.Environment.MEDIA_MOUNTED.equals(v.getState())) continue;
                File d = Build.VERSION.SDK_INT >= 30 ? v.getDirectory() : (v.getUuid() == null ? null : new File("/storage/" + v.getUuid()));
                if (d != null && d.isDirectory()) out.add(d);
            }
        } catch (RuntimeException ignored) { }
        return out;
    }

    private String readPermission() {
        return Build.VERSION.SDK_INT >= 33 ? "android.permission.READ_MEDIA_AUDIO" : android.Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean mayRead() { return checkSelfPermission(readPermission()) == PackageManager.PERMISSION_GRANTED; }

    private void askRead() { requestPermissions(new String[]{readPermission()}, ASK_READ); }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code != ASK_READ) return;
        if (mayRead()) openUsb(); else toast(S.t("بدون إذن قراءة الملفات ما نقدر نشوف الفلاشة. تقدر تضيف من الجوال"));
    }
}
