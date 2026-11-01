package com.auratune.player.ui

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Мягкие низкоконтрастные палитры Material 3 (как в ArchiveTune и SimpMusic)
val SoftLavenderScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    secondaryContainer = Color(0xFF332D41),
    onSecondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFF141318),
    surface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFF26242C),
    onBackground = Color(0xFFE6E1E9),
    onSurface = Color(0xFFE6E1E9),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

val SoftSageScheme = darkColorScheme(
    primary = Color(0xFFA6D3B8),
    onPrimary = Color(0xFF0F3723),
    primaryContainer = Color(0xFF274E38),
    onPrimaryContainer = Color(0xFFC1EFD3),
    secondary = Color(0xFFB7CCB8),
    secondaryContainer = Color(0xFF2A362D),
    onSecondaryContainer = Color(0xFFD3E8D3),
    background = Color(0xFF121513),
    surface = Color(0xFF1A1E1B),
    surfaceVariant = Color(0xFF232825),
    onBackground = Color(0xFFE1E4E0),
    onSurface = Color(0xFFE1E4E0),
    onSurfaceVariant = Color(0xFFC1C9C1)
)

@Composable
fun AuraTuneMaterial3Theme(
    useSystemMonet: Boolean = true,
    useSagePalette: Boolean = false,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var scheme: ColorScheme = when {
        useSystemMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Нативный движок Material You (Monet) из Android 12..17 (Pixel)
            dynamicDarkColorScheme(context)
        }
        useSagePalette -> SoftSageScheme
        else -> SoftLavenderScheme
    }

    if (amoledBlack) {
        scheme = scheme.copy(
            background = Color(0xFF000000),
            surface = Color(0xFF0B0B0D)
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
