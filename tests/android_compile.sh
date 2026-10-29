#!/usr/bin/env bash
# Compiles both Android modules (the screensaver `app` and the home `launcher`) with javac against Robolectric's android-all (Android 14, API 34),
# with a generated stub R and BuildConfig: no Gradle, no SDK. Catches what the pure-Java tests cannot, a call that does not exist or a type that
# does not fit in the Android-facing code.
# usage: tests/android_compile.sh [checkout]      (default: this one)
#   PASS ...  both compile        FAIL ...  javac's errors follow (exit 1)
#   A source using a class that is @hide in the public SDK fails first (tests/hidden_api_scan.py, no jar needed). With the Android SDK here
#   (ANDROID_HOME or ANDROID_SDK_ROOT with platforms/android-34) the real public android.jar is used, which has no hidden classes at all.
#   SKIP ...  no javac, or the jar is not cached and cannot be fetched (offline): not a failure (exit 0)
#   FAIL ...  also a jar whose sha1 is not the pinned one (a corrupt or swapped download is never silently skipped, and never used)
# The jar (about 130 MB) is fetched once from Maven Central's Google mirror, checked against the sha1 below, and kept OUTSIDE the repo in
# ${URTV_CACHE:-${XDG_CACHE_HOME:-$HOME/.cache}/urtv}. URTV_ANDROID_JAR=/path/to.jar uses a jar you already have (the sha1 is still checked).
set -uo pipefail
ROOT=$(cd "${1:-$(dirname "$0")/..}" && pwd)
VER=14-robolectric-10818077
SHA1=94b1490a891e9be559aa35c87cd8a0c163f32d83
URL=https://maven-central.storage-download.googleapis.com/maven2/org/robolectric/android-all/$VER/android-all-$VER.jar
CACHE=${URTV_CACHE:-${XDG_CACHE_HOME:-$HOME/.cache}/urtv}
JAR=${URTV_ANDROID_JAR:-$CACHE/android-all-$VER.jar}

sha1_of() { if command -v sha1sum >/dev/null 2>&1; then sha1sum "$1" | cut -d' ' -f1; else shasum -a 1 "$1" | cut -d' ' -f1; fi; }

# Hidden APIs first (needs no jar, no javac): android-all has the framework's @hide classes, the public android.jar the apps are really built against has not.
python3 -I "$(dirname "$0")/hidden_api_scan.py" "$ROOT" || exit 1

command -v javac >/dev/null 2>&1 || { echo "SKIP android compile: no javac on this machine"; exit 0; }
# The real public android.jar (API 34) when the Android SDK is here (CI, a developer's machine): it has no hidden classes, so it is the exact check.
SDK_JAR=
for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}"; do
  [ -n "$sdk" ] && [ -f "$sdk/platforms/android-34/android.jar" ] && { SDK_JAR=$sdk/platforms/android-34/android.jar; break; }
done
KIND="android-all API 34 plus the hidden-API scan"
if [ -n "$SDK_JAR" ]; then JAR=$SDK_JAR; KIND="the public android.jar, API 34"; fi
if [ -z "$SDK_JAR" ] && { [ ! -f "$JAR" ] || [ "$(sha1_of "$JAR")" != "$SHA1" ]; }; then
  [ -z "${URTV_ANDROID_JAR:-}" ] || { echo "::error::URTV_ANDROID_JAR is not the expected android-all $VER (sha1 differs)"; echo "FAIL android compile: URTV_ANDROID_JAR is not the expected android-all $VER (sha1 differs)"; exit 1; }
  mkdir -p "$CACHE" 2>/dev/null || { echo "SKIP android compile: cannot make the cache folder"; exit 0; }
  TMPJ=$(mktemp -p "$CACHE" jar.XXXXXX 2>/dev/null) || { echo "SKIP android compile: cannot write to the cache folder"; exit 0; }
  if ! curl -fsSL --retry 2 --connect-timeout 15 --max-time 600 -o "$TMPJ" "$URL" 2>/dev/null; then
    rm -f "$TMPJ"; echo "SKIP android compile: android-all $VER is not cached and could not be downloaded (offline?)"; exit 0
  fi
  if [ "$(sha1_of "$TMPJ")" != "$SHA1" ]; then
    rm -f "$TMPJ"; echo "::error::the downloaded android-all $VER does not match the pinned sha1; it was not used"; echo "FAIL android compile: the downloaded android-all $VER does not match the pinned sha1 (not used)"; exit 1
  fi
  mv "$TMPJ" "$JAR"
fi

W=$(mktemp -d)
trap 'rm -rf "$W"' EXIT
python3 -I - "$ROOT" "$W" <<'PY'
import glob, os, re, sys
root, w = sys.argv[1:3]
for mod, pkg, srcdir in (('launcher', 'sa.urtv.home', 'android/launcher/src/main/java'), ('app', 'sa.urtv.screensaver', 'android/app/src/main/java')):
    names = {}
    for f in glob.glob(f'{root}/{srcdir}/**/*.java', recursive=True):
        for t, n in re.findall(r'\bR\.(\w+)\.(\w+)', open(f, encoding='utf-8').read()):
            names.setdefault(t, set()).add(n)
    out = [f'package {pkg};\npublic final class R {{']
    for t, ns in sorted(names.items()):
        out.append(f'  public static final class {t} {{')
        for i, n in enumerate(sorted(ns)):
            out.append(f'    public static final int {n} = {0x7f000000 + i};')
        out.append('  }')
    out.append('}')
    d = f'{w}/stub_{mod}/' + pkg.replace('.', '/')
    os.makedirs(d, exist_ok=True)
    open(d + '/R.java', 'w').write('\n'.join(out))
    open(d + '/BuildConfig.java', 'w').write(f'package {pkg};\npublic final class BuildConfig {{ public static final String VERSION_NAME="x"; public static final String MARKETING="4.0"; public static final int VERSION_CODE=1; public static final boolean DEBUG=false; public static final String BRANCH="master"; public static final String APPLICATION_ID="{pkg}"; }}')
PY
bad=0
for m in launcher app; do
  mkdir -p "$W/out_$m"
  err=$(javac -encoding UTF-8 -proc:none -cp "$JAR" -d "$W/out_$m" $(find "$ROOT/android/$m/src/main/java" -name '*.java') $(find "$W/stub_$m" -name '*.java') 2>&1 | grep -v '^Picked up\|^Note:')
  if [ -n "$err" ] && echo "$err" | grep -q 'error'; then
    echo "FAIL android compile: the $m module does not compile against $KIND"; echo "$err"; bad=1
  else
    echo "PASS android compile: the $m module compiles against $KIND"
  fi
done
exit $bad
