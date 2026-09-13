package de.filefly.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import de.filefly.common.ThemeMode
import de.filefly.common.ThemePreferences

// FileFly Material-3-Theme. Standardmäßig Material You (dynamische Systemfarbe) auf
// Android 12+, mit einer Marken-Palette als Fallback (Firefly-Gold auf Nachtblau).
private val FireflyGold = Color(0xFFFFC24B)
private val FireflyGoldDark = Color(0xFF8A6A1F)
private val NightBlue = Color(0xFF0E1A2B)
private val NightBlueSurface = Color(0xFF16263D)
private val Sky = Color(0xFF6FB2E0)

private val LightColors =
    lightColorScheme(
        primary = FireflyGoldDark,
        onPrimary = Color.White,
        secondary = Sky,
        tertiary = FireflyGold,
        background = Color(0xFFFDFBF6),
        surface = Color(0xFFFFFFFF),
    )

private val DarkColors =
    darkColorScheme(
        primary = FireflyGold,
        onPrimary = NightBlue,
        secondary = Sky,
        tertiary = FireflyGold,
        background = NightBlue,
        surface = NightBlueSurface,
        onBackground = Color(0xFFEAF0F7),
        onSurface = Color(0xFFEAF0F7),
    )

@Composable
fun FileFlyTheme(
    prefs: ThemePreferences = ThemePreferences(),
    systemInDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dark =
        when (prefs.mode) {
            ThemeMode.SYSTEM -> systemInDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val colorScheme =
        when {
            prefs.useDynamicColor && dynamicAvailable && dark -> dynamicDarkColorScheme(context)
            prefs.useDynamicColor && dynamicAvailable -> dynamicLightColorScheme(context)
            dark -> DarkColors
            else -> LightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
