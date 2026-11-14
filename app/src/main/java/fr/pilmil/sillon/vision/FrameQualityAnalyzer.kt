package fr.pilmil.sillon.vision

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * Statut de qualité de cadrage de la feuille pour le diagnostic IA.
 */
enum class FrameQualityStatus {
    /** Optimal : nette, bien éclairée, feuille bien présente dans le cadre. */
    OPTIMAL,
    /** Avertissement : acceptable mais conditions sous-optimales (légèrement sombre, un peu loin). */
    WARNING,
    /** Inexploitable : floue, trop sombre, éblouissement ou absence de végétation. */
    UNUSABLE,
}

data class FrameQuality(
    val status: FrameQualityStatus,
    val sharpness: Float,
    val brightness: Float,
    val plantCoverage: Float,
    val tip: String,
)

/**
 * Analyseur de flux vidéo CameraX temps-réel (30 FPS).
 * Évalue la netteté (opérateur laplacien), l'exposition (luminance Y) et la couverture végétale (Excess Green YUV->RGB)
 * dans le tiers central de l'image (zone de visée).
 */
class FrameQualityAnalyzer(
    private val onQualityEvaluated: (FrameQuality) -> Unit,
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val yPlane = image.planes.getOrNull(0)
        val uPlane = image.planes.getOrNull(1)
        val vPlane = image.planes.getOrNull(2)

        if (yPlane == null) {
            image.close()
            return
        }

        try {
            val quality = evaluate(image, yPlane, uPlane, vPlane)
            onQualityEvaluated(quality)
        } finally {
            image.close()
        }
    }

    private fun evaluate(
        image: ImageProxy,
        yPlane: ImageProxy.PlaneProxy,
        uPlane: ImageProxy.PlaneProxy?,
        vPlane: ImageProxy.PlaneProxy?,
    ): FrameQuality {
        val width = image.width
        val height = image.height
        val yBuffer = yPlane.buffer
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        val uBuffer = uPlane?.buffer
        val uRowStride = uPlane?.rowStride ?: 0
        val uPixelStride = uPlane?.pixelStride ?: 1

        val vBuffer = vPlane?.buffer
        val vRowStride = vPlane?.rowStride ?: 0
        val vPixelStride = vPlane?.pixelStride ?: 1

        // Zone centrale : tiers central (33% à 66%)
        val startX = width / 4
        val endX = (width * 3) / 4
        val startY = height / 4
        val endY = (height * 3) / 4

        val step = 4 // Échantillonnage 1 pixel sur 4 pour minimiser la consommation CPU
        var totalLuminance = 0L
        var totalLaplacian = 0L
        var plantPixelCount = 0
        var sampledPixels = 0

        fun getY(x: Int, y: Int): Int {
            val index = y * yRowStride + x * yPixelStride
            return if (index < yBuffer.capacity()) yBuffer.get(index).toInt() and 0xFF else 0
        }

        for (y in startY + 1 until endY - 1 step step) {
            for (x in startX + 1 until endX - 1 step step) {
                val center = getY(x, y)
                totalLuminance += center

                // Laplacien discret : |4*C - (H + B + G + D)|
                val up = getY(x, y - 1)
                val down = getY(x, y + 1)
                val left = getY(x - 1, y)
                val right = getY(x + 1, y)
                val lap = abs(4 * center - up - down - left - right)
                totalLaplacian += lap

                // Estimation de végétation verte (si plans U et V disponibles)
                if (uBuffer != null && vBuffer != null) {
                    val uvX = x / 2
                    val uvY = y / 2
                    val uIndex = uvY * uRowStride + uvX * uPixelStride
                    val vIndex = uvY * vRowStride + uvX * vPixelStride

                    if (uIndex < uBuffer.capacity() && vIndex < vBuffer.capacity()) {
                        val u = (uBuffer.get(uIndex).toInt() and 0xFF) - 128
                        val v = (vBuffer.get(vIndex).toInt() and 0xFF) - 128

                        // Conversion rapide YUV -> G
                        val g = center - 0.344f * u - 0.714f * v
                        val r = center + 1.402f * v
                        val b = center + 1.772f * u
                        val exG = 2 * g - r - b
                        if (exG > 15f) {
                            plantPixelCount++
                        }
                    }
                }
                sampledPixels++
            }
        }

        if (sampledPixels == 0) {
            return FrameQuality(FrameQualityStatus.UNUSABLE, 0f, 0f, 0f, "Positionnez la caméra")
        }

        val avgBrightness = totalLuminance.toFloat() / sampledPixels
        val avgSharpness = totalLaplacian.toFloat() / sampledPixels
        val plantCoverage = if (uBuffer != null) plantPixelCount.toFloat() / sampledPixels else 0.5f

        val (status, tip) = when {
            avgSharpness < 7.5f -> FrameQualityStatus.UNUSABLE to "Image floue : stabilisez l'appareil"
            avgBrightness < 45.0f -> FrameQualityStatus.UNUSABLE to "Trop sombre : éclairez la feuille"
            avgBrightness > 225.0f -> FrameQualityStatus.UNUSABLE to "Éblouissement : évitez le contre-jour"
            plantCoverage < 0.12f -> FrameQualityStatus.WARNING to "Rapprochez-vous de la feuille"
            avgSharpness < 13.0f || plantCoverage < 0.25f -> FrameQualityStatus.WARNING to "Qualité moyenne : centrer la feuille"
            else -> FrameQualityStatus.OPTIMAL to "Cadre optimal : analyse prête !"
        }

        return FrameQuality(
            status = status,
            sharpness = avgSharpness,
            brightness = avgBrightness,
            plantCoverage = plantCoverage,
            tip = tip,
        )
    }
}
