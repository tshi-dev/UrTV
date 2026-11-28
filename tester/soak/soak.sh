#!/usr/bin/env bash
# The playback soak (R-454, a report: «the screen froze after a day of Quran»): on the CI Android TV emulator (.github/workflows/soak.yml, manual only),
# the home plays the radio and a recitation in turns for HOURS, and every 5 minutes the home's PSS and threads, system_server's PSS and the ANRs so far
# are written to OUT/samples.tsv; tester/soak/judge.py then says GREEN or RED (growth, a restart, an ANR). Never run on a person's TV.
#   soak.sh SAVER_APK HOME_APK OUT_DIR HOURS
set +e
SAVER_APK=$1; HOME_APK=$2; OUT=$3; HOURS=${4:-4}
cd "$(dirname "$0")/../.."
mkdir -p "$OUT"
exec > >(tee -a "$OUT/soak.log") 2>&1
case "$HOURS" in ''|*[!0-9]*) echo "!! soak: HOURS must be a whole number"; exit 2;; esac

adb wait-for-device
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell settings put system screen_off_timeout 2147483647   # the soak is about playback, not the screensaver
adb shell settings put secure screensaver_enabled 0
adb install -r -g "$SAVER_APK" || adb install -r "$SAVER_APK"
adb install -r -g "$HOME_APK" || adb install -r "$HOME_APK"
adb shell cmd role add-role-holder --user 0 android.app.role.HOME sa.urtv.home || true
adb shell am start -W -n sa.urtv.home/.MainActivity > /dev/null
sleep 20
adb logcat -c

play() {   # radio station 1 on even hours, a long surah (al-Baqarah, the first reciter) on odd ones
  if [ $(( $1 % 2 )) = 0 ]; then adb shell am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.PLAY_RADIO --ei station 1 > /dev/null; echo radio
  else adb shell am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.PLAY_SURAH --es reciter "${RECITER:-afasy}" --ei surah 2 > /dev/null; echo surah; fi
}
pss() { adb shell dumpsys meminfo "$1" 2>/dev/null | tr -d '\r' | awk '/TOTAL PSS:/ {print $3; exit} /^ *TOTAL[: ]/ {print $2; exit}'; }

printf '# minutes\tpid\thome_pss_kb\thome_threads\tsystem_server_pss_kb\tanrs\tplaying\n' > "$OUT/samples.tsv"
start=$(date +%s); hour=-1; what=""
while :; do
  now=$(date +%s); mins=$(( (now - start) / 60 ))
  [ "$mins" -ge $(( HOURS * 60 )) ] && break
  h=$(( mins / 60 ))
  if [ "$h" != "$hour" ]; then hour=$h; what=$(play "$h"); fi
  pid=$(adb shell pidof sa.urtv.home | tr -d '\r' | awk '{print $1}'); [ -n "$pid" ] || pid=-
  thr=-; [ "$pid" != - ] && thr=$(adb shell "ls /proc/$pid/task 2>/dev/null | wc -l" | tr -d '\r ')
  hp=$(pss sa.urtv.home); sp=$(pss system_server)
  anr=$(adb logcat -d -b events -s am_anr 2>/dev/null | grep -c am_anr)
  adb shell input keyevent KEYCODE_DPAD_RIGHT; adb shell input keyevent KEYCODE_DPAD_LEFT   # the remote in use now and then, as a person would
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$mins" "$pid" "${hp:--}" "${thr:--}" "${sp:--}" "${anr:-0}" "$what" | tee -a "$OUT/samples.tsv"
  sleep 300
done
adb shell dumpsys meminfo sa.urtv.home > "$OUT/meminfo-end.txt" 2>&1
adb shell "ps -T -p \$(pidof sa.urtv.home)" > "$OUT/threads-end.txt" 2>&1   # which threads: ExoPlayer, station-url, …
adb logcat -d -s UrTVTilawah UrTVRadio AndroidRuntime ActivityManager > "$OUT/logcat.txt" 2>&1
python3 -I tester/soak/judge.py "$OUT/samples.tsv" | tee "$OUT/verdict.txt"
exit "${PIPESTATUS[0]}"
