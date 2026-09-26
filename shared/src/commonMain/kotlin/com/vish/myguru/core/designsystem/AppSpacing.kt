package com.vish.myguru.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppSpacing(
    val default: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp, // Standard screen padding
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp
)
// Provides the spacing down the Compose tree
val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
