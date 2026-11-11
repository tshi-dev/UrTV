#!/usr/bin/env bash
# On the CI emulator (.github/workflows/update-shots.yml): pictures of «تحديث تلفزيوني» before (master's layout) and after (this branch's), in the glass and sand looks,
# Arabic and English, in four states. Debug builds only: the activities DebugUpdateShot and DebugSetupShot draw a state without the network or a TV to set up. A shot is taken only while UpdateActivity is the resumed activity; otherwise the script fails.
#   update_shots.sh SAVER_APK BEFORE_LAUNCHER_APK AFTER_LAUNCHER_APK OUT_DIR
set -e
timeout 120 adb wait-for-device
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell wm size 1920x1080
adb shell wm density 320
timeout 180 adb install -r -g "$1" || timeout 180 adb install -r "$1"
OUT="$4"; mkdir -p "$OUT"
saver() { adb shell "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.SET $*" > /dev/null; }
# shoot NAME COMPONENT EXPECTED_ACTIVITY STATE: starts the debug activity, waits until the expected activity is the resumed one (else the script fails), then takes the picture
shoot() {
  local name="$1" comp="$2" expect="$3" state="$4" ok=0
  adb shell am force-stop sa.urtv.home
  adb shell am start -n "$comp" --es preview "$state" > /dev/null
  # the screen must be OURS and resumed before a picture is taken (the first run photographed the emulator's own home, with third parties' thumbnails in it)
  for i in $(seq 1 30); do
    if adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | grep -q "$expect"; then ok=1; break; fi
    sleep 1
  done
  if [ "$ok" != 1 ]; then
    echo "FAIL $name: the top activity is not $expect:"
    adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" || true
    exit 1
  fi
  sleep 2
  adb exec-out screencap -p > "$OUT/$name.png.tmp"
  if ! adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | grep -q "$expect"; then
    echo "FAIL $name: the top activity changed during the shot"; rm -f "$OUT/$name.png.tmp"; exit 1
  fi
  mv "$OUT/$name.png.tmp" "$OUT/$name.png"
  echo "shot $name $(stat -c %s "$OUT/$name.png")"
}

for which in before after; do
  apk="$2"; [ "$which" = after ] && apk="$3"
  timeout 180 adb install -r -g "$apk" || timeout 180 adb install -r "$apk"
  for look in glass sand; do
    for lang in ar en; do
      saver --es look "$look" --es lang "$lang"
      sleep 2
      for state in available downloading waiting_owner done; do
        shoot "$which-update-$look-$lang-$state" sa.urtv.home/.DebugUpdateShot sa.urtv.home/.UpdateActivity "$state"
      done
      for state in start plan progress restart done; do
        shoot "$which-setup-$look-$lang-$state" sa.urtv.home/.setup.DebugSetupShot sa.urtv.home/.setup.SetupActivity "$state"
      done
    done
  done
done
# the redesigned setup (design/43) in the glass look, dark and light, Arabic: every state, including the USB prompt card (prompt) and its 20 s notice (promptlate); this branch only (master has no such states)
for appearance in dark light; do
  saver --es look glass --es lang ar --es appearance "$appearance"
  sleep 2
  for state in start plan progress restart prompt promptlate done; do
    shoot "after-setup-glass-$appearance-ar-$state" sa.urtv.home/.setup.DebugSetupShot sa.urtv.home/.setup.SetupActivity "$state"
  done
done
saver --es look glass --es lang ar --es appearance dark
# «مركز التحكم» (design/43 23-26): the side panel at rest, with the focus on a row, in «تخصيص», and in the light look; the panel is a popup of the home, which stays the resumed activity
for state in rest focus customise; do
  shoot "after-control-glass-dark-ar-$state" sa.urtv.home/.DebugControlShot sa.urtv.home/.MainActivity "$state"
done
saver --es look glass --es lang ar --es appearance light
sleep 2
shoot "after-control-glass-light-ar-rest" sa.urtv.home/.DebugControlShot sa.urtv.home/.MainActivity rest
saver --es look glass --es lang ar --es appearance dark
# the home's stand-in picture (HomeSnapshot): held in front (the skeleton on a fresh emulator), then the live home
shoot "after-snapshot-held" sa.urtv.home/.DebugSnapShot sa.urtv.home/.MainActivity hold
shoot "after-snapshot-live" sa.urtv.home/.DebugSnapShot sa.urtv.home/.MainActivity live
# «توحيد المظهر» (design/43 sync-03…06): the home with the switch on in the glass, the graphite and the light look (the upper buttons, the cards and the apps bar one material), then off in the glass look
for spec in "glass dark on" "graphite dark on" "glass light on" "glass dark off"; do
  set -- $spec
  saver --es look "$1" --es lang ar --es appearance "$2" --es looksync "$3"
  shoot "after-home-sync-$1-$2-$3" sa.urtv.home/.DebugSnapShot sa.urtv.home/.MainActivity live
done
saver --es look glass --es lang ar --es appearance dark --es looksync on
# «تلفزيونك»: the app's entry (YourTvHubActivity, exported: started as the launcher does): four windows; Arabic dark and light, English, and the sand look
for spec in "glass ar dark" "glass ar light" "glass en dark" "sand ar light"; do
  set -- $spec
  saver --es look "$1" --es lang "$2" --es appearance "$3"
  sleep 2
  shoot "after-hub-$1-$3-$2" sa.urtv.screensaver/.YourTvHubActivity sa.urtv.screensaver/.YourTvHubActivity hub
done
saver --es look glass --es lang ar --es appearance dark
# «UrTV النظيف»: the confirmation (focus on «إلغاء»), the sheet «تخصيص», the «on» screen, the second step (a CI emulator has no debugging on: its refusal); then everything back off
saver --es look glass --es lang ar --es appearance dark
for state in describe customise on factory; do
  shoot "after-clean-glass-dark-ar-$state" sa.urtv.home/.DebugCleanShot "$([ "$state" = factory ] && echo sa.urtv.home/.setup.FactoryActivity || echo sa.urtv.home/.CleanActivity)" "$state"
done
adb shell am start -n sa.urtv.home/.DebugCleanShot --es preview off > /dev/null || true

