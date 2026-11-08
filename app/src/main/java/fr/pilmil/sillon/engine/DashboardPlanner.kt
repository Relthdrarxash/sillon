package fr.pilmil.sillon.engine

import fr.pilmil.sillon.model.FrostDate
import fr.pilmil.sillon.model.Garden
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Où en est-on par rapport à la date repère des dernières gelées. */
sealed interface FrostStatus {
    /** La prochaine date repère est dans [days] jours. */
    data class Upcoming(val date: LocalDate, val days: Long) : FrostStatus

    /** La date repère de cette saison est passée depuis [days] jours. */
    data class Passed(val date: LocalDate, val days: Long) : FrostStatus
}

/**
 * Ce que l'accueil affiche : les travaux de la quinzaine calculés sur la date de gelée,
 * ou, en morte-saison, les premiers travaux à venir et leur date.
 */
data class DashboardPlan(
    val frost: FrostStatus,
    val sowIndoors: List<CalendarTask>,
    val sowOutdoors: List<CalendarTask>,
    val transplant: List<CalendarTask>,
    val harvest: List<CalendarTask>,
    val gardenHarvest: List<HarvestTask>,
    /** Rempli seulement quand rien n'est à faire cette quinzaine : date des prochains travaux. */
    val nextWorkFrom: LocalDate?,
) {
    val isEmpty: Boolean
        get() = sowIndoors.isEmpty() && sowOutdoors.isEmpty() && transplant.isEmpty() && harvest.isEmpty() && gardenHarvest.isEmpty()
}

object DashboardPlanner {

    /** Une quinzaine : +/- 7 jours autour de la date idéale. */
    const val TOLERANCE_DAYS = 7

    /** Au-delà, la date repère « passée » n'intéresse plus : on compte jusqu'à celle de l'an prochain. */
    const val PASSED_RELEVANCE_DAYS = 90L

    private const val LOOKAHEAD_DAYS = 200

    fun frostStatus(frostDate: FrostDate, today: LocalDate): FrostStatus {
        val thisYear = safeDate(frostDate, today.year)
        val days = ChronoUnit.DAYS.between(today, thisYear)
        return when {
            days >= 0 -> FrostStatus.Upcoming(thisYear, days)
            -days <= PASSED_RELEVANCE_DAYS -> FrostStatus.Passed(thisYear, -days)
            else -> {
                val next = safeDate(frostDate, today.year + 1)
                FrostStatus.Upcoming(next, ChronoUnit.DAYS.between(today, next))
            }
        }
    }

    fun plan(garden: Garden, today: LocalDate, engine: FrostEngine = FrostEngine()): DashboardPlan {
        val frost = garden.frostDate
        val sowIndoors = engine.getSowIndoorsTasks(frost, today, TOLERANCE_DAYS)
        val sowOutdoors = engine.getSowOutdoorsTasks(frost, today, TOLERANCE_DAYS)
        val transplant = engine.getTransplantTasks(frost, today, TOLERANCE_DAYS)
        val harvest = engine.getEstimatedCatalogHarvestTasks(frost, today)
        val gardenHarvest = engine.getGardenHarvestTasks(garden, today)
        val nothing = sowIndoors.isEmpty() && sowOutdoors.isEmpty() && transplant.isEmpty() && harvest.isEmpty() && gardenHarvest.isEmpty()
        return DashboardPlan(
            frost = frostStatus(frost, today),
            sowIndoors = sowIndoors,
            sowOutdoors = sowOutdoors,
            transplant = transplant,
            harvest = harvest,
            gardenHarvest = gardenHarvest,
            nextWorkFrom = if (nothing) nextWorkDate(frost, today, engine) else null,
        )
    }

    /** Premier jour où un semis ou une plantation s'ouvre, dans les mois qui viennent. */
    fun nextWorkDate(frost: FrostDate, today: LocalDate, engine: FrostEngine = FrostEngine()): LocalDate? {
        for (offset in 1..LOOKAHEAD_DAYS) {
            val day = today.plusDays(offset.toLong())
            val hasWork = engine.getSowIndoorsTasks(frost, day, TOLERANCE_DAYS).isNotEmpty() ||
                engine.getSowOutdoorsTasks(frost, day, TOLERANCE_DAYS).isNotEmpty() ||
                engine.getTransplantTasks(frost, day, TOLERANCE_DAYS).isNotEmpty()
            if (hasWork) return day
        }
        return null
    }

    /** Le 29 février n'existe pas tous les ans : on retombe sur le 28. */
    private fun safeDate(frostDate: FrostDate, year: Int): LocalDate =
        runCatching { frostDate.toLocalDate(year) }.getOrElse { LocalDate.of(year, frostDate.month, 28) }
}
