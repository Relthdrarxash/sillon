package fr.pilmil.sillon.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.MonthDay

@Serializable
data class FrostDate(
    val month: Int,
    val day: Int,
) {
    init {
        require(month in 1..12) { "Le mois doit être compris entre 1 et 12 (reçu: $month)" }
        require(day in 1..31) { "Le jour doit être compris entre 1 et 31 (reçu: $day)" }
    }

    fun toMonthDay(): MonthDay = MonthDay.of(month, day)

    fun toLocalDate(year: Int): LocalDate = LocalDate.of(year, month, day)

    companion object {
        fun of(month: Int, day: Int): FrostDate = FrostDate(month, day)
        fun from(monthDay: MonthDay): FrostDate = FrostDate(monthDay.monthValue, monthDay.dayOfMonth)
        fun from(localDate: LocalDate): FrostDate = FrostDate(localDate.monthValue, localDate.dayOfMonth)
    }
}

@Serializable
enum class ParcelType(val frenchLabel: String) {
    RAISED_BED_SQUARES("Carré potager surélevé"),
    ROW_BED("Planche en lignes"),
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
