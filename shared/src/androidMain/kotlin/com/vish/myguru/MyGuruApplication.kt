package com.vish.myguru

import android.app.Application
import com.vish.myguru.core.database.appContext
import com.vish.myguru.di.initKoin
import org.koin.android.ext.koin.androidContext

class MyGuruApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Supplies context to the Android DatabaseBuilder
        appContext = this

        // Starts Koin with Android application context
        initKoin {
            androidContext(this@MyGuruApplication)
        }
    }
}