package fr.pilmil.sillon.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

class IosSillonPrefs : SillonPrefs {

    private val defaults = NSUserDefaults.standardUserDefaults

    override var onboarded: Boolean
        get() = defaults.boolForKey(KEY_ONBOARDED)
        set(value) {
            defaults.setBool(value, forKey = KEY_ONBOARDED)
        }

    override var departmentCode: String?
        get() = defaults.stringForKey(KEY_DEPARTMENT)
        set(value) {
            if (value == null) {
                defaults.removeObjectForKey(KEY_DEPARTMENT)
            } else {
                defaults.setObject(value, forKey = KEY_DEPARTMENT)
            }
        }

    override var sowingReminders: Boolean
        get() = defaults.boolForKey(KEY_REMINDERS)
        set(value) {
            defaults.setBool(value, forKey = KEY_REMINDERS)
        }

    override var smartViewfinderEnabled: Boolean
        get() = if (defaults.objectForKey(KEY_SMART_VIEWFINDER) == null) true else defaults.boolForKey(KEY_SMART_VIEWFINDER)
        set(value) {
            defaults.setBool(value, forKey = KEY_SMART_VIEWFINDER)
        }

    override var dashboardVisits: Int
        get() = defaults.integerForKey(KEY_DASHBOARD_VISITS).toInt()
        set(value) {
            defaults.setInteger(value.toLong(), forKey = KEY_DASHBOARD_VISITS)
        }

    private companion object {
        const val KEY_ONBOARDED = "sillon_onboarded"
        const val KEY_DEPARTMENT = "sillon_department"
        const val KEY_REMINDERS = "sillon_sowing_reminders"
        const val KEY_SMART_VIEWFINDER = "sillon_smart_viewfinder"
        const val KEY_DASHBOARD_VISITS = "sillon_dashboard_visits"
    }
}

@Composable
actual fun rememberSillonPrefs(): SillonPrefs = remember { IosSillonPrefs() }
