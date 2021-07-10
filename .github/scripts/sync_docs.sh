#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
# Update the docs directory to reflect the state of the resources directory

readonly DOCS='docs'
readonly RESOURCES='ui/src/main/resources'
readonly INDEX_DEV='index-dev.html'
readonly INDEX='index.html'

# Rename index-dev.html and update it to use the fully optimised JS
mv --verbose "${RESOURCES}/${INDEX_DEV}" "${RESOURCES}/${INDEX}"
sed --in-place 's/src=".*"/src="main\.js"/g' "${RESOURCES}/${INDEX}"

# Copy all files in the resources directory over to the docs directory.
# We exclude main.js such that rsync doesn't delete that file in the
# docs directory. Other scripts are responsible for ensuring that the
# main.js file is up to date.
if [[ ! -d "${DOCS}" ]]; then
  mkdir --verbose --parents "${DOCS}"
fi
rsync --verbose \
      --recursive \
      --inplace \
      --times \
      --delete \
      --exclude='main.js' \
      "${RESOURCES}/" "${DOCS}"
