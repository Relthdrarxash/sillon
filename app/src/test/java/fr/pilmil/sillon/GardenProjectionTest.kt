package fr.pilmil.sillon

import androidx.compose.ui.geometry.Offset
import fr.pilmil.sillon.ui.garden.GardenProjection
import fr.pilmil.sillon.ui.garden.ParcelFootprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenProjectionTest {

    private val width = 1080f
    private val height = 1242f
    private val proj = GardenProjection.forScene(width, height)

    @Test
    fun allerRetourProjectUnproject() {
        for ((x, y, z) in listOf(
            Triple(0f, 0f, 0f),
            Triple(-0.55f, 0.21f, 0.3f),
            Triple(0.4f, 0.21f, 1.1f),
            Triple(1.2f, 0f, 2.0f),
        )) {
            val screen = proj.project(x, y, z)
            val back = proj.unprojectToPlane(screen, y)
            assertNotNull(back)
            assertEquals(x, back!!.first, 0.002f)
            assertEquals(z, back.second, 0.002f)
        }
    }

    @Test
    fun plusLoinEstPlusPetitEtPlusHaut() {
        assertTrue(proj.scaleAt(2f) < proj.scaleAt(1f))
        assertTrue(proj.scaleAt(1f) < proj.scaleAt(0f))
        assertTrue(proj.project(0f, 0f, 1f).y < proj.project(0f, 0f, 0f).y)
        val near = proj.project(0.6f, 0f, 0f).x - proj.project(-0.6f, 0f, 0f).x
        val far = proj.project(0.6f, 0f, 1.2f).x - proj.project(-0.6f, 0f, 1.2f).x
        assertEquals(0.82f, near / width, 0.01f)
        // Avec le rebord à 25 cm de haut, la silhouette du carré approche 88 % de la largeur.
        val rim = proj.project(0.6f, 0.25f, 0f).x - proj.project(-0.6f, 0.25f, 0f).x
        assertEquals(0.88f, rim / width, 0.02f)
        assertTrue("bord lointain / proche = ${far / near}", far / near in 0.76f..0.82f)
    }

    @Test
    fun auDessusDeLHorizonRienNeSeProjette() {
        // Un pixel tout en haut regarde vers le haut du ciel : pas d'intersection avec le sol d'un jardin raisonnable,
        // et un plan situé au-dessus de la caméra n'est jamais atteint.
        assertNull(proj.unprojectToPlane(Offset(width / 2f, -5000f), 0f))
        assertNull(proj.unprojectToPlane(Offset(width / 2f, height / 2f), 50f))
    }

    @Test
    fun tapAuCentreDUneCaseRendLaBonneCase() {
        val fp = ParcelFootprint(x0 = -0.6f, z0 = 0f, rows = 4, cols = 4, cell = 0.275f, wall = 0.05f)
        val soilY = 0.19f
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val screen = proj.project(fp.cellCenterX(col), soilY, fp.cellCenterZ(row))
                val hit = proj.unprojectToPlane(screen, soilY)
                assertNotNull(hit)
                assertEquals(row to col, fp.cellAt(hit!!.first, hit.second))
            }
        }
        // La rangée 0 est la plus lointaine : son centre est plus haut à l'écran.
        assertTrue(
            proj.project(fp.cellCenterX(0), soilY, fp.cellCenterZ(0)).y <
                proj.project(fp.cellCenterX(0), soilY, fp.cellCenterZ(3)).y,
        )
        assertNull(fp.cellAt(fp.x0 + 0.01f, 0.5f))
    }
}
