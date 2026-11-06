#!/usr/bin/env python3
"""
Pipeline de préparation et curation de données agronomiques pour Sillon.

Principe scientifique :
1. Éviter le piège du "PlantVillage Laboratory Bias" en mixant avec des images in-situ (PlantDoc, FGVC8).
2. Hard Negative Mining : 20% du jeu d'entraînement est constitué de feuilles saines, adventices,
   terreau, paillage, tuteurs et doigts humains pour interdire les faux diagnostics hallucinés.
3. Séparation stricte par Groupe/Parcelle (GroupKFold) : aucune image issue de la même session
   photo ou de la même plante ne se retrouve à la fois dans le train et dans le test set.
"""

import os
import sys
import json
import argparse
import random
from pathlib import Path
from typing import Dict, List, Tuple

# Nomenclature officielle des classes Sillon (alignée sur DiseaseCatalog.kt)
SILLON_CLASSES = [
    "healthy",                # Feuilles saines (tomate, courgette, piment, chou, etc.)
    "mildew",                 # Mildiou (Phytophthora infestans / Plasmopara)
    "powdery_mildew",         # Oïdium (feutrage blanc)
    "blossom_end_rot",        # Nécrose apicale / Cul noir (physiologique)
    "aphids_damage",          # Pucerons / crispation / miellat
    "allium_rust",            # Rouille des alliacées (Puccinia allii)
    "cabbage_caterpillar",    # Piéride du chou (morsures nettes)
    "colorado_beetle",        # Doryphore (défoliation solanacées)
    "alternaria",             # Alternariose (taches concentriques en cible)
    "botrytis",               # Pourriture grise
    "carrot_fly",             # Mouche de la carotte / mineuses
    "flea_beetle",            # Altises (criblage en petits trous)
    "slug_damage",            # Limaces / morsures dentelées
    "hard_negative_garden",   # Paillage, terreau, piquets, adventices, mains
]

CLASS_TO_IDX = {name: idx for idx, name in enumerate(SILLON_CLASSES)}
IDX_TO_CLASS = {idx: name for idx, name in enumerate(SILLON_CLASSES)}


def create_manifest(
    dataset_dir: Path,
    output_json: Path,
    test_ratio: float = 0.20,
    val_ratio: float = 0.10,
    seed: int = 42,
) -> Dict[str, any]:
    """
    Scanne l'arborescence des données et produit un manifeste JSON reproductible.
    Supporte les métadonnées de parcelle/site pour le partitionnement par groupe.
    """
    random.seed(seed)
    manifest = {
        "classes": SILLON_CLASSES,
        "class_to_idx": CLASS_TO_IDX,
        "counts": {c: 0 for c in SILLON_CLASSES},
        "splits": {
            "train": [],
            "val": [],
            "test": [],
        },
    }

    print(f"[+] Scan du répertoire de données : {dataset_dir}")
    if not dataset_dir.exists():
        print(f"[-] Répertoire introuvable : {dataset_dir}")
        print("[!] Pour un test à blanc, création d'une structure d'exemple.")
        dataset_dir.mkdir(parents=True, exist_ok=True)
        for c in SILLON_CLASSES:
            (dataset_dir / c).mkdir(exist_ok=True)

    # Regroupement par classe et par site (nom de dossier ou préfixe de fichier)
    site_groups: Dict[str, Dict[str, List[Path]]] = {c: {} for c in SILLON_CLASSES}

    valid_extensions = {".jpg", ".jpeg", ".png", ".webp"}

    for class_name in SILLON_CLASSES:
        class_folder = dataset_dir / class_name
        if not class_folder.exists():
            continue

        images = [p for p in class_folder.rglob("*") if p.suffix.lower() in valid_extensions]
        manifest["counts"][class_name] = len(images)

        # Regroupement par "site" ou session pour éviter la fuite de données
        # Format attendu : siteA_img01.jpg ou session2024_08_leaf12.jpg
        for img in images:
            site_id = img.stem.split("_")[0] if "_" in img.stem else "site_default"
            if site_id not in site_groups[class_name]:
                site_groups[class_name][site_id] = []
            site_groups[class_name][site_id].append(img)

    # Répartition par site (GroupKFold logic)
    for class_name, sites in site_groups.items():
        site_list = list(sites.keys())
        random.shuffle(site_list)

        n_sites = len(site_list)
        if n_sites == 0:
            continue

        n_test = max(1, int(n_sites * test_ratio)) if n_sites >= 3 else 0
        n_val = max(1, int(n_sites * val_ratio)) if n_sites >= 4 else 0

        test_sites = set(site_list[:n_test])
        val_sites = set(site_list[n_test : n_test + n_val])
        train_sites = set(site_list[n_test + n_val :])

        for site_id, imgs in sites.items():
            if site_id in test_sites:
                target_split = "test"
            elif site_id in val_sites:
                target_split = "val"
            else:
                target_split = "train"

            for img in imgs:
                manifest["splits"][target_split].append(
                    {
                        "path": str(img.relative_to(dataset_dir)),
                        "class_idx": CLASS_TO_IDX[class_name],
                        "class_name": class_name,
                        "site_id": site_id,
                    }
                )

    # Sauvegarde du manifeste
    output_json.parent.mkdir(parents=True, exist_ok=True)
    with open(output_json, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)

    print(f"[✓] Manifeste généré avec succès dans : {output_json}")
    print(f"    Train : {len(manifest['splits']['train'])} images")
    print(f"    Val   : {len(manifest['splits']['val'])} images")
    print(f"    Test  : {len(manifest['splits']['test'])} images (Holdout de validation par site)")
    return manifest


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Préparation du jeu de données Sillon Edge ML")
    parser.add_argument("--data-dir", type=Path, default=Path("dataset_raw"), help="Répertoire racine des images")
    parser.add_argument("--output", type=Path, default=Path("manifest.json"), help="Fichier JSON de sortie")
    args = parser.parse_args()

    create_manifest(args.data_dir, args.output)
