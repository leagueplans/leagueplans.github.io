#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
# Pushes updated versions of main.js and index.html to
# the docs folder, for use as github-pages.

sbt ui/fullOptJS/webpack

if [ ! -d docs ]; then
  mkdir --verbose --parents docs
fi
cp --verbose ui/index-dev.html docs/index.html
mv --verbose ui/target/scala-3.0.0/scalajs-bundler/main/ui-opt.js docs/main.js
sed --in-place 's/src=".*"/src="main\.js"/g' docs/index.html

git config --local user.email '41898282+github-actions[bot]@users.noreply.github.com'
git config --local user.name 'github-actions[bot]'
git add docs
git commit --message='Publish github-pages'
git push
