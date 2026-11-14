package fr.pilmil.sillon.screenshots

import fr.pilmil.sillon.data.FrenchFrostCatalog
import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.model.FrostDate
import fr.pilmil.sillon.model.Garden
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.ParcelSlot
import fr.pilmil.sillon.model.ParcelType
import java.time.LocalDate
import java.time.ZoneId

/** Données FABRIQUÉES pour les captures : aucun export réel, date du jour figée pour des images reproductibles. */
object Sample {
    val today: LocalDate = LocalDate.of(2026, 5, 18)

    /** Calvados (14) : dernières gelées vers le 20 avril. */
    const val DEPARTMENT_CODE = "14"
    val frostDate = FrostDate(4, 20)

    /** Une case plantée : `daysAgo` jours avant [today]. */
    private fun planted(row: Int, col: Int, vegetableId: String, daysAgo: Long) = ParcelSlot(
        row = row,
        col = col,
        vegetableId = vegetableId,
        plantedDate = today.minusDays(daysAgo).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )

    /** Construit une grille depuis des identifiants de plantes, ligne par ligne ; les dates s'étalent sur 14 à 24 jours. */
    private fun parcel(id: String, name: String, size: Int, ids: List<String>) = Parcel(
        id = id,
        name = name,
        type = ParcelType.RAISED_BED_SQUARES,
        rows = size,
        cols = size,
        slots = ids.mapIndexed { i, veg -> planted(i / size, i % size, veg, 14L + (i * 7L) % 11L) },
    )

    // Tomate/basilic, tomate/oeillet d'Inde, capucine/courgette : synergies. Laitue au-dessus du persil : alerte.
    private val carreSud = parcel(
        "sud", "Carré sud", 4,
        listOf(
            "tomate", "basilic", "tomate", "oeillet_d_inde",
            "carotte", "tomate", "basilic", "capucine",
            "courgette", "capucine", "laitue", "radis",
            "fraise", "souci", "persil", "carotte",
        ),
    )

    private val carreAromates = parcel(
        "aromates", "Carré aromates", 3,
        listOf(
            "ciboulette", "carotte", "persil",
            "thym", "tomate", "basilic",
            "sauge", "persil", "ciboulette",
        ),
    )

    // Haricot nain contre poireau : alerte voisinage. Pois, carotte, radis, laitue : synergies.
    private val carreLegumineuses = parcel(
        "legumineuses", "Carré des légumineuses", 4,
        listOf(
            "haricot_nain", "carotte", "pois", "laitue",
            "radis", "haricot_nain", "carotte", "radis",
            "carotte", "poireau", "laitue", "pois",
            "haricot_nain", "carotte", "radis", "carotte",
        ),
    )

    /** Avancements variés (jeunes pousses, plants moyens, prêts à récolter) pour montrer les plantes à tous les stades. */
    private val stages = listOf(0.95f, 0.55f, 0.85f, 0.15f, 0.4f, 1f, 0.7f, 0.3f, 0.9f, 0.6f, 0.2f, 0.8f, 0.5f, 1f, 0.35f, 0.75f)

    private fun aged(parcel: Parcel): Parcel = parcel.copy(
        slots = parcel.slots.mapIndexed { i, slot ->
            val veg = VegetableCatalog.ALL_VEGETABLES.first { it.id == slot.vegetableId }
            planted(slot.row, slot.col, veg.id, (veg.daysToHarvest.first * stages[(i * 5 + 3) % stages.size]).toLong())
        },
    )

    /** Même jardin que [garden], mais plantes d'âges différents : sert à la capture de la vue en perspective. */
    val gardenVivant = Garden(
        id = "demo",
        name = "Potager de démonstration",
        frostDate = frostDate,
        parcels = listOf(carreSud, carreAromates, carreLegumineuses).map(::aged),
    )

    val garden = Garden(
        id = "demo",
        name = "Potager de démonstration",
        frostDate = frostDate,
        parcels = listOf(carreSud, carreAromates, carreLegumineuses),
    )

    /** Guadeloupe (971) : début de la saison de plantation au 1er juin ; le 3 juin, plusieurs plantations s'ouvrent. */
    const val DEPARTMENT_CODE_TROPICAL = "971"
    val todayTropical: LocalDate = LocalDate.of(2026, 6, 3)
    val seasonStartTropical: FrostDate = FrenchFrostCatalog.getByCode(DEPARTMENT_CODE_TROPICAL)!!.seasonStart

    // Grandes cultures au fond (papayers), tubercules, puis cultures basses devant.
    private val carreTropical = parcel(
        "tropical", "Carré tropical", 4,
        listOf(
            "papayer", "igname", "igname", "papayer",
            "pois_d_angole", "taro_madere", "taro_madere", "pois_d_angole",
            "mais_doux", "patate_douce", "patate_douce", "haricot_nain",
            "patate_douce", "taro_madere", "concombre", "patate_douce",
        ),
    )

    val gardenTropical = Garden(
        id = "demo-971",
        name = "Potager de démonstration (Guadeloupe)",
        frostDate = seasonStartTropical,
        parcels = listOf(aged(carreTropical)),
    )
}
