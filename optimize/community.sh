#!/usr/bin/env bash
# For a TV our own lists do not know: the community list for exactly this model, if there is one (optimize/lists/community/, from
# CleanYourTV's tested lists, MIT; optimize/community.py decides which, conservatively, and never a list for another model).
# Applied: its packages minus everything in keep.txt and core.txt, minus launchers and setup wizards (the home switch is ours), minus what UAD-ng
# rates Unsafe, minus what the person kept (keep.sh restore). Recorded in optimize/run/community-applied.txt: the report shows them,
# keep.sh off lists them by name, reset.sh turns them back on.
#   optimize/community.sh
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
RUN="${URTV_RUN_DIR:-optimize/run}"
LISTS=optimize/lists
mkdir -p "$RUN"
tv() { "$PY" -I optimize/tv.py "$@"; }
applied="$RUN/community-applied.txt"; name="$RUN/community-name.txt"
: > "$applied"; rm -f "$name"

installed=$(tv "pm list packages" | tr -d '\r')
disabled=$(tv "pm list packages -d" | tr -d '\r')
# our lists know a TV when several of their TCL packages are on it (they are TCL lists; the Google ones are on every Android TV)
mine=0
for p in $(grep -vE '^[[:space:]]*(#|$)' "$LISTS/tcl-safe.txt" "$LISTS/tcl-optional.txt" | awk '{print $1}' | sed 's/^[^:]*://' | grep '^com\.tcl\.' | sort -u); do
  ! grep -qx "package:$p" <<< "$installed" || mine=$((mine + 1))
done
if [ "$mine" -ge 3 ]; then echo "   (our own lists know this TV: no community list)"; exit 0; fi

chosen=$("$PY" -I optimize/community.py match < "$RUN/tv.txt" 2> "$RUN/community-why.txt" || true)
if [ -z "$chosen" ]; then
  echo "   (unknown model: no optional list. $(cat "$RUN/community-why.txt"))"
  exit 0
fi
tmp=$(mktemp -d)
printf '%s\n' "$installed" > "$tmp/installed"
# the apps that answer HOME on this TV are never disabled here
tv "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2> /dev/null" | tr -d '\r' \
  | sed -n 's#^[[:space:]]*\([A-Za-z0-9_.]*\)/.*#\1#p' | sort -u > "$tmp/homes"
if [ ! -s "$tmp/homes" ]; then   # every TV has a home app: an empty answer is «the TV did not say», and the home must not be guessed by its name
  echo "!! community list not applied: the TV did not say which app is its home screen, so nothing was turned off. Run optimize/community.sh again."
  rm -rf "$tmp"; exit 1
fi
kept=$("$PY" -I optimize/offlist.py kept) || exit 1   # an unreadable kept.txt stops the step
grep -vE '^[[:space:]]*(#|$)' "$LISTS/keep.txt" | awk '{print $1}' > "$tmp/keep"
printf '%s\n' "$kept" >> "$tmp/keep"
"$PY" -I optimize/community.py packages "$chosen" --keep "$tmp/keep" --homes "$tmp/homes" --installed "$tmp/installed" > "$tmp/pkgs"
# never Android's own core, whatever the list says (optimize/lists/core.txt; disable.sh would refuse the whole list)
grep -vEf <(grep -vE '^[[:space:]]*(#|$)' "$LISTS/core.txt" | sed 's/.*/^(&)$/') "$tmp/pkgs" > "$tmp/pkgs2" || true; mv "$tmp/pkgs2" "$tmp/pkgs"
# what owners often want (an update service, voice, MiLink, a video player): left on, and offered to the person instead
rm -f "$RUN/community-ask.txt"
if grep -qE '^[^#[:space:]]' "$LISTS/community-ask.txt" 2> /dev/null; then
  grep -E -f <(grep -E '^[^#[:space:]]' "$LISTS/community-ask.txt") "$tmp/pkgs" > "$RUN/community-ask.txt" || true
  if [ -s "$RUN/community-ask.txt" ]; then
    grep -vxFf "$RUN/community-ask.txt" "$tmp/pkgs" > "$tmp/pkgs2" || true; mv "$tmp/pkgs2" "$tmp/pkgs"
    echo "   left on until the person says (ask in one Arabic line; on a yes add them to optimize/lists/mine.txt): $(paste -sd' ' "$RUN/community-ask.txt")"
  else rm -f "$RUN/community-ask.txt"; fi
fi
# already off before we came: not ours (reset.sh would turn it on)
grep -vxFf <(sed 's/^package://' <<< "$disabled") "$tmp/pkgs" > "$tmp/todo" || true
if [ -s "$tmp/todo" ]; then   # never what UAD-ng rates Unsafe; offline (the check cannot run) NOTHING from the community list is applied
  if rated=$("$PY" -I optimize/check-packages.py "$tmp/todo" 2> /dev/null); then
    unsafe=$(awk '$1 == "Unsafe" {print $2}' <<< "$rated")
    if [ -n "$unsafe" ]; then
      echo "   left on, rated Unsafe: $(paste -sd' ' - <<< "$unsafe")"
      grep -vxFf <(printf '%s\n' "$unsafe") "$tmp/todo" > "$tmp/todo2" || true; mv "$tmp/todo2" "$tmp/todo"
    fi
  else
    # the third-party list is the least vetted: without the one safety rating it is not applied at all
    echo "!! community list not applied: the Unsafe check (UAD-ng) needs the internet and could not run. Nothing from the community list was turned off; run optimize/community.sh again when online."
    rm -rf "$tmp"; exit 0   # a !! line, not a stop of the whole setup: nothing was applied (community-applied.txt stays empty)
  fi
fi
base=$(basename "$chosen" .txt)
if [ ! -s "$tmp/todo" ]; then echo "   community list $base: nothing on this TV to turn off"; echo "$base" > "$name"; exit 0; fi
{ echo "# applied from community/$base.txt"; cat "$tmp/todo"; } > "$RUN/community-list.txt"
optimize/disable.sh "$RUN/community-list.txt" > /dev/null
cp "$tmp/todo" "$applied"
echo "$base" > "$name"
echo "   community list $base (CleanYourTV, MIT): $(wc -l < "$applied" | tr -d ' ') packages turned off"
rm -rf "$tmp"
