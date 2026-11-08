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
import androidx.compose.runtime.saveable.rememberSaveable
import fr.pilmil.sillon.data.FrenchFrostCatalog
import fr.pilmil.sillon.data.SillonPrefs
import fr.pilmil.sillon.engine.DashboardPlanner
import fr.pilmil.sillon.reminders.SowingReminders
import fr.pilmil.sillon.ui.onboarding.DepartmentDialog
import fr.pilmil.sillon.ui.onboarding.OnboardingScreen

enum class SillonScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("Aujourd'hui", Icons.Filled.CalendarMonth),
    GARDEN("Parcelles", Icons.Filled.Yard),
    CATALOG("Plantes", Icons.Filled.Eco),
    DOCTOR("Diagnostic", Icons.Filled.Healing),
    SETTINGS("Réglages", Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: GardenRepository
    private lateinit var prefs: SillonPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = GardenRepository(this)
        prefs = SillonPrefs(this)

        setContent {
            SillonTheme(darkTheme = isSystemInDarkTheme()) {
                var currentScreen by rememberSaveable { mutableStateOf(SillonScreen.DASHBOARD) }
                var garden by remember { mutableStateOf(repository.loadGarden()) }
                var onboarded by remember { mutableStateOf(prefs.onboarded) }
                var departmentCode by remember { mutableStateOf(prefs.departmentCode) }
                var remindersOn by remember { mutableStateOf(prefs.sowingReminders) }
                var pickingDepartment by remember { mutableStateOf(false) }

                fun chooseDepartment(code: String?) {
                    prefs.departmentCode = code
                    departmentCode = code
                    val dep = code?.let(FrenchFrostCatalog::getByCode) ?: return
                    garden = repository.updateFrostDate(dep.medianLastFrost.month, dep.medianLastFrost.day)
                }

                fun setReminders(on: Boolean) {
                    prefs.sowingReminders = on
                    remindersOn = on
                    SowingReminders.apply(this, on)
                }

                val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    setReminders(granted)
                    if (!granted) Toast.makeText(this, "Notifications refusées : activez-les dans les réglages du téléphone pour recevoir les rappels.", Toast.LENGTH_LONG).show()
                }

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
                            Toast.makeText(this, "La sauvegarde n'a pas pu être écrite. Réessayez en choisissant un autre dossier.", Toast.LENGTH_LONG).show()
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
                                    Toast.makeText(this, "Ce fichier n'est pas une sauvegarde Sillon. Choisissez le fichier .json exporté depuis Sillon.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Le fichier n'a pas pu être lu. Réessayez ou choisissez un autre fichier.", Toast.LENGTH_LONG).show()
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
                            Toast.makeText(this, "Le fichier tableur n'a pas pu être écrit. Réessayez en choisissant un autre dossier.", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                if (!onboarded) {
                    OnboardingScreen(
                        onDepartment = { chooseDepartment(it?.code) },
                        onFinish = { reminders ->
                            setReminders(reminders)
                            prefs.onboarded = true
                            onboarded = true
                        },
                    )
                    return@SillonTheme
                }

                if (pickingDepartment) {
                    DepartmentDialog(
                        selectedCode = departmentCode,
                        onPick = {
                            chooseDepartment(it.code)
                            pickingDepartment = false
                        },
                        onDismiss = { pickingDepartment = false },
                    )
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
                            SillonScreen.DASHBOARD -> {
                                val today = LocalDate.now()
                                val plan = remember(garden, today) { DashboardPlanner.plan(garden, today) }
                                val tip = remember(today) {
                                    val withAdvice = vegetables.filter { it.advice.isNotBlank() }
                                    withAdvice.getOrNull(today.dayOfYear % withAdvice.size.coerceAtLeast(1))?.let { "Conseil du jour : ${it.name}" to it.advice }
                                }
                                DashboardScreen(
                                    plan = plan,
                                    placeLabel = departmentCode?.let(FrenchFrostCatalog::getByCode)?.let { "${it.name} (${it.code})" },
                                    tip = tip,
                                    onNavigateToGarden = { currentScreen = SillonScreen.GARDEN },
                                    onNavigateToCatalog = { currentScreen = SillonScreen.CATALOG },
                                    onOpenSettings = { pickingDepartment = true },
                                )
                            }

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
                                    // Une date réglée à la main ne correspond plus au département.
                                    // Le jour est ramené à la longueur du mois (pas de 31 avril ni de 29 février).
                                    val day = d.coerceAtMost(java.time.YearMonth.of(2023, m).lengthOfMonth())
                                    garden = repository.updateFrostDate(m, day)
                                    prefs.departmentCode = null
                                    departmentCode = null
                                },
                                departmentLabel = departmentCode?.let(FrenchFrostCatalog::getByCode)?.let { "${it.name} (${it.code})" },
                                onPickDepartment = { pickingDepartment = true },
                                remindersOn = remindersOn,
                                onRemindersChange = { on ->
                                    if (on && !SowingReminders.canNotify(this@MainActivity) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        setReminders(on)
                                    }
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
