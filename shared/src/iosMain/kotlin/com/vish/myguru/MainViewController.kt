package com.vish.myguru

import androidx.compose.ui.window.ComposeUIViewController
import com.vish.myguru.di.initKoin
import com.vish.myguru.features.vacabulary.ui.MainScreen
import com.vish.myguru.features.vacabulary.ui.MainViewModel
import org.koin.compose.viewmodel.koinViewModel
fun doInitKoin() {
    initKoin()
}

fun MainViewController() = ComposeUIViewController {
    val viewModel: MainViewModel = koinViewModel()
    MainScreen(viewModel = viewModel)
}