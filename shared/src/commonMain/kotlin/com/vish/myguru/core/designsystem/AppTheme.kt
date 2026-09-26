package com.vish.myguru

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vish.myguru.core.designsystem.AppColors
import com.vish.myguru.core.designsystem.AppSpacing


// 2. TOP-LEVEL Declarations (This is what Android Studio was missing)
val LocalAppColors = staticCompositionLocalOf { AppColors() }
val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }

// 3. The Theme Object (Used in your UI code like Theme.colors.surface)
object Theme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSpacing.current
}

// 4. The Provider Composable (Wraps your app)
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppColors provides AppColors(),
        LocalAppSpacing provides AppSpacing(),
        content = content
    )
}