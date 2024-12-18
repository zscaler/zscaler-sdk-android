package com.zscaler.sdk.demoapp.util

import android.util.Log
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature

object ProxyUtility {
    private const val TAG = "ProxyUtility"

    fun clearWebViewProxy() {
        if (isWebKitClassAvailable()) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().clearProxyOverride(
                    {
                        Log.d(TAG,
                            "clearWebViewProxy: clearProxyOverride execute() called with: proxyManager = $it")
                    },
                    {
                        Log.d(TAG, "clearWebViewProxy: clearProxyOverride run() called")
                        Log.d(TAG, "clearWebViewProxy: Failed to clear proxy in WebView")
                    }
                )
            } else {
                Log.e(TAG, "clearWebViewProxy: WebView proxy override not supported")
            }
        } else {
            Log.e(TAG, "clearWebViewProxy: Androidx Webkit dependencies not found")
        }
    }

    private fun isWebKitClassAvailable(): Boolean {
        try {
            Class.forName("androidx.webkit.WebViewFeature")
            Class.forName("androidx.webkit.ProxyController")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "isWebKitClassAvailable: Androidx Webkit dependencies not found")
        }
        return false
    }
}