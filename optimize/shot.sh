#!/usr/bin/env bash
# Screenshots without navigating by hand. Saves full-size PNGs in optimize/run/shots/<NAME>.png, and next to
# each a <NAME>-small.png, 960 px wide: the full ones are too large for some agents to attach (with Pillow, which
# setup.sh installs; without it optimize/png_small.py makes it). An empty capture is taken again (tv.py --shot).
#   optimize/shot.sh home [NAME]          the home screen (after Home, 8 s)
#   optimize/shot.sh screensaver [NAME]   the screensaver (started now, 34 s for its data; SHOT_WAIT=N changes that), then Back; where
#                                         Android won't start it over ADB, our app's preview of the same page
#   optimize/shot.sh favourites [NAME]    «ترتيب المفضلة»
#   optimize/shot.sh settings [NAME]      شاشة تلفزيونك's settings
#   optimize/shot.sh now NAME             whatever is on screen now
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
mkdir -p optimize/run/shots
tv() { "$PY" -I optimize/tv.py "$@" > /dev/null; }
case "${1:-}" in
  home)        tv "input keyevent KEYCODE_HOME"; sleep 8; name=home ;;
  screensaver) how=$(optimize/start-saver.sh --preview); echo "screensaver: $how"; sleep "${SHOT_WAIT:-34}"; name=screensaver ;;
  favourites)  tv "am start -n sa.urtv.home/.DockActivity"; sleep 4; name=favourites ;;
  settings)    tv "am start --activity-clear-task -n sa.urtv.screensaver/.SettingsActivity"; sleep 4; name=settings ;;
  now)         name="${2:?usage: optimize/shot.sh now NAME}" ;;
  *) sed -n '2,10p' "$0"; exit 1 ;;
esac
file="optimize/run/shots/${2:-$name}.png"
"$PY" -I optimize/tv.py --shot "$file"
[ "$1" = screensaver ] && tv "input keyevent KEYCODE_BACK"
small="${file%.png}-small.png"
# the small copy: Pillow when it is there, else optimize/png_small.py (the standard library only, a few seconds); an empty or broken file
# is said as that, never as «Pillow is missing» (4.4: zero-byte screenshots were blamed on Pillow)
if [ ! -s "$file" ]; then
  echo "!! $file is empty: the screenshot was not taken. Run the same command again."; exit 1
elif "$PY" -I -c 'import PIL' 2> /dev/null; then
  "$PY" -I -c 'import sys; from PIL import Image
im = Image.open(sys.argv[1]).convert("RGB"); w = 960
im.resize((w, max(1, round(im.height * w / im.width))), Image.LANCZOS).save(sys.argv[2], optimize=True)' "$file" "$small" 2> /dev/null \
    || { echo "(no small copy: Pillow could not read $file)"; small=; }
elif ! "$PY" -I optimize/png_small.py "$file" "$small"; then
  small=
fi
echo "$file"
[ -z "$small" ] || echo "$small"
