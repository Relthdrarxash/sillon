#!/usr/bin/env python3
"""
Entraînement du diagnostic photo de Sillon, de bout en bout.

    images PlantVillage (labo) + PlantDoc (terrain)
      -> classes Sillon (sain, mildiou, oïdium, alternariose, autre problème)
      -> MobileNetV3 Small/Large ou EfficientNet-B0 (transfert depuis ImageNet)
      -> calibration de la confiance (température)
      -> évaluation séparée sur les photos de TERRAIN (simple et avec test-time augmentation)
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

Variante améliorée (voir README) :
    python train_sillon.py ... --backbone large --img-size 288 --recipe improved --merge-leaf-spots
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

# Classes de base (celles des dossiers et des jeux de données). « id » = identifiant dans DiseaseCatalog.kt
# (null pour « sain » et « autre », qui ne renvoient vers aucune fiche). Avec --merge-leaf-spots,
# les sorties du modèle sont celles de build_classes() : mildiou et alternariose y sont regroupés.
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

# Taches foliaires que les photos de terrain distinguent mal : regroupement optionnel.
LEAF_SPOT_MEMBERS = ["mildiou", "alternariose"]
LEAF_SPOT_LABEL = "tache_feuille"
# Une classe testée sur moins d'images donne un intervalle de confiance trop large pour conclure.
MIN_TEST_IMAGES = 60
# Licences acceptées pour --extra-field (CC-BY-NC et CC-BY-ND sont refusées).
ACCEPTED_LICENSES = {"cc0", "cc-by", "cc-by-sa"}


def build_classes(merge_leaf_spots: bool) -> tuple[list[dict], np.ndarray]:
    """
    Classes en sortie du modèle, et table indice de base -> indice de sortie.
    Fusionnées : « tache_feuille » (catalogId null) prend la place de mildiou et retire alternariose ;
    group_members dit à l'appli vers quelles fiches de la clé de diagnostic renvoyer.
    """
    if not merge_leaf_spots:
        return [dict(c) for c in CLASSES], np.arange(len(CLASSES))
    classes: list[dict] = []
    remap = np.zeros(len(CLASSES), dtype=int)
    group_index = None
    for i, c in enumerate(CLASSES):
        if c["label"] in LEAF_SPOT_MEMBERS:
            if group_index is None:
                group_index = len(classes)
                classes.append({"label": LEAF_SPOT_LABEL, "catalogId": None, "group_members": list(LEAF_SPOT_MEMBERS)})
            remap[i] = group_index
        else:
            remap[i] = len(classes)
            classes.append(dict(c))
    return classes, remap

# Plantes potagères gardées. Le reste (pommier, maïs, vigne, agrumes...) est ignoré.
CROPS = {"tomato", "potato", "pepper", "squash", "strawberry"}
IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
DEFAULT_IMG_SIZE = 224
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


def normalize_license(text: str) -> str:
    """'CC BY-SA 4.0' -> 'cc-by-sa' ; 'cc0-1.0' -> 'cc0'. Le reste est laissé tel quel (donc refusé)."""
    t = re.sub(r"[\s_]+", "-", text.strip().lower())
    t = re.sub(r"-?(international|unported|generic)", "", t)
    t = re.sub(r"-?v?\d+(\.\d+)?$", "", t)
    return t


def check_extra_field(root: Path) -> list[str]:
    """
    Vérifie qu'un dossier --extra-field est publiable : un fichier ATTRIBUTIONS.csv (colonnes path et
    license, comme collect_data.py) doit lister chaque image avec une licence CC0, CC-BY ou CC-BY-SA.
    Renvoie la liste des problèmes (vide si tout est en ordre).
    """
    import csv

    attributions = root / "ATTRIBUTIONS.csv"
    if not attributions.is_file():
        return [f"{root} : fichier ATTRIBUTIONS.csv absent (colonnes path, license, attribution)"]
    with attributions.open(encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        if not reader.fieldnames or not {"path", "license"} <= set(reader.fieldnames):
            return [f"{attributions} : colonnes path et license obligatoires"]
        licenses = {row["path"].replace("\\", "/"): normalize_license(row["license"]) for row in reader}
    problems = []
    refused = sorted({v for v in licenses.values() if v not in ACCEPTED_LICENSES})
    if refused:
        problems.append(f"{attributions} : licence(s) refusée(s) {refused} (acceptées : CC0, CC-BY, CC-BY-SA)")
    images = [p for p in root.rglob("*") if p.suffix.lower() in IMAGE_EXTENSIONS]
    if not images:
        problems.append(f"{root} : aucune image")
    missing = [p for p in images if p.relative_to(root).as_posix() not in licenses]
    if missing:
        problems.append(f"{root} : {len(missing)} image(s) sans ligne d'attribution (ex. {missing[0].relative_to(root)})")
    return problems


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


def wilson(k: int, n: int, z: float = 1.96) -> list[float]:
    """Intervalle de confiance de Wilson à 95 % d'une proportion k/n (n = 0 : [0, 1])."""
    if n <= 0:
        return [0.0, 1.0]
    p = k / n
    denom = 1 + z * z / n
    centre = (p + z * z / (2 * n)) / denom
    half = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / denom
    return [round(max(0.0, centre - half), 3), round(min(1.0, centre + half), 3)]


def report(probs: np.ndarray, labels: np.ndarray, names: list[str]) -> dict:
    n = len(names)
    pred = probs.argmax(axis=1)
    confusion = np.zeros((n, n), dtype=int)
    for t, p in zip(labels, pred):
        confusion[t, p] += 1
    per_class, f1s = {}, []
    for i, name in enumerate(names):
        tp = int(confusion[i, i])
        support, predicted = int(confusion[i, :].sum()), int(confusion[:, i].sum())
        precision = tp / max(1, predicted)
        recall = tp / max(1, support)
        f1 = 2 * precision * recall / max(1e-9, precision + recall)
        per_class[name] = {
            "precision": round(float(precision), 3), "recall": round(float(recall), 3), "f1": round(float(f1), 3), "n": support,
            "precision_ci95": wilson(tp, predicted), "recall_ci95": wilson(tp, support),
        }
        if support:
            f1s.append(f1)
    correct = int((pred == labels).sum())
    return {
        "n": int(len(labels)),
        "accuracy": round(float((pred == labels).mean()), 3) if len(labels) else 0.0,
        "accuracy_ci95": wilson(correct, len(labels)),
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


def selective_curve(probs: np.ndarray, labels: np.ndarray) -> list[dict]:
    """
    Prédiction sélective : pour chaque seuil, part des photos auxquelles l'appli répond (couverture)
    et justesse de ces réponses (précision), avec l'intervalle de Wilson. Sert à choisir le seuil.
    """
    conf, correct = probs.max(axis=1), probs.argmax(axis=1) == labels
    curve = []
    for t in np.arange(0.30, 0.951, 0.05):
        shown = conf >= t
        k, n = int(correct[shown].sum()), int(shown.sum())
        curve.append({
            "threshold": round(float(t), 2),
            "coverage": round(float(shown.mean()), 3) if len(labels) else 0.0,
            "precision": round(k / n, 3) if n else None,
            "precision_ci95": wilson(k, n) if n else None,
            "n_shown": n,
        })
    return curve


def merge_probs(probs: np.ndarray) -> np.ndarray:
    """Probabilités à 13 classes de base -> 12 classes (mildiou + alternariose additionnés)."""
    _, remap = build_classes(True)
    merged = np.zeros((len(probs), int(remap.max()) + 1))
    for i in range(probs.shape[1]):
        merged[:, remap[i]] += probs[:, i]
    return merged


def leaf_spot_group(pred_names: list[str], true_base: list[str], merged: bool) -> dict:
    """
    Rappel de mildiou et d'alternariose pris séparément, et du groupe « taches de feuille ».
    recall_exact : la bonne maladie est nommée (impossible quand le modèle est fusionné).
    recall_group : la photo est classée dans le groupe (mildiou ou alternariose, ou tache_feuille).
    """
    in_group = {LEAF_SPOT_LABEL} if merged else set(LEAF_SPOT_MEMBERS)
    out = {"members": {}}
    for member in LEAF_SPOT_MEMBERS:
        idx = [i for i, t in enumerate(true_base) if t == member]
        n = len(idx)
        grp = sum(pred_names[i] in in_group for i in idx)
        entry = {"n": n, "recall_group": round(grp / n, 3) if n else 0.0, "recall_group_ci95": wilson(grp, n)}
        if not merged:
            exact = sum(pred_names[i] == member for i in idx)
            entry.update(recall_exact=round(exact / n, 3) if n else 0.0, recall_exact_ci95=wilson(exact, n))
        out["members"][member] = entry
    true_in = [t in LEAF_SPOT_MEMBERS for t in true_base]
    pred_in = [p in in_group for p in pred_names]
    tp = sum(a and b for a, b in zip(true_in, pred_in))
    n_true, n_pred = sum(true_in), sum(pred_in)
    out["group"] = {
        "n": n_true, "recall": round(tp / max(1, n_true), 3), "recall_ci95": wilson(tp, n_true),
        "precision": round(tp / max(1, n_pred), 3), "precision_ci95": wilson(tp, n_pred),
    }
    return out


def evaluate_block(probs: np.ndarray, true_idx: np.ndarray, true_base: list[str], names: list[str], merged: bool, target_precision: float) -> dict:
    """Toutes les métriques d'un jeu de probabilités (simple ou TTA) sur le test de terrain."""
    block = report(probs, true_idx, names)
    if len(true_idx):
        pred_names = [names[i] for i in probs.argmax(axis=1)]
        block["leaf_spot_group"] = leaf_spot_group(pred_names, true_base, merged)
        if not merged:
            # Même évaluation à 12 classes, pour comparer honnêtement avec un modèle fusionné.
            merged_names = [c["label"] for c in build_classes(True)[0]]
            _, remap = build_classes(True)
            block["merged_view"] = report(merge_probs(probs), remap[true_idx], merged_names)
    threshold, coverage = choose_threshold(probs, true_idx, target_precision) if len(true_idx) else (0.8, 0.0)
    block["threshold"], block["coverage_at_threshold"] = threshold, coverage
    block["selective_curve"] = selective_curve(probs, true_idx)
    return block


def format_report(title: str, block: dict, merged: bool) -> str:
    """Rapport texte lisible d'un bloc d'évaluation."""
    lines = [f"== {title} (n = {block['n']}) ==",
             f"précision globale {block['accuracy']:.3f} {block['accuracy_ci95']}   macro-F1 {block['macro_f1']:.3f}   ECE {block['ece']:.3f}",
             f"{'classe':14s}{'n':>5s}  {'rappel':>7s} {'IC 95 % rappel':>16s}  {'précision':>9s} {'IC 95 % précision':>18s}  F1"]
    for name, m in block["per_class"].items():
        flag = "  <- aucune image de test" if m["n"] == 0 else "  <- IC trop large pour conclure" if m["n"] < MIN_TEST_IMAGES else ""
        lines.append(f"{name:14s}{m['n']:5d}  {m['recall']:7.3f} {str(m['recall_ci95']):>16s}  {m['precision']:9.3f} {str(m['precision_ci95']):>18s}  {m['f1']:.2f}{flag}")
    group = block.get("leaf_spot_group")
    if group:
        lines.append("Taches de feuille (mildiou, alternariose) :")
        for member, e in group["members"].items():
            exact = f", rappel exact {e['recall_exact']:.3f} {e['recall_exact_ci95']}" if "recall_exact" in e else ""
            lines.append(f"  {member:13s} n={e['n']:4d}  rappel dans le groupe {e['recall_group']:.3f} {e['recall_group_ci95']}{exact}")
        g = group["group"]
        lines.append(f"  groupe : rappel {g['recall']:.3f} {g['recall_ci95']}, précision {g['precision']:.3f} {g['precision_ci95']}")
    view = block.get("merged_view")
    if view:
        lines.append(f"Vue à 12 classes (mildiou+alternariose fusionnés) : macro-F1 {view['macro_f1']:.3f}, précision {view['accuracy']:.3f}")
    lines.append(f"Seuil retenu {block['threshold']} -> couverture {block['coverage_at_threshold']:.3f}")
    lines.append("Couverture / précision des réponses affichées selon le seuil :")
    for c in block["selective_curve"]:
        prec = f"{c['precision']:.3f} {c['precision_ci95']}" if c["precision"] is not None else "-"
        lines.append(f"  seuil {c['threshold']:.2f}  couverture {c['coverage']:.3f}  précision {prec}  (n = {c['n_shown']})")
    return "\n".join(lines)


def low_support_warnings(block: dict) -> list[str]:
    warnings = [
        f"ATTENTION : {name} n'a que {m['n']} image(s) de test (< {MIN_TEST_IMAGES}) : intervalle trop large pour conclure."
        for name, m in block["per_class"].items() if 0 < m["n"] < MIN_TEST_IMAGES
    ]
    empty = [name for name, m in block["per_class"].items() if m["n"] == 0]
    if empty:
        warnings.append(f"ATTENTION : aucune image de test pour {', '.join(empty)} : rien à conclure sur ces classes.")
    return warnings


# ---------------------------------------------------------------------------------------------
# Entraînement
# ---------------------------------------------------------------------------------------------

BACKBONES = ("small", "large", "efficientnet_lite0")


def build_backbone(keras, name: str, size: int, weights):
    """
    Base convolutive qui reçoit des pixels RGB 0..255 (prétraitement inclus dans le modèle).
    efficientnet_lite0 : Keras Applications n'a pas de variante Lite, on utilise EfficientNetB0
    (plus lourd : environ 5 Mo en TFLite à poids 8 bits, contre 1 Mo pour small).
    """
    shape = (size, size, 3)
    if name == "small":
        return keras.applications.MobileNetV3Small(input_shape=shape, include_top=False, weights=weights, pooling="avg", include_preprocessing=True)
    if name == "large":
        return keras.applications.MobileNetV3Large(input_shape=shape, include_top=False, weights=weights, pooling="avg", include_preprocessing=True)
    print("efficientnet_lite0 indisponible dans Keras Applications : EfficientNetB0 utilisé à la place.")
    return keras.applications.EfficientNetB0(input_shape=shape, include_top=False, weights=weights, pooling="avg")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--plantvillage", type=Path, action="append", default=[], help="Dossier PlantVillage (photos de labo). Répétable.")
    parser.add_argument("--plantdoc", type=Path, action="append", default=[], help="Dossier PlantDoc ou autres photos de terrain. Répétable.")
    parser.add_argument("--classed", type=Path, action="append", default=[], help="Photos de terrain rangées par classe Sillon (collect_data.py). Répétable.")
    parser.add_argument("--extra-field", type=Path, action="append", default=[], help="Photos de terrain supplémentaires, dossier par classe Sillon, avec ATTRIBUTIONS.csv (licences CC0, CC-BY, CC-BY-SA). Jamais utilisées pour le test. Répétable.")
    parser.add_argument("--out", type=Path, default=Path("out"))
    parser.add_argument("--backbone", choices=BACKBONES, default="small", help="small (1 Mo), large, ou efficientnet_lite0 (EfficientNetB0 faute de Lite dans Keras).")
    parser.add_argument("--img-size", type=int, default=DEFAULT_IMG_SIZE, help="Côté de l'image d'entrée (224 par défaut ; 288 ou 320 possibles). Écrit dans le JSON.")
    parser.add_argument("--merge-leaf-spots", action="store_true", help="Fusionne mildiou et alternariose en une classe tache_feuille.")
    parser.add_argument("--recipe", choices=("baseline", "improved"), default="baseline", help="improved = mixup/cutmix, dégel progressif, cosine decay, 30 époques de fine-tuning (chaque option reste réglable).")
    parser.add_argument("--epochs-head", type=int, default=5)
    parser.add_argument("--epochs-fine", type=int, default=None, help="Époques de fine-tuning (20 en baseline, 30 en improved).")
    parser.add_argument("--mix", choices=("none", "mixup", "cutmix", "both"), default=None, help="Mélange d'images par lot (improved : both).")
    parser.add_argument("--mix-alpha", type=float, default=0.2, help="Paramètre Beta du mélange : petit = léger.")
    parser.add_argument("--mix-prob", type=float, default=0.3, help="Part des lots mélangés.")
    parser.add_argument("--progressive-unfreeze", action="store_true", default=None, help="Dégel en trois paliers (dernier tiers, deux tiers, tout sauf BatchNorm).")
    parser.add_argument("--cosine", action="store_true", default=None, help="Taux d'apprentissage en cosine decay à chaque palier de fine-tuning.")
    parser.add_argument("--patience", type=int, default=4, help="Arrêt anticipé : époques sans progrès de la validation.")
    parser.add_argument("--resume", action="store_true", help="Reprend après le dernier palier terminé (poids dans <out>/checkpoints).")
    parser.add_argument("--batch", type=int, default=32)
    parser.add_argument("--field-weight", type=float, default=3.0, help="Poids des photos de terrain à l'entraînement.")
    parser.add_argument("--target-precision", type=float, default=0.85)
    parser.add_argument("--min-field-f1", type=float, default=0.60, help="En dessous, le modèle n'est pas installé dans l'appli (sauf --force).")
    parser.add_argument("--install", action="store_true", help="Copier le modèle dans app/src/main/assets/models/.")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--synthetic", action="store_true", help="Vérifier le pipeline sur un jeu factice (1 époque, sans ImageNet).")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    improved = args.recipe == "improved"
    if args.epochs_fine is None:
        args.epochs_fine = 30 if improved else 20
    if args.mix is None:
        args.mix = "both" if improved else "none"
    if args.progressive_unfreeze is None:
        args.progressive_unfreeze = improved
    if args.cosine is None:
        args.cosine = improved
    if args.img_size < 96 or args.img_size % 32:
        parser.error("--img-size : multiple de 32, au moins 96 (224, 288, 320...)")

    for root in args.extra_field:
        problems = check_extra_field(root)
        if problems:
            print("--extra-field refusé, dossier non publiable :", file=sys.stderr)
            for p in problems:
                print(f"  - {p}", file=sys.stderr)
            return 3

    import tensorflow as tf
    import keras

    img = args.img_size
    classes, remap = build_classes(args.merge_leaf_spots)
    names = [c["label"] for c in classes]
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
    # Les jeux supplémentaires n'alimentent que l'entraînement et la validation : le test de terrain
    # reste le même d'une variante à l'autre, donc les chiffres restent comparables.
    for root in args.extra_field:
        samples += scan_classed(root, test_ratio=0.0, val_ratio=0.15)

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

    # Poids : classes rares et photos de terrain comptent davantage (sur les classes de sortie).
    counts = np.bincount([remap[s.label] for s in train], minlength=len(names)).astype(float)
    class_weight = np.where(counts > 0, counts.sum() / (len(names) * np.maximum(counts, 1)), 0.0)

    def dataset(items: list[Sample], training: bool):
        paths = [str(s.path) for s in items]
        labels = [int(remap[s.label]) for s in items]
        weights_ = [class_weight[remap[s.label]] * (args.field_weight if s.source == "field" else 1.0) for s in items]

        def load(path, label, weight):
            image = tf.io.decode_image(tf.io.read_file(path), channels=3, expand_animations=False)
            image.set_shape([None, None, 3])
            image = tf.image.resize(image, (img, img))  # valeurs 0..255, comme dans l'appli
            return image, tf.one_hot(label, len(names)), tf.cast(weight, tf.float32)

        ds = tf.data.Dataset.from_tensor_slices((paths, labels, weights_))
        if training:
            ds = ds.shuffle(len(paths), seed=args.seed, reshuffle_each_iteration=True)
        ds = ds.map(load, num_parallel_calls=tf.data.AUTOTUNE)
        ds = ds.batch(args.batch)
        if training and args.mix != "none":
            ds = ds.map(mix_batch, num_parallel_calls=tf.data.AUTOTUNE)
        return ds.prefetch(tf.data.AUTOTUNE)

    def mix_batch(images, labels, weights_):
        """Mixup et/ou cutmix léger sur un lot (une fois sur mix-prob) ; étiquettes et poids mélangés au prorata."""
        size = tf.shape(images)[0]
        order = tf.random.shuffle(tf.range(size))
        other_images, other_labels, other_weights = tf.gather(images, order), tf.gather(labels, order), tf.gather(weights_, order)
        a = tf.random.gamma([], args.mix_alpha)
        b = tf.random.gamma([], args.mix_alpha)
        lam = a / tf.maximum(a + b, 1e-6)
        # cutmix : un rectangle de l'autre image, d'aire (1 - lam)
        ratio = tf.sqrt(1.0 - lam)
        cut_h, cut_w = tf.cast(ratio * img, tf.int32), tf.cast(ratio * img, tf.int32)
        cy = tf.random.uniform([], 0, img, dtype=tf.int32)
        cx = tf.random.uniform([], 0, img, dtype=tf.int32)
        y1, y2 = tf.maximum(cy - cut_h // 2, 0), tf.minimum(cy + cut_h // 2, img)
        x1, x2 = tf.maximum(cx - cut_w // 2, 0), tf.minimum(cx + cut_w // 2, img)
        rows, cols = tf.range(img)[:, None], tf.range(img)[None, :]
        box = tf.cast((rows >= y1) & (rows < y2) & (cols >= x1) & (cols < x2), tf.float32)[None, :, :, None]
        lam_cut = 1.0 - tf.reduce_sum(box) / float(img * img)
        use_cut = {"cutmix": True, "mixup": False}.get(args.mix)
        pick_cut = tf.constant(use_cut) if use_cut is not None else tf.random.uniform([]) < 0.5
        mixed = tf.where(pick_cut, images * (1.0 - box) + other_images * box, lam * images + (1.0 - lam) * other_images)
        lam_used = tf.where(pick_cut, lam_cut, lam)
        apply = tf.random.uniform([]) < args.mix_prob
        images = tf.where(apply, mixed, images)
        lam_final = tf.where(apply, lam_used, 1.0)
        return images, lam_final * labels + (1.0 - lam_final) * other_labels, lam_final * weights_ + (1.0 - lam_final) * other_weights

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

    base = build_backbone(keras, args.backbone, img, weights)
    inputs = keras.Input((img, img, 3), name="image")
    x = augment(inputs)
    x = base(x)
    x = keras.layers.Dropout(0.3)(x)
    logits = keras.layers.Dense(len(names), name="logits")(x)
    model = keras.Model(inputs, logits)
    loss = keras.losses.CategoricalCrossentropy(from_logits=True, label_smoothing=0.1)

    train_ds, val_ds = dataset(train, True), dataset(val, False)
    steps = math.ceil(len(train) / args.batch)

    def learning_rate(peak: float, epochs: int):
        if not args.cosine:
            return peak
        return keras.optimizers.schedules.CosineDecay(peak, decay_steps=max(1, epochs * steps), alpha=0.02)

    # Paliers : (nom, part de la base dégelée en partant de la fin, époques, taux d'apprentissage).
    # Sans dégel progressif : tête, puis dernier tiers de la base (comportement historique).
    if args.progressive_unfreeze:
        e1 = max(1, round(args.epochs_fine * 0.4))
        e2 = max(1, round(args.epochs_fine * 0.3))
        e3 = max(1, args.epochs_fine - e1 - e2)
        stages = [("tête", 0.0, args.epochs_head, 1e-3), ("tiers", 1 / 3, e1, 1e-4), ("deux tiers", 2 / 3, e2, 5e-5), ("tout", 1.0, e3, 2e-5)]
    else:
        stages = [("tête", 0.0, args.epochs_head, 1e-3), ("tiers", 1 / 3, args.epochs_fine, 1e-4)]

    args.out.mkdir(parents=True, exist_ok=True)
    ckpt_dir = args.out / "checkpoints"
    ckpt_dir.mkdir(exist_ok=True)
    state_path = ckpt_dir / "state.json"
    fingerprint = {"backbone": args.backbone, "img_size": img, "merge": args.merge_leaf_spots, "stages": [s[0] for s in stages]}
    done = -1
    if args.resume and state_path.exists():
        state = json.loads(state_path.read_text())
        if state.get("fingerprint") == fingerprint:
            done = state["stage_done"]
            model.load_weights(str(ckpt_dir / f"stage{done}.weights.h5"))
            print(f"Reprise après le palier {done} ({stages[done][0]}).")
        else:
            print("Points de reprise d'une autre configuration : ignorés.")

    for k, (stage_name, fraction, epochs, peak) in enumerate(stages):
        if k <= done:
            continue
        print(f"--- Palier {k} : {stage_name}, {epochs} époque(s) ---")
        callbacks = []
        if fraction == 0.0:
            base.trainable = False
        else:
            base.trainable = True
            layers = base.layers
            for layer in layers[: int(len(layers) * (1 - fraction))]:
                layer.trainable = False
            if args.progressive_unfreeze:  # BatchNorm gelée : statistiques stables avec de petits lots
                for layer in layers:
                    if isinstance(layer, keras.layers.BatchNormalization):
                        layer.trainable = False
            callbacks.append(keras.callbacks.EarlyStopping(patience=args.patience, restore_best_weights=True))
        model.compile(keras.optimizers.Adam(learning_rate(peak, epochs) if fraction else peak), loss, weighted_metrics=["accuracy"])
        model.fit(train_ds, validation_data=val_ds, epochs=epochs, callbacks=callbacks)
        model.save_weights(str(ckpt_dir / f"stage{k}.weights.h5"))
        state_path.write_text(json.dumps({"fingerprint": fingerprint, "stage_done": k}))

    def tta_views(images):
        """5 vues : image entière, miroirs horizontal et vertical, recadrage central 87,5 %, et ce recadrage en miroir."""
        crop = tf.image.resize(tf.image.central_crop(images, 0.875), (img, img))
        return [images, tf.image.flip_left_right(images), tf.image.flip_up_down(images), crop, tf.image.flip_left_right(crop)]

    def predict_logits(items: list[Sample], tta: bool = False) -> tuple[np.ndarray, np.ndarray]:
        if not items:
            return np.zeros((0, len(names))), np.zeros(0, dtype=int)
        truth = np.array([remap[s.label] for s in items])
        if not tta:
            return model.predict(dataset(items, False), verbose=0), truth
        out = [np.mean([model(v, training=False).numpy() for v in tta_views(batch)], axis=0) for batch, _, _ in dataset(items, False)]
        return np.concatenate(out), truth

    true_base = [LABELS[s.label] for s in field_test]
    lab_val = [s for s in val if s.source == "lab"]
    lab_logits, lab_labels = predict_logits(lab_val)
    val_logits, val_labels = predict_logits(val)
    test_logits, test_labels = predict_logits(field_test)
    # La température est ajustée sur la validation, séparément pour chaque mode d'évaluation.
    temperature = fit_temperature(val_logits, val_labels)
    val_tta, _ = predict_logits(val, tta=True)
    test_tta, _ = predict_logits(field_test, tta=True)
    temperature_tta = fit_temperature(val_tta, val_labels)

    field_block = evaluate_block(softmax(test_logits / temperature), test_labels, true_base, names, args.merge_leaf_spots, args.target_precision)
    tta_block = evaluate_block(softmax(test_tta / temperature_tta), test_labels, true_base, names, args.merge_leaf_spots, args.target_precision)
    threshold, coverage = field_block["threshold"], field_block["coverage_at_threshold"]
    metrics = {
        "field_test": {k: v for k, v in field_block.items() if k not in ("threshold", "coverage_at_threshold")},
        "field_test_tta": {**{k: v for k, v in tta_block.items() if k not in ("threshold", "coverage_at_threshold")},
                           "temperature": temperature_tta, "threshold": tta_block["threshold"],
                           "coverage_at_threshold": tta_block["coverage_at_threshold"]},
        "lab_validation": report(softmax(lab_logits / temperature), lab_labels, names) if len(lab_labels) else None,
        "threshold": threshold,
        "field_coverage_at_threshold": coverage,
    }
    # Rapport texte lisible et JSON de métriques complet, à côté du modèle.
    text = "\n\n".join([
        f"Sillon : {args.backbone} {img} px, recette {args.recipe}, fusion des taches de feuille : {'oui' if args.merge_leaf_spots else 'non'}",
        format_report("Test de terrain, évaluation simple (celle que fait l'appli)", field_block, args.merge_leaf_spots),
        format_report("Test de terrain, avec test-time augmentation (5 vues, non disponible dans l'appli)", tta_block, args.merge_leaf_spots),
    ] + low_support_warnings(field_block))
    print(text)
    (args.out / "rapport_metriques.txt").write_text(text + "\n", encoding="utf-8")
    (args.out / "metrics.json").write_text(json.dumps(metrics, indent=2, ensure_ascii=False), encoding="utf-8")

    # Export : image 0..255 -> probabilités calibrées. Les augmentations sont inactives en inférence.
    export_in = keras.Input((img, img, 3), name="image")
    probs = keras.layers.Softmax(name="probabilities")(keras.layers.Rescaling(1.0 / temperature)(model(export_in, training=False)))
    export_model = keras.Model(export_in, probs)

    saved = args.out / "saved_model"
    if saved.exists():
        shutil.rmtree(saved)
    export_model.export(str(saved))
    converter = tf.lite.TFLiteConverter.from_saved_model(str(saved))
    converter.optimizations = [tf.lite.Optimize.DEFAULT]  # poids en 8 bits, entrées et sorties en float
    tflite = converter.convert()
    model_path = args.out / "sillon_disease.tflite"
    model_path.write_bytes(tflite)

    # Vérification : le fichier TFLite répond comme le modèle Keras, et prend bien des float32 NHWC.
    interpreter = tf.lite.Interpreter(model_path=str(model_path))
    interpreter.allocate_tensors()
    inp, out = interpreter.get_input_details()[0], interpreter.get_output_details()[0]
    assert tuple(inp["shape"]) == (1, img, img, 3) and inp["dtype"] == np.float32, f"entrée TFLite inattendue : {inp['shape']} {inp['dtype']}"
    assert tuple(out["shape"]) == (1, len(names)) and out["dtype"] == np.float32, f"sortie TFLite inattendue : {out['shape']} {out['dtype']}"
    probe = np.random.default_rng(0).uniform(0, 255, (1, img, img, 3)).astype(np.float32)
    interpreter.set_tensor(inp["index"], probe)
    interpreter.invoke()
    lite = interpreter.get_tensor(out["index"])[0]
    reference = export_model.predict(probe, verbose=0)[0]
    gap = float(np.abs(lite - reference).max())
    print(f"TFLite : {len(tflite) / 1e6:.2f} Mo, entrée {tuple(inp['shape'])} {np.dtype(inp['dtype']).name}, écart max avec Keras {gap:.3f}")

    meta = {
        "format": 1,
        "created": dt.date.today().isoformat(),
        "input": {"width": img, "height": img, "channels": 3, "order": "RGB", "range": [0, 255], "layout": "NHWC"},
        "output": "probabilities",
        "classes": classes,
        "temperature": temperature,
        "threshold": threshold,
        "metrics": metrics,
        "synthetic": args.synthetic,
        "backbone": "efficientnet_b0" if args.backbone == "efficientnet_lite0" else f"mobilenet_v3_{args.backbone}",
        "merge_leaf_spots": args.merge_leaf_spots,
        "recipe": {"name": args.recipe, "mix": args.mix, "progressive_unfreeze": bool(args.progressive_unfreeze), "cosine": bool(args.cosine),
                   "epochs_head": args.epochs_head, "epochs_fine": args.epochs_fine},
        "extra_field": [str(p) for p in args.extra_field],
    }
    meta_path = args.out / "sillon_disease.json"
    meta_path.write_text(json.dumps(meta, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"Écrit : {model_path}, {meta_path}, {args.out / 'rapport_metriques.txt'}")

    if args.install:
        macro_f1 = metrics["field_test"]["macro_f1"]
        good = macro_f1 >= args.min_field_f1 and not args.synthetic
        if not good and not args.force:
            print(f"Modèle NON installé : F1 terrain {macro_f1} < {args.min_field_f1} (ou jeu synthétique).", file=sys.stderr)
            return 2
        assets = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "assets" / "models"
        assets.mkdir(parents=True, exist_ok=True)
        shutil.copy(model_path, assets / model_path.name)
        shutil.copy(meta_path, assets / meta_path.name)
        print(f"Installé dans {assets}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
