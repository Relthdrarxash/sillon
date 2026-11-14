package fr.pilmil.sillon.engine

import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.model.CompanionBenefitType
import fr.pilmil.sillon.model.CompanionRelation
import fr.pilmil.sillon.model.CompanionRiskType
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.ParcelSlot
import fr.pilmil.sillon.model.Vegetable
import kotlin.math.abs
import kotlin.math.max

enum class AdjacencyType(val frenchLabel: String) {
    ORTHOGONAL("Orthogonale (directe)"),
    DIAGONAL("Diagonale"),
}

data class SlotPosition(val row: Int, val col: Int)

data class CompanionSynergy(
    val slot1: SlotPosition,
    val slot2: SlotPosition,
    val vegetable1: Vegetable,
    val vegetable2: Vegetable,
    val benefitType: CompanionBenefitType,
    val rationale: String,
    val adjacency: AdjacencyType,
)

data class CompanionAntagonism(
    val slot1: SlotPosition,
    val slot2: SlotPosition,
    val vegetable1: Vegetable,
    val vegetable2: Vegetable,
    val riskType: CompanionRiskType,
    val rationale: String,
    val adjacency: AdjacencyType,
    val biologicalAlert: String,
)

data class PairEvaluation(
    val vegetable1: Vegetable,
    val vegetable2: Vegetable,
    val isBeneficial: Boolean,
    val isAntagonistic: Boolean,
    val beneficialRelations: List<CompanionRelation>,
    val antagonisticRelations: List<CompanionRelation>,
    val summary: String,
)

data class ParcelEvaluationResult(
    val parcelId: String,
    val synergies: List<CompanionSynergy>,
    val antagonisms: List<CompanionAntagonism>,
    val companionScore: Int,
) {
    val hasAntagonisms: Boolean get() = antagonisms.isNotEmpty()
    val hasSynergies: Boolean get() = synergies.isNotEmpty()
}

class CompanionEngine(
    private val catalogMap: Map<String, Vegetable> = VegetableCatalog.BY_ID,
) {

    /**
     * Évalue l'interaction agronomique entre deux légumes.
     */
    fun evaluatePair(veg1: Vegetable, veg2: Vegetable): PairEvaluation {
        val beneficial = mutableListOf<CompanionRelation>()
        val antagonistic = mutableListOf<CompanionRelation>()

        // Relations définies de veg1 vers veg2
        veg1.goodCompanions.filter { it.targetPlantId == veg2.id }.forEach { beneficial.add(it) }
        veg1.badCompanions.filter { it.targetPlantId == veg2.id }.forEach { antagonistic.add(it) }

        // Relations réciproques définies de veg2 vers veg1
        veg2.goodCompanions.filter { it.targetPlantId == veg1.id }.forEach { rel ->
            if (beneficial.none { it.targetPlantId == rel.targetPlantId && it.rationale == rel.rationale }) {
                beneficial.add(rel)
            }
        }
        veg2.badCompanions.filter { it.targetPlantId == veg1.id }.forEach { rel ->
            if (antagonistic.none { it.targetPlantId == rel.targetPlantId && it.rationale == rel.rationale }) {
                antagonistic.add(rel)
            }
        }

        val summary = when {
            antagonistic.isNotEmpty() ->
                "[Alerte Biologique] Association défavorable entre ${veg1.name} et ${veg2.name} : ${antagonistic.first().rationale}"
            beneficial.isNotEmpty() ->
                "[Synergie] Excellente association entre ${veg1.name} et ${veg2.name} : ${beneficial.first().rationale}"
            else ->
                "Association neutre sans interaction notable entre ${veg1.name} et ${veg2.name}."
        }

        return PairEvaluation(
            vegetable1 = veg1,
            vegetable2 = veg2,
            isBeneficial = beneficial.isNotEmpty(),
            isAntagonistic = antagonistic.isNotEmpty(),
            beneficialRelations = beneficial,
            antagonisticRelations = antagonistic,
            summary = summary,
        )
    }

    /**
     * Évalue l'interaction entre deux identifiants de légumes.
     */
    fun evaluatePair(veg1Id: String, veg2Id: String): PairEvaluation? {
        val veg1 = catalogMap[veg1Id] ?: return null
        val veg2 = catalogMap[veg2Id] ?: return null
        return evaluatePair(veg1, veg2)
    }

    /**
     * Évalue les synergies et antagonismes entre toutes les cases adjacentes d'une parcelle
     * (adjacence horizontale, verticale et diagonale).
     */
    fun evaluateParcel(
        parcel: Parcel,
        customCatalog: Map<String, Vegetable> = catalogMap,
    ): ParcelEvaluationResult {
        val slotMap = parcel.slots.associateBy { SlotPosition(it.row, it.col) }
        val slotsWithVeg = parcel.slots.filter { !it.vegetableId.isNullOrBlank() }

        val synergies = mutableListOf<CompanionSynergy>()
        val antagonisms = mutableListOf<CompanionAntagonism>()

        // Parcourir chaque paire unique de cases adjacentes
        for (i in slotsWithVeg.indices) {
            val s1 = slotsWithVeg[i]
            val veg1Id = s1.vegetableId ?: continue
            val veg1 = customCatalog[veg1Id] ?: continue
            val pos1 = SlotPosition(s1.row, s1.col)

            for (j in (i + 1) until slotsWithVeg.size) {
                val s2 = slotsWithVeg[j]
                val veg2Id = s2.vegetableId ?: continue
                val veg2 = customCatalog[veg2Id] ?: continue
                val pos2 = SlotPosition(s2.row, s2.col)

                val deltaRow = abs(pos1.row - pos2.row)
                val deltaCol = abs(pos1.col - pos2.col)

                // Vérifier l'adjacence dans la grille (distance de Tchebychev = 1)
                if (max(deltaRow, deltaCol) == 1) {
                    val adjacency = if (deltaRow == 0 || deltaCol == 0) {
                        AdjacencyType.ORTHOGONAL
                    } else {
                        AdjacencyType.DIAGONAL
                    }

                    val pairEval = evaluatePair(veg1, veg2)

                    if (pairEval.isBeneficial) {
                        val relation = pairEval.beneficialRelations.first()
                        synergies.add(
                            CompanionSynergy(
                                slot1 = pos1,
                                slot2 = pos2,
                                vegetable1 = veg1,
                                vegetable2 = veg2,
                                benefitType = relation.benefitType ?: CompanionBenefitType.GENERAL_SYNERGY,
                                rationale = relation.rationale,
                                adjacency = adjacency,
                            )
                        )
                    }

                    if (pairEval.isAntagonistic) {
                        val relation = pairEval.antagonisticRelations.first()
                        val riskType = relation.riskType ?: CompanionRiskType.GENERAL_ANTAGONISM
                        val alertMsg = "[Alerte Biologique] Risque (${riskType.frenchLabel}) entre ${veg1.name} (case [${pos1.row},${pos1.col}]) et ${veg2.name} (case [${pos2.row},${pos2.col}]) : ${relation.rationale}"
                        antagonisms.add(
                            CompanionAntagonism(
                                slot1 = pos1,
                                slot2 = pos2,
                                vegetable1 = veg1,
                                vegetable2 = veg2,
                                riskType = riskType,
                                rationale = relation.rationale,
                                adjacency = adjacency,
                                biologicalAlert = alertMsg,
                            )
                        )
                    }
                }
            }
        }

        // Score: chaque synergie apporte +2 points (+3 si orthogonale), chaque conflit retranche -5 points
        val synergyPoints = synergies.sumOf { syn -> if (syn.adjacency == AdjacencyType.ORTHOGONAL) 3 else 2 as Int }
        val antagonismPoints = antagonisms.sumOf { ant -> if (ant.adjacency == AdjacencyType.ORTHOGONAL) 6 else 4 as Int }
        val score = synergyPoints - antagonismPoints

        return ParcelEvaluationResult(
            parcelId = parcel.id,
            synergies = synergies,
            antagonisms = antagonisms,
            companionScore = score,
        )
    }

    /**
     * Retourne la liste des meilleurs compagnons pour un légume donné.
     */
    fun findGoodCompanionsFor(vegId: String): List<Vegetable> {
        val veg = catalogMap[vegId] ?: return emptyList()
        val directIds = veg.goodCompanions.map { it.targetPlantId }.toSet()
        val reverseIds = catalogMap.values
            .filter { other -> other.goodCompanions.any { it.targetPlantId == vegId } }
            .map { it.id }
            .toSet()

        return (directIds + reverseIds).mapNotNull { catalogMap[it] }
    }

    /**
     * Retourne la liste des mauvais compagnons (antagonistes) pour un légume donné.
     */
    fun findBadCompanionsFor(vegId: String): List<Vegetable> {
        val veg = catalogMap[vegId] ?: return emptyList()
        val directIds = veg.badCompanions.map { it.targetPlantId }.toSet()
        val reverseIds = catalogMap.values
            .filter { other -> other.badCompanions.any { it.targetPlantId == vegId } }
            .map { it.id }
            .toSet()

        return (directIds + reverseIds).mapNotNull { catalogMap[it] }
    }
}
