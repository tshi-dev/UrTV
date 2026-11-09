#!/usr/bin/env bash
# How the setup went, for the TV's owner to send to UrTV's author if they want to: the TV model and Android
# version, how long each step took and where it stopped, memory before and after, the protected-features check,
# and the agent's notes. Nothing personal (no city, clubs, apps, names, network addresses or paths).
#   optimize/share.sh --preview "what went wrong, how long it cost, how it was fixed"
#   optimize/share.sh --send
# --preview writes optimize/run/share.md and prints exactly what would be sent; nothing is sent. Show the owner that
# text and ask. --send then sends the file as it is to the author's Google Form (anonymous, no account); without a
# file it says so. If it can't send, a QR code takes it to the phone: docs/share/ (GitHub Pages) shows it with a
# button that sends it. Without either flag it does both in one go (older instructions).
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
[ -x "$PY" ] || PY="$(command -v python3 || command -v python)"   # a problem from a computer without the setup's venv
# the QR code (older setups): installed only into the setup's own venv, never into the computer's Python (4.3 S10)
if ! "$PY" -c "import segno" 2> /dev/null && [ "$PY" = optimize/venv/bin/python ]; then "$PY" -m pip install -q segno || true; fi
"$PY" -I optimize/share.py "$@"
