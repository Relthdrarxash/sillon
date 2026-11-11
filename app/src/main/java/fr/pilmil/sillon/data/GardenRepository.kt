package fr.pilmil.sillon.data

import android.content.Context
import fr.pilmil.sillon.model.BotanicalFamily
import fr.pilmil.sillon.model.FrostDate
import fr.pilmil.sillon.model.Garden
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.ParcelSlot
import fr.pilmil.sillon.model.ParcelType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Résultat d'une modification : le potager à afficher, et si son enregistrement sur disque a réussi. */
data class GardenUpdate(val garden: Garden, val saved: Boolean)

class GardenRepository(private val dir: File) {

    constructor(context: Context) : this(context.filesDir)

    private val gardenFile: File
        get() = File(dir, FILE_NAME)

    @Volatile
    private var recoveryNotice = false

    /**
     * Vrai (une seule fois) si le dernier chargement a dû écarter un fichier illisible :
     * l'interface doit alors prévenir la personne que son potager est reparti de zéro.
     */
    fun consumeRecoveryNotice(): Boolean {
        val pending = recoveryNotice
        recoveryNotice = false
        return pending
    }

    /**
     * Charge le potager sauvegardé en local ou crée un potager initial bienveillant.
     * Un fichier illisible n'est jamais écrasé : il est mis de côté en `.corrupt-<horodatage>`.
     */
    @Synchronized
    fun loadGarden(): Garden {
        if (!gardenFile.exists()) return createDefaultGarden().also { saveGarden(it) }
        val content = try {
            gardenFile.readText(StandardCharsets.UTF_8)
        } catch (e: IOException) {
            // Lecture impossible (et non contenu invalide) : on n'écarte rien, on ne réécrit pas.
            e.printStackTrace()
            return createDefaultGarden()
        }
        return try {
            GardenCodec.decode(content)
        } catch (e: Exception) {
            e.printStackTrace()
            quarantineCorruptFile()
            recoveryNotice = true
            createDefaultGarden().also { saveGarden(it) }
        }
    }

    private fun quarantineCorruptFile() {
        val target = File(dir, "$FILE_NAME.corrupt-${System.currentTimeMillis()}")
        try {
            Files.move(gardenFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // On ne garde que les 3 copies les plus récentes (l'horodatage trie les noms).
        dir.listFiles { f -> f.name.startsWith("$FILE_NAME.corrupt-") }
            ?.sortedByDescending { it.name }
            ?.drop(MAX_CORRUPT_COPIES)
            ?.forEach { it.delete() }
    }

    /**
     * Sauvegarde atomique du potager : écriture dans un fichier temporaire, fsync, puis renommage.
     * Renvoie false si l'écriture a échoué (l'ancien fichier reste alors intact).
     */
    @Synchronized
    fun saveGarden(garden: Garden): Boolean {
        val tempFile = File(dir, "$FILE_NAME.tmp")
        return try {
            val bytes = GardenCodec.encode(garden).toByteArray(StandardCharsets.UTF_8)
            FileOutputStream(tempFile).use { out ->
                out.write(bytes)
                out.fd.sync()
            }
            Files.move(tempFile.toPath(), gardenFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    /**
     * Modifie une case spécifique d'une parcelle.
     */
    @Synchronized
    fun updateSlot(parcelId: String, row: Int, col: Int, vegetableId: String?): GardenUpdate {
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
        return GardenUpdate(updatedGarden, saveGarden(updatedGarden))
    }

    /**
     * Ajoute une nouvelle parcelle (ex. Carré potager 3x3 ou Planche en lignes).
     */
    @Synchronized
    fun addParcel(name: String, type: ParcelType, rows: Int, cols: Int): GardenUpdate {
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
        return GardenUpdate(updated, saveGarden(updated))
    }

    /**
     * Supprime une parcelle.
     */
    @Synchronized
    fun deleteParcel(parcelId: String): GardenUpdate {
        val current = loadGarden()
        val updated = current.copy(parcels = current.parcels.filterNot { it.id == parcelId })
        return GardenUpdate(updated, saveGarden(updated))
    }

    /**
     * Met à jour la date repère des dernières gelées.
     */
    @Synchronized
    fun updateFrostDate(month: Int, day: Int): GardenUpdate {
        val current = loadGarden()
        val updated = current.copy(frostDate = FrostDate(month, day))
        return GardenUpdate(updated, saveGarden(updated))
    }

    /**
     * Export 1-tap vers un flux OutputStream (SAF ou fichier de partage).
     * 100% hors-ligne, sans compte, export lisible et réimportable.
     */
    fun exportToJson(outputStream: OutputStream) {
        val garden = loadGarden()
        val content = GardenCodec.encode(garden)
        outputStream.write(content.toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }

    /**
     * Import d'une sauvegarde JSON : bornes et catalogue validés, ancien potager copié en `.bak` avant remplacement.
     * L'échec porte un [GardenValidationException] quand le fichier est refusé.
     */
    @Synchronized
    fun importFromJson(inputStream: InputStream): Result<Garden> {
        return try {
            val content = inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                val buffer = CharArray(GardenCodec.MAX_FILE_CHARS + 1)
                val read = reader.readFully(buffer)
                String(buffer, 0, read)
            }
            val importedGarden = GardenCodec.decodeAndValidate(content)
            if (gardenFile.exists()) {
                Files.copy(gardenFile.toPath(), File(dir, "$FILE_NAME.bak").toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            if (!saveGarden(importedGarden)) throw IOException("Écriture du potager importé impossible")
            Result.success(importedGarden)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun java.io.Reader.readFully(buffer: CharArray): Int {
        var total = 0
        while (total < buffer.size) {
            val n = read(buffer, total, buffer.size - total)
            if (n < 0) break
            total += n
        }
        return total
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

    private companion object {
        const val FILE_NAME = "sillon_garden.json"
        const val MAX_CORRUPT_COPIES = 3
    }
}
