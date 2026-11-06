package fr.pilmil.sillon.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.pilmil.sillon.ui.components.SillonBadge
import fr.pilmil.sillon.ui.components.SillonCard
import fr.pilmil.sillon.ui.theme.SillonPalette

data class DashboardTaskItem(
    val vegetableName: String,
    val actionType: String,
    val details: String,
    val icon: ImageVector,
    val badgeColor: Color,
    val textColor: Color,
)

@Composable
fun DashboardScreen(
    frostDateDescription: String = "15 mai (Saintes Glaces)",
    daysUntilFrost: Int = -12,
    onNavigateToGarden: () -> Unit = {},
    onNavigateToCatalog: () -> Unit = {},
    onNavigateToDoctor: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // En-tête artisan
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Au potager aujourd'hui",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Calendrier phénologique calculé sur vos dernières gelées locales.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Carte Climat & Gelée locale
        item {
            SillonCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SillonPalette.SageContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AcUnit,
                            contentDescription = null,
                            tint = SillonPalette.SageDark,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dernières gelées repères",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = frostDateDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SillonBadge(
                        text = if (daysUntilFrost >= 0) "Dans ${daysUntilFrost}j" else "Passées (${-daysUntilFrost}j)",
                        containerColor = if (daysUntilFrost >= 0) SillonPalette.AlertContainer else SillonPalette.SageContainer,
                        textColor = if (daysUntilFrost >= 0) SillonPalette.AlertAmber else SillonPalette.SageDark,
                    )
                }
            }
        }

        // Section Semis & Travaux de la quinzaine
        item {
            Text(
                text = "Travaux recommandés (quinzaine)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // 4 Catégories agronomiques
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TaskCategoryCard(
                    title = "Semis sous abri & chaleur",
                    subtitle = "Tomates, piments, aubergines, poivrons",
                    badgeText = "Châssis / Godet",
                    icon = Icons.Filled.Spa,
                    containerColor = Color(0xFFF9F4EB),
                    accentColor = SillonPalette.Terracotta,
                    onClick = onNavigateToCatalog,
                )

                TaskCategoryCard(
                    title = "Semis en pleine terre",
                    subtitle = "Carottes hâtives, fèves, pois ridés, radis",
                    badgeText = "Plein champ",
                    icon = Icons.Filled.Grass,
                    containerColor = Color(0xFFEDF4ED),
                    accentColor = SillonPalette.Sage,
                    onClick = onNavigateToCatalog,
                )

                TaskCategoryCard(
                    title = "Repiquage & plantations",
                    subtitle = "Oignons de printemps, laitues de printemps",
                    badgeText = "Carrés potagers",
                    icon = Icons.Filled.LocalFlorist,
                    containerColor = Color(0xFFF3EFE7),
                    accentColor = SillonPalette.PeatInk,
                    onClick = onNavigateToGarden,
                )

                TaskCategoryCard(
                    title = "Récolte en cours",
                    subtitle = "Mâche d'hiver, poireaux perpétuels, épinards",
                    badgeText = "À cueillir",
                    icon = Icons.Filled.Eco,
                    containerColor = Color(0xFFEBF2F4),
                    accentColor = SillonPalette.SynergyBlue,
                    onClick = onNavigateToGarden,
                )
            }
        }

        // Carte Règle d'or permaculture / Conseil agronomique du moment
        item {
            SillonCard(
                backgroundColor = SillonPalette.SageContainer.copy(alpha = 0.5f),
                borderColor = SillonPalette.Sage.copy(alpha = 0.3f),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = SillonPalette.SageDark,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Règle des associations vivantes",
                            style = MaterialTheme.typography.titleSmall,
                            color = SillonPalette.SageDark,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pour protéger vos carottes de la mouche de la carotte (Psila rosae), intercalez toujours des rangs d'oignons ou de poireaux : leurs odeurs alliacées masquent la présence des Apiacées.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCategoryCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    containerColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, SillonPalette.FurrowLine),
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            SillonBadge(
                text = badgeText,
                containerColor = Color.White.copy(alpha = 0.8f),
                textColor = accentColor,
            )
        }
    }
}
