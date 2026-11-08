# Diagnostic photo de Sillon : entraînement

Ce dossier produit le modèle embarqué dans l'appli (`app/src/main/assets/models/`) :
`sillon_disease.tflite` (environ 1 Mo) et `sillon_disease.json` (classes, seuil, métriques).
Tant que ces deux fichiers sont absents, l'appli n'affiche pas « Analyser une photo ».

## Le plus simple : Kaggle (gratuit, avec GPU)

1. Créez un notebook sur [kaggle.com](https://www.kaggle.com) et importez `sillon_kaggle.ipynb`.
2. Settings : **Accelerator GPU T4**, **Internet On**.
3. **Add Data** : un jeu **PlantVillage** et un jeu **PlantDoc**.
4. Run All (1 à 2 heures), puis téléchargez les deux fichiers de `out/` et placez-les dans
   `app/src/main/assets/models/`.

En local : `python train_sillon.py --plantvillage <dossier> --plantdoc <dossier> --install`.
Vérification rapide du pipeline, sans données ni GPU : `python train_sillon.py --synthetic`
(le modèle obtenu est marqué `synthetic` et l'appli refuse de l'utiliser).

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
PlantVillage est photographié en labo et donne des scores trompeurs.

- `metrics.field_test.macro_f1` : sous 0,60, `--install` refuse d'installer le modèle.
- `threshold` : seuil choisi pour que les réponses affichées soient justes au moins 85 % du temps.
  En dessous, l'appli dit qu'elle ne sait pas et propose des pistes.
- `field_coverage_at_threshold` : part des photos pour lesquelles l'appli ose répondre.
- `temperature` : calibration de la confiance, déjà intégrée au fichier `.tflite`.

## Licences des données

Vérifiez la licence des jeux utilisés avant de publier un modèle entraîné dessus
(PlantVillage et PlantDoc sont diffusés sous des licences différentes selon les copies).
