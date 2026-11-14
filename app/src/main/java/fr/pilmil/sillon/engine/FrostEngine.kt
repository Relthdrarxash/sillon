package fr.pilmil.sillon.engine

import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.model.Climate
import fr.pilmil.sillon.model.FrostDate
import fr.pilmil.sillon.model.Garden
import fr.pilmil.sillon.model.Vegetable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class GardeningAction(val frenchLabel: String) {
    SOW_INDOORS("Semis sous abri / au chaud"),
    SOW_OUTDOORS("Semis direct en pleine terre"),
    TRANSPLANT("Repiquage / Plantation au potager"),
    HARVEST("Récolte"),
}

data class CalendarTask(
    val vegetable: Vegetable,
    val action: GardeningAction,
    val targetDate: LocalDate,
    val windowStart: LocalDate,
    val windowEnd: LocalDate,
    val daysUntilTarget: Long,
    val description: String,
)

data class HarvestTask(
    val parcelId: String,
    val parcelName: String,
    val row: Int,
    val col: Int,
    val vegetable: Vegetable,
    val plantedDate: LocalDate,
    val harvestWindowStart: LocalDate,
    val harvestWindowEnd: LocalDate,
    val daysRemainingInHarvest: Long,
    val description: String,
)

/**
 * Calendrier ancré sur [FrostDate] : la dernière gelée en [Climate.TEMPERATE], le début de la saison de
 * plantation du territoire en [Climate.TROPICAL]. Seules les plantes qui ont un calendrier dans ce climat
 * produisent des tâches.
 */
class FrostEngine(
    private val defaultCatalog: List<Vegetable> = VegetableCatalog.ALL_VEGETABLES,
    val climate: Climate = Climate.TEMPERATE,
) {

    private val anchorName = if (climate == Climate.TROPICAL) "le début de saison" else "la dernière gelée"

    private fun List<Vegetable>.forClimate(): List<Vegetable> = filter { climate in it.climates }

    /**
     * Dates de l'ancre à essayer. Sous les tropiques la saison peut commencer en fin d'année (1er novembre) :
     * un semis de janvier se rattache à l'ancre de l'année précédente, une récolte longue à celle d'il y a deux ans.
     */
    private fun anchors(frostDate: FrostDate, currentDate: LocalDate): List<LocalDate> =
        if (climate == Climate.TROPICAL) (-2..1).map { getLastFrostDate(frostDate, currentDate.year + it) }
        else listOf(getLastFrostDate(frostDate, currentDate.year))

    /** Position relative à l'ancre, en toutes lettres (« 3 sem. après la dernière gelée »). */
    private fun relative(weeks: Int): String =
        if (climate == Climate.TROPICAL) "${abs(weeks)} sem. ${if (weeks >= 0) "après" else "avant"} le début de saison"
        else if (weeks >= 0) "$weeks sem. après gelée" else "${abs(weeks)} sem. avant dernière gelée"

    /**
     * Calcule la date de dernière gelée pour l'année courante de référence.
     */
    fun getLastFrostDate(frostDate: FrostDate, year: Int): LocalDate {
        return frostDate.toLocalDate(year)
    }

    /**
     * Détermine si une date cible se situe dans la fenêtre active (par exemple une quinzaine: +/- 7 jours).
     */
    fun isWithinWindow(targetDate: LocalDate, currentDate: LocalDate, toleranceDays: Int = 7): Boolean {
        val days = abs(ChronoUnit.DAYS.between(targetDate, currentDate))
        return days <= toleranceDays
    }

    /**
     * Tâches de semis sous abri / en pépinière pour la quinzaine courante.
     * Basé sur [Vegetable.sowIndoorsWeeksBeforeFrost].
     */
    fun getSowIndoorsTasks(
        frostDate: FrostDate,
        currentDate: LocalDate,
        toleranceDays: Int = 7,
        catalog: List<Vegetable> = defaultCatalog,
    ): List<CalendarTask> {
        val anchors = anchors(frostDate, currentDate)

        return catalog.forClimate().mapNotNull { veg ->
            val weeksBefore = veg.sowIndoorsWeeksBeforeFrost ?: return@mapNotNull null
            val targetDate = anchors.map { it.minusWeeks(weeksBefore.toLong()) }
                .firstOrNull { currentDate in it.minusDays(toleranceDays.toLong())..it.plusDays(toleranceDays.toLong()) }
                ?: return@mapNotNull null
            val windowStart = targetDate.minusDays(toleranceDays.toLong())
            val windowEnd = targetDate.plusDays(toleranceDays.toLong())

            CalendarTask(
                vegetable = veg,
                action = GardeningAction.SOW_INDOORS,
                targetDate = targetDate,
                windowStart = windowStart,
                windowEnd = windowEnd,
                daysUntilTarget = ChronoUnit.DAYS.between(currentDate, targetDate),
                description = "Semer '${veg.name}' en terrine ou godets au chaud ($weeksBefore sem. avant $anchorName). Température min: ${veg.tempGerminationMin}°C.",
            )
        }.sortedBy { abs(it.daysUntilTarget) }
    }

    /**
     * Tâches de semis direct en pleine terre pour la période courante.
     * Basé sur [Vegetable.sowOutdoorsWeeksAfterFrost].
     */
    fun getSowOutdoorsTasks(
        frostDate: FrostDate,
        currentDate: LocalDate,
        toleranceDays: Int = 7,
        catalog: List<Vegetable> = defaultCatalog,
    ): List<CalendarTask> {
        val anchors = anchors(frostDate, currentDate)

        return catalog.forClimate().mapNotNull { veg ->
            val weeksAfter = veg.sowOutdoorsWeeksAfterFrost ?: return@mapNotNull null
            val targetDate = anchors.map { it.plusWeeks(weeksAfter.toLong()) }
                .firstOrNull { currentDate in it.minusDays(toleranceDays.toLong())..it.plusDays(toleranceDays.toLong()) }
                ?: return@mapNotNull null
            val windowStart = targetDate.minusDays(toleranceDays.toLong())
            val windowEnd = targetDate.plusDays(toleranceDays.toLong())

            val weekInfo = relative(weeksAfter)
            CalendarTask(
                vegetable = veg,
                action = GardeningAction.SOW_OUTDOORS,
                targetDate = targetDate,
                windowStart = windowStart,
                windowEnd = windowEnd,
                daysUntilTarget = ChronoUnit.DAYS.between(currentDate, targetDate),
                description = "Semer '${veg.name}' directement en place ($weekInfo). Espacement dans le rang: ${veg.spacingInRowCm} cm.",
            )
        }.sortedBy { abs(it.daysUntilTarget) }
    }

    /**
     * Tâches de repiquage / plantation en extérieur pour la période courante.
     * Basé sur [Vegetable.transplantWeeksAfterFrost].
     */
    fun getTransplantTasks(
        frostDate: FrostDate,
        currentDate: LocalDate,
        toleranceDays: Int = 7,
        catalog: List<Vegetable> = defaultCatalog,
    ): List<CalendarTask> {
        val anchors = anchors(frostDate, currentDate)

        return catalog.forClimate().mapNotNull { veg ->
            val weeksAfter = veg.transplantWeeksAfterFrost ?: return@mapNotNull null
            val targetDate = anchors.map { it.plusWeeks(weeksAfter.toLong()) }
                .firstOrNull { currentDate in it.minusDays(toleranceDays.toLong())..it.plusDays(toleranceDays.toLong()) }
                ?: return@mapNotNull null
            val windowStart = targetDate.minusDays(toleranceDays.toLong())
            val windowEnd = targetDate.plusDays(toleranceDays.toLong())

            val weekInfo = relative(weeksAfter)
            CalendarTask(
                vegetable = veg,
                action = GardeningAction.TRANSPLANT,
                targetDate = targetDate,
                windowStart = windowStart,
                windowEnd = windowEnd,
                daysUntilTarget = ChronoUnit.DAYS.between(currentDate, targetDate),
                description = "Repiquer '${veg.name}' en pleine terre ($weekInfo). Densité au carré: ${veg.squareFootDensity}/30x30cm.",
            )
        }.sortedBy { abs(it.daysUntilTarget) }
    }

    /**
     * Tâches de récolte effectives pour les carrés/rangs plantés d'un jardin.
     */
    fun getGardenHarvestTasks(
        garden: Garden,
        currentDate: LocalDate,
        catalogMap: Map<String, Vegetable> = VegetableCatalog.BY_ID,
    ): List<HarvestTask> {
        val results = mutableListOf<HarvestTask>()

        for (parcel in garden.parcels) {
            for (slot in parcel.slots) {
                val vegId = slot.vegetableId ?: continue
                val plantedEpoch = slot.plantedDate ?: continue
                val veg = catalogMap[vegId] ?: continue

                val plantedDate = Instant.ofEpochMilli(plantedEpoch)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val harvestStart = plantedDate.plusDays(veg.daysToHarvest.first.toLong())
                val harvestEnd = plantedDate.plusDays(
                    (veg.daysToHarvest.last + (veg.harvestDurationWeeks * 7)).toLong()
                )

                if (currentDate in harvestStart..harvestEnd) {
                    val remainingDays = ChronoUnit.DAYS.between(currentDate, harvestEnd)
                    results.add(
                        HarvestTask(
                            parcelId = parcel.id,
                            parcelName = parcel.name,
                            row = slot.row,
                            col = slot.col,
                            vegetable = veg,
                            plantedDate = plantedDate,
                            harvestWindowStart = harvestStart,
                            harvestWindowEnd = harvestEnd,
                            daysRemainingInHarvest = remainingDays,
                            description = "Récolter '${veg.name}' en ${parcel.name} (case [${slot.row}, ${slot.col}]). Période de récolte active (reste env. $remainingDays jours).",
                        )
                    )
                }
            }
        }

        return results.sortedBy { it.daysRemainingInHarvest }
    }

    /**
     * Tâches de récolte estimées pour l'ensemble du catalogue en fonction de la date de gelée.
     */
    fun getEstimatedCatalogHarvestTasks(
        frostDate: FrostDate,
        currentDate: LocalDate,
        catalog: List<Vegetable> = defaultCatalog,
    ): List<CalendarTask> {
        val anchors = anchors(frostDate, currentDate)

        return catalog.forClimate().mapNotNull { veg ->
            // Semaines de plantation relatives à l'ancre : repiquage d'abord, sinon semis en place.
            val plantWeeks = veg.transplantWeeksAfterFrost ?: veg.sowOutdoorsWeeksAfterFrost ?: return@mapNotNull null

            anchors.firstNotNullOfOrNull { anchor ->
                val plantDate = anchor.plusWeeks(plantWeeks.toLong())
                val harvestStart = plantDate.plusDays(veg.daysToHarvest.first.toLong())
                val harvestEnd = plantDate.plusDays((veg.daysToHarvest.last + (veg.harvestDurationWeeks * 7)).toLong())

                if (currentDate in harvestStart..harvestEnd) {
                    CalendarTask(
                        vegetable = veg,
                        action = GardeningAction.HARVEST,
                        targetDate = harvestStart,
                        windowStart = harvestStart,
                        windowEnd = harvestEnd,
                        daysUntilTarget = ChronoUnit.DAYS.between(currentDate, harvestStart),
                        description = "Fenêtre de récolte de '${veg.name}' ouverte (durée estimée: ${veg.harvestDurationWeeks} semaines).",
                    )
                } else {
                    null
                }
            }
        }.sortedBy { it.vegetable.name }
    }

    /**
     * Retourne l'ensemble de toutes les tâches dynamiques du calendrier pour le jour courant.
     */
    fun getAllCalendarTasks(
        frostDate: FrostDate,
        currentDate: LocalDate,
        toleranceDays: Int = 7,
        catalog: List<Vegetable> = defaultCatalog,
    ): List<CalendarTask> {
        return getSowIndoorsTasks(frostDate, currentDate, toleranceDays, catalog) +
            getSowOutdoorsTasks(frostDate, currentDate, toleranceDays, catalog) +
            getTransplantTasks(frostDate, currentDate, toleranceDays, catalog) +
            getEstimatedCatalogHarvestTasks(frostDate, currentDate, catalog)
    }
}
