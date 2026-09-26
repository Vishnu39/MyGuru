package com.vish.myguru.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class AppTypography(
    val headingLarge: TextStyle = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold
    ),
    val bodyNormal: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    val labelSmall: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Light
    )

)

val LocalAppTypography = staticCompositionLocalOf { AppTypography() }
