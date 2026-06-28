package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
  )

private val OccultDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF3131),     // NeonRed
    onPrimary = Color(0xFF000000),   // Pitch Black
    secondary = Color(0xFFFFBABA),   // SoftRose
    background = Color(0xFF0A0000),  // DeepRedBlack
    surface = Color(0xFF1A0000),     // DarkMaroon
    onBackground = Color(0xFFE0E0E0),// LightNeutral
    onSurface = Color(0xFFE0E0E0)    // LightNeutral
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Set to false to enforce our gorgeous theme
  content: @Composable () -> Unit,
) {
  val colorScheme = OccultDarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
