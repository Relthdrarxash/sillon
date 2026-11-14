package fr.pilmil.sillon.data

import android.content.Context
import fr.pilmil.sillon.model.Climate

/** Petits réglages hors du fichier du potager : premier lancement, département choisi, rappels. */
class SillonPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("sillon_prefs", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    /** Code du département choisi (ex. « 35 »), ou null si la date a été réglée à la main. */
    var departmentCode: String?
        get() = prefs.getString(KEY_DEPARTMENT, null)
        set(value) = prefs.edit().putString(KEY_DEPARTMENT, value).apply()

    var sowingReminders: Boolean
        get() = prefs.getBoolean(KEY_REMINDERS, false)
        set(value) = prefs.edit().putBoolean(KEY_REMINDERS, value).apply()

    /** Assistant de cadrage photo intelligent (cadre coloré et déclenchement automatique). */
    var smartViewfinderEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_VIEWFINDER, true)
        set(value) = prefs.edit().putBoolean(KEY_SMART_VIEWFINDER, value).apply()

    /** Nombre d'ouvertures de l'accueil pour masquer les textes d'explication répétitifs. */
    var dashboardVisits: Int
        get() = prefs.getInt(KEY_DASHBOARD_VISITS, 0)
        set(value) = prefs.edit().putInt(KEY_DASHBOARD_VISITS, value).apply()

    val department: DepartmentFrost?
        get() = departmentCode?.let(FrenchFrostCatalog::getByCode)

    /** Climat du jardin, déduit du département ; tempéré tant qu'aucun département n'est choisi. */
    val climate: Climate
        get() = department?.climate ?: Climate.TEMPERATE

    private companion object {
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_DEPARTMENT = "department"
        const val KEY_REMINDERS = "sowing_reminders"
        const val KEY_SMART_VIEWFINDER = "smart_viewfinder"
        const val KEY_DASHBOARD_VISITS = "dashboard_visits"
    }
}
