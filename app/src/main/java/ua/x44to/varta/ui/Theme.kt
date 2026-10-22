package ua.x44to.varta.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ua.x44to.varta.scan.Level

/** Синій вартового і жовтий: спокійні кольори, щоб червоний «небезпечно» було видно одразу. */
private val Light = lightColorScheme(
    primary = Color(0xFF2747C4), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF), onPrimaryContainer = Color(0xFF001257),
    secondary = Color(0xFF5A5D72), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFE1F9), onSecondaryContainer = Color(0xFF171B2C),
    tertiary = Color(0xFF7A5900), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA1), onTertiaryContainer = Color(0xFF261900),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAF8FF), onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFAF8FF), onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE2E1EC), onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680), outlineVariant = Color(0xFFC6C5D0),
    inverseSurface = Color(0xFF2F3036), inverseOnSurface = Color(0xFFF1F0F7), inversePrimary = Color(0xFFB8C3FF),
    surfaceDim = Color(0xFFDAD9E0), surfaceBright = Color(0xFFFAF8FF),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF4F3FA), surfaceContainer = Color(0xFFEEEDF4),
    surfaceContainerHigh = Color(0xFFE9E7EF), surfaceContainerHighest = Color(0xFFE3E1E9),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB8C3FF), onPrimary = Color(0xFF00218A),
    primaryContainer = Color(0xFF0B31AB), onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC3C5DD), onSecondary = Color(0xFF2C2F42),
    secondaryContainer = Color(0xFF424659), onSecondaryContainer = Color(0xFFDFE1F9),
    tertiary = Color(0xFFFFC857), onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4200), onTertiaryContainer = Color(0xFFFFDEA1),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318), onBackground = Color(0xFFE3E1E9),
    surface = Color(0xFF121318), onSurface = Color(0xFFE3E1E9),
    surfaceVariant = Color(0xFF45464F), onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A), outlineVariant = Color(0xFF45464F),
    inverseSurface = Color(0xFFE3E1E9), inverseOnSurface = Color(0xFF2F3036), inversePrimary = Color(0xFF2747C4),
    surfaceDim = Color(0xFF121318), surfaceBright = Color(0xFF38393F),
    surfaceContainerLowest = Color(0xFF0D0E13), surfaceContainerLow = Color(0xFF1A1B21), surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A2F), surfaceContainerHighest = Color(0xFF34343A),
)

@Composable
fun isDark(theme: Int): Boolean = when (theme) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun VartaTheme(theme: Int, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isDark(theme)) Dark else Light, content = content)
}

private val dark: Boolean
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** Колір рівня: текст/значок і тло картки. */
object LevelColors {
    @Composable @ReadOnlyComposable
    fun fg(l: Level): Color = when (l) {
        Level.DANGER -> if (dark) Color(0xFFFF8A80) else Color(0xFFC62828)
        Level.WARN -> if (dark) Color(0xFFFFC857) else Color(0xFF9A5B00)
        Level.INFO -> MaterialTheme.colorScheme.primary
        Level.OK -> if (dark) Color(0xFF7BD88F) else Color(0xFF1B7F3A)
    }

    @Composable @ReadOnlyComposable
    fun bg(l: Level): Color = when (l) {
        Level.DANGER -> if (dark) Color(0xFF4A1714) else Color(0xFFFFE3DF)
        Level.WARN -> if (dark) Color(0xFF3B2D0C) else Color(0xFFFFF0D2)
        Level.INFO -> MaterialTheme.colorScheme.surfaceContainerHigh
        Level.OK -> if (dark) Color(0xFF15351F) else Color(0xFFDDF4E3)
    }
}
