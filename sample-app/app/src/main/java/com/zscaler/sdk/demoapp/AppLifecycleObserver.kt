package com.zscaler.sdk.demoapp

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.zscaler.sdk.android.ZscalerSDK

class AppLifecycleObserver : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForeground() {
        // Call this as soon as app comes to foreground or network requests are going to be resumed, whichever is earlier.
        ZscalerSDK.resume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackground() {
        // Call this when app goes to background or when ongoing network requests are finished, whichever is later.
        ZscalerSDK.suspend()
    }

}