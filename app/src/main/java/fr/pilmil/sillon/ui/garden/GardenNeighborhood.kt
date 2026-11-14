package fr.pilmil.sillon.ui.garden

import fr.pilmil.sillon.model.CompanionBenefitType
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.Vegetable

/** Affinités et alertes d'une case, d'après ses quatre voisins orthogonaux. */
internal data class CellNeighborhood(val hasGood: Boolean, val hasBad: Boolean)

/** Une relation détectée entre deux plantes voisines, pour la carte « Voisinage ». */
internal data class NeighborLink(
    val first: Vegetable,
    val second: Vegetable,
    val positive: Boolean,
    val protects: Boolean,
)

private fun neighborsOf(parcel: Parcel, vegetables: List<Vegetable>, r: Int, c: Int): List<Vegetable> = listOfNotNull(
    parcel.getSlot(r - 1, c)?.vegetableId?.let { id -> vegetables.find { it.id == id } },
    parcel.getSlot(r + 1, c)?.vegetableId?.let { id -> vegetables.find { it.id == id } },
    parcel.getSlot(r, c - 1)?.vegetableId?.let { id -> vegetables.find { it.id == id } },
    parcel.getSlot(r, c + 1)?.vegetableId?.let { id -> vegetables.find { it.id == id } },
)

/** Même logique que la vue à plat : une case a une affinité (ou une alerte) si l'un de ses voisins figure dans ses listes. */
internal fun neighborhoodOf(parcel: Parcel, vegetables: List<Vegetable>): Map<Pair<Int, Int>, CellNeighborhood> {
    val result = HashMap<Pair<Int, Int>, CellNeighborhood>()
    for (r in 0 until parcel.rows) {
        for (c in 0 until parcel.cols) {
            val veg = vegetables.find { it.id == parcel.getSlot(r, c)?.vegetableId }
            val neighborVegs = neighborsOf(parcel, vegetables, r, c)
            val hasGood = veg != null && neighborVegs.any { neighbor ->
                veg.goodCompanions.any { it.targetPlantId == neighbor.id }
            }
            val hasBad = veg != null && neighborVegs.any { neighbor ->
                veg.badCompanions.any { it.targetPlantId == neighbor.id }
            }
            result[r to c] = CellNeighborhood(hasGood, hasBad)
        }
    }
    return result
}

/** Paires de plantes voisines concernées, sans doublon (A-B et B-A comptent pour une seule). */
internal fun neighborLinks(parcel: Parcel, vegetables: List<Vegetable>): List<NeighborLink> {
    val seen = HashSet<String>()
    val links = ArrayList<NeighborLink>()
    for (r in 0 until parcel.rows) {
        for (c in 0 until parcel.cols) {
            val veg = vegetables.find { it.id == parcel.getSlot(r, c)?.vegetableId } ?: continue
            for (neighbor in neighborsOf(parcel, vegetables, r, c)) {
                if (neighbor.id == veg.id) continue
                val good = veg.goodCompanions.firstOrNull { it.targetPlantId == neighbor.id }
                val bad = veg.badCompanions.firstOrNull { it.targetPlantId == neighbor.id }
                for ((relation, positive) in listOf(good to true, bad to false)) {
                    if (relation == null) continue
                    val key = listOf(veg.id, neighbor.id).sorted().joinToString("|") + positive
                    if (seen.add(key)) {
                        links += NeighborLink(
                            first = veg,
                            second = neighbor,
                            positive = positive,
                            protects = relation.benefitType == CompanionBenefitType.PEST_REPELLENT,
                        )
                    }
                }
            }
        }
    }
    return links
}
