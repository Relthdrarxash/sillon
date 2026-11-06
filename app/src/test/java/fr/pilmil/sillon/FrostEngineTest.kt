package fr.pilmil.sillon

import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.engine.FrostEngine
import fr.pilmil.sillon.engine.GardeningAction
import fr.pilmil.sillon.model.FrostDate
import fr.pilmil.sillon.model.Garden
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.ParcelSlot
import fr.pilmil.sillon.model.ParcelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FrostEngineTest {

    private lateinit var engine: FrostEngine
    // Référence : Dernière gelée médiane au 15 mai (Saintes Glaces)
    private val frostDate = FrostDate(5, 15)

    @Before
    fun setUp() {
        engine = FrostEngine()
    }

    @Test
    fun testFrostDateCalculation() {
        val date2026 = engine.getLastFrostDate(frostDate, 2026)
        assertEquals(LocalDate.of(2026, 5, 15), date2026)
    }

    @Test
    fun testSowIndoorsTomatoWindow() {
        // La tomate se sème au chaud 8 semaines avant la dernière gelée
        // 15 mai 2026 - 8 semaines = 20 mars 2026
        val currentDate = LocalDate.of(2026, 3, 20)
        val tasks = engine.getSowIndoorsTasks(frostDate, currentDate, toleranceDays = 7)

        val tomatoTask = tasks.find { it.vegetable.id == "tomate" }
        assertNotNull("La tomate doit être proposée au semis sous abri le 20 mars", tomatoTask)
        assertEquals(GardeningAction.SOW_INDOORS, tomatoTask?.action)
        assertEquals(LocalDate.of(2026, 3, 20), tomatoTask?.targetDate)

        // Hors période (ex: en plein été le 20 juillet), la tomate ne doit pas être proposée
        val summerDate = LocalDate.of(2026, 7, 20)
        val summerTasks = engine.getSowIndoorsTasks(frostDate, summerDate, toleranceDays = 7)
        assertFalse("La tomate ne doit pas être proposée au semis sous abri en juillet", summerTasks.any { it.vegetable.id == "tomate" })
    }

    @Test
    fun testSowOutdoorsBeansWindow() {
        // Le haricot nain se sème en pleine terre 2 semaines après la dernière gelée (sol réchauffé)
        // 15 mai 2026 + 2 semaines = 29 mai 2026
        val currentDate = LocalDate.of(2026, 5, 29)
        val tasks = engine.getSowOutdoorsTasks(frostDate, currentDate, toleranceDays = 7)

        val beanTask = tasks.find { it.vegetable.id == "haricot_nain" }
        assertNotNull("Le haricot nain doit être proposé au semis extérieur fin mai", beanTask)
        assertEquals(GardeningAction.SOW_OUTDOORS, beanTask?.action)
        assertEquals(LocalDate.of(2026, 5, 29), beanTask?.targetDate)
    }

    @Test
    fun testTransplantTomatoWindow() {
        // La tomate se repique en pleine terre 1 semaine après la dernière gelée (après les Saintes Glaces)
        // 15 mai 2026 + 1 semaine = 22 mai 2026
        val currentDate = LocalDate.of(2026, 5, 22)
        val tasks = engine.getTransplantTasks(frostDate, currentDate, toleranceDays = 7)

        val tomatoTask = tasks.find { it.vegetable.id == "tomate" }
        assertNotNull("La tomate doit être proposée au repiquage après les gelées", tomatoTask)
        assertEquals(GardeningAction.TRANSPLANT, tomatoTask?.action)
        assertEquals(LocalDate.of(2026, 5, 22), tomatoTask?.targetDate)
    }

    @Test
    fun testEarlySpringHardyCrops() {
        // Les pois et fèves se sèment en extérieur bien avant les dernières gelées (-6 semaines = ~3 avril)
        val earlyApril = LocalDate.of(2026, 4, 3)
        val outdoorTasks = engine.getSowOutdoorsTasks(frostDate, earlyApril, toleranceDays = 7)

        val peaTask = outdoorTasks.find { it.vegetable.id == "pois" }
        assertNotNull("Les pois doivent être proposés au semis précoce début avril", peaTask)
    }

    @Test
    fun testGardenHarvestActiveWindow() {
        // Création d'un jardin avec un carré de radis semé le 1er avril 2026
        val plantDate = LocalDate.of(2026, 4, 1)
        val epochMilli = plantDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val garden = Garden(
            id = "garden_1",
            name = "Mon Potager",
            frostDate = frostDate,
            parcels = listOf(
                Parcel(
                    id = "parcel_1",
                    name = "Carré Aromates & Racines",
                    type = ParcelType.RAISED_BED_SQUARES,
                    rows = 2,
                    cols = 2,
                    slots = listOf(
                        ParcelSlot(row = 0, col = 0, vegetableId = "radis", plantedDate = epochMilli)
                    )
                )
            )
        )

        // Les radis ont une durée avant récolte de 21 à 35 jours.
        // Le 25 avril 2026 (J+24), ils doivent être récoltables !
        val harvestDate = LocalDate.of(2026, 4, 25)
        val harvestTasks = engine.getGardenHarvestTasks(garden, harvestDate)

        assertEquals(1, harvestTasks.size)
        val task = harvestTasks.first()
        assertEquals("radis", task.vegetable.id)
        assertEquals(0, task.row)
        assertEquals(0, task.col)
        assertTrue(task.daysRemainingInHarvest > 0)

        // Bien plus tard (1er août 2026), la fenêtre de récolte est dépassée
        val lateDate = LocalDate.of(2026, 8, 1)
        val lateTasks = engine.getGardenHarvestTasks(garden, lateDate)
        assertTrue("La récolte des radis ne doit plus être active en août", lateTasks.isEmpty())
    }

    @Test
    fun testAllCalendarTasksAggregation() {
        val springDay = LocalDate.of(2026, 5, 20)
        val allTasks = engine.getAllCalendarTasks(frostDate, springDay, toleranceDays = 7)
        assertTrue("La liste globale des tâches printanières ne doit pas être vide", allTasks.isNotEmpty())
    }
}
