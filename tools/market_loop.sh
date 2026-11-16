#!/usr/bin/env bash
# One scheduled run of "Markets" (UrTV's .github/tools/market_loop.sh, the same loop): GitHub drops and delays cron runs, so while an exchange is
# open the run stays alive and fetches every LOOP_STEP seconds (300) until two minutes before the next full hour; outside the open hours it
# fetches once. The script keeps what it has for a symbol that fails; tools/publish.py replaces markets/ on the data branch when a price changed.
set -u
STEP="${LOOP_STEP:-300}"

in_hours() {   # Tadawul 07:00-12:00 UTC Sun-Thu, the US 13:00-21:00 UTC Mon-Fri (the script itself decides what is open inside them)
  local h d
  h=$((10#$(date -u +%H))); d=$(date -u +%w)
  { [ "$d" -le 4 ] && [ "$h" -ge 7 ] && [ "$h" -le 11 ]; } || { [ "$d" -ge 1 ] && [ "$d" -le 5 ] && [ "$h" -ge 13 ] && [ "$h" -le 20 ]; }
}

end=$(( $(date +%s) / 3600 * 3600 + 3600 - 120 ))
first=1
while true; do
  rm -rf prev && tools/prev.sh markets data-markets markets prev/markets   # the last file, kept for a symbol that fails this time
  out="$(mktemp)"
  if ! GITHUB_OUTPUT="$out" MARKETS_PREV=prev/markets python3 -u .github/tools/market_prices.py; then
    [ "$first" = 1 ] && exit 1   # the first fetch failing is a failed run; a later one is tried again at the next step
  elif grep -q '^changed=true$' "$out"; then
    python3 tools/publish.py --put markets:markets -m "Market prices"
  fi
  first=0
  in_hours || break
  [ "$(date +%s)" -ge $(( end - STEP )) ] && break
  sleep "$STEP"
done
