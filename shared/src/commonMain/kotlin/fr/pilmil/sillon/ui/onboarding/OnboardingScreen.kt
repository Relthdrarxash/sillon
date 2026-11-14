package fr.pilmil.sillon.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.pilmil.sillon.data.DepartmentFrost
import fr.pilmil.sillon.data.FrenchFrostCatalog
import fr.pilmil.sillon.engine.DiagnosticEngine
import fr.pilmil.sillon.model.formatDayMonthFr
import fr.pilmil.sillon.ui.PlatformBackHandler
import fr.pilmil.sillon.ui.components.SillonButton
import fr.pilmil.sillon.ui.components.SillonOutlinedButton
import fr.pilmil.sillon.ui.rememberReminderPermissionRequester

/**
 * Premier lancement en deux écrans : où est le jardin (pour la date de gelée),
 * puis les rappels de semis, facultatifs.
 */
@Composable
fun OnboardingScreen(
    onDepartment: (DepartmentFrost?) -> Unit,
    onFinish: (reminders: Boolean) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val requestReminders = rememberReminderPermissionRequester(onResult = onFinish)
    PlatformBackHandler(enabled = step > 0) { step = 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (step) {
            0 -> {
                Title("Où se trouve votre potager ?")
                Text(
                    "Sillon cale tous les semis sur la date moyenne des dernières gelées de votre département. Rien ne quitte votre téléphone.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DepartmentSearch(
                    modifier = Modifier.weight(1f),
                    onPick = {
                        onDepartment(it)
                        step = 1
                    },
                )
                SillonOutlinedButton(
                    text = "Je réglerai plus tard",
                    onClick = {
                        onDepartment(null)
                        step = 1
                    },
                )
            }
            else -> {
                Title("Un rappel quand c'est le moment ?")
                Text(
                    "Le samedi matin, Sillon peut vous dire quels semis et plantations s'ouvrent dans la quinzaine. Pas de message quand il n'y a rien à faire.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Vous pourrez changer d'avis dans les Réglages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                SillonButton(
                    text = "Oui, me prévenir",
                    icon = Icons.Filled.NotificationsActive,
                    onClick = requestReminders,
                )
                SillonOutlinedButton(text = "Non merci", onClick = { onFinish(false) })
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() },
    )
}

private fun String.fold(): String = buildString(length) {
    for (ch in this@fold.lowercase()) {
        append(
            when (ch) {
                'à', 'â', 'ä' -> 'a'
                'ç' -> 'c'
                'é', 'è', 'ê', 'ë' -> 'e'
                'î', 'ï' -> 'i'
                'ô', 'ö' -> 'o'
                'ù', 'û', 'ü' -> 'u'
                'ÿ' -> 'y'
                else -> ch
            }
        )
    }
}

/** Recherche par numéro ou par nom, sans accent ni majuscule à respecter. */
@Composable
fun DepartmentSearch(
    onPick: (DepartmentFrost) -> Unit,
    modifier: Modifier = Modifier,
    selectedCode: String? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) {
        val q = query.trim().fold()
        if (q.isEmpty()) FrenchFrostCatalog.DEPARTMENTS
        else FrenchFrostCatalog.DEPARTMENTS.filter { it.code.lowercase().startsWith(q) || it.name.fold().contains(q) }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Département (numéro ou nom)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(Modifier.fillMaxWidth()) {
            items(results, key = { it.code }) { dep ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(role = Role.Button) { onPick(dep) }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                ) {
                    Text(dep.code, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(44.dp))
                    Column(Modifier.weight(1f)) {
                        Text(dep.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Dernières gelées vers le ${formatDayMonthFr(dep.medianLastFrost.toLocalDate(2026))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (dep.code == selectedCode) {
                        androidx.compose.material3.Icon(Icons.Filled.Check, contentDescription = "Choisi", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/** La même recherche, dans une boîte de dialogue, pour les Réglages. */
@Composable
fun DepartmentDialog(selectedCode: String?, onPick: (DepartmentFrost) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mon département") },
        text = { DepartmentSearch(onPick = onPick, selectedCode = selectedCode, modifier = Modifier.heightIn(max = 420.dp)) },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
