package fr.pilmil.sillon

import fr.pilmil.sillon.engine.DashboardPlanner
import fr.pilmil.sillon.engine.FrostStatus
import fr.pilmil.sillon.model.FrostDate
import fr.pilmil.sillon.model.Garden
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DashboardPlannerTest {

    private val frost = FrostDate(5, 15)
    private val emptyGarden = Garden(id = "g", name = "Test", frostDate = frost)

    @Test
    fun frostAheadCountsDownThisYear() {
        val status = DashboardPlanner.frostStatus(frost, LocalDate.of(2026, 5, 1))
        assertEquals(FrostStatus.Upcoming(LocalDate.of(2026, 5, 15), 14), status)
    }

    @Test
    fun frostJustPassedIsShownAsPassed() {
        val status = DashboardPlanner.frostStatus(frost, LocalDate.of(2026, 6, 1))
        assertEquals(FrostStatus.Passed(LocalDate.of(2026, 5, 15), 17), status)
    }

    @Test
    fun frostLongPassedCountsDownToNextYear() {
        val status = DashboardPlanner.frostStatus(frost, LocalDate.of(2026, 11, 8))
        assertTrue(status is FrostStatus.Upcoming)
        assertEquals(LocalDate.of(2027, 5, 15), (status as FrostStatus.Upcoming).date)
    }

    @Test
    fun springDashboardListsTomatoSowing() {
        // 15 mai - 8 semaines = 20 mars : semis de tomates au chaud.
        val plan = DashboardPlanner.plan(emptyGarden, LocalDate.of(2026, 3, 20))
        assertTrue(plan.sowIndoors.any { it.vegetable.id == "tomate" })
        assertNull(plan.nextWorkFrom)
    }

    @Test
    fun novemberDashboardNeverSuggestsTomatoes() {
        val plan = DashboardPlanner.plan(emptyGarden, LocalDate.of(2026, 11, 8))
        assertTrue(plan.sowIndoors.none { it.vegetable.id == "tomate" })
    }

    @Test
    fun deadSeasonPointsToNextWork() {
        val plan = DashboardPlanner.plan(emptyGarden, LocalDate.of(2026, 12, 20))
        if (plan.isEmpty) {
            assertNotNull(plan.nextWorkFrom)
            assertTrue(plan.nextWorkFrom!!.isAfter(LocalDate.of(2026, 12, 20)))
        }
    }
}
