#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
# Takes a name for a scraper, runs it, and then syncs the the UI resources with the produced files

readonly SCRAPER=$1
readonly USER_AGENT='OSRS planner CI (+https://github.com/DanielMoss/osrs-planner)'
readonly TMP='tmp'
readonly RESOURCES='ui/src/main/resources'

if [[ ! -d "${RESOURCES}" ]]; then
  mkdir --verbose --parents "${RESOURCES}"
fi

run () {
  local EXTRA_ARGS=$1
  sbt "wikiScraper/run \"scraper=${SCRAPER}\" \"user-agent=${USER_AGENT}\" \"target-directory=${TMP}\" ${EXTRA_ARGS}"
}

sync () {
  local INCLUDES=$1

  echo -vv \
       --archive \
       "${INCLUDES}" '--exclude="*"' \
       --delete \
       "${TMP}/dump/" "${RESOURCES}/"


  rsync -vv \
        --archive \
        "${INCLUDES}" '--exclude="*"' \
        --delete \
        "${TMP}/dump/" "${RESOURCES}/"
}

case "${SCRAPER}" in
  "items")
    run "\"id-map=scraper/src/main/resources/id-map.json\""
    sync '--include="data/items.json" --include="images/items"'
    ;;

  "skill-icons")
    run ""
    sync '--include="/images/" --include="/images/skill-icons/***"'
    ;;
esac
