package com.example.financeapp.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF005226),
    onPrimaryContainer = Color(0xFF8AD79C),
    secondary = GreenSecondaryDark,
    onSecondary = Color(0xFF243526),
    secondaryContainer = Color(0xFF3B4B3C),
    onSecondaryContainer = Color(0xFFBACCB8),
    tertiary = GreenTertiaryDark,
    onTertiary = Color(0xFF00363B),
    tertiaryContainer = Color(0xFF1E4D51),
    onTertiaryContainer = Color(0xFFA0D0D4)
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA3F2B4),
    onPrimaryContainer = Color(0xFF00210B),
    secondary = GreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8D3),
    onSecondaryContainer = Color(0xFF101F12),
    tertiary = GreenTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEBF1),
    onTertiaryContainer = Color(0xFF001F23)
)

@Composable
fun FinanceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor to false so custom green colors are used instead of Android Material You system colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
