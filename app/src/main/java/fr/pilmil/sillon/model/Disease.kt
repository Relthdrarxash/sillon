package fr.pilmil.sillon.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlantOrgan(val frenchLabel: String) {
    LEAF("Feuille"),
    STEM("Tige"),
    FRUIT("Fruit"),
    ROOT("Racine"),
    FLOWER("Fleur"),
    SEEDLING("Jeune pousse / Semis"),
}

@Serializable
enum class PathogenType(val frenchLabel: String) {
    FUNGUS("Maladie cryptogamique (champignon)"),
    PEST("Ravageur (insecte, mollusque, acarien)"),
    PHYSIOLOGICAL("Désordre physiologique / abiotique"),
    BACTERIA("Bactérie"),
    VIRUS("Virus"),
}

@Serializable
data class Disease(
    val id: String,
    val name: String,
    val latinName: String? = null,
    val pathogenType: PathogenType,
    val affectedOrgans: List<PlantOrgan>,
    val symptoms: List<String>,
    val causes: List<String>,
    val organicRemedies: List<String>,
    val targetFamilies: List<BotanicalFamily> = emptyList(),
    val preventiveMeasures: List<String> = emptyList(),
)
