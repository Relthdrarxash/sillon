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

Les autres fiches (pucerons, rouille, limaces...) n'ont pas de jeu de photos public suffisant :
elles restent accessibles par la clé de diagnostic. Pour les ajouter, il faut des photos de terrain
triées (quelques centaines par classe), dans un dossier par classe dont le nom contient la plante
et le problème, puis compléter `label_for_folder` dans `train_sillon.py`.

## Lire les résultats

Seules les photos de **terrain** jamais vues (PlantDoc, dossier `test`) servent à juger le modèle :
PlantVillage est photographié en labo et donne des scores trompeurs.

- `metrics.field_test.macro_f1` : sous 0,60, `--install` refuse d'installer le modèle.
- `threshold` : seuil choisi pour que les réponses affichées soient justes au moins 85 % du temps.
  En dessous, l'appli dit qu'elle ne sait pas et propose des pistes.
- `field_coverage_at_threshold` : part des photos pour lesquelles l'appli ose répondre.
- `temperature` : calibration de la confiance, déjà intégrée au fichier `.tflite`.

## Licences des données

Vérifiez la licence des jeux utilisés avant de publier un modèle entraîné dessus
(PlantVillage et PlantDoc sont diffusés sous des licences différentes selon les copies).
