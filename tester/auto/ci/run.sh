#!/usr/bin/env bash
# Runs on the CI emulator (.github/workflows/auto-round.yml): installs both apps, makes the scripts talk to the emulator, runs tester/auto/round.sh.
#   run.sh SAVER_APK HOME_APK OUT_DIR [SKIP_STEPS]
# The CI checkout is thrown away, so optimize/tv.py (the client that talks to a TV over Wi-Fi) is replaced here by tester/auto/ci/tv_adb.py, which sends the same
# words with the runner's adb: saver.sh, music.sh, start-saver.sh, settings_move.py and round.sh then run unchanged. Never run this on a person's machine.
set +e
SAVER_APK=$1; HOME_APK=$2; OUT=$3; SKIP=${4:-}
cd "$(dirname "$0")/../../.."
mkdir -p "$OUT"
exec > >(tee -a "$OUT/ci.log") 2>&1

adb wait-for-device
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell wm size 1920x1080
adb shell wm density 320
cp tester/auto/ci/tv_adb.py optimize/tv.py
adb install -r -g "$SAVER_APK" || adb install -r "$SAVER_APK"
adb install -r -g "$HOME_APK" || adb install -r "$HOME_APK"
export PY=python3

# the apps' versions in the report, and the TV they ran on
{ echo "emulator: $(adb shell getprop ro.product.model | tr -d '\r') android $(adb shell getprop ro.build.version.release | tr -d '\r')"
  for p in sa.urtv.home sa.urtv.screensaver; do echo "$p $(adb shell dumpsys package $p | grep -m1 versionName | tr -d '\r ')"; done; } > "$OUT/device.txt"
cat "$OUT/device.txt"

# our home is the Home of this TV (the emulator's own launcher is the default until it is told: the Home key would bring that one, and every step that waits for our home would fail)
adb shell cmd role add-role-holder --user 0 android.app.role.HOME sa.urtv.home || echo "(the Home role could not be given)"
adb shell cmd package set-home-activity sa.urtv.home/.MainActivity || echo "(the home could not be made the default)"
echo "home resolves to: $(adb shell cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN | tr -d '\r' | tail -n 1)" | tee -a "$OUT/device.txt"

# a city, so the home has its clock, prayers and weather (and the round has a city to box on the shots)
bash optimize/saver.sh start same || echo "(«ابدأ بـ» could not be answered)"   # 4.4: before the home's first start, or the chooser stands over the round
python3 -I optimize/tv.py "am start -W -n sa.urtv.home/.MainActivity" > /dev/null
sleep 15
bash optimize/saver.sh city الرياض || echo "(the city could not be set)"

# the network: no way to a station = radio is left out, and the report says so
net=0; for try in 1 2 3 4 5; do adb shell "ping -c 1 -W 4 raw.githubusercontent.com" > /dev/null 2>&1 && { net=1; break; }; sleep 6; done   # (run 22: one lost ping left the whole radio step out: asked up to 5 times)
if [ "$net" = 0 ]; then
  echo "the emulator has no network: radio is left out"
  SKIP="${SKIP:+$SKIP,}radio"
fi

export ROUND_THRESHOLDS=tester/auto/ci/thresholds-emulator.tsv STATION_S="${STATION_S:-20}"
bash tester/auto/round.sh --no-install --build "${BUILD_NAME:-emulator}" ${SKIP:+--skip "$SKIP"}
rc=$?
mkdir -p "$OUT/auto"
cp -r "tester/auto/out/${BUILD_NAME:-emulator}/." "$OUT/auto/"
cp "$OUT/device.txt" "$OUT/auto/"
echo "round exit code $rc"
exit 0
