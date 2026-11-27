# شجرة الإعدادات

كل إعداد في التلفزيون بمكانه الكامل، مولّدة من الكود (`.github/tools/settings_tree.py`: SettingsHub وPrefs وdocs/SETTINGS.md وصفحة الجوال ومركز التحكم).
لا تعدّلها باليد: شغّل الأداة بعد أي تغيير في الإعدادات، و`tests/settings_tree_test.py` يفشل إذا صارت قديمة.

بين القوسين أين يظهر الإعداد. «الأصل» القيمة قبل أي تغيير، و«مثال للوكيل» أمر واحد كمثال وتحته القيم اللي يقبلها.

Every setting of the TV with its full path, generated from the code; do not edit by hand. [ ] says where it shows (the home, the screensaver,
both, or the whole TV); «الأصل» is the default, «مثال للوكيل» one example of the agent's command with the values it accepts under it; «صفحة الجوال»
the phone page's key, «مركز التحكم» the control centre's row that opens it; «من التلفزيون فقط» a row with no command on purpose.

## «شكل كنبة» مفعّل: الشاشة الجديدة (القائمة الجانبية)

```text
الإعدادات
— اللغة
—— اللغة [كل التلفزيون]
   مثال للوكيل: optimize/saver.sh lang en  (`ar` = Arabic, the default; anything else is refused and nothing is sent; `optimize/saver.sh show` prints `lang:`)
     القيم المقبولة: ar|en
   صفحة الجوال: lang
—— الأرقام [الرئيسية وشاشة التوقف]
   الخيارات: هندية ١٢٣ / عربية 123
   الأصل: هندية ١٢٣
   مثال للوكيل: optimize/saver.sh digits latin  (`hindi` = ١٢٣, the default)
     القيم المقبولة: hindi|latin
—— التاريخ بالأرقام [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh datenum on  (`off`, the default: in words)
     القيم المقبولة: on|off
—— أسماء الأشهر الميلادية [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh months levant  (`default` = يناير، فبراير… as always)
     القيم المقبولة: default|levant
—— نظام الساعة [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh clock24 on
     القيم المقبولة: on|off
—— فرق التاريخ الهجري [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh hijrioffset -1  (whole days, `-2` to `+2`; `0` = Umm al-Qura as it is, the default; nothing else is accepted)
     القيم المقبولة: -2|-1|0|+1|+2   (days; 0 = Umm al-Qura as it is)
— الموقع والطقس
—— المدينة [كل التلفزيون]
   مثال للوكيل: optimize/saver.sh city <name>  (Arabic or English; a short form is fine if only one city matches; use `city 'GB|London'` to qualify the country)
   مثال للوكيل: optimize/saver.sh city '<name>' <lat> <lon> <time zone> [country code]  (`… 25.86 43.49 Asia/Riyadh` in Saudi Arabia, `… 29.37 47.98 Asia/Kuwait` elsewhere)
   صفحة الجوال: city
   مركز التحكم: city
—— الدولة [كل التلفزيون]
   مثال للوكيل: optimize/saver.sh city '<name>' <lat> <lon> <time zone> SA
—— الطقس
——— التحية والطقس [الرئيسية]
    مثال للوكيل: optimize/saver.sh greeting off
      القيم المقبولة: on|off
——— اسمك في التحية [الرئيسية]
    مثال للوكيل: optimize/saver.sh greetingname علاء  (a name of at most 30 letters on one line; the name is isolated, so Latin letters sit right in an Arabic line)
      القيم المقبولة: NAME|off   (the name after the greeting on the home, at most 30 letters)
    مثال للوكيل: optimize/saver.sh greetingname off
      القيم المقبولة: NAME|off   (the name after the greeting on the home, at most 30 letters)
——— الرطوبة والرياح [شاشة التوقف]
    مثال للوكيل: optimize/saver.sh wxmore on  (`off` = the default)
      القيم المقبولة: on|off
— المظهر
—— أشكال جاهزة والتخطيط
——— المظهر على الرئيسية [الرئيسية]
    مثال للوكيل: optimize/saver.sh lookpanel
——— رجّع الشكل السابق [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh lookback
——— أشكال جاهزة [الرئيسية وشاشة التوقف]
    الخيارات: شكلك المعتاد / كنبة / tvOS / صورة فقط / مواقيت يومي / مباريات اليوم / ليل هادئ
    مثال للوكيل: optimize/saver.sh preset family  (tv, couch, clean, family, sport, calm, or a mix: couch-prayers, clean-icons, sport-night, tv-glass)
      القيم المقبولة: tv|couch|tvos|clean|family|sport|calm|couch-prayers|clean-icons|sport-night|tv-glass
——— شكل شاشة الإعدادات [كل التلفزيون]
    مثال للوكيل: optimize/saver.sh settingsstyle couch  (or `buttons`, `tvos`)
      القيم المقبولة: buttons|couch|tvos
——— تخصيص [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh preset tv
      القيم المقبولة: tv|couch|tvos|clean|family|sport|calm|couch-prayers|clean-icons|sport-night|tv-glass
——— رجّع بطاقاتك [الرئيسية]
    مثال للوكيل: optimize/saver.sh preset tv  (the person's own look and cards back)
      القيم المقبولة: tv|couch|tvos|clean|family|sport|calm|couch-prayers|clean-icons|sport-night|tv-glass
——— شكل كنبة [الرئيسية]
    الخيارات: مفعّل / متوقف
    الأصل: متوقف
    تشغيله يضبط مرة وحدة: التخطيط كنبة، الخلفية الآفاق، الساعة زجاجية، شكل التطبيقات أيقونات، ويحفظ اختياراتك؛ وإيقافه يرجّعها كما كانت. وتغيير أي واحد منها وهو مفعّل يبقيه مفعّلًا ويقول «مخصّص»
    مثال للوكيل: optimize/saver.sh couch on
      القيم المقبولة: on|off
——— التخطيط [الرئيسية]
    الخيارات: تلفزيونك / كنبة / كرة القدم / tvOS
    الأصل: تلفزيونك
    مثال للوكيل: optimize/saver.sh homelayout classic  (تلفزيونك, the default)
      القيم المقبولة: classic|couch|football
——— وضع الألوان [الرئيسية]
    مثال للوكيل: optimize/saver.sh look navy
      القيم المقبولة: glass|graphite|navy|clean|sand
    مثال للوكيل: optimize/saver.sh appearance auto
      القيم المقبولة: dark|light|auto
——— صفحات الأسهم والمباريات داكنة [الرئيسية]
    مثال للوكيل: optimize/saver.sh pagesdark off
      القيم المقبولة: on|off
——— لوحة المظهر على الرئيسية [الرئيسية]
    مثال للوكيل: optimize/saver.sh lookpanelrow on  (or `off`)
      القيم المقبولة: on|off
——— زر رجّع الشكل السابق [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh lookundo on  (or `off`)
      القيم المقبولة: on|off
——— تبديل الشكل بلا وميض [الرئيسية]
    مثال للوكيل: optimize/saver.sh smoothlook on  (or `off`)
      القيم المقبولة: on|off
—— الخلفية والآفاق
——— الخلفية [الرئيسية]
    الخيارات: صوري وحزم الصور / الآفاق
    الأصل: صوري وحزم الصور
    مثال للوكيل: optimize/saver.sh background photos  (the default)
      القيم المقبولة: photos|scenes
——— حزم الآفاق [الرئيسية]
    الخيارات: آفاق السعودية / آفاق عالمية
    الأصل: آفاق السعودية
    مثال للوكيل: optimize/saver.sh scenepacks saudi  (the default)
      القيم المقبولة: saudi|couch|saudi,couch
——— بقاء كل أفق [الرئيسية]
    الخيارات: ١٠ ثوانٍ / ٢٠ ثانية / دقيقة واحدة / خمس دقائق
    الأصل: ٢٠ ثانية
    مثال للوكيل: optimize/saver.sh scenestay 10
      القيم المقبولة: 10|20|60|300
——— الأفق يتبع الوقت [الرئيسية]
    الخيارات: مفعّل / متوقف
    الأصل: متوقف
    مثال للوكيل: optimize/saver.sh scenetime on  (`off`, the default)
      القيم المقبولة: on|off
——— الأفق يتبع الطقس [الرئيسية]
    الخيارات: مفعّل / متوقف
    الأصل: متوقف
    مثال للوكيل: optimize/saver.sh sceneweather on  (`off`, the default)
      القيم المقبولة: on|off
—— الساعة والنص
——— الساعة [الرئيسية]
    الخيارات: عادية / زجاجية خلف الصورة
    الأصل: عادية
    مثال للوكيل: optimize/saver.sh clock plain
      القيم المقبولة: plain|glass
——— حجم الساعة [شاشة التوقف]
    الخيارات: كبيرة / متوسطة / صغيرة
    الأصل: كبيرة
    مثال للوكيل: optimize/saver.sh clocksize <large, medium, small>  (`large` is the default and today's look, unchanged)
——— تصميم شاشة التوقف [شاشة التوقف]
    الخيارات: معلومات / نظيفة / ساعة كبيرة / لوحة / خط اليوم
    الأصل: معلومات
    مثال للوكيل: optimize/saver.sh design <info, clean, big, frame, dayline>  (`info` = «معلومات», the screensaver as it always was, the default)
——— رجوع مرتين يفتح شاشة التوقف [شاشة التوقف]
    مثال للوكيل: optimize/saver.sh backtwice on
      القيم المقبولة: on|off
——— الشريط في الوسط [شاشة التوقف]
    الخيارات: بدون / الصلوات / الخانة اليسرى / الطقس
    الأصل: بدون
    مثال للوكيل: optimize/saver.sh bar <none, prayers, slot, weather>  (`none` is the default)
——— مكان الشريط [شاشة التوقف]
    الخيارات: أسفل الشاشة / تحت الساعة
    الأصل: أسفل الشاشة
    مثال للوكيل: optimize/saver.sh barat <bottom, clock>  (`bottom` of the screen is the default; `clock` = under the clock)
——— مكان الطقس [شاشة التوقف]
    الخيارات: كما في التصميم / مكانه المعتاد / تحت الساعة / في الزاوية / مخفي
    الأصل: كما في التصميم
    مثال للوكيل: optimize/saver.sh wx <auto, usual, clock, corner, off>  (`auto` = as the design has it, the default: its place at the top in `info` and `dayline`, hidden in the others)
——— إظهار الثواني [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh seconds on  (`off` = the default, the clock as always)
      القيم المقبولة: on|off
    مثال للوكيل: optimize/saver.sh seconds on
      القيم المقبولة: on|off
——— نص أوضح [الرئيسية]
    مثال للوكيل: optimize/saver.sh cleartext on  (`off` = the default)
      القيم المقبولة: on|off
——— تعتيم الليل [شاشة التوقف]
    مثال للوكيل: optimize/saver.sh nightdim off  (`on` = the default, as always: darker from 21:00, the most from 23:00 to 05:00, normal again at 07:00, by the city's clock)
      القيم المقبولة: on|off
—— التطبيقات
——— شكل التطبيقات [الرئيسية]
    الخيارات: بنرات / أيقونات / مربعات ملونة / tvOS
    الأصل: مربعات ملونة
    مثال للوكيل: optimize/saver.sh appsstyle tiles  («مربعات ملونة», the default: today's tray of tiles, its focus as it was: the ring, or `zoom` where that was chosen)
      القيم المقبولة: tiles|banners|icons|glass
——— محاذاة التطبيقات [الرئيسية]
    مثال للوكيل: optimize/saver.sh appsalign center  (or `right`, `left`)
      القيم المقبولة: center|right|left
——— حجم شريط tvOS [الرئيسية]
    الخيارات: 50 / 60 / 70 / 80 / 90 / 100 / 110 / 120 / 130 / 140 / 150
    الأصل: 100
    مثال للوكيل: optimize/saver.sh glasssize 70  (50 to 150 in steps of 10; `100` the default)
      القيم المقبولة: 50..150 (steps of 10; 100 the default)
——— صفوف شريط tvOS [الرئيسية]
    الخيارات: صف واحد / صفّان / ثلاثة صفوف
    الأصل: صف واحد
    مثال للوكيل: optimize/saver.sh glassrows 2  (`1` the default, to `3`; «tvOS» only)
      القيم المقبولة: 1|2|3
——— توهّج لون التطبيق [الرئيسية]
    مثال للوكيل: optimize/saver.sh glow light  (`off` = the default, `light` = a third of the strength, `on` = full)
      القيم المقبولة: off|light|on
—— البطاقات
——— شكل البطاقة [الرئيسية وشاشة التوقف]
    الخيارات: الحالية / داكنة / زجاجية / داكنة بإطار / زجاجية بإطار / مربعات / الشكل السابق
    الأصل: الحالية
    مثال للوكيل: optimize/saver.sh cardstyle <current, dark1, glass1, dark3, glass3, boxes, classic>
——— البطاقة الأولى [الرئيسية]
    مثال للوكيل: optimize/saver.sh home-card prayers,hourly,verse  (a comma list, up to 3, in this order; one value works; `none` or `""` = no card)
    مثال للوكيل: optimize/saver.sh home-card prayers,hourly,verse
    صفحة الجوال: home_cards
——— البطاقة الثانية [الرئيسية]
    مثال للوكيل: optimize/saver.sh home-card prayers,hourly,verse
——— البطاقة الثالثة [الرئيسية]
    مثال للوكيل: optimize/saver.sh home-card prayers,hourly,verse
——— أكمل المشاهدة [الرئيسية]
    مثال للوكيل: optimize/saver.sh watchnext off  (`on`, the default)
      القيم المقبولة: on|off
—— الصور
——— صور شاشة التوقف [الرئيسية وشاشة التوقف]
    من التلفزيون فقط (بلا أمر، عن قصد)
——— صوري [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/photos.sh add <files they named>
    مثال للوكيل: optimize/photos.sh mode mix  (with ours, the default)
    مثال للوكيل: optimize/photos.sh mode only
    مثال للوكيل: optimize/photos.sh mode album
    مثال للوكيل: optimize/photos.sh list
    مثال للوكيل: optimize/photos.sh del N
    مثال للوكيل: optimize/photos.sh clear  (back to ours only)
    مثال للوكيل: optimize/photos.sh show  (a screenshot with them; it waits 70 s for the page to settle, so allow 80 s)
    مثال للوكيل: optimize/saver.sh homemine mix  (with «صورنا»)
      القيم المقبولة: same|mix|only|off
——— أضف من فلاشة أو التنزيلات [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/photos.sh add <files they named>
——— تبديل الصورة كل [شاشة التوقف]
    مثال للوكيل: optimize/saver.sh photos <1, 5, 10, 15, 30, 60>  (minutes; 30 = the default)
    مثال للوكيل: optimize/saver.sh photos 30s  (every 30 seconds)
      القيم المقبولة: 30s|1|5|10|15|30|60 (minutes; 30s = every 30 seconds)
—— ألبوم صوري
——— سرعة ألبوم صوري [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh albummode ten
      القيم المقبولة: seconds|ten|minute
——— شكل صورة ألبوم صوري [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh albumfit fill
      القيم المقبولة: fit|fill
——— حجم ألبوم صوري [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh albumsize tall
      القيم المقبولة: normal|tall
——— عنوان ألبوم صوري [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh albumtitle off
      القيم المقبولة: on|off
——— تاريخ الصورة في ألبوم صوري [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh albumdate off
      القيم المقبولة: on|off
— التطبيقات
—— الشاشة الرئيسية للتلفزيون
——— اجعل UrTV الشاشة الرئيسية [كل التلفزيون]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— تخلّص من واجهة المصنع [كل التلفزيون]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— رجّع واجهة المصنع [كل التلفزيون]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— قبل ما تحذف التطبيق… [كل التلفزيون]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— ⁦UrTV⁩ النظيف [الرئيسية وشاشة التوقف]
    من التلفزيون فقط (بلا أمر، عن قصد)
——— الخروج من تلفزيونك [كل التلفزيون]
    مثال للوكيل: optimize/saver.sh backexit hold  (a long Back, about a second, leaves)
      القيم المقبولة: press|hold|three
    مثال للوكيل: optimize/saver.sh backexit hold
      القيم المقبولة: press|hold|three
—— شريط التطبيقات
——— تطبيقات في الشريط [الرئيسية]
    الخيارات: 5 / 6 / 7 / 8 / 9 / 10 / all
    الأصل: 5
    مثال للوكيل: optimize/saver.sh favbar 7
      القيم المقبولة: 5|6|7|8|9|10|all
    صفحة الجوال: fav_bar
——— ترتيب التطبيقات المفضلة [الرئيسية]
    مثال للوكيل: optimize/dock.sh apps
    مثال للوكيل: optimize/dock.sh set <pkg> …  (1–10, in their order: exactly those, never topped up to five; editing the row on the TV keeps its floor of five)
    مثال للوكيل: optimize/dock.sh show
    مثال للوكيل: optimize/dock.sh reset
——— تعديل المفضلة [الرئيسية]
    مثال للوكيل: optimize/dock.sh set
——— تطبيقات مقترحة [الرئيسية]
    من التلفزيون فقط (بلا أمر، عن قصد)
——— ترتيب كل التطبيقات [الرئيسية]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— أسماء المصادر [الرئيسية]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— قفل التطبيقات [كل التلفزيون]
    مثال للوكيل: optimize/dock.sh pin lock com.netflix.ninja
——— تفاصيل التطبيقات [كل التلفزيون]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
— الأزرار السريعة
—— مركز التحكم [الرئيسية]
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
—— زر مركز التحكم [الرئيسية]
   مثال للوكيل: optimize/saver.sh pillshidden center
     القيم المقبولة: markets,matches,center,phone,inputs | none
—— زر الجوال [الرئيسية]
   مثال للوكيل: optimize/saver.sh pillshidden phone
     القيم المقبولة: markets,matches,center,phone,inputs | none
—— زر البحث [الرئيسية]
   مثال للوكيل: optimize/saver.sh searchpill on  (`off`, the default)
     القيم المقبولة: on|off
—— زر المهام [الرئيسية]
   مثال للوكيل: optimize/saver.sh taskspill off  (`on` brings it back; default on)
     القيم المقبولة: on|off
—— زر المصادر [الرئيسية]
   مثال للوكيل: optimize/saver.sh pillshidden inputs
     القيم المقبولة: markets,matches,center,phone,inputs | none
—— زر الأسواق [الرئيسية]
   مثال للوكيل: optimize/saver.sh pillshidden markets
     القيم المقبولة: markets,matches,center,phone,inputs | none
—— زر المباريات [الرئيسية]
   مثال للوكيل: optimize/saver.sh pillshidden matches
     القيم المقبولة: markets,matches,center,phone,inputs | none
—— زر تلاوة وأذان [الرئيسية]
   مثال للوكيل: optimize/saver.sh listenpill off  (`on` brings it back; default on, so nothing changes for anyone; `saver.sh show` says `listenpill:`)
     القيم المقبولة: on|off
—— جرس التحديثات [الرئيسية]
   مثال للوكيل: optimize/saver.sh updatebell off  (`on` brings it back; default on, so nothing changes for anyone; `saver.sh show` says `updatebell:`)
     القيم المقبولة: on|off
—— الرجوع يفتح شاشة التوقف [الرئيسية]
   مثال للوكيل: optimize/dock.sh backsaver on  (`off` = the default: Back does nothing at the top of the home, as always; no argument says `back-saver: on|off`)
—— زر الرئيسية يفتح تلفزيونك مباشرة [الرئيسية]
   مثال للوكيل: optimize/dock.sh home-key on\|off\|status
—— زر يوتيوب في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key youtube org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key youtube default
—— زر نتفليكس في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key netflix org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key netflix default
—— زر Prime Video في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key prime org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key prime default
—— زر ديزني بلس في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key disney org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key disney default
—— زر Paramount Plus في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key paramount org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key paramount default
—— زر Free TV في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key freetv org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key freetv default
—— الزر المخصص ★ في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key star org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key star default
—— زر BRAVIA CORE في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key core org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key core default
—— زر Crunchyroll في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key crunchyroll org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key crunchyroll default
—— زر Google Play في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key play org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key play default
—— زر SonyLIV في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key sonyliv org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key sonyliv default
—— زر Tabii في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key tabii org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key tabii default
—— زر TCL Channel في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key tclchannel org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key tclchannel default
—— زر Okko في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key okko org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key okko default
—— زر VK في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key vk org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key vk default
—— زر Rutube في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key rutube org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key rutube default
—— زر KION في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key kion org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key kion default
—— زر Wink في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key wink org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key wink default
—— زر IVI في الريموت [الرئيسية]
   مثال للوكيل: optimize/dock.sh key ivi org.smarttube.stable
   مثال للوكيل: optimize/dock.sh key ivi default
— الصلاة والأذان
—— الأذان [كل التلفزيون]
   مثال للوكيل: optimize/music.sh adhan
   صفحة الجوال: adhan_on, adhan_short, adhan_stopkey, adhan_voice, adhan_fajr_voice, adhan_volume
—— الأذان لكل صلاة [كل التلفزيون]
   مثال للوكيل: optimize/music.sh adhan state <fajr|dhuhr|asr|maghrib|isha> <both|sound|screen|none>
   مثال للوكيل: optimize/music.sh adhan screen <prayer> on|off
   مثال للوكيل: optimize/music.sh adhan
   مثال للوكيل: optimize/music.sh adhan prayer <fajr|dhuhr|asr|maghrib|isha> <general|silent|own|own_fajr|1-21>
   صفحة الجوال: adhan_prayers
—— تنبيه قبل الأذان [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh adhanbar off  (or `on`)
     القيم المقبولة: on|off
   مثال للوكيل: optimize/saver.sh adhanlead 15  (or `10`, the default)
     القيم المقبولة: 10|15
—— الشروق بعد الفجر [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh sunrise on  (`off` = the default, the panel as always)
     القيم المقبولة: on|off
—— شريط الوقت بين الصلاتين [شاشة التوقف]
   مثال للوكيل: optimize/saver.sh prayerbar on  (`off` = the default)
—— طريقة الحساب [كل التلفزيون]
   مثال للوكيل: optimize/saver.sh prayermethod jafari
—— تعديل وقت الصلاة [كل التلفزيون]
   مثال للوكيل: optimize/saver.sh prayeradj fajr +2 maghrib -1 …  (fajr dhuhr asr maghrib isha, whole minutes from -20 to +20, the others stay)
   مثال للوكيل: optimize/saver.sh prayeradj on
   مثال للوكيل: optimize/saver.sh prayeradj fajr +2 maghrib -1
—— افتح تلاوة وأذان [كل التلفزيون]
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
— المباريات والأسواق
—— المباريات
——— مباريات اليوم [كل التلفزيون]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— الفرق المتابعة [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh find-team <club>  (Arabic or English)
    مثال للوكيل: optimize/saver.sh teams <id:Name> …
    مثال للوكيل: optimize/saver.sh teams none  (`teams ""` does the same)
    مثال للوكيل: optimize/saver.sh teams 136013:Al-Hilal
——— تطبيق المباريات [الرئيسية]
    مثال للوكيل: optimize/saver.sh matchapp auto  (the default: UrTV Player when it is installed and signed with its own key)
      القيم المقبولة: auto|none|<package>
——— الدوريات كاملة [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh leagues 'Saudi-Arabian Pro League'  (the Arabic name, «دوري روشن السعودي», works too; `""` = none)
      القيم المقبولة: 'دوري روشن السعودي' ('' = none; optimize/saver.sh show lists the ones set)
    مثال للوكيل: optimize/saver.sh leagues 'id4719'
      القيم المقبولة: 'دوري روشن السعودي' ('' = none; optimize/saver.sh show lists the ones set)
    مثال للوكيل: optimize/saver.sh leagues 'Saudi-Arabian Pro League'
      القيم المقبولة: 'دوري روشن السعودي' ('' = none; optimize/saver.sh show lists the ones set)
——— بطاقة المباريات لليوم فقط [الرئيسية]
    مثال للوكيل: optimize/saver.sh matchestoday on  (`off` = the default: the week's next games, as always)
      القيم المقبولة: on|off
——— رياضات أخرى [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh events f1,ufc,boxing  (`""` = none)
      القيم المقبولة: f1,ufc,boxing ('' = none)
——— إخفاء المباريات المنتهية [الرئيسية]
    مثال للوكيل: optimize/saver.sh fbhide on  (`off`, the default: an ended game stays with its score)
      القيم المقبولة: on|off
——— تنبيه بداية المباراة [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh kickoff off  (`on`, the default; `silent` = on with no sound, `whistle` = on with the short whistle)
      القيم المقبولة: on|off|silent|whistle
——— صوت التنبيه [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh kickoff silent  (no sound)
      القيم المقبولة: on|off|silent|whistle
    مثال للوكيل: optimize/saver.sh kickoff whistle  (the short whistle, the default)
      القيم المقبولة: on|off|silent|whistle
——— عند الهدف [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh kickoffgoal on  (`off`, the default)
      القيم المقبولة: on|off
——— صافرة النهاية [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh kickoffend on  (`off`, the default)
      القيم المقبولة: on|off
——— فوق تطبيق آخر [كل التلفزيون]
    مثال للوكيل: optimize/saver.sh kickoffover on  (`off`, the default)
      القيم المقبولة: on|off
    مثال للوكيل: optimize/install.sh addon
    مثال للوكيل: optimize/dock.sh overlay on
——— جرّب التنبيه [الرئيسية وشاشة التوقف]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
—— صفحة الأسواق
——— افتح صفحة الأسواق [الرئيسية]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— شريط الأسواق [شاشة التوقف]
    مثال للوكيل: optimize/saver.sh markets off
      القيم المقبولة: tasi,gold,btc | off | on
——— الأسواق [شاشة التوقف]
    مثال للوكيل: optimize/saver.sh markets <up to 4 of tasi, aramco, spx, btc, gold, gold21, silver, oil, xrp>  (`oil` = Brent, in dollars per barrel; `gold` = the 24-carat gram in riyals, `gold21` = the 21-carat gram, both can be shown)
    مثال للوكيل: optimize/saver.sh home-card … stocks
    مثال للوكيل: optimize/saver.sh stocks
    مثال للوكيل: optimize/saver.sh markets
      القيم المقبولة: tasi,gold,btc | off | on
——— عرض الأسواق [شاشة التوقف]
    الخيارات: الاتجاه فقط ▲▼ / السعر والنسبة
    الأصل: الاتجاه فقط ▲▼
    مثال للوكيل: optimize/saver.sh mkstyle prices  (`arrows` is the default)
      القيم المقبولة: prices
—— الأسهم
——— قائمة الأسهم [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh stocks 2222 1120 AAPL
——— تغيّر الأسهم [الرئيسية وشاشة التوقف]
    الخيارات: بالنسبة ٪ / بالمبلغ
    الأصل: بالنسبة ٪
    مثال للوكيل: optimize/saver.sh stkchg amount
      القيم المقبولة: pct|amount
—— جديد المسلسلات
——— التصنيفات [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh genres Drama,Comedy
——— اللغات [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh langs Arabic,English
      القيم المقبولة: Arabic,English
——— مسلسلات مختارة [الرئيسية وشاشة التوقف]
    من التلفزيون فقط (بلا أمر، عن قصد)
——— أقل تقييم [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh minrating 7
— شاشة التوقف
—— معاينة الشاشة [شاشة التوقف]
   مثال للوكيل: optimize/shot.sh screensaver
—— بدء شاشة التوقف بعد [شاشة التوقف]
   مثال للوكيل: optimize/saver.sh idle <1, 2, 5, 10, 15, 30>  (minutes; the TV's own timer)
   صفحة الجوال: saver_idle
—— الوضع الخفيف [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh lite on
     القيم المقبولة: auto|on|off
—— بجانب الساعة
——— المحتوى [شاشة التوقف]
    مثال للوكيل: optimize/saver.sh card <shows, prayers, forecast, hourly, prayerrow, matches, nextmatch, matchday, livenow, minitable, nextmatches, yesterday, weekend, cuppath, formula, national, verse, hadith, wisdom, adhkar, stocks, tasks, year, smart, today, album, none>  (the app's own list, Prefs.SAVER_PANELS; `nextprayer` is a home-screen card only: `home-card`)
——— بطاقة ثانية [شاشة التوقف]
    مثال للوكيل: optimize/saver.sh card2 <the same list as card, but never the card that is already first>  (`card2 none` = one card, the default; the second card can't repeat the first, and needs a first card: `card verse` first)
——— تبديل الآية والحديث [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh verse 3
      القيم المقبولة: day|3|10
——— الآية والحديث [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh versekind verse  (`hadith` = hadith only; `both` = the default: a verse one day, a hadith the next, as always; anything else is refused and nothing is sent)
      القيم المقبولة: both|verse|hadith
——— الخانة اليسرى [شاشة التوقف]
    الخيارات: المباريات / الطقس / الصيام / عدّ تنازلي / الأسواق / بدون
    الأصل: المباريات
    مثال للوكيل: optimize/saver.sh slot <matches, weather, fasting, countdown, markets, none>
——— العدّ التنازلي [شاشة التوقف]
    مثال للوكيل: optimize/saver.sh countdown <YYYY-MM-DD> '<name>'
    مثال للوكيل: optimize/saver.sh slot countdown  (`countdown clear` removes it)
      القيم المقبولة: fasting
——— شريط السنة تحت الساعة [شاشة التوقف]
    مثال للوكيل: optimize/saver.sh yearstrip on  (`off` = the default)
——— المناسبة القادمة [شاشة التوقف]
    مثال للوكيل: optimize/saver.sh occasion off
      القيم المقبولة: on|off
——— المناسبات الوطنية [شاشة التوقف]
    الخيارات: تلقائي / إيقاف
    الأصل: تلقائي
    مثال للوكيل: optimize/saver.sh national off  (`auto` is the default)
      القيم المقبولة: auto|off
— العائلة والأمان
—— الرقابة الأبوية [كل التلفزيون]
   مثال للوكيل: optimize/dock.sh pin set 1234  (4 digits; sets or changes it, never printed back)
—— تصفية المواقع [كل التلفزيون]
   مثال للوكيل: optimize/dock.sh familydns on
—— الجوالات المسموح لها [كل التلفزيون]
   مثال للوكيل: optimize/dock.sh sleep-setup
   مثال للوكيل: optimize/dock.sh type-setup
— حول
—— تحديث تلفزيوني [كل التلفزيون]
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
—— التحديث التلقائي للصفحة [الرئيسية وشاشة التوقف]
   من التلفزيون فقط (بلا أمر، عن قصد)
—— تحديث صفحة شاشة التوقف [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh update  (it also updates by itself; keep «التحديث التلقائي للصفحة» on)
   مثال للوكيل: optimize/saver.sh update
—— النسخ التجريبية [كل التلفزيون]
   مثال للوكيل: optimize/saver.sh updatechannel beta
     القيم المقبولة: beta|latest|next   (next is testing only)
—— أرسل ملاحظة عن النسخة التجريبية [كل التلفزيون]
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
—— إعداد تلفزيوني [كل التلفزيون]
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
—— مسح الملفات المؤقتة [كل التلفزيون]
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
—— أرسل تقرير [كل التلفزيون]
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
—— مصادر البيانات [الرئيسية وشاشة التوقف]
   من التلفزيون فقط (بلا أمر، عن قصد)
```

## «شكل كنبة» متوقف (الأصل): شاشة ٤.٣.٢ (أزرار كبيرة، و«البطاقات» و«المزيد» شاشتا أزرار)

نفس الصفوف ونفس القيم؛ يتغيّر مكانها فقط. كل صف تحت زره في شاشة ٤.٣.٢.

```text
الإعدادات
— المدينة
—— المدينة [كل التلفزيون]
   مثال للوكيل: optimize/saver.sh city <name>  (Arabic or English; a short form is fine if only one city matches; use `city 'GB|London'` to qualify the country)
   مثال للوكيل: optimize/saver.sh city '<name>' <lat> <lon> <time zone> [country code]  (`… 25.86 43.49 Asia/Riyadh` in Saudi Arabia, `… 29.37 47.98 Asia/Kuwait` elsewhere)
   صفحة الجوال: city
   مركز التحكم: city
— الأذان
—— الأذان [كل التلفزيون]
   مثال للوكيل: optimize/music.sh adhan
   صفحة الجوال: adhan_on, adhan_short, adhan_stopkey, adhan_voice, adhan_fajr_voice, adhan_volume
—— الأذان لكل صلاة [كل التلفزيون]
   مثال للوكيل: optimize/music.sh adhan state <fajr|dhuhr|asr|maghrib|isha> <both|sound|screen|none>
   مثال للوكيل: optimize/music.sh adhan screen <prayer> on|off
   مثال للوكيل: optimize/music.sh adhan
   مثال للوكيل: optimize/music.sh adhan prayer <fajr|dhuhr|asr|maghrib|isha> <general|silent|own|own_fajr|1-21>
   صفحة الجوال: adhan_prayers
— الموقع والصلاة
—— الموقع
——— الدولة [كل التلفزيون]
    مثال للوكيل: optimize/saver.sh city '<name>' <lat> <lon> <time zone> SA
——— المدينة [كل التلفزيون]
    مثال للوكيل: optimize/saver.sh city <name>  (Arabic or English; a short form is fine if only one city matches; use `city 'GB|London'` to qualify the country)
    مثال للوكيل: optimize/saver.sh city '<name>' <lat> <lon> <time zone> [country code]  (`… 25.86 43.49 Asia/Riyadh` in Saudi Arabia, `… 29.37 47.98 Asia/Kuwait` elsewhere)
    صفحة الجوال: city
    مركز التحكم: city
——— طريقة الحساب [كل التلفزيون]
    مثال للوكيل: optimize/saver.sh prayermethod jafari
——— تعديل وقت الصلاة [كل التلفزيون]
    مثال للوكيل: optimize/saver.sh prayeradj fajr +2 maghrib -1 …  (fajr dhuhr asr maghrib isha, whole minutes from -20 to +20, the others stay)
    مثال للوكيل: optimize/saver.sh prayeradj on
    مثال للوكيل: optimize/saver.sh prayeradj fajr +2 maghrib -1
— الساعة والأرقام
—— الأرقام [الرئيسية وشاشة التوقف]
   الخيارات: هندية ١٢٣ / عربية 123
   الأصل: هندية ١٢٣
   مثال للوكيل: optimize/saver.sh digits latin  (`hindi` = ١٢٣, the default)
     القيم المقبولة: hindi|latin
—— التاريخ بالأرقام [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh datenum on  (`off`, the default: in words)
     القيم المقبولة: on|off
—— أسماء الأشهر الميلادية [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh months levant  (`default` = يناير، فبراير… as always)
     القيم المقبولة: default|levant
—— نظام الساعة [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh clock24 on
     القيم المقبولة: on|off
—— إظهار الثواني [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh seconds on  (`off` = the default, the clock as always)
     القيم المقبولة: on|off
   مثال للوكيل: optimize/saver.sh seconds on
     القيم المقبولة: on|off
—— فرق التاريخ الهجري [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh hijrioffset -1  (whole days, `-2` to `+2`; `0` = Umm al-Qura as it is, the default; nothing else is accepted)
     القيم المقبولة: -2|-1|0|+1|+2   (days; 0 = Umm al-Qura as it is)
— تلاوة وأذان
—— افتح تلاوة وأذان [كل التلفزيون]
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
—— زر تلاوة وأذان [الرئيسية]
   مثال للوكيل: optimize/saver.sh listenpill off  (`on` brings it back; default on, so nothing changes for anyone; `saver.sh show` says `listenpill:`)
     القيم المقبولة: on|off
— البطاقات
—— شاشة التوقف
——— معاينة الشاشة [شاشة التوقف]
    مثال للوكيل: optimize/shot.sh screensaver
——— الوضع الخفيف [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh lite on
      القيم المقبولة: auto|on|off
——— العرض
———— تنبيه قبل الأذان [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh adhanbar off  (or `on`)
       القيم المقبولة: on|off
     مثال للوكيل: optimize/saver.sh adhanlead 15  (or `10`, the default)
       القيم المقبولة: 10|15
———— الشروق بعد الفجر [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh sunrise on  (`off` = the default, the panel as always)
       القيم المقبولة: on|off
———— شريط الوقت بين الصلاتين [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh prayerbar on  (`off` = the default)
———— الرطوبة والرياح [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh wxmore on  (`off` = the default)
       القيم المقبولة: on|off
———— شريط السنة تحت الساعة [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh yearstrip on  (`off` = the default)
———— سرعة ألبوم صوري [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh albummode ten
       القيم المقبولة: seconds|ten|minute
———— تاريخ الصورة في ألبوم صوري [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh albumdate off
       القيم المقبولة: on|off
———— عنوان ألبوم صوري [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh albumtitle off
       القيم المقبولة: on|off
———— شكل صورة ألبوم صوري [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh albumfit fill
       القيم المقبولة: fit|fill
———— حجم ألبوم صوري [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh albumsize tall
       القيم المقبولة: normal|tall
———— توهّج لون التطبيق [الرئيسية]
     مثال للوكيل: optimize/saver.sh glow light  (`off` = the default, `light` = a third of the strength, `on` = full)
       القيم المقبولة: off|light|on
———— نص أوضح [الرئيسية]
     مثال للوكيل: optimize/saver.sh cleartext on  (`off` = the default)
       القيم المقبولة: on|off
———— المناسبة القادمة [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh occasion off
       القيم المقبولة: on|off
———— تعتيم الليل [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh nightdim off  (`on` = the default, as always: darker from 21:00, the most from 23:00 to 05:00, normal again at 07:00, by the city's clock)
       القيم المقبولة: on|off
———— المناسبات الوطنية [شاشة التوقف]
     الخيارات: تلقائي / إيقاف
     الأصل: تلقائي
     مثال للوكيل: optimize/saver.sh national off  (`auto` is the default)
       القيم المقبولة: auto|off
———— تصميم شاشة التوقف [شاشة التوقف]
     الخيارات: معلومات / نظيفة / ساعة كبيرة / لوحة / خط اليوم
     الأصل: معلومات
     مثال للوكيل: optimize/saver.sh design <info, clean, big, frame, dayline>  (`info` = «معلومات», the screensaver as it always was, the default)
———— رجوع مرتين يفتح شاشة التوقف [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh backtwice on
       القيم المقبولة: on|off
———— الشريط في الوسط [شاشة التوقف]
     الخيارات: بدون / الصلوات / الخانة اليسرى / الطقس
     الأصل: بدون
     مثال للوكيل: optimize/saver.sh bar <none, prayers, slot, weather>  (`none` is the default)
———— مكان الشريط [شاشة التوقف]
     الخيارات: أسفل الشاشة / تحت الساعة
     الأصل: أسفل الشاشة
     مثال للوكيل: optimize/saver.sh barat <bottom, clock>  (`bottom` of the screen is the default; `clock` = under the clock)
———— مكان الطقس [شاشة التوقف]
     الخيارات: كما في التصميم / مكانه المعتاد / تحت الساعة / في الزاوية / مخفي
     الأصل: كما في التصميم
     مثال للوكيل: optimize/saver.sh wx <auto, usual, clock, corner, off>  (`auto` = as the design has it, the default: its place at the top in `info` and `dayline`, hidden in the others)
———— حجم الساعة [شاشة التوقف]
     الخيارات: كبيرة / متوسطة / صغيرة
     الأصل: كبيرة
     مثال للوكيل: optimize/saver.sh clocksize <large, medium, small>  (`large` is the default and today's look, unchanged)
———— الخانة اليسرى [شاشة التوقف]
     الخيارات: المباريات / الطقس / الصيام / عدّ تنازلي / الأسواق / بدون
     الأصل: المباريات
     مثال للوكيل: optimize/saver.sh slot <matches, weather, fasting, countdown, markets, none>
———— العدّ التنازلي [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh countdown <YYYY-MM-DD> '<name>'
     مثال للوكيل: optimize/saver.sh slot countdown  (`countdown clear` removes it)
       القيم المقبولة: fasting
——— بجانب الساعة
———— المحتوى [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh card <shows, prayers, forecast, hourly, prayerrow, matches, nextmatch, matchday, livenow, minitable, nextmatches, yesterday, weekend, cuppath, formula, national, verse, hadith, wisdom, adhkar, stocks, tasks, year, smart, today, album, none>  (the app's own list, Prefs.SAVER_PANELS; `nextprayer` is a home-screen card only: `home-card`)
———— بطاقة ثانية [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh card2 <the same list as card, but never the card that is already first>  (`card2 none` = one card, the default; the second card can't repeat the first, and needs a first card: `card verse` first)
———— شكل البطاقة [الرئيسية وشاشة التوقف]
     الخيارات: الحالية / داكنة / زجاجية / داكنة بإطار / زجاجية بإطار / مربعات / الشكل السابق
     الأصل: الحالية
     مثال للوكيل: optimize/saver.sh cardstyle <current, dark1, glass1, dark3, glass3, boxes, classic>
———— تبديل الآية والحديث [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh verse 3
       القيم المقبولة: day|3|10
———— الآية والحديث [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh versekind verse  (`hadith` = hadith only; `both` = the default: a verse one day, a hadith the next, as always; anything else is refused and nothing is sent)
       القيم المقبولة: both|verse|hadith
——— التوقيت
———— تبديل الصورة كل [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh photos <1, 5, 10, 15, 30, 60>  (minutes; 30 = the default)
     مثال للوكيل: optimize/saver.sh photos 30s  (every 30 seconds)
       القيم المقبولة: 30s|1|5|10|15|30|60 (minutes; 30s = every 30 seconds)
———— صور شاشة التوقف [الرئيسية وشاشة التوقف]
     من التلفزيون فقط (بلا أمر، عن قصد)
———— صوري [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/photos.sh add <files they named>
     مثال للوكيل: optimize/photos.sh mode mix  (with ours, the default)
     مثال للوكيل: optimize/photos.sh mode only
     مثال للوكيل: optimize/photos.sh mode album
     مثال للوكيل: optimize/photos.sh list
     مثال للوكيل: optimize/photos.sh del N
     مثال للوكيل: optimize/photos.sh clear  (back to ours only)
     مثال للوكيل: optimize/photos.sh show  (a screenshot with them; it waits 70 s for the page to settle, so allow 80 s)
     مثال للوكيل: optimize/saver.sh homemine mix  (with «صورنا»)
       القيم المقبولة: same|mix|only|off
———— أضف من فلاشة أو التنزيلات [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/photos.sh add <files they named>
———— بدء شاشة التوقف بعد [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh idle <1, 2, 5, 10, 15, 30>  (minutes; the TV's own timer)
     صفحة الجوال: saver_idle
—— الشاشة الرئيسية
——— الشاشة الرئيسية (تلفزيونك)
———— اجعل UrTV الشاشة الرئيسية [كل التلفزيون]
     إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
———— إعداد تلفزيوني [كل التلفزيون]
     إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
———— رجّع واجهة المصنع [كل التلفزيون]
     إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
———— تخلّص من واجهة المصنع [كل التلفزيون]
     إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
———— قبل ما تحذف التطبيق… [كل التلفزيون]
     إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
———— ⁦UrTV⁩ النظيف [الرئيسية وشاشة التوقف]
     من التلفزيون فقط (بلا أمر، عن قصد)
———— صفحات الأسهم والمباريات داكنة [الرئيسية]
     مثال للوكيل: optimize/saver.sh pagesdark off
       القيم المقبولة: on|off
———— البطاقة الأولى [الرئيسية]
     مثال للوكيل: optimize/saver.sh home-card prayers,hourly,verse  (a comma list, up to 3, in this order; one value works; `none` or `""` = no card)
     مثال للوكيل: optimize/saver.sh home-card prayers,hourly,verse
     صفحة الجوال: home_cards
———— البطاقة الثانية [الرئيسية]
     مثال للوكيل: optimize/saver.sh home-card prayers,hourly,verse
———— البطاقة الثالثة [الرئيسية]
     مثال للوكيل: optimize/saver.sh home-card prayers,hourly,verse
———— التحية والطقس [الرئيسية]
     مثال للوكيل: optimize/saver.sh greeting off
       القيم المقبولة: on|off
———— اسمك في التحية [الرئيسية]
     مثال للوكيل: optimize/saver.sh greetingname علاء  (a name of at most 30 letters on one line; the name is isolated, so Latin letters sit right in an Arabic line)
       القيم المقبولة: NAME|off   (the name after the greeting on the home, at most 30 letters)
     مثال للوكيل: optimize/saver.sh greetingname off
       القيم المقبولة: NAME|off   (the name after the greeting on the home, at most 30 letters)
———— زر المصادر [الرئيسية]
     مثال للوكيل: optimize/saver.sh pillshidden inputs
       القيم المقبولة: markets,matches,center,phone,inputs | none
———— زر الأسواق [الرئيسية]
     مثال للوكيل: optimize/saver.sh pillshidden markets
       القيم المقبولة: markets,matches,center,phone,inputs | none
———— زر المباريات [الرئيسية]
     مثال للوكيل: optimize/saver.sh pillshidden matches
       القيم المقبولة: markets,matches,center,phone,inputs | none
———— زر الجوال [الرئيسية]
     مثال للوكيل: optimize/saver.sh pillshidden phone
       القيم المقبولة: markets,matches,center,phone,inputs | none
———— زر البحث [الرئيسية]
     مثال للوكيل: optimize/saver.sh searchpill on  (`off`, the default)
       القيم المقبولة: on|off
———— زر مركز التحكم [الرئيسية]
     مثال للوكيل: optimize/saver.sh pillshidden center
       القيم المقبولة: markets,matches,center,phone,inputs | none
———— زر المهام [الرئيسية]
     مثال للوكيل: optimize/saver.sh taskspill off  (`on` brings it back; default on)
       القيم المقبولة: on|off
———— الرجوع يفتح شاشة التوقف [الرئيسية]
     مثال للوكيل: optimize/dock.sh backsaver on  (`off` = the default: Back does nothing at the top of the home, as always; no argument says `back-saver: on|off`)
———— زر الرئيسية يفتح تلفزيونك مباشرة [الرئيسية]
     مثال للوكيل: optimize/dock.sh home-key on\|off\|status
———— زر يوتيوب في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key youtube org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key youtube default
———— زر نتفليكس في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key netflix org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key netflix default
———— زر Prime Video في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key prime org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key prime default
———— زر ديزني بلس في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key disney org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key disney default
———— زر Paramount Plus في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key paramount org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key paramount default
———— زر Free TV في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key freetv org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key freetv default
———— الزر المخصص ★ في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key star org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key star default
———— زر BRAVIA CORE في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key core org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key core default
———— زر Crunchyroll في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key crunchyroll org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key crunchyroll default
———— زر Google Play في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key play org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key play default
———— زر SonyLIV في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key sonyliv org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key sonyliv default
———— زر Tabii في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key tabii org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key tabii default
———— زر TCL Channel في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key tclchannel org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key tclchannel default
———— زر Okko في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key okko org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key okko default
———— زر VK في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key vk org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key vk default
———— زر Rutube في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key rutube org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key rutube default
———— زر KION في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key kion org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key kion default
———— زر Wink في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key wink org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key wink default
———— زر IVI في الريموت [الرئيسية]
     مثال للوكيل: optimize/dock.sh key ivi org.smarttube.stable
     مثال للوكيل: optimize/dock.sh key ivi default
———— الخروج من تلفزيونك [كل التلفزيون]
     مثال للوكيل: optimize/saver.sh backexit hold  (a long Back, about a second, leaves)
       القيم المقبولة: press|hold|three
     مثال للوكيل: optimize/saver.sh backexit hold
       القيم المقبولة: press|hold|three
———— جرس التحديثات [الرئيسية]
     مثال للوكيل: optimize/saver.sh updatebell off  (`on` brings it back; default on, so nothing changes for anyone; `saver.sh show` says `updatebell:`)
       القيم المقبولة: on|off
———— أكمل المشاهدة [الرئيسية]
     مثال للوكيل: optimize/saver.sh watchnext off  (`on`, the default)
       القيم المقبولة: on|off
— المظهر والخلفيات
—— المظهر على الرئيسية [الرئيسية]
   مثال للوكيل: optimize/saver.sh lookpanel
—— رجّع الشكل السابق [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh lookback
—— أشكال جاهزة [الرئيسية وشاشة التوقف]
   الخيارات: شكلك المعتاد / كنبة / tvOS / صورة فقط / مواقيت يومي / مباريات اليوم / ليل هادئ
   مثال للوكيل: optimize/saver.sh preset family  (tv, couch, clean, family, sport, calm, or a mix: couch-prayers, clean-icons, sport-night, tv-glass)
     القيم المقبولة: tv|couch|tvos|clean|family|sport|calm|couch-prayers|clean-icons|sport-night|tv-glass
—— شكل شاشة الإعدادات [كل التلفزيون]
   مثال للوكيل: optimize/saver.sh settingsstyle couch  (or `buttons`, `tvos`)
     القيم المقبولة: buttons|couch|tvos
—— تخصيص [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh preset tv
     القيم المقبولة: tv|couch|tvos|clean|family|sport|calm|couch-prayers|clean-icons|sport-night|tv-glass
—— رجّع بطاقاتك [الرئيسية]
   مثال للوكيل: optimize/saver.sh preset tv  (the person's own look and cards back)
     القيم المقبولة: tv|couch|tvos|clean|family|sport|calm|couch-prayers|clean-icons|sport-night|tv-glass
—— شكل كنبة [الرئيسية]
   الخيارات: مفعّل / متوقف
   الأصل: متوقف
   تشغيله يضبط مرة وحدة: التخطيط كنبة، الخلفية الآفاق، الساعة زجاجية، شكل التطبيقات أيقونات، ويحفظ اختياراتك؛ وإيقافه يرجّعها كما كانت. وتغيير أي واحد منها وهو مفعّل يبقيه مفعّلًا ويقول «مخصّص»
   مثال للوكيل: optimize/saver.sh couch on
     القيم المقبولة: on|off
—— التخطيط [الرئيسية]
   الخيارات: تلفزيونك / كنبة / كرة القدم / tvOS
   الأصل: تلفزيونك
   مثال للوكيل: optimize/saver.sh homelayout classic  (تلفزيونك, the default)
     القيم المقبولة: classic|couch|football
—— الخلفية [الرئيسية]
   الخيارات: صوري وحزم الصور / الآفاق
   الأصل: صوري وحزم الصور
   مثال للوكيل: optimize/saver.sh background photos  (the default)
     القيم المقبولة: photos|scenes
—— الساعة [الرئيسية]
   الخيارات: عادية / زجاجية خلف الصورة
   الأصل: عادية
   مثال للوكيل: optimize/saver.sh clock plain
     القيم المقبولة: plain|glass
—— شكل التطبيقات [الرئيسية]
   الخيارات: بنرات / أيقونات / مربعات ملونة / tvOS
   الأصل: مربعات ملونة
   مثال للوكيل: optimize/saver.sh appsstyle tiles  («مربعات ملونة», the default: today's tray of tiles, its focus as it was: the ring, or `zoom` where that was chosen)
     القيم المقبولة: tiles|banners|icons|glass
—— محاذاة التطبيقات [الرئيسية]
   مثال للوكيل: optimize/saver.sh appsalign center  (or `right`, `left`)
     القيم المقبولة: center|right|left
—— حجم شريط tvOS [الرئيسية]
   الخيارات: 50 / 60 / 70 / 80 / 90 / 100 / 110 / 120 / 130 / 140 / 150
   الأصل: 100
   مثال للوكيل: optimize/saver.sh glasssize 70  (50 to 150 in steps of 10; `100` the default)
     القيم المقبولة: 50..150 (steps of 10; 100 the default)
—— صفوف شريط tvOS [الرئيسية]
   الخيارات: صف واحد / صفّان / ثلاثة صفوف
   الأصل: صف واحد
   مثال للوكيل: optimize/saver.sh glassrows 2  (`1` the default, to `3`; «tvOS» only)
     القيم المقبولة: 1|2|3
—— حزم الآفاق [الرئيسية]
   الخيارات: آفاق السعودية / آفاق عالمية
   الأصل: آفاق السعودية
   مثال للوكيل: optimize/saver.sh scenepacks saudi  (the default)
     القيم المقبولة: saudi|couch|saudi,couch
—— بقاء كل أفق [الرئيسية]
   الخيارات: ١٠ ثوانٍ / ٢٠ ثانية / دقيقة واحدة / خمس دقائق
   الأصل: ٢٠ ثانية
   مثال للوكيل: optimize/saver.sh scenestay 10
     القيم المقبولة: 10|20|60|300
—— الأفق يتبع الوقت [الرئيسية]
   الخيارات: مفعّل / متوقف
   الأصل: متوقف
   مثال للوكيل: optimize/saver.sh scenetime on  (`off`, the default)
     القيم المقبولة: on|off
—— الأفق يتبع الطقس [الرئيسية]
   الخيارات: مفعّل / متوقف
   الأصل: متوقف
   مثال للوكيل: optimize/saver.sh sceneweather on  (`off`, the default)
     القيم المقبولة: on|off
—— وضع الألوان [الرئيسية]
   مثال للوكيل: optimize/saver.sh look navy
     القيم المقبولة: glass|graphite|navy|clean|sand
   مثال للوكيل: optimize/saver.sh appearance auto
     القيم المقبولة: dark|light|auto
—— لوحة المظهر على الرئيسية [الرئيسية]
   مثال للوكيل: optimize/saver.sh lookpanelrow on  (or `off`)
     القيم المقبولة: on|off
—— زر رجّع الشكل السابق [الرئيسية وشاشة التوقف]
   مثال للوكيل: optimize/saver.sh lookundo on  (or `off`)
     القيم المقبولة: on|off
—— تبديل الشكل بلا وميض [الرئيسية]
   مثال للوكيل: optimize/saver.sh smoothlook on  (or `off`)
     القيم المقبولة: on|off
— مركز التحكم
—— مركز التحكم [الرئيسية]
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
— التحكم من الجوال
—— الجوالات المسموح لها [كل التلفزيون]
   مثال للوكيل: optimize/dock.sh sleep-setup
   مثال للوكيل: optimize/dock.sh type-setup
— تحديث تلفزيوني
—— تحديث تلفزيوني [كل التلفزيون]
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
—— التطبيق
——— التحديث التلقائي للصفحة [الرئيسية وشاشة التوقف]
    من التلفزيون فقط (بلا أمر، عن قصد)
——— تحديث صفحة شاشة التوقف [الرئيسية وشاشة التوقف]
    مثال للوكيل: optimize/saver.sh update  (it also updates by itself; keep «التحديث التلقائي للصفحة» on)
    مثال للوكيل: optimize/saver.sh update
——— النسخ التجريبية [كل التلفزيون]
    مثال للوكيل: optimize/saver.sh updatechannel beta
      القيم المقبولة: beta|latest|next   (next is testing only)
——— أرسل ملاحظة عن النسخة التجريبية [كل التلفزيون]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— مسح الملفات المؤقتة [كل التلفزيون]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— مصادر البيانات [الرئيسية وشاشة التوقف]
    من التلفزيون فقط (بلا أمر، عن قصد)
— أرسل تقرير
—— أرسل تقرير [كل التلفزيون]
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
— المزيد
—— المباريات والأسواق
——— المباريات
———— مباريات اليوم [كل التلفزيون]
     إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
———— الفرق المتابعة [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh find-team <club>  (Arabic or English)
     مثال للوكيل: optimize/saver.sh teams <id:Name> …
     مثال للوكيل: optimize/saver.sh teams none  (`teams ""` does the same)
     مثال للوكيل: optimize/saver.sh teams 136013:Al-Hilal
———— تطبيق المباريات [الرئيسية]
     مثال للوكيل: optimize/saver.sh matchapp auto  (the default: UrTV Player when it is installed and signed with its own key)
       القيم المقبولة: auto|none|<package>
———— الدوريات كاملة [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh leagues 'Saudi-Arabian Pro League'  (the Arabic name, «دوري روشن السعودي», works too; `""` = none)
       القيم المقبولة: 'دوري روشن السعودي' ('' = none; optimize/saver.sh show lists the ones set)
     مثال للوكيل: optimize/saver.sh leagues 'id4719'
       القيم المقبولة: 'دوري روشن السعودي' ('' = none; optimize/saver.sh show lists the ones set)
     مثال للوكيل: optimize/saver.sh leagues 'Saudi-Arabian Pro League'
       القيم المقبولة: 'دوري روشن السعودي' ('' = none; optimize/saver.sh show lists the ones set)
———— بطاقة المباريات لليوم فقط [الرئيسية]
     مثال للوكيل: optimize/saver.sh matchestoday on  (`off` = the default: the week's next games, as always)
       القيم المقبولة: on|off
———— رياضات أخرى [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh events f1,ufc,boxing  (`""` = none)
       القيم المقبولة: f1,ufc,boxing ('' = none)
———— إخفاء المباريات المنتهية [الرئيسية]
     مثال للوكيل: optimize/saver.sh fbhide on  (`off`, the default: an ended game stays with its score)
       القيم المقبولة: on|off
———— تنبيه بداية المباراة [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh kickoff off  (`on`, the default; `silent` = on with no sound, `whistle` = on with the short whistle)
       القيم المقبولة: on|off|silent|whistle
———— صوت التنبيه [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh kickoff silent  (no sound)
       القيم المقبولة: on|off|silent|whistle
     مثال للوكيل: optimize/saver.sh kickoff whistle  (the short whistle, the default)
       القيم المقبولة: on|off|silent|whistle
———— عند الهدف [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh kickoffgoal on  (`off`, the default)
       القيم المقبولة: on|off
———— صافرة النهاية [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh kickoffend on  (`off`, the default)
       القيم المقبولة: on|off
———— فوق تطبيق آخر [كل التلفزيون]
     مثال للوكيل: optimize/saver.sh kickoffover on  (`off`, the default)
       القيم المقبولة: on|off
     مثال للوكيل: optimize/install.sh addon
     مثال للوكيل: optimize/dock.sh overlay on
———— جرّب التنبيه [الرئيسية وشاشة التوقف]
     إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— صفحة الأسواق
———— افتح صفحة الأسواق [الرئيسية]
     إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— الأسواق (أعلى اليسار)
———— شريط الأسواق [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh markets off
       القيم المقبولة: tasi,gold,btc | off | on
———— الأسواق [شاشة التوقف]
     مثال للوكيل: optimize/saver.sh markets <up to 4 of tasi, aramco, spx, btc, gold, gold21, silver, oil, xrp>  (`oil` = Brent, in dollars per barrel; `gold` = the 24-carat gram in riyals, `gold21` = the 21-carat gram, both can be shown)
     مثال للوكيل: optimize/saver.sh home-card … stocks
     مثال للوكيل: optimize/saver.sh stocks
     مثال للوكيل: optimize/saver.sh markets
       القيم المقبولة: tasi,gold,btc | off | on
———— عرض الأسواق [شاشة التوقف]
     الخيارات: الاتجاه فقط ▲▼ / السعر والنسبة
     الأصل: الاتجاه فقط ▲▼
     مثال للوكيل: optimize/saver.sh mkstyle prices  (`arrows` is the default)
       القيم المقبولة: prices
——— الأسهم
———— قائمة الأسهم [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh stocks 2222 1120 AAPL
———— تغيّر الأسهم [الرئيسية وشاشة التوقف]
     الخيارات: بالنسبة ٪ / بالمبلغ
     الأصل: بالنسبة ٪
     مثال للوكيل: optimize/saver.sh stkchg amount
       القيم المقبولة: pct|amount
—— التطبيقات والمفضلة
——— تطبيقات في الشريط [الرئيسية]
    الخيارات: 5 / 6 / 7 / 8 / 9 / 10 / all
    الأصل: 5
    مثال للوكيل: optimize/saver.sh favbar 7
      القيم المقبولة: 5|6|7|8|9|10|all
    صفحة الجوال: fav_bar
——— ترتيب التطبيقات المفضلة [الرئيسية]
    مثال للوكيل: optimize/dock.sh apps
    مثال للوكيل: optimize/dock.sh set <pkg> …  (1–10, in their order: exactly those, never topped up to five; editing the row on the TV keeps its floor of five)
    مثال للوكيل: optimize/dock.sh show
    مثال للوكيل: optimize/dock.sh reset
——— تطبيقات مقترحة [الرئيسية]
    من التلفزيون فقط (بلا أمر، عن قصد)
——— أسماء المصادر [الرئيسية]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— تعديل المفضلة [الرئيسية]
    مثال للوكيل: optimize/dock.sh set
——— ترتيب كل التطبيقات [الرئيسية]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
——— قفل التطبيقات [كل التلفزيون]
    مثال للوكيل: optimize/dock.sh pin lock com.netflix.ninja
——— تفاصيل التطبيقات [كل التلفزيون]
    إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
—— جديد المسلسلات
——— جديد المسلسلات
———— التصنيفات [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh genres Drama,Comedy
———— اللغات [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh langs Arabic,English
       القيم المقبولة: Arabic,English
———— مسلسلات مختارة [الرئيسية وشاشة التوقف]
     من التلفزيون فقط (بلا أمر، عن قصد)
———— أقل تقييم [الرئيسية وشاشة التوقف]
     مثال للوكيل: optimize/saver.sh minrating 7
—— الرقابة الأبوية
——— الرقابة الأبوية [كل التلفزيون]
    مثال للوكيل: optimize/dock.sh pin set 1234  (4 digits; sets or changes it, never printed back)
—— تصفية المواقع
——— تصفية المواقع [كل التلفزيون]
    مثال للوكيل: optimize/dock.sh familydns on
—— اللغة
——— اللغة [كل التلفزيون]
    مثال للوكيل: optimize/saver.sh lang en  (`ar` = Arabic, the default; anything else is refused and nothing is sent; `optimize/saver.sh show` prints `lang:`)
      القيم المقبولة: ar|en
    صفحة الجوال: lang
— بحث في الإعدادات
—— بحث في الإعدادات
   إجراء أو شاشة من التلفزيون (ما فيه قيمة يضبطها أمر)
```
