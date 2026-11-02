# Sourced by install.sh, run.sh and restore-home.sh (needs PY). The journal of a home switch: optimize/run/home-switch.json.
# install.sh writes it before it changes anything on the TV (the stock home, the setup wizard and the HOME role holder as they
# were), so a restart of the TV in the middle of the switch (it kills the safety switch with the shell) can be undone from here.
#   state: switching (changes may be on the TV) · done (the new home was confirmed) · restored · aborted (nothing was changed)
#   step (the last field, after the fields the app's SetupEngine writes too): the step of the switch that is about to act, written BEFORE it acts (armed, wizard_off, wifi_check, home_start,
#         stock_off, switch, verify): after a cut-off the journal says how far the switch got
TV_TMP=/data/local/tmp
jfile() { echo "${URTV_RUN_DIR:-optimize/run}/home-switch.json"; }
jget() { [ -f "$(jfile)" ] || return 0; sed -n "s/^  \"$1\": \"\(.*\)\",*\$/\1/p" "$(jfile)" | head -1; }
jwrite() {   # nonce state stock role_holder_before wizard_before stock_before
  local f; f=$(jfile); mkdir -p "$(dirname "$f")"
  printf '{\n  "nonce": "%s",\n  "state": "%s",\n  "stock": "%s",\n  "role_holder_before": "%s",\n  "wizard_before": "%s",\n  "stock_before": "%s",\n  "safety_switch": "armed",\n  "written": "%s",\n  "step": "armed"\n}\n' \
    "$1" "$2" "$3" "$4" "$5" "$6" "$(date '+%Y-%m-%d %H:%M:%S')" > "$f.tmp" && mv "$f.tmp" "$f"
}
jstate() {   # new state, if there is a journal
  local f; f=$(jfile); [ -f "$f" ] || return 0
  sed "s/^  \"state\": \".*\",/  \"state\": \"$1\",/" "$f" > "$f.tmp" && mv "$f.tmp" "$f"
}

jstep() {   # the step about to act (see «step» above); called before the step changes anything on the TV
  local f; f=$(jfile); [ -f "$f" ] || return 0
  sed "s/^  \"step\": \"[^\"]*\"/  \"step\": \"$1\"/" "$f" > "$f.tmp" && mv "$f.tmp" "$f"   # (a journal of an older version, or the app's, has no step: left as it is)
}

jsafety() {   # armed | unavailable: whether a safety switch could be kept running on the TV (some TVs end background jobs with the ADB session)
  local f; f=$(jfile); [ -f "$f" ] || return 0
  sed "s/^  \"safety_switch\": \".*\",/  \"safety_switch\": \"$1\",/" "$f" > "$f.tmp" && mv "$f.tmp" "$f"
}

# Put the home back as the journal says. $1 full: also the stock home's helpers (restore-home.sh); anything else: only what
# the switch itself changed. The setup wizard is only ever turned ON here, never off.
jrestore() {
  local stock pkg holder nonce cmds=() l
  stock=$(jget stock); nonce=$(jget nonce); holder=$(jget role_holder_before); pkg=${stock%%/*}
  [ -n "$stock" ] || { echo "!! The journal $(jfile) has no stock home in it: use optimize/restore.sh"; return 1; }
  { [ -n "$holder" ] && [ "$holder" != sa.urtv.home ]; } || holder=$pkg
  cmds=("pm enable $pkg" "pm enable com.google.android.tungsten.setupwraith")
  if [ "${1:-}" = full ]; then
    while read -r l; do cmds+=("pm enable $l"); done < <(grep -vE '^[[:space:]]*(#|$)' optimize/lists/home-replaced.txt | awk '{print $1}')
  fi
  [ "$holder" = "$pkg" ] || cmds+=("pm enable $holder")   # another launcher that was home before
  cmds+=("cmd role add-role-holder --user 0 android.app.role.HOME $holder")
  [ "$holder" != "$pkg" ] || cmds+=("cmd package set-home-activity --user 0 $stock")
  [ -z "$nonce" ] || cmds+=("touch $TV_TMP/home_ok.$nonce")   # a safety switch still running stops
  cmds+=("input keyevent KEYCODE_HOME")
  printf '%s\n' "${cmds[@]}" | "$PY" -I optimize/tv.py --batch > /dev/null || return 1
  jstate restored
}

# At the start of install.sh and run.sh: a switch that was cut off (the TV or this computer restarted in the middle) is undone
# from the journal, but only when Home is not ours and the switch never confirmed itself.
jrecover() {
  local st res nonce
  st=$(jget state)
  case "$st" in switching) ;; *) return 0 ;; esac
  nonce=$(jget nonce)
  res=$("$PY" -I optimize/tv.py "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" 2> /dev/null) \
    || { echo "(An earlier home switch was cut off, but the TV did not answer: it is checked on the next run.)"; return 0; }
  if grep -q sa.urtv.home <<< "$res"; then return 0; fi   # ours is home: the install goes on and checks it
  if "$PY" -I optimize/tv.py "[ -f $TV_TMP/home_ok.$nonce ] && echo MARKER" 2> /dev/null | grep -q MARKER; then
    jstate restored; return 0   # it was confirmed or put back by the switch itself
  fi
  echo "An earlier home switch was cut off (the TV or this computer restarted) and Home is not ours: putting the home back from optimize/run/home-switch.json."
  if jrestore; then echo "Stock home restored. The home switch starts again if it is a step of this run."
  else echo "!! Could not restore the home from the journal (the TV did not answer). Run optimize/restore-home.sh when it is on and on Wi-Fi."; return 1; fi
}
