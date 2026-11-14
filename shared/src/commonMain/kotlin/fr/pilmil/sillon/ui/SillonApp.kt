package fr.pilmil.sillon.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import fr.pilmil.sillon.data.DiseaseCatalog
import fr.pilmil.sillon.data.FrenchFrostCatalog
import fr.pilmil.sillon.data.GardenStore
import fr.pilmil.sillon.data.GardenUpdate
import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.data.rememberGardenStore
import fr.pilmil.sillon.data.rememberSillonPrefs
import fr.pilmil.sillon.engine.DashboardPlanner
import fr.pilmil.sillon.model.Garden
import fr.pilmil.sillon.model.plusDays
import fr.pilmil.sillon.model.todayLocalDate
import fr.pilmil.sillon.ui.catalog.CatalogScreen
import fr.pilmil.sillon.ui.dashboard.DashboardScreen
import fr.pilmil.sillon.ui.dashboard.ForecastHorizon
import fr.pilmil.sillon.ui.doctor.DoctorScreen
import fr.pilmil.sillon.ui.garden.GardenScreen
import fr.pilmil.sillon.ui.onboarding.DepartmentDialog
import fr.pilmil.sillon.ui.onboarding.OnboardingScreen
import fr.pilmil.sillon.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

enum class SillonScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("Aujourd'hui", Icons.Filled.CalendarMonth),
    GARDEN("Parcelles", Icons.Filled.Yard),
    CATALOG("Plantes", Icons.Filled.Eco),
    DOCTOR("Diagnostic", Icons.Filled.Healing),
    SETTINGS("Réglages", Icons.Filled.Settings),
}

@Composable
fun SillonApp(
    externalGarden: Garden? = null,
    onGardenReplaced: (Garden) -> Unit = {},
    photoDiagnosisContent: (@Composable (onOpenDisease: (String) -> Unit) -> Unit)? = null,
    onApplyReminders: (enabled: Boolean, garden: Garden) -> Unit = { _, _ -> },
    cloudBackupAvailable: Boolean = false,
    cloudBackupPossible: Boolean = false,
    cloudBackupOn: Boolean = false,
    onCloudBackupChange: (Boolean) -> Unit = {},
    onExportJson: ((Garden) -> Unit)? = null,
    onImportJson: ((onLoaded: (Garden) -> Unit) -> Unit)? = null,
    onExportCsv: ((GardenStore) -> Unit)? = null,
) {
    val prefs = rememberSillonPrefs()
    val store = rememberGardenStore()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by rememberSaveable { mutableStateOf(SillonScreen.DASHBOARD) }
    var loadedGarden by remember { mutableStateOf<Garden?>(null) }
    var onboarded by remember { mutableStateOf(prefs.onboarded) }
    var departmentCode by remember { mutableStateOf(prefs.departmentCode) }
    var remindersOn by remember { mutableStateOf(prefs.sowingReminders) }
    var smartViewfinderOn by remember { mutableStateOf(prefs.smartViewfinderEnabled) }
    var pickingDepartment by remember { mutableStateOf(false) }

    val showIntroHints = remember {
        val visits = prefs.dashboardVisits
        prefs.dashboardVisits = visits + 1
        visits < 3
    }

    LaunchedEffect(Unit) {
        loadedGarden = store.loadGarden()
        if (store.consumeRecoveryNotice()) {
            snackbarHostState.showSnackbar(
                "Le fichier du potager était illisible : un potager neuf a été créé."
            )
        }
    }

    LaunchedEffect(externalGarden) {
        if (externalGarden != null) {
            loadedGarden = externalGarden
        }
    }

    fun update(change: () -> GardenUpdate) {
        val result = change()
        loadedGarden = result.garden
        onGardenReplaced(result.garden)
        if (!result.saved) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Le potager n'a pas pu être enregistré : vérifiez l'espace libre."
                )
            }
        }
    }

    fun chooseDepartment(code: String?) {
        prefs.departmentCode = code
        departmentCode = code
        val dep = code?.let(FrenchFrostCatalog::getByCode) ?: return
        update { store.updateFrostDate(dep.medianLastFrost.month, dep.medianLastFrost.day) }
    }

    fun setReminders(on: Boolean, garden: Garden) {
        prefs.sowingReminders = on
        remindersOn = on
        onApplyReminders(on, garden)
    }

    val reminderPermissionRequester = rememberReminderPermissionRequester { granted ->
        val g = loadedGarden ?: return@rememberReminderPermissionRequester
        setReminders(granted, g)
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Notifications refusées : activez-les dans les réglages du téléphone."
                )
            }
        }
    }

    val garden: Garden = loadedGarden ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!onboarded) {
        OnboardingScreen(
            onDepartment = { chooseDepartment(it?.code) },
            onFinish = { reminders ->
                setReminders(reminders, garden)
                prefs.onboarded = true
                onboarded = true
            },
        )
        return
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

    PlatformBackHandler(enabled = currentScreen != SillonScreen.DASHBOARD) {
        currentScreen = SillonScreen.DASHBOARD
    }

    val diseases = DiseaseCatalog.ALL_DISEASES
    val vegetables = VegetableCatalog.ALL_VEGETABLES

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SillonBottomNavigation(
                currentScreen = currentScreen,
                onSelectScreen = { currentScreen = it },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                SillonScreen.DASHBOARD -> {
                    val today = remember { todayLocalDate() }
                    val horizons = remember(garden, today) {
                        listOf(
                            ForecastHorizon("Aujourd'hui", "Travaux autour du jour J", DashboardPlanner.plan(garden, today)),
                            ForecastHorizon("Prochains jours", "Horizon à +3 jours", DashboardPlanner.plan(garden, today.plusDays(3))),
                            ForecastHorizon("Semaine suivante", "Horizon à +7 jours", DashboardPlanner.plan(garden, today.plusDays(7))),
                            ForecastHorizon("À venir", "Horizon à +15 jours", DashboardPlanner.plan(garden, today.plusDays(15))),
                        )
                    }
                    val activeCropIds = remember(garden) {
                        garden.parcels.flatMap { it.slots }.mapNotNull { it.vegetableId }.toSet()
                    }
                    val activeGardenMarker = remember(garden, activeCropIds) {
                        val parcelCount = garden.parcels.size
                        val plantedCount = garden.parcels.sumOf { p -> p.slots.count { it.vegetableId != null } }
                        when {
                            parcelCount == 0 -> null
                            plantedCount == 0 -> "$parcelCount parcelle${if (parcelCount > 1) "s" else ""} prête${if (parcelCount > 1) "s" else ""} à planter"
                            else -> "$parcelCount parcelle${if (parcelCount > 1) "s" else ""} · $plantedCount culture${if (plantedCount > 1) "s" else ""} en place"
                        }
                    }
                    val tip = remember(today, activeCropIds) {
                        val plantedAdvice = vegetables.filter { it.id in activeCropIds && it.advice.isNotBlank() }
                        val pool = plantedAdvice.ifEmpty {
                            vegetables.filter { it.advice.isNotBlank() }
                        }
                        pool.getOrNull(today.dayOfYear % pool.size.coerceAtLeast(1))?.let {
                            "${if (plantedAdvice.isNotEmpty()) "Sur vos parcelles" else "Repère"} · ${it.name}" to it.advice
                        }
                    }
                    DashboardScreen(
                        horizons = horizons,
                        placeLabel = departmentCode?.let(FrenchFrostCatalog::getByCode)?.let { "${it.name} (${it.code})" },
                        tip = tip,
                        showIntroHints = showIntroHints,
                        activeGardenMarker = activeGardenMarker,
                        onNavigateToGarden = { currentScreen = SillonScreen.GARDEN },
                        onNavigateToCatalog = { currentScreen = SillonScreen.CATALOG },
                        onOpenSettings = { pickingDepartment = true },
                    )
                }

                SillonScreen.GARDEN -> GardenScreen(
                    garden = garden,
                    vegetables = vegetables,
                    onUpdateSlot = { pId, r, c, vId ->
                        update { store.updateSlot(pId, r, c, vId) }
                    },
                    onAddParcel = { name, type, r, c ->
                        update { store.addParcel(name, type, r, c) }
                    },
                    onDeleteParcel = { pId ->
                        update { store.deleteParcel(pId) }
                    },
                )

                SillonScreen.CATALOG -> CatalogScreen(
                    vegetables = vegetables,
                )

                SillonScreen.DOCTOR -> DoctorScreen(
                    diseases = diseases,
                    photoDiagnosisContent = photoDiagnosisContent,
                )

                SillonScreen.SETTINGS -> SettingsScreen(
                    frostMonth = garden.frostDate.month,
                    frostDay = garden.frostDate.day,
                    onFrostDateChange = { m, d ->
                        val day = d.coerceAtMost(daysInMonth(m))
                        update { store.updateFrostDate(m, day) }
                        prefs.departmentCode = null
                        departmentCode = null
                    },
                    departmentLabel = departmentCode?.let(FrenchFrostCatalog::getByCode)?.let { "${it.name} (${it.code})" },
                    onPickDepartment = { pickingDepartment = true },
                    remindersOn = remindersOn,
                    onRemindersChange = { on ->
                        if (on) {
                            reminderPermissionRequester()
                        } else {
                            setReminders(false, garden)
                        }
                    },
                    cloudBackupAvailable = cloudBackupAvailable,
                    cloudBackupPossible = cloudBackupPossible,
                    cloudBackupOn = cloudBackupOn,
                    onCloudBackupChange = onCloudBackupChange,
                    smartViewfinderOn = smartViewfinderOn,
                    onSmartViewfinderChange = { on ->
                        prefs.smartViewfinderEnabled = on
                        smartViewfinderOn = on
                    },
                    onExportJson = { onExportJson?.invoke(garden) },
                    onImportJson = {
                        onImportJson?.invoke { imported ->
                            store.saveGarden(imported)
                            loadedGarden = imported
                        }
                    },
                    onExportCsv = { onExportCsv?.invoke(store) },
                )
            }
        }
    }
}

private fun daysInMonth(month: Int): Int = when (month) {
    2 -> 28
    4, 6, 9, 11 -> 30
    else -> 31
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

        SillonScreen.entries.forEach { screen ->
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
