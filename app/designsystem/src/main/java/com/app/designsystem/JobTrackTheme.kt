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
private val LightColors = lightColorScheme(primary = Color(0xFF215C43), secondary = Color(0xFF4D6358), tertiary = Color(0xFF3C6473))
private val DarkColors = darkColorScheme(primary = Color(0xFF93D5AC), secondary = Color(0xFFB4CCBC), tertiary = Color(0xFFA4CEDF))
@Composable
fun JobTrackTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = Typography(), content = content)
}
