package fr.pilmil.sillon.engine

import fr.pilmil.sillon.data.DiseaseCatalog
import fr.pilmil.sillon.model.BotanicalFamily
import fr.pilmil.sillon.model.Disease
import fr.pilmil.sillon.model.PlantOrgan
import fr.pilmil.sillon.model.Vegetable
import kotlin.math.min

data class DiagnosticMatch(
    val disease: Disease,
    val score: Int,
    val confidencePercent: Int,
    val organMatched: Boolean,
    val familyMatched: Boolean,
    val matchingSymptoms: List<String>,
    val organicRemedies: List<String>,
    val preventiveMeasures: List<String>,
)

class DiagnosticEngine(
    private val diseases: List<Disease> = DiseaseCatalog.ALL_DISEASES,
) {

    /**
     * Arbre de diagnostic multicritère combinant l'organe atteint, les symptômes observés
     * et la famille botanique de la plante pour identifier les affections probables et leurs traitements bio.
     */
    fun diagnose(
        organ: PlantOrgan? = null,
        symptoms: List<String> = emptyList(),
        family: BotanicalFamily? = null,
        catalog: List<Disease> = diseases,
    ): List<DiagnosticMatch> {
        val normalizedQueries = symptoms.map { normalize(it) }.filter { it.isNotBlank() }

        val matches = catalog.mapNotNull { disease ->
            var score = 0
            val matchingSymptoms = mutableListOf<String>()

            // 1. Évaluation de l'organe touché
            val organMatched = if (organ != null) {
                if (disease.affectedOrgans.contains(organ)) {
                    score += 30
                    true
                } else {
                    score -= 25 // Pénalité si l'organe n'est jamais touché par cette affection
                    false
                }
            } else {
                false
            }

            // 2. Évaluation de la famille botanique cible
            val familyMatched = if (family != null) {
                when {
                    disease.targetFamilies.contains(family) -> {
                        score += 35
                        true
                    }
                    disease.targetFamilies.isEmpty() -> {
                        // Ravageur ou pathogène généraliste (ex. limaces, pucerons, fonte des semis)
                        score += 15
                        true
                    }
                    else -> {
                        // Maladie hautement spécifique à d'autres familles
                        score -= 30
                        false
                    }
                }
            } else {
                false
            }

            // 3. Correspondance textuelle et sémantique des symptômes
            val allDiseaseText = buildString {
                append(disease.name).append(" ")
                disease.symptoms.forEach { append(it).append(" ") }
                disease.causes.forEach { append(it).append(" ") }
            }
            val normDiseaseText = normalize(allDiseaseText)

            for (query in normalizedQueries) {
                val queryTokens = query.split("\\s+".toRegex()).filter { it.length >= 3 }
                var matchedTokensCount = 0

                for (token in queryTokens) {
                    if (normDiseaseText.contains(token)) {
                        matchedTokensCount++
                    }
                }

                for (symptom in disease.symptoms) {
                    val normSymptom = normalize(symptom)
                    if (queryTokens.any { normSymptom.contains(it) } || normSymptom.contains(query)) {
                        if (!matchingSymptoms.contains(symptom)) {
                            matchingSymptoms.add(symptom)
                        }
                    }
                }

                score += matchedTokensCount * 15

                // Bonus pour correspondance exacte
                if (normDiseaseText.contains(query)) {
                    score += 25
                }
            }

            if (score > 0) {
                // Normalisation du score en pourcentage de confiance (plafond à 98% sans analyse labo)
                val confidence = min(98, score)
                DiagnosticMatch(
                    disease = disease,
                    score = score,
                    confidencePercent = confidence,
                    organMatched = organMatched,
                    familyMatched = familyMatched,
                    matchingSymptoms = matchingSymptoms,
                    organicRemedies = disease.organicRemedies,
                    preventiveMeasures = disease.preventiveMeasures,
                )
            } else {
                null
            }
        }

        return matches.sortedByDescending { it.score }
    }

    /**
     * Diagnostic rapide à partir d'une plante du potager.
     */
    fun diagnoseVegetable(
        vegetable: Vegetable,
        organ: PlantOrgan? = null,
        symptoms: List<String> = emptyList(),
    ): List<DiagnosticMatch> {
        return diagnose(
            organ = organ,
            symptoms = symptoms,
            family = vegetable.family,
        )
    }

    private fun normalize(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input.lowercase().trim()) {
            sb.append(
                when (ch) {
                    'à', 'á', 'â', 'ã', 'ä', 'å' -> 'a'
                    'ç' -> 'c'
                    'è', 'é', 'ê', 'ë' -> 'e'
                    'ì', 'í', 'î', 'ï' -> 'i'
                    'ñ' -> 'n'
                    'ò', 'ó', 'ô', 'õ', 'ö' -> 'o'
                    'ù', 'ú', 'û', 'ü' -> 'u'
                    'ý', 'ÿ' -> 'y'
                    else -> ch
                },
            )
        }
        return sb.toString()
    }
}
