#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
# Takes a name for a scraper and copies what it wrote to tmp/dump into the UI, after checking
# that it wrote only the files it should have. The scrape runs third-party code on pages from the
# wiki, so this runs apart from it, where it can't be tampered with. Item data isn't synced: it's
# reviewed first, and the review tool promotes whatever is accepted.

readonly SCRAPER=$1
readonly DUMP='tmp/dump'
readonly TARGET='ui/src/main/web'

# Fails unless every file in the dump matches the pattern, an extended regular expression for
# its path from the dump, starting with "./"
only () {
  local unexpected
  unexpected="$(cd "${DUMP}" && find . -mindepth 1 -regextype posix-extended ! -type d ! \( -type f -regex "$1" \))"
  if [[ -n "${unexpected}" ]]; then
    echo "::error::The scrape wrote files it shouldn't have"
    echo "${unexpected}"
    exit 1
  fi
}

sync () {
  rsync --archive \
        --delete \
        "$@" \
        --exclude="*" \
        "${DUMP}/" "${TARGET}/"
}

case "${SCRAPER}" in
  "skill-icons")
    only '\./dynamic/assets/images/skill-icons/[A-Za-z0-9_ -]+\.png'
    sync --include="/dynamic/" --include="/dynamic/assets/" --include="/dynamic/assets/images/" --include="/dynamic/assets/images/skill-icons/***"
    ;;

  *)
    echo "::error::Nothing to sync for ${SCRAPER}"
    exit 1
    ;;
esac
