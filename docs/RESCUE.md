# إنقاذ تلفزيونك — لو صار شيء بعد الإعداد

**بالعربي أولاً. English below.**

UrTV ما يوقف الشاشة الأصلية للتلفزيون ولا «وسيط الإعداد» (من 4.3.2)، فما يقدر يسبب شاشة سوداء. ولو صار شيء غريب (زر الرئيسية ما يفتح شيئاً، أو التلفزيون يعيد التشغيل)، ترجّع التلفزيون كما كان بواحدة من هذي الطرق. **ابدأ بالأولى**، وكل طريقة بعدها تحتاج أكثر:

| الطريقة | تحتاج |
|---|---|
| ١. التلفزيون وحده | الريموت فقط |
| ٢. «إنقاذ تلفزيونك» | الريموت و Downloader |
| ٣. جوال أندرويد | جوال أندرويد على نفس الواي فاي |
| ٤. آيفون | آيفون على نفس الواي فاي (أبطأ) |
| ٥. أي كمبيوتر فيه Chrome | كيبل USB، ويشتغل على قليل من التلفزيونات |

## ١. من التلفزيون وحده (الريموت فقط)

1. **الإعدادات ← التطبيقات ← التطبيقات الافتراضية ← الشاشة الرئيسية** (أو «تطبيق الصفحة الرئيسية») واختر الشاشة الأصلية (Google TV أو Android TV Home). بعض التلفزيونات ما فيها هذا الخيار: كمّل للخطوة ٢.
2. لو الشاشة الأصلية متوقفة: **الإعدادات ← التطبيقات ← عرض كل التطبيقات ← إظهار تطبيقات النظام**، افتح **Google TV Home** (أو **Android TV Home**) واضغط **تفعيل**. وبنفس الطريقة **وسيط الإعداد** (Setup / Android TV Setup) لو كان متوقفاً.
3. اضغط زر الرئيسية في الريموت.

## ٢. على التلفزيون: «إنقاذ تلفزيونك» (بلا جوال)

1. اضغط **زر الإعدادات** في الريموت ← **التطبيقات** ← **Downloader** ← **فتح**. (لو ما عندك Downloader: ثبّته من متجر Play.)
2. اكتب في Downloader الرابط: `https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-rescue.apk` (أو كود Downloader المكتوب على شاشة «إعداد تلفزيوني» لما يتوفر)، وثبّت **«إنقاذ تلفزيونك»**.
3. افتحه واضغط **«رجّع التلفزيون كما كان»**. لو سألك التلفزيون «السماح بتصحيح USB؟» اختر **«السماح دائماً»**.

يشغّل كل ما أوقفه إعدادنا والشاشة الأصلية ووسيط الإعداد، ويرجّع زر الرئيسية للشاشة الأصلية، ويطفي «إضافات تلفزيونك». ما يحذف شيئاً. يحتاج «تصحيح USB» مفعّلاً (الإعدادات ← تفضيلات الجهاز ← حول ← اضغط «Build» سبع مرات ← خيارات المطوّر ← تصحيح USB).

## ٣. من جوال أندرويد

1. ثبّت على جوالك تطبيقاً مجانياً للـ ADB مثل **Bugjaeger** أو **Remote ADB Shell** من متجر Play.
2. الجوال والتلفزيون على نفس الواي فاي. في التلفزيون: «تصحيح USB» مفعّل (والتصحيح عبر الشبكة إن وُجد). اعرف عنوان التلفزيون من الإعدادات ← الشبكة.
3. في التطبيق اتصل بعنوان التلفزيون (المنفذ 5555)، ووافق على رسالة التلفزيون، ثم الصق هذا السطر كما هو:

```
pm enable com.google.android.apps.tv.launcherx; pm enable com.google.android.tvlauncher; pm enable com.google.android.tungsten.setupwraith; cmd role add-role-holder --user 0 android.app.role.HOME com.google.android.apps.tv.launcherx; cmd role add-role-holder --user 0 android.app.role.HOME com.google.android.tvlauncher; input keyevent KEYCODE_HOME
```

السطر يشغّل الشاشة الأصلية (Google TV أو Android TV) ووسيط الإعداد ويرجّع لها زر الرئيسية. أي سطر لا يخص تلفزيونك يطلع خطأ ويكمل الباقي، هذا طبيعي.

## ٤. من آيفون: iSH

يحتاج: تطبيق **iSH Shell** (مجاني في App Store)، الآيفون والتلفزيون على نفس الواي فاي، و«تصحيح USB» مع **التصحيح عبر الشبكة** في التلفزيون (المنفذ 5555). أبطأ من الجوال الأندرويد، وخلّ iSH مفتوحاً قدامك وهو يشتغل (الآيفون يوقفه لو طلعت منه).

1. افتح iSH واكتب (يحتاج إنترنت، مرة وحدة):
   `apk update && apk add android-tools`
2. اتصل بالتلفزيون (اكتب عنوانه مكان «عنوان_التلفزيون»)، ووافق على رسالة التلفزيون «السماح دائماً»:
   `adb connect عنوان_التلفزيون:5555`
3. اكتب `adb shell` واضغط إدخال، ثم الصق السطر نفسه من الطريقة ٣.

لو `adb connect` ما رد: تأكد إن التلفزيون يقبل التصحيح عبر الشبكة على 5555، أو استعمل الطريقة ٣ أو ٥.

## ٥. من أي كمبيوتر فيه Chrome (بلا تثبيت): Tango

يحتاج: Chrome أو Edge على كمبيوتر، و**كيبل USB** بين الكمبيوتر والتلفزيون، و«تصحيح USB» في التلفزيون. أغلب التلفزيونات منفذ USB فيها للفلاش فقط وما تقبل كمبيوتر، فهذي الطريقة تشتغل على أجهزة البث اللي منفذها يقبل كيبل بيانات (مثل بعض أجهزة Google TV وAndroid TV box). المتصفح ما يقدر يتصل بالتلفزيون عبر الواي فاي بلا برنامج إضافي.

1. افتح **tangoapp.dev** (Tango، مفتوح المصدر، رخصة MIT).
2. اضغط **Add** واختر التلفزيون، ووافق على رسالة التلفزيون.
3. افتح **Shell** والصق السطر نفسه من الطريقة ٣.

---

## English

UrTV never turns the TV's own home or its setup wizard off (since 4.3.2), so it cannot leave a black screen. If anything goes wrong, put the TV back with one of these routes, easiest first:

1. **The TV alone (remote only).** Settings → Apps → Default apps → Home app → the stock home (Google TV / Android TV Home); not every TV has it. If the stock home was turned off: Settings → Apps → See all apps → Show system apps → Google TV Home (or Android TV Home) → Enable; the same for the setup wizard. Press Home.
2. **On the TV: «إنقاذ تلفزيونك» (UrTV Rescue), no phone.** Remote's Settings key → Apps → Downloader → Open; enter `https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-rescue.apk` (or the Downloader code shown on the setup screen once it exists), install, open, press «رجّع التلفزيون كما كان». Accept «Allow USB debugging?» with «Always allow». It turns on what our setup turned off, the stock home and the setup wizard, gives Home back to the stock home and turns «إضافات تلفزيونك» off. Nothing is removed. Needs USB debugging on.
3. **Android phone** (Bugjaeger or Remote ADB Shell, free on Play): same Wi-Fi, connect to the TV's address on port 5555, accept the prompt, paste the line above.
4. **iPhone: iSH Shell** (free, App Store), same Wi-Fi, network debugging on the TV (port 5555): `apk update && apk add android-tools`, `adb connect TV_ADDRESS:5555`, accept the prompt, `adb shell`, paste the same line. Slow; keep iSH in front while it runs (iOS pauses it in the background).
5. **Any computer with Chrome, nothing installed: Tango** (tangoapp.dev, open source, MIT). Needs a USB data cable to the TV: most TVs' USB ports only take a flash drive, so this works on streaming boxes whose port takes a data cable. A browser cannot reach the TV over Wi-Fi without a helper program. Add → pick the TV → accept the prompt → Shell → paste the same line.
