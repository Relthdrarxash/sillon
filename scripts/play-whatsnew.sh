#!/usr/bin/env bash
# Prépare les notes de version pour Google Play à partir de la fiche fastlane.
# Pour chaque langue : changelogs/<versionCode>.txt s'il existe, sinon changelogs/default.txt.
# Résultat : distribution/whatsnew/whatsnew-<langue>, tronqué à 500 caractères (limite de Google).
# Usage : scripts/play-whatsnew.sh <versionCode>
set -euo pipefail
code="${1:?versionCode manquant}"
out=distribution/whatsnew
rm -rf "$out" && mkdir -p "$out"
for dir in fastlane/metadata/android/*/; do
  locale="$(basename "$dir")"
  src="$dir/changelogs/$code.txt"
  [[ -f "$src" ]] || src="$dir/changelogs/default.txt"
  if [[ -f "$src" ]]; then
    python3 -c 'import sys; print(open(sys.argv[1], encoding="utf-8").read().strip()[:500])' "$src" > "$out/whatsnew-$locale"
    echo "$locale : $(basename "$src")"
  fi
done
