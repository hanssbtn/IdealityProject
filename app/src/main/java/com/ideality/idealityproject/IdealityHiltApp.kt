package com.ideality.idealityproject

import android.app.Application
import com.google.android.filament.utils.Utils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp class IdealityHiltApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Utils.init()
    }
}