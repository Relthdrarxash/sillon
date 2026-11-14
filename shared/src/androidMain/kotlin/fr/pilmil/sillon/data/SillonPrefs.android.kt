package fr.pilmil.sillon.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class AndroidSillonPrefs(context: Context) : SillonPrefs {

    private val prefs = context.getSharedPreferences("sillon_prefs", Context.MODE_PRIVATE)

    override var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    override var departmentCode: String?
        get() = prefs.getString(KEY_DEPARTMENT, null)
        set(value) = prefs.edit().putString(KEY_DEPARTMENT, value).apply()

    override var sowingReminders: Boolean
        get() = prefs.getBoolean(KEY_REMINDERS, false)
        set(value) = prefs.edit().putBoolean(KEY_REMINDERS, value).apply()

    override var smartViewfinderEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_VIEWFINDER, true)
        set(value) = prefs.edit().putBoolean(KEY_SMART_VIEWFINDER, value).apply()

    override var dashboardVisits: Int
        get() = prefs.getInt(KEY_DASHBOARD_VISITS, 0)
        set(value) = prefs.edit().putInt(KEY_DASHBOARD_VISITS, value).apply()

    private companion object {
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_DEPARTMENT = "department"
        const val KEY_REMINDERS = "sowing_reminders"
        const val KEY_SMART_VIEWFINDER = "smart_viewfinder"
        const val KEY_DASHBOARD_VISITS = "dashboard_visits"
    }
}

fun SillonPrefs(context: Context): SillonPrefs = AndroidSillonPrefs(context)

@Composable
actual fun rememberSillonPrefs(): SillonPrefs {
    val context = LocalContext.current
    return remember { AndroidSillonPrefs(context) }
}
