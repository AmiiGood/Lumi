package io.github.amiigood.lumi.lumi.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = LumiPrimaryLight,
    onPrimary = LumiOnPrimaryLight,
    primaryContainer = LumiPrimaryContainerLight,
    onPrimaryContainer = LumiOnPrimaryContainerLight,
    secondary = LumiSecondaryLight,
    onSecondary = LumiOnSecondaryLight,
    secondaryContainer = LumiSecondaryContainerLight,
    onSecondaryContainer = LumiOnSecondaryContainerLight,
    background = LumiBackgroundLight,
    onBackground = LumiOnBackgroundLight,
    surface = LumiSurfaceLight,
    onSurface = LumiOnSurfaceLight,
    surfaceVariant = LumiSurfaceVariantLight,
    onSurfaceVariant = LumiOnSurfaceVariantLight,
    outline = LumiOutlineLight
)

private val DarkColors = darkColorScheme(
    primary = LumiPrimaryDark,
    onPrimary = LumiOnPrimaryDark,
    primaryContainer = LumiPrimaryContainerDark,
    onPrimaryContainer = LumiOnPrimaryContainerDark,
    secondary = LumiSecondaryDark,
    onSecondary = LumiOnSecondaryDark,
    secondaryContainer = LumiSecondaryContainerDark,
    onSecondaryContainer = LumiOnSecondaryContainerDark,
    background = LumiBackgroundDark,
    onBackground = LumiOnBackgroundDark,
    surface = LumiSurfaceDark,
    onSurface = LumiOnSurfaceDark,
    surfaceVariant = LumiSurfaceVariantDark,
    onSurfaceVariant = LumiOnSurfaceVariantDark,
    outline = LumiOutlineDark
)

@Composable
fun LumiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LumiTypography,
        content = content
    )
}