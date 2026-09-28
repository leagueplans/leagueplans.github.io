#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
# Takes a name for a scraper and runs it. Whether the output is synced into the UI depends
# on the scraper: some produce files that can be taken as they are, while item data is
# reviewed first and promoted by the review tool.

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
  # Nothing is synced into the UI here. The dump is reviewed first, and the review tool
  # promotes whatever is accepted.
  "items")
    run "original-items=data/items.json"
    ;;

  "skill-icons")
    run
    sync --include="/dynamic/" --include="/dynamic/assets/" --include="/dynamic/assets/images/" --include="/dynamic/assets/images/skill-icons/***"
    ;;
esac
