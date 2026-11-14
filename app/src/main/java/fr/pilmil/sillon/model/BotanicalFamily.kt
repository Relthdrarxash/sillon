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
    ASPARAGACEAE("Asparagacées", NutrientRequirement.LIGHT, 5),

    // Familles tropicales (pack outre-mer). Exigences : Guide tropical CIRAD/RITA FT17, FIT Igname (Chambre
    // d'agriculture Martinique 2014), fiche papaye PIP/COLEACP. Pauses : sourcée pour les Dioscoréacées
    // (« ne pas se succéder », 3 ans), par convention pour les autres.
    CONVOLVULACEAE("Convolvulacées", NutrientRequirement.MEDIUM, 2),
    DIOSCOREACEAE("Dioscoréacées", NutrientRequirement.HEAVY, 3),
    ARACEAE("Aracées", NutrientRequirement.HEAVY, 2),
    CARICACEAE("Caricacées", NutrientRequirement.HEAVY, 2);

    val label: String get() = frenchName
    val frenchLabel: String get() = frenchName
}
