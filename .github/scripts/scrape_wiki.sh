#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
# Takes a name for a scraper and runs it, writing its output to tmp/. What goes into the UI is
# copied there by sync_scrape.sh, in a job of its own: some scrapers produce files that can be
# taken as they are, while item data is reviewed first and promoted by the review tool.

readonly SCRAPER=$1
readonly TMP='tmp'

run () {
  sbt "wikiScraper/run \"scraper=${SCRAPER}\" \"target-directory=${TMP}\" $*"
}

case "${SCRAPER}" in
  "items")
    run "original-items=data/items.json"
    ;;

  "skill-icons")
    run
    ;;
esac
