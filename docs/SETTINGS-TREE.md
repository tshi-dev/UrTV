# شجرة الإعدادات

كل إعداد في التلفزيون بمكانه الكامل، مولّدة من الكود (`.github/tools/settings_tree.py`: SettingsHub وPrefs وdocs/SETTINGS.md وصفحة الجوال ومركز التحكم).
لا تعدّلها باليد: شغّل الأداة بعد أي تغيير في الإعدادات، و`tests/settings_tree_test.py` يفشل إذا صارت قديمة.

Every setting of the TV with its full path, generated from the code; do not edit by hand. «الوكيل» is the agent's command, «صفحة الجوال» the phone
page's key, «مركز التحكم» the control centre's row that opens it; «الأصل» is the default.

```text
الإعدادات
— اللغة
—— اللغة
   الوكيل: optimize/saver.sh lang en
   صفحة الجوال: lang
—— الأرقام: هندية ١٢٣ / عربية 123 (الأصل: هندية ١٢٣)
   الوكيل: optimize/saver.sh digits latin
—— أسماء الأشهر الميلادية
   الوكيل: optimize/saver.sh months levant
—— نظام الساعة
—— فرق التاريخ الهجري
   الوكيل: optimize/saver.sh hijrioffset -1
— الموقع والطقس
—— المدينة
   الوكيل: optimize/saver.sh city '<name>' <lat> <lon> <time zone> [country code]
   صفحة الجوال: city
   مركز التحكم: city
—— الدولة
—— الطقس
——— التحية والطقس
    الوكيل: optimize/saver.sh greeting off
——— الرطوبة والرياح
    الوكيل: optimize/saver.sh wxmore on
— الشكل والمشاهد
—— الشكل الجاهز: شكل تلفزيونك / شكل كنبة (الأصل: شكل تلفزيونك)
   الوكيل: optimize/saver.sh look urtv
—— التخطيط: تلفزيونك / كنبة / كرة القدم (الأصل: تلفزيونك)
   الوكيل: optimize/saver.sh homelayout classic
—— الخلفية: صوري وحزم الصور / المشاهد (الأصل: صوري وحزم الصور)
   الوكيل: optimize/saver.sh background photos
—— الساعة: عادية / زجاجية خلف الصورة (الأصل: عادية)
   الوكيل: optimize/saver.sh clock plain
—— شكل التطبيقات: إطار / تكبير وظل (الأصل: إطار)
   الوكيل: optimize/saver.sh appsstyle frame
—— المشاهد
——— حزم المشاهد: مشاهد السعودية / مشاهد Couch TV (الأصل: مشاهد السعودية)
    الوكيل: optimize/saver.sh scenepacks saudi
——— بقاء كل مشهد: ١٠ ثوانٍ / ٢٠ ثانية / دقيقة واحدة / خمس دقائق (الأصل: ٢٠ ثانية)
    الوكيل: optimize/saver.sh scenestay 10
——— المشهد يتبع الوقت: مفعّل / متوقف (الأصل: متوقف)
    الوكيل: optimize/saver.sh scenetime on
——— المشهد يتبع الطقس: مفعّل / متوقف (الأصل: متوقف)
    الوكيل: optimize/saver.sh sceneweather on
—— الألوان
——— المظهر
—— الساعة والنص
——— حجم الساعة: كبيرة / متوسطة / صغيرة (الأصل: كبيرة)
    الوكيل: optimize/saver.sh clocksize <large, medium, small>
——— إظهار الثواني
——— نص أوضح
    الوكيل: optimize/saver.sh cleartext on
——— توهّج لون التطبيق
    الوكيل: optimize/saver.sh glow light
——— تعتيم الليل
    الوكيل: optimize/saver.sh nightdim off
——— صفحات الأسهم والمباريات داكنة
    الوكيل: optimize/saver.sh pagesdark off
—— البطاقات
——— شكل البطاقة: الحالية / داكنة / زجاجية / داكنة بإطار / زجاجية بإطار / مربعات (الأصل: الحالية)
    الوكيل: optimize/saver.sh cardstyle <current, dark1, glass1, dark3, glass3, boxes>
——— البطاقة الأولى
    صفحة الجوال: home_cards
——— البطاقة الثانية
——— البطاقة الثالثة
——— أكمل المشاهدة
    الوكيل: optimize/saver.sh watchnext off
—— الصور
——— تبديل الصورة كل
    الوكيل: optimize/saver.sh photos <1, 5, 10, 15, 30, 60>
——— صور شاشة التوقف
——— صوري
    الوكيل: optimize/photos.sh add <files they named>
——— سرعة ألبوم صوري
——— شكل صورة ألبوم صوري
——— حجم ألبوم صوري
——— عنوان ألبوم صوري
——— تاريخ الصورة في ألبوم صوري
— التطبيقات
—— تطبيقات في الشريط: 5 / 6 / 7 / 8 / 9 / 10 / all (الأصل: 5)
   صفحة الجوال: fav_bar
—— ترتيب التطبيقات المفضلة
—— تعديل المفضلة
   الوكيل: optimize/dock.sh set
—— تطبيقات مقترحة
—— ترتيب كل التطبيقات
—— أسماء المصادر
—— قفل التطبيقات
—— تفاصيل التطبيقات
—— الشاشة الرئيسية للتلفزيون
——— اجعل UrTV الشاشة الرئيسية
——— تخلّص من واجهة المصنع
——— رجّع واجهة المصنع
——— قبل ما تحذف التطبيق…
——— ⁦UrTV⁩ النظيف
——— الخروج من تلفزيونك
— الأزرار السريعة
—— مركز التحكم
—— زر مركز التحكم
—— زر الجوال
—— زر المهام
   الوكيل: optimize/saver.sh taskspill off
—— زر الأسواق
—— زر المباريات
—— زر تلاوة وأذان
   الوكيل: optimize/saver.sh listenpill off
—— جرس التحديثات
   الوكيل: optimize/saver.sh updatebell off
—— الرجوع يفتح شاشة التوقف
— الصلاة والأذان
—— الأذان
   الوكيل: optimize/music.sh adhan
   صفحة الجوال: adhan_on, adhan_short, adhan_stopkey, adhan_voice, adhan_fajr_voice, adhan_volume
—— تنبيه قبل الأذان
   الوكيل: optimize/saver.sh adhanbar off
—— الشروق بعد الفجر
   الوكيل: optimize/saver.sh sunrise on
—— شريط الوقت بين الصلاتين
   الوكيل: optimize/saver.sh prayerbar on
—— طريقة الحساب
   الوكيل: optimize/saver.sh prayermethod gulf
—— تعديل وقت الصلاة
—— افتح تلاوة وأذان
— المباريات والأسواق
—— المباريات
——— مباريات اليوم
——— الفرق المتابعة
——— تطبيق المباريات
    الوكيل: optimize/saver.sh matchapp auto
——— الدوريات كاملة
——— رياضات أخرى
    الوكيل: optimize/saver.sh events f1,ufc,boxing
—— صفحة الأسواق
——— صفحة الأسواق
——— شريط الأسواق
    الوكيل: optimize/saver.sh markets off
——— الأسواق
    الوكيل: optimize/saver.sh markets <up to 4 of tasi, aramco, spx, btc, gold, silver, oil, xrp>
——— عرض الأسواق: الاتجاه فقط ▲▼ / السعر والنسبة (الأصل: الاتجاه فقط ▲▼)
    الوكيل: optimize/saver.sh mkstyle prices
—— الأسهم
——— قائمة الأسهم
——— تغيّر الأسهم: بالنسبة ٪ / بالمبلغ (الأصل: بالنسبة ٪)
—— جديد المسلسلات
——— التصنيفات
——— اللغات
——— مسلسلات مختارة
——— أقل تقييم
— شاشة التوقف
—— معاينة الشاشة
   الوكيل: optimize/shot.sh screensaver
—— بدء شاشة التوقف بعد
   الوكيل: optimize/saver.sh idle <1, 2, 5, 10, 15, 30>
   صفحة الجوال: saver_idle
—— بجانب الساعة
——— المحتوى
    الوكيل: optimize/saver.sh card <verse, wisdom, adhkar, stocks, tasks, year, shows, prayers, forecast, hourly, prayerrow, matches, none>
——— بطاقة ثانية
    الوكيل: optimize/saver.sh card2 <any choice of card but nextprayer and the first card>
——— تبديل الآية والحديث
    الوكيل: optimize/saver.sh verse 3
——— الآية والحديث
    الوكيل: optimize/saver.sh versekind verse
——— الخانة اليسرى: المباريات / الطقس / الصيام / عدّ تنازلي / الأسواق / بدون (الأصل: المباريات)
    الوكيل: optimize/saver.sh slot <matches, weather, fasting, countdown, markets, none>
——— العدّ التنازلي
    الوكيل: optimize/saver.sh countdown <YYYY-MM-DD> '<name>'
——— شريط السنة تحت الساعة
    الوكيل: optimize/saver.sh yearstrip on
——— المناسبة القادمة
    الوكيل: optimize/saver.sh occasion off
——— المناسبات الوطنية: تلقائي / إيقاف (الأصل: تلقائي)
    الوكيل: optimize/saver.sh national off
— العائلة والأمان
—— الرقابة الأبوية
—— تصفية المواقع
—— تحكم من الجوال
— حول
—— تحديث تلفزيوني
—— التحديث التلقائي للصفحة
—— تحديث صفحة شاشة التوقف
—— إعداد تلفزيوني
—— مسح الملفات المؤقتة
—— أرسل تقرير
```
