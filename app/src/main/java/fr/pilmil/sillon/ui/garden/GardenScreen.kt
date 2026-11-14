package fr.pilmil.sillon.ui.garden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import fr.pilmil.sillon.ui.components.SillonOutlinedButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.pilmil.sillon.model.Garden
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.ParcelSlot
import fr.pilmil.sillon.model.ParcelType
import fr.pilmil.sillon.model.Vegetable
import fr.pilmil.sillon.ui.catalog.INDICATIVE_NOTICE
import fr.pilmil.sillon.ui.components.SillonBadge
import fr.pilmil.sillon.ui.components.SillonButton
import fr.pilmil.sillon.ui.components.SillonCard
import fr.pilmil.sillon.ui.theme.SillonPalette
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(
    garden: Garden,
    vegetables: List<Vegetable>,
    onUpdateSlot: (parcelId: String, row: Int, col: Int, vegetableId: String?) -> Unit,
    onAddParcel: (name: String, type: ParcelType, rows: Int, cols: Int) -> Unit,
    onDeleteParcel: (parcelId: String) -> Unit,
    today: LocalDate = LocalDate.now(),
    // Case mise en évidence dès l'ouverture (sans feuille de choix) ; sert surtout aux captures d'écran.
    initialSelectedCell: Pair<Int, Int>? = null,
    initialPerspective: Boolean = true,
) {
    var selectedParcelIndex by remember { mutableStateOf(0) }
    var selectedSlotCoord by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // La case reste mise en évidence après la fermeture de la feuille : on voit ce qu'on vient de modifier.
    var highlightedCell by remember { mutableStateOf(initialSelectedCell) }
    var perspectiveView by remember { mutableStateOf(initialPerspective) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var showAddParcelDialog by remember { mutableStateOf(false) }

    val activeParcel = garden.parcels.getOrNull(selectedParcelIndex) ?: garden.parcels.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // En-tête compact et choix de la vue
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Mon potager",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ViewChip("Perspective", perspectiveView) { perspectiveView = true }
                    ViewChip("À plat", !perspectiveView) { perspectiveView = false }
                }
            }
        }

        // Onglets des parcelles existantes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(garden.parcels.indices.toList()) { index ->
                        val parcel = garden.parcels[index]
                        val isSelected = index == selectedParcelIndex
                        Surface(
                            modifier = Modifier.clickable {
                                selectedParcelIndex = index
                                highlightedCell = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SillonPalette.Sage else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SillonPalette.Sage else MaterialTheme.colorScheme.outline,
                            ),
                        ) {
                            Text(
                                text = parcel.name,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showAddParcelDialog = true },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(SillonPalette.SageContainer, CircleShape)
                        .size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Ajouter une parcelle",
                        tint = SillonPalette.SageDark,
                    )
                }
            }
        }

        // Scène en perspective ou grille à plat de la parcelle active
        if (activeParcel != null) {
            item {
                if (perspectiveView) {
                    GardenScene(
                        parcels = garden.parcels,
                        selectedIndex = garden.parcels.indexOf(activeParcel),
                        vegetables = vegetables,
                        today = today,
                        selectedCell = highlightedCell,
                        onSelectParcel = {
                            selectedParcelIndex = it
                            highlightedCell = null
                        },
                        onCellClick = { r, c ->
                            highlightedCell = Pair(r, c)
                            selectedSlotCoord = Pair(r, c)
                        },
                        isDark = isDark,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                    )
                } else {
                    ParcelGridView(
                        parcel = activeParcel,
                        vegetables = vegetables,
                        onSlotClick = { r, c ->
                            highlightedCell = Pair(r, c)
                            selectedSlotCoord = Pair(r, c)
                        },
                    )
                }
            }

            // Légende des alertes & synergies
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SillonPalette.Sage),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Affinité positive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SillonPalette.AlertAmber),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Alerte voisinage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { NeighborhoodCard(activeParcel, vegetables) }
        } else {
            item {
                Text(
                    text = "Aucune parcelle configurée. Cliquez sur '+' pour créer votre premier carré.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // Modal Bottom Sheet pour choisir ou modifier la plante de la case
    if (selectedSlotCoord != null && activeParcel != null) {
        val (row, col) = selectedSlotCoord!!
        val currentSlot = activeParcel.getSlot(row, col)
        val currentVeg = vegetables.find { it.id == currentSlot?.vegetableId }

        ModalBottomSheet(
            onDismissRequest = { selectedSlotCoord = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    text = "Emplacement Ligne ${row + 1}, Carré ${col + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (currentVeg != null) "Actuellement planté : ${currentVeg.name}" else "Case libre (30 × 30 cm)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (currentVeg != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SillonOutlinedButton(
                        text = "Vider cet emplacement",
                        icon = Icons.Filled.Clear,
                        onClick = {
                            onUpdateSlot(activeParcel.id, row, col, null)
                            selectedSlotCoord = null
                        },
                        borderColor = SillonPalette.AlertAmber,
                        textColor = SillonPalette.AlertAmber,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Planter une variété :",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(vegetables) { veg ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateSlot(activeParcel.id, row, col, veg.id)
                                    selectedSlotCoord = null
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = SillonPalette.LinGround,
                            border = BorderStroke(1.dp, SillonPalette.FurrowLine),
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = veg.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "${veg.family.frenchLabel} • ${veg.squareFootDensity}/carré",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (veg.needsMoreSpace) {
                                        Text(
                                            text = "Demande plus d'espace qu'un carré de 30 cm.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SillonPalette.AlertAmber,
                                        )
                                    }
                                    if (veg.indicative) {
                                        Text(
                                            text = INDICATIVE_NOTICE,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontStyle = FontStyle.Italic,
                                        )
                                    }
                                }
                                if (currentSlot?.vegetableId == veg.id) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = SillonPalette.Sage,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogue d'ajout de parcelle
    if (showAddParcelDialog) {
        var parcelName by remember { mutableStateOf("Nouveau Carré") }
        var selectedSize by remember { mutableStateOf(3) }

        AlertDialog(
            onDismissRequest = { showAddParcelDialog = false },
            title = {
                Text(
                    text = "Nouvelle parcelle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Format standard : Carré potager surélevé divisé en carrés de 30×30 cm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSize = 3 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedSize == 3) SillonPalette.SageContainer else SillonPalette.LinGround,
                            border = BorderStroke(
                                1.5.dp,
                                if (selectedSize == 3) SillonPalette.Sage else SillonPalette.FurrowLine,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(text = "3 × 3", fontWeight = FontWeight.Bold)
                                Text(text = "9 carrés (90 cm)", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSize = 4 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedSize == 4) SillonPalette.SageContainer else SillonPalette.LinGround,
                            border = BorderStroke(
                                1.5.dp,
                                if (selectedSize == 4) SillonPalette.Sage else SillonPalette.FurrowLine,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(text = "4 × 4", fontWeight = FontWeight.Bold)
                                Text(text = "16 carrés (1,20 m)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddParcel("Carré ${garden.parcels.size + 1}", ParcelType.RAISED_BED_SQUARES, selectedSize, selectedSize)
                    showAddParcelDialog = false
                }) {
                    Text("Créer la parcelle", fontWeight = FontWeight.Bold, color = SillonPalette.Sage)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddParcelDialog = false }) {
                    Text("Annuler")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun ParcelGridView(
    parcel: Parcel,
    vegetables: List<Vegetable>,
    onSlotClick: (row: Int, col: Int) -> Unit,
) {
    val neighborhood = remember(parcel, vegetables) { neighborhoodOf(parcel, vegetables) }
    SillonCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (r in 0 until parcel.rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (c in 0 until parcel.cols) {
                        val slot = parcel.getSlot(r, c)
                        val veg = vegetables.find { it.id == slot?.vegetableId }
                        val neighbors = neighborhood[r to c]
                        val hasGoodCompanion = neighbors?.hasGood == true
                        val hasBadCompanion = neighbors?.hasBad == true

                        SlotCell(
                            vegetable = veg,
                            hasGoodCompanion = hasGoodCompanion,
                            hasBadCompanion = hasBadCompanion,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { onSlotClick(r, c) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotCell(
    vegetable: Vegetable?,
    hasGoodCompanion: Boolean,
    hasBadCompanion: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (vegetable != null) SillonPalette.SageContainer.copy(alpha = 0.5f) else SillonPalette.LinGround,
        border = BorderStroke(
            1.5.dp,
            when {
                hasBadCompanion -> SillonPalette.AlertAmber
                hasGoodCompanion -> SillonPalette.Sage
                vegetable != null -> SillonPalette.Sage.copy(alpha = 0.4f)
                else -> SillonPalette.FurrowLine
            },
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Badge d'affinité en haut à droite
            if (hasGoodCompanion) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SillonPalette.Sage),
                )
            } else if (hasBadCompanion) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SillonPalette.AlertAmber),
                )
            }

            if (vegetable != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = vegetable.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(
                        text = "×${vegetable.squareFootDensity}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = SillonPalette.SageDark,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Ajouter",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ViewChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) SillonPalette.SageContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) SillonPalette.Sage else MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) SillonPalette.SageDark else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

/** Les affinités et alertes réellement détectées dans la parcelle, en phrases courtes. */
@Composable
private fun NeighborhoodCard(parcel: Parcel, vegetables: List<Vegetable>) {
    val links = remember(parcel, vegetables) { neighborLinks(parcel, vegetables) }
    SillonCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Voisinage",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (links.isEmpty()) {
                Text(
                    text = "Aucune affinité ni alerte entre les plantes de ce carré pour l'instant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            for (link in links.take(6)) {
                val second = link.second.name.replaceFirstChar { it.lowercase() }
                val sentence = when {
                    link.positive && link.protects -> "${link.first.name} et $second se protègent mutuellement"
                    link.positive -> "${link.first.name} et $second s'entraident"
                    else -> "${link.first.name} et $second se gênent : à éloigner"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (link.positive) SillonPalette.Sage else SillonPalette.AlertAmber),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = sentence,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (links.size > 6) {
                Text(
                    text = "et ${links.size - 6} autres",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
