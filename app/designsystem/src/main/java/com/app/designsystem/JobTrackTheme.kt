package com.app.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.core.model.ThemeMode

object Spacing {
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val touch = 48.dp
}
private val LightColors = lightColorScheme(
    primary = Color(0xFF215C43), onPrimary = Color.White,
    primaryContainer = Color(0xFFA7F2C2), onPrimaryContainer = Color(0xFF002115),
    secondary = Color(0xFF4D6358), secondaryContainer = Color(0xFFD0E8D7), onSecondaryContainer = Color(0xFF0B2015),
    tertiary = Color(0xFF3C6473), surface = Color(0xFFF8FAF7), onSurface = Color(0xFF17201B), surfaceContainer = Color(0xFFEDEFEA),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF93D5AC), onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF075139), onPrimaryContainer = Color(0xFFADF2C6),
    secondary = Color(0xFFB4CCBC), secondaryContainer = Color(0xFF354C40), onSecondaryContainer = Color(0xFFD0E8D7),
    tertiary = Color(0xFFA4CEDF), surface = Color(0xFF101512), onSurface = Color(0xFFDEE5DD), surfaceContainer = Color(0xFF1B211D),
)
@Composable
fun JobTrackTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = Typography(), content = content)
}
