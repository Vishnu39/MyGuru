package com.vish.myguru.core.designsystem

import androidx.compose.ui.graphics.Color

data class AppColors(
    val primaryBackground: Color = Color(0xFF121212),
    val surface: Color = Color(0xFF1E1E1E),
    val primaryText: Color = Color(0xFFFFFFFF),
    val secondaryText: Color = Color(0xFFAAAAAA),

    // Semantic colors for German Mechanics
    val articleMasculine: Color = Color(0xFF4A90E2), // der (Blue)
    val articleFeminine: Color = Color(0xFFE24A4A),  // die (Red)
    val articleNeuter: Color = Color(0xFF4AE26B)     // das (Green)
)
