#!/usr/bin/env python3
"""
Collecte de photos de terrain iNaturalist pour les classes Sillon sans jeu de données public.

    python collect_data.py                      # -> dataset_raw/<classe>/u<observateur>_inat<obs>.jpg
    python train_sillon.py --plantdoc <PlantDoc> --classed dataset_raw

- Licences CC0, CC-BY et CC-BY-SA uniquement ; une ligne d'attribution par image dans
  dataset_raw/ATTRIBUTIONS.csv (à publier avec tout modèle entraîné dessus).
- Le préfixe u<id> est l'observateur : train_sillon.py s'en sert pour que les photos d'un même
  jardin ne soient jamais à la fois à l'entraînement et au test. Le nombre de photos par
  observateur et par classe est plafonné pour la diversité.
- Images réduites à 320 px côté long : le corpus complet tient en moins de 200 Mo.
- Les noms de dossiers sont des labels de train_sillon.py (CLASSES).

Limite connue : pour les ravageurs, iNaturalist montre surtout l'animal, pas les dégâts sur la feuille.
"""

import argparse
import csv
import io
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import requests
from PIL import Image

API = "https://api.inaturalist.org/v1/observations"
LICENSES = "cc0,cc-by,cc-by-sa"
MAX_SIDE = 320
LARVA = {"term_id": 1, "term_value_id": 6}

# Label Sillon -> taxons iNaturalist (id, nom, filtres supplémentaires).
# « captive » : plantes cultivées, qu'iNaturalist range hors du grade « research ».
INAT_SOURCES = {
    "sain": [
        (51737, "Solanum lycopersicum", {"captive": "true"}),
        (1599236, "Cucurbita pepo", {"captive": "true"}),
        (53858, "Solanum tuberosum", {"captive": "true"}),
    ],
    "mildiou": [
        (53860, "Phytophthora infestans", {}),
        (196477, "Pseudoperonospora cubensis", {}),
    ],
    "oidium": [
        (1578546, "Podosphaera xanthii", {}),
        (1542754, "Erysiphe pisi", {}),
        (333884, "Golovinomyces", {}),
    ],
    "alternariose": [(360210, "Alternaria solani", {})],
    "pucerons": [
        (326910, "Brevicoryne brassicae", {}),
        (1584763, "Aphis fabae", {}),
        (53859, "Macrosiphum euphorbiae", {}),
        (243745, "Myzus persicae", {}),
    ],
    "altises": [(178465, "Phyllotreta", {})],
    "rouille": [(1489211, "Puccinia allii", {})],
    "limaces": [
        (54583, "Arion vulgaris", {}),
        (124432, "Deroceras reticulatum", {}),
        (62470, "Limax maximus", {}),
    ],
    "pieride_chou": [
        (55401, "Pieris brassicae", LARVA),
        (55626, "Pieris rapae", LARVA),
    ],
    "botrytis": [(324263, "Botrytis cinerea", {})],
    "doryphore": [(84778, "Leptinotarsa decemlineata", {})],
    # Adventices et auxiliaires qu'il ne faut jamais prendre pour une maladie ou un ravageur
    "hors_sujet": [
        (53021, "Convolvulus arvensis", {}),
        (967514, "Taraxacum", {}),
        (1087592, "Stellaria media", {}),
        (51702, "Coccinella septempunctata", {}),
        (81545, "Lumbricus terrestris", {}),
    ],
}


def save_resized(data: bytes, dest: Path) -> bool:
    try:
        img = Image.open(io.BytesIO(data)).convert("RGB")
    except Exception:
        return False
    img.thumbnail((MAX_SIDE, MAX_SIDE))
    dest.parent.mkdir(parents=True, exist_ok=True)
    img.save(dest, "JPEG", quality=90)
    return True


def fetch_inat_records(taxon_id: int, extra: dict, target: int, per_user: int) -> list:
    """Parcourt les observations (plus récentes d'abord) jusqu'à `target` photos retenues."""
    records, per_user_count, id_below = [], {}, None
    while len(records) < target:
        params = {
            "taxon_id": taxon_id,
            "photo_license": LICENSES,
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
        resp = requests.get(API, params=params, timeout=60)
        resp.raise_for_status()
        results = resp.json()["results"]
        time.sleep(1.0)  # politesse envers l'API iNaturalist (~1 req/s)
        if not results:
            break
        for obs in results:
            id_below = obs["id"]
            user_id = obs["user"]["id"]
            photo = next((p for p in obs.get("photos", []) if p.get("license_code")), None)
            if not photo or per_user_count.get(user_id, 0) >= per_user:
                continue
            per_user_count[user_id] = per_user_count.get(user_id, 0) + 1
            records.append({
                "obs_id": obs["id"],
                "user_id": user_id,
                "url": photo["url"].replace("/square.", "/medium."),
                "license": photo["license_code"],
                "attribution": photo.get("attribution", ""),
            })
            if len(records) >= target:
                break
    return records


def download_inat(out_dir: Path, per_class: int, per_user: int, writer) -> None:
    session = requests.Session()

    def grab(job):
        class_name, taxon_name, rec = job
        dest = out_dir / class_name / f"u{rec['user_id']}_inat{rec['obs_id']}.jpg"
        if dest.exists():
            return job, True
        try:
            r = session.get(rec["url"], timeout=60)
            r.raise_for_status()
            return job, save_resized(r.content, dest)
        except Exception:
            return job, False

    for class_name, taxa in INAT_SOURCES.items():
        quota = per_class // len(taxa) + 1
        jobs = []
        for taxon_id, taxon_name, extra in taxa:
            recs = fetch_inat_records(taxon_id, extra, quota, per_user)
            print(f"  {class_name:14s} {taxon_name:28s} {len(recs)} photos")
            jobs += [(class_name, taxon_name, r) for r in recs]
        ok = 0
        with ThreadPoolExecutor(max_workers=4) as pool:
            for (cls, taxon_name, rec), success in pool.map(grab, jobs):
                if success:
                    ok += 1
                    writer.writerow([
                        f"{cls}/u{rec['user_id']}_inat{rec['obs_id']}.jpg", cls, taxon_name,
                        f"https://www.inaturalist.org/observations/{rec['obs_id']}",
                        rec["license"], rec["attribution"],
                    ])
        print(f"[✓] {class_name}: {ok} images")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Collecte de photos de terrain iNaturalist pour Sillon")
    parser.add_argument("--out", type=Path, default=Path("dataset_raw"))
    parser.add_argument("--per-class", type=int, default=600)
    parser.add_argument("--per-user", type=int, default=3)
    args = parser.parse_args()

    args.out.mkdir(parents=True, exist_ok=True)
    attribution_path = args.out / "ATTRIBUTIONS.csv"
    is_new = not attribution_path.exists()
    with open(attribution_path, "a", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        if is_new:
            writer.writerow(["path", "class", "source_taxon", "source_url", "license", "attribution"])
        download_inat(args.out, args.per_class, args.per_user, writer)
