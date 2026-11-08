package fr.pilmil.sillon

import fr.pilmil.sillon.data.DiseaseCatalog
import fr.pilmil.sillon.engine.PhotoDiagnosis
import fr.pilmil.sillon.engine.PhotoModelSpec
import fr.pilmil.sillon.engine.PhotoVerdict
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoDiagnosisTest {

    // Même forme que le JSON produit par tools/ml/train_sillon.py.
    private val spec = PhotoModelSpec.parse(
        """
        {
          "format": 1, "created": "2026-11-08",
          "input": {"width": 224, "height": 224, "channels": 3, "order": "RGB", "range": [0, 255], "layout": "NHWC"},
          "output": "probabilities",
          "classes": [
            {"label": "sain", "catalogId": null},
            {"label": "mildiou", "catalogId": "mildiou"},
            {"label": "oidium", "catalogId": "oidium"},
            {"label": "alternariose", "catalogId": "alternariose"},
            {"label": "autre", "catalogId": null}
          ],
          "temperature": 1.4, "threshold": 0.7, "metrics": {}, "synthetic": false
        }
        """.trimIndent(),
    )

    @Test
    fun everyModelClassPointsToAnExistingSheet() {
        spec.classes.mapNotNull { it.catalogId }.forEach { assertNotNull("Fiche manquante : $it", DiseaseCatalog.getById(it)) }
    }

    @Test
    fun pixelsBecomeRgbFloatsInModelOrder() {
        val input = PhotoDiagnosis.toInput(intArrayOf(0xFF102030.toInt(), 0xFFFFFFFF.toInt()), 2, 1)
        assertArrayEquals(floatArrayOf(16f, 32f, 48f, 255f, 255f, 255f), input, 0f)
    }

    @Test
    fun confidentDiseaseOpensItsSheet() {
        val verdict = PhotoDiagnosis.interpret(floatArrayOf(0.05f, 0.8f, 0.05f, 0.05f, 0.05f), spec)
        assertEquals(PhotoVerdict.Disease("mildiou", 0.8f), verdict)
    }

    @Test
    fun confidentHealthyLeaf() {
        assertTrue(PhotoDiagnosis.interpret(floatArrayOf(0.9f, 0.03f, 0.03f, 0.02f, 0.02f), spec) is PhotoVerdict.Healthy)
    }

    @Test
    fun belowThresholdGivesLeadsInsteadOfAnAnswer() {
        val verdict = PhotoDiagnosis.interpret(floatArrayOf(0.1f, 0.45f, 0.05f, 0.35f, 0.05f), spec)
        assertEquals(PhotoVerdict.Unsure(listOf("mildiou" to 0.45f, "alternariose" to 0.35f)), verdict)
    }

    @Test
    fun otherProblemNeverPretendsToKnow() {
        val verdict = PhotoDiagnosis.interpret(floatArrayOf(0.02f, 0.03f, 0.02f, 0.03f, 0.9f), spec)
        assertEquals(PhotoVerdict.Unsure(emptyList()), verdict)
    }
}
