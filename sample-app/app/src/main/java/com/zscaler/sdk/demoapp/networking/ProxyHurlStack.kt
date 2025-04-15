package com.zscaler.sdk.demoapp.networking

import android.util.Log
import com.android.volley.toolbox.HurlStack
import com.zscaler.sdk.android.ZscalerSDK
import com.zscaler.sdk.demoapp.util.ProxyUtility
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.ProxySelector
import java.net.URL

class ProxyHurlStack : HurlStack() {
    private val TAG = "ProxyHurlStack"

    @Throws(IOException::class)
    override fun createConnection(url: URL): HttpURLConnection {
        val urlConnection: HttpURLConnection
        var proxy: Proxy? = null
        try {
            proxy = ProxySelector.getDefault().select(url.toURI())[0]
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Exception: ",e)
        }
        urlConnection = if (proxy == null) {
            url.openConnection() as HttpURLConnection
        } else {
            url.openConnection(proxy) as HttpURLConnection
        }

        val proxyInfo = ZscalerSDK.proxyInfo()
        val username = proxyInfo?.username
        val password = proxyInfo?.password

        if (proxyInfo?.username?.isNotEmpty() == true && proxyInfo.password?.isNotEmpty() == true) {
            ProxyUtility.setupAuthenticator(username,password)
        }
        //Return a URLConnection that supports Proxy and Proxy Auth (if needed)
        return urlConnection
    }
}