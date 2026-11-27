# Review of 4.4: the delta after the first review

Range: 987aa209..1210cac954a22bc8ebf0b5e83e4708c96dbd2641 (origin/work/44-beta4; beta 4 46dcad40 is in it)
Reviewer: a fresh independent review session (read-only), with five fresh subagent reviewers by area: the home screen (launcher top level), setup / pairing / the add-on, the adhan and the phone page, the screensaver app, and everything not Java (the page src/, .github/tools, workflows, tester/auto, games tools, tests). Every high finding was checked against the code by the review session itself.
Date: 2026-11-27
Read: 307 commits, 728 files changed, 101139 insertions(+), 2511 deletions(-) (the games' json data only parsed and run through games/check.py and check_words.py; dist/ not read, src/ instead)
API surface diff (`api_surface_diff.py --base 987aa209`): PASS, 0 new UI-thread paths to a cross-process call. New: the signature permission `sa.urtv.home.LOOK` (both apps) and the exported `.LookPanelActivity` behind it; the launcher provider `sa.urtv.home.pairfiles` (EXTRAS, signature); `READ_MEDIA_IMAGES` in the launcher; `INTERNET` in the add-on (the paired route); DockReceiver's new actions (DUMP). Against v4.3.517: PASS.
Regressions group (`bash tests/run.sh regressions` on 1210cac9, in the review container): 176/182 passed; FAIL tests/regressions/attribution_test.py (F91); tests/check.py and the four page .mjs tests could not run here (no fontTools, no playwright in this container: environment, not code), CI's run decides them

Gate record: the findings below are also in `docs/dev/reviews/4.4.md` (F87–F119), whose Range now ends at 1210cac9. **Status: PENDING.** `review_gate.py` fails while F87–F91 (high) are open.

Summary: blocker 0, high 5, medium 11, low 17, all open. The highs: a Cancel on the USB-debugging question again ends with the wrong message and no retry (R-262); the home trusts any app answering the add-on's authority with the pairing code; photos from a stick fail on a normal phone photo; the match archive aborts and freezes the sports data; the attribution gate is red on 22 commits. Migration from 4.3 is clean (no renamed or retyped key; city, teams, looks, adhan, photos kept); nothing touches setupwraith or the Wi-Fi settings; no secret, PIN or recovery code is printed, logged or committed; the phone page escapes what it shows and keeps its PIN gate.

Blockers and majors (high) open: F87, F88, F89, F90, F91.

## Findings

### F87 [high] [open] «إلغاء» on the USB-debugging question ends as «انقطع الاتصال» with no retry (R-262 back) (delta D-S1)

- Where: `android/launcher/src/main/java/sa/urtv/home/setup/SetupSession.java:48-60` (openAny); read at `setup/SetupRunner.java:56, :741, :964`
- What a person sees: «جهّز كل شي» on any TV whose paired route is not "paired" (most TVs: no add-on, not paired, Android before 11): they press «إلغاء» on «Allow USB debugging?», or no dialog comes; the screen says the connection dropped, not «ضغطت «إلغاء»…», and the retry offer is gone.
- Why: openAny rethrows the 5555 failure as `first.getMessage() + "; paired route: " + why`; SetupRunner and friendly() compare with `AdbClient.CANCELLED.equals(...)`, End.retry with the exact texts, so none match. When the TV is paired, a Cancel silently goes on over the paired route, against the person's «no».
- Smallest fix: in openAny rethrow `first` unchanged when its message is CANCELLED (no fallback after a Cancel), and keep the 5555 message as it was (log the paired-route word, do not append it); a test that drives openAny with a cancel and checks End.retry. `tests/pair_test.py:65-66` pins the suffix, not this consequence.

### F88 [high] [open] The home hands the pairing code and shell commands to whatever app owns `sa.urtv.extras.bridge` (delta D-S2)

- Where: `android/launcher/src/main/java/sa/urtv/home/setup/PairRoute.java:25-30` (call), used by PairBridge.submit, PairRoute.open and the run/exec lambdas; the same in the older `Extras.java` calls
- What a person sees: nothing. On a TV without «إضافات تلفزيونك», any installed app that declares that authority with no permission answers PAIR_STATUS (available) and PAIR_ARM, then receives PAIR_CODE, the six-digit code the person types on the phone page while the TV's pairing dialog is open: it pairs its own key with adbd and has a full adb shell; it also receives every setup command and can forge their output.
- Why: the add-on checks its caller (ExtrasCallProvider, SignDigest.callerIs(HOME)) but the home never checks who answers the provider.
- Smallest fix: at the top of PairRoute.call (and Extras' calls) `resolveContentProvider(EXTRAS_AUTHORITY, 0)`: package must be `sa.urtv.extras` and `SignDigest.of` equal to the home's; else null ("no_addon"). Pin it in `tests/pair_commands_test.py`.

### F89 [high] [open] «أضف من فلاشة أو التنزيلات» fails on a normal phone photo and says to install the screensaver app (delta D-L1)

- Where: `android/launcher/src/main/java/sa/urtv/home/PhotoAddActivity.java:125-133`; `IconEditorActivity.java:322-331` (decode); `PhotoShrink.screenMax`
- What a person sees: a 1080p TV, a 12 MP photo (4032x3024) on a stick or in DCIM, OK: «ثبّت أو حدّث تطبيق «شاشة تلفزيونك» أولاً» (or «ما قدرنا نقرأ الصورة» when the decode runs out of memory); the photo is not added.
- Why: screenMax is a long-side limit; decode treats it as a short-side one (`side = min(w,h)`; 3024/2 < 1920 so inSampleSize 1): the whole 12 MP bitmap (~48 MB) is decoded and never scaled; the 3-5 MB JPEG goes through ContentResolver.call in a Bundle (Binder limit about 1 MB), call() swallows the TransactionTooLargeException and returns null, which reads as «not installed». Own.check would refuse it anyway (over 3 MB / 3840x2160).
- Smallest fix: make the bytes as the phone path does (`PhotoShrink.toSize(bytes, screenMax)`, long side) or scale the decoded bitmap to screenMax on its long side; a null from call() while the screensaver is installed reads «ما قدرنا نضيف». `tests/photo_from_file_test.py` only greps `PhotoShrink.screenMax(this)`: test the size of a fake 4032x3024 instead.

### F90 [high] [open] The match archive stops with «the store shrank» and pushes nothing once a game is fed again under another season (#70) (delta D-T1)

- Where: `.github/tools/match_archive.py:148-153` (Store.put deletes the copy in the other season), `:393-396` (the shrink check)
- What a person sees: after this branch reaches the archive workflow, the store still has MLS games under «2026» and «2026-2027» (#70); the next fixtures run feeds them again: every archive run fails and pushes nothing, so results, form, h2h, sports/ and honours inputs on every TV's page freeze for as long as TheSportsDB sends those games.
- Why: put() deletes cross-season copies but they are not in `dropped`; `after < before - dropped` raises. Reproduced with the test's own fixture fed events (`build(leagues_dir(mls, lg="4346"), prev70)`: «the store shrank from 5 to 3 events»). `tests/match_archive_test.py:392-400` passes only because it feeds no events.
- Smallest fix: count put()'s cross-season deletions (a `moved` counter added to `dropped`), or leave them to dedupe(); add the fed-events case to the test.

### F91 [high] [open] The release gate is red on this range: `tests/regressions/attribution_test.py` fails on 22 commits (delta D-T2)

- Where: `tests/regressions/attribution_test.py` (strict in `regressions.yml`, ATTRIBUTION_STRICT=1)
- What a person sees: nothing ships: android.yml's publish and promote-beta refuse. The gate does its job; this is a release blocker, not an unsafe path.
- Why: 22 commits of the range are authored or committed as `tshi-dev <tshi-dev@users.noreply.github.com>` (no `272740088+`), the tip 1210cac9 among them («committer is not tshi-dev» and «another person's commit that no Merge PR brought in»).
- Smallest fix: history is not rewritten: the owner adds the shas to ALLOW with their reason (or says otherwise); fix the release session's git email so it does not recur.

### F92 [medium] [open] Offline or on a stream failure, the Fajr adhan («الصلاة خير من النوم») can stand in at Dhuhr, Asr, Maghrib or Isha (delta D-A1)

- Where: `android/launcher/src/main/java/sa/urtv/home/tilawah/MusicService.java:639-643` (standIn), used at `:623` and in the stream-failure branch; `tilawah/AdhanKeep.java:44-49` (stand)
- What a person sees: a default TV (voices 1 and 10): voice 1 not saved yet (just changed, or its keep fetch failed), voice 10 saved; offline at Dhuhr: the Fajr recording plays and the bar names it. The same for a per-prayer muezzin.
- Why: for a non-Fajr voice `other` is prefs.fajrVoice() and stand() tries it first; the lowest-saved loop can also pick 10, 13, 17, 20 (AdhanVoices.FAJR). Nothing looks at which prayer it is. `tests/AdhanKeepRun.java:23` (`stand(3, 10, s)` → 10) pins the bug.
- Smallest fix: stand() takes whether the prayer is Fajr and skips AdhanVoices.isFajr(v) otherwise; standIn passes `adhanPrayer == FAJR`; a «Fajr voice never at Dhuhr» case in AdhanKeepRun.

### F93 [medium] [open] «أذاني من ملف»: the file copy and a cross-process call run on the UI thread (delta D-A2)

- Where: `android/launcher/src/main/java/sa/urtv/home/tilawah/AudioActivity.java:202-207` (AudioUse.set inside runOnUiThread); `AudioUse.java:36, :41`
- What a person sees: a slow TV (TCL): the remote freezes after the file is picked, for the copy (up to 5 min of audio through SafeReplace) plus AdhanScheduler.schedule → Place.get, a query to the screensaver that may have to start it (the R-271 / QA-12 pattern). The older use() path has the same; the gate does not see a lambda posted from a thread.
- Smallest fix: call AudioUse.set on the «audio-copy» thread, post back only the toast and finish(); AudioUse.set uses AdhanScheduler.scheduleSoon.

### F94 [medium] [open] After a wrong pairing code, no new code is accepted though the TV says to type the new one (delta D-S3)

- Where: `android/extras/src/main/java/sa/urtv/home/pair/PairState.java` (finish → FAILED; begin accepts only ARMED); `setup/PairBridge.java` (retry has no caller); `assets/phone/adb.html:96`
- What a person sees: «ما انقبل الرمز. اكتب الرمز الجديد…» on the TV; the phone's next code gets not_armed («off») until they press «ابدأ الإقران» again behind the PIN.
- Smallest fix: PairBridge.submit calls PairBridge.retry when the add-on's state is failed (still armed on the home side), or the message says to press «ابدأ الإقران» again.

### F95 [medium] [open] Paired-route connections leak in the add-on; after two, every open answers «busy» until it dies (delta D-S4)

- Where: `android/extras/src/main/java/sa/urtv/home/pair/PairHost.java` (MAX_OPEN = 2, open(), OPEN)
- What a person sees: a paired-only Google TV: the home is killed mid-setup twice (low memory, update, force-stop); then setup, SetupRecovery, the boot restore, reapplyHomeRole and enableStock all fail until a reboot (the add-on is a long-lived accessibility process).
- Why: a handle is closed only by PAIR_CLOSE/PAIR_FORGET; the size check and the put are in two synchronized blocks.
- Smallest fix: when full, close and evict the oldest (or any idle over N minutes); check and insert under one lock.

### F96 [medium] [open] No timeout on the paired route (delta D-S5)

- Where: `pair/PairShell.java` run/exec, `pair/PairHost.java` run/exec
- What a person sees: wireless debugging's Wi-Fi link stalls mid-setup: the setup thread and an add-on binder thread hang for good; SetupRunner stays «running».
- Why: the loopback AdbClient sets a read timeout everywhere; the libadb stream read and the ContentResolver.call have none (libadb's own default not confirmed: its source was not reachable).
- Smallest fix: set the libadb manager's timeout and cap each run in the add-on (a future with get(timeout)).

### F97 [medium] [open] Photos from a stick or Downloads: likely nothing found on Android 10 TVs (unverified on a TV) (delta D-L2)

- Where: `android/launcher/src/main/AndroidManifest.xml` (no `requestLegacyExternalStorage`, targetSdk 34); PhotoAddActivity, AudioActivity's file lists
- What a person sees: an Android 10 TV: the permission is allowed and the list still says «لا صور هنا…».
- Why: API 29 with targetSdk ≥ 29 is scoped storage: File.listFiles on /storage/emulated/0/Download, DCIM or /storage/<uuid> returns nothing; path access came back only in 11.
- Smallest fix: `android:requestLegacyExternalStorage="true"` (ignored on 11+), or MediaStore on API 29. Check on one Android 10 TV first.

### F98 [medium] [open] Portrait photos from a stick are added sideways (delta D-L3)

- Where: `PhotoAddActivity.java:125-128`, `IconEditorActivity.java:322-331`
- Why: BitmapFactory ignores EXIF orientation and the re-encoded JPEG drops it (the phone path is right: the browser rotates).
- Smallest fix: read ExifInterface and rotate with a Matrix before compressing (thumbnails and the icon editor too).

### F99 [medium] [open] «الوضع الخفيف» turns itself on for 14 days after one low-memory burst, saying «مرتين هذا الأسبوع» (delta D-L4)

- Where: `MainActivity.java:546` (onTrimMemory → LiteAutoStore.note); `LiteAuto.java:52-59`
- Why: every RUNNING_LOW/CRITICAL callback adds a signal, no debounce; Android sends them in bursts seconds apart; two within 7 days is enough. `tests/LiteAutoRun.java` adds 40 trims 1 s apart and never asserts «not on».
- Smallest fix: one signal per kind per some hours (or two at least ~1 h apart); a test «two trims 1 s apart → off».

### F100 [medium] [open] An input favourite (HDMI) is dropped by «ترتيب المفضلة» and by «تعديل المفضلة» (delta D-L5)

- Where: `DockActivity.java:52` → `Dock.shown` (`Dock.java:242-251`, keeps only launchable packages); FavEdit's row from dockCards in MainActivity (~3010-3020)
- What a person sees: HDMI 1 added as a favourite from «أسماء المصادر»; any change in «ترتيب المفضلة» saves the row without `@input:…`: the tile is gone. In «تعديل المفضلة», an input the TV does not report at that moment is dropped on save.
- Smallest fix: Dock.shown keeps InputFav.isInput entries; leaveEdit merges back saved inputs missing from the row.

### F101 [medium] [open] «المظهر على الرئيسية» opens over the settings screen, not the home (delta D-S6)

- Where: `android/app/src/main/java/sa/urtv/screensaver/LookPanelActivity.java:52-59`; app manifest (`.LookPanelActivity` has no taskAffinity)
- What a person sees: with the panel switch on (off by default): Settings → «المظهر على الرئيسية»: the see-through panel sits on SettingsActivity; the home's move aside and every preview happen behind it.
- Why: NEW_TASK with the app's default affinity reuses the saver task (hub, settings) and brings it over the home.
- Smallest fix: `android:taskAffinity="sa.urtv.screensaver.look"` (as `.cover`), or finish SettingsActivity before opening.

### F102 [medium] [open] tester/auto/round.sh changes a person's TV and does not put it back: the adhan per prayer, the home layout, screen size and density (delta D-T3)

- Where: `tester/auto/round.sh:271-286` (`adhan state … both`, `before` discarded), `:178` (`homelayout classic`), `:110, :255` (`wm size reset; wm density reset`)
- What a person sees: a tester's TV with Fajr on «شاشة فقط» plays Fajr aloud at night after a round; a «كنبة»/tvOS layout comes back «كلاسيكي» (the preset maybe «custom»); a size or density override is lost.
- Why: restore() re-imports `saver.sh export`, which has no prayer_sound/prayer_screen (settings_move.read_adhan) nor homelayout; wm is reset, not restored.
- Smallest fix: save the per-prayer state (lib.adhan_state), the layout (and settingsstyle) and the wm Override lines before the steps, and set them back in restore().

### F103 [low] [open] The new «idle» radio restart has no limit (delta D-A3)

- Where: `tilawah/MusicService.java:929-932`, `tilawah/StallWatch.java:33-39`
- Why: idleTooLong returns RESTART outside check()'s restarts window, so MAX_RESTARTS/GIVE_UP never applies; with focus refused at onPrepared the station restarts every ~20 s for good, cycling sources («يعيد الاتصال…»).
- Smallest fix: count the idle restart in the same window; treat a refused focus as waiting.

### F104 [low] [open] An adhan preview while offline plays another muezzin under the chosen row (delta D-A4)

- Where: `MusicService.java:623`
- Smallest fix: `previewSeconds == 0 &&` on the stand-in condition.

### F105 [low] [open] «لا فلاشة» can no longer show in «أذاني من ملف» (delta D-A5)

- Where: `tilawah/AudioActivity.java:155-158` (Downloads is always a root, so roots is never empty)
- Smallest fix: say no USB stick was found above the Downloads list when it is the only root.

### F106 [low] [open] The add-on swallows Home when the home is gone (delta D-S7)

- Where: `android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java:289-298` (homeKeyCaught true when startActivity fails; roleAnswer kept when askHome is null)
- Smallest fix: return false when the start fails or the home is not installed; clear roleAnswer on the home's PACKAGE_REMOVED.

### F107 [low] [open] The add-on now ships ARM native code only (delta D-S8)

- Where: `android/extras/build.gradle:10` (abiFilters armeabi-v7a, arm64-v8a: Conscrypt's .so)
- What a person sees: an x86 box without ARM translation can no longer install or update the add-on (INSTALL_FAILED_NO_MATCHING_ABIS); before it had no native code. Rare on TVs.
- Smallest fix: add x86/x86_64, or accept it in the docs.

### F108 [low] [open] Small leftovers on the paired route (delta D-S9)

- Where: `ControlCenter.open` (attach listener never removed when attached with a null token); `setup/PairFiles` copies in cache/pair left when the home dies mid-exec; a RuntimeException in the add-on's PAIR_RUN/PAIR_OPEN reaches the home as null and reads «no_addon» instead of «failed».
- Smallest fix: remove the listener; sweep cache/pair at start; catch RuntimeException in the add-on and answer «failed».

### F109 [low] [open] The home can stay shrunk after «المظهر», and its card page keep running behind an app (delta D-L6)

- Where: `MainActivity.java:372, :384-387, :401-411`; `HomeCard.java:190`
- Why: aside(true) is undone only by the panel's LOOK_PANEL close (onStop); a dead screensaver process leaves it; a preview's homeCard.refresh() while paused sets resumed=true and a direct jump to an app gives no second onPause.
- Smallest fix: onResume resets the scale when the panel is not up; the close path calls homeCard.pause() when not resumed.

### F110 [low] [open] The safe page's «افتح الشاشة الأصلية» may enable a stock home the maker disabled (delta D-L7)

- Where: `SafeActivity.java:46, :66-67, :104-113`; `StockHome.decide` (`StockHome.java:42`)
- Why: disabledStock() returns any disabled KNOWN package (a vendor-disabled tvlauncher next to an enabled one) and decide() prefers it.
- Smallest fix: only packages in our own ledger (disabled-by-us.txt) count as disabled; otherwise open the enabled home.

### F111 [low] [open] sceneBlank is never reset: after one failed scene, «الآفاق» runs with the photo wall still rotating under it (delta D-L8)

- Where: `MainActivity.java:4275-4319`
- Smallest fix: sceneBlank = false when the scene layer is removed or a new one created.

### F112 [low] [open] «محاذاة التطبيقات» right/left does nothing on the «زجاجي» bar in 2-3 rows (delta D-L9)

- Where: `FavBar.java:244` (absolute RIGHT/LEFT) vs `DockGrid.java:60-62` (relative START/END only)
- Smallest fix: DockGrid also handles Gravity.LEFT/RIGHT.

### F113 [low] [open] Settings rows «الأذان لكل صلاة», «المؤذن», «الأدعية والتذكيرات» show stale values after a change (delta D-S10)

- Where: `SettingsActivity.java:2178-2184, :2208, :2217`; onResume `:233-246`
- Why: onResume resets adhanAsked and the others but not prayersAsked / homeAsked.
- Smallest fix: `prayersAsked = false; homeAsked.clear();` in onResume.

### F114 [low] [open] The look panel offers «tvOS» as a layout alone (delta D-S11)

- Where: `LookPanelActivity.java:190, :250-256`
- Why: values(0) is all of HOME_LAYOUTS; pickHomeLayout and SaverReceiver keep the rule «no half look».
- Smallest fix: skip TVOS_LAYOUT in part 0 unless it is already the layout.

### F115 [low] [open] «رجّع الشكل السابق» leaves the settings style of a full look behind (delta D-S12)

- Where: `Prefs.java` ~1735-1758 (lookWhole/lookPut, PREV_KEYS)
- Smallest fix: add settingsStyle, settingsStyleBefore, homePanelHidden to PREV_KEYS.

### F116 [low] [open] `tests/settings_432_places.tsv` is not the 4.3.2 list it says (delta D-S13)

- Where: `tests/settings_432_places.tsv` (4.4 rows; lines 83 and 126 list only the new names; one `renamed` line)
- Why: settings_one_form_test then passes without checking that the real 4.3.2 names still lead somewhere; a search for «اجعل…» no longer finds the row.
- Smallest fix: `renamed` lines for the two old names (and a search alias); drop the rows that are not from 4.3.2.

### F117 [low] [open] auto-round: the prune deletes the newest rounds, and `inputs.skip` goes into the script unquoted (delta D-T4)

- Where: `.github/workflows/auto-round.yml:108` (`sort | head -n -9`: lexical, auto-10 before auto-2; a failed push still prints the link), `:83` (`${{ inputs.skip }}` in `script:`, injection by anyone who can dispatch, i.e. writers)
- Smallest fix: `sort -t- -k2,2n`; fail the step on a failed push; pass skip through env and quote it.

### F118 [low] [open] honours.yml also publishes from `next` to data-honours, which stable TVs read (delta D-T5)

- Where: `.github/workflows/honours.yml:12-17`
- Smallest fix: master, schedule and manual runs only.

### F119 [low] [open] honours.py keeps its «a source that does not answer keeps the published files» only in part (delta D-T6)

- Where: `.github/tools/honours.py` (http: `int(retry-after)` crashes on an HTTP-date; a Wikipedia batch missing some pages shrinks a league's list; only fully empty bodies fall back)
- Smallest fix: parse Retry-After as a date too; fall back per league when the new list is shorter than the published one.
