package fr.pilmil.sillon.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.window.ComposeUIViewController
import fr.pilmil.sillon.ui.SillonApp
import fr.pilmil.sillon.ui.theme.SillonTheme
import platform.UIKit.UIViewController

/** Point d'entrée iOS complet de Sillon (UI, persistance locale et rappels UNUserNotificationCenter). */
fun MainViewController(): UIViewController = ComposeUIViewController {
    SillonTheme(darkTheme = isSystemInDarkTheme()) {
        SillonApp(
            onApplyReminders = { enabled, garden ->
                if (enabled) {
                    IosSowingReminders.scheduleWeekly(garden)
                } else {
                    IosSowingReminders.cancelAll()
                }
            },
        )
    }
}
