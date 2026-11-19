#!/usr/bin/env bash
# Puts CI pictures on the data-screens branch, never on the branch that was pushed, so the working branches and master
# carry no generated pictures. Usage (in a workflow, with contents: write):
#   bash .github/screens/publish_shots.sh <dir with the pictures> <folder on data-screens> "<commit message>"
# The folder is replaced as a whole (the newest run wins); the rest of data-screens (screens.yml's run-*/ and latest/) is kept.
# Links: https://raw.githubusercontent.com/<owner>/<repo>/data-screens/<folder>/<name>.png (the list: <folder>/index.txt).
# No force push: if data-screens moved meanwhile, the commit is rebased and pushed again (3 tries).
set -euo pipefail
src=$1; folder=$2; msg=$3
test -d "$src" || { echo "no pictures in $src: nothing is published"; exit 0; }
src=$(cd "$src" && pwd)   # absolute: the copy below runs inside the clone of data-screens (a relative path pointed nowhere there)
url="https://x-access-token:${GITHUB_TOKEN:?}@github.com/${GITHUB_REPOSITORY:?}.git"
work=$(mktemp -d)
if ! git clone --quiet --depth 20 --branch data-screens "$url" "$work"; then
  git init --quiet "$work"; git -C "$work" checkout --quiet -b data-screens; git -C "$work" remote add origin "$url"
fi
cd "$work"
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
rm -rf "$folder"; mkdir -p "$folder"
cp -r "$src"/. "$folder"/
(cd "$folder" && find . -type f -name '*.png' | sed 's#^\./##' | sort \
  | sed "s#^#https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/data-screens/$folder/#") > "$folder/index.txt"
git add -A
git commit --quiet -m "$msg" || { echo "the pictures did not change"; exit 0; }
for try in 1 2 3; do
  if git push --quiet origin HEAD:data-screens; then
    echo "Pictures: https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/data-screens/$folder/index.txt"; exit 0
  fi
  git pull --quiet --rebase origin data-screens || git pull --quiet --rebase --depth 50 origin data-screens
done
echo "could not push to data-screens"; exit 1
