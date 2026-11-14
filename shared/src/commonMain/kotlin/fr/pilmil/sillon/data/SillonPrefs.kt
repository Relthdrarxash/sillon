package fr.pilmil.sillon.data

import androidx.compose.runtime.Composable

interface SillonPrefs {
    var onboarded: Boolean
    var departmentCode: String?
    var sowingReminders: Boolean
    var smartViewfinderEnabled: Boolean
    var dashboardVisits: Int

    val department: DepartmentFrost?
        get() = departmentCode?.let(FrenchFrostCatalog::getByCode)
}

@Composable
expect fun rememberSillonPrefs(): SillonPrefs
