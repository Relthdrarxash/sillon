package fr.pilmil.sillon.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Description du modèle embarqué, lue dans assets/models/sillon_disease.json
 * (produit par tools/ml/train_sillon.py, à côté de sillon_disease.tflite).
 */
@Serializable
data class PhotoModelSpec(
    val format: Int,
    val input: Input,
    val classes: List<ModelClass>,
    val threshold: Float,
    val synthetic: Boolean = false,
) {
    @Serializable
    data class Input(val width: Int, val height: Int, val channels: Int = 3, val order: String = "RGB")

    @Serializable
    data class ModelClass(val label: String, val catalogId: String? = null)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): PhotoModelSpec = json.decodeFromString(serializer(), text)
    }
}

/** Ce que l'appli peut dire honnêtement d'une photo. */
sealed interface PhotoVerdict {
    /** Une maladie connue du catalogue, avec une confiance au-dessus du seuil. */
    data class Disease(val catalogId: String, val confidence: Float) : PhotoVerdict

    /** Feuille saine, avec une confiance au-dessus du seuil. */
    data class Healthy(val confidence: Float) : PhotoVerdict

    /**
     * Pas assez sûr, ou un problème que le modèle ne sait pas nommer :
     * on renvoie vers la clé de diagnostic, avec les pistes les plus probables s'il y en a.
     */
    data class Unsure(val leads: List<Pair<String, Float>>) : PhotoVerdict
}

object PhotoDiagnosis {

    const val HEALTHY_LABEL = "sain"
    private const val MIN_LEAD = 0.20f

    /**
     * Pixels ARGB d'une image déjà mise à la taille du modèle -> tenseur NHWC float RGB 0..255,
     * exactement ce que le script d'entraînement fournit au modèle.
     */
    fun toInput(argb: IntArray, width: Int, height: Int): FloatArray {
        require(argb.size == width * height) { "taille de pixels incohérente" }
        val out = FloatArray(width * height * 3)
        for (i in argb.indices) {
            val p = argb[i]
            out[i * 3] = ((p shr 16) and 0xFF).toFloat()
            out[i * 3 + 1] = ((p shr 8) and 0xFF).toFloat()
            out[i * 3 + 2] = (p and 0xFF).toFloat()
        }
        return out
    }

    /** Probabilités du modèle -> verdict, avec le seuil calibré sur des photos de terrain. */
    fun interpret(probabilities: FloatArray, spec: PhotoModelSpec): PhotoVerdict {
        require(probabilities.size == spec.classes.size) { "le modèle et sa description ne correspondent pas" }
        val ranked = probabilities.indices.sortedByDescending { probabilities[it] }
        val best = ranked.first()
        val confidence = probabilities[best]
        val bestClass = spec.classes[best]
        if (confidence >= spec.threshold) {
            if (bestClass.label == HEALTHY_LABEL) return PhotoVerdict.Healthy(confidence)
            bestClass.catalogId?.let { return PhotoVerdict.Disease(it, confidence) }
        }
        val leads = ranked
            .filter { probabilities[it] >= MIN_LEAD }
            .mapNotNull { i -> spec.classes[i].catalogId?.let { it to probabilities[i] } }
        return PhotoVerdict.Unsure(leads)
    }
}
