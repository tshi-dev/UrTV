#!/usr/bin/env bash
# One scheduled run of "Market prices". GitHub drops and delays cron runs (one in 2 h 48 min was seen in a Tadawul session), so while an exchange is open
# the run stays alive and fetches every LOOP_STEP seconds (300) until two minutes before the next full hour; the next hour's timers (at :00, :20 and :40,
# any one of which is enough) start the next loop. Outside the open hours it fetches once. Each fetch is the same as before: the script keeps what it has
# for a symbol that fails, and one orphan commit replaces the branch data-markets.
set -u
STEP="${LOOP_STEP:-300}"

in_hours() {   # Tadawul 07:00-12:00 UTC Sun-Thu, the US 13:00-21:00 UTC Mon-Fri (the script itself decides what is open inside them)
  local h d
  h=$((10#$(date -u +%H))); d=$(date -u +%w)
  { [ "$d" -le 4 ] && [ "$h" -ge 7 ] && [ "$h" -le 11 ]; } || { [ "$d" -ge 1 ] && [ "$d" -le 5 ] && [ "$h" -ge 13 ] && [ "$h" -le 20 ]; }
}

push() {   # the markets folder as a single parentless commit, force-pushed (the working tree is not touched, so this can run again and again)
  local idx tree commit
  idx="$(mktemp -u)"
  GIT_INDEX_FILE="$idx" git add -f markets
  tree="$(GIT_INDEX_FILE="$idx" git write-tree)"
  commit="$(git commit-tree "$tree" -m "Market prices")"
  git push -f origin "$commit:refs/heads/data-markets"
  rm -f "$idx"
}

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
end=$(( $(date +%s) / 3600 * 3600 + 3600 - 120 ))
first=1
while true; do
  rm -rf prev && mkdir -p prev
  git fetch --depth=1 origin data-markets 2>/dev/null && git archive FETCH_HEAD | tar -x -C prev || true   # the file of the last run, kept for a symbol that fails this time
  out="$(mktemp)"
  if ! GITHUB_OUTPUT="$out" MARKETS_PREV=prev/markets python3 -u .github/tools/market_prices.py; then
    [ "$first" = 1 ] && exit 1   # the first fetch failing is a failed run; a later one is tried again at the next step
  elif grep -q '^changed=true$' "$out"; then
    push
  fi
  first=0
  in_hours || break
  [ "$(date +%s)" -ge $(( end - STEP )) ] && break
  sleep "$STEP"
done
