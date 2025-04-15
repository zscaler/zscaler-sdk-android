package com.zscaler.sdk.demoapp.util

import android.util.Log
import android.webkit.HttpAuthHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.zscaler.sdk.android.networking.ZscalerSDKProxyInfo
import java.net.Authenticator
import java.net.PasswordAuthentication

object ProxyUtility {
    private const val TAG = "ProxyUtility"

    /**
     * Clear the web view proxy settings in manual mode
     */
    fun clearWebViewProxy() {
        if (isWebKitClassAvailable()) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().clearProxyOverride(
                    {
                        Log.d(TAG,
                            "clearWebViewProxy: clearProxyOverride success")
                    },
                    {
                        Log.e(TAG, "clearWebViewProxy: Failed to clear proxy in WebView")
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

    /**
     * Set the web view proxy settings in manual mode
     * After starting either tunnel, this needs to be called for setting the web view proxy settings to route network traffic over the tunnel
     *
     * Note that as per android official docs, web view network connections are not guaranteed to
     * immediately use the new proxy setting; wait for the success listener before loading a page.
     */
    fun setWebViewProxy(proxyInfo: ZscalerSDKProxyInfo) {
        if (isWebKitClassAvailable()) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                val proxyConfig = ProxyConfig.Builder()
                    .addProxyRule("${proxyInfo.proxyHost}:${proxyInfo.proxyPort}")
                    .build()
                ProxyController.getInstance().setProxyOverride(proxyConfig,
                    {
                        //on success
                        Log.d(TAG, "setProxyInWebView: setProxyOverride success")
                    },
                    {
                        Log.e(TAG, "setProxyInWebView: Failed to set proxy in WebView")
                    })
            } else {
                Log.e(TAG, "setProxyInWebView: WebView proxy override not supported")
            }
        } else {
            Log.e(TAG, "setProxyInWebView: Androidx Webkit dependencies not found")
        }
    }

    fun setupAuthenticator(username: String?, password: String?) {
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password?.toCharArray())
            }
        })
    }
}


class WebViewClientWithProxyAuthSupport(private val proxyInfo: ZscalerSDKProxyInfo) : WebViewClient() {
    override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
        if (proxyInfo.username?.isNotEmpty() == true && proxyInfo.password?.isNotEmpty() == true) {
            handler?.proceed(proxyInfo.username, proxyInfo.password)
        }
        super.onReceivedHttpAuthRequest(view, handler, host, realm)
    }

    // to block redirection to chrome or default browser , instead url should open in web-view
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return false
    }
}
