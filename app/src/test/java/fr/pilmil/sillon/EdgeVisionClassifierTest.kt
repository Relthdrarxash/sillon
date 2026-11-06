package fr.pilmil.sillon

import fr.pilmil.sillon.engine.EdgeVisionClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EdgeVisionClassifierTest {

    private lateinit var classifier: EdgeVisionClassifier

    @Before
    fun setUp() {
        classifier = EdgeVisionClassifier()
    }

    @Test
    fun testHealthyLeafClassification() {
        // Crée une image factice 100x100 100% vert vif
        val pixels = IntArray(100 * 100) { EdgeVisionClassifier.rgb(34, 150, 45) }

        val result = classifier.analyzePixels(pixels, 100, 100)
        assertTrue("Une feuille verte sans tache doit être considérée saine", result.isHealthy)
        assertFalse("Pas besoin de triage manuel pour une feuille saine", result.needsManualTriage)
    }

    @Test
    fun testPowderyMildewDetection() {
        // Crée une image factice avec feutrage blanc étendu
        val pixels = IntArray(100 * 100)
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                val color = if (x < 30) {
                    // Poudre blanche oïdium
                    EdgeVisionClassifier.rgb(230, 230, 230)
                } else {
                    EdgeVisionClassifier.rgb(34, 139, 34)
                }
                pixels[y * 100 + x] = color
            }
        }

        val result = classifier.analyzePixels(pixels, 100, 100)
        val oidiumPred = result.predictions.find { it.disease.id == "oidium" }
        assertTrue("L'oïdium doit être détecté lors d'un feutrage blanc", oidiumPred != null)
        assertTrue("La confiance oïdium doit être significative", oidiumPred!!.confidence >= 0.65f)
    }

    @Test
    fun testLowConfidenceFallbackToManualTriage() {
        // Image ambiguë (fond gris-brun clair indéterminé)
        val pixels = IntArray(100 * 100) { EdgeVisionClassifier.rgb(110, 100, 90) }

        val result = classifier.analyzePixels(pixels, 100, 100)
        assertTrue("En cas d'ambiguïté visuelle, le système doit basculer vers le triage manuel", result.needsManualTriage)
    }

    @Test
    fun testLateBlightMildewDetection() {
        // Crée une image factice avec taches nécrotiques brunes huileuses typiques du mildiou
        val pixels = IntArray(100 * 100)
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                val color = if (x < 25) {
                    // Brun nécrotique (r=90, g=55, b=30)
                    EdgeVisionClassifier.rgb(90, 55, 30)
                } else {
                    // Vert limbe
                    EdgeVisionClassifier.rgb(40, 140, 40)
                }
                pixels[y * 100 + x] = color
            }
        }

        val result = classifier.analyzePixels(pixels, 100, 100)
        val mildiouPred = result.predictions.find { it.disease.id == "mildiou" }
        assertTrue("Le mildiou doit être détecté en présence de nécrose brune", mildiouPred != null)
        assertTrue("La confiance du mildiou doit être supérieure à 60%", mildiouPred!!.confidence >= 0.60f)
        assertFalse("La feuille ne doit pas être qualifiée de saine", result.isHealthy)
    }
}
