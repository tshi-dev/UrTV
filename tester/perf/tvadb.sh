#!/usr/bin/env bash
# An adb-shaped front for the round's TV command, so tester/perf/switch.sh (which calls «$ADB shell …» and «$ADB logcat …») runs through the same TV the round
# talks to (the real TV's tv.py, the CI emulator's shim, a test's stand-in): the arguments after an optional «shell» are one command line for $ROUND_TV.
[ "${1:-}" = shell ] && shift
exec ${ROUND_TV:?tvadb.sh: ROUND_TV is the TV command of the round} "$*"
