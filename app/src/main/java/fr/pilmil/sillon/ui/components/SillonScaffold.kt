package fr.pilmil.sillon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.pilmil.sillon.SillonScreen

/** Enveloppe de l'appli : barre de navigation du bas et contenu de l'onglet courant. Partagée avec les captures d'écran. */
@Composable
fun SillonScaffold(
    currentScreen: SillonScreen,
    onSelectScreen: (SillonScreen) -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            SillonBottomNavigation(
                currentScreen = currentScreen,
                onSelectScreen = onSelectScreen,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
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

        SillonScreen.values().forEach { screen ->
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
