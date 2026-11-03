#!/usr/bin/env bash
# Builds N tiny launchable stub apps (urtv.stub1 ... ) so the emulator has enough favourites to fill and overflow the tray.
#   make_stubs.sh OUT_DIR [N]
# Needs the Android SDK build-tools (aapt2, apksigner) and a platform android.jar, as every GitHub runner has; signed with the debug key.
set -euo pipefail
OUT=$1; N=${2:-8}
SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}
BT=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
JAR=$(ls -d "$SDK"/platforms/android-* | sort -V | tail -1)/android.jar
KS="$HOME/.android/debug.keystore"
if [ ! -f "$KS" ]; then mkdir -p "$HOME/.android"; keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1; fi
mkdir -p "$OUT"
for i in $(seq 1 "$N"); do
  d=$(mktemp -d)
  cat > "$d/AndroidManifest.xml" <<M
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="urtv.stub$i">
  <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="30"/>
  <application android:label="Stub $i">
    <activity android:name="android.app.Activity" android:exported="true">
      <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LEANBACK_LAUNCHER"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
    </activity>
  </application>
</manifest>
M
  "$BT/aapt2" link -o "$d/stub.apk" --manifest "$d/AndroidManifest.xml" -I "$JAR" >/dev/null
  "$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android "$d/stub.apk"
  cp "$d/stub.apk" "$OUT/stub$i.apk"
done
ls "$OUT"
