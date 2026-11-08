#!/usr/bin/env python3
"""
Entraînement du diagnostic photo de Sillon, de bout en bout.

    images PlantVillage (labo) + PlantDoc (terrain)
      -> classes Sillon (sain, mildiou, oïdium, alternariose, autre problème)
      -> MobileNetV3-Small (transfert depuis ImageNet)
      -> calibration de la confiance (température)
      -> évaluation séparée sur les photos de TERRAIN
      -> sillon_disease.tflite + sillon_disease.json, prêts pour l'appli

Pensé pour un notebook Kaggle ou Colab avec GPU (voir sillon_kaggle.ipynb), mais tourne aussi
sur CPU, plus lentement. Rien n'est envoyé nulle part : l'appli embarque le fichier .tflite.

Pourquoi ces choix :
- Seules les classes réellement présentes dans des jeux de données publics sont apprises.
  Les autres maladies de la clé de diagnostic restent consultables à la main dans l'appli.
- La classe « autre » regroupe les autres maladies des mêmes plantes (septoriose, bactérioses,
  virus...) : sans elle, le modèle forcerait une réponse fausse parmi les trois maladies connues.
- PlantVillage est photographié en labo, sur fond uni : un modèle entraîné dessus seul s'effondre
  au jardin. Les photos de terrain (PlantDoc) pèsent donc davantage à l'entraînement, et c'est
  sur elles seules que la qualité est jugée.
- La confiance est calibrée (température), puis un seuil est choisi pour que les réponses
  affichées soient justes au moins 85 % du temps sur le terrain. En dessous, l'appli dit
  « je ne sais pas » et renvoie vers la clé de diagnostic.

Exemples :
    python train_sillon.py --plantvillage /kaggle/input/plantvillage-dataset --plantdoc /kaggle/input/plantdoc-dataset
    python train_sillon.py --synthetic            # vérification rapide du pipeline, sans données
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import math
import random
import re
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path

import numpy as np

# Classes apprises, dans l'ordre des sorties du modèle. « id » = identifiant dans DiseaseCatalog.kt
# (null pour « sain » et « autre », qui ne renvoient vers aucune fiche).
CLASSES = [
    {"label": "sain", "catalogId": None},
    {"label": "mildiou", "catalogId": "mildiou"},
    {"label": "oidium", "catalogId": "oidium"},
    {"label": "alternariose", "catalogId": "alternariose"},
    {"label": "autre", "catalogId": None},
    # Ravageurs et maladies sans jeu public : photos de terrain iNaturalist (collect_data.py,
    # option --classed). Le modèle ne les apprend que si leur dossier est fourni.
    {"label": "pucerons", "catalogId": "pucerons"},
    {"label": "altises", "catalogId": "altises"},
    {"label": "rouille", "catalogId": "rouille"},
    {"label": "limaces", "catalogId": "limaces"},
    {"label": "pieride_chou", "catalogId": "pieride_chou"},
    {"label": "botrytis", "catalogId": "botrytis"},
    {"label": "doryphore", "catalogId": "doryphore"},
    # Adventices, auxiliaires (coccinelles, vers de terre)... : jamais un diagnostic.
    {"label": "hors_sujet", "catalogId": None},
]
LABELS = [c["label"] for c in CLASSES]

# Plantes potagères gardées. Le reste (pommier, maïs, vigne, agrumes...) est ignoré.
CROPS = {"tomato", "potato", "pepper", "squash", "strawberry"}
IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
IMG_SIZE = 224
# Variantes de PlantVillage à ignorer : on ne garde que les photos couleur d'origine.
SKIP_PATH_PARTS = {"grayscale", "segmented"}


def normalize(name: str) -> str:
    """'Pepper,_bell___Bacterial_spot' -> 'pepper bell bacterial spot'."""
    return re.sub(r"[^a-z]+", " ", name.lower()).strip()


def label_for_folder(folder_name: str) -> str | None:
    """
    Classe Sillon d'un dossier PlantVillage ou PlantDoc, ou None s'il est hors sujet.
    Exemples : 'Tomato___Late_blight' -> mildiou ; 'Squash Powdery mildew leaf' -> oidium ;
    'Tomato leaf' -> sain ; 'Tomato Septoria leaf spot' -> autre ; 'Apple___Black_rot' -> None.
    """
    words = normalize(folder_name).split()
    if not words or not (set(words) & CROPS):
        return None
    text = " ".join(words)
    if "late blight" in text:
        return "mildiou"
    if "powdery mildew" in text:
        return "oidium"
    if "early blight" in text:
        return "alternariose"
    plain = set(words) - CROPS - {"leaf", "leaves", "bell"}
    if "healthy" in words or not plain:
        return "sain"
    return "autre"


@dataclass
class Sample:
    path: Path
    label: int
    source: str  # "lab" (PlantVillage) ou "field" (PlantDoc)
    split: str  # "train", "val" ou "test"


def scan(root: Path, source: str, rng: random.Random, val_ratio: float) -> list[Sample]:
    """
    Parcourt un jeu de données. Pour PlantDoc, les dossiers 'train' et 'test' d'origine sont
    respectés (le test reste jamais vu). Ailleurs, un tirage reproductible met val_ratio des images
    en validation.
    """
    samples: list[Sample] = []
    for folder in sorted(p for p in root.rglob("*") if p.is_dir()):
        parts = {normalize(x) for x in folder.relative_to(root).parts}
        if parts & SKIP_PATH_PARTS:
            continue
        label = label_for_folder(folder.name)
        if label is None:
            continue
        images = sorted(p for p in folder.iterdir() if p.suffix.lower() in IMAGE_EXTENSIONS)
        if not images:
            continue
        in_test = "test" in parts
        for image in images:
            if in_test:
                split = "test"
            else:
                split = "val" if rng.random() < val_ratio else "train"
            samples.append(Sample(image, LABELS.index(label), source, split))
    return samples


def scan_classed(root: Path, test_ratio: float, val_ratio: float) -> list[Sample]:
    """
    Photos de terrain rangées par classe Sillon (root/<label>/<groupe>_<nom>.jpg, voir collect_data.py).
    Le découpage se fait par groupe (l'observateur) : un même jardin ne se retrouve jamais à la fois
    à l'entraînement et au test. Le tirage dépend du seul nom du groupe, il est donc reproductible.
    """
    import zlib

    samples: list[Sample] = []
    for folder in sorted(p for p in root.iterdir() if p.is_dir()):
        if folder.name not in LABELS:
            print(f"  dossier ignoré (pas une classe Sillon) : {folder.name}")
            continue
        for image in sorted(p for p in folder.iterdir() if p.suffix.lower() in IMAGE_EXTENSIONS):
            group = image.stem.split("_")[0]
            draw = zlib.crc32(f"{folder.name}/{group}".encode()) / 2**32
            split = "test" if draw < test_ratio else "val" if draw < test_ratio + val_ratio else "train"
            samples.append(Sample(image, LABELS.index(folder.name), "field", split))
    return samples


def make_synthetic(root: Path, per_class: int = 12) -> tuple[Path, Path]:
    """Petit jeu factice aux noms de dossiers réels, pour vérifier le pipeline sans données."""
    from PIL import Image

    lab, field = root / "plantvillage" / "color", root / "plantdoc"
    lab_folders = ["Tomato___healthy", "Tomato___Late_blight", "Squash___Powdery_mildew", "Potato___Early_blight", "Tomato___Septoria_leaf_spot", "Apple___Black_rot"]
    field_folders = ["Tomato leaf", "Tomato leaf late blight", "Squash Powdery mildew leaf", "Tomato Early blight leaf", "Tomato Septoria leaf spot"]
    rng = np.random.default_rng(0)
    for base, folders in ((lab, lab_folders), (field / "train", field_folders), (field / "test", field_folders)):
        for i, name in enumerate(folders):
            d = base / name
            d.mkdir(parents=True, exist_ok=True)
            for k in range(per_class):
                color = np.array([40 + 40 * i, 120, 60 + 20 * i]) + rng.integers(-20, 20, 3)
                pixels = np.clip(rng.normal(color, 25, (64, 64, 3)), 0, 255).astype(np.uint8)
                Image.fromarray(pixels).save(d / f"img_{k}.jpg")
    return lab, field


# ---------------------------------------------------------------------------------------------
# Métriques (numpy seul, pour ne dépendre de rien d'autre que TensorFlow)
# ---------------------------------------------------------------------------------------------

def softmax(logits: np.ndarray) -> np.ndarray:
    z = logits - logits.max(axis=1, keepdims=True)
    e = np.exp(z)
    return e / e.sum(axis=1, keepdims=True)


def fit_temperature(logits: np.ndarray, labels: np.ndarray) -> float:
    """Température qui minimise la log-vraisemblance négative sur la validation."""
    best_t, best_nll = 1.0, math.inf
    for t in np.arange(0.5, 5.01, 0.05):
        p = softmax(logits / t)[np.arange(len(labels)), labels]
        nll = float(-np.log(np.clip(p, 1e-12, 1)).mean())
        if nll < best_nll:
            best_t, best_nll = float(t), nll
    return round(best_t, 2)


def expected_calibration_error(probs: np.ndarray, labels: np.ndarray, bins: int = 10) -> float:
    conf, pred = probs.max(axis=1), probs.argmax(axis=1)
    ece = 0.0
    for lo in np.linspace(0, 1, bins, endpoint=False):
        mask = (conf > lo) & (conf <= lo + 1 / bins)
        if mask.any():
            ece += abs(conf[mask].mean() - (pred[mask] == labels[mask]).mean()) * mask.mean()
    return float(ece)


def report(probs: np.ndarray, labels: np.ndarray) -> dict:
    n = len(LABELS)
    pred = probs.argmax(axis=1)
    confusion = np.zeros((n, n), dtype=int)
    for t, p in zip(labels, pred):
        confusion[t, p] += 1
    per_class, f1s = {}, []
    for i, name in enumerate(LABELS):
        tp = confusion[i, i]
        precision = tp / max(1, confusion[:, i].sum())
        recall = tp / max(1, confusion[i, :].sum())
        f1 = 2 * precision * recall / max(1e-9, precision + recall)
        support = int(confusion[i, :].sum())
        per_class[name] = {"precision": round(float(precision), 3), "recall": round(float(recall), 3), "f1": round(float(f1), 3), "n": support}
        if support:
            f1s.append(f1)
    return {
        "n": int(len(labels)),
        "accuracy": round(float((pred == labels).mean()), 3) if len(labels) else 0.0,
        "macro_f1": round(float(np.mean(f1s)), 3) if f1s else 0.0,
        "ece": round(expected_calibration_error(probs, labels), 3) if len(labels) else 0.0,
        "per_class": per_class,
        "confusion": confusion.tolist(),
    }


def choose_threshold(probs: np.ndarray, labels: np.ndarray, target_precision: float) -> tuple[float, float]:
    """Plus petit seuil de confiance pour lequel les réponses affichées sont justes à target_precision."""
    conf, correct = probs.max(axis=1), probs.argmax(axis=1) == labels
    for t in np.arange(0.40, 0.951, 0.05):
        shown = conf >= t
        if shown.sum() >= 10 and correct[shown].mean() >= target_precision:
            return round(float(t), 2), round(float(shown.mean()), 3)
    return 0.95, round(float((conf >= 0.95).mean()), 3)


# ---------------------------------------------------------------------------------------------
# Entraînement
# ---------------------------------------------------------------------------------------------

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--plantvillage", type=Path, action="append", default=[], help="Dossier PlantVillage (photos de labo). Répétable.")
    parser.add_argument("--plantdoc", type=Path, action="append", default=[], help="Dossier PlantDoc ou autres photos de terrain. Répétable.")
    parser.add_argument("--classed", type=Path, action="append", default=[], help="Photos de terrain rangées par classe Sillon (collect_data.py). Répétable.")
    parser.add_argument("--out", type=Path, default=Path("out"))
    parser.add_argument("--epochs-head", type=int, default=5)
    parser.add_argument("--epochs-fine", type=int, default=20)
    parser.add_argument("--batch", type=int, default=32)
    parser.add_argument("--field-weight", type=float, default=3.0, help="Poids des photos de terrain à l'entraînement.")
    parser.add_argument("--target-precision", type=float, default=0.85)
    parser.add_argument("--min-field-f1", type=float, default=0.60, help="En dessous, le modèle n'est pas installé dans l'appli (sauf --force).")
    parser.add_argument("--install", action="store_true", help="Copier le modèle dans app/src/main/assets/models/.")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--synthetic", action="store_true", help="Vérifier le pipeline sur un jeu factice (1 époque, sans ImageNet).")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    import tensorflow as tf
    import keras

    rng = random.Random(args.seed)
    tf.keras.utils.set_random_seed(args.seed)

    if args.synthetic:
        lab, field = make_synthetic(args.out / "synthetic")
        args.plantvillage, args.plantdoc = [lab], [field]
        args.epochs_head, args.epochs_fine = 1, 1
        weights = None
    else:
        weights = "imagenet"
        if not args.plantvillage and not args.plantdoc and not args.classed:
            parser.error("indiquez au moins --plantvillage, --plantdoc ou --classed (ou --synthetic)")

    samples: list[Sample] = []
    for root in args.plantvillage:
        samples += scan(root, "lab", rng, val_ratio=0.15)
    for root in args.plantdoc:
        samples += scan(root, "field", rng, val_ratio=0.15)
    for root in args.classed:
        samples += scan_classed(root, test_ratio=0.20, val_ratio=0.15)

    def subset(split: str, source: str | None = None) -> list[Sample]:
        return [s for s in samples if s.split == split and (source is None or s.source == source)]

    train, val = subset("train"), subset("val")
    field_test = subset("test", "field") or subset("val", "field")
    print("Images retenues par classe et par origine :")
    for i, name in enumerate(LABELS):
        lab_n = sum(1 for s in samples if s.label == i and s.source == "lab")
        field_n = sum(1 for s in samples if s.label == i and s.source == "field")
        print(f"  {name:13s} labo {lab_n:6d}   terrain {field_n:5d}")
    print(f"Entraînement {len(train)}, validation {len(val)}, test terrain {len(field_test)}")
    if not train or not val:
        print("Pas assez d'images : vérifiez les chemins des jeux de données.", file=sys.stderr)
        return 1

    # Poids : classes rares et photos de terrain comptent davantage.
    counts = np.bincount([s.label for s in train], minlength=len(LABELS)).astype(float)
    class_weight = np.where(counts > 0, counts.sum() / (len(LABELS) * np.maximum(counts, 1)), 0.0)

    def dataset(items: list[Sample], training: bool):
        paths = [str(s.path) for s in items]
        labels = [s.label for s in items]
        weights_ = [class_weight[s.label] * (args.field_weight if s.source == "field" else 1.0) for s in items]

        def load(path, label, weight):
            image = tf.io.decode_image(tf.io.read_file(path), channels=3, expand_animations=False)
            image = tf.image.resize(image, (IMG_SIZE, IMG_SIZE))  # valeurs 0..255, comme dans l'appli
            return image, tf.one_hot(label, len(LABELS)), weight

        ds = tf.data.Dataset.from_tensor_slices((paths, labels, weights_))
        if training:
            ds = ds.shuffle(len(paths), seed=args.seed, reshuffle_each_iteration=True)
        ds = ds.map(load, num_parallel_calls=tf.data.AUTOTUNE)
        return ds.batch(args.batch).prefetch(tf.data.AUTOTUNE)

    # Augmentations proches des conditions du jardin : cadrage, rotation, soleil, contraste.
    augment = keras.Sequential(
        [
            keras.layers.RandomFlip("horizontal_and_vertical"),
            keras.layers.RandomRotation(0.15),
            keras.layers.RandomZoom((-0.25, 0.1)),
            keras.layers.RandomBrightness(0.25, value_range=(0, 255)),
            keras.layers.RandomContrast(0.3),
        ],
        name="augment",
    )

    # include_preprocessing : le modèle attend des pixels RGB 0..255, rien à normaliser côté appli.
    base = keras.applications.MobileNetV3Small(
        input_shape=(IMG_SIZE, IMG_SIZE, 3), include_top=False, weights=weights,
        pooling="avg", include_preprocessing=True,
    )
    inputs = keras.Input((IMG_SIZE, IMG_SIZE, 3), name="image")
    x = augment(inputs)
    x = base(x)
    x = keras.layers.Dropout(0.3)(x)
    logits = keras.layers.Dense(len(LABELS), name="logits")(x)
    model = keras.Model(inputs, logits)
    loss = keras.losses.CategoricalCrossentropy(from_logits=True, label_smoothing=0.1)

    train_ds, val_ds = dataset(train, True), dataset(val, False)

    # 1. Tête seule, base figée.
    base.trainable = False
    model.compile(keras.optimizers.Adam(1e-3), loss, weighted_metrics=["accuracy"])
    model.fit(train_ds, validation_data=val_ds, epochs=args.epochs_head)

    # 2. Ajustement fin du dernier tiers de la base.
    base.trainable = True
    for layer in base.layers[: int(len(base.layers) * 2 / 3)]:
        layer.trainable = False
    model.compile(keras.optimizers.Adam(1e-4), loss, weighted_metrics=["accuracy"])
    model.fit(
        train_ds, validation_data=val_ds, epochs=args.epochs_fine,
        callbacks=[keras.callbacks.EarlyStopping(patience=4, restore_best_weights=True)],
    )

    def predict_logits(items: list[Sample]) -> tuple[np.ndarray, np.ndarray]:
        if not items:
            return np.zeros((0, len(LABELS))), np.zeros(0, dtype=int)
        return model.predict(dataset(items, False), verbose=0), np.array([s.label for s in items])

    val_logits, val_labels = predict_logits(val)
    temperature = fit_temperature(val_logits, val_labels)
    test_logits, test_labels = predict_logits(field_test)
    field_probs = softmax(test_logits / temperature)
    lab_val = [s for s in val if s.source == "lab"]
    lab_logits, lab_labels = predict_logits(lab_val)

    field_report = report(field_probs, test_labels)
    threshold, coverage = choose_threshold(field_probs, test_labels, args.target_precision) if len(test_labels) else (0.8, 0.0)
    metrics = {
        "field_test": field_report,
        "lab_validation": report(softmax(lab_logits / temperature), lab_labels),
        "threshold": threshold,
        "field_coverage_at_threshold": coverage,
    }
    print(json.dumps({k: v for k, v in metrics.items() if k != "lab_validation"}, indent=2, ensure_ascii=False))

    # Export : image 0..255 -> probabilités calibrées. Les augmentations sont inactives en inférence.
    export_in = keras.Input((IMG_SIZE, IMG_SIZE, 3), name="image")
    probs = keras.layers.Softmax(name="probabilities")(keras.layers.Rescaling(1.0 / temperature)(model(export_in, training=False)))
    export_model = keras.Model(export_in, probs)

    args.out.mkdir(parents=True, exist_ok=True)
    saved = args.out / "saved_model"
    if saved.exists():
        shutil.rmtree(saved)
    export_model.export(str(saved))
    converter = tf.lite.TFLiteConverter.from_saved_model(str(saved))
    converter.optimizations = [tf.lite.Optimize.DEFAULT]  # poids en 8 bits, entrées et sorties en float
    tflite = converter.convert()
    model_path = args.out / "sillon_disease.tflite"
    model_path.write_bytes(tflite)

    # Vérification : le fichier TFLite répond comme le modèle Keras.
    interpreter = tf.lite.Interpreter(model_path=str(model_path))
    interpreter.allocate_tensors()
    inp, out = interpreter.get_input_details()[0], interpreter.get_output_details()[0]
    probe = np.random.default_rng(0).uniform(0, 255, (1, IMG_SIZE, IMG_SIZE, 3)).astype(np.float32)
    interpreter.set_tensor(inp["index"], probe)
    interpreter.invoke()
    lite = interpreter.get_tensor(out["index"])[0]
    reference = export_model.predict(probe, verbose=0)[0]
    gap = float(np.abs(lite - reference).max())
    print(f"TFLite : {len(tflite) / 1e6:.2f} Mo, écart max avec Keras {gap:.3f}")

    meta = {
        "format": 1,
        "created": dt.date.today().isoformat(),
        "input": {"width": IMG_SIZE, "height": IMG_SIZE, "channels": 3, "order": "RGB", "range": [0, 255], "layout": "NHWC"},
        "output": "probabilities",
        "classes": CLASSES,
        "temperature": temperature,
        "threshold": threshold,
        "metrics": metrics,
        "synthetic": args.synthetic,
    }
    meta_path = args.out / "sillon_disease.json"
    meta_path.write_text(json.dumps(meta, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"Écrit : {model_path} et {meta_path}")

    if args.install:
        good = field_report["macro_f1"] >= args.min_field_f1 and not args.synthetic
        if not good and not args.force:
            print(f"Modèle NON installé : F1 terrain {field_report['macro_f1']} < {args.min_field_f1} (ou jeu synthétique).", file=sys.stderr)
            return 2
        assets = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "assets" / "models"
        assets.mkdir(parents=True, exist_ok=True)
        shutil.copy(model_path, assets / model_path.name)
        shutil.copy(meta_path, assets / meta_path.name)
        print(f"Installé dans {assets}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
