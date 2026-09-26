package com.vish.myguru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vish.myguru.features.vacabulary.ui.MainScreen
import com.vish.myguru.features.vacabulary.ui.MainViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MainViewModel = koinViewModel()
            MainScreen(viewModel = viewModel)
        }
    }
}
