#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

readonly DOCS='docs'

sbt test ui/fullOptJS/webpack

if [[ ! -d "${DOCS}" ]]; then
  mkdir --verbose --parents "${DOCS}"
fi
mv --verbose ui/target/scala-2.13/scalajs-bundler/main/ui-opt-bundle.js "${DOCS}"/main.js
