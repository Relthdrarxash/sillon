package fr.pilmil.sillon.shared

import platform.Foundation.NSDateComponents
import platform.Foundation.NSUserDefaults
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Rappels hebdomadaires de semis sur iOS via UNUserNotificationCenter (samedi 9 h, 100 % hors-ligne).
 */
object IosSowingReminders {
    private const val PREF_ENABLED = "sillon_sowing_reminders"
    private const val IDENTIFIER = "fr.pilmil.sillon.weekly_sowing"

    fun isEnabled(): Boolean = NSUserDefaults.standardUserDefaults.boolForKey(PREF_ENABLED)

    fun setEnabled(enabled: Boolean, onResult: (Boolean) -> Unit) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        if (!enabled) {
            center.removePendingNotificationRequestsWithIdentifiers(listOf(IDENTIFIER))
            NSUserDefaults.standardUserDefaults.setBool(false, forKey = PREF_ENABLED)
            onResult(false)
            return
        }

        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { granted, _ ->
            if (granted) {
                scheduleSaturdayMorning(center)
                NSUserDefaults.standardUserDefaults.setBool(true, forKey = PREF_ENABLED)
                onResult(true)
            } else {
                NSUserDefaults.standardUserDefaults.setBool(false, forKey = PREF_ENABLED)
                onResult(false)
            }
        }
    }

    private fun scheduleSaturdayMorning(center: UNUserNotificationCenter) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(IDENTIFIER))
        val content = UNMutableNotificationContent().apply {
            setTitle("Au potager ce week-end")
            setBody("De nouveaux semis ou plantations s'ouvrent dans votre fenêtre climatique.")
            setSound(UNNotificationSound.defaultSound())
        }
        val components = NSDateComponents().apply {
            weekday = 7 // Samedi (calendrier grégorien Apple : 1 = Dimanche .. 7 = Samedi)
            hour = 9
            minute = 0
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            components,
            repeats = true,
        )
        val request = UNNotificationRequest.requestWithIdentifier(IDENTIFIER, content, trigger)
        center.addNotificationRequest(request, withCompletionHandler = null)
    }
}
