# Protocole de Rigueur Scientifique & Validation du Modèle Edge ML (Sillon)

> **Document de référence agronomique & méthodologique**  
> *Auteur : Pilmil Studio / Sillon Core Engineering*  
> *Statut : Spécification & Protocole d'audit officiel v1.0*  
> *Licence : Mozilla Public License 2.0 (MPL-2.0)*

---

## 1. Problématique & État de l'Art : Le "Piège PlantVillage"

La quasi-totalité des applications mobiles commerciales (PictureThis, Blossom, PlantNet) ou des projets académiques promettant un "docteur des plantes par intelligence artificielle" souffrent d'une illusion méthodologique majeure : **le biais de laboratoire du jeu de données PlantVillage**.

### 1.1. Anatomie du biais de laboratoire (Hughes & Salathé, 2015)
Le jeu de données *PlantVillage* (~54 000 images) a été constitué en cueillant des feuilles au sécateur et en les photographiant à plat sur des feuilles de papier blanc ou gris, sous un éclairage néon homogène de laboratoire.
Lorsqu'un réseau convolutif (CNN) ou un Vision Transformer (ViT) est entraîné directement sur ce corpus :
1. **Taux de précision apparent en validation (sur papier) : 99.2%**
2. **Taux de précision réel en potager (in-the-wild) : 21% à 34%**

### 1.2. Pourquoi le modèle échoue-t-il dans un vrai potager ?
* **Biais de texture de fond (*Shortcut Learning*)** : Le réseau apprend les micro-artefacts du fond gris de laboratoire et la luminosité des tubes fluorescents plutôt que la morphologie de la nécrose ou du mycélium.
* **Absence d'éléments perturbateurs** : Dans un vrai potager, l'arrière-plan comporte du terreau meuble, du paillage de paille ou chanvre, des adventices (liseron, pissenlit), des tuteurs en bambou, des ficelles de jute et souvent les doigts du jardinier tenant la feuille.
* **Variations d'irradiance solaire extrêmes** : Plein soleil zénithal (sur-exposition, reflets cuticulaires brillants) contre ombre dense du sous-bois foliaire.
* **Flou de bougé et vent** : Les feuilles de potager oscillent sous le vent, générant des artefacts de compression et du flou cinétique.

---

## 2. Le Danger Écologique & Agronomique : Confusion Biotique vs Abiotique

Dans un jardin amateur ou en permaculture, une fausse prédiction de l'IA n'est pas un simple désagrément d'UX : **c'est un risque écologique et cultural**.

```
Exemple critique : La Tomate et le Calcium
┌─────────────────────────────────────────────────────────────┐
│ Symptôme observé : Tache noire nécrotique sous la tomate    │
└──────────────────────────────┬──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
   Diagnostic Erroné (IA naïve)          Diagnostic Réel (Physiologique)
   "Mildiou / Alternariose"              "Nécrose apicale (Cul Noir)"
   Traitement conseillé :                Cause réelle : Carence en Ca++ 
   Pulvérisation de bouillie             induite par arrosage irrégulier.
   bordelaise (cuivre lourd).            Remède : Paillage + régularité 
   Conséquence : Pollution des sols,     d'arrosage. Cuivre totalement 
   blocage de la vie microbienne.        inutile et toxique.
```

Pour cette raison, **Sillon interdit à son modèle de poser un diagnostic à l'aveugle sans garde-fous agronomiques**.

---

## 3. Les 6 Piliers du Protocole d'Entraînement Rigoureux Sillon

Pour garantir un modèle digne d'une publication scientifique et respectueux de la charte Pilmil Studio, le pipeline d'entraînement suit six exigences non-négociables :

### Pilier 1 : Jeu de Données Hybride & Hard Negative Mining
Le corpus d'entraînement ne repose jamais sur un seul jeu de données :
* **PlantDoc (CVPR 2020)** : Images capturées en plein champ avec bruit environnemental réel.
* **FGVC8 / Kaggle Plant Pathology Challenge** : Photographies haute résolution prises in-situ sous différents angles.
* **PlantVillage filtré & segmenté** : Uniquement utilisé pour enrichir la diversité des spores après extraction du masque foliaire et incrustation synthétique sur des fonds réels de potager.
* **20% de Hard Negatives (Classe `hard_negative_garden`)** : Le jeu de données intègre obligatoirement des images de :
  * Terreau noir et terre argileuse,
  * Paillage (paille, foin, broyat de bois),
  * Tuteurs, piquets et étiquettes,
  * Mauvaises herbes et graminées communes,
  * Mains et doigts humains.
  *Sans ces négatifs, un modèle classifie un caillou ou un doigt avec 95% de confiance comme étant du mildiou.*

### Pilier 2 : Partitionnement Spatial & Temporel Strict (GroupKFold)
* **Interdiction formelle du `random_split` classique** : Deux photos prises d'une même feuille à 3 secondes d'intervalle ne doivent **jamais** se retrouver l'une dans le train set et l'autre dans le test set.
* **GroupKFold par Parcelle / Session** : La validation et le test set sont constitués de fermes ou de jardins géographiquement distincts, photographiés à des dates différentes.

### Pilier 3 : Augmentations Photométriques Agressives
Simulations systématiques appliquées lors de l'entraînement (via `Albumentations`) :
* *SunFlare* et *ColorJitter* (variation de saturation $\pm 35\%$, teinte $\pm 10\%$, luminosité $\pm 35\%$).
* *RandomShadow* : Ombres nettes projetées sur la feuille.
* *GaussianBlur* & *MotionBlur* : Dégradation du piqué optique pour émuler les capteurs d'entrée de gamme de smartphones sous Android 8+.
* Gouttelettes d'eau et buée simulées.

### Pilier 4 : Architecture Sobre & Quantification INT8
* **Backbone** : `MobileNetV3-Small` (0.75 depth multiplier) avec résolution d'entrée $224 \times 224 \times 3$.
* **Taille cible** : **$\le 4.2 \text{ Mo}$** en INT8 Post-Training Quantization (PTQ).
* **Dataset de calibration INT8** : 500 images représentatives de vrais jardins (et non du bruit gaussien) afin d'éviter la saturation des tenseurs d'activation sur les couches de convolution pointwise.
* **Empreinte mémoire runtime** : $< 18 \text{ Mo}$ de RAM, inférence $< 45 \text{ ms}$ sur processeur ARM Cortex-A53 (sans nécessiter de NPU dédié).

### Pilier 5 : Calibration Probabiliste (Expected Calibration Error < 0.08)
Un réseau de neurones standard souffre de **surconfiance pathologique** (il prédit 99% alors qu'il a tort).
* Utilisation du **Label Smoothing (0.1)** pendant l'entraînement.
* Calcul systématique de l'**ECE (Expected Calibration Error)** sur le jeu de test holdout.
* Ajustement par **Scaling de Température** ($T$) sur les logits en sortie :
  $$P(y = c | x) = \frac{\exp(z_c / T)}{\sum_k \exp(z_k / T)}$$

### Pilier 6 : Le Garde-Fou Clinique Hybride (Clinical Fallback)
Le modèle de vision n'est qu'un **assistant de premier niveau**, jamais un juge suprême :
* Si la confiance maximale est inférieure au seuil de rigueur (**$\text{Confidence} < 0.65$**) :
  * L'application affiche explicitement : *"Incertitude visuelle : nous ne devinons pas au hasard."*
  * Sillon bascule instantanément sur la **clé dichotomique clinique INRAE** (`DiagnosticEngine`), en posant 2 questions simples au jardinier (ex: *"Le duvet est-il situé au-dessus ou en-dessous du limbe ?"*).
* Si l'image correspond à une anomalie physiologique (ex: cul noir), le moteur oriente vers les facteurs culturaux (arrosage, pH du sol) et **bloque l'affichage de produits fongicides**.

---

## 4. Grille d'Audit et de Vérification Scientifique

Avant toute publication d'une nouvelle version du modèle `.tflite` dans `app/src/main/assets/models/`, le modèle doit valider la check-list suivante :

| Critère d'évaluation | Seuil d'acceptabilité | Méthode de vérification | Statut Sillon |
| :--- | :--- | :--- | :--- |
| **Biais de fond (Background Invariance)** | $\Delta \text{Pred} < 5\%$ sur permutation de fond | Test de découpage de feuille sur 5 fonds neutres/naturels | **Obligatoire** |
| **Précision F1-Score Macro (Holdout par site)** | $\ge 82\%$ sur les 13 pathologies | Évaluation sur parcelles géographiquement disjointes | **Obligatoire** |
| **Rejet Hard Negative (Terre, mains, tuteurs)** | Spécificité $\ge 96\%$ sur classe négative | 1 000 images d'éléments potagers non-malades | **Obligatoire** |
| **Erreur de Calibration (ECE)** | $\text{ECE} \le 0.08$ | 10 bins de confiance sur jeu de test | **Obligatoire** |
| **Taille binaire TFLite INT8** | $\le 5.0 \text{ Mo}$ | Vérification taille du fichier `.tflite` | **3.8 Mo** |
| **Zéro télémétrie réseau** | 0 octet émis | Audit `tcpdump` / absence de permission `INTERNET` | **Garanti par Manifest** |

---

## 5. Synthèse pour l'Utilisateur & Philosophie Pilmil Studio

1. **Intégration prête dans l'application** : Le socle Android de Sillon est nativement conçu pour charger le modèle quantifié `.tflite` localement, avec un fallback robuste sur l'analyse chromatique et l'arbre de décision clinique.
2. **Honnêteté intellectuelle** : Nous refusons de prétendre qu'un modèle peut diagnostiquer avec certitude une carence complexe à partir d'une photo floue prise au crépuscule. Sillon combine la vision locale et l'anamnèse agronomique pour une fiabilité maximale sans jamais envoyer les photos sur des serveurs distants.
