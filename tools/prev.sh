#!/usr/bin/env bash
# The last published copy of a dataset, for the tools that keep what a failed source did not answer this time:
#   tools/prev.sh <folder on the data branch> <old UrTV branch> <folder on that branch> <out dir>
# From this repository's data branch; while the dataset is not there yet (the first runs), from the old branch of tshi-dev/UrTV, so the
# first run here starts from the same state as the old writer (the match archive's store above all). Never fails: an empty <out dir> at worst.
set -u
dest="$1"; old_branch="$2"; old_dir="$3"; out="$4"
mkdir -p "$out"
tmp="$(mktemp -d)"
if git fetch -q --depth=1 origin "+refs/heads/${DATA_BRANCH:-data}:refs/remotes/origin/${DATA_BRANCH:-data}" 2>/dev/null \
   && git archive "origin/${DATA_BRANCH:-data}" "$dest" 2>/dev/null | tar -x -C "$tmp" 2>/dev/null && [ -d "$tmp/$dest" ]; then
  cp -r "$tmp/$dest/." "$out/"
  echo "prev: $dest from urtv-data@${DATA_BRANCH:-data}"
elif git fetch -q --depth=1 "${APP_REPO:-https://github.com/tshi-dev/UrTV}" "$old_branch" 2>/dev/null \
   && git archive FETCH_HEAD ${old_dir:+"$old_dir"} 2>/dev/null | tar -x -C "$tmp" 2>/dev/null; then
  cp -r "$tmp/${old_dir:-.}/." "$out/"
  echo "prev: $dest from UrTV@$old_branch (not in urtv-data yet)"
else
  echo "prev: $dest: none yet"
fi
rm -rf "$tmp"
