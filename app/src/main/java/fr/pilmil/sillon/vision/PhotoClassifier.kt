package fr.pilmil.sillon.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import fr.pilmil.sillon.engine.PhotoDiagnosis
import fr.pilmil.sillon.engine.PhotoModelSpec
import fr.pilmil.sillon.engine.PhotoVerdict
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/** Issue d'une analyse : on distingue la photo illisible, le modèle en échec et l'analyse annulée. */
sealed interface PhotoResult {
    data class Success(val verdict: PhotoVerdict) : PhotoResult
    /** La photo n'a pas pu être lue ou décodée. */
    data class DecodeFailed(val cause: Throwable?) : PhotoResult
    /** Le modèle a échoué sur une photo pourtant lue. */
    data class InferenceFailed(val cause: Throwable) : PhotoResult
    /** Le classifieur a été fermé (écran quitté) avant ou pendant l'analyse : rien à afficher. */
    data object Cancelled : PhotoResult
}

/**
 * Diagnostic photo sur le téléphone : la photo ne quitte jamais l'appareil.
 * Le modèle est optionnel : tant qu'aucun sillon_disease.tflite n'est embarqué dans les assets,
 * [load] renvoie null et l'appli n'affiche pas la fonction.
 */
class PhotoClassifier private constructor(
    private val interpreter: Interpreter,
    val spec: PhotoModelSpec,
) : AutoCloseable {

    // Protège l'interpréteur : close() attend la fin d'une inférence en cours, et classify() refuse un interpréteur fermé.
    private val lock = Any()
    private var closed = false

    /** Décode la photo puis l'analyse ; ne lève jamais d'exception. */
    fun analyse(context: Context, uri: Uri): PhotoResult {
        val bitmap = try {
            decode(context, uri)
        } catch (e: Exception) {
            return PhotoResult.DecodeFailed(e)
        }
        return try {
            classify(bitmap)?.let { PhotoResult.Success(it) } ?: PhotoResult.Cancelled
        } catch (e: Exception) {
            PhotoResult.InferenceFailed(e)
        }
    }

    /** Verdict du modèle, ou null si le classifieur a été fermé entre-temps. */
    fun classify(bitmap: Bitmap): PhotoVerdict? {
        val w = spec.input.width
        val h = spec.input.height
        val square = centerSquare(bitmap)
        val scaled = Bitmap.createScaledBitmap(square, w, h, true)
        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)
        val values = PhotoDiagnosis.toInput(pixels, w, h)

        val input = ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder())
        input.asFloatBuffer().put(values)
        val output = Array(1) { FloatArray(spec.classes.size) }
        synchronized(lock) {
            if (closed) return null
            interpreter.run(input, output)
        }
        return PhotoDiagnosis.interpret(output[0], spec)
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            interpreter.close()
        }
    }

    companion object {
        private const val MODEL = "models/sillon_disease.tflite"
        private const val SPEC = "models/sillon_disease.json"

        fun isAvailable(context: Context): Boolean =
            runCatching { context.assets.list("models")?.contains("sillon_disease.tflite") == true }.getOrDefault(false)

        /** Charge le modèle, ou null s'il est absent, synthétique ou illisible. */
        fun load(context: Context): PhotoClassifier? = runCatching {
            if (!isAvailable(context)) return null
            val spec = PhotoModelSpec.parse(context.assets.open(SPEC).bufferedReader().use { it.readText() })
            // Un modèle de vérification du pipeline ne doit jamais servir à de vrais diagnostics.
            if (spec.synthetic) return null
            PhotoClassifier(Interpreter(mapModel(context), Interpreter.Options().setNumThreads(2)), spec)
        }.getOrNull()

        private fun mapModel(context: Context): MappedByteBuffer =
            context.assets.openFd(MODEL).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }

        /**
         * Lit une photo en la réduisant tout de suite (inutile de charger 12 Mpx pour une image de 224 px),
         * et la remet à l'endroit selon son orientation EXIF. Lève une exception si elle est illisible.
         */
        fun decode(context: Context, uri: Uri, target: Int = 512): Bitmap {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            (resolver.openInputStream(uri) ?: error("Photo introuvable")).use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("Format d'image non reconnu")
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = (resolver.openInputStream(uri) ?: error("Photo introuvable")).use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Décodage de la photo impossible")
            // Une orientation EXIF illisible n'empêche pas l'analyse : on garde la photo telle quelle.
            val orientation = runCatching {
                resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
            return applyOrientation(bitmap, orientation)
        }

        private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
                else -> return bitmap
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        private fun centerSquare(bitmap: Bitmap): Bitmap {
            val side = minOf(bitmap.width, bitmap.height)
            return Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        }
    }
}
