package com.crewroster.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class CrewRosterExtraColors(
    val car1: Color,
    val car2: Color,
    val car3: Color,
    val muted: Color,
    val statusGood: Color,
    val statusWarning: Color,
    val statusCritical: Color,
    val sequential: Color,
    val sequentialTrack: Color,
    val grid: Color,
    val surface2: Color,
    val text2: Color
)

private val LightExtraColors = CrewRosterExtraColors(
    car1 = Car1Light,
    car2 = Car2Light,
    car3 = Car3Light,
    muted = Muted,
    statusGood = StatusGood,
    statusWarning = StatusWarning,
    statusCritical = StatusCriticalLight,
    sequential = SequentialBlueLight,
    sequentialTrack = GridLight,
    grid = GridLight,
    surface2 = Surface2Light,
    text2 = Text2Light
)

private val DarkExtraColors = CrewRosterExtraColors(
    car1 = Car1Dark,
    car2 = Car2Dark,
    car3 = Car3Dark,
    muted = Muted,
    statusGood = StatusGood,
    statusWarning = StatusWarning,
    statusCritical = StatusCriticalDark,
    sequential = SequentialBlueDark,
    sequentialTrack = GridDark,
    grid = GridDark,
    surface2 = Surface2Dark,
    text2 = Text2Dark
)

private val LocalCrewRosterColors = staticCompositionLocalOf { LightExtraColors }

private val LightColorScheme = lightColorScheme(
    primary = Car1Light,
    onPrimary = Color.White,
    secondary = Car2Light,
    onSecondary = Color.White,
    tertiary = Car3Light,
    onTertiary = Color.White,
    background = PageLight,
    onBackground = TextLight,
    surface = SurfaceLight,
    onSurface = TextLight,
    surfaceVariant = Surface2Light,
    onSurfaceVariant = Text2Light,
    error = StatusCriticalLight,
    onError = Color.White,
    outline = GridLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Car1Dark,
    onPrimary = Color.White,
    secondary = Car2Dark,
    onSecondary = Color.White,
    tertiary = Car3Dark,
    onTertiary = Color.White,
    background = PageDark,
    onBackground = TextDark,
    surface = SurfaceDark,
    onSurface = TextDark,
    surfaceVariant = Surface2Dark,
    onSurfaceVariant = Text2Dark,
    error = StatusCriticalDark,
    onError = Color.Black,
    outline = GridDark
)

private val CrewRosterTypography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
)

object CrewRosterExtras {
    val colors: CrewRosterExtraColors
        @Composable get() = LocalCrewRosterColors.current
}

@Composable
fun CrewRosterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalCrewRosterColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CrewRosterTypography,
            content = content
        )
    }
}
