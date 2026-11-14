package fr.pilmil.sillon.ui.doctor

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import fr.pilmil.sillon.vision.PhotoClassifier
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PestControl
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import fr.pilmil.sillon.model.Disease
import fr.pilmil.sillon.model.PlantOrgan
import fr.pilmil.sillon.ui.components.GlossaryText
import fr.pilmil.sillon.ui.components.SillonBadge
import fr.pilmil.sillon.ui.components.SillonCard
import fr.pilmil.sillon.ui.theme.SillonPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorScreen(
    diseases: List<Disease>,
    onSelectDisease: (Disease) -> Unit = {},
) {
    var selectedOrgan by remember { mutableStateOf<PlantOrgan?>(null) }
    var selectedDiseaseId by remember { mutableStateOf<String?>(null) }

    val filteredDiseases = remember(diseases, selectedOrgan) {
        if (selectedOrgan == null) diseases
        else diseases.filter { it.affectedOrgans.contains(selectedOrgan) }
    }

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val photoAvailable = remember { PhotoClassifier.isAvailable(context) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // En-tête
        item {
            Column {
                Text(
                    text = "Docteur du Potager",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Clé de diagnostic hors-ligne : choisissez l'organe touché, puis comparez les symptômes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Diagnostic photo sur le téléphone, seulement si un modèle est embarqué.
        if (photoAvailable) item {
            PhotoDiagnosisCard(
                onOpenDisease = { id ->
                    // Ouvre la fiche dans la liste complète, puis y fait défiler l'écran.
                    selectedOrgan = null
                    selectedDiseaseId = id
                    val index = diseases.indexOfFirst { it.id == id }
                    if (index >= 0) scope.launch { listState.animateScrollToItem(DISEASE_LIST_OFFSET + index) }
                },
            )
        }

        // Sélecteur d'organe touché
        item {
            Column {
                Text(
                    text = "1. Quel organe est affecté ?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                val organs = listOf(
                    null to "Tous les organes",
                    PlantOrgan.LEAF to "Feuilles",
                    PlantOrgan.STEM to "Tiges / Collet",
                    PlantOrgan.FRUIT to "Fruits / Légumes",
                    PlantOrgan.ROOT to "Racines",
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(organs) { (organ, label) ->
                        val isSelected = selectedOrgan == organ
                        Surface(
                            modifier = Modifier.clickable { selectedOrgan = organ },
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
        }

        // Titre liste affections
        item {
            Text(
                text = "2. Diagnostics probables (${filteredDiseases.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }

        // Liste des maladies identifiées
        items(filteredDiseases, key = { it.id }) { disease ->
            val isExpanded = selectedDiseaseId == disease.id
            DiseaseCard(
                disease = disease,
                isExpanded = isExpanded,
                onToggleExpand = {
                    selectedDiseaseId = if (isExpanded) null else disease.id
                },
            )
        }
    }
}

@Composable
private fun DiseaseCard(
    disease: Disease,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    SillonCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = if (isExpanded) SillonPalette.Sage else MaterialTheme.colorScheme.outline,
        onClick = onToggleExpand,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            when (disease.pathogenType.name) {
                                "FUNGUS" -> SillonPalette.AlertContainer
                                "PEST" -> SillonPalette.TerracottaContainer
                                else -> SillonPalette.SageContainer
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (disease.pathogenType.name == "PEST") Icons.Filled.PestControl else Icons.Filled.Healing,
                        contentDescription = null,
                        tint = when (disease.pathogenType.name) {
                            "FUNGUS" -> SillonPalette.AlertAmber
                            "PEST" -> SillonPalette.Terracotta
                            else -> SillonPalette.SageDark
                        },
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = disease.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    if (disease.latinName != null) {
                        Text(
                            text = disease.latinName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
                SillonBadge(
                    text = disease.pathogenType.frenchLabel.take(15) + "...",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Organes touchés
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                disease.affectedOrgans.forEach { organ ->
                    SillonBadge(
                        text = organ.frenchLabel,
                        containerColor = SillonPalette.LinSurfaceVariant,
                        textColor = SillonPalette.PeatMuted,
                    )
                }
            }

            // Détails et remèdes bio si étendu
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Symptômes
                    Text(
                        text = "Symptômes caractéristiques :",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    disease.symptoms.forEach { symptom ->
                        GlossaryText(
                            text = "• $symptom",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Remèdes biologiques
                    Text(
                        text = "Remèdes certifiés bio & permaculture :",
                        style = MaterialTheme.typography.labelSmall,
                        color = SillonPalette.SageDark,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    disease.organicRemedies.forEach { remedy ->
                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = SillonPalette.Sage,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            GlossaryText(
                                text = remedy,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp,
                            )
                        }
                    }

                    // Prévention
                    if (disease.preventiveMeasures.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Mesures préventives d'anticipation :",
                            style = MaterialTheme.typography.labelSmall,
                            color = SillonPalette.SynergyBlue,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        disease.preventiveMeasures.forEach { prev ->
                            GlossaryText(
                                text = "→ $prev",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Nombre d'éléments avant la liste des maladies quand la photo est proposée : en-tête, photo, organe, titre. */
private const val DISEASE_LIST_OFFSET = 4
