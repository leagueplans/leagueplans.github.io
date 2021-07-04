#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
# Updates the docs folder, for use with github-pages

readonly DOCS='docs'
readonly RESOURCES='ui/src/main/resources'
readonly INDEX_DEV='index-dev.html'
readonly INDEX='index.html'

# Set up the resources directory to how we want the docs directory to look
sbt ui/fullOptJS/webpack
mv --verbose ui/target/scala-2.13/scalajs-bundler/main/ui-opt-bundle.js "${RESOURCES}"/main.js
mv --verbose "${RESOURCES}/${INDEX_DEV}" "${RESOURCES}/${INDEX}"
sed --in-place 's/src=".*"/src="main\.js"/g' "${DOCS}/${INDEX}"

# Update the docs directory
if [[ ! -d "${DOCS}" ]]; then
  mkdir --verbose --parents "${DOCS}"
fi
rsync --verbose --recursive --inplace --delete --times "${RESOURCES}/" "${DOCS}"

# Push any changes
git config --local user.email '41898282+github-actions[bot]@users.noreply.github.com'
git config --local user.name 'github-actions[bot]'
git add "${DOCS}"
git commit --message='Publish github-pages'
git push
