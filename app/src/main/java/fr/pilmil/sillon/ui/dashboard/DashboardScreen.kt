package fr.pilmil.sillon.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.pilmil.sillon.engine.DashboardPlan
import fr.pilmil.sillon.engine.FrostStatus
import fr.pilmil.sillon.ui.components.SillonBadge
import fr.pilmil.sillon.ui.components.SillonCard
import fr.pilmil.sillon.ui.theme.SillonPalette
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH)

/** Horizon temporel affiché dans le carrousel prévisionnel de l'accueil. */
data class ForecastHorizon(
    val label: String,
    val rangeHint: String,
    val plan: DashboardPlan,
)

/** Surcharge de compatibilité pour les aperçus et tests passant un [DashboardPlan] unique. */
@Composable
fun DashboardScreen(
    plan: DashboardPlan,
    placeLabel: String?,
    tip: Pair<String, String>?,
    onNavigateToGarden: () -> Unit = {},
    onNavigateToCatalog: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    DashboardScreen(
        horizons = listOf(ForecastHorizon("Aujourd'hui", "Travaux autour du jour J", plan)),
        placeLabel = placeLabel,
        tip = tip,
        onNavigateToGarden = onNavigateToGarden,
        onNavigateToCatalog = onNavigateToCatalog,
        onOpenSettings = onOpenSettings,
    )
}

/**
 * Accueil : les travaux calculés à partir de la date de gelée du jardin,
 * avec glissement horizontal sur les prochains jours.
 */
@Composable
fun DashboardScreen(
    horizons: List<ForecastHorizon>,
    placeLabel: String?,
    tip: Pair<String, String>?,
    showIntroHints: Boolean = true,
    activeGardenMarker: String? = null,
    onNavigateToGarden: () -> Unit = {},
    onNavigateToCatalog: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val currentPlan = horizons.first().plan
    val pagerState = rememberPagerState(pageCount = { horizons.size })
    val scope = rememberCoroutineScope()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            SillonPalette.Sage.copy(alpha = 0.11f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background,
        ),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Au potager aujourd'hui",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(modifier = Modifier.height(4.dp))
                val subtitle = when {
                    activeGardenMarker != null -> activeGardenMarker
                    showIntroHints -> "Calculé pour votre date de dernières gelées."
                    else -> null
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { FrostCard(currentPlan.frost, placeLabel, onOpenSettings) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Prévisions",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        horizons.indices.forEach { index ->
                            val selected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(if (selected) 20.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) SillonPalette.Sage else MaterialTheme.colorScheme.outline,
                                    ),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    horizons.forEachIndexed { index, horizon ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selected) SillonPalette.Sage else MaterialTheme.colorScheme.surface,
                                )
                                .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = horizon.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 12.dp,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    HorizonPageContent(
                        horizon = horizons[page],
                        onNavigateToGarden = onNavigateToGarden,
                        onNavigateToCatalog = onNavigateToCatalog,
                    )
                }
            }
        }

        if (tip != null && (showIntroHints || activeGardenMarker != null)) {
            val (title, text) = tip
            item {
                SillonCard(
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    borderColor = MaterialTheme.colorScheme.outline,
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizonPageContent(
    horizon: ForecastHorizon,
    onNavigateToGarden: () -> Unit,
    onNavigateToCatalog: () -> Unit,
) {
    val plan = horizon.plan
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = horizon.rangeHint,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (plan.isEmpty) {
            RestCard(plan.nextWorkFrom, onNavigateToCatalog)
        } else {
            if (plan.gardenHarvest.isNotEmpty()) {
                TaskCard(
                    title = "À récolter dans vos parcelles",
                    names = plan.gardenHarvest.map { "${it.vegetable.name} (${it.parcelName})" },
                    icon = Icons.Filled.Eco,
                    accent = SillonPalette.SynergyBlue,
                    onClick = onNavigateToGarden,
                )
            }
            if (plan.sowIndoors.isNotEmpty()) {
                TaskCard(
                    title = "Semis sous abri, au chaud",
                    names = plan.sowIndoors.map { it.vegetable.name },
                    icon = Icons.Filled.Spa,
                    accent = SillonPalette.Terracotta,
                    onClick = onNavigateToCatalog,
                )
            }
            if (plan.sowOutdoors.isNotEmpty()) {
                TaskCard(
                    title = "Semis en pleine terre",
                    names = plan.sowOutdoors.map { it.vegetable.name },
                    icon = Icons.Filled.Grass,
                    accent = SillonPalette.Sage,
                    onClick = onNavigateToCatalog,
                )
            }
            if (plan.transplant.isNotEmpty()) {
                TaskCard(
                    title = "Repiquage et plantations",
                    names = plan.transplant.map { it.vegetable.name },
                    icon = Icons.Filled.LocalFlorist,
                    accent = SillonPalette.SageDark,
                    onClick = onNavigateToGarden,
                )
            }
            if (plan.harvest.isNotEmpty()) {
                TaskCard(
                    title = "Saison de récolte",
                    names = plan.harvest.map { it.vegetable.name },
                    icon = Icons.Filled.Eco,
                    accent = SillonPalette.SynergyBlue,
                    onClick = onNavigateToCatalog,
                )
            }
        }
    }
}

@Composable
private fun FrostCard(
    frost: FrostStatus,
    placeLabel: String?,
    onOpenSettings: () -> Unit,
) {
    SillonCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Filled.AcUnit, SillonPalette.Sage)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dernières gelées repères",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val date = when (frost) {
                        is FrostStatus.Upcoming -> frost.date
                        is FrostStatus.Passed -> frost.date
                    }
                    Text(
                        text = listOfNotNull(dayMonth.format(date), placeLabel).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when (frost) {
                    is FrostStatus.Upcoming -> SillonBadge(
                        text = if (frost.days == 0L) "Aujourd'hui" else "Dans ${frost.days} j",
                        containerColor = SillonPalette.AlertContainer,
                        textColor = SillonPalette.AlertAmber,
                    )
                    is FrostStatus.Passed -> SillonBadge(
                        text = "Passées",
                        containerColor = SillonPalette.SageContainer,
                        textColor = SillonPalette.SageDark,
                    )
                }
            }
            if (placeLabel == null) {
                TextButton(onClick = onOpenSettings) { Text("Ajuster à mon département") }
            }
        }
    }
}

@Composable
private fun RestCard(nextWorkFrom: LocalDate?, onNavigateToCatalog: () -> Unit) {
    SillonCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
    ) {
        Column {
            Text(
                text = "Le potager se repose",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (nextWorkFrom != null) {
                    "Aucun semis ni plantation sur cette période. Prochains travaux vers le ${dayMonth.format(nextWorkFrom)}."
                } else {
                    "Aucun semis ni plantation sur cette période : paillage, compost et préparation des planches."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )
            TextButton(onClick = onNavigateToCatalog) { Text("Préparer la saison dans le catalogue") }
        }
    }
}

@Composable
private fun TaskCard(
    title: String,
    names: List<String>,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
) {
    val shown = names.take(5)
    val more = names.size - shown.size
    SillonCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
        onClick = onClick,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon, accent)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = shown.joinToString(", ") + if (more > 0) " et $more autre${if (more > 1) "s" else ""}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            SillonBadge(text = "${names.size}", containerColor = accent.copy(alpha = 0.15f), textColor = accent)
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
    }
}
