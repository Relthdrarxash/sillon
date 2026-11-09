# Diagnostic photo de Sillon : entraînement

Ce dossier produit le modèle embarqué dans l'appli (`app/src/main/assets/models/`) :
`sillon_disease.tflite` (environ 1 Mo) et `sillon_disease.json` (classes, seuil, métriques).
Tant que ces deux fichiers sont absents, l'appli n'affiche pas « Analyser une photo ».

## Le plus simple : Kaggle (gratuit, avec GPU)

Pas de GPU en local : l'entraînement se fait sur Kaggle avec `sillon_kaggle.ipynb`, qui compare trois variantes
en une passe (baseline MobileNetV3-Small 224 px, puis Large 288 px avec la recette améliorée, avec et sans fusion
mildiou/alternariose).

1. **Compte** : sur [kaggle.com](https://www.kaggle.com), vérifiez votre compte **par téléphone** (Settings > Phone verification).
   Sans cela, Kaggle n'active ni le GPU ni Internet dans les notebooks.
2. **Notebook** : *Create > New Notebook*, puis *File > Import Notebook* et choisissez `sillon_kaggle.ipynb`.
3. **Settings** (panneau de droite) : **Accelerator** = GPU T4 (x2) ou P100, **Internet** = On.
4. **Jeux de données** (*Add Data*) : un jeu PlantVillage et un jeu PlantDoc, plus, si vous en avez un, le jeu iNaturalist
   déjà collecté (sinon le notebook lance `collect_data.py`, environ 20 min). Renseignez ensuite les variables de la
   première cellule (fragments de noms de dossiers ; les valeurs `TODO` sont à compléter, aucun nom n'est présumé).
   Le dépôt étant privé, mettez plutôt `tools/ml` dans un jeu Kaggle privé et renseignez `SCRIPTS_HINT`.
5. **Lancer** : *Save Version > Save & Run All (Commit)*. Le calcul continue sans l'onglet ouvert. Durée estimée (non
   mesurée) : 5 à 9 h pour les trois variantes, sur un quota GPU d'environ 30 h par semaine.
   Si la session est coupée : rouvrez le notebook, ajoutez la sortie de la version précédente (*Add Data > Notebook
   Output*), renseignez `PREVIOUS_OUTPUT` et relancez : les variantes terminées sont ignorées, celle en cours repart
   du dernier palier (`--resume`).
6. **Récupérer** : onglet *Output* de la version enregistrée, dossiers `out_<variante>/` (`sillon_disease.tflite`,
   `sillon_disease.json`, `rapport_metriques.txt`, `metrics.json`).
7. **Me renvoyer** : le tableau comparatif imprimé par l'avant-dernière cellule et les trois `rapport_metriques.txt`
   (et le `.tflite` + `.json` de la variante qui gagne). Ne copiez rien dans `app/` à la main (voir le critère plus bas).

En local, sans GPU : `python train_sillon.py --plantvillage <dossier> --plantdoc <dossier> --classed dataset_raw` (très lent),
et `python train_sillon.py --synthetic` pour vérifier le pipeline sans données (le modèle obtenu est marqué `synthetic`
et l'appli refuse de l'utiliser). Le `.tflite` n'est copié dans l'appli qu'avec `--install`.

## Options d'entraînement

| Option | Effet |
|---|---|
| `--backbone small\|large\|efficientnet_lite0` | MobileNetV3-Small (1 Mo, défaut), MobileNetV3-Large (environ 3 Mo), ou EfficientNet : Keras Applications n'a pas de variante Lite, **EfficientNetB0 est utilisé à sa place** (environ 4,5 Mo) |
| `--img-size 224\|288\|320` | Côté de l'image d'entrée (multiple de 32). Écrit dans `input.width/height` du JSON |
| `--merge-leaf-spots` | Fusionne mildiou et alternariose en `tache_feuille` (`catalogId` null, `group_members: ["mildiou", "alternariose"]`) |
| `--recipe improved` | Active en bloc : mixup + cutmix, dégel progressif, cosine decay, 30 époques de fine-tuning |
| `--mix none\|mixup\|cutmix\|both`, `--mix-alpha 0.2`, `--mix-prob 0.3` | Mélange d'images par lot, léger (un lot sur trois, forme Beta(0,2)) ; étiquettes et poids mélangés au prorata |
| `--progressive-unfreeze` | Dégel en trois paliers (dernier tiers, deux tiers, toute la base sauf BatchNorm) avec un taux d'apprentissage décroissant |
| `--cosine` | Cosine decay du taux d'apprentissage à chaque palier de fine-tuning |
| `--epochs-fine N`, `--epochs-head N`, `--patience N` | Durées (20 en baseline, 30 en improved) et arrêt anticipé (4 par défaut) |
| `--resume` | Reprend après le dernier palier terminé (poids dans `<out>/checkpoints`) |
| `--extra-field <dossier>` | Photos de terrain supplémentaires, voir plus bas |

Inchangés : pondération des classes (`class_weight`), label smoothing 0,1, poids x3 des photos de terrain, découpage
par observateur, température, seuil. Les images sont toujours fournies au modèle en RGB 0..255 NHWC.

## Expansion et amélioration du jeu de données (`expand_dataset.py`)

Pour enrichir le modèle et améliorer sa précision sur le terrain sans dépendre uniquement des photos de labo (PlantVillage) ou des faibles effectifs de PlantDoc (ex. n = 22 pour alternariose, n = 49 pour mildiou), le module `expand_dataset.py` permet de collecter et formater automatiquement des données complémentaires :

1. **API GBIF (Global Biodiversity Information Facility)** : observations scientifiques de terrain géoréférencées pour *Phytophthora infestans*, *Alternaria*, *Botrytis*, *Puccinia*, ainsi que pour les ravageurs et les feuillages sains de plus de 10 familles de légumes (chou, carotte, poireau, haricot, oignon...).
2. **iNaturalist étendu** : requêtes avec filtres larves/stades foliaires et couverture complète des légumes du potager.
3. **Ingestion de jeux externes (Kaggle, PlantWild, Roboflow...)** : remappage automatique vers les 13 classes Sillon, déduplication MD5, vérification stricte des licences libres (CC0, CC-BY, CC-BY-SA) et génération automatique de `ATTRIBUTIONS.csv`.

```bash
# Collecte GBIF pour les classes cibles
python expand_dataset.py --gbif --out extra_gbif --per-class 150

# Ingestion d'un jeu Kaggle ou externe décompressé
python expand_dataset.py --ingest /kaggle/input/nom-du-jeu --source-name plantwild --default-license cc-by --out extra_plantwild

# Vérifier la conformité d'un dossier avant entraînement
python expand_dataset.py --verify extra_gbif

# Entraîner avec ces données supplémentaires
python train_sillon.py --plantvillage <PV> --plantdoc <PD> --classed dataset_raw --extra-field extra_gbif --recipe improved
```

## Jeux de données candidats (pistes à vérifier)

Ces jeux ne sont **pas** utilisés aujourd'hui. Pour chacun, la licence, les conditions de redistribution et le contenu
réel (cultures, classes, photos de terrain ou de labo) restent **à confirmer** avant tout ajout ; une recherche est en cours
séparément. Aucune licence n'est à considérer comme établie ici.

- **PlantSeg** : piste à vérifier (licence à confirmer).
- **PlantWild** : piste à vérifier (licence à confirmer).
- **Plant Pathology 2020** : piste à vérifier (licence à confirmer ; à ma connaissance centré sur le pommier, donc peu utile pour les cultures de Sillon).
- **PlantNet-300K** : piste à vérifier (licence à confirmer ; jeu d'identification d'espèces plutôt que de maladies).

Pour en ajouter un : ranger les photos en un dossier par classe Sillon (`<dossier>/<classe>/<observateur>_<nom>.jpg`, mêmes
noms de classes que `--classed`), puis créer `<dossier>/ATTRIBUTIONS.csv` avec au moins les colonnes `path` (relatif au
dossier) et `license`, une ligne par image (même schéma que celui de `collect_data.py`). `train_sillon.py --extra-field <dossier>`
**refuse** le dossier si le fichier manque, si une image n'y figure pas, ou si une licence n'est pas CC0, CC-BY ou CC-BY-SA
(CC-BY-NC, CC-BY-ND et inconnues refusées). Ces photos ne servent qu'à l'entraînement et à la validation, jamais au test :
le test de terrain reste identique d'une variante à l'autre, donc les chiffres restent comparables. Publiez ensuite les attributions avec le modèle.

## Ce que le modèle sait reconnaître

| Classe | Source des images | Fiche dans l'appli |
|---|---|---|
| sain | feuilles saines de tomate, pomme de terre, poivron, courge, fraisier | aucune |
| mildiou | *late blight* (tomate, pomme de terre) | Mildiou |
| oidium | *powdery mildew* (courge) | Oïdium |
| alternariose | *early blight* (tomate, pomme de terre) | Alternariose |
| autre | autres maladies des mêmes plantes (septoriose, bactérioses, virus...) | renvoi vers la clé |

## Ravageurs et autres maladies : photos iNaturalist

Les autres fiches n'ont pas de jeu de photos public. `collect_data.py` récupère des photos de
terrain iNaturalist (licences CC0, CC-BY, CC-BY-SA seulement), réduites à 320 px, dans un dossier
par classe, avec un fichier `ATTRIBUTIONS.csv` à publier avec le modèle (celui du modèle embarqué
actuel est [`ATTRIBUTIONS.csv`](ATTRIBUTIONS.csv), 5 134 photos) :

```
python collect_data.py                  # environ 20 min, moins de 200 Mo
python train_sillon.py --plantdoc <PlantDoc> --classed dataset_raw --install
```

| Classe | Taxons iNaturalist | Fiche dans l'appli |
|---|---|---|
| pucerons | *Brevicoryne brassicae*, *Aphis fabae*, *Macrosiphum euphorbiae*, *Myzus persicae* | Pucerons |
| altises | *Phyllotreta* | Altises |
| rouille | *Puccinia allii* | Rouille |
| limaces | *Arion vulgaris*, *Deroceras reticulatum*, *Limax maximus* | Limaces |
| pieride_chou | chenilles de *Pieris brassicae* et *P. rapae* | Piéride du chou |
| botrytis | *Botrytis cinerea* | Botrytis |
| doryphore | *Leptinotarsa decemlineata* | Doryphore |
| hors_sujet | liseron, pissenlit, mouron, coccinelle, ver de terre | aucune (jamais un diagnostic) |

iNaturalist complète aussi sain, mildiou, oïdium et alternariose. Les photos d'un même observateur
restent toutes du même côté (entraînement, validation ou test). Limite : pour les ravageurs, les
photos montrent surtout l'animal, pas les dégâts sur la feuille. Cul noir, mouche de la carotte,
teigne du poireau, fonte des semis et anthracnose n'ont pas assez de photos libres : elles restent
accessibles par la clé de diagnostic.

## Lire les résultats

Seules les photos de **terrain** jamais vues (PlantDoc `test` et la part test de `--classed`) servent à juger le modèle :
PlantVillage est photographié en labo et donne des scores trompeurs. Chaque entraînement écrit `rapport_metriques.txt`
(lisible), `metrics.json` et les mêmes chiffres dans `sillon_disease.json`.

- `metrics.field_test` : évaluation simple, **celle que fait l'appli**. `metrics.field_test_tta` : même modèle avec
  test-time augmentation (moyenne sur 5 vues : image, deux miroirs, recadrage central 87,5 % et son miroir). L'appli ne fait pas de TTA ;
  ces chiffres disent seulement ce qu'on gagnerait à en faire.
- Par classe : précision, rappel, F1, effectif `n`, et intervalles de confiance de Wilson à 95 % (`recall_ci95`,
  `precision_ci95`). Une classe de moins de 60 photos de test déclenche l'avertissement « intervalle trop large pour conclure ».
- `leaf_spot_group` : rappel de mildiou et d'alternariose séparément (`recall_exact`, absent d'un modèle fusionné) et
  rappel du groupe (`recall_group`). `merged_view` (modèle non fusionné) : l'évaluation à 12 classes, pour le comparer à un modèle fusionné.
- `selective_curve` : pour chaque seuil de confiance, couverture (part des photos auxquelles l'appli répond) et précision
  des réponses affichées avec son intervalle ; sert à choisir le seuil. `threshold` : seuil retenu pour que les réponses
  affichées soient justes au moins 85 % du temps. En dessous, l'appli dit qu'elle ne sait pas.
- `temperature` : calibration de la confiance, déjà intégrée au fichier `.tflite`.

### Quand installer un nouveau modèle

- `--install` refuse tout modèle dont le macro-F1 de terrain est inférieur à **0,60** (ou synthétique), sauf `--force`.
- Ce garde-fou ne suffit pas pour **remplacer** le modèle actuel. Un nouveau modèle ne le remplace que s'il fait mieux sur le
  macro-F1 **et** sur le rappel de mildiou **et** d'alternariose, avec des intervalles de confiance qui ne se chevauchent pas trop :
  borne basse du nouveau au moins égale à l'estimation de l'ancien (le notebook imprime ce verdict, à relire à la main).
  Avec n = 49 pour le mildiou et n = 22 pour l'alternariose, un écart de quelques points ne prouve rien.
- Comparez toujours à la variante `baseline_small224` du même passage (même découpage de test), pas à des chiffres d'une autre exécution.
- Pour un modèle fusionné, le rappel de mildiou/alternariose est un rappel de groupe, plus facile à atteindre : le notebook le
  confronte au rappel exact de la baseline et affiche aussi le rappel de groupe de la baseline.

## Ce que l'appli doit adapter

Compatible sans changement : taille d'entrée autre que 224 (`PhotoClassifier` lit `input.width/height` dans le JSON, recadre au
centre et redimensionne), `.tflite` toujours en float32 en entrée et en sortie avec poids 8 bits (quantification dynamique, l'`Interpreter` est
chargé comme avant), champs JSON supplémentaires (`ignoreUnknownKeys`). Le script vérifie la forme et le type des tenseurs TFLite à l'export.

À adapter seulement pour un modèle **fusionné** (`--merge-leaf-spots`) : la classe `tache_feuille` a `catalogId` null et
`PhotoModelSpec.ModelClass` ne lit pas `group_members` ; `PhotoDiagnosis.interpret` renverrait donc « pas sûr » sans piste. Il faut
lire `group_members` et renvoyer vers la clé de diagnostic des taches de feuille (fiches Mildiou et Alternariose). Les backbones
Large et EfficientNet sont plus lents et plus gros (3 à 5 Mo contre 1 Mo) : à mesurer sur le Pixel 7a avant d'adopter.

## Licences et Transparence des Données d'Entraînement

Dans le respect de la charte de transparence et d'éthique de **Pilmil Studio** et de la licence MPL 2.0 :

1. **PlantVillage** :
   - *Auteurs* : David P. Hughes & Marcel Salathé (2015), dépôt Sharada P. Mohanty (2016).
   - *Licence* : Creative Commons Attribution 3.0 (CC-BY 3.0) / Open Access.
   - *Utilisation* : Filtré sur 9 965 photos couleur de cultures potagères (tomates, pommes de terre, poivrons, courges). Utilisé uniquement comme régularisateur de laboratoire, le score du modèle n'étant calculé que sur les photos de terrain.

2. **PlantDoc** :
   - *Auteurs* : Davinder Singh, Nidhi Jain, Pranjali Jain, Pratik Kayal et al. (CoDS-COMAD 2020).
   - *Licence* : Creative Commons Attribution 4.0 International (CC-BY 4.0).
   - *Utilisation* : 1 326 photographies in-situ en conditions réelles de culture (feuilles en plein champ, ombres, variations d'éclairage).

3. **iNaturalist Open Data** :
   - *Auteurs* : Réseau mondial de science citoyenne iNaturalist (observations validées au grade recherche ou identification communautaire).
   - *Licences* : Uniquement sous licences libres permises (**CC0**, **CC-BY**, **CC-BY-SA**).
   - *Traçabilité* : Chaque image conserve son observateur d'origine (`u<user_id>_inat<obs_id>.jpg`) et son URL source dans [`ATTRIBUTIONS.csv`](dataset_raw/ATTRIBUTIONS.csv).
   - *Garantie scientifique* : Découpage `GroupKFold` par observateur : aucune plante ou session photo d'un même jardinier ne peut se retrouver à la fois dans l'entraînement et dans le jeu de test.
