package fr.pilmil.sillon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.pilmil.sillon.R

object SillonPalette {
    // Teintes claires (Blé doré & Feuillage vif — lisibilité plein soleil)
    val LinGround = Color(0xFFFFF7DD)
    val LinSurface = Color(0xFFFFFFFF)
    val LinSurfaceVariant = Color(0xFFF6E9C4)
    val PeatInk = Color(0xFF2A1F14)
    val PeatMuted = Color(0xFF5E5240)
    val Sage = Color(0xFF2B7A3B)
    val SageDark = Color(0xFF1B5028)
    val SageContainer = Color(0xFFD6F0B8)
    val Terracotta = Color(0xFFBF3F27)
    val TerracottaContainer = Color(0xFFFFDDD2)
    val SproutGreen = Color(0xFF5FA843)
    val FurrowLine = Color(0xFFE2CFA0)
    val AlertAmber = Color(0xFFC2560F)
    val AlertContainer = Color(0xFFFFE9C7)
    val SynergyBlue = Color(0xFF1F6A96)
    val SynergyContainer = Color(0xFFD3ECFA)

    // Teintes sombres (Humus nocturne & Terreau)
    val NightPeatGround = Color(0xFF17140F)
    val NightPeatSurface = Color(0xFF241F17)
    val NightPeatVariant = Color(0xFF30291D)
    val NightText = Color(0xFFF5EEDC)
    val NightMuted = Color(0xFFB5AB94)
    val NightSage = Color(0xFF9AD27F)
    val NightSageContainer = Color(0xFF2A4A22)
    val NightTerracotta = Color(0xFFF09A7B)
    val NightTerracottaContainer = Color(0xFF4A2418)
    val LedgeLight = Color(0xFFD8BE7E)
    val LedgeDark = Color(0xFF0C0A07)
    val NightFurrowLine = Color(0xFF433824)
}

val FrauncesFontFamily = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

val NunitoFontFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
)

private fun frauncesStyle(size: Int, weight: FontWeight = FontWeight.Bold, lineHeight: Int = (size * 1.25).toInt()) =
    TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
    )

private fun nunitoStyle(size: Int, weight: FontWeight = FontWeight.Normal, lineHeight: Int = (size * 1.35).toInt()) =
    TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
    )

private val SillonTypography = Typography(
    displayLarge = frauncesStyle(36),
    displayMedium = frauncesStyle(30),
    displaySmall = frauncesStyle(26),
    headlineLarge = frauncesStyle(24),
    headlineMedium = frauncesStyle(21),
    headlineSmall = frauncesStyle(19),
    titleLarge = frauncesStyle(18),
    titleMedium = nunitoStyle(17, FontWeight.Bold),
    titleSmall = nunitoStyle(15, FontWeight.Bold),
    bodyLarge = nunitoStyle(18, FontWeight.Normal, 26),
    bodyMedium = nunitoStyle(16, FontWeight.Normal, 23),
    bodySmall = nunitoStyle(14, FontWeight.Normal, 20),
    labelLarge = nunitoStyle(16, FontWeight.Bold),
    labelMedium = nunitoStyle(14, FontWeight.SemiBold),
    labelSmall = nunitoStyle(12, FontWeight.SemiBold),
)

private val SillonShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val LightColors: ColorScheme = lightColorScheme(
    primary = SillonPalette.Sage,
    onPrimary = Color.White,
    primaryContainer = SillonPalette.SageContainer,
    onPrimaryContainer = SillonPalette.SageDark,
    secondary = SillonPalette.Terracotta,
    onSecondary = Color.White,
    secondaryContainer = SillonPalette.TerracottaContainer,
    onSecondaryContainer = SillonPalette.Terracotta,
    tertiary = SillonPalette.SproutGreen,
    onTertiary = Color.White,
    background = SillonPalette.LinGround,
    onBackground = SillonPalette.PeatInk,
    surface = SillonPalette.LinSurface,
    onSurface = SillonPalette.PeatInk,
    surfaceVariant = SillonPalette.LinSurfaceVariant,
    onSurfaceVariant = SillonPalette.PeatMuted,
    outline = SillonPalette.FurrowLine,
    outlineVariant = SillonPalette.LinSurfaceVariant,
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = SillonPalette.NightSage,
    onPrimary = SillonPalette.NightPeatGround,
    primaryContainer = SillonPalette.NightSageContainer,
    onPrimaryContainer = SillonPalette.NightSage,
    secondary = SillonPalette.NightTerracotta,
    onSecondary = SillonPalette.NightPeatGround,
    secondaryContainer = SillonPalette.NightTerracottaContainer,
    onSecondaryContainer = SillonPalette.NightTerracotta,
    tertiary = SillonPalette.SproutGreen,
    onTertiary = Color.White,
    background = SillonPalette.NightPeatGround,
    onBackground = SillonPalette.NightText,
    surface = SillonPalette.NightPeatSurface,
    onSurface = SillonPalette.NightText,
    surfaceVariant = SillonPalette.NightPeatVariant,
    onSurfaceVariant = SillonPalette.NightMuted,
    outline = SillonPalette.NightFurrowLine,
    outlineVariant = SillonPalette.NightPeatVariant,
)

@Composable
fun SillonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SillonTypography,
        shapes = SillonShapes,
        content = content,
    )
}
