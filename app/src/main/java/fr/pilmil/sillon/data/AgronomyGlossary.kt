package fr.pilmil.sillon.data

/**
 * Entrée du glossaire agronomique : terme technique et explication simple pour les débutants.
 */
data class GlossaryEntry(
    val id: String,
    val title: String,
    val definition: String,
    val patterns: List<String>,
)

data class GlossaryMatch(
    val start: Int,
    val end: Int,
    val entry: GlossaryEntry,
)

/**
 * Glossaire embarqué 100 % hors-ligne pour vulgariser les termes botaniques et agronomiques
 * affichés dans les fiches légumes, les associations et le guide de santé végétale.
 */
object AgronomyGlossary {
    val entries: List<GlossaryEntry> = listOf(
        GlossaryEntry(
            id = "allelopathie",
            title = "Allélopathie",
            definition = "Une plante libère par ses racines ou ses feuilles des substances naturelles qui freinent (ou parfois stimulent) la croissance d'une plante voisine.",
            patterns = listOf("allélopathiques", "allélopathique", "allélopathie"),
        ),
        GlossaryEntry(
            id = "nematodes_galles",
            title = "Nématodes à galles",
            definition = "Vers microscopiques du sol qui piquent les racines et forment de petites boules (galles), empêchant la plante de bien se nourrir.",
            patterns = listOf("nématodes à galles", "vers nématodes", "nématodes", "nématode"),
        ),
        GlossaryEntry(
            id = "nouaison",
            title = "Nouaison",
            definition = "Moment clé où la fleur fanée se transforme en tout petit fruit après avoir été pollinisée par le vent ou les insectes.",
            patterns = listOf("nouaison"),
        ),
        GlossaryEntry(
            id = "cryptogamique",
            title = "Maladie cryptogamique",
            definition = "Maladie causée par un champignon microscopique (comme le mildiou, l'oïdium ou la rouille), souvent favorisée par l'humidité.",
            patterns = listOf("maladies cryptogamiques", "cryptogamiques", "cryptogamique"),
        ),
        GlossaryEntry(
            id = "exsudats",
            title = "Exsudats racinaires",
            definition = "Substances naturelles sécrétées dans la terre par les racines d'une plante (par exemple pour repousser des parasites du sol).",
            patterns = listOf("exsudats racinaires", "exsudats"),
        ),
        GlossaryEntry(
            id = "cuticule",
            title = "Cuticule foliaire",
            definition = "Fine pellicule protectrice à la surface des feuilles : plus elle est solide, mieux la plante résiste au dessèchement et aux champignons.",
            patterns = listOf("cuticule foliaire", "cuticule"),
        ),
        GlossaryEntry(
            id = "symbiose_rhizobienne",
            title = "Symbiose rhizobienne",
            definition = "Entraide entre les racines des pois/haricots et des bactéries du sol (rhizobiums) : ces bactéries forment de petites boules (nodosités) qui captent l'azote de l'air pour nourrir la plante.",
            patterns = listOf("symbiose rhizobienne", "nodosités symbiotiques", "rhizobiums", "nodosités"),
        ),
        GlossaryEntry(
            id = "osmotique",
            title = "Action osmotique",
            definition = "Effet de dessèchement au contact d'un sel minéral (comme le bicarbonate), qui déshydrate et stoppe le champignon en surface.",
            patterns = listOf("osmotique"),
        ),
        GlossaryEntry(
            id = "confusion_olfactive",
            title = "Confusion olfactive",
            definition = "Les odeurs fortes d'une plante voisine (oignon, poireau, œillet d'Inde) brouillent le flair des insectes ravageurs qui peinent alors à repérer leur cible.",
            patterns = listOf("confusion olfactive"),
        ),
        GlossaryEntry(
            id = "aleurodes",
            title = "Aleurodes",
            definition = "Petites « mouches blanches » qui se cachent sous les feuilles, piquent la sève et affaiblissent les légumes (surtout sous serre ou abri).",
            patterns = listOf("aleurodes", "aleurode"),
        ),
        GlossaryEntry(
            id = "doryphores",
            title = "Doryphores",
            definition = "Insectes rayés jaune et noir dont les larves rouges dévorent très vite le feuillage des pommes de terre et des aubergines.",
            patterns = listOf("doryphores", "doryphore"),
        ),
        GlossaryEntry(
            id = "silice",
            title = "Silice",
            definition = "Minéral naturel abondant dans la prêle : pulvérisée sur le feuillage, elle renforce la paroi des feuilles contre les champignons.",
            patterns = listOf("silice"),
        ),
        GlossaryEntry(
            id = "sclerotes",
            title = "Sclérote",
            definition = "Petite masse dure formée par certains champignons pour survivre plusieurs années en dormance dans la terre avant de réattaquer.",
            patterns = listOf("sclérotes", "sclérote"),
        ),
        GlossaryEntry(
            id = "mildiou",
            title = "Mildiou",
            definition = "Champignon très fréquent par temps doux et humide : taches brunes sur les feuilles et pourriture des fruits (tomate, pomme de terre).",
            patterns = listOf("mildiou"),
        ),
        GlossaryEntry(
            id = "oidium",
            title = "Oïdium",
            definition = "Champignon (« maladie du blanc ») qui forme un duvet poudreux blanc sur les feuilles, souvent en fin d'été.",
            patterns = listOf("oïdium"),
        ),
        GlossaryEntry(
            id = "solanacees",
            title = "Solanacées",
            definition = "Famille botanique des tomates, pommes de terre, aubergines, poivrons et piments. Plantes gourmandes qui partagent les mêmes maladies.",
            patterns = listOf("solanacées"),
        ),
        GlossaryEntry(
            id = "brassicacees",
            title = "Brassicacées",
            definition = "Famille des choux, radis, navets, moutarde et roquette. Elles partagent les mêmes ravageurs (altises, piérides du chou).",
            patterns = listOf("brassicacées"),
        ),
        GlossaryEntry(
            id = "apiacees",
            title = "Apiacées",
            definition = "Famille des carottes, panais, persil, céleri, aneth et fenouil, reconnaissables à leurs fleurs en ombrelles.",
            patterns = listOf("apiacées"),
        ),
        GlossaryEntry(
            id = "alliacees",
            title = "Alliacées",
            definition = "Famille de l'ail, de l'oignon, de l'échalote, du poireau et de la ciboulette : riches en composés soufrés qui repoussent divers insectes.",
            patterns = listOf("alliacées"),
        ),
        GlossaryEntry(
            id = "fabacees",
            title = "Fabacées",
            definition = "Famille des pois, haricots, fèves et trèfles (légumineuses) : elles enrichissent naturellement la terre en azote.",
            patterns = listOf("fabacées"),
        ),
        GlossaryEntry(
            id = "cucurbitacees",
            title = "Cucurbitacées",
            definition = "Famille des courgettes, courges, potirons, concombres et melons : plantes vigoureuses qui aiment le compost et l'arrosage au pied.",
            patterns = listOf("cucurbitacées"),
        ),
        GlossaryEntry(
            id = "chenopodiacees",
            title = "Chénopodiacées",
            definition = "Famille des betteraves, épinards et blettes (poirées).",
            patterns = listOf("chénopodiacées"),
        ),
    )

    private val sortedPatterns: List<Pair<String, GlossaryEntry>> = entries
        .flatMap { entry -> entry.patterns.map { it.lowercase() to entry } }
        .sortedByDescending { it.first.length }

    /**
     * Repère les occurrences de termes du glossaire dans [text] sans chevauchement,
     * en privilégiant les expressions les plus longues et en respectant les limites de mots.
     */
    fun findMatches(text: String): List<GlossaryMatch> {
        if (text.isBlank()) return emptyList()
        val lower = text.lowercase()
        val candidates = mutableListOf<GlossaryMatch>()

        for ((pattern, entry) in sortedPatterns) {
            var searchFrom = 0
            while (searchFrom < lower.length) {
                val idx = lower.indexOf(pattern, searchFrom)
                if (idx < 0) break
                val end = idx + pattern.length
                val validStart = idx == 0 || !lower[idx - 1].isLetterOrDigit()
                val validEnd = end == lower.length || !lower[end].isLetterOrDigit()
                if (validStart && validEnd) {
                    val overlaps = candidates.any { existing ->
                        idx < existing.end && end > existing.start
                    }
                    if (!overlaps) {
                        candidates.add(GlossaryMatch(start = idx, end = end, entry = entry))
                    }
                }
                searchFrom = end
            }
        }
        return candidates.sortedBy { it.start }
    }
}
