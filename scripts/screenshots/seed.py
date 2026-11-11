#!/usr/bin/env python3
"""Données d'exemple pour les captures d'écran (jamais utilisées dans l'appli).

Usage : seed.py <epoch_secondes> <potager.json> <prefs.xml>
Écrit un potager plausible (deux parcelles, plantations datées par rapport à « maintenant »)
et les réglages de premier lancement terminé, département d'Ille-et-Vilaine.
"""
import json
import sys

now_ms = int(sys.argv[1]) * 1000
garden_out, prefs_out = sys.argv[2], sys.argv[3]
DAY = 86_400_000


def slot(row, col, veg, days_ago):
    return {"row": row, "col": col, "vegetableId": veg, "plantedDate": now_ms - days_ago * DAY}


carre = [
    slot(0, 0, "tomate", 9), slot(0, 1, "basilic", 9), slot(0, 2, "tomate", 9), slot(0, 3, "tagete", 14),
    slot(1, 0, "carotte", 30), slot(1, 1, "oignon", 40), slot(1, 2, "carotte", 30), slot(1, 3, "fenouil", 12),
    slot(2, 0, "laitue", 21), slot(2, 1, "radis", 16), slot(2, 2, "laitue", 21), slot(2, 3, "radis", 16),
    slot(3, 0, "haricot_nain", 3), slot(3, 1, "fraise", 60), slot(3, 2, "ciboulette", 60), slot(3, 3, "courgette", 6),
]
planche = [
    slot(0, 0, "pomme_de_terre", 35), slot(0, 1, "pois", 28), slot(0, 2, "poireau", 45), slot(0, 3, "betterave", 20),
    slot(1, 0, "radis", 10), slot(1, 1, "epinard", 25), slot(1, 2, "blette", 18), slot(1, 3, "laitue", 12),
]
garden = {
    "id": "potager_demo",
    "name": "Mon potager",
    "frostDate": {"month": 4, "day": 15},
    "parcels": [
        {"id": "parcel_carre", "name": "Carré des tomates", "type": "RAISED_BED_SQUARES", "rows": 4, "cols": 4, "slots": carre},
        {"id": "parcel_planche", "name": "Planche du fond", "type": "ROW_BED", "rows": 2, "cols": 4, "slots": planche},
    ],
}
with open(garden_out, "w", encoding="utf-8") as f:
    json.dump(garden, f, ensure_ascii=False, indent=2)

with open(prefs_out, "w", encoding="utf-8") as f:
    f.write("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n"
            "    <boolean name=\"onboarded\" value=\"true\" />\n"
            "    <string name=\"department\">35</string>\n"
            "    <boolean name=\"sowing_reminders\" value=\"false\" />\n</map>\n")
print("potager et réglages écrits")
