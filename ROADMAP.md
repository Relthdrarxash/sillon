# Feuille de route — Sillon

> Dernière mise à jour : novembre 2026. Voir aussi `pilmil-prive/projets/sillon/feuille-de-route.md`.

## 1. Fonctionnalités à venir
- **v1.0 (socle & validation)** :
  - Évaluation du modèle embarqué (14 classes, 1,1 Mo) sur plusieurs centaines de vraies photos de jardin, avec publication honnête du taux de réussite réel.
  - Première release étiquetée sur GitHub et fiche Google Play / F-Droid.
- **v1.5 (mémoire du jardin)** :
  - Historique pluriannuel des cultures par emplacement avec alertes de rotation.
  - Conseils d'arrosage tenant compte du paillage et de la météo saisie (100 % hors-ligne).
  - Intégration de variétés anciennes et semences reproductibles dans le catalogue.
- **v2.0 (horizon 2028, sous réserve de partenariat/financement)** :
  - Détection d'objets en direct caméra (type YOLO nano quantifié TFLite/ONNX) après constitution d'un jeu d'images annotées.

## 2. Aspect visuel & UX
- Amélioration de l'aide au cadrage photo (indication claire netteté / exposition / part de végétal).
- Tests de captures d'écran sur les fiches cultures, le plan du potager et le mode nuit.

## 3. Qualité de code & sécurité
- Sauvegarde Android configurable par application (`-Ppilmil.allowAutoBackup=false` par défaut, avec `data_extraction_rules.xml` et `backup_rules.xml`).
- Exécution systématique de `testDebugUnitTest` et `:app:lintDebug` en CI.
- À faire : ajouter `ktlint` / `detekt` et brancher `pilmil-core` (`BackupVault` `.pilmil`) pour les exports de plans de jardin.

## 4. Feuille de route iOS
- **État actuel** : 100 % Android (`VegetableCatalog` et `DiseaseCatalog` sont déjà en Kotlin pur).
- **Étapes** :
  1. Migrer les catalogues et la logique de calendrier/rotation vers un module KMP `commonMain`.
  2. Porter l'interface en Compose Multiplatform avec `PHPickerViewController` pour les photos.
  3. Publier une v1 iOS sans diagnostic photo, puis étudier le pont Core ML / LiteRT pour une v1.1.
