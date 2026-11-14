package fr.pilmil.sillon

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import fr.pilmil.sillon.backup.BackupPolicy
import fr.pilmil.sillon.backup.CloudBackupConsent
import fr.pilmil.sillon.data.GardenRepository
import fr.pilmil.sillon.data.GardenValidationException
import fr.pilmil.sillon.model.Garden
import fr.pilmil.sillon.reminders.SowingReminders
import fr.pilmil.sillon.ui.SillonApp
import fr.pilmil.sillon.ui.doctor.PhotoDiagnosisCard
import fr.pilmil.sillon.ui.theme.SillonTheme
import fr.pilmil.sillon.vision.PhotoClassifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var repository: GardenRepository
    private val diskExecutor = Executors.newSingleThreadExecutor()
    private val disk: CoroutineDispatcher = diskExecutor.asCoroutineDispatcher()

    override fun onDestroy() {
        super.onDestroy()
        diskExecutor.shutdown()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        repository = GardenRepository(this)

        setContent {
            SillonTheme(darkTheme = isSystemInDarkTheme()) {
                val scope = rememberCoroutineScope()
                val cloudConsent = remember { CloudBackupConsent(noBackupFilesDir) }
                var cloudBackupOn by remember { mutableStateOf(cloudConsent.enabled) }
                val deviceSecure = remember {
                    (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceSecure == true
                }
                val photoAvailable = remember { PhotoClassifier.isAvailable(this@MainActivity) }
                var importedGarden by remember { mutableStateOf<Garden?>(null) }

                val exportJsonLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            val written = withContext(disk) {
                                try {
                                    contentResolver.openOutputStream(uri)?.use { out -> repository.exportToJson(out) } != null
                                } catch (_: Exception) {
                                    false
                                }
                            }
                            Toast.makeText(
                                this@MainActivity,
                                if (written) "Sauvegarde exportée avec succès" else "La sauvegarde n'a pas pu être écrite. Réessayez en choisissant un autre dossier.",
                                if (written) Toast.LENGTH_SHORT else Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }

                val importJsonLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            val result = withContext(disk) {
                                try {
                                    contentResolver.openInputStream(uri)?.use { repository.importFromJson(it) }
                                        ?: Result.failure(IOException("Flux illisible"))
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                            }
                            result.onSuccess { imported ->
                                importedGarden = imported
                                Toast.makeText(
                                    this@MainActivity,
                                    "Potager restauré (${imported.parcels.size} parcelles). L'ancien est gardé en copie de secours.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }.onFailure { error ->
                                val message = (error as? GardenValidationException)?.message
                                    ?: "Le fichier n'a pas pu être importé. Réessayez ou choisissez un autre fichier."
                                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                val exportCsvLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/csv")
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            val written = withContext(disk) {
                                try {
                                    contentResolver.openOutputStream(uri)?.use { out -> repository.exportToCsv(out) } != null
                                } catch (_: Exception) {
                                    false
                                }
                            }
                            Toast.makeText(
                                this@MainActivity,
                                if (written) "Fichier tableur CSV exporté" else "Le fichier tableur n'a pas pu être écrit. Réessayez en choisissant un autre dossier.",
                                if (written) Toast.LENGTH_SHORT else Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }

                SillonApp(
                    externalGarden = importedGarden,
                    onGardenReplaced = { importedGarden = null },
                    photoDiagnosisContent = if (photoAvailable) {
                        { onOpenDisease -> PhotoDiagnosisCard(onOpenDisease = onOpenDisease) }
                    } else {
                        null
                    },
                    onApplyReminders = { enabled, _ ->
                        SowingReminders.apply(this@MainActivity, enabled)
                    },
                    cloudBackupAvailable = BackupPolicy.settingAvailable(Build.VERSION.SDK_INT),
                    cloudBackupPossible = deviceSecure,
                    cloudBackupOn = cloudBackupOn,
                    onCloudBackupChange = { on ->
                        cloudConsent.enabled = on
                        cloudBackupOn = on
                    },
                    onExportJson = {
                        exportJsonLauncher.launch("sillon_sauvegarde_${LocalDate.now()}.json")
                    },
                    onImportJson = {
                        importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    onExportCsv = {
                        exportCsvLauncher.launch("sillon_potager_${LocalDate.now()}.csv")
                    },
                )
            }
        }
    }
}
