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
import fr.pilmil.sillon.engine.EdgeVisionClassifier
import fr.pilmil.sillon.engine.VisionAnalysisResult
import fr.pilmil.sillon.model.Disease
import fr.pilmil.sillon.model.PlantOrgan
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
    var simulationResult by remember { mutableStateOf<VisionAnalysisResult?>(null) }
    var simulationScenarioName by remember { mutableStateOf<String?>(null) }
    val visionClassifier = remember { EdgeVisionClassifier() }

    val filteredDiseases = remember(diseases, selectedOrgan) {
        if (selectedOrgan == null) diseases
        else diseases.filter { it.affectedOrgans.contains(selectedOrgan) }
    }

    LazyColumn(
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
                    text = "Clé diagnostique dichotomique hors-ligne & remèdes certifiés bio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

        // Section Modèle Edge ML Local & Démonstrateur Interactif
        item {
            SillonCard(
                backgroundColor = SillonPalette.SageContainer.copy(alpha = 0.35f),
                borderColor = SillonPalette.Sage.copy(alpha = 0.3f),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SillonPalette.Sage),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Biotech,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vision locale sur appareil (Edge ML)",
                                style = MaterialTheme.typography.titleSmall,
                                color = SillonPalette.SageDark,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Modèle MobileNetV3 INT8 (3.8 Mo) & analyse spectrale 100% locale, sans cloud.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Éprouver la rigueur du modèle (Simulation d'échantillon) :",
                        style = MaterialTheme.typography.labelSmall,
                        color = SillonPalette.SageDark,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            modifier = Modifier.clickable {
                                simulationScenarioName = "Feuille saine de courgette"
                                val px = IntArray(100 * 100) { EdgeVisionClassifier.rgb(35, 145, 45) }
                                simulationResult = visionClassifier.analyzePixels(px, 100, 100)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SillonPalette.Sage.copy(alpha = 0.5f)),
                        ) {
                            Text(
                                text = "🌿 Feuille saine",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        Surface(
                            modifier = Modifier.clickable {
                                simulationScenarioName = "Attaque de Mildiou (Solanacées)"
                                val px = IntArray(100 * 100) { i ->
                                    val x = i % 100
                                    if (x < 28) EdgeVisionClassifier.rgb(95, 60, 30) // Brun nécrotique
                                    else EdgeVisionClassifier.rgb(45, 135, 40)
                                }
                                simulationResult = visionClassifier.analyzePixels(px, 100, 100)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SillonPalette.Sage.copy(alpha = 0.5f)),
                        ) {
                            Text(
                                text = "🍂 Alerte Mildiou",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        Surface(
                            modifier = Modifier.clickable {
                                simulationScenarioName = "Feutrage Oïdium (Cucurbitacées)"
                                val px = IntArray(100 * 100) { i ->
                                    val x = i % 100
                                    if (x < 32) EdgeVisionClassifier.rgb(235, 235, 235) // Blanc oïdium
                                    else EdgeVisionClassifier.rgb(40, 140, 40)
                                }
                                simulationResult = visionClassifier.analyzePixels(px, 100, 100)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SillonPalette.Sage.copy(alpha = 0.5f)),
                        ) {
                            Text(
                                text = "⚪ Feutrage Oïdium",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        Surface(
                            modifier = Modifier.clickable {
                                simulationScenarioName = "Échantillon ambigu (Fond / Adventice)"
                                val px = IntArray(100 * 100) { EdgeVisionClassifier.rgb(115, 105, 95) }
                                simulationResult = visionClassifier.analyzePixels(px, 100, 100)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SillonPalette.AlertAmber),
                        ) {
                            Text(
                                text = "❓ Fond incertain",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = SillonPalette.AlertAmber,
                            )
                        }
                    }

                    simulationResult?.let { res ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = simulationScenarioName ?: "Résultat analyse",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    val topConf = res.predictions.firstOrNull()?.confidence ?: 0f
                                    SillonBadge(
                                        text = if (res.isHealthy) "Saine" else if (res.needsManualTriage) "Incertain (< 65%)" else "${(topConf * 100).toInt()}% conf.",
                                        containerColor = if (res.isHealthy) SillonPalette.SageContainer else if (res.needsManualTriage) SillonPalette.AlertContainer else SillonPalette.TerracottaContainer,
                                        textColor = if (res.isHealthy) SillonPalette.SageDark else if (res.needsManualTriage) SillonPalette.AlertAmber else SillonPalette.Terracotta,
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Spectre : Nécrose ${(res.necrosisRatio * 100).toInt()}% | Oïdium ${(res.powderyCoverageRatio * 100).toInt()}% | Chlorose ${(res.chlorosisRatio * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                if (res.needsManualTriage) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "⚠️ Garde-fou agronomique : L'IA refuse de deviner au hasard pour éviter un faux traitement. Basculement vers la clé clinique recommandée.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SillonPalette.AlertAmber,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                } else if (res.predictions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val top = res.predictions.first()
                                    Text(
                                        text = "Diagnostic principal : ${top.disease.name} (${top.dominantSymptomObserved})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SillonPalette.SageDark,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
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
                        Text(
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
                            Text(
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
                            Text(
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
