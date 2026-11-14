package fr.pilmil.sillon

import fr.pilmil.sillon.data.GardenCodec
import fr.pilmil.sillon.data.GardenRepository
import fr.pilmil.sillon.data.GardenValidationException
import fr.pilmil.sillon.model.FrostDate
import fr.pilmil.sillon.model.Garden
import fr.pilmil.sillon.model.GrowingEnvironment
import fr.pilmil.sillon.model.LightSource
import fr.pilmil.sillon.model.Orientation
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.ParcelSlot
import fr.pilmil.sillon.model.ParcelType
import fr.pilmil.sillon.model.WindExposure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GardenRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun sample(rows: Int = 3, cols: Int = 3, vegetable: String = "tomate", planted: Long? = 1_700_000_000_000L) = Garden(
        id = "g",
        name = "Test",
        frostDate = FrostDate(5, 15),
        parcels = listOf(
            Parcel("p1", "Carré", ParcelType.RAISED_BED_SQUARES, rows, cols, listOf(ParcelSlot(0, 0, vegetable, planted))),
        ),
    )

    private fun import(repo: GardenRepository, content: String) = repo.importFromJson(content.byteInputStream())

    @Test
    fun exportPuisImportRestitueLePotager() {
        val source = GardenRepository(tmp.newFolder("a"))
        source.saveGarden(sample())
        val out = java.io.ByteArrayOutputStream()
        source.exportToJson(out)

        val target = GardenRepository(tmp.newFolder("b"))
        val result = import(target, out.toString("UTF-8"))
        assertEquals(sample(), result.getOrThrow())
        assertEquals(sample(), target.loadGarden())
    }

    @Test
    fun importInvalideEstRefuseEtLAncienPotagerResteEnPlace() {
        val repo = GardenRepository(tmp.newFolder())
        repo.saveGarden(sample())
        val invalids = listOf(
            "pas du json",
            GardenCodec.encode(sample(rows = 0)),
            GardenCodec.encode(sample(cols = 5000)),
            GardenCodec.encode(sample(vegetable = "plante_inconnue")),
            GardenCodec.encode(sample(planted = -5L)),
            GardenCodec.encode(sample(planted = Long.MAX_VALUE)),
        )
        invalids.forEach { content ->
            val failure = import(repo, content).exceptionOrNull()
            assertTrue("Devrait être refusé : ${content.take(40)}", failure is GardenValidationException)
        }
        assertEquals(sample(), repo.loadGarden())
    }

    @Test
    fun importValideGardeUneCopieBak() {
        val dir = tmp.newFolder()
        val repo = GardenRepository(dir)
        repo.saveGarden(sample())
        val neuf = sample(rows = 4, cols = 4)
        assertTrue(import(repo, GardenCodec.encode(neuf)).isSuccess)
        assertEquals(neuf, repo.loadGarden())
        assertEquals(sample(), GardenCodec.decode(java.io.File(dir, "sillon_garden.json.bak").readText()))
    }

    @Test
    fun fichierCorrompuEstMisDeCoteAvantDeRepartirDuDefaut() {
        val dir = tmp.newFolder()
        java.io.File(dir, "sillon_garden.json").writeText("{ corrompu")
        val repo = GardenRepository(dir)

        val garden = repo.loadGarden()
        assertEquals("Mon Potager", garden.name)
        assertTrue(repo.consumeRecoveryNotice())
        assertFalse("L'indicateur ne se consomme qu'une fois", repo.consumeRecoveryNotice())
        val copies = dir.listFiles { f -> f.name.startsWith("sillon_garden.json.corrupt-") }!!
        assertEquals(1, copies.size)
        assertEquals("{ corrompu", copies[0].readText())
        // Le potager par défaut a été réécrit : le rechargement ne signale plus rien.
        repo.loadGarden()
        assertFalse(repo.consumeRecoveryNotice())
    }

    @Test
    fun auPlusTroisCopiesCorrompuesSontGardees() {
        val dir = tmp.newFolder()
        val repo = GardenRepository(dir)
        repeat(5) {
            java.io.File(dir, "sillon_garden.json").writeText("cassé $it")
            repo.loadGarden()
            Thread.sleep(3) // horodatages distincts
        }
        val copies = dir.listFiles { f -> f.name.startsWith("sillon_garden.json.corrupt-") }!!
        assertEquals(3, copies.size)
    }

    @Test
    fun saveGardenRenvoieFauxSiLEcritureEchoue() {
        val dir = tmp.newFolder()
        val repo = GardenRepository(java.io.File(dir, "inexistant"))
        assertFalse(repo.saveGarden(sample()))
    }

    @Test
    fun retrocompatibiliteAncienJsonSansChampsBalconOuInterieur() {
        val legacyJson = """
            {
              "id": "legacy_1",
              "name": "Potager v1.0",
              "frostDate": { "month": 4, "day": 20 },
              "parcels": [
                {
                  "id": "p1",
                  "name": "Planche Sud",
                  "type": "ROW_BED",
                  "rows": 2,
                  "cols": 3,
                  "slots": [
                    { "row": 0, "col": 0, "vegetableId": "radis", "plantedDate": 1700000000000 }
                  ]
                }
              ]
            }
        """.trimIndent()

        val decoded = GardenCodec.decodeAndValidate(legacyJson)
        assertEquals(GrowingEnvironment.OUTDOOR, decoded.environment)
        assertNull(decoded.orientation)
        assertNull(decoded.windExposure)
        assertFalse(decoded.covered)
        assertNull(decoded.lightSource)
        assertNull(decoded.growLightHoursPerDay)
        assertEquals(1, decoded.parcels.size)
        assertNull(decoded.parcels[0].containerVolumeLiters)
    }

    @Test
    fun serialisationBalconEtInterieurEtValidationBornes() {
        val balconyGarden = Garden(
            id = "balcon_1",
            name = "Mon Balcon",
            frostDate = FrostDate(4, 15),
            environment = GrowingEnvironment.BALCONY,
            orientation = Orientation.SE,
            windExposure = WindExposure.MEDIUM,
            covered = true,
            parcels = listOf(
                Parcel(
                    id = "bac_1",
                    name = "Jardinière aromatique",
                    type = ParcelType.CONTAINER,
                    rows = 1,
                    cols = 3,
                    containerVolumeLiters = 25,
                    slots = listOf(ParcelSlot(0, 0, "basilic", 1_700_000_000_000L)),
                )
            ),
        )
        val encodedBalcony = GardenCodec.encode(balconyGarden)
        assertEquals(balconyGarden, GardenCodec.decodeAndValidate(encodedBalcony))

        val indoorGarden = Garden(
            id = "indoor_1",
            name = "Étagère Salon",
            frostDate = FrostDate(5, 1),
            environment = GrowingEnvironment.INDOOR,
            lightSource = LightSource.GROW_LIGHT,
            growLightHoursPerDay = 14,
            parcels = listOf(
                Parcel(
                    id = "shelf_1",
                    name = "Rebord cuisine",
                    type = ParcelType.WINDOWSILL,
                    rows = 1,
                    cols = 2,
                    containerVolumeLiters = 8,
                ),
                Parcel(
                    id = "shelf_2",
                    name = "Sous lampe LED",
                    type = ParcelType.GROW_LIGHT_SHELF,
                    rows = 1,
                    cols = 4,
                    containerVolumeLiters = 12,
                ),
            ),
        )
        val encodedIndoor = GardenCodec.encode(indoorGarden)
        assertEquals(indoorGarden, GardenCodec.decodeAndValidate(encodedIndoor))

        val invalidHours = indoorGarden.copy(growLightHoursPerDay = 25)
        val invalidVolume = balconyGarden.copy(
            parcels = listOf(balconyGarden.parcels[0].copy(containerVolumeLiters = 0))
        )
        assertTrue(
            runCatching { GardenCodec.decodeAndValidate(GardenCodec.encode(invalidHours)) }.exceptionOrNull() is GardenValidationException
        )
        assertTrue(
            runCatching { GardenCodec.decodeAndValidate(GardenCodec.encode(invalidVolume)) }.exceptionOrNull() is GardenValidationException
        )
    }
}
