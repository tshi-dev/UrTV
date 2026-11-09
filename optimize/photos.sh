#!/usr/bin/env bash
# The owner's own photos for the screensaver and the home screen: 1 to 30 (10 on a weak TV, under 2.5 GB), from their agent. No remote, no menus.
#   optimize/photos.sh add FILE...       make each ready on this computer (upright, at the TV's screen size, at most 1920 px wide,
#                                        JPEG 85, no location or camera data) and put it on the TV (up to 50 in all; 10 on a weak TV)
#   optimize/photos.sh list              how many, the mode, each one's size
#   optimize/photos.sh del N             remove photo N (N from 'list'); the ones after it move up
#   optimize/photos.sh clear             remove them all: the TV is as before
#   optimize/photos.sh mode mix|only     mix = they join our Saudi photos (default); only = they are the only ones
#   optimize/photos.sh show              screenshot the screensaver showing them (optimize/run/shots/own-photo.png);
#                                        it waits 70 s for the page to settle on one of them, so it takes about 80 s
# Both the screensaver and the home screen's wallpaper use them; the owner can change the mode or remove them all in the app's
# settings («صوري»), and adds them only here. Needs Pillow on this computer (optimize/setup.sh installs it).
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
MAX=30   # the most any TV keeps; the TV's own limit (10 on a weak TV) is read from its list (count)
STAGE=/data/local/tmp/urtv-own
tv() { "$PY" -I optimize/tv.py "$@"; }
send() {   # $1 extras; prints the receiver's answer (the "data" of am broadcast); exit 1 on "error:" or when there is no answer
  local raw out
  raw=$(tv "am broadcast -n sa.urtv.screensaver/.SaverReceiver -a sa.urtv.screensaver.PHOTOS $1" | tr -d '\r') || raw=""
  if ! grep -q 'data=' <<< "$raw"; then
    echo "!! photos.sh: no answer from the TV's screensaver app (it is not installed, was just updated, or the connection dropped), so nothing is known to have changed. Run optimize/install.sh saver, then the same command again." >&2
    return 1
  fi
  out=$(sed -n '/data="/,$p' <<< "$raw" | sed '1s/.*data="//; $s/"$//')
  echo "$out"
  [[ "$out" != error:* ]]
}
count() {   # photos on the TV now
  local n
  local l m
  l=$(send "--es op list")
  n=$(sed -n 's/^photos: \([0-9]*\) of.*/\1/p' <<< "$l")
  m=$(sed -n 's/^photos: [0-9]* of \([0-9]*\).*/\1/p' <<< "$l")
  [[ "$m" =~ ^[0-9]+$ ]] && [ "$m" -gt 0 ] && echo "$m" > "${TMPDIR:-/tmp}/urtv-photos-max.$$"
  [[ "$n" =~ ^[0-9]+$ ]] || { echo "error: the TV did not say how many photos it has (is the screensaver app installed and up to date?)" >&2; exit 1; }
  echo "$n"
}
screen() {   # the TV's screen as WxH (the size apps get, so an overridden size counts); 1920x1080 when it does not say
  local s
  s=$(tv "wm size" 2> /dev/null | grep -Eo '[0-9]+x[0-9]+' | tail -1 || true)
  echo "${s:-1920x1080}"
}
work=
cleanup() { [ -z "$work" ] || rm -rf "$work"; }
trap cleanup EXIT

case "${1:-}" in
  list)  send "--es op list" ;;
  clear) send "--es op clear" ;;
  del)
    [[ "${2:-}" =~ ^[0-9]+$ ]] || { echo "usage: optimize/photos.sh del N   (N from 'list')"; exit 1; }
    send "--es op del --ei n $2" ;;
  mode)
    case "${2:-}" in mix|only) ;; *) echo "usage: optimize/photos.sh mode mix|only"; exit 1 ;; esac
    send "--es op mode --es mode $2" ;;
  add)
    shift
    [ $# -gt 0 ] || { echo "usage: optimize/photos.sh add FILE..."; exit 1; }
    [ $# -le $MAX ] || { echo "error: at most $MAX photos"; exit 1; }
    for f in "$@"; do [ -f "$f" ] || { echo "error: $f is not a file"; exit 1; }; done
    have=$(count)
    tvmax=$(cat "${TMPDIR:-/tmp}/urtv-photos-max.$$" 2> /dev/null || echo "$MAX"); rm -f "${TMPDIR:-/tmp}/urtv-photos-max.$$"
    [[ "$tvmax" =~ ^[0-9]+$ ]] && [ "$tvmax" -le "$MAX" ] || tvmax=$MAX
    MAX=$tvmax   # this TV's own limit (an older screensaver app says 10)
    [ $((have + $#)) -le $MAX ] || { echo "error: the TV has $have; room for $((MAX - have)) more (photos.sh del N removes one)"; exit 1; }
    size=$(screen)
    work=$(mktemp -d)
    k=0
    for f in "$@"; do   # all of them first: one bad file stops everything before anything is uploaded
      k=$((k + 1))
      px=$("$PY" -I optimize/photo_prep.py "$f" "$work/$k.jpg" --screen "$size") || exit 1   # its message is already on screen
      echo "prepared $f: $px px"
    done
    tv "mkdir -p $STAGE" > /dev/null
    k=0
    for f in "$@"; do
      k=$((k + 1))
      bytes=$(wc -c < "$work/$k.jpg" | tr -d ' ')
      tv --push-b64 "$work/$k.jpg" "$STAGE/in.jpg" | tail -1
      # the shell streams it into the app (its folder is private); where this TV lacks `content write`, the app reads it itself
      tv "content write --uri content://sa.urtv.screensaver.config/own-in < $STAGE/in.jpg" > /dev/null 2>&1 || true
      out=$(send "--es op add --ei size $bytes" || true)
      case "$out" in error:*)
        out=$(send "--es op add --ei size $bytes --es path $STAGE/in.jpg" || true) ;;
      esac
      tv "rm -f $STAGE/in.jpg" > /dev/null || true
      case "$out" in saved*) ;; *) echo "${out:-error: no answer from the TV}"; echo "(photos kept so far: see 'photos.sh list')"; exit 1 ;; esac
      echo "added $f"
    done
    tv "rmdir $STAGE" > /dev/null 2>&1 || true
    send "--es op list" ;;
  show)
    n=$(count)
    [ "$n" -gt 0 ] || { echo "error: no photos on the TV yet (photos.sh add FILE...)"; exit 1; }
    was=$(send "--es op list" | sed -n 's/^mode: \([a-z]*\).*/\1/p')
    [ -n "$was" ] || { echo "!! photos.sh: the TV did not say its photo mode, so nothing was changed. Run the same command again." >&2; exit 1; }
    if [ "$was" != only ]; then   # only theirs rotate, so what is on screen is one of them; the owner's mode comes back after
      trap 'send "--es op mode --es mode ${was:-mix}" > /dev/null || true; cleanup' EXIT
      send "--es op mode --es mode only" > /dev/null
    fi
    SHOT_WAIT=70 optimize/shot.sh screensaver own-photo ;;
  *) echo "usage:"; sed -n '3,13p' "$0"; exit 1 ;;
esac
