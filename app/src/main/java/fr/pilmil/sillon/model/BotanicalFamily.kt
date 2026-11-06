package fr.pilmil.sillon.model

import kotlinx.serialization.Serializable

@Serializable
enum class NutrientRequirement(val frenchLabel: String) {
    HEAVY("Exigeant"),
    MEDIUM("Moyennement exigeant"),
    LIGHT("Peu exigeant"),
    LIGHT_FIXER("Fixateur d'azote / Améliorant"),
}

@Serializable
enum class BotanicalFamily(
    val frenchName: String,
    val nutrientRequirement: NutrientRequirement,
    val recommendedPauseYears: Int,
) {
    SOLANACEAE("Solanacées", NutrientRequirement.HEAVY, 4),
    FABACEAE("Fabacées", NutrientRequirement.LIGHT_FIXER, 3),
    BRASSICACEAE("Brassicacées", NutrientRequirement.HEAVY, 4),
    APIACEAE("Apiacées", NutrientRequirement.MEDIUM, 3),
    CUCURBITACEAE("Cucurbitacées", NutrientRequirement.HEAVY, 3),
    ALLIACEAE("Alliacées", NutrientRequirement.LIGHT, 3),
    ASTERACEAE("Astéracées", NutrientRequirement.MEDIUM, 3),
    CHENOPODIACEAE("Chénopodiacées", NutrientRequirement.MEDIUM, 3),
    LAMIACEAE("Lamiacées", NutrientRequirement.LIGHT, 2),
    POACEAE("Poacées", NutrientRequirement.MEDIUM, 3),
    BORAGINACEAE("Boraginacées", NutrientRequirement.LIGHT, 2),
    TROPAEOLACEAE("Tropaéolacées", NutrientRequirement.LIGHT, 2),
    VALERIANACEAE("Valérianacées", NutrientRequirement.LIGHT, 2),
    MALVACEAE("Malvacées", NutrientRequirement.MEDIUM, 3),
    POLYGONACEAE("Polygonacées", NutrientRequirement.MEDIUM, 3),
    ROSACEAE("Rosacées", NutrientRequirement.MEDIUM, 3),
    ASPARAGACEAE("Asparagacées", NutrientRequirement.LIGHT, 5);

    val label: String get() = frenchName
    val frenchLabel: String get() = frenchName
}
