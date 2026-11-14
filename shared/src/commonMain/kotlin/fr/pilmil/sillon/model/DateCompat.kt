package fr.pilmil.sillon.model

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

fun LocalDate.plusDays(days: Long): LocalDate = plus(DatePeriod(days = days.toInt()))

fun LocalDate.minusDays(days: Long): LocalDate = minus(DatePeriod(days = days.toInt()))

fun LocalDate.plusWeeks(weeks: Long): LocalDate = plus(DatePeriod(days = (weeks * 7).toInt()))

fun LocalDate.minusWeeks(weeks: Long): LocalDate = minus(DatePeriod(days = (weeks * 7).toInt()))

fun daysBetween(from: LocalDate, to: LocalDate): Long = from.daysUntil(to).toLong()

fun epochMillisToLocalDate(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date

fun todayLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

private val FRENCH_MONTHS = arrayOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

fun formatDayMonthFr(date: LocalDate): String =
    "${date.dayOfMonth} ${FRENCH_MONTHS[date.monthNumber - 1]}"
