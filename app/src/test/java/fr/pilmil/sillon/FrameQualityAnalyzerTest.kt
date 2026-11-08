package fr.pilmil.sillon

import fr.pilmil.sillon.vision.FrameQuality
import fr.pilmil.sillon.vision.FrameQualityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FrameQualityAnalyzerTest {

    @Test
    fun frameQualityStatusEnumValuesExist() {
        val optimal = FrameQualityStatus.OPTIMAL
        val warning = FrameQualityStatus.WARNING
        val unusable = FrameQualityStatus.UNUSABLE
        assertEquals(3, FrameQualityStatus.entries.size)
        assertNotNull(optimal)
        assertNotNull(warning)
        assertNotNull(unusable)
    }

    @Test
    fun frameQualityDataClassIntegrity() {
        val quality = FrameQuality(
            status = FrameQualityStatus.OPTIMAL,
            sharpness = 18.5f,
            brightness = 140f,
            plantCoverage = 0.65f,
            tip = "Cadre optimal !",
        )
        assertEquals(FrameQualityStatus.OPTIMAL, quality.status)
        assertEquals(18.5f, quality.sharpness, 0.001f)
        assertEquals(140f, quality.brightness, 0.001f)
        assertEquals(0.65f, quality.plantCoverage, 0.001f)
        assertEquals("Cadre optimal !", quality.tip)
    }
}
