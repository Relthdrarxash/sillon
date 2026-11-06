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
    // Teintes claires (Papier lin & Terre vivante — lisibilité plein soleil)
    val LinGround = Color(0xFFF7F4ED)
    val LinSurface = Color(0xFFFFFFFF)
    val LinSurfaceVariant = Color(0xFFEDE8DD)
    val PeatInk = Color(0xFF1B221A)
    val PeatMuted = Color(0xFF566254)
    val Sage = Color(0xFF2E6347)
    val SageDark = Color(0xFF1E432F)
    val SageContainer = Color(0xFFDBE8DC)
    val Terracotta = Color(0xFFA85532)
    val TerracottaContainer = Color(0xFFF8E3D8)
    val SproutGreen = Color(0xFF6E9854)
    val FurrowLine = Color(0xFFDFD7CA)
    val AlertAmber = Color(0xFFB55D18)
    val AlertContainer = Color(0xFFFDF0E2)
    val SynergyBlue = Color(0xFF2E6486)
    val SynergyContainer = Color(0xFFDCEAF2)

    // Teintes sombres (Humus nocturne & Terreau)
    val NightPeatGround = Color(0xFF131713)
    val NightPeatSurface = Color(0xFF1B221B)
    val NightPeatVariant = Color(0xFF242E24)
    val NightText = Color(0xFFEDE9E0)
    val NightMuted = Color(0xFF9AA897)
    val NightSage = Color(0xFF85B092)
    val NightSageContainer = Color(0xFF233D2D)
    val NightTerracotta = Color(0xFFDB8D6D)
    val NightTerracottaContainer = Color(0xFF422419)
    val NightFurrowLine = Color(0xFF2C382B)
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

private fun frauncesStyle(size: Int, weight: FontWeight = FontWeight.SemiBold, lineHeight: Int = (size * 1.25).toInt()) =
    TextStyle(
        fontFamily = FrauncesFontFamily,
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
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
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
