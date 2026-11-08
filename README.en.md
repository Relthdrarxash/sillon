# Sillon

[Français](README.md) · **English**

[![Build](https://img.shields.io/badge/Build-Passing-2e7d32.svg)](https://github.com/Relthdrarxash/sillon)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Offline](https://img.shields.io/badge/Network-100%25%20Offline-orange.svg)](app/src/main/AndroidManifest.xml)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-brightgreen.svg)](app/build.gradle.kts)

An Android application for **living vegetable gardens & agronomic planning**, 100% offline, free, open-source, ad-free, with no account, no tracking, and zero internet permission. Part of the **Pilmil Studio** public interest suite (along with *Sauve*, *Sommelle*, *Lunaison*, and *Respire*).

Designing a thriving and resilient food garden should not rely on folklore or arbitrary monthly calendars. Fixed-date schedules ignore regional climates and seasonal shifts, while commercial apps demand intrusive accounts, subscriptions, or upload private garden pictures to remote servers.

**Sillon** brings evidence-based agronomy and plant biology straight into your pocket—operational even in the deepest rural dead zones: dynamic phenological planning indexed against median last frost dates ($J_{\text{frost}}$), spatial 2D allelopathy and companion planting analysis, 4-year crop rotation schedules to preserve soil microbiology, and fully local on-device leaf health diagnostics.

---

## What the App Does

### 1. Dynamic Phenological Calendar Indexed to Last Frost ($J_{\text{frost}}$)
- **Beyond rigid calendar months:** Rigid guidelines like "sow tomatoes in March" fail to account for altitude and microclimates. Sillon shifts every indoor sowing, direct seeding, transplanting, and harvest window relative to the **median date of the last spring frost** ($J_{\text{frost}}$).
- **101 pre-configured French departments:** Integrated 30-year climatological normal database (`FrenchFrostCatalog`), customizable down to the exact day to fit local microclimates (hillside, sheltered valley, urban heat island).
- **Weekly gardening dashboard:** "Today" screen instantly highlights indoor seed starting, direct ground sowing, transplanting, and active harvest windows for the current fortnight ($\pm 7$ day phenological tolerance).

### 2. Visual Garden Bed & Raised Square Planner
- **Modular layouts:** Easily configure raised square beds (3x3, 4x4) or traditional in-ground row plots.
- **Tailored densities:** Automatic calculation of Square Foot Gardening densities (1, 4, 9, or 16 plants per slot) or standard centimeter spacing between plants and rows.
- **Growth tracking:** Visual slot status, days elapsed since transplanting, and projected harvest windows.

### 3. Allelopathy & Companion Planting Engine (`CompanionEngine`)
- **62 modeled vegetable crops, aromatic herbs, and companion flowers** (`VegetableCatalog`) with botanical families, water requirements, sun exposure, and germination temperature thresholds.
- **2D spatial relational graph:** Automated evaluation of neighboring cells (orthogonal and diagonal adjacency using Chebyshev distance).
- **Documented synergies:** Detection of olfactory pest masking between Alliums and Apiaceae (*Psila rosae* vs. *Acrolepiopsis assectella*), nematicidal root exudates ($\alpha$-terthienyl from *Tagetes*), and root depth stratification.
- **Biological antagonism alerts:** Immediate warning for critical conflicts (shared blight vectors between tomatoes and potatoes, allelopathic growth inhibition from fennel or walnut).

### 4. 4-Year Crop Rotation Engine (`RotationEngine`)
- **Agroecological 4-year cycle:**
  1. *Year 1:* Heavy feeders (Solanaceae, Cucurbitaceae, Brassicaceae) on richly amended compost soil.
  2. *Year 2:* Medium feeders & deep taproots (Apiaceae, Alliums, Chenopodiaceae).
  3. *Year 3:* Atmospheric nitrogen-fixing legumes via symbiotic Rhizobium nodulation (Fabaceae).
  4. *Year 4:* Living cover crops, green manures (phacelia, white mustard, clover), and soil rest.
- **Soil-borne pest breaking:** Identifies family replanting violations within a 3- to 5-year exclusion window to break the lifecycle of resting spores, root-knot nematodes, and fungal pathogens (*Plasmodiophora*, *Pythium*, *Rhizoctonia*).

### 5. Plant Health Doctor & 100% Offline Edge Vision (`DiagnosticEngine`)
- **Dichotomous differential diagnosis:** Step-by-step symptomatic triage across affected organs (leaf, stem, fruit, collar, roots) covering 14 common garden diseases and pests.
- **On-device photo diagnosis (in preparation):** a small model (MobileNetV3, about 1 MB, LiteRT) will recognise late blight, powdery mildew and early blight on the phone, and answer "not sure" below a confidence threshold calibrated on field photos. The feature only appears once a trained model is bundled; the training pipeline lives in [`tools/ml`](tools/ml/README.md). No image ever leaves the device.
- **Certified organic remedies:** Strictly organic biocontrol treatments (horsetail silica decoction to strengthen leaf cuticles, potassium bicarbonate for osmotic powdery mildew lysis, and copper strictly capped below 4 g/m²/year).

---

## Ethical & Technical Commitments (Pilmil Studio)

Like all applications created by **Pilmil Studio**, Sillon follows a strict ethical charter:

- **Zero INTERNET permission:** No network permissions declared in `AndroidManifest.xml`. The app is technically and structurally incapable of connecting to the internet.
- **0 accounts, 0 passwords, 0 servers:** No login walls, no onboarding delays, no personal profiling.
- **0 trackers, 0 telemetry, 0 ads:** Completely free of tracking SDKs (no Firebase, no Google Analytics, no third-party ad libraries).
- **Sovereign, open data:** Your garden data belongs exclusively to you. Stored locally in human-readable JSON (`sillon_garden.json`).
- **1-tap backup & restore:** Seamless JSON and CSV spreadsheet exports/imports via Android's native Storage Access Framework (SAF).
- **Free & Open Source:** Published under the [GNU GPL v3.0](LICENSE) license.

---

## Software Architecture

Built using modern, clean Android engineering practices:

```
fr.pilmil.sillon
├── data/
│   ├── DiseaseCatalog.kt       # Catalog of 14 major garden diseases and pests
│   ├── FrenchFrostCatalog.kt   # Climate normals for 101 French departments
│   ├── GardenRepository.kt     # Atomic JSON local storage & SAF/CSV exporters
│   └── VegetableCatalog.kt      # Scientific profiles for 62 garden crops
├── engine/
│   ├── CompanionEngine.kt      # Relational graph & spatial allelopathy evaluator
│   ├── DiagnosticEngine.kt     # Rule-based differential diagnostic engine
│   ├── EdgeVisionClassifier.kt # On-device vision: agronomic chrominance indices
│   ├── FrostEngine.kt          # Dynamic phenology indexed to J_frost (Degree Days)
│   └── RotationEngine.kt       # Soil fatigue prevention & 4-year cycle engine
├── model/
│   ├── BotanicalFamily.kt      # Botanical families, trophic needs & pause periods
│   ├── CompanionRelation.kt    # Taxonomy of biological synergies and risks
│   ├── Disease.kt              # Pathogen definitions & organic treatment guidelines
│   ├── GardenModels.kt         # Parcel, slot, and crop history data structures
│   └── Vegetable.kt            # Agronomic botanical model of a plant
└── ui/
    ├── catalog/CatalogScreen.kt# Crop catalog & technical specification sheets
    ├── dashboard/DashboardScreen.kt # Weekly task overview & calendar windows
    ├── doctor/DoctorScreen.kt  # Symptom triage & Edge Vision screen
    ├── garden/GardenScreen.kt  # Interactive parcel grid & companion overlay
    ├── settings/SettingsScreen.kt # Frost calibration & JSON/CSV import/export
    └── theme/                  # Earthy Material 3 design system
```

### Technical specifications
- **Language:** Kotlin 2.0+ (100% pure Kotlin).
- **User Interface:** Jetpack Compose & Material 3 (Material You with full dark theme support).
- **Typography:** *Fraunces* (warm serif titles) and *Nunito* (sunlight-readable clean sans-serif).
- **Target:** Android 15 / 14 (compileSdk 36, targetSdk 36, minSdk 26 - Android 8.0+).
- **Pure Kotlin engines:** Calculation engines are decoupled from Android platform classes for fast unit testing (< 2s) and deterministic execution.

---

## Building and Testing

Build the project using standard Gradle and JDK 17.

### Run unit tests
Executes tests covering all agronomic engines (phenology, companion matrix, rotation rules, and chrominance classification):

```bash
./gradlew testDebugUnitTest
```

### Build debug APK
Compiles and packages the debug APK:

```bash
./gradlew assembleDebug
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Scientific Foundations

To read the in-depth literature review of allelopathic interactions (alpha-terthienyl exudation, VOC olfactory masking, Rhizobium nitrogenase biochemistry, juglone cytotoxicity), mathematical models of Growing Degree Days (GDD), and organic biocontrol protocols (horsetail silica, potassium bicarbonate), read the scientific dossier:

**[Read the Scientific Foundations of Sillon (docs/scientific-foundations.md)](docs/scientific-foundations.md)**

---

## The Pilmil Studio Suite

Sillon is part of the digital commons initiative by **Pilmil Studio**:

- **[Sauve](https://github.com/Relthdrarxash/sauve):** 100% offline anti-panic first aid with voice and haptic metronome guidance.
- **[Sommelle](https://github.com/Relthdrarxash/sommelle):** Evidence-based CBT-I sleep companion without accounts or ads.
- **[Lunaison](https://github.com/Relthdrarxash/lunaison):** Private, offline menstrual cycle tracking with medical guides.
- **[Respire](https://github.com/Relthdrarxash/respire):** Mindful breathing pauses before reflex app usage.

---

## License

Sillon is free software distributed under the **GNU General Public License v3.0** ([GNU GPLv3](LICENSE)). You are free to inspect, run, modify, and redistribute it, provided that all derivative works remain open source under the same license terms.

---

## Contact & Feedback

Agronomic questions, field feedback, and code contributions:  
**Pilmil Studio** — [pilmil.studio@proton.me](mailto:pilmil.studio@proton.me)
