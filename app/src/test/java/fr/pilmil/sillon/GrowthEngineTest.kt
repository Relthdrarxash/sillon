package fr.pilmil.sillon

import fr.pilmil.sillon.engine.GrowthEngine
import fr.pilmil.sillon.engine.GrowthStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthEngineTest {

    // Culture type : récolte entre 60 et 80 jours, pendant 4 semaines.
    private fun at(day: Int) = GrowthEngine.growth(day, 60..80, 4)

    @Test
    fun `les stades se suivent dans l ordre jusqu a la recolte`() {
        assertEquals(GrowthStage.SETTLING, at(0).stage)
        assertEquals(GrowthStage.SETTLING, at(8).stage)
        assertEquals(GrowthStage.YOUNG, at(9).stage)
        assertEquals(GrowthStage.YOUNG, at(23).stage)
        assertEquals(GrowthStage.GROWING, at(24).stage)
        assertEquals(GrowthStage.GROWING, at(47).stage)
        assertEquals(GrowthStage.ALMOST_READY, at(48).stage)
        assertEquals(GrowthStage.ALMOST_READY, at(59).stage)
        assertEquals(GrowthStage.READY, at(60).stage)
    }

    @Test
    fun `l avancement ne recule jamais et finit a 1`() {
        var previous = -1f
        for (day in 0..120) {
            val progress = at(day).progress
            assertTrue("jour $day", progress >= previous)
            assertTrue("jour $day", progress in 0f..1f)
            previous = progress
        }
        assertEquals(1f, at(60).progress, 0f)
        assertEquals(0.5f, at(30).progress, 0.0001f)
    }

    @Test
    fun `fourchette de jours avant la recolte`() {
        assertEquals(60..80, at(0).daysUntilHarvest)
        assertEquals(30..50, at(30).daysUntilHarvest)
        assertEquals(1..21, at(59).daysUntilHarvest)
        assertNull(at(60).daysUntilHarvest)
    }

    @Test
    fun `la recolte dure jusqu a la fin de la fenetre, comme dans les taches du tableau de bord`() {
        // Fin de fenêtre : 80 + 4 × 7 = 108 jours.
        assertEquals(48, at(60).harvestDaysLeft)
        assertEquals(GrowthStage.READY, at(108).stage)
        assertEquals(0, at(108).harvestDaysLeft)
        assertEquals(GrowthStage.FINISHED, at(109).stage)
        assertNull(at(109).harvestDaysLeft)
        assertNull(at(30).harvestDaysLeft)
    }

    @Test
    fun `phrases affichees`() {
        assertEquals("Installation, récolte dans 60 à 80 jours", at(0).summary())
        assertEquals("En croissance, récolte dans 30 à 50 jours", at(30).summary())
        assertEquals("À récolter, encore environ 48 jours", at(60).summary())
        assertEquals("À récolter, encore 1 jour", at(107).summary())
        assertEquals("À récolter, dernier jour de récolte", at(108).summary())
        assertEquals("Récolte terminée : la place peut être libérée", at(200).summary())
        // Durée unique dans le catalogue : pas de fourchette.
        assertEquals("Jeune plant, récolte dans 20 jours", GrowthEngine.growth(10, 30..30, 2).summary())
        assertEquals("Bientôt prête, récolte dans 1 jour", GrowthEngine.growth(29, 30..30, 2).summary())
    }

    @Test
    fun `valeurs inattendues - date dans le futur ou catalogue incoherent`() {
        // Horloge reculée : on reste au premier stade plutôt que de planter l'appli.
        assertEquals(GrowthStage.SETTLING, at(-5).stage)
        assertEquals(0f, at(-5).progress, 0f)
        // Durées nulles ou inversées dans une fiche : pas de division par zéro.
        assertEquals(GrowthStage.READY, GrowthEngine.growth(1, 0..0, 0).stage)
        assertEquals(GrowthStage.FINISHED, GrowthEngine.growth(3, 0..0, 0).stage)
        assertEquals(GrowthStage.SETTLING, GrowthEngine.growth(0, 40..30, 1).stage)
        assertEquals(40..40, GrowthEngine.growth(0, 40..30, 1).daysUntilHarvest)
    }
}
