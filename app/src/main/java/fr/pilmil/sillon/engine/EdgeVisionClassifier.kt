package fr.pilmil.sillon.engine

import android.graphics.Bitmap
import fr.pilmil.sillon.data.DiseaseCatalog
import fr.pilmil.sillon.model.Disease

data class DiseasePrediction(
    val disease: Disease,
    val confidence: Float,
    val dominantSymptomObserved: String,
)

data class VisionAnalysisResult(
    val predictions: List<DiseasePrediction>,
    val chlorosisRatio: Float,
    val necrosisRatio: Float,
    val powderyCoverageRatio: Float,
    val isHealthy: Boolean,
    val needsManualTriage: Boolean,
)

/**
 * Classifieur de vision sur appareil (Edge Vision).
 * 100% hors-ligne : analyse locale par chrominance spectrale & inférence embarquée.
 * Zéro octet envoyé sur le réseau.
 */
class EdgeVisionClassifier {

    companion object {
        const val CONFIDENCE_THRESHOLD = 0.65f

        fun rgb(r: Int, g: Int, b: Int): Int {
            return (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
        }
    }

    /**
     * Analyse une image de feuille potagère (Bitmap standard 224x224 RGB).
     */
    fun analyzeLeafBitmap(bitmap: Bitmap): VisionAnalysisResult {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return analyzePixels(pixels, width, height)
    }

    /**
     * Analyse directe d'un buffer de pixels ARGB.
     * Calcule les indices chromatiques agronomiques (ExG, Chlorose, Nécrose, Oïdium)
     * et établit un diagnostic différentiel probabiliste.
     */
    fun analyzePixels(pixels: IntArray, width: Int, height: Int): VisionAnalysisResult {
        val totalPixels = (width * height).coerceAtLeast(1)

        var greenCount = 0
        var yellowCount = 0
        var brownNecrosisCount = 0
        var whitePowderCount = 0

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            // Index agronomique d'excès de vert (Excess Green Index : 2G - R - B)
            val exg = 2 * g - r - b
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            if (exg > 30 && g > r && g > b) {
                greenCount++
            } else if (r > 130 && g > 130 && b < 100 && kotlin.math.abs(r - g) < 30) {
                // Jaunissement / Chlorose foliaire
                yellowCount++
            } else if (r in 60..140 && g in 40..100 && b < 60 && r > g) {
                // Taches brunes / Nécrose / Mildiou
                brownNecrosisCount++
            } else if (luminance > 190 && kotlin.math.abs(r - g) < 15 && kotlin.math.abs(g - b) < 15) {
                // Feutrage blanc / Oïdium
                whitePowderCount++
            }
        }

        val chlorosisRatio = yellowCount.toFloat() / totalPixels
        val necrosisRatio = brownNecrosisCount.toFloat() / totalPixels
        val powderyRatio = whitePowderCount.toFloat() / totalPixels
        val greenRatio = greenCount.toFloat() / totalPixels

        val predictions = mutableListOf<DiseasePrediction>()

        // Diagnostic Oïdium (feutrage blanc)
        if (powderyRatio > 0.08f) {
            val confidence = (powderyRatio * 3.5f).coerceIn(0.40f, 0.92f)
            predictions.add(
                DiseasePrediction(
                    disease = DiseaseCatalog.OIDIUM,
                    confidence = confidence,
                    dominantSymptomObserved = "Présence anormale de feutrage blanc sur le limbe (${(powderyRatio * 100).toInt()}% de la surface)",
                )
            )
        }

        // Diagnostic Mildiou (taches nécrotiques brunes + marges chlorotiques)
        if (necrosisRatio > 0.06f) {
            val confidence = (necrosisRatio * 4.0f + chlorosisRatio * 1.5f).coerceIn(0.45f, 0.94f)
            predictions.add(
                DiseasePrediction(
                    disease = DiseaseCatalog.MILDIOU,
                    confidence = confidence,
                    dominantSymptomObserved = "Taches nécrotiques huileuses et dessèchement (${(necrosisRatio * 100).toInt()}% de surface nécrosée)",
                )
            )
        }

        // Diagnostic Cul Noir (Nécrose apicale) si nécrose localisée
        if (necrosisRatio in 0.04f..0.20f && chlorosisRatio < 0.05f) {
            predictions.add(
                DiseasePrediction(
                    disease = DiseaseCatalog.CUL_NOIR,
                    confidence = 0.60f,
                    dominantSymptomObserved = "Nécrose ferme localisée sans chlorose diffuse (possible carence en calcium / arrosage irrégulier)",
                )
            )
        }

        // Diagnostic Chlorose / Carence / Pucerons
        if (chlorosisRatio > 0.12f && necrosisRatio < 0.04f) {
            predictions.add(
                DiseasePrediction(
                    disease = DiseaseCatalog.PUCERONS,
                    confidence = 0.58f,
                    dominantSymptomObserved = "Jaunissement et crispation foliaire (possible piqûre de sève ou carence)",
                )
            )
        }

        val topConfidence = predictions.maxOfOrNull { it.confidence } ?: 0f
        val isHealthy = greenRatio > 0.65f && necrosisRatio < 0.03f && powderyRatio < 0.03f
        val needsManualTriage = topConfidence < CONFIDENCE_THRESHOLD && !isHealthy

        return VisionAnalysisResult(
            predictions = predictions.sortedByDescending { it.confidence },
            chlorosisRatio = chlorosisRatio,
            necrosisRatio = necrosisRatio,
            powderyCoverageRatio = powderyRatio,
            isHealthy = isHealthy,
            needsManualTriage = needsManualTriage,
        )
    }
}
