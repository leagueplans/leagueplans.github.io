#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
# Takes a name for a scraper, runs it, and then syncs the the UI resources with the produced files

readonly SCRAPER=$1
readonly TMP='tmp'
readonly TARGET='ui/src/main/web'

if [[ ! -d "${TARGET}" ]]; then
  mkdir --verbose --parents "${TARGET}"
fi

run () {
  sbt "wikiScraper/run \"scraper=${SCRAPER}\" \"target-directory=${TMP}\" $*"
}

sync () {
  rsync --archive \
        --delete \
        "$@" \
        --exclude="*" \
        "${TMP}/dump/" "${TARGET}/"
}

case "${SCRAPER}" in
  "items")
    run "id-map=scraper/src/main/resources/id-map.json"
    sync --include="/data/" --include="/data/items.json" \
         --include="/dynamic/" --include="/dynamic/assets/" --include="/dynamic/assets/images/" --include="/dynamic/assets/images/items/***"
    ;;

  "skill-icons")
    run
    sync --include="/dynamic/" --include="/dynamic/assets/" --include="/dynamic/assets/images/" --include="/dynamic/assets/images/skill-icons/***"
    ;;
esac
