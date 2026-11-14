package fr.pilmil.sillon

import fr.pilmil.sillon.data.AgronomyGlossary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgronomyGlossaryTest {

    @Test
    fun `matches longest expression without overlap`() {
        val text = "Ses exsudats racinaires détruisent les nématodes à galles."
        val matches = AgronomyGlossary.findMatches(text)

        assertEquals(listOf("exsudats", "nematodes_galles"), matches.map { it.entry.id })
        assertEquals("exsudats racinaires", text.substring(matches[0].start, matches[0].end))
        assertEquals("nématodes à galles", text.substring(matches[1].start, matches[1].end))
    }

    @Test
    fun `matches case-insensitively and respects word boundaries`() {
        val text = "Allélopathie puissante inhibant la nouaison et les Solanacées."
        val matches = AgronomyGlossary.findMatches(text)

        assertEquals(
            listOf("allelopathie", "nouaison", "solanacees"),
            matches.map { it.entry.id },
        )
    }

    @Test
    fun `returns empty list when no technical term is present`() {
        val text = "Arroser régulièrement au pied le matin."
        assertTrue(AgronomyGlossary.findMatches(text).isEmpty())
    }
}
