package fr.pilmil.sillon.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.pilmil.sillon.model.BotanicalFamily
import fr.pilmil.sillon.model.Vegetable
import fr.pilmil.sillon.ui.components.SillonBadge
import fr.pilmil.sillon.ui.components.SillonCard
import fr.pilmil.sillon.ui.theme.SillonPalette

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    vegetables: List<Vegetable>,
    onSelectVegetable: (Vegetable) -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFamily by remember { mutableStateOf<BotanicalFamily?>(null) }
    var expandedVegetableId by remember { mutableStateOf<String?>(null) }

    val filteredVegetables = remember(vegetables, searchQuery, selectedFamily) {
        vegetables.filter { plant ->
            val matchesQuery = searchQuery.isBlank() ||
                    plant.name.contains(searchQuery, ignoreCase = true) ||
                    plant.latinName.contains(searchQuery, ignoreCase = true)
            val matchesFamily = selectedFamily == null || plant.family == selectedFamily
            matchesQuery && matchesFamily
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // En-tête
        item {
            Column {
                Text(
                    text = "Encyclopédie vivante",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${vegetables.size} fiches botaniques complètes, affinités & distances.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Barre de recherche
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                placeholder = {
                    Text(
                        text = "Chercher une variété (ex: Tomate, Poireau)...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = SillonPalette.Sage,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Effacer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SillonPalette.Sage,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                singleLine = true,
            )
        }

        // Filtres par famille botanique
        item {
            val families = listOf(
                null to "Toutes",
                BotanicalFamily.SOLANACEAE to "Solanacées",
                BotanicalFamily.FABACEAE to "Fabacées (Légumineuses)",
                BotanicalFamily.BRASSICACEAE to "Brassicacées (Choux)",
                BotanicalFamily.APIACEAE to "Apiacées (Carotte)",
                BotanicalFamily.CUCURBITACEAE to "Cucurbitacées",
                BotanicalFamily.ALLIACEAE to "Alliacées",
                BotanicalFamily.ASTERACEAE to "Astéracées (Salades)",
                BotanicalFamily.LAMIACEAE to "Lamiacées (Aromatiques)",
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(families) { (family, label) ->
                    val isSelected = selectedFamily == family
                    Surface(
                        modifier = Modifier.clickable { selectedFamily = family },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SillonPalette.Sage else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) SillonPalette.Sage else MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        // Liste des cartes de légumes
        items(filteredVegetables, key = { it.id }) { vegetable ->
            val isExpanded = expandedVegetableId == vegetable.id
            VegetableCard(
                vegetable = vegetable,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedVegetableId = if (isExpanded) null else vegetable.id
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VegetableCard(
    vegetable: Vegetable,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    SillonCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
        onClick = onToggleExpand,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SillonPalette.SageContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Eco,
                        contentDescription = null,
                        tint = SillonPalette.SageDark,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vegetable.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = vegetable.latinName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic,
                    )
                }
                SillonBadge(
                    text = "${vegetable.squareFootDensity}/carré",
                    containerColor = SillonPalette.SageContainer,
                    textColor = SillonPalette.SageDark,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ligne résumée : Famille + Température + Besoins
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SillonBadge(
                    text = vegetable.family.frenchLabel,
                    containerColor = SillonPalette.LinSurfaceVariant,
                    textColor = SillonPalette.PeatInk,
                )
                SillonBadge(
                    text = "Germ. ≥ ${vegetable.tempGerminationMin}°C",
                    containerColor = SillonPalette.LinSurfaceVariant,
                    textColor = SillonPalette.PeatMuted,
                    icon = Icons.Filled.Thermostat,
                )
                SillonBadge(
                    text = vegetable.waterNeed.frenchLabel,
                    containerColor = SillonPalette.LinSurfaceVariant,
                    textColor = SillonPalette.SynergyBlue,
                    icon = Icons.Filled.WaterDrop,
                )
            }

            // Détails dépliés
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // Conseil agronomique
                    Text(
                        text = "Conseil de culture :",
                        style = MaterialTheme.typography.labelMedium,
                        color = SillonPalette.SageDark,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = vegetable.advice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Compagnons favorables
                    if (vegetable.goodCompanions.isNotEmpty()) {
                        Text(
                            text = "Bons voisins :",
                            style = MaterialTheme.typography.labelSmall,
                            color = SillonPalette.SageDark,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            vegetable.goodCompanions.forEach { companion ->
                                SillonBadge(
                                    text = "${companion.targetPlantId.replace('_', ' ').replaceFirstChar { it.uppercase() }} : ${companion.rationale}",
                                    containerColor = SillonPalette.SageContainer,
                                    textColor = SillonPalette.SageDark,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Compagnons antagonistes
                    if (vegetable.badCompanions.isNotEmpty()) {
                        Text(
                            text = "À éloigner :",
                            style = MaterialTheme.typography.labelSmall,
                            color = SillonPalette.AlertAmber,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            vegetable.badCompanions.forEach { bad ->
                                SillonBadge(
                                    text = "${bad.targetPlantId.replace('_', ' ').replaceFirstChar { it.uppercase() }} : ${bad.rationale}",
                                    containerColor = SillonPalette.AlertContainer,
                                    textColor = SillonPalette.AlertAmber,
                                    icon = Icons.Filled.Warning,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
