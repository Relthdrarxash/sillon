package fr.pilmil.sillon

import fr.pilmil.sillon.data.FrenchFrostCatalog
import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.engine.DashboardPlanner
import fr.pilmil.sillon.engine.FrostEngine
import fr.pilmil.sillon.engine.RotationEngine
import fr.pilmil.sillon.model.BotanicalFamily
import fr.pilmil.sillon.model.Climate
import fr.pilmil.sillon.model.CropHistoryEntry
import fr.pilmil.sillon.model.FrostDate
import fr.pilmil.sillon.model.Garden
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.ParcelSlot
import fr.pilmil.sillon.model.ParcelType
import fr.pilmil.sillon.model.Vegetable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TropicalCalendarTest {

    private val tropicalOnly = VegetableCatalog.ALL_VEGETABLES.filter { Climate.TEMPERATE !in it.climates }
    private val ids = tropicalOnly.map { it.id }.toSet()

    // Guadeloupe : début de la saison de plantation au 1er juin.
    private val seasonStart = FrenchFrostCatalog.getByCode("971")!!.seasonStart
    private val tropicalGarden = Garden(id = "g971", name = "Test 971", frostDate = seasonStart)
    private val tropicalEngine = FrostEngine(climate = Climate.TROPICAL)

    @Test
    fun tropicalPackIsCoherent() {
        assertTrue("Le pack doit contenir des espèces tropicales", tropicalOnly.size >= 5)
        val all = VegetableCatalog.ALL_VEGETABLES
        assertEquals("Les identifiants doivent être uniques", all.size, all.map { it.id }.toSet().size)
        assertTrue(all.all { it.climates.isNotEmpty() })
        for (v in tropicalOnly) {
            assertEquals("Une espèce tropicale ne doit l'être que pour le climat tropical : ${v.id}", setOf(Climate.TROPICAL), v.climates)
            assertTrue(
                "${v.id} doit avoir au moins une date de plantation pour figurer au calendrier",
                v.sowIndoorsWeeksBeforeFrost != null || v.sowOutdoorsWeeksAfterFrost != null || v.transplantWeeksAfterFrost != null,
            )
            for (c in v.goodCompanions + v.badCompanions) {
                assertNotNull("Compagnon '${c.targetPlantId}' de '${v.id}' introuvable", VegetableCatalog.getById(c.targetPlantId))
            }
        }
        // Les antagonismes ne sont pas documentés pour ces cultures : on n'en invente pas.
        assertTrue(tropicalOnly.all { it.badCompanions.isEmpty() })
    }

    @Test
    fun temperateSpeciesKeepDefaultClimateAndAreNotIndicative() {
        val tomato = VegetableCatalog.getById("tomate")!!
        assertEquals(setOf(Climate.TEMPERATE), tomato.climates)
        assertFalse(tomato.indicative)
    }

    @Test
    fun oldVegetableJsonWithoutNewFieldsStillDecodes() {
        val json = Json { encodeDefaults = true }
        val full = json.encodeToJsonElement(VegetableCatalog.TOMATE) as JsonObject
        val old = JsonObject(full - "climates" - "indicative")
        val decoded = json.decodeFromJsonElement<Vegetable>(old)
        assertEquals(setOf(Climate.TEMPERATE), decoded.climates)
        assertFalse(decoded.indicative)
    }

    @Test
    fun overseasDepartmentsAreTropicalAndMetropoleIsTemperate() {
        for (code in listOf("971", "972", "973", "974", "976", "977", "978", "987")) {
            assertEquals("Le $code doit être tropical", Climate.TROPICAL, FrenchFrostCatalog.getByCode(code)!!.climate)
            assertEquals(Climate.TROPICAL, FrenchFrostCatalog.getClimate(code))
        }
        assertEquals(Climate.TEMPERATE, FrenchFrostCatalog.getClimate("75"))
        assertEquals(Climate.TEMPERATE, FrenchFrostCatalog.getClimate("2A"))
        assertEquals(Climate.TEMPERATE, FrenchFrostCatalog.getClimate(null))
        val tropical = FrenchFrostCatalog.DEPARTMENTS.filter { it.climate == Climate.TROPICAL }
        assertEquals(8, tropical.size)
        // Plus de « gelée » fictive au 1er janvier : chaque territoire a son début de saison.
        assertTrue(tropical.none { it.seasonStart == FrostDate(1, 1) })
        assertEquals(FrostDate(6, 1), FrenchFrostCatalog.getFrostDate("972"))
        assertEquals(FrostDate(11, 1), FrenchFrostCatalog.getFrostDate("974"))
    }

    @Test
    fun overseasCollectivitiesAreCoveredAndOthersAreNot() {
        assertEquals("Polynésie française", FrenchFrostCatalog.getByCode("987")!!.name)
        assertEquals("Saint-Martin", FrenchFrostCatalog.getByCode("977")!!.name)
        assertEquals("Saint-Barthélemy", FrenchFrostCatalog.getByCode("978")!!.region)
        assertEquals(FrostDate(11, 1), FrenchFrostCatalog.getFrostDate("987"))
        assertEquals(FrostDate(6, 1), FrenchFrostCatalog.getFrostDate("977"))
        assertEquals(FrostDate(6, 1), FrenchFrostCatalog.getFrostDate("978"))
        // Sans normales sourcées : volontairement absents, ils retombent sur le tempéré par défaut.
        for (code in listOf("975", "986", "988")) {
            assertEquals(null, FrenchFrostCatalog.getByCode(code))
            assertEquals(Climate.TEMPERATE, FrenchFrostCatalog.getClimate(code))
        }
    }

    @Test
    fun searchFindsCollectivitiesByNameOrCode() {
        // Même règle que DepartmentSearch : préfixe du code ou nom sans accent ni casse.
        fun fold(s: String) = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()
        fun search(q: String) = FrenchFrostCatalog.DEPARTMENTS
            .filter { it.code.lowercase().startsWith(fold(q)) || fold(it.name).contains(fold(q)) }.map { it.code }
        assertEquals(listOf("987"), search("Polynésie"))
        assertEquals(listOf("987"), search("987"))
        assertEquals(listOf("977"), search("Saint-Martin"))
        assertEquals(listOf("978"), search("saint-barthelemy"))
    }

    @Test
    fun collectivitiesHaveNoTemperateTasks() {
        for (code in listOf("977", "978", "987")) {
            val dep = FrenchFrostCatalog.getByCode(code)!!
            val garden = Garden(id = "g$code", name = code, frostDate = dep.seasonStart)
            var d = LocalDate.of(2026, 1, 1)
            while (d.year == 2026) {
                val plan = DashboardPlanner.plan(garden, d, tropicalEngine)
                val all = plan.sowIndoors + plan.sowOutdoors + plan.transplant + plan.harvest
                assertTrue("Tâche tempérée à $code le $d", all.none { Climate.TROPICAL !in it.vegetable.climates })
                d = d.plusDays(7)
            }
        }
        val polynesia = FrenchFrostCatalog.getByCode("987")!!.seasonStart
        val plan = DashboardPlanner.plan(Garden(id = "g987", name = "987", frostDate = polynesia), LocalDate.of(2026, 11, 9), tropicalEngine)
        assertEquals(Climate.TROPICAL, plan.climate)
        assertTrue(plan.transplant.any { it.vegetable.id == "taro_madere" })
    }

    @Test
    fun oldGardensWithoutNewCodesStillWork() {
        // Un jardin enregistré avant l'ajout garde son code et sa date : rien ne change pour 971..976 et la métropole.
        assertEquals(FrostDate(6, 1), FrenchFrostCatalog.getByCode("971")!!.seasonStart)
        assertEquals(Climate.TROPICAL, FrenchFrostCatalog.getClimate("976"))
        assertEquals(Climate.TEMPERATE, FrenchFrostCatalog.getClimate("75"))
        assertEquals(FrenchFrostCatalog.DEFAULT_FROST_DATE, FrenchFrostCatalog.getFrostDate("999"))
    }

    @Test
    fun guadeloupeJuneListsTropicalTasks() {
        val plan = DashboardPlanner.plan(tropicalGarden, LocalDate.of(2026, 6, 3), tropicalEngine)
        assertEquals(Climate.TROPICAL, plan.climate)
        assertTrue(plan.sowOutdoors.any { it.vegetable.id == "pois_d_angole" })
        assertTrue(plan.transplant.any { it.vegetable.id == "papayer" })
        assertFalse(plan.isEmpty)
    }

    @Test
    fun temperateSpeciesHaveNoCalendarTasksInTheTropics() {
        // Le 6 avril, c'est l'ancre moins 8 semaines : le semis de tomate serait proposé en climat tempéré.
        val day = LocalDate.of(2026, 4, 6)
        assertTrue(FrostEngine().getSowIndoorsTasks(seasonStart, day).any { it.vegetable.id == "tomate" })
        val plan = DashboardPlanner.plan(tropicalGarden, day, tropicalEngine)
        val all = plan.sowIndoors + plan.sowOutdoors + plan.transplant + plan.harvest
        assertTrue(all.none { Climate.TROPICAL !in it.vegetable.climates })
    }

    @Test
    fun temperateSpeciesStayPlantableAndHarvestableInTropicalParcels() {
        val planted = LocalDate.of(2026, 3, 10).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val garden = tropicalGarden.copy(
            parcels = listOf(
                Parcel("p", "Carré", ParcelType.RAISED_BED_SQUARES, 2, 2, listOf(ParcelSlot(0, 0, "radis", planted))),
            ),
        )
        val plan = DashboardPlanner.plan(garden, LocalDate.of(2026, 4, 5), tropicalEngine)
        assertTrue(plan.gardenHarvest.any { it.vegetable.id == "radis" })
    }

    @Test
    fun tropicalOnlySpeciesNeverAppearInParis() {
        val paris = FrenchFrostCatalog.getByCode("75")!!.medianLastFrost
        val engine = FrostEngine()
        var day = LocalDate.of(2026, 1, 1)
        while (day.year == 2026) {
            val shown = engine.getAllCalendarTasks(paris, day).map { it.vegetable.id }
            assertTrue("Espèce tropicale au calendrier tempéré le $day : ${shown.filter { it in ids }}", shown.none { it in ids })
            day = day.plusDays(1)
        }
    }

    @Test
    fun reunionHarvestSpansTheNewYear() {
        // Papayer planté à l'ancre du 1er novembre 2025 : récolte ouverte à partir de fin juin 2026.
        val reunion = FrenchFrostCatalog.getByCode("974")!!.seasonStart
        val tasks = tropicalEngine.getEstimatedCatalogHarvestTasks(reunion, LocalDate.of(2026, 7, 15))
        assertTrue(tasks.any { it.vegetable.id == "papayer" })
    }

    @Test
    fun tropicalDescriptionsDoNotMentionFrost() {
        val reunion = FrenchFrostCatalog.getByCode("974")!!.seasonStart
        val day = LocalDate.of(2026, 11, 1)
        val tasks = tropicalEngine.getAllCalendarTasks(reunion, day)
        assertTrue(tasks.isNotEmpty())
        assertTrue(tasks.none { it.description.contains("gelée") })
    }

    @Test
    fun tropicalFamiliesHaveRotationRules() {
        val families = listOf(BotanicalFamily.CONVOLVULACEAE, BotanicalFamily.DIOSCOREACEAE, BotanicalFamily.ARACEAE, BotanicalFamily.CARICACEAE)
        assertTrue(families.all { it.recommendedPauseYears >= 2 })
        val yam = VegetableCatalog.getById("igname")!!
        val history = listOf(CropHistoryEntry("p", 0, 0, 2025, BotanicalFamily.DIOSCOREACEAE, "igname"))
        val issues = RotationEngine().checkRotation(yam, "p", 0, 0, 2026, history)
        assertTrue("L'igname ne doit pas succéder à elle-même", issues.isNotEmpty())
    }
}
