# Sillon

**Français** · [English](README.en.md)

[![Build](https://github.com/Relthdrarxash/sillon/actions/workflows/android.yml/badge.svg)](https://github.com/Relthdrarxash/sillon/actions/workflows/android.yml)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](LICENSE)
[![Offline](https://img.shields.io/badge/Network-100%25%20Offline-orange.svg)](app/src/main/AndroidManifest.xml)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-brightgreen.svg)](app/build.gradle.kts)

Application Android de **potager vivant & calendrier agronomique**, 100 % hors-ligne, libre, gratuite, sans publicité, sans inscription et sans la moindre collecte de données. Fait partie de la suite d'applications d'intérêt général **Pilmil Studio** (avec *Sauve*, *Sommelle*, *Lunaison* et *Respire*).

Concevoir un potager nourricier et résilient ne s'improvise pas : les calendriers par mois fixes ignorent les disparités climatiques locales, les associations empiriques colportent des mythes non prouvés et les applications commerciales exigent des comptes en ligne, des abonnements ou l'envoi de photos sur des serveurs distants.

**Sillon** remet l'agronomie rationnelle et la biologie végétale au centre du jardin, directement dans votre poche, même au fond du potager sans réseau : calcul dynamique des périodes culturales indexées sur la date médiane de dernière gelée ($J_{\text{gel}}$), détection spatiale des synergies et antagonismes allélopathiques, respect du cycle de rotation des cultures sur 4 ans et diagnostic foliaire embarqué 100 % local.

---

## Captures d'écran

<p align="center"><img src="docs/captures/s1.webp" width="150" alt="Travaux de la quinzaine"> <img src="docs/captures/s2.webp" width="150" alt="Plan du potager"> <img src="docs/captures/s3.webp" width="150" alt="Encyclopédie des plantes"> <img src="docs/captures/s4.webp" width="150" alt="Fiche d'une plante"> <img src="docs/captures/s5.webp" width="150" alt="Docteur du potager"> <img src="docs/captures/s6.webp" width="150" alt="Réglages"></p>

## Ce que fait l'application

### 1. Calendrier agronomique dynamique indexé sur les gelées ($J_{\text{gel}}$)
- **Fini les calendriers rigides :** Les dates fixes (« semer les tomates en mars ») sont inadaptées aux écarts thermiques régionaux. Sillon asservit toutes les fenêtres de culture à la **date médiane des dernières gelées printanières** ($J_{\text{gel}}$), garantissant une cohérence thermique réelle.
- **101 départements français pré-configurés :** Base climatique intégrée (`FrenchFrostCatalog`) issue des normales trentenaires, personnalisable au jour près pour épouser votre microclimat d'ubac, de versant ou d'altitude.
- **Guidage hebdomadaire bienveillant :** Écran « Aujourd'hui » affichant instantanément les semis sous abri/au chaud, les semis directs en pleine terre, les repiquages et les récoltes actifs pour la quinzaine, avec tolérance phénologique ($\pm 7$ jours).

### 2. Planificateur visuel de parcelles & carrés vivants
- **Conception modulaire :** Créez vos carrés potagers surélevés (3x3, 4x4) ou planches de culture maraîchères en lignes.
- **Densités adaptées :** Calcul automatique des densités au pied carré (*Square Foot Gardening* : 1, 4, 9 ou 16 plants/case) ou des écartements maraîchers traditionnels en centimètres.
- **Visualisation d'occupation :** État d'avancement des cultures, jours écoulés depuis le repiquage et projection du créneau de récolte.

### 3. Moteur d'allélopathie & compagnonnage végétal (`CompanionEngine`)
- **62 légumes, herbes aromatiques et fleurs compagnes** minutieusement modélisés (`VegetableCatalog`) avec leur famille botanique, besoins hydriques, exposition solaire et seuils thermiques de germination.
- **Graphe relationnel spatial en 2D :** Analyse automatique des cases adjacentes (contiguïté orthogonale et diagonale par distance de Tchebychev).
- **Synergies documentées :** Détection de la confusion olfactive Alliacées/Apiacées (*Psila rosae* et *Acrolepiopsis assectella*), des effets nématicides racinaires de l'alpha-terthiényle des *Tagetes*, et de l'optimisation racinaire stratifiée.
- **Alertes biologiques d'antagonisme :** Signalement immédiat des incompatibilités majeures (vecteurs épidémiques croisés comme le mildiou entre tomate et pomme de terre, allélopathie inhibitrice du fenouil ou du noyer).

### 4. Suivi pluriannuel des rotations culturales (`RotationEngine`)
- **Cycle agro-écologique sur 4 ans :**
  1. *Année 1 :* Cultures gourmandes très exigeantes (Solanacées, Cucurbitacées, Brassicacées) sur sol fraîchement amendé.
  2. *Année 2 :* Cultures moyennement exigeantes et racines pivotantes (Apiacées, Alliacées, Chénopodiacées).
  3. *Année 3 :* Légumineuses améliorantes fixatrices d'azote atmosphérique par symbiose rhizobienne (Fabacées).
  4. *Année 4 :* Couverts vivants, engrais verts régénérateurs (phacélie, moutarde, trèfle) et repos structuré du sol.
- **Prévention sanitaire tellurique :** Détection des récurrences d'une même famille sur un délai inférieur à la pause requise (3 à 5 ans) pour briser le cycle des sclérotes, nématodes et champignons terricoles (*Plasmodiophora*, *Pythium*, *Rhizoctonia*).

### 5. Guide de santé végétale & Edge Vision 100 % hors-ligne (`DiagnosticEngine`)
- **Diagnostic différentiel dichotomique :** Analyse pas-à-pas des symptômes selon les organes touchés (feuille, tige, collet, fruit, racine) sur 14 pathologies courantes.
- **Diagnostic photo sur l'appareil (en préparation) :** un petit modèle (MobileNetV3, environ 1 Mo, LiteRT) reconnaîtra sur le téléphone le mildiou, l'oïdium et l'alternariose, et répondra « je ne sais pas » sous un seuil de confiance calibré sur des photos de terrain. La fonction n'apparaît dans l'appli que lorsqu'un modèle entraîné est embarqué ; le pipeline d'entraînement est dans [`tools/ml`](tools/ml/README.md). Aucune image ne transite sur internet.
- **Pharmacopée biologique certifiée :** Préconisations strictes en biocontrôle respectant le cahier des charges de l'Agriculture Biologique (décoction de prêle pour sa silice renforçant la cuticule, bicarbonate de potassium à action osmotique contre l'oïdium, cuivre raisonné plafonné à moins de 4 g/m²/an).

---

## Les Engagements Éthiques et Techniques Pilmil Studio

Comme l'ensemble des créations de **Pilmil Studio**, Sillon est conçu selon une charte d'éthique radicale :

- **Zéro permission INTERNET :** Aucune autorisation réseau n'est déclarée dans le manifeste (`AndroidManifest.xml`). L'application est structurellement incapable d'émettre ou de recevoir le moindre paquet sur le réseau.
- **0 compte, 0 mot de passe, 0 serveur :** Aucune authentification requise, aucun écran d'onboarding verbeux, aucun profilage utilisateur.
- **0 traceur, 0 publicité, 0 télémétrie :** Aucun kit d'analyse (Google Analytics, Firebase, etc.) n'est embarqué dans le code binaire.
- **Données souveraines & formats ouverts :** Votre potager vous appartient. Les données sont enregistrées localement dans un fichier JSON clair et accessible (`sillon_garden.json`).
- **Sauvegarde & Restauration 1-tap :** Exportation et réimportation instantanées en JSON et au format tableur CSV (compatible LibreOffice Calc et Microsoft Excel) via le Storage Access Framework (SAF) d'Android.
- **Pas de sauvegarde dans le nuage :** les données sont exclues de la sauvegarde Android vers Google. Vous choisissez : transfert direct vers un nouveau téléphone lors de sa configuration (Android 12 ou plus), ou export JSON et CSV enregistré où vous voulez. Ces fichiers ne sont pas chiffrés.
- **Bien commun libre :** Publié sous licence [Mozilla Public License 2.0](LICENSE) (MPL-2.0).

---

## Architecture Logicielle

Le projet est conçu selon les standards modernes d'ingénierie logicielle Android :

```
fr.pilmil.sillon
├── data/
│   ├── DiseaseCatalog.kt       # Base des 14 maladies et ravageurs majeurs
│   ├── FrenchFrostCatalog.kt   # Normales climatiques des 101 départements français
│   ├── GardenRepository.kt     # Persistance locale JSON atomique & exports SAF/CSV
│   └── VegetableCatalog.kt      # Référentiel des 62 espèces maraîchères et compagnes
├── engine/
│   ├── CompanionEngine.kt      # Graphe relationnel & calcul spatial d'allélopathie
│   ├── DiagnosticEngine.kt     # Moteur d'inférence de diagnostic différentiel
│   ├── EdgeVisionClassifier.kt # Vision sur appareil : indices chromatiques agronomiques
│   ├── FrostEngine.kt          # Phénologie dynamique indexée sur J_gel (Degrés Jours)
│   └── RotationEngine.kt       # Gestion de la fatigue des sols & cycle quadriennal
├── model/
│   ├── BotanicalFamily.kt      # Familles botaniques, besoins trophiques et pauses
│   ├── CompanionRelation.kt    # Typologie des synergies et risques biologiques
│   ├── Disease.kt              # Modélisation des pathologies et remèdes bio
│   ├── GardenModels.kt         # Modèles de parcelles, slots et historiques
│   └── Vegetable.kt            # Fiche technique agronomique d'une espèce
└── ui/
    ├── catalog/CatalogScreen.kt# Exploration du catalogue & fiches détaillées
    ├── dashboard/DashboardScreen.kt # Tableau de bord hebdomadaire des travaux
    ├── doctor/DoctorScreen.kt  # Diagnostic phytosanitaire & Edge Vision
    ├── garden/GardenScreen.kt  # Grille interactive des parcelles & alertes
    ├── settings/SettingsScreen.kt # Calage climatique & exports JSON/CSV
    └── theme/                  # Material 3 aux tonalités telluriques naturelles
```

### Spécifications techniques
- **Langage :** Kotlin 2.0+ (100% Kotlin).
- **Interface :** Jetpack Compose & Material 3 (Material You avec support natif du thème sombre).
- **Typographie :** *Fraunces* (titrages agronomiques chaleureux) et *Nunito* (lisibilité optimale en plein soleil).
- **Cible :** Android 15 / 14 (compileSdk 36, targetSdk 36, minSdk 26).
- **Moteurs purs sans dépendance Android :** Les moteurs de calcul agronomique sont découplés du framework Android pour garantir une testabilité unitaire totale et des performances instantanées.

---

## Compilation et Tests

Le projet se compile directement avec Gradle et le JDK 17 standard.

### Exécuter la suite de tests unitaires
Les tests automatisés vérifient l'intégralité des moteurs agronomiques (phénologie, matrices de compagnonnage, règles de rotation, vision chromatique) :

```bash
./gradlew testDebugUnitTest
```

### Générer l'APK de développement
Pour compiler et empaqueter l'application Android :

```bash
./gradlew assembleDebug
```
L'exécutable généré sera disponible dans `app/build/outputs/apk/debug/app-debug.apk`.

---

## Fondements Scientifiques

Pour consulter la revue bibliographique complète des mécanismes allélopathiques (sécrétions d'alpha-terthiényle, confusion olfactive par COV, biochimie de la nitrogénase chez les Fabacées, toxicité de la juglone), l'explication mathématique des Degrés Jours de Croissance ($DJC$) et les protocoles de biocontrôle (silice de prêle, bicarbonate de potassium), référez-vous au dossier scientifique :

**[Consulter les Fondements Scientifiques de Sillon (docs/scientific-foundations.md)](docs/scientific-foundations.md)**

---

## La suite Pilmil Studio

Sillon s'inscrit dans la démarche des applications d'intérêt général de **Pilmil Studio** :

- **[Sauve](https://github.com/Relthdrarxash/sauve) :** Premiers secours d'urgence « Anti-Panique » 100 % hors-ligne guidés par la voix.
- **[Sommelle](https://github.com/Relthdrarxash/sommelle) :** Accompagnement de l'insomnie par la TCC-I sans compte ni publicité.
- **[Lunaison](https://github.com/Relthdrarxash/lunaison) :** Suivi intime du cycle menstruel respectueux de la vie privée.
- **[Respire](https://github.com/Relthdrarxash/respire) :** Déconnexion numérique et micro-pauses respiratoires sereines.

---

## Licence

Sillon est un logiciel libre distribué sous licence **[Mozilla Public License 2.0](LICENSE)** (MPL-2.0) : vous pouvez l'utiliser, l'étudier, le modifier et le redistribuer ; les fichiers que vous modifiez doivent rester publiés sous la même licence, avec leur code source.

---

## Contact & Contribution

Questions agronomiques, retours de terrain et contributions :  
**Pilmil Studio** : [pilmil.studio@proton.me](mailto:pilmil.studio@proton.me)
