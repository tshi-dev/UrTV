#!/usr/bin/env bash
# «استرجاع الشاشة الرئيسية»: put the stock home back, from the journal of the home switch (optimize/run/home-switch.json:
# the stock home, the setup wizard and the HOME role holder as they were before optimize/install.sh launcher).
# It needs ADB: the TV on and on Wi-Fi, this computer on the same network. The home app has no restore button: with no home
# there is nothing to press (the safety switch on the TV covers the first 5 minutes of a switch, this covers the rest).
# No journal (a TV switched by an older version): it does what optimize/restore.sh does for the home screen.
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
. optimize/home-journal.sh
echo "استرجاع الشاشة الرئيسية: this needs ADB (the TV on, on Wi-Fi, this computer on the same network). It cannot be done from the TV itself."
# a journal without the stock home in it (cut off while written) cannot restore anything: «the TV did not answer» would be wrong (4.3 S10)
if [ -f "$(jfile)" ] && [ -z "$(jget stock)" ]; then
  echo "The journal $(jfile) has no stock home in it: restoring the way optimize/restore.sh does."
  exec bash optimize/restore.sh
elif [ -f "$(jfile)" ]; then
  jrestore full || { echo "!! The TV did not answer. Check it is on and on Wi-Fi (optimize/find-tv.sh if its address changed), then run the same command again."; exit 1; }
  echo "Restored from the journal. Press Home on the remote. The home switch can be done again with optimize/install.sh launcher."
else
  echo "No journal of a home switch here: restoring the way optimize/restore.sh does."
  exec bash optimize/restore.sh
fi
