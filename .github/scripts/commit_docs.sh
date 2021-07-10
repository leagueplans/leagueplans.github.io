#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

readonly DOCS='docs'
git add "${DOCS}"

if [[ `git status "${DOCS}" --porcelain --untracked-files=no` ]]; then
  git config --local user.email '41898282+github-actions[bot]@users.noreply.github.com'
  git config --local user.name 'github-actions[bot]'
  git commit --message='Publish github-pages'
  git push
fi
