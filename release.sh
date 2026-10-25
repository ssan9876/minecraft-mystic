#!/usr/bin/env bash
# Publishes the version in gradle.properties as a GitHub release, and points the players' mods list at it.
# Then run ./deploy.sh to put it on the server.
#
#   ./release.sh
#
# Players' join scripts read the mods list from the Rings repo (its mods.txt), so this updates the line for this mod
# in the Rings checkout next to this one (MODS_TXT, default ../mc-mod/mods.txt). Commit and push it there afterwards.
set -euo pipefail

cd "$(dirname "$0")"
MODS_TXT="${MODS_TXT:-../mc-mod/mods.txt}"
version="$(sed -n 's/^version=//p' gradle.properties)"
tag="v$version"
jar="build/libs/mystic-$version.jar"

if gh release view "$tag" >/dev/null 2>&1; then
  echo "Release $tag already exists. Bump version in gradle.properties first."; exit 1
fi
[ -z "$(git status --porcelain)" ] || { echo "Commit your changes first."; exit 1; }

git push -q
./gradlew build --console=plain -q
gh release create "$tag" "$jar" --title "Mystic Arts $version" --generate-notes

repo="$(gh repo view --json nameWithOwner -q .nameWithOwner)"
line="mod https://github.com/$repo/releases/download/$tag/mystic-$version.jar"
if grep -q '^mod https://github.com/[^ ]*/mystic-[^ ]*\.jar$' "$MODS_TXT"; then
  sed -i "s|^mod https://github.com/[^ ]*/mystic-[^ ]*\.jar$|$line|" "$MODS_TXT"
else
  echo "$line" >> "$MODS_TXT"
fi
echo "Released $tag and updated $MODS_TXT. Commit and push that, then run ./deploy.sh to install it on the server."
