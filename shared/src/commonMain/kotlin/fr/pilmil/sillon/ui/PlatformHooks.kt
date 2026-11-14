package fr.pilmil.sillon.ui

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

@Composable
expect fun rememberReminderPermissionRequester(onResult: (Boolean) -> Unit): () -> Unit
