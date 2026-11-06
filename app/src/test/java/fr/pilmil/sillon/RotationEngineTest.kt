package fr.pilmil.sillon

import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.engine.RotationEngine
import fr.pilmil.sillon.engine.RotationSeverity
import fr.pilmil.sillon.model.BotanicalFamily
import fr.pilmil.sillon.model.CropHistoryEntry
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.ParcelSlot
import fr.pilmil.sillon.model.ParcelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RotationEngineTest {

    private lateinit var engine: RotationEngine

    @Before
    fun setUp() {
        engine = RotationEngine()
    }

    @Test
    fun testSolanaceaeConflictWithinThreeYears() {
        // En 2023 : Pomme de terre (Solanaceae) a été cultivée sur la case [0, 0] de la parcelle "p1"
        val history = listOf(
            CropHistoryEntry(
                parcelId = "p1",
                row = 0,
                col = 0,
                year = 2023,
                family = BotanicalFamily.SOLANACEAE,
                vegetableId = "pomme_de_terre",
            )
        )

        val tomato = VegetableCatalog.TOMATE // Solanaceae

        // Scénario A : En 2024 (1 an après) -> Conflit majeur
        val results2024 = engine.checkRotation(tomato, "p1", 0, 0, 2024, history)
        val conflict2024 = results2024.find { it.severity == RotationSeverity.CONFLICT_ERROR }
        assertNotNull("Une tomate plantée 1 an après une pomme de terre doit être bloquée", conflict2024)
        assertEquals(1, conflict2024?.yearsElapsed)

        // Scénario B : En 2025 (2 ans après) -> Conflit majeur
        val results2025 = engine.checkRotation(tomato, "p1", 0, 0, 2025, history)
        val conflict2025 = results2025.find { it.severity == RotationSeverity.CONFLICT_ERROR }
        assertNotNull("Une tomate plantée 2 ans après une pomme de terre doit être bloquée", conflict2025)
        assertEquals(2, conflict2025?.yearsElapsed)

        // Scénario C : En 2026 (3 ans après) -> Conflit majeur (le délai recommandé est de 4 ans)
        val results2026 = engine.checkRotation(tomato, "p1", 0, 0, 2026, history)
        val conflict2026 = results2026.find { it.severity == RotationSeverity.CONFLICT_ERROR }
        assertNotNull("Une tomate plantée 3 ans après des Solanacées doit être signalée en conflit de rotation", conflict2026)
        assertEquals(3, conflict2026?.yearsElapsed)
    }

    @Test
    fun testSolanaceaeAllowedAfterPausePeriod() {
        // En 2020 : Solanacées
        val history = listOf(
            CropHistoryEntry(
                parcelId = "p1",
                row = 0,
                col = 0,
                year = 2020,
                family = BotanicalFamily.SOLANACEAE,
                vegetableId = "tomate",
            )
        )

        val candidate = VegetableCatalog.AUBERGINE // Solanaceae (pause de 4 ans)

        // En 2025 (5 ans écoulés >= 4 ans recommandés) -> Aucun conflit de famille
        val results2025 = engine.checkRotation(candidate, "p1", 0, 0, 2025, history)
        val familyConflicts = results2025.filter { it.severity == RotationSeverity.CONFLICT_ERROR }
        assertTrue("Après 5 ans de pause, il ne doit plus y avoir de conflit de rotation pour les Solanacées", familyConflicts.isEmpty())
    }

    @Test
    fun testBeneficialRotationAfterNitrogenFixer() {
        // En 2025 : Haricot nain (Fabaceae, LIGHT_FIXER)
        val history = listOf(
            CropHistoryEntry(
                parcelId = "p1",
                row = 0,
                col = 0,
                year = 2025,
                family = BotanicalFamily.FABACEAE,
                vegetableId = "haricot_nain",
            )
        )

        // En 2026 : Tomate (Solanaceae, HEAVY)
        val results = engine.checkRotation(VegetableCatalog.TOMATE, "p1", 0, 0, 2026, history)

        // Aucun conflit de famille
        val familyConflicts = results.filter { it.severity == RotationSeverity.CONFLICT_ERROR }
        assertTrue(familyConflicts.isEmpty())

        // Doit signaler une rotation bénéfique
        val beneficial = results.find { it.severity == RotationSeverity.BENEFICIAL }
        assertNotNull("La succession d'une Fabacée par une Solanacée exigeante doit être reconnue comme bénéfique", beneficial)
    }

    @Test
    fun testSuccessiveHeavyFeedersWarning() {
        // En 2025 : Chou pommé (Brassicaceae, HEAVY)
        val history = listOf(
            CropHistoryEntry(
                parcelId = "p1",
                row = 0,
                col = 0,
                year = 2025,
                family = BotanicalFamily.BRASSICACEAE,
                vegetableId = "chou_pomme",
            )
        )

        // En 2026 : Tomate (Solanaceae, HEAVY) -> Deux familles différentes mais successives et très gourmandes
        val results = engine.checkRotation(VegetableCatalog.TOMATE, "p1", 0, 0, 2026, history)

        val warning = results.find { it.severity == RotationSeverity.EXHAUSTION_WARNING }
        assertNotNull("Une succession directe de cultures très exigeantes doit émettre un avertissement d'épuisement du sol", warning)
    }

    @Test
    fun testEvaluateParcelWithRotationHistory() {
        val history = listOf(
            CropHistoryEntry("parcel_1", 0, 0, 2024, BotanicalFamily.SOLANACEAE, "tomate"),
            CropHistoryEntry("parcel_1", 0, 1, 2024, BotanicalFamily.FABACEAE, "pois"),
        )

        val parcel = Parcel(
            id = "parcel_1",
            name = "Parcelle Potagère",
            type = ParcelType.RAISED_BED_SQUARES,
            rows = 1,
            cols = 2,
            slots = listOf(
                ParcelSlot(row = 0, col = 0, vegetableId = "pomme_de_terre"), // Solanaceae sur Solanaceae (2026 - 2024 = 2 ans < 4)
                ParcelSlot(row = 0, col = 1, vegetableId = "carotte"),        // Apiaceae sur Fabaceae (légitime)
            )
        )

        val issues = engine.evaluateParcel(parcel, targetYear = 2026, history = history)
        val slot00Issues = issues.filter { it.row == 0 && it.col == 0 }
        val slot01Issues = issues.filter { it.row == 0 && it.col == 1 }

        assertTrue("La case [0,0] doit avoir un conflit de rotation", slot00Issues.any { it.severity == RotationSeverity.CONFLICT_ERROR })
        assertTrue("La case [0,1] ne doit avoir aucun conflit majeur", slot01Issues.none { it.severity == RotationSeverity.CONFLICT_ERROR })
    }
}
