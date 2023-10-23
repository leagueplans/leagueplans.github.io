#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
# Update the docs directory to reflect the state of the distribution directory

readonly DOCS='docs'
readonly DISTRIBUTION='ui/target/vite'

if [[ ! -d "${DOCS}" ]]; then
  mkdir --verbose --parents "${DOCS}"
fi
rsync --verbose \
      --recursive \
      --inplace \
      --times \
      --delete \
      --exclude 'CNAME' \
      "${DISTRIBUTION}/" "${DOCS}"
