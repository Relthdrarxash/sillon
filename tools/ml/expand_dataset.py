#!/usr/bin/env python3
"""
Module d'expansion et d'amélioration du corpus d'images pour le modèle IA de Sillon.

Ce module permet de :
1. Collecter des observations de terrain via l'API GBIF (Global Biodiversity Information Facility)
   pour enrichir les classes sous-représentées (mildiou, alternariose, rouille, botrytis, ravageurs,
   et feuilles saines de nombreuses familles de légumes).
2. Étendre la collecte iNaturalist avec de nouveaux taxons potagers et critères de recherche.
3. Ingérer, normaliser et filtrer des jeux de données externes (Kaggle, PlantWild, PlantDoc, Roboflow, etc.)
   en les convertissant au format attendu par train_sillon.py (--extra-field ou --classed) :
   - Arborescence par classe Sillon : <out>/<classe>/u<observateur>_<nom>.jpg
   - Vérification stricte des licences libres (CC0, CC-BY, CC-BY-SA uniquement)
   - Génération automatique ou mise à jour de ATTRIBUTIONS.csv
   - Déduplication par empreinte MD5 / hachage visuel
   - Redimensionnement standardisé (par défaut 320 px côté long)

Usage en ligne de commande :
    # 1. Collecte GBIF pour les maladies potagères ciblées
    python expand_dataset.py --gbif --out extra_field_gbif --per-class 200

    # 2. Collecte combinée GBIF + iNaturalist étendu
    python expand_dataset.py --gbif --inat --out extra_field_all --per-class 150

    # 3. Ingestion et remappage d'un jeu Kaggle ou externe décompressé
    python expand_dataset.py --ingest /kaggle/input/plantwild --source-name plantwild --default-license cc-by --out extra_field_plantwild

    # 4. Vérification de conformité d'un dossier avant entraînement
    python expand_dataset.py --verify extra_field_gbif
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import os
import re
import sys
import time
import urllib.parse
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any, Callable

try:
    import requests
    from PIL import Image
except ImportError:
    print("Erreur : bibliothèques 'requests' et 'Pillow' requises (pip install requests pillow)", file=sys.stderr)
    sys.exit(1)

# Licences compatibles avec la redistribution libre et acceptées par train_sillon.py
ACCEPTED_LICENSES = {"cc0", "cc-by", "cc-by-sa"}
GBIF_API_OCCURRENCE = "https://api.gbif.org/v1/occurrence/search"
INAT_API_OBSERVATIONS = "https://api.inaturalist.org/v1/observations"

DEFAULT_MAX_SIDE = 320
DEFAULT_QUALITY = 90

# Classes officielles reconnues par Sillon
SILLON_CLASSES = [
    "sain",
    "mildiou",
    "oidium",
    "alternariose",
    "autre",
    "pucerons",
    "altises",
    "rouille",
    "limaces",
    "pieride_chou",
    "botrytis",
    "doryphore",
    "hors_sujet",
]

# -------------------------------------------------------------------------------------------------
# Sources taxonomiques GBIF pour enrichir le corpus avec des données scientifiques de terrain
# -------------------------------------------------------------------------------------------------
# Chaque entrée associe une classe Sillon à des noms scientifiques de champignons / pathogènes / insectes.
GBIF_TAXA = {
    "mildiou": [
        "Phytophthora infestans",          # Mildiou pomme de terre / tomate
        "Pseudoperonospora cubensis",      # Mildiou des cucurbitacées
        "Plasmopara viticola",             # Mildiou (vigne / modèle comparatif)
        "Bremia lactucae",                 # Mildiou de la salade
    ],
    "alternariose": [
        "Alternaria solani",               # Alternariose tomate / pomme de terre
        "Alternaria alternata",            # Alternariose générale
        "Alternaria dauci",                # Alternariose de la carotte
        "Alternaria brassicae",            # Alternariose des choux
    ],
    "oidium": [
        "Podosphaera xanthii",             # Oïdium des courges
        "Erysiphe pisi",                   # Oïdium du pois
        "Golovinomyces cichoracearum",     # Oïdium des composées / courges
        "Leveillula taurica",              # Oïdium du poivron / tomate
    ],
    "rouille": [
        "Puccinia allii",                  # Rouille de l'ail / poireau / oignon
        "Puccinia porri",                  # Rouille du poireau
        "Uromyces appendiculatus",         # Rouille du haricot
        "Puccinia malvacearum",            # Rouille des mauves
    ],
    "botrytis": [
        "Botrytis cinerea",                # Pourriture grise
    ],
    "pucerons": [
        "Aphis fabae",                     # Puceron noir de la fève
        "Brevicoryne brassicae",           # Puceron cendré du chou
        "Macrosiphum euphorbiae",          # Puceron vert de la tomate / pdt
        "Myzus persicae",                  # Puceron du pêcher / solanacées
    ],
    "altises": [
        "Phyllotreta atra",                # Altise noire des crucifères
        "Phyllotreta nemorum",             # Altise à bandes jaunes
        "Phyllotreta undulata",
        "Phyllotreta cruciferae",
    ],
    "limaces": [
        "Arion vulgaris",                  # Limace espagnole
        "Deroceras reticulatum",           # Petite limace grise
        "Limax maximus",                   # Grande limace léopard
        "Arion rufus",                     # Grande limace rouge
    ],
    "pieride_chou": [
        "Pieris brassicae",                # Grande piéride
        "Pieris rapae",                    # Petite piéride
    ],
    "doryphore": [
        "Leptinotarsa decemlineata",       # Doryphore de la pomme de terre
    ],
    # Élargissement des feuilles saines à l'ensemble des familles potagères de Sillon
    # (évite les faux positifs sur chou, oignon, carotte, haricot que le modèle n'a jamais vus sains)
    "sain": [
        "Solanum lycopersicum",            # Tomate
        "Solanum tuberosum",               # Pomme de terre
        "Capsicum annuum",                 # Poivron / piment
        "Cucurbita pepo",                  # Courgette / citrouille
        "Brassica oleracea",               # Chou
        "Allium cepa",                     # Oignon
        "Allium porrum",                   # Poireau
        "Phaseolus vulgaris",              # Haricot
        "Pisum sativum",                   # Pois
        "Daucus carota",                   # Carotte
        "Spinacia oleracea",               # Épinard
        "Lactuca sativa",                  # Salade
    ],
    # Autres maladies foliaires fréquentes pour renforcer la classe discriminante « autre »
    "autre": [
        "Septoria lycopersici",            # Septoriose de la tomate
        "Fulvia fulva",                    # Cladosporiose de la tomate
        "Verticillium dahliae",            # Verticilliose
        "Cladosporium allii-porri",        # Cladosporiose de l'ail
    ],
}

# -------------------------------------------------------------------------------------------------
# Sources iNaturalist étendues (avec paramètres spécifiques et taxons complémentaires)
# -------------------------------------------------------------------------------------------------
INAT_EXTENDED_SOURCES = {
    "sain": [
        (51737, "Solanum lycopersicum", {"captive": "true"}),
        (1599236, "Cucurbita pepo", {"captive": "true"}),
        (53858, "Solanum tuberosum", {"captive": "true"}),
        (47912, "Brassica oleracea", {"captive": "true"}),
        (55610, "Allium cepa", {"captive": "true"}),
        (59203, "Phaseolus vulgaris", {"captive": "true"}),
        (54629, "Daucus carota", {"captive": "true"}),
    ],
    "mildiou": [
        (53860, "Phytophthora infestans", {}),
        (196477, "Pseudoperonospora cubensis", {}),
        (1659825, "Plasmopara viticola", {}),
        (381716, "Bremia lactucae", {}),
    ],
    "oidium": [
        (1578546, "Podosphaera xanthii", {}),
        (1542754, "Erysiphe pisi", {}),
        (333884, "Golovinomyces", {}),
        (460592, "Leveillula taurica", {}),
    ],
    "alternariose": [
        (360210, "Alternaria solani", {}),
        (327991, "Alternaria brassicae", {}),
        (381199, "Alternaria dauci", {}),
        (327990, "Alternaria alternata", {}),
    ],
    "pucerons": [
        (326910, "Brevicoryne brassicae", {}),
        (1584763, "Aphis fabae", {}),
        (53859, "Macrosiphum euphorbiae", {}),
        (243745, "Myzus persicae", {}),
    ],
    "altises": [
        (178465, "Phyllotreta", {}),
    ],
    "rouille": [
        (1489211, "Puccinia allii", {}),
        (323955, "Uromyces appendiculatus", {}),
        (1677365, "Puccinia malvacearum", {}),
    ],
    "limaces": [
        (54583, "Arion vulgaris", {}),
        (124432, "Deroceras reticulatum", {}),
        (62470, "Limax maximus", {}),
    ],
    "pieride_chou": [
        (55401, "Pieris brassicae", {"term_id": 1, "term_value_id": 6}),  # Larves / chenilles
        (55626, "Pieris rapae", {"term_id": 1, "term_value_id": 6}),
    ],
    "botrytis": [
        (324263, "Botrytis cinerea", {}),
    ],
    "doryphore": [
        (84778, "Leptinotarsa decemlineata", {}),
    ],
    "autre": [
        (384351, "Septoria lycopersici", {}),
        (362083, "Fulvia fulva", {}),
        (360233, "Verticillium dahliae", {}),
    ],
    "hors_sujet": [
        (53021, "Convolvulus arvensis", {}),
        (967514, "Taraxacum", {}),
        (1087592, "Stellaria media", {}),
        (51702, "Coccinella septempunctata", {}),
        (81545, "Lumbricus terrestris", {}),
    ],
}


def normalize_license(text: str) -> str:
    """Harmonise le nom d'une licence pour vérification."""
    if not text:
        return "unknown"
    t = re.sub(r"[\s_]+", "-", text.strip().lower())
    t = re.sub(r"-?(international|unported|generic)", "", t)
    t = re.sub(r"-?v?\d+(\.\d+)?$", "", t)
    if "cc0" in t or "public-domain" in t:
        return "cc0"
    if "cc-by-sa" in t:
        return "cc-by-sa"
    if "cc-by" in t and "sa" not in t and "nc" not in t and "nd" not in t:
        return "cc-by"
    return t


def sanitize_filename(name: str) -> str:
    return re.sub(r"[^a-zA-Z0-9_\-\.]", "_", name)


def compute_image_hash(data: bytes) -> str:
    """Hachage MD5 rapide pour détecter et éliminer les doublons stricts."""
    return hashlib.md5(data).hexdigest()


def save_and_resize_image(data: bytes, dest: Path, max_side: int = DEFAULT_MAX_SIDE) -> bool:
    """Ouvre l'image en RGB, supprime les métadonnées superflues et redimensionne à max_side."""
    try:
        img = Image.open(io.BytesIO(data)).convert("RGB")
    except Exception:
        return False
    w, h = img.size
    if max(w, h) > max_side:
        img.thumbnail((max_side, max_side), Image.Resampling.LANCZOS)
    dest.parent.mkdir(parents=True, exist_ok=True)
    img.save(dest, "JPEG", quality=DEFAULT_QUALITY, optimize=True)
    return True


class AttributionsManager:
    """Gère la lecture, l'écriture et la déduplication du fichier ATTRIBUTIONS.csv."""

    def __init__(self, out_dir: Path):
        self.csv_path = out_dir / "ATTRIBUTIONS.csv"
        self.seen_paths: set[str] = set()
        self.seen_hashes: set[str] = set()
        self.rows: list[dict[str, str]] = []
        self._load_existing()

    def _load_existing(self):
        if self.csv_path.is_file():
            try:
                with self.csv_path.open("r", encoding="utf-8") as f:
                    reader = csv.DictReader(f)
                    for row in reader:
                        p = row.get("path", "")
                        self.seen_paths.add(p)
                        self.rows.append(row)
            except Exception as e:
                print(f"  [Avertissement] Lecture du fichier ATTRIBUTIONS.csv existant : {e}")

    def add(self, rel_path: str, source: str, license_code: str, attribution: str, url: str) -> None:
        if rel_path in self.seen_paths:
            return
        row = {
            "path": rel_path.replace("\\", "/"),
            "source": source,
            "license": license_code,
            "attribution": attribution,
            "url": url,
        }
        self.rows.append(row)
        self.seen_paths.add(rel_path)

    def flush(self):
        self.csv_path.parent.mkdir(parents=True, exist_ok=True)
        fieldnames = ["path", "source", "license", "attribution", "url"]
        with self.csv_path.open("w", encoding="utf-8", newline="") as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            for r in self.rows:
                writer.writerow(r)


# =================================================================================================
# Module de collecte GBIF
# =================================================================================================

def fetch_gbif_records_for_taxon(scientific_name: str, target_count: int, per_user: int = 4) -> list[dict[str, Any]]:
    """
    Interroge l'API GBIF Occurrences pour un taxon donné avec filtres d'images et licences libres.
    """
    records: list[dict[str, Any]] = []
    user_counts: dict[str, int] = {}
    offset = 0
    limit = 100

    # Codes de licence acceptés par GBIF correspondant à CC0, CC-BY, CC-BY-SA
    # Dans GBIF : CC0_1_0, CC_BY_4_0, CC_BY_SA_4_0
    licenses_gbif = ["CC0_1_0", "CC_BY_4_0", "CC_BY_SA_4_0"]

    session = requests.Session()

    while len(records) < target_count and offset < 2000:
        params: dict[str, Any] = {
            "scientificName": scientific_name,
            "mediaType": "StillImage",
            "limit": limit,
            "offset": offset,
        }
        # Multi-paramètre pour les licences
        url_params = urllib.parse.urlencode(params, doseq=True)
        lic_params = "&".join(f"license={lic}" for lic in licenses_gbif)
        url = f"{GBIF_API_OCCURRENCE}?{url_params}&{lic_params}"

        try:
            resp = session.get(url, timeout=30)
            if resp.status_code != 200:
                break
            data = resp.json()
        except Exception:
            break

        results = data.get("results", [])
        if not results:
            break

        for item in results:
            occ_id = str(item.get("key", ""))
            # Identifiant observateur / collecteur pour découpage sans fuite
            recorded_by = item.get("recordedBy") or item.get("datasetKey") or "gbif"
            user_id = sanitize_filename(str(hash(recorded_by) % 1_000_000))

            if user_counts.get(user_id, 0) >= per_user:
                continue

            # Trouver le média photo avec licence valide
            media_list = item.get("media", [])
            chosen_media = None
            for m in media_list:
                m_type = m.get("type", "").lower()
                m_format = m.get("format", "").lower()
                if "image" in m_type or "image" in m_format or "jpeg" in m_format or "png" in m_format:
                    chosen_media = m
                    break

            if not chosen_media or not chosen_media.get("identifier"):
                continue

            raw_license = chosen_media.get("license") or item.get("license", "")
            norm_lic = normalize_license(raw_license)
            if norm_lic not in ACCEPTED_LICENSES:
                continue

            user_counts[user_id] = user_counts.get(user_id, 0) + 1
            records.append({
                "id": occ_id,
                "user_id": user_id,
                "scientific_name": scientific_name,
                "image_url": chosen_media["identifier"],
                "license": norm_lic,
                "attribution": chosen_media.get("rightsHolder") or recorded_by,
            })

            if len(records) >= target_count:
                break

        offset += limit
        time.sleep(0.3)

    return records


def download_gbif_dataset(out_dir: Path, per_class: int = 150, max_workers: int = 8) -> None:
    """Télécharge les images GBIF pour chaque classe Sillon configurée."""
    print(f"\n=== Lancement de la collecte GBIF -> {out_dir} (cible ~{per_class} img/classe) ===")
    attributions = AttributionsManager(out_dir)

    for class_name, taxa in GBIF_TAXA.items():
        print(f"\n[GBIF] Classe '{class_name}' ({len(taxa)} taxons cibles)...")
        target_per_taxon = max(15, per_class // len(taxa))
        all_records = []

        for taxon in taxa:
            recs = fetch_gbif_records_for_taxon(taxon, target_count=target_per_taxon)
            all_records.extend(recs)
            print(f"  - {taxon}: {len(recs)} enregistrements trouvés")

        # Téléchargement multithread
        download_jobs = []
        for rec in all_records:
            dest_name = f"u{rec['user_id']}_gbif{rec['id']}.jpg"
            dest_path = out_dir / class_name / dest_name
            download_jobs.append((class_name, dest_path, rec))

        success = 0
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            session = requests.Session()

            def fetch_job(job):
                c_name, d_path, r = job
                rel_path = f"{c_name}/{d_path.name}"
                if d_path.exists():
                    return rel_path, r, True
                try:
                    res = session.get(r["image_url"], timeout=20)
                    if res.status_code == 200 and save_and_resize_image(res.content, d_path):
                        return rel_path, r, True
                except Exception:
                    pass
                return rel_path, r, False

            futures = [executor.submit(fetch_job, j) for j in download_jobs]
            for fut in as_completed(futures):
                rel_path, r, ok = fut.result()
                if ok:
                    success += 1
                    attributions.add(rel_path, "gbif", r["license"], r["attribution"], r["image_url"])

        print(f"  -> {success} images enregistrées pour '{class_name}'.")
        attributions.flush()


# =================================================================================================
# Module de collecte iNaturalist étendue
# =================================================================================================

def fetch_inat_records(taxon_id: int, extra: dict, target: int, per_user: int = 3) -> list[dict[str, Any]]:
    records, per_user_count, id_below = [], {}, None
    session = requests.Session()
    while len(records) < target:
        params: dict[str, Any] = {
            "taxon_id": taxon_id,
            "photo_license": "cc0,cc-by,cc-by-sa",
            "photos": "true",
            "per_page": 200,
            "order_by": "id",
            "order": "desc",
            **extra,
        }
        if "captive" not in extra:
            params["quality_grade"] = "research,needs_id"
        if id_below:
            params["id_below"] = id_below
        try:
            resp = session.get(INAT_API_OBSERVATIONS, params=params, timeout=45)
            if resp.status_code != 200:
                break
            results = resp.json().get("results", [])
        except Exception:
            break

        time.sleep(1.0)  # Politesse envers l'API iNaturalist (~1 req/s)
        if not results:
            break

        for obs in results:
            id_below = obs["id"]
            user_id = obs["user"]["id"]
            photo = next((p for p in obs.get("photos", []) if p.get("license_code")), None)
            if not photo or per_user_count.get(user_id, 0) >= per_user:
                continue
            norm_lic = normalize_license(photo["license_code"])
            if norm_lic not in ACCEPTED_LICENSES:
                continue

            per_user_count[user_id] = per_user_count.get(user_id, 0) + 1
            records.append({
                "obs_id": obs["id"],
                "user_id": user_id,
                "url": photo["url"].replace("/square.", "/medium."),
                "license": norm_lic,
                "attribution": photo.get("attribution", ""),
            })
            if len(records) >= target:
                break
    return records


def download_inat_dataset(out_dir: Path, per_class: int = 150, max_workers: int = 6) -> None:
    print(f"\n=== Lancement de la collecte iNaturalist étendue -> {out_dir} ===")
    attributions = AttributionsManager(out_dir)

    for class_name, sources in INAT_EXTENDED_SOURCES.items():
        print(f"\n[iNaturalist] Classe '{class_name}' ({len(sources)} sources)...")
        target_per_source = max(10, per_class // len(sources))
        all_records = []
        for taxon_id, taxon_name, extra in sources:
            recs = fetch_inat_records(taxon_id, extra, target_per_source)
            all_records.extend(recs)
            print(f"  - {taxon_name} (id {taxon_id}): {len(recs)} obs")

        download_jobs = []
        for rec in all_records:
            dest_name = f"u{rec['user_id']}_inat{rec['obs_id']}.jpg"
            dest_path = out_dir / class_name / dest_name
            download_jobs.append((class_name, dest_path, rec))

        success = 0
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            session = requests.Session()

            def fetch_job(job):
                c_name, d_path, r = job
                rel_path = f"{c_name}/{d_path.name}"
                if d_path.exists():
                    return rel_path, r, True
                try:
                    res = session.get(r["url"], timeout=20)
                    if res.status_code == 200 and save_and_resize_image(res.content, d_path):
                        return rel_path, r, True
                except Exception:
                    pass
                return rel_path, r, False

            futures = [executor.submit(fetch_job, j) for j in download_jobs]
            for fut in as_completed(futures):
                rel_path, r, ok = fut.result()
                if ok:
                    success += 1
                    attributions.add(rel_path, "inaturalist", r["license"], r["attribution"], r["url"])

        print(f"  -> {success} images enregistrées pour '{class_name}'.")
        attributions.flush()


# =================================================================================================
# Module d'ingestion et remappage de jeux externes / Kaggle (PlantWild, PlantDoc, Roboflow, etc.)
# =================================================================================================

KEYWORDS_MAP: list[tuple[list[str], str]] = [
    # Mildiou (Late blight, downy mildew)
    (["late blight", "phytophthora", "downy mildew", "pseudoperonospora", "plasmopara"], "mildiou"),
    # Oïdium (Powdery mildew)
    (["powdery mildew", "oidium", "erysiphe", "podosphaera"], "oidium"),
    # Alternariose (Early blight)
    (["early blight", "alternaria"], "alternariose"),
    # Rouille (Rust)
    (["rust", "puccinia", "uromyces"], "rouille"),
    # Botrytis (Grey mold / Pourriture grise)
    (["botrytis", "gray mold", "grey mold"], "botrytis"),
    # Pucerons (Aphids)
    (["aphid", "aphids", "puceron", "pucerons"], "pucerons"),
    # Altises (Flea beetle)
    (["flea beetle", "altise", "altises", "phyllotreta"], "altises"),
    # Limaces / Escargots (Slugs / Snails)
    (["slug", "slugs", "limace", "limaces", "snail"], "limaces"),
    # Piéride du chou (Cabbage worm / butterfly)
    (["cabbage worm", "pieris", "pieride", "caterpillar"], "pieride_chou"),
    # Doryphore (Colorado potato beetle)
    (["colorado beetle", "colorado potato beetle", "doryphore", "leptinotarsa"], "doryphore"),
    # Sain (Healthy)
    (["healthy", "sain", "health"], "sain"),
    # Autre maladie foliaire (Septoria, leaf spot, mosaic, virus, bacterial)
    (["septoria", "leaf spot", "bacterial spot", "mosaic", "yellow virus", "blight", "scab"], "autre"),
]


def classify_folder_or_name(text: str) -> str | None:
    """Déduit la classe Sillon à partir du nom d'un dossier ou fichier externe."""
    normalized = re.sub(r"[^a-z0-9]+", " ", text.lower()).strip()
    for keywords, target_class in KEYWORDS_MAP:
        for kw in keywords:
            if re.search(r"\b" + re.escape(kw) + r"\b", normalized) or kw in normalized:
                return target_class
    return None


def ingest_external_dataset(
    source_root: Path,
    out_dir: Path,
    source_name: str,
    default_license: str = "cc-by",
    max_images_per_class: int = 500,
) -> None:
    """
    Ingère un dossier d'images brut (ex. jeu de données Kaggle), mappe les dossiers
    aux classes Sillon, vérifie la licence, convertit et structure le jeu pour train_sillon.py.
    """
    print(f"\n=== Ingestion du jeu externe '{source_name}' depuis {source_root} -> {out_dir} ===")
    norm_lic = normalize_license(default_license)
    if norm_lic not in ACCEPTED_LICENSES:
        print(f"Erreur : licence '{default_license}' refusée. Doit être parmi {ACCEPTED_LICENSES}", file=sys.stderr)
        return

    attributions = AttributionsManager(out_dir)
    image_exts = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
    count_per_class: dict[str, int] = {c: 0 for c in SILLON_CLASSES}

    for item in sorted(source_root.rglob("*")):
        if not item.is_file() or item.suffix.lower() not in image_exts:
            continue

        # Déterminer la classe par les dossiers parents ou le nom de fichier
        target_class = None
        for p in reversed(item.relative_to(source_root).parts):
            target_class = classify_folder_or_name(p)
            if target_class:
                break

        if not target_class or target_class not in SILLON_CLASSES:
            continue

        if count_per_class[target_class] >= max_images_per_class:
            continue

        try:
            data = item.read_bytes()
        except Exception:
            continue

        # Déduplication par MD5
        h = compute_image_hash(data)
        if h in attributions.seen_hashes:
            continue
        attributions.seen_hashes.add(h)

        idx = count_per_class[target_class]
        # Format u<source_user>_<name>.jpg pour le split sans fuite par observateur
        dest_filename = f"u{source_name}_{idx:05d}.jpg"
        dest_path = out_dir / target_class / dest_filename

        if save_and_resize_image(data, dest_path):
            rel_path = f"{target_class}/{dest_filename}"
            attributions.add(rel_path, source_name, norm_lic, f"Dataset {source_name} contributor", str(item.name))
            count_per_class[target_class] += 1

    print("\nBilan de l'ingestion :")
    for c, cnt in count_per_class.items():
        if cnt > 0:
            print(f"  - {c}: {cnt} images")
    attributions.flush()
    print(f"Fichier ATTRIBUTIONS.csv mis à jour : {attributions.csv_path}")


# =================================================================================================
# Module de vérification de conformité pour train_sillon.py --extra-field
# =================================================================================================

def verify_extra_field_compliance(folder: Path) -> bool:
    """Vérifie la stricte conformité d'un dossier avec train_sillon.py --extra-field."""
    print(f"\n=== Vérification de conformité pour : {folder} ===")
    if not folder.is_dir():
        print(f"Erreur : '{folder}' n'est pas un dossier existant.", file=sys.stderr)
        return False

    attributions_file = folder / "ATTRIBUTIONS.csv"
    if not attributions_file.is_file():
        print(f"ERREUR : ATTRIBUTIONS.csv manquant dans {folder}.", file=sys.stderr)
        return False

    with attributions_file.open("r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        if not {"path", "license"}.issubset(set(reader.fieldnames or [])):
            print("ERREUR : Colonnes 'path' et 'license' obligatoires dans ATTRIBUTIONS.csv.", file=sys.stderr)
            return False
        entries = {row["path"].replace("\\", "/"): normalize_license(row["license"]) for row in reader}

    refused_licenses = {lic for lic in entries.values() if lic not in ACCEPTED_LICENSES}
    if refused_licenses:
        print(f"ERREUR : Licences non autorisées détectées : {refused_licenses}", file=sys.stderr)
        return False

    images = [p for p in folder.rglob("*") if p.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}]
    if not images:
        print("ERREUR : Aucune image trouvée.", file=sys.stderr)
        return False

    missing_attr = [p for p in images if p.relative_to(folder).as_posix() not in entries]
    if missing_attr:
        print(f"ERREUR : {len(missing_attr)} image(s) sans ligne dans ATTRIBUTIONS.csv (ex: {missing_attr[0]}).", file=sys.stderr)
        return False

    # Comptage par classe
    by_class: dict[str, int] = {}
    for p in images:
        c = p.parent.name
        by_class[c] = by_class.get(c, 0) + 1

    print("SUCCÈS : Dossier 100% conforme pour '--extra-field' !")
    print(f"  Total images validées : {len(images)}")
    for c, n in sorted(by_class.items()):
        print(f"  - {c} : {n} images")
    return True


# =================================================================================================
# CLI Principal
# =================================================================================================

def main():
    parser = argparse.ArgumentParser(
        description="Expansion et amélioration du jeu de données pour l'IA Sillon",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--out", type=Path, default=Path("dataset_expanded"), help="Dossier de destination")
    parser.add_argument("--gbif", action="store_true", help="Activer la collecte GBIF")
    parser.add_argument("--inat", action="store_true", help="Activer la collecte iNaturalist étendue")
    parser.add_argument("--per-class", type=int, default=150, help="Nombre cible d'images par classe")
    parser.add_argument("--ingest", type=Path, help="Dossier d'un jeu externe (ex. Kaggle) à ingérer")
    parser.add_argument("--source-name", type=str, default="external", help="Nom du jeu externe pour l'attribution")
    parser.add_argument("--default-license", type=str, default="cc-by", help="Licence du jeu ingéré (cc0, cc-by, cc-by-sa)")
    parser.add_argument("--verify", type=Path, help="Vérifier la conformité d'un dossier pour train_sillon.py --extra-field")

    args = parser.parse_args()

    if args.verify:
        success = verify_extra_field_compliance(args.verify)
        sys.exit(0 if success else 1)

    if args.ingest:
        ingest_external_dataset(
            source_root=args.ingest,
            out_dir=args.out,
            source_name=args.source_name,
            default_license=args.default_license,
            max_images_per_class=args.per_class,
        )

    if args.gbif:
        download_gbif_dataset(out_dir=args.out, per_class=args.per_class)

    if args.inat:
        download_inat_dataset(out_dir=args.out, per_class=args.per_class)

    if not (args.gbif or args.inat or args.ingest or args.verify):
        parser.print_help()


if __name__ == "__main__":
    main()
