package fr.pilmil.sillon.screenshots

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityOptionsCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import fr.pilmil.sillon.SillonScreen
import fr.pilmil.sillon.data.DiseaseCatalog
import fr.pilmil.sillon.data.FrenchFrostCatalog
import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.engine.DashboardPlanner
import fr.pilmil.sillon.engine.FrostEngine
import fr.pilmil.sillon.model.Climate
import fr.pilmil.sillon.model.Garden as GardenModel
import fr.pilmil.sillon.ui.catalog.CatalogScreen
import fr.pilmil.sillon.ui.components.SillonScaffold
import fr.pilmil.sillon.ui.dashboard.DashboardScreen
import fr.pilmil.sillon.ui.doctor.DoctorScreen
import fr.pilmil.sillon.ui.garden.GardenScreen
import fr.pilmil.sillon.ui.onboarding.OnboardingScreen
import fr.pilmil.sillon.ui.settings.SettingsScreen
import fr.pilmil.sillon.ui.theme.SillonTheme
import org.junit.Rule
import org.junit.Test

/**
 * Une capture par écran, sans téléphone ni émulateur. Les images sortent dans
 * app/build/paparazzi/images ; tools/screenshots.sh les copie vers docs/captures.
 * Non rendus : le viseur CameraX et l'analyse LiteRT du diagnostic photo (matériel requis).
 */
class ScreensTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "fr"),
        maxPercentDifference = 0.1,
        useDeviceResolution = true,
    )

    // L'accueil ouvre des sélecteurs de fichiers et de permissions : un registre factice suffit au rendu.
    private val registryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) = Unit
        }
    }

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        paparazzi.snapshot(name) {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                SillonTheme(darkTheme = dark) { content() }
            }
        }
    }

    /** Un onglet de l'appli dans son enveloppe (barre du bas, onglet courant sélectionné). */
    private fun tab(name: String, screen: SillonScreen, dark: Boolean = false, content: @Composable () -> Unit) =
        shot(name, dark) { SillonScaffold(currentScreen = screen, onSelectScreen = {}, content = content) }

    private val department = FrenchFrostCatalog.getByCode(Sample.DEPARTMENT_CODE)

    private val departmentTropical = FrenchFrostCatalog.getByCode(Sample.DEPARTMENT_CODE_TROPICAL)

    @Composable
    private fun Dashboard(
        garden: GardenModel = Sample.garden,
        today: java.time.LocalDate = Sample.today,
        climate: Climate = Climate.TEMPERATE,
        place: fr.pilmil.sillon.data.DepartmentFrost? = department,
    ) {
        val plan = DashboardPlanner.plan(garden, today, FrostEngine(climate = climate))
        val withAdvice = VegetableCatalog.ALL_VEGETABLES.filter { it.advice.isNotBlank() && climate in it.climates }
        val tip = withAdvice[today.dayOfYear % withAdvice.size].let { "Conseil du jour : ${it.name}" to it.advice }
        DashboardScreen(plan = plan, placeLabel = place?.let { "${it.name} (${it.code})" }, tip = tip)
    }

    // Date figée : les plantes ne changent jamais de stade d'une capture à l'autre.
    @Composable
    private fun Garden(selectedCell: Pair<Int, Int>? = null, perspective: Boolean = true) = GardenScreen(
        garden = Sample.gardenVivant,
        vegetables = VegetableCatalog.ALL_VEGETABLES,
        onUpdateSlot = { _, _, _, _ -> },
        onAddParcel = { _, _, _, _ -> },
        onDeleteParcel = {},
        today = Sample.today,
        initialSelectedCell = selectedCell,
        initialPerspective = perspective,
    )

    @Test fun accueil() = tab("accueil", SillonScreen.DASHBOARD) { Dashboard() }
    @Test fun accueilSombre() = tab("accueil_sombre", SillonScreen.DASHBOARD, dark = true) { Dashboard() }

    @Test fun parcelles() = tab("parcelles", SillonScreen.GARDEN) { Garden() }
    @Test fun parcellesSombre() = tab("parcelles_sombre", SillonScreen.GARDEN, dark = true) { Garden() }

    // Case choisie, feuille de sélection fermée.
    @Test fun parcellesPerspective() = tab("parcelles_perspective", SillonScreen.GARDEN) { Garden(selectedCell = 1 to 2) }
    @Test fun parcellesPerspectiveSombre() = tab("parcelles_perspective_sombre", SillonScreen.GARDEN, dark = true) { Garden(selectedCell = 1 to 2) }

    @Test fun parcellesPlat() = tab("parcelles_plat", SillonScreen.GARDEN) { Garden(perspective = false) }

    // Outre-mer : calendrier ancré sur le début de saison, cultures tropicales.
    @Test fun accueilTropical() = tab("accueil_tropical", SillonScreen.DASHBOARD) {
        Dashboard(Sample.gardenTropical, Sample.todayTropical, Climate.TROPICAL, departmentTropical)
    }

    @Test fun parcellesTropical() = tab("parcelles_tropical", SillonScreen.GARDEN) {
        GardenScreen(
            garden = Sample.gardenTropical,
            vegetables = VegetableCatalog.ALL_VEGETABLES,
            onUpdateSlot = { _, _, _, _ -> },
            onAddParcel = { _, _, _, _ -> },
            onDeleteParcel = {},
            today = Sample.today,
            initialSelectedCell = 0 to 1,
            initialPerspective = true,
        )
    }

    // Fiche dépliée d'une plante aux valeurs indicatives : la mention doit s'y lire.
    @Test fun plantesTropical() = tab("plantes_tropical", SillonScreen.CATALOG) {
        CatalogScreen(vegetables = VegetableCatalog.ALL_VEGETABLES, initiallyExpandedId = "igname", initiallyTropicalOnly = true)
    }

    @Test fun plantes() = tab("plantes", SillonScreen.CATALOG) {
        CatalogScreen(vegetables = VegetableCatalog.ALL_VEGETABLES)
    }

    @Test fun diagnostic() = tab("diagnostic", SillonScreen.DOCTOR) {
        DoctorScreen(diseases = DiseaseCatalog.ALL_DISEASES)
    }

    @Test fun reglages() = tab("reglages", SillonScreen.SETTINGS) {
        SettingsScreen(
            frostMonth = Sample.frostDate.month,
            frostDay = Sample.frostDate.day,
            departmentLabel = department?.let { "${it.name} (${it.code})" },
            remindersOn = true,
            smartViewfinderOn = true,
        )
    }

    // Pas de barre du bas : l'accueil de bienvenue occupe tout l'écran.
    @Test fun bienvenue() = shot("bienvenue") {
        OnboardingScreen(onDepartment = {}, onFinish = {})
    }
}
