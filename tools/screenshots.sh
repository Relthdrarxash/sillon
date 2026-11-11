#!/usr/bin/env bash
# Régénère les captures d'écran de l'appli (docs/captures/*.png) sans émulateur ni téléphone.
# Paparazzi rend les écrans Compose sur la JVM ; le script lance l'enregistrement puis copie les PNG.
# Usage : ./tools/screenshots.sh
set -euo pipefail

racine="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$racine"

sortie="docs/captures"
# Dossier où le mode « record » de Paparazzi écrit ses images (ignoré par git : seule docs/captures est suivie).
images="app/src/test/snapshots/images"

# Repart d'un dossier d'images propre et force le rendu : une capture périmée ne doit jamais passer pour récente.
rm -rf "$images"
./gradlew :app:cleanTestDebugUnitTest :app:recordPaparazziDebug --no-daemon

mkdir -p "$sortie"

# Paparazzi nomme les fichiers <classe>_<méthode>_<nom du snapshot>.png : on les retrouve par le nom du snapshot.
for nom in accueil accueil_sombre parcelles parcelles_sombre parcelles_perspective parcelles_perspective_sombre parcelles_plat parcelles_tropical accueil_tropical plantes_tropical plantes diagnostic reglages bienvenue; do
  source=$(find "$images" -name "*_${nom}.png" | head -n 1)
  if [ -z "$source" ]; then
    echo "Capture introuvable : ${nom}" >&2
    exit 1
  fi
  cp "$source" "$sortie/${nom}.png"
  echo "OK ${sortie}/${nom}.png"
done
