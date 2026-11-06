package fr.pilmil.sillon

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import fr.pilmil.sillon.data.DiseaseCatalog
import fr.pilmil.sillon.data.GardenRepository
import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.model.BotanicalFamily
import fr.pilmil.sillon.model.Disease
import fr.pilmil.sillon.model.FrostDate
import fr.pilmil.sillon.model.SunExposure
import fr.pilmil.sillon.model.Vegetable
import fr.pilmil.sillon.model.WaterNeed
import fr.pilmil.sillon.ui.catalog.CatalogScreen
import fr.pilmil.sillon.ui.dashboard.DashboardScreen
import fr.pilmil.sillon.ui.doctor.DoctorScreen
import fr.pilmil.sillon.ui.garden.GardenScreen
import fr.pilmil.sillon.ui.settings.SettingsScreen
import fr.pilmil.sillon.ui.theme.SillonTheme
import java.time.LocalDate

enum class SillonScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("Aujourd'hui", Icons.Filled.CalendarMonth),
    GARDEN("Parcelles", Icons.Filled.Yard),
    CATALOG("Plantes", Icons.Filled.Eco),
    DOCTOR("Diagnostic", Icons.Filled.Healing),
    SETTINGS("Réglages", Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: GardenRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = GardenRepository(this)

        setContent {
            SillonTheme(darkTheme = isSystemInDarkTheme()) {
                var currentScreen by remember { mutableStateOf(SillonScreen.DASHBOARD) }
                var garden by remember { mutableStateOf(repository.loadGarden()) }

                // Lanceur d'exportation JSON (SAF)
                val exportJsonLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) {
                        try {
                            contentResolver.openOutputStream(uri)?.use { out ->
                                repository.exportToJson(out)
                            }
                            Toast.makeText(this, "Sauvegarde exportée avec succès", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Erreur lors de l'export : ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                // Lanceur d'importation JSON (SAF)
                val importJsonLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        try {
                            contentResolver.openInputStream(uri)?.use { input ->
                                val result = repository.importFromJson(input)
                                result.onSuccess { imported ->
                                    garden = imported
                                    Toast.makeText(this, "Potager restauré (${imported.parcels.size} parcelles)", Toast.LENGTH_SHORT).show()
                                }.onFailure { error ->
                                    Toast.makeText(this, "Fichier invalide : ${error.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Erreur d'import : ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                // Lanceur d'exportation CSV (SAF)
                val exportCsvLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/csv")
                ) { uri ->
                    if (uri != null) {
                        try {
                            contentResolver.openOutputStream(uri)?.use { out ->
                                repository.exportToCsv(out)
                            }
                            Toast.makeText(this, "Fichier tableur CSV exporté", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Erreur d'export : ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                BackHandler(enabled = currentScreen != SillonScreen.DASHBOARD) {
                    currentScreen = SillonScreen.DASHBOARD
                }

                // Obtention directe et typée des catalogues agronomiques
                val diseases = DiseaseCatalog.ALL_DISEASES
                val vegetables = VegetableCatalog.ALL_VEGETABLES

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        SillonBottomNavigation(
                            currentScreen = currentScreen,
                            onSelectScreen = { currentScreen = it },
                        )
                    },
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            SillonScreen.DASHBOARD -> DashboardScreen(
                                frostDateDescription = "${garden.frostDate.day} ${getMonthName(garden.frostDate.month)}",
                                daysUntilFrost = calculateDaysUntilFrost(garden.frostDate),
                                onNavigateToGarden = { currentScreen = SillonScreen.GARDEN },
                                onNavigateToCatalog = { currentScreen = SillonScreen.CATALOG },
                                onNavigateToDoctor = { currentScreen = SillonScreen.DOCTOR },
                            )

                            SillonScreen.GARDEN -> GardenScreen(
                                garden = garden,
                                vegetables = vegetables,
                                onUpdateSlot = { pId, r, c, vId ->
                                    garden = repository.updateSlot(pId, r, c, vId)
                                },
                                onAddParcel = { name, type, r, c ->
                                    garden = repository.addParcel(name, type, r, c)
                                },
                                onDeleteParcel = { pId ->
                                    garden = repository.deleteParcel(pId)
                                },
                            )

                            SillonScreen.CATALOG -> CatalogScreen(
                                vegetables = vegetables,
                            )

                            SillonScreen.DOCTOR -> DoctorScreen(
                                diseases = diseases,
                            )

                            SillonScreen.SETTINGS -> SettingsScreen(
                                frostMonth = garden.frostDate.month,
                                frostDay = garden.frostDate.day,
                                onFrostDateChange = { m, d ->
                                    garden = repository.updateFrostDate(m, d)
                                },
                                onExportJson = {
                                    val fileName = "sillon_sauvegarde_${LocalDate.now()}.json"
                                    exportJsonLauncher.launch(fileName)
                                },
                                onImportJson = {
                                    importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onExportCsv = {
                                    val fileName = "sillon_potager_${LocalDate.now()}.csv"
                                    exportCsvLauncher.launch(fileName)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getCatalogVegetables(): List<Vegetable> = VegetableCatalog.ALL_VEGETABLES

    private fun calculateDaysUntilFrost(frostDate: FrostDate): Int {
        val now = LocalDate.now()
        val thisYearFrost = LocalDate.of(now.year, frostDate.month, frostDate.day)
        return (thisYearFrost.toEpochDay() - now.toEpochDay()).toInt()
    }

    private fun getMonthName(month: Int): String {
        val months = listOf(
            "janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre"
        )
        return months.getOrElse(month - 1) { "" }
    }
}

@Composable
private fun SillonBottomNavigation(
    currentScreen: SillonScreen,
    onSelectScreen: (SillonScreen) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        val navColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SillonScreen.values().forEach { screen ->
            NavigationBarItem(
                selected = currentScreen == screen,
                onClick = { onSelectScreen(screen) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                alwaysShowLabel = true,
                colors = navColors,
            )
        }
    }
}
