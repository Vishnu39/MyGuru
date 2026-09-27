package com.vish.myguru.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.vish.myguru.features.vacabulary.ui.MainViewModel
import com.vish.myguru.features.vacabulary.repository.VocabularyRepository
import com.vish.myguru.core.database.AppDatabase
import com.vish.myguru.core.database.WordDao
import com.vish.myguru.core.database.getDatabaseBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule = module {
    // 1. Room Database instance (using the multiplatform Bundled SQLite driver)
    single<AppDatabase> {
        getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

// 2. DAO
    single<WordDao> {
        get<AppDatabase>().wordDao()
    }

    // 3. Repository
    single { VocabularyRepository(wordDao = get()) }

    // 4. ViewModel (injects the single VocabularyRepository)
    viewModel { MainViewModel(get()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(appModule)
}