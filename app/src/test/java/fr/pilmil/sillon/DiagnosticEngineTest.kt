package fr.pilmil.sillon

import fr.pilmil.sillon.data.DiseaseCatalog
import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.engine.DiagnosticEngine
import fr.pilmil.sillon.model.BotanicalFamily
import fr.pilmil.sillon.model.PlantOrgan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiagnosticEngineTest {

    private lateinit var engine: DiagnosticEngine

    @Before
    fun setUp() {
        engine = DiagnosticEngine()
    }

    @Test
    fun testDiagnoseMildiouOnTomatoLeaf() {
        // Diagnostic sur feuille de tomate avec taches huileuses et duvet blanc
        val results = engine.diagnose(
            organ = PlantOrgan.LEAF,
            symptoms = listOf("taches brunes huileuses", "duvet blanc"),
            family = BotanicalFamily.SOLANACEAE,
        )

        assertFalse("Des résultats doivent être trouvés", results.isEmpty())
        val topMatch = results.first()
        assertEquals("mildiou", topMatch.disease.id)
        assertTrue(topMatch.organMatched)
        assertTrue(topMatch.familyMatched)
        assertTrue(topMatch.confidencePercent >= 70)
        assertTrue(topMatch.organicRemedies.any { it.contains("bouillie bordelaise", ignoreCase = true) || it.contains("prêle", ignoreCase = true) })
    }

    @Test
    fun testDiagnoseCulNoirOnFruit() {
        // Diagnostic sur fruit de tomate avec tache noire déprimée à l'extrémité
        val results = engine.diagnose(
            organ = PlantOrgan.FRUIT,
            symptoms = listOf("tache circulaire noire deprimee opposee queue"),
            family = BotanicalFamily.SOLANACEAE,
        )

        assertFalse(results.isEmpty())
        val topMatch = results.first()
        assertEquals("cul_noir", topMatch.disease.id)
        assertTrue(topMatch.organicRemedies.any { it.contains("arrosage", ignoreCase = true) || it.contains("paillage", ignoreCase = true) })
    }

    @Test
    fun testDiagnoseCarrotFlyOnRoots() {
        val results = engine.diagnose(
            organ = PlantOrgan.ROOT,
            symptoms = listOf("galeries sinueuses brunâtres", "pourriture racine"),
            family = BotanicalFamily.APIACEAE,
        )

        assertFalse(results.isEmpty())
        val topMatch = results.first()
        assertEquals("mouche_carotte", topMatch.disease.id)
        assertTrue(topMatch.organicRemedies.any { it.contains("poireau", ignoreCase = true) || it.contains("filet", ignoreCase = true) || it.contains("voile", ignoreCase = true) })
    }

    @Test
    fun testDiagnoseAltisesOnCabbage() {
        val cabbage = VegetableCatalog.CHOU_POMME
        val results = engine.diagnoseVegetable(
            vegetable = cabbage,
            organ = PlantOrgan.LEAF,
            symptoms = listOf("trous façon passoire", "petits coléoptères noirs sautent"),
        )

        assertFalse(results.isEmpty())
        val topMatch = results.first()
        assertEquals("altises", topMatch.disease.id)
    }

    @Test
    fun testUniversalPestsMatchedAcrossFamilies() {
        // Les limaces et les pucerons sont polyphages (targetFamilies = emptyList)
        val slugResults = engine.diagnose(
            organ = PlantOrgan.LEAF,
            symptoms = listOf("bave argentée séchée", "feuilles déchiquetées nuit"),
            family = BotanicalFamily.ASTERACEAE, // Salade
        )

        assertFalse(slugResults.isEmpty())
        val slugMatch = slugResults.find { it.disease.id == "limaces" }
        assertNotNull("Les limaces doivent être identifiées sur la salade", slugMatch)
    }
}
