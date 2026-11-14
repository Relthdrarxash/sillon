package fr.pilmil.sillon.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.pilmil.sillon.model.Climate
import fr.pilmil.sillon.ui.components.SillonBadge
import fr.pilmil.sillon.ui.components.SillonButton
import fr.pilmil.sillon.ui.components.SillonCard
import fr.pilmil.sillon.ui.components.SillonOutlinedButton
import fr.pilmil.sillon.ui.components.SillonStepper
import fr.pilmil.sillon.ui.theme.SillonPalette

@Composable
fun SettingsScreen(
    frostMonth: Int = 5,
    frostDay: Int = 15,
    climate: Climate = Climate.TEMPERATE,
    onFrostDateChange: (Int, Int) -> Unit = { _, _ -> },
    onExportJson: () -> Unit = {},
    onImportJson: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    departmentLabel: String? = null,
    onPickDepartment: () -> Unit = {},
    remindersOn: Boolean = false,
    onRemindersChange: (Boolean) -> Unit = {},
    smartViewfinderOn: Boolean = true,
    onSmartViewfinderChange: (Boolean) -> Unit = {},
) {
    val monthNames = listOf(
        "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Titre
        item {
            Column {
                Text(
                    text = "Réglages & Données",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Paramétrage microclimatique et souveraineté de vos données.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Section Climat & Gelées
        item {
            SillonCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (climate == Climate.TROPICAL) Icons.Filled.WbSunny else Icons.Filled.AcUnit,
                            contentDescription = null,
                            tint = SillonPalette.Sage,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (climate == Climate.TROPICAL) "Début de la saison de plantation" else "Date repère des dernières gelées",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (climate == Climate.TROPICAL) {
                            "En outre-mer, il n'y a pas de gelée : le calendrier part du début de la saison de plantation de votre territoire. Cette date est une estimation, qui varie de 1 à 2 mois selon les sources et les microclimats : ajustez-la au jour près. Tous les travaux et rappels de semis sont calculés par rapport à ce jour."
                        } else {
                            "En France métropolitaine, la date médiane se situe entre mi-mars (zone méditerranéenne) et mi-mai (Saints de Glace en plaine / montagne). Tous les travaux et rappels de semis sont calculés par rapport à ce jour J."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SillonStepper(
                            value = frostDay,
                            onValueChange = { onFrostDateChange(frostMonth, it) },
                            min = 1,
                            max = 31,
                            label = "Jour du mois",
                            modifier = Modifier.weight(1f),
                        )
                        SillonStepper(
                            value = frostMonth,
                            onValueChange = { onFrostDateChange(it, frostDay) },
                            min = 1,
                            max = 12,
                            label = "Mois (${monthNames.getOrElse(frostMonth - 1) { "" }})",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SillonOutlinedButton(
                        text = departmentLabel?.let { "Lieu : $it" } ?: "Choisir mon département ou territoire",
                        icon = Icons.Filled.Place,
                        onClick = onPickDepartment,
                    )
                }
            }
        }

        // Section Rappels de semis
        item {
            SillonCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = SillonPalette.Sage,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rappels de semis",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Le samedi matin, quand des semis ou plantations s'ouvrent dans la quinzaine.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = remindersOn, onCheckedChange = onRemindersChange)
                }
            }
        }

        // Section Diagnostic Photo & Viseur Intelligent
        item {
            SillonCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline,
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CenterFocusStrong,
                                contentDescription = null,
                                tint = SillonPalette.SageDark,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Assistant de cadrage photo",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Switch(checked = smartViewfinderOn, onCheckedChange = onSmartViewfinderChange)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Viseur avec indicateur dynamique de netteté et d'exposition (vert/jaune/rouge) et déclenchement automatique dès que la feuille est bien cadrée.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Si désactivé, Sillon utilise l'appareil photo standard de votre téléphone sans demander de permission.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SillonPalette.PeatMuted,
                    )
                }
            }
        }

        // Section Sauvegarde & Transfert
        item {
            SillonCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = SillonPalette.Terracotta,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sauvegarde & Changement de téléphone",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sillon ne requiert aucun compte et n'a aucun serveur distant. Pour sauvegarder ou migrer vos parcelles vers un nouveau smartphone, exportez votre fichier de sauvegarde en un tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    SillonButton(
                        text = "Sauvegarder mon potager (JSON)",
                        icon = Icons.Filled.FileDownload,
                        onClick = onExportJson,
                        containerColor = SillonPalette.Sage,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SillonOutlinedButton(
                        text = "Restaurer une sauvegarde (JSON)",
                        icon = Icons.Filled.FileUpload,
                        onClick = onImportJson,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SillonOutlinedButton(
                        text = "Exporter pour tableur (CSV / LibreOffice)",
                        icon = Icons.Filled.TableChart,
                        onClick = onExportCsv,
                    )
                }
            }
        }

        // Section Philosophie Pilmil Studio
        item {
            SillonCard(
                backgroundColor = SillonPalette.LinSurfaceVariant.copy(alpha = 0.5f),
                borderColor = SillonPalette.FurrowLine,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = SillonPalette.SageDark,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Garantie Pilmil Studio",
                            style = MaterialTheme.typography.titleSmall,
                            color = SillonPalette.SageDark,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Aucune permission Internet dans le manifeste Android.\n" +
                               "• Zéro traceur, zéro SDK publicitaire, zéro analyse de comportement.\n" +
                               "• Calendrier, associations et diagnostic 100 % calculés sur votre téléphone.\n" +
                               "• Logiciel libre sous licence Mozilla Public License 2.0 (MPL-2.0).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
    }
}
