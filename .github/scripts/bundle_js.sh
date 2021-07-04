#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

npm ci
sbt test ui/fullOptJS/webpack
mv --verbose ui/target/scala-2.13/scalajs-bundler/main/ui-opt-bundle.js docs/main.js
