package fr.pilmil.sillon.ui

import androidx.compose.runtime.Composable
import fr.pilmil.sillon.shared.IosSowingReminders

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Pas de bouton retour matériel sur iOS.
}

@Composable
actual fun rememberReminderPermissionRequester(onResult: (Boolean) -> Unit): () -> Unit = {
    IosSowingReminders.requestAuthorization { granted ->
        onResult(granted)
    }
}
