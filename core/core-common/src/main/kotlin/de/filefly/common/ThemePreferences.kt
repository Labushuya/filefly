package de.filefly.common

// Theme-Modell in core-common (NICHT core-data), damit FileflyTheme in core-ui
// den Typ nutzen kann, ohne dass core-ui von core-data abhängt (Zyklus-Vermeidung).
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ThemePreferences(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    // Default true: FileFly nutzt Material You (dynamische Systemfarbe), wo verfügbar.
    val useDynamicColor: Boolean = true,
)
