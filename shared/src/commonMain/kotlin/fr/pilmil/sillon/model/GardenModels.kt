package fr.pilmil.sillon.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class FrostDate(
    val month: Int,
    val day: Int,
) {
    init {
        require(month in 1..12) { "Le mois doit être compris entre 1 et 12 (reçu: $month)" }
        require(day in 1..31) { "Le jour doit être compris entre 1 et 31 (reçu: $day)" }
    }

    fun toLocalDate(year: Int): LocalDate = LocalDate(year, month, day)

    companion object {
        fun of(month: Int, day: Int): FrostDate = FrostDate(month, day)
        fun from(localDate: LocalDate): FrostDate = FrostDate(localDate.monthNumber, localDate.dayOfMonth)
    }
}

@Serializable
enum class GrowingEnvironment(val frenchLabel: String) {
    OUTDOOR("Pleine terre"),
    BALCONY("Balcon / terrasse"),
    INDOOR("Intérieur"),
}

@Serializable
enum class Orientation(val frenchLabel: String) {
    N("Nord"),
    NE("Nord-Est"),
    E("Est"),
    SE("Sud-Est"),
    S("Sud"),
    SW("Sud-Ouest"),
    W("Ouest"),
    NW("Nord-Ouest"),
}

@Serializable
enum class WindExposure(val frenchLabel: String) {
    LOW("Faible"),
    MEDIUM("Modérée"),
    HIGH("Forte"),
}

@Serializable
enum class LightSource(val frenchLabel: String) {
    WINDOW("Fenêtre"),
    GROW_LIGHT("Lampe horticole"),
}

@Serializable
enum class ParcelType(val frenchLabel: String) {
    RAISED_BED_SQUARES("Carré potager surélevé"),
    ROW_BED("Planche en lignes"),
    CONTAINER("Pot / jardinière"),
    WINDOWSILL("Rebord de fenêtre"),
    GROW_LIGHT_SHELF("Étagère sous lampe"),
    ;

    val isContainerBased: Boolean
        get() = this == CONTAINER || this == WINDOWSILL || this == GROW_LIGHT_SHELF
}

@Serializable
data class ParcelSlot(
    val row: Int,
    val col: Int,
    val vegetableId: String? = null,
    val plantedDate: Long? = null,
    val notes: String? = null,
)

@Serializable
data class Parcel(
    val id: String,
    val name: String,
    val type: ParcelType,
    val rows: Int,
    val cols: Int,
    val slots: List<ParcelSlot> = emptyList(),
    val containerVolumeLiters: Int? = null,
) {
    fun getSlot(row: Int, col: Int): ParcelSlot? =
        slots.find { it.row == row && it.col == col }
}

@Serializable
data class Garden(
    val id: String,
    val name: String,
    val frostDate: FrostDate,
    val parcels: List<Parcel> = emptyList(),
    val environment: GrowingEnvironment = GrowingEnvironment.OUTDOOR,
    val orientation: Orientation? = null,
    val windExposure: WindExposure? = null,
    val covered: Boolean = false,
    val lightSource: LightSource? = null,
    val growLightHoursPerDay: Int? = null,
)

@Serializable
data class CropHistoryEntry(
    val parcelId: String,
    val row: Int,
    val col: Int,
    val year: Int,
    val family: BotanicalFamily,
    val vegetableId: String,
)
