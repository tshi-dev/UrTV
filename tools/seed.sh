#!/usr/bin/env bash
# The datasets as they are on tshi-dev/UrTV's old data branches, published unchanged in one commit (seed.yml):  tools/seed.sh leagues archive ...
set -euo pipefail
declare -A SRC=([leagues]="data-leagues leagues football/leagues" [archive]="data-archive . football/archive" [catalogue]="data-catalogue catalogue football/catalogue"
                [games]="data-games . games" [markets]="data-markets markets markets" [stations]="data-stations stations stations"
                [photos]="data-photo-packs packs-built photos")
puts=()
for s in "$@"; do
  read -r branch dir dest <<<"${SRC[$s]:?unknown dataset $s}"
  mkdir -p "seed/$s"
  git fetch -q --depth=1 "${APP_REPO:-https://github.com/tshi-dev/UrTV}" "$branch"
  git archive FETCH_HEAD "$dir" | tar -x -C "seed/$s"
  src="seed/$s/$dir"
  if [ "$s" = games ]; then mkdir -p seed/games-pub && cp -r "$src/games/." seed/games-pub/ && cp -r "$src/quiz" seed/games-pub/; src=seed/games-pub; fi
  if [ "$s" = photos ]; then find "$src" -type f ! -name '*.json' -delete; fi   # metadata only: the pictures stay as UrTV's releases
  puts+=(--put "$src:$dest")
done
python3 tools/publish.py "${puts[@]}" -m "Seed from UrTV: $*"
