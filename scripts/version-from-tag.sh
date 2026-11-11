#!/usr/bin/env bash
# Calcule le numéro de version Android à partir d'une étiquette Git vX.Y.Z.
#   v4.2.0  -> versionName 4.2.0, versionCode 40200
#   v4.2    -> versionName 4.2,   versionCode 40200
# Le code augmente toujours avec la version, ce que Google Play exige.
# Usage : scripts/version-from-tag.sh v4.2.0   (écrit VERSION_NAME et VERSION_CODE sur la sortie)
set -euo pipefail
tag="${1:?étiquette manquante, par exemple v4.2.0}"
name="${tag#v}"
if [[ ! "$name" =~ ^([0-9]+)\.([0-9]+)(\.([0-9]+))?$ ]]; then
  echo "Étiquette « $tag » invalide : attendu vX.Y ou vX.Y.Z" >&2
  exit 1
fi
major="${BASH_REMATCH[1]}"; minor="${BASH_REMATCH[2]}"; patch="${BASH_REMATCH[4]:-0}"
if (( minor > 99 || patch > 99 )); then
  echo "Mineur et correctif doivent rester sous 100 (reçu $name)" >&2
  exit 1
fi
echo "VERSION_NAME=$name"
echo "VERSION_CODE=$(( major * 10000 + minor * 100 + patch ))"
