# Spécification — Modes « Balcon » et « Intérieur »

> Statut : proposition (novembre 2026). Rien n'est implémenté.
> Cible : v1.5 de Sillon (voir `ROADMAP.md`).

## 1. Objectif

Permettre aux personnes sans jardin (appartement, balcon, rebord de fenêtre) d'utiliser Sillon.
Aujourd'hui, le modèle ne connaît que la pleine terre (`ParcelType.RAISED_BED_SQUARES`, `ParcelType.ROW_BED`)
et le calendrier dépend entièrement de la date de dernière gelée (`Garden.frostDate`).

Contraintes inchangées : 100 % hors-ligne, aucune permission réseau, aucune donnée transmise.

## 2. Environnements de culture

| Environnement | Support | Calendrier | Facteur limitant principal |
|---|---|---|---|
| `OUTDOOR` (actuel) | Pleine terre, carrés, planches | Date de dernière gelée | Température |
| `BALCONY` | Pots, jardinières, bacs | Date de dernière gelée | Exposition, vent, arrosage |
| `INDOOR` | Rebord de fenêtre, lampe horticole | Indépendant des gelées (toute l'année) | Lumière |

### 2.1 Balcon / terrasse
- Conserve le calcul basé sur `frostDate`.
- Nouveaux paramètres du jardin : orientation (`N`, `NE`, `E`, `SE`, `S`, `SO`, `O`, `NO`), exposition au vent (faible / moyenne / forte), balcon couvert ou non.
- Rappels d'arrosage plus fréquents qu'en pleine terre ; si le balcon est couvert, la pluie n'est jamais comptée.

### 2.2 Intérieur
- Pas de date de gelée : semis possibles toute l'année pour les cultures compatibles.
- Source de lumière : fenêtre (avec orientation) ou lampe horticole (durée d'éclairage quotidienne saisie par l'utilisateur).
- Catalogue filtré sur les cultures marquées `indoorSuitable`.
- Catégories ciblées : aromatiques, salades à couper, micro-pousses, graines germées, radis.

## 3. Modèle de données

Tous les nouveaux champs ont une valeur par défaut pour que les fichiers de jardin existants restent lisibles par `GardenCodec`.

### 3.1 `GardenModels.kt`
```kotlin
@Serializable
enum class GrowingEnvironment { OUTDOOR, BALCONY, INDOOR }

@Serializable
enum class Orientation { N, NE, E, SE, S, SW, W, NW }

@Serializable
enum class LightSource { WINDOW, GROW_LIGHT }

// Garden : ajouts
val environment: GrowingEnvironment = GrowingEnvironment.OUTDOOR,
val orientation: Orientation? = null,
val windExposure: WindExposure? = null,      // BALCONY uniquement
val covered: Boolean = false,                // BALCONY uniquement
val lightSource: LightSource? = null,        // INDOOR uniquement
val growLightHoursPerDay: Int? = null,       // INDOOR + GROW_LIGHT

// ParcelType : ajouts
CONTAINER("Pot / jardinière"),
WINDOWSILL("Rebord de fenêtre"),
GROW_LIGHT_SHELF("Étagère sous lampe"),

// Parcel : ajout
val containerVolumeLiters: Int? = null,
```

`frostDate` reste obligatoire pour `OUTDOOR` et `BALCONY`. Pour `INDOOR`, deux options à trancher :
rendre le champ nullable (migration de `GardenCodec`), ou le conserver et l'ignorer.

### 3.2 `Vegetable.kt`
```kotlin
val indoorSuitable: Boolean = false,
val minContainerLiters: Int? = null,
val minDirectSunHours: Int? = null,
```

## 4. Moteurs et écrans touchés

| Composant | Modification |
|---|---|
| `DashboardPlanner` | Branche `INDOOR` sans gelée ; semis échelonnés (ex. toutes les 2–3 semaines pour les salades à couper, valeur à sourcer) |
| `SowingReminders` (Android) / `IosSowingReminders` | Rappels indépendants des gelées en mode intérieur |
| `RotationEngine` | Désactivé pour `CONTAINER`, `WINDOWSILL`, `GROW_LIGHT_SHELF` ; remplacé par un rappel de renouvellement du substrat |
| `CompanionEngine` | Conservé pour les jardinières partagées ; sans objet pour un pot individuel |
| `GrowthEngine` | Ajustement éventuel des durées en intérieur (à valider, pas de coefficient inventé) |
| `OnboardingScreen` | Première question : « Où cultivez-vous ? » (jardin / balcon / intérieur) |
| `GardenScreen` | Représentation en pots plutôt qu'en grille pour les nouveaux types |
| `CatalogScreen` | Filtre « Compatible intérieur » et volume de pot minimal sur la fiche |
| `DiagnosticEngine` / `DiseaseCatalog` | Ajout des problèmes typiques de l'intérieur (voir §6) |

## 5. Estimation de la lumière

- **Android** : capteur `Sensor.TYPE_LIGHT` (lux), sans permission. L'utilisateur pose le téléphone à l'emplacement prévu, écran vers la fenêtre ; Sillon classe le résultat (faible / moyenne / forte) et le rapproche des besoins des cultures.
- **iOS** : pas d'accès public au capteur de luminosité ambiante. Alternative : estimation à partir de l'exposition de la caméra (permission caméra déjà demandée pour le diagnostic), ou saisie manuelle.
- Affichage explicite : « estimation indicative, dépend de l'heure et de la météo ». Proposer plusieurs mesures à différents moments de la journée.
- Seuils de classement à sourcer avant implémentation.

## 6. Diagnostic : problèmes typiques de l'intérieur

À vérifier contre le `DiseaseCatalog` actuel et les 14 classes du modèle embarqué :
- Moucherons du terreau (sciarides).
- Cochenilles.
- Tétranyques (araignées rouges), favorisés par l'air sec.
- Pucerons.
- Oïdium.
- Étiolement (manque de lumière) — trouble physiologique, pas un pathogène.
- Excès d'arrosage / pourriture racinaire.

Les troubles physiologiques peuvent passer par le questionnaire de `DiagnosticEngine` sans réentraîner le modèle photo.

## 7. Données agronomiques

Aucune valeur chiffrée (volume de pot, heures de soleil, seuils de lux, intervalles de semis) ne doit entrer dans le catalogue sans source citée.
Pistes de sources à évaluer : fiches techniques d'instituts (INRAE, GNIS / SEMAE), guides de jardinage d'associations reconnues, documentation des semenciers.
Chaque valeur ajoutée dans `VegetableCatalog.kt` indique sa source en commentaire.

## 8. Découpage proposé

1. **Modèle de données** : nouveaux enums et champs avec valeurs par défaut, tests de rétrocompatibilité de `GardenCodec` (`GardenRepositoryTest`).
2. **Mode balcon** : onboarding, type `CONTAINER`, orientation, rappels d'arrosage, désactivation de la rotation.
3. **Mode intérieur** : calendrier sans gelée, filtre catalogue, premières fiches aromatiques et micro-pousses sourcées.
4. **Mesure de lumière** : Android d'abord, puis iOS.
5. **Diagnostic intérieur** : entrées du `DiseaseCatalog` et questionnaire.

## 9. Hors périmètre

- Culture hydroponique automatisée et capteurs connectés (contraire au principe sans réseau).
- Culture de plantes à statut légal restreint (voir discussion du 14 novembre 2026 : risque juridique pour une association française, classification d'âge, politiques des stores).

## 10. Questions ouvertes

- `frostDate` nullable ou ignorée en mode intérieur ?
- Un même utilisateur peut-il avoir plusieurs jardins d'environnements différents (balcon + intérieur) ? Le modèle `Garden` le permet déjà si l'environnement est porté par le jardin.
- Faut-il porter l'environnement au niveau de la parcelle plutôt que du jardin (un balcon avec des pots dehors et un rebord intérieur) ?
