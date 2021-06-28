#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
# Pushes updated versions of main.js and index.html to
# the docs folder, for use as github-pages.

sbt ui/fullLinkJS

if [ ! -d docs ]; then
  mkdir -p docs
fi

cp ui/index-dev.html docs/index.html
mv ui/target/scala-3.0.0/ui-opt/main.js docs/main.js
sed -i 's/(?<=src=").*(?=")/main\.js/g' docs/index.html

git config --local user.email '41898282+github-actions[bot]@users.noreply.github.com'
git config --local user.name 'github-actions[bot]'
git commit -am 'Publish github-pages'
git push
