# Automatic round: 4.4b4-654

## Health metrics

| metric | now | limit | previous | change | verdict |
|---|---|---|---|---|---|
| cold_start_home_ms | 0ms | max 4000 |  |  | ok |
| cold_start_saver_ms | 936ms | max 5000 |  |  | ok |
| jank_home_frames | 434 |  |  |  | no limit |
| jank_home_pct | 22.58% | max 15 |  |  | RED |
| jank_home_p90_ms | 69ms |  |  |  | no limit |
| jank_home_p99_ms | 73 | max 120 |  |  | ok |
|  | ms |  |  |  | no limit |
| pss_home_kb | 49778KB | max 300000 |  |  | ok |
| pss_saver_kb | 47828KB | max 300000 |  |  | ok |
| crashes | 0 | zero 0 |  |  | ok |
| anrs | 0 | zero 0 |  |  | ok |
| frozen_lines | 0 | zero 0 |  |  | ok |

## Checks

| step | result | what |
|---|---|---|
| layout-classic-none | PASS | كلاسيكي، بلا بطاقة: the home is in front (look at shots/layout-classic-none.png) |
| layout-classic-stocks | PASS | كلاسيكي، الأسهم: the home is in front (look at shots/layout-classic-stocks.png) |
| layout-classic-matches | PASS | كلاسيكي، المباريات: the home is in front (look at shots/layout-classic-matches.png) |
| layout-couch-none | PASS | كنبة، بلا بطاقة: the home is in front (look at shots/layout-couch-none.png) |
| layout-couch-stocks | PASS | كنبة، الأسهم: the home is in front (look at shots/layout-couch-stocks.png) |
| layout-couch-matches | PASS | كنبة، المباريات: the home is in front (look at shots/layout-couch-matches.png) |
| layout-football-none | PASS | كرة القدم، بلا بطاقة: the home is in front (look at shots/layout-football-none.png) |
| layout-football-stocks | PASS | كرة القدم، الأسهم: the home is in front (look at shots/layout-football-stocks.png) |
| layout-football-matches | PASS | كرة القدم، المباريات: the home is in front (look at shots/layout-football-matches.png) |
| settings-buttons-pray | PASS | أزرار ← الصلاة والأذان: «الأذان لكل صلاة» is on the screen |
| settings-buttons-photos | PASS | أزرار ← صوري والخلفيات: «احذف صورة» is on the screen |
| settings-buttons-look | PASS | أزرار ← الشكل: «التخطيط» is on the screen |
| settings-buttons-saver | FAIL | أزرار ← شاشة التوقف: «بدء شاشة التوقف بعد» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-buttons-apps | FAIL | أزرار ← التطبيقات والأزرار: «الخروج من تلفزيونك» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-buttons-content | FAIL | أزرار ← المباريات والأسواق: «مباريات اليوم» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-buttons-family | FAIL | أزرار ← العائلة والجوال: «الرقابة الأبوية» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-buttons-lang | FAIL | أزرار ← اللغة والموقع: «المدينة» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-buttons-update | FAIL | أزرار ← حول والتحديث: «تحديث تلفزيوني» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-buttons-search | FAIL | أزرار ← بحث في الإعدادات: «بحث في الإعدادات» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-couch-pray | FAIL | كنبة ← الصلاة والأذان: «الأذان لكل صلاة» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-couch-photos | FAIL | كنبة ← صوري والخلفيات: «احذف صورة» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-couch-look | FAIL | كنبة ← الشكل: «التخطيط» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-couch-saver | FAIL | كنبة ← شاشة التوقف: «بدء شاشة التوقف بعد» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-couch-apps | FAIL | كنبة ← التطبيقات والأزرار: «الخروج من تلفزيونك» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-couch-content | FAIL | كنبة ← المباريات والأسواق: «مباريات اليوم» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-couch-family | FAIL | كنبة ← العائلة والجوال: «الرقابة الأبوية» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-couch-lang | FAIL | كنبة ← اللغة والموقع: «المدينة» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-couch-update | FAIL | كنبة ← حول والتحديث: «تحديث تلفزيوني» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-couch-search | FAIL | كنبة ← بحث في الإعدادات: «بحث في الإعدادات» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-tvos-pray | FAIL | tvOS ← الصلاة والأذان: «الأذان لكل صلاة» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-tvos-photos | FAIL | tvOS ← صوري والخلفيات: «احذف صورة» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-tvos-look | FAIL | tvOS ← الشكل: «التخطيط» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-tvos-saver | FAIL | tvOS ← شاشة التوقف: «بدء شاشة التوقف بعد» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-tvos-apps | FAIL | tvOS ← التطبيقات والأزرار: «الخروج من تلفزيونك» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-tvos-content | FAIL | tvOS ← المباريات والأسواق: «مباريات اليوم» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-tvos-family | FAIL | tvOS ← العائلة والجوال: «الرقابة الأبوية» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-tvos-lang | FAIL | tvOS ← اللغة والموقع: «المدينة» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-tvos-update | FAIL | tvOS ← حول والتحديث: «تحديث تلفزيوني» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| settings-tvos-search | FAIL | tvOS ← بحث في الإعدادات: «بحث في الإعدادات» was not on the screen (front:       topResumedActivity=ActivityRecord{50a7374 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12573} |
| Failed to write while dumping service activity: Broken pipe) |  |  |
| saver-start | PASS | a dream runs |
| radio-0 | PASS | : playing, the mute unchanged (yes) |
| radio-1 | FAIL | : not playing after 30 s (see raw/radio-1.log) |
| radio-2 | FAIL | : not playing after 30 s (see raw/radio-2.log) |
| radio-3 | FAIL | : not playing after 30 s (see raw/radio-3.log) |
| radio-4 | FAIL | the تلاوة screen did not open:     topResumedActivity=ActivityRecord{ef57eb7 u0 com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.MainActivity t12578} |
| Failed to write while dumping service activity: Broken pipe |  |  |
| radio-5 | FAIL | the تلاوة screen did not open:     topResumedActivity=ActivityRecord{ef57eb7 u0 com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.MainActivity t12578} |
| Failed to write while dumping service activity: Broken pipe |  |  |
| radio-6 | FAIL | the تلاوة screen did not open:     topResumedActivity=ActivityRecord{ef57eb7 u0 com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.MainActivity t12578} |
| Failed to write while dumping service activity: Broken pipe |  |  |
| radio-7 | FAIL | the تلاوة screen did not open:       topResumedActivity=ActivityRecord{2f9ceab u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12585} |
| Failed to write while dumping service activity: Broken pipe |  |  |
| radio-8 | FAIL | the تلاوة screen did not open:       topResumedActivity=ActivityRecord{2f9ceab u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12585} |
| Failed to write while dumping service activity: Broken pipe |  |  |
| radio-9 | FAIL | the تلاوة screen did not open:       topResumedActivity=ActivityRecord{2f9ceab u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12585} |
| Failed to write while dumping service activity: Broken pipe |  |  |
| radio-10 | FAIL | the تلاوة screen did not open:       topResumedActivity=ActivityRecord{2f9ceab u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12585} |
| Failed to write while dumping service activity: Broken pipe |  |  |
| radio-11 | FAIL | the تلاوة screen did not open:       topResumedActivity=ActivityRecord{2f9ceab u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12585} |
| Failed to write while dumping service activity: Broken pipe |  |  |
| radio-12 | FAIL | the تلاوة screen did not open:       topResumedActivity=ActivityRecord{2f9ceab u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12585} |
| Failed to write while dumping service activity: Broken pipe |  |  |
| display-720p-home | PASS | the home at 1280x720, density 213 |
| display-720p-settings | PASS | the settings at 720p |
| font-large-home | FAIL | the home was not in front at the large font |
| font-large-settings | PASS | the settings at font scale 1.3 |
| adhan-fajr-both | PASS | الفجر: both |
| adhan-fajr-sound | FAIL | الفجر: set sound, the status says «both» |
| adhan-fajr-screen | FAIL | الفجر: set screen, the status says «both» |
| adhan-fajr-none | FAIL | الفجر: set none, the status says «both» |
| adhan-dhuhr-both | PASS | الظهر: both |
| adhan-dhuhr-sound | FAIL | الظهر: set sound, the status says «both» |
| adhan-dhuhr-screen | FAIL | الظهر: set screen, the status says «both» |
| adhan-dhuhr-none | FAIL | الظهر: set none, the status says «both» |
| adhan-asr-both | PASS | العصر: both |
| adhan-asr-sound | FAIL | العصر: set sound, the status says «both» |
| adhan-asr-screen | FAIL | العصر: set screen, the status says «both» |
| adhan-asr-none | FAIL | العصر: set none, the status says «both» |
| adhan-maghrib-both | PASS | المغرب: both |
| adhan-maghrib-sound | FAIL | المغرب: set sound, the status says «both» |
| adhan-maghrib-screen | FAIL | المغرب: set screen, the status says «both» |
| adhan-maghrib-none | FAIL | المغرب: set none, the status says «both» |
| adhan-isha-both | PASS | العشاء: both |
| adhan-isha-sound | FAIL | العشاء: set sound, the status says «both» |
| adhan-isha-screen | FAIL | العشاء: set screen, the status says «both» |
| adhan-isha-none | FAIL | العشاء: set none, the status says «both» |
| games-list | FAIL | the games list was not on the screen:       topResumedActivity=ActivityRecord{a724e96 u0 sa.urtv.screensaver/android.service.dreams.DreamActivity t12587} |
| Failed to write while dumping service activity: Broken pipe |  |  |

## Verdict

RED: jank_home_pct = 22.58% (limit max 15); step settings-buttons-saver failed; step settings-buttons-apps failed; step settings-buttons-content failed; step settings-buttons-family failed; step settings-buttons-lang failed; step settings-buttons-update failed; step settings-buttons-search failed; step settings-couch-pray failed; step settings-couch-photos failed; step settings-couch-look failed; step settings-couch-saver failed; step settings-couch-apps failed; step settings-couch-content failed; step settings-couch-family failed; step settings-couch-lang failed; step settings-couch-update failed; step settings-couch-search failed; step settings-tvos-pray failed; step settings-tvos-photos failed; step settings-tvos-look failed; step settings-tvos-saver failed; step settings-tvos-apps failed; step settings-tvos-content failed; step settings-tvos-family failed; step settings-tvos-lang failed; step settings-tvos-update failed; step settings-tvos-search failed; step radio-1 failed; step radio-2 failed; step radio-3 failed; step radio-4 failed; step radio-5 failed; step radio-6 failed; step radio-7 failed; step radio-8 failed; step radio-9 failed; step radio-10 failed; step radio-11 failed; step radio-12 failed; step font-large-home failed; step adhan-fajr-sound failed; step adhan-fajr-screen failed; step adhan-fajr-none failed; step adhan-dhuhr-sound failed; step adhan-dhuhr-screen failed; step adhan-dhuhr-none failed; step adhan-asr-sound failed; step adhan-asr-screen failed; step adhan-asr-none failed; step adhan-maghrib-sound failed; step adhan-maghrib-screen failed; step adhan-maghrib-none failed; step adhan-isha-sound failed; step adhan-isha-screen failed; step adhan-isha-none failed; step games-list failed
