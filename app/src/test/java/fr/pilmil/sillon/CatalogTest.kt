package fr.pilmil.sillon

import fr.pilmil.sillon.data.DiseaseCatalog
import fr.pilmil.sillon.data.FrenchFrostCatalog
import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.model.BotanicalFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {

    @Test
    fun testCatalogSizeAndDiversity() {
        val all = VegetableCatalog.ALL_VEGETABLES
        assertTrue("Le catalogue doit contenir au moins 40 plantes (reçu: ${all.size})", all.size >= 40)

        // Vérifier la présence des familles botaniques essentielles
        val families = all.map { it.family }.toSet()
        assertTrue("Doit contenir des Solanacées", families.contains(BotanicalFamily.SOLANACEAE))
        assertTrue("Doit contenir des Fabacées", families.contains(BotanicalFamily.FABACEAE))
        assertTrue("Doit contenir des Brassicacées", families.contains(BotanicalFamily.BRASSICACEAE))
        assertTrue("Doit contenir des Apiacées", families.contains(BotanicalFamily.APIACEAE))
        assertTrue("Doit contenir des Alliacées", families.contains(BotanicalFamily.ALLIACEAE))
        assertTrue("Doit contenir des Cucurbitacées", families.contains(BotanicalFamily.CUCURBITACEAE))
        assertTrue("Doit contenir des Chénopodiacées", families.contains(BotanicalFamily.CHENOPODIACEAE))
        assertTrue("Doit contenir des Astéracées", families.contains(BotanicalFamily.ASTERACEAE))
        assertTrue("Doit contenir des Lamiacées", families.contains(BotanicalFamily.LAMIACEAE))

        // Vérifier les fleurs compagnes
        val flowerIds = listOf("tagete", "bourrache", "capucine", "phacelie", "souci")
        flowerIds.forEach { id ->
            assertNotNull("La fleur compagne '$id' doit être présente dans le catalogue", VegetableCatalog.getById(id))
        }
    }

    @Test
    fun testAllCompanionReferencesAreValid() {
        val catalogMap = VegetableCatalog.BY_ID

        for (vegetable in VegetableCatalog.ALL_VEGETABLES) {
            // Vérifier toutes les bonnes associations
            for (companion in vegetable.goodCompanions) {
                val targetId = companion.targetPlantId
                assertTrue(
                    "Dans '${vegetable.id}', le bon compagnon '$targetId' doit exister dans le catalogue",
                    catalogMap.containsKey(targetId)
                )
                assertNotNull("Le type de bénéfice ne doit pas être nul", companion.benefitType)
                assertFalse("La justification ne doit pas être vide", companion.rationale.isBlank())
            }

            // Vérifier tous les mauvais compagnons
            for (companion in vegetable.badCompanions) {
                val targetId = companion.targetPlantId
                assertTrue(
                    "Dans '${vegetable.id}', le mauvais compagnon '$targetId' doit exister dans le catalogue",
                    catalogMap.containsKey(targetId)
                )
                assertNotNull("Le type de risque ne doit pas être nul", companion.riskType)
                assertFalse("La justification ne doit pas être vide", companion.rationale.isBlank())
            }
        }
    }

    @Test
    fun testVegetableScientificFieldsIntegrity() {
        for (v in VegetableCatalog.ALL_VEGETABLES) {
            assertFalse("L'ID ne doit pas être vide", v.id.isBlank())
            assertFalse("Le nom ne doit pas être vide pour ${v.id}", v.name.isBlank())
            assertFalse("Le nom latin ne doit pas être vide pour ${v.id}", v.latinName.isBlank())
            assertTrue("La densité en carré potager doit être > 0 pour ${v.id}", v.squareFootDensity in listOf(1, 2, 4, 8, 9, 16))
            assertTrue("L'espacement dans le rang doit être > 0 pour ${v.id}", v.spacingInRowCm > 0)
            assertTrue("L'espacement entre rangs doit être > 0 pour ${v.id}", v.spacingBetweenRowsCm > 0)
            assertTrue("La température de germination min doit être réaliste pour ${v.id}", v.tempGerminationMin in 0..25)
            assertTrue("La plage de températures optimales doit être cohérente pour ${v.id}", v.tempOptimalMin <= v.tempOptimalMax)
            assertTrue("La durée de levée doit être positive pour ${v.id}", v.daysToEmerge.first > 0 && v.daysToEmerge.first <= v.daysToEmerge.last)
            assertTrue("La durée avant récolte doit être positive pour ${v.id}", v.daysToHarvest.first > 0 && v.daysToHarvest.first <= v.daysToHarvest.last)
            assertTrue("La durée de récolte doit être > 0 pour ${v.id}", v.harvestDurationWeeks > 0)
            assertFalse("Le conseil agronomique ne doit pas être vide pour ${v.id}", v.advice.isBlank())
        }
    }

    @Test
    fun testFrenchFrostCatalogCoverage() {
        val departments = FrenchFrostCatalog.DEPARTMENTS
        assertTrue("Le catalogue des gelées doit contenir au moins 96 départements (reçu: ${departments.size})", departments.size >= 96)

        // Vérifier les départements métropolitains de référence (ex: 75 Paris, 13 Bouches-du-Rhône, 67 Bas-Rhin, 29 Finistère)
        val paris = FrenchFrostCatalog.getByCode("75")
        assertNotNull(paris)
        assertTrue(paris!!.medianLastFrost.month in 3..5)

        val marseille = FrenchFrostCatalog.getByCode("13")
        assertNotNull(marseille)
        assertEquals(3, marseille!!.medianLastFrost.month) // Mars en zone méditerranéenne

        val strasbourg = FrenchFrostCatalog.getByCode("67")
        assertNotNull(strasbourg)
        assertEquals(5, strasbourg!!.medianLastFrost.month) // Mai en Alsace

        // Vérifier les DOM-TOM
        val reunion = FrenchFrostCatalog.getByCode("974")
        assertNotNull(reunion)
    }

    @Test
    fun testDiseaseCatalogIntegrity() {
        val diseases = DiseaseCatalog.ALL_DISEASES
        assertTrue("Le catalogue des maladies doit contenir au moins 10 affections clés", diseases.size >= 10)

        val requiredDiseases = listOf(
            "mildiou",
            "oidium",
            "cul_noir",
            "pucerons",
            "altises",
            "rouille",
            "limaces",
            "mouche_carotte",
            "teigne_poireau",
            "pieride_chou",
        )

        for (id in requiredDiseases) {
            val disease = DiseaseCatalog.getById(id)
            assertNotNull("La maladie ou ravageur '$id' doit être catalogué", disease)
            assertTrue("Les organes atteints ne doivent pas être vides pour '$id'", disease!!.affectedOrgans.isNotEmpty())
            assertTrue("Les symptômes doivent être documentés pour '$id'", disease.symptoms.isNotEmpty())
            assertTrue("Les causes doivent être documentées pour '$id'", disease.causes.isNotEmpty())
            assertTrue("Les remèdes bio doivent être documentés pour '$id'", disease.organicRemedies.isNotEmpty())
        }
    }
}
