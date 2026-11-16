# إنقاذ تلفزيونك — لو صار شيء بعد الإعداد

**بالعربي أولاً. English below.**

UrTV ما يوقف الشاشة الأصلية للتلفزيون ولا «وسيط الإعداد» (من 4.3.2)، فما يقدر يسبب شاشة سوداء. ولو صار شيء غريب (زر الرئيسية ما يفتح شيئاً، أو التلفزيون يعيد التشغيل)، ترجّع التلفزيون كما كان بواحدة من هذي الطرق، بلا كمبيوتر.

## ١. من التلفزيون نفسه: «إنقاذ تلفزيونك»

1. اضغط **زر الإعدادات** في الريموت ← **التطبيقات** ← **Downloader** ← **فتح**. (لو ما عندك Downloader: ثبّته من متجر Play.)
2. اكتب في Downloader الرابط: `https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-rescue.apk` (أو كود Downloader المكتوب على شاشة «إعداد تلفزيوني» لما يتوفر)، وثبّت **«إنقاذ تلفزيونك»**.
3. افتحه واضغط **«رجّع التلفزيون كما كان»**. لو سألك التلفزيون «السماح بتصحيح USB؟» اختر **«السماح دائماً»**.

يشغّل كل ما أوقفه إعدادنا والشاشة الأصلية ووسيط الإعداد، ويرجّع زر الرئيسية للشاشة الأصلية، ويطفي «إضافات تلفزيونك». ما يحذف شيئاً. يحتاج «تصحيح USB» مفعّلاً (الإعدادات ← تفضيلات الجهاز ← حول ← اضغط «Build» سبع مرات ← خيارات المطوّر ← تصحيح USB).

## ٢. من الجوال (أندرويد) لو الريموت ما يوصلك لـ Downloader

1. ثبّت على جوالك تطبيقاً مجانياً للـ ADB مثل **Bugjaeger** أو **Remote ADB Shell** من متجر Play.
2. الجوال والتلفزيون على نفس الواي فاي. في التلفزيون: «تصحيح USB» مفعّل (والتصحيح عبر الشبكة إن وُجد). اعرف عنوان التلفزيون من الإعدادات ← الشبكة.
3. في التطبيق اتصل بعنوان التلفزيون (المنفذ 5555)، ووافق على رسالة التلفزيون، ثم الصق هذا السطر كما هو:

```
pm enable com.google.android.apps.tv.launcherx; pm enable com.google.android.tvlauncher; pm enable com.google.android.tungsten.setupwraith; cmd role add-role-holder --user 0 android.app.role.HOME com.google.android.apps.tv.launcherx; cmd role add-role-holder --user 0 android.app.role.HOME com.google.android.tvlauncher; input keyevent KEYCODE_HOME
```

السطر يشغّل الشاشة الأصلية (Google TV أو Android TV) ووسيط الإعداد ويرجّع لها زر الرئيسية. أي سطر لا يخص تلفزيونك يطلع خطأ ويكمل الباقي، هذا طبيعي.

## ٣. من إعدادات التلفزيون فقط

الإعدادات ← التطبيقات ← التطبيقات الافتراضية ← **الشاشة الرئيسية** (أو «تطبيق الصفحة الرئيسية») واختر الشاشة الأصلية.

---

## English

UrTV never turns the TV's own home or its setup wizard off (since 4.3.2), so it cannot leave a black screen. If anything goes wrong, put the TV back with no computer:

1. **On the TV: «إنقاذ تلفزيونك» (UrTV Rescue).** Remote's Settings key → Apps → Downloader → Open; enter `https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-rescue.apk` (or the Downloader code shown on the setup screen once it exists), install, open, press «رجّع التلفزيون كما كان». Accept «Allow USB debugging?» with «Always allow». It turns on what our setup turned off, the stock home and the setup wizard, gives Home back to the stock home and turns «إضافات تلفزيونك» off. Nothing is removed.
2. **From an Android phone** (Bugjaeger or Remote ADB Shell, free on Play): same Wi-Fi, connect to the TV's address on port 5555, accept the prompt, paste the line above.
3. **TV settings only:** Settings → Apps → Default apps → Home app → the stock home.
