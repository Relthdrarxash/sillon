package fr.pilmil.sillon.model

import kotlinx.serialization.Serializable

@Serializable
enum class CompanionBenefitType(val frenchLabel: String) {
    PEST_REPELLENT("Répulsif / Confusion olfactive"),
    NITROGEN_FIXATION("Fixation symbiotique de l'azote"),
    POLLINATOR_ATTRACTOR("Attraction des pollinisateurs et auxiliaires"),
    GROWTH_STIMULATOR("Stimulation de croissance et vigueur"),
    SHADE_OR_GROUND_COVER("Couverture du sol et maintien de l'humidité"),
    PHYSICAL_SUPPORT("Support physique / tuteurage naturel"),
    DYNAMIC_ACCUMULATOR("Accumulation dynamique de minéraux"),
    GENERAL_SYNERGY("Synergie agronomique générale"),
}

@Serializable
enum class CompanionRiskType(val frenchLabel: String) {
    ALLELOPATHY("Allélopathie négative / inhibition"),
    SHARED_DISEASE_OR_PEST("Sensibilité aux mêmes maladies ou ravageurs"),
    NUTRIENT_COMPETITION("Concurrence nutritive sévère"),
    CANOPY_OR_ROOT_SHADOW("Compétition racinaire ou ombrage excessif"),
    FLAVOR_DEGRADATION("Altération de la saveur"),
    GENERAL_ANTAGONISM("Antagonisme agronomique général"),
}

@Serializable
data class CompanionRelation(
    val targetPlantId: String,
    val rationale: String,
    val benefitType: CompanionBenefitType? = null,
    val riskType: CompanionRiskType? = null,
) {
    constructor(targetPlantId: String, rationale: String, benefitType: CompanionBenefitType) :
        this(targetPlantId, rationale, benefitType = benefitType, riskType = null)

    constructor(targetPlantId: String, rationale: String, riskType: CompanionRiskType) :
        this(targetPlantId, rationale, benefitType = null, riskType = riskType)

    val isBeneficial: Boolean get() = benefitType != null
    val isHarmful: Boolean get() = riskType != null
}
