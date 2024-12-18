package com.zscaler.sdk.demoapp.lifecycle

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }
}