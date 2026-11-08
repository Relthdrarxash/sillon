package fr.pilmil.sillon.ui.doctor

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import fr.pilmil.sillon.data.DiseaseCatalog
import fr.pilmil.sillon.engine.PhotoVerdict
import fr.pilmil.sillon.ui.components.SillonButton
import fr.pilmil.sillon.ui.components.SillonCard
import fr.pilmil.sillon.ui.components.SillonOutlinedButton
import fr.pilmil.sillon.vision.PhotoClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * « Analyser une photo » : visible seulement si un modèle est embarqué.
 * Le résultat est une piste, jamais un verdict : il renvoie vers la fiche de la maladie,
 * ou vers la clé de diagnostic quand le modèle n'est pas assez sûr.
 */
@Composable
fun PhotoDiagnosisCard(onOpenDisease: (String) -> Unit) {
    val context = LocalContext.current
    val classifier = remember { PhotoClassifier.load(context) } ?: return
    DisposableEffect(classifier) { onDispose { classifier.close() } }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var verdict by remember { mutableStateOf<PhotoVerdict?>(null) }
    var failed by remember { mutableStateOf(false) }

    fun analyse(uri: Uri) {
        busy = true
        failed = false
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                PhotoClassifier.decode(context, uri)?.let { runCatching { classifier.classify(it) }.getOrNull() }
            }
            verdict = result
            failed = result == null
            busy = false
        }
    }

    val photoFile = remember { File(context.cacheDir, "photos/feuille.jpg").apply { parentFile?.mkdirs() } }
    val photoUri = remember { FileProvider.getUriForFile(context, "${context.packageName}.photos", photoFile) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) analyse(photoUri) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::analyse) }

    SillonCard(backgroundColor = MaterialTheme.colorScheme.surface, borderColor = MaterialTheme.colorScheme.outline) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Analyser une photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Une feuille bien éclairée, en gros plan. L'analyse se fait sur le téléphone : la photo n'est envoyée nulle part.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SillonButton(
                text = "Prendre une photo",
                icon = Icons.Filled.CameraAlt,
                enabled = !busy,
                onClick = {
                    try {
                        camera.launch(photoUri)
                    } catch (_: ActivityNotFoundException) {
                        failed = true
                    }
                },
            )
            SillonOutlinedButton(
                text = "Choisir dans la galerie",
                icon = Icons.Filled.PhotoLibrary,
                onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            )

            Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                when {
                    busy -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Analyse en cours…", style = MaterialTheme.typography.bodyMedium)
                    }
                    failed -> Text("La photo n'a pas pu être lue. Réessayez avec une autre image.", style = MaterialTheme.typography.bodyMedium)
                    else -> verdict?.let { VerdictText(it, onOpenDisease) }
                }
            }
            Text(
                "Reconnaît pour l'instant le mildiou, l'oïdium et l'alternariose sur tomate, pomme de terre, poivron, courge et fraisier. C'est une piste à confirmer avec la clé ci-dessous, pas un diagnostic certain.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VerdictText(verdict: PhotoVerdict, onOpenDisease: (String) -> Unit) {
    fun percent(p: Float) = "${(p * 100).toInt()} %"
    when (verdict) {
        is PhotoVerdict.Healthy -> Text(
            "Feuille d'apparence saine (confiance ${percent(verdict.confidence)}).",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        is PhotoVerdict.Disease -> {
            val name = DiseaseCatalog.getById(verdict.catalogId)?.name ?: verdict.catalogId
            Text(
                "Ressemble à : $name (confiance ${percent(verdict.confidence)}).",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { onOpenDisease(verdict.catalogId) }) { Text("Voir la fiche $name") }
        }
        is PhotoVerdict.Unsure -> {
            Text(
                "Je ne reconnais pas ce problème avec assez de certitude. Utilisez la clé de diagnostic ci-dessous.",
                style = MaterialTheme.typography.bodyLarge,
            )
            verdict.leads.forEach { (id, p) ->
                val name = DiseaseCatalog.getById(id)?.name ?: id
                TextButton(onClick = { onOpenDisease(id) }) { Text("Piste possible : $name (${percent(p)})") }
            }
        }
    }
}
