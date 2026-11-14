package fr.pilmil.sillon.engine

/**
 * Stade d'une culture entre sa mise en place et la fin de sa récolte.
 *
 * Les noms restent neutres : la date enregistrée peut être un semis comme un repiquage,
 * on ne parle donc ni de « graine » ni de « levée ». Le libellé accompagne toujours le
 * pictogramme : aucune information ne passe par l'image ou la couleur seule.
 */
enum class GrowthStage(val frenchLabel: String) {
    SETTLING("Installation"),
    YOUNG("Jeune plant"),
    GROWING("En croissance"),
    ALMOST_READY("Bientôt prête"),
    READY("À récolter"),
    FINISHED("Récolte terminée"),
}

/**
 * État d'une culture à une date donnée.
 *
 * @property progress avancement vers le début de la récolte, de 0 à 1.
 * @property daysUntilHarvest fourchette de jours avant le début de la récolte (du plus tôt au plus
 *   tard), ou `null` une fois la récolte commencée.
 * @property harvestDaysLeft jours de récolte restants, ou `null` hors période de récolte.
 */
data class Growth(
    val stage: GrowthStage,
    val progress: Float,
    val daysUntilHarvest: IntRange?,
    val harvestDaysLeft: Int?,
) {
    /** Phrase courte pour l'affichage et les lecteurs d'écran : « En croissance, récolte dans 12 à 20 jours ». */
    fun summary(): String = when {
        stage == GrowthStage.FINISHED -> "${stage.frenchLabel} : la place peut être libérée"
        harvestDaysLeft != null -> when {
            harvestDaysLeft <= 0 -> "${stage.frenchLabel}, dernier jour de récolte"
            harvestDaysLeft == 1 -> "${stage.frenchLabel}, encore 1 jour"
            else -> "${stage.frenchLabel}, encore environ $harvestDaysLeft jours"
        }
        daysUntilHarvest != null -> "${stage.frenchLabel}, récolte dans ${formatRange(daysUntilHarvest)}"
        else -> stage.frenchLabel
    }

    private fun formatRange(range: IntRange): String = when {
        range.last <= 0 -> "quelques jours"
        range.first <= 0 -> if (range.last == 1) "1 jour au plus" else "${range.last} jours au plus"
        range.first == range.last -> if (range.first == 1) "1 jour" else "${range.first} jours"
        else -> "${range.first} à ${range.last} jours"
    }
}

/**
 * Calcule le stade d'une culture à partir des durées du catalogue. Mêmes bornes que les tâches de
 * récolte de [FrostEngine] : la récolte commence au plus tôt à `daysToHarvest.first` et se termine
 * à `daysToHarvest.last` plus la durée de récolte.
 */
object GrowthEngine {

    private const val SETTLING_END = 0.15f
    private const val YOUNG_END = 0.40f
    private const val GROWING_END = 0.80f

    fun growth(
        daysSincePlanting: Int,
        daysToHarvest: IntRange,
        harvestDurationWeeks: Int,
    ): Growth {
        val days = daysSincePlanting.coerceAtLeast(0)
        val harvestStart = daysToHarvest.first.coerceAtLeast(1)
        val harvestEnd = daysToHarvest.last.coerceAtLeast(harvestStart) + harvestDurationWeeks.coerceAtLeast(0) * 7

        if (days > harvestEnd) {
            return Growth(GrowthStage.FINISHED, 1f, null, null)
        }
        if (days >= harvestStart) {
            return Growth(GrowthStage.READY, 1f, null, harvestEnd - days)
        }

        val progress = days.toFloat() / harvestStart
        val stage = when {
            progress < SETTLING_END -> GrowthStage.SETTLING
            progress < YOUNG_END -> GrowthStage.YOUNG
            progress < GROWING_END -> GrowthStage.GROWING
            else -> GrowthStage.ALMOST_READY
        }
        val latest = daysToHarvest.last.coerceAtLeast(harvestStart)
        return Growth(stage, progress, (harvestStart - days)..(latest - days), null)
    }
}
