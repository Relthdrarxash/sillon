package fr.pilmil.sillon.data

import fr.pilmil.sillon.model.Garden
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Une sauvegarde refusée, avec un message destiné à la personne (pas à un journal). */
class GardenValidationException(message: String) : Exception(message)

/**
 * Sérialisation et validation du potager, sans dépendance Android : testable en JVM pure.
 */
object GardenCodec {
    const val MAX_PARCELS = 100
    const val MAX_SIDE = 50
    const val MAX_FILE_CHARS = 5_000_000
    private const val MIN_DATE = 946_684_800_000L // 1er janvier 2000
    private const val DAY_MS = 86_400_000L

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(garden: Garden): String = json.encodeToString(garden)

    /** Lecture brute, sans contrôle des bornes : sert au fichier local écrit par l'appli elle-même. */
    fun decode(content: String): Garden = json.decodeFromString<Garden>(content)

    /** Lecture d'un fichier importé : refuse tout ce qui sortirait du cadre de l'appli. */
    fun decodeAndValidate(content: String, now: Long = System.currentTimeMillis()): Garden {
        if (content.length > MAX_FILE_CHARS) throw GardenValidationException("Ce fichier est trop volumineux pour être une sauvegarde Sillon.")
        val garden = try {
            decode(content)
        } catch (e: Exception) {
            throw GardenValidationException("Ce fichier n'est pas une sauvegarde Sillon lisible. Choisissez le fichier .json exporté depuis Sillon.")
        }
        validate(garden, now)
        return garden
    }

    fun validate(garden: Garden, now: Long = System.currentTimeMillis()) {
        fun refuse(reason: String): Nothing = throw GardenValidationException("Sauvegarde refusée : $reason")
        if (garden.parcels.size > MAX_PARCELS) refuse("plus de $MAX_PARCELS parcelles.")
        if (garden.parcels.map { it.id }.toSet().size != garden.parcels.size) refuse("deux parcelles portent le même identifiant.")
        garden.growLightHoursPerDay?.let { hours ->
            if (hours !in 1..24) refuse("durée d'éclairage invalide ($hours h/j, doit être entre 1 et 24).")
        }
        garden.parcels.forEach { parcel ->
            if (parcel.rows !in 1..MAX_SIDE || parcel.cols !in 1..MAX_SIDE) {
                refuse("la parcelle « ${parcel.name} » doit avoir entre 1 et $MAX_SIDE lignes et colonnes (reçu : ${parcel.rows} × ${parcel.cols}).")
            }
            parcel.containerVolumeLiters?.let { vol ->
                if (vol !in 1..2000) refuse("volume de pot invalide dans « ${parcel.name} » ($vol L).")
            }
            val seen = HashSet<Pair<Int, Int>>()
            parcel.slots.forEach { slot ->
                if (slot.row !in 0 until parcel.rows || slot.col !in 0 until parcel.cols) {
                    refuse("une case de « ${parcel.name} » est hors de la parcelle (ligne ${slot.row + 1}, colonne ${slot.col + 1}).")
                }
                if (!seen.add(slot.row to slot.col)) refuse("« ${parcel.name} » contient deux plantes dans la même case.")
                slot.vegetableId?.let { id ->
                    if (VegetableCatalog.getById(id) == null) refuse("plante inconnue « $id ».")
                }
                slot.plantedDate?.let { date ->
                    if (date < MIN_DATE || date > now + DAY_MS) refuse("date de plantation invalide dans « ${parcel.name} ».")
                }
            }
        }
    }
}
