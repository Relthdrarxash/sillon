package fr.pilmil.sillon.data

import android.content.Context
import fr.pilmil.sillon.model.BotanicalFamily
import fr.pilmil.sillon.model.FrostDate
import fr.pilmil.sillon.model.Garden
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.ParcelSlot
import fr.pilmil.sillon.model.ParcelType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class GardenRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val gardenFile: File
        get() = File(context.filesDir, "sillon_garden.json")

    /**
     * Charge le potager sauvegardé en local ou crée un potager initial bienveillant.
     */
    fun loadGarden(): Garden {
        return try {
            if (gardenFile.exists()) {
                val content = gardenFile.readText(StandardCharsets.UTF_8)
                json.decodeFromString<Garden>(content)
            } else {
                createDefaultGarden().also { saveGarden(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            createDefaultGarden()
        }
    }

    /**
     * Sauvegarde atomique du potager en local.
     */
    fun saveGarden(garden: Garden) {
        try {
            val content = json.encodeToString(garden)
            val tempFile = File(context.filesDir, "sillon_garden.json.tmp")
            tempFile.writeText(content, StandardCharsets.UTF_8)
            tempFile.renameTo(gardenFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Modifie une case spécifique d'une parcelle.
     */
    fun updateSlot(parcelId: String, row: Int, col: Int, vegetableId: String?): Garden {
        val currentGarden = loadGarden()
        val updatedParcels = currentGarden.parcels.map { parcel ->
            if (parcel.id == parcelId) {
                val existingSlot = parcel.slots.find { it.row == row && it.col == col }
                val newSlots = parcel.slots.filterNot { it.row == row && it.col == col }.toMutableList()
                if (vegetableId != null) {
                    newSlots.add(
                        existingSlot?.copy(
                            vegetableId = vegetableId,
                            plantedDate = System.currentTimeMillis(),
                        ) ?: ParcelSlot(row = row, col = col, vegetableId = vegetableId, plantedDate = System.currentTimeMillis())
                    )
                }
                parcel.copy(slots = newSlots)
            } else {
                parcel
            }
        }
        val updatedGarden = currentGarden.copy(parcels = updatedParcels)
        saveGarden(updatedGarden)
        return updatedGarden
    }

    /**
     * Ajoute une nouvelle parcelle (ex. Carré potager 3x3 ou Planche en lignes).
     */
    fun addParcel(name: String, type: ParcelType, rows: Int, cols: Int): Garden {
        val currentGarden = loadGarden()
        val newParcel = Parcel(
            id = "parcel_${System.currentTimeMillis()}",
            name = name,
            type = type,
            rows = rows,
            cols = cols,
            slots = emptyList(),
        )
        val updated = currentGarden.copy(parcels = currentGarden.parcels + newParcel)
        saveGarden(updated)
        return updated
    }

    /**
     * Supprime une parcelle.
     */
    fun deleteParcel(parcelId: String): Garden {
        val current = loadGarden()
        val updated = current.copy(parcels = current.parcels.filterNot { it.id == parcelId })
        saveGarden(updated)
        return updated
    }

    /**
     * Met à jour la date repère des dernières gelées.
     */
    fun updateFrostDate(month: Int, day: Int): Garden {
        val current = loadGarden()
        val updated = current.copy(frostDate = FrostDate(month, day))
        saveGarden(updated)
        return updated
    }

    /**
     * Export 1-tap vers un flux OutputStream (SAF ou fichier de partage).
     * 100% hors-ligne, sans compte, export lisible et réimportable.
     */
    fun exportToJson(outputStream: OutputStream) {
        val garden = loadGarden()
        val content = json.encodeToString(garden)
        outputStream.write(content.toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }

    /**
     * Import d'une sauvegarde JSON avec validation de structure.
     */
    fun importFromJson(inputStream: InputStream): Result<Garden> {
        return try {
            val content = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val importedGarden = json.decodeFromString<Garden>(content)
            saveGarden(importedGarden)
            Result.success(importedGarden)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export CSV pour tableur (LibreOffice Calc, Excel, etc.).
     */
    fun exportToCsv(outputStream: OutputStream) {
        val garden = loadGarden()
        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        writer.use { out ->
            // BOM UTF-8 pour Excel
            out.write("\uFEFF")
            out.write("Parcelle;Type;Ligne;Colonne;Plante ID;Date Plantation\n")
            garden.parcels.forEach { parcel ->
                for (r in 0 until parcel.rows) {
                    for (c in 0 until parcel.cols) {
                        val slot = parcel.getSlot(r, c)
                        out.write(
                            "${parcel.name};${parcel.type.frenchLabel};${r + 1};${c + 1};" +
                            "${slot?.vegetableId ?: "Vide"};${slot?.plantedDate ?: ""}\n"
                        )
                    }
                }
            }
        }
    }

    /**
     * Modèle initial pédagogique : Carré de départ 3x3 (9 carrés de 30x30 cm).
     */
    private fun createDefaultGarden(): Garden {
        val sampleSlots = listOf(
            ParcelSlot(row = 0, col = 0, vegetableId = "radis"),
            ParcelSlot(row = 0, col = 1, vegetableId = "carotte"),
            ParcelSlot(row = 0, col = 2, vegetableId = "oignon"),
            ParcelSlot(row = 1, col = 0, vegetableId = "laitue"),
            ParcelSlot(row = 1, col = 1, vegetableId = "basilic"),
            ParcelSlot(row = 1, col = 2, vegetableId = "tomate"),
            ParcelSlot(row = 2, col = 0, vegetableId = "epinard"),
            ParcelSlot(row = 2, col = 1, vegetableId = "haricot_nain"),
            ParcelSlot(row = 2, col = 2, vegetableId = "tagete"),
        )

        val defaultParcel = Parcel(
            id = "parcel_demo_1",
            name = "Carré Vivant n°1 (1,20 m)",
            type = ParcelType.RAISED_BED_SQUARES,
            rows = 3,
            cols = 3,
            slots = sampleSlots,
        )

        return Garden(
            id = "garden_local",
            name = "Mon Potager",
            frostDate = FrostDate(month = 5, day = 15), // Par défaut mi-mai (Saints de Glace)
            parcels = listOf(defaultParcel),
        )
    }
}
