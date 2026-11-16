#!/usr/bin/env bash
# The app's files the generators read (the leagues the app offers, its teams, tournaments, stations, the page's symbols...), from
# tshi-dev/UrTV at APP_REF (default master), copied to the same paths here, where the tools (copied unchanged to .github/tools) look for
# them. So the data follows the app without a copy of its lists in this repository:  tools/inputs.sh <path>...
set -euo pipefail
ref="${APP_REF:-master}"
tmp="$(mktemp -d)"
git clone -q --depth 1 --filter=blob:none --sparse --branch "$ref" "${APP_REPO:-https://github.com/tshi-dev/UrTV}" "$tmp/app"
git -C "$tmp/app" sparse-checkout set --no-cone "$@"
for p in "$@"; do
  [ -e "$tmp/app/$p" ] || { echo "inputs: $p is not in UrTV@$ref" >&2; exit 1; }
  mkdir -p "$(dirname "$p")"
  cp -r "$tmp/app/$p" "$p"
done
echo "inputs: UrTV@$ref $(git -C "$tmp/app" rev-parse --short HEAD): $*"
rm -rf "$tmp"
