#!/usr/bin/env bash
# One-time setup: the pure-Python ADB client (no adb binary needed, works where macOS blocks adb).
# The APKs are built by GitHub Actions, so no Android SDK is needed here. Windows: run it in Git Bash.
# Adapted from github.com/mihirgupta0900/tv-home (MIT).
set -euo pipefail
cd "$(dirname "$0")"
. ./python.sh
pick_python || exit 1
# A phone or an iPad has no wheels for cryptography and Pillow (they would have to be built): Termux (pkg install python-cryptography) and Alpine /
# iSH (apk add py3-cryptography py3-pillow) ship them, so the venv may see the system's packages and pip installs only what is missing. Set
# URTV_SYSTEM_SITE=1 to ask for it anywhere; Termux and Alpine are recognised by themselves. See docs/PHONE-SETUP.md.
system_site=
if [ "${URTV_SYSTEM_SITE:-}" = 1 ] || [ -f /etc/alpine-release ] || [[ "${PREFIX:-}" == */com.termux/* ]]; then system_site=--system-site-packages; fi
"${SYS_PY[@]}" -m venv ${system_site:+"$system_site"} venv
if [ ! -e venv/bin/python ] && [ -e venv/Scripts/python.exe ]; then
  # Windows puts it in venv\Scripts: every script calls optimize/venv/bin/python, so point that there
  mkdir -p venv/bin
  printf '#!/usr/bin/env bash\nexec "$(dirname "$0")/../Scripts/python.exe" "$@"\n' > venv/bin/python
  chmod +x venv/bin/python
fi
# requirements.txt: exact versions with every file's sha256 (.github/tools/pin_requirements.py), so pip refuses a changed file. A phone's
# system packages are other versions with no hash to match: there pip gets the names alone, and installs only what the system lacks.
reqs=requirements.txt
hashes=--require-hashes
if [ -n "$system_site" ]; then   # Pillow only serves photos.sh: it must not stop the rest where it cannot be built
  hashes=
  reqs=$(mktemp); sed -n 's/^\([A-Za-z][A-Za-z0-9_.-]*\)==.*/\1/p' requirements.txt | grep -vi '^pillow$' > "$reqs" || true
  venv/bin/python -c "import PIL" > /dev/null 2>&1 || venv/bin/python -m pip install -q --no-cache-dir pillow > /dev/null 2>&1 \
    || echo "(Pillow is not here: photos.sh needs it; Termux: pkg install python-pillow, Alpine / iSH: apk add py3-pillow, then optimize/setup.sh again)"
fi
if ! venv/bin/python -m pip install -q --no-cache-dir ${hashes:+"$hashes"} -r "$reqs"; then
  echo "!! Couldn't install the Python packages into optimize/venv (pip's error is above). If it is a network"
  echo "   error, allow this terminal network access (pypi.org), then run optimize/setup.sh again."
  [ -z "$system_site" ] || echo "   On a phone or an iPad the packages that need building come from the system: Termux: pkg install python-cryptography; Alpine / iSH: apk add py3-cryptography (docs/PHONE-SETUP.md)."
  exit 1
fi
[ "$reqs" = requirements.txt ] || rm -f "$reqs"
# iPhone/Samsung HEIC photos for photos.sh: optional, so a computer with no wheel for it still gets everything else
venv/bin/python -m pip install -q --no-cache-dir pillow-heif > /dev/null 2>&1 || echo "(HEIC photos won't be readable here: photos.sh takes JPEG, PNG, WebP, GIF and BMP)"
# every `venv/bin/python -I` call verifies TLS with certifi (python.org's macOS Python ships no CA certificates)
for sp in venv/lib/python3.*/site-packages venv/Lib/site-packages; do
  [ -d "$sp" ] || continue
  printf '%s\n' 'import os' 'try:' '    import certifi' '    os.environ.setdefault("SSL_CERT_FILE", certifi.where())' \
    'except ImportError:' '    pass' > "$sp/sitecustomize.py"
done
# pip said yes: make sure tv.py will really start (keygen loads cryptography's DLL, which plain adb_shell doesn't)
if ! err=$(venv/bin/python -c "import adb_shell.adb_device, adb_shell.auth.keygen, adb_shell.auth.sign_pythonrsa" 2>&1); then
  echo "$err" | tail -1
  if grep -qi "too long" <<< "$err"; then
    echo "!! This folder's path is too long for Windows (over 260 characters inside optimize/venv). Clone UrTV into"
    echo "   a short folder, such as C:\UrTV: git clone -c core.longpaths=true https://github.com/tshi-dev/UrTV C:/UrTV"
  else
    echo "!! The ADB package doesn't load from optimize/venv. Delete the folder optimize/venv, then run"
    echo "   optimize/setup.sh again."
  fi
  exit 1
fi
echo "✓ Python ADB client in optimize/venv"
# an HTTP error status still means the TLS handshake verified; only a certificate or network error fails
if venv/bin/python -I -c 'import urllib.request as u, urllib.error as e
try: u.urlopen("https://github.com", timeout=15)
except e.HTTPError: pass' 2>/dev/null; then
  echo "✓ TLS certificates"
else
  echo "!! This computer can't open https://github.com from optimize/venv (certificates or network). Check that it"
  echo "   is online, delete the folder optimize/venv, then run optimize/setup.sh again."
fi
# the key the TV trusts lives in the home folder, outside this clone (tv.py makes it on the first connection, and moves an
# older one here from optimize/adbkey): a new clone of UrTV is the same computer and the TV does not ask again
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) keydir='%USERPROFILE%\.urtv' ;; *) keydir='~/.urtv' ;; esac
echo "✓ ADB key: $keydir (outside this folder; a new clone of UrTV uses the same one. If an older UrTV folder has optimize/adbkey, copy it there)"
