package com.zscaler.sdk.demoapp

import android.util.Log
import com.zscaler.sdk.android.ZscalerSDK

import com.zscaler.sdk.android.networking.ZscalerSDKProxyInfo
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.Route
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

interface ApiService {

    @GET
    fun getData(@Url url: String): Call<ResponseBody>

    @FormUrlEncoded
    @POST
    fun postData(
        @Url url: String,
        @FieldMap params: Map<String, String> = emptyMap()
    ): Call<ResponseBody>
}

object ManualRetrofitApiClient {
    private var TAG = "ManualRetrofitApiClient"
    private var retrofit: Retrofit? = null
    private var baseUrl: String = ""

    fun getRetrofitWithProxyInfo(baseUrl: String, proxyInfo: ZscalerSDKProxyInfo): Retrofit? {
        if (retrofit == null || ManualRetrofitApiClient.baseUrl != baseUrl) {
            val client = getOkHttpclient(proxyInfo)
            ManualRetrofitApiClient.baseUrl = baseUrl
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit
    }

    fun clearRetroFitInstance() {
        retrofit = null
    }

    private fun getOkHttpclient(zscalerSDKProxyInfo: ZscalerSDKProxyInfo): OkHttpClient {
        val proxyHost = zscalerSDKProxyInfo.proxyHost
        val proxyPort = zscalerSDKProxyInfo.proxyPort
        val username = zscalerSDKProxyInfo.username
        val password = zscalerSDKProxyInfo.password
        val builder = OkHttpClient.Builder()
        setProxyForHttpRequest(proxyHost, proxyPort)
        if (username?.isNotEmpty() == true && password?.isNotEmpty() == true) {
            val authenticator = Authenticator { _: Route?, response: Response ->
                val credential = Credentials.basic(username, password)
                response.request.newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build()
            }
            builder.proxyAuthenticator(authenticator)
        }
        return builder.build()
    }

    private fun setProxyForHttpRequest(proxyHost: String, proxyPort: Int) {
        ProxySelector.setDefault(object : ProxySelector() {
            override fun select(uri: URI?): MutableList<Proxy> {
                val proxyList = mutableListOf<Proxy>()
                proxyList.add(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
                return proxyList
            }

            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                Log.d(TAG, "connectFailed : proxy connectFailed() with: uri = $uri, SocketAddress = $sa, IOException = ${ioe?.message}")
            }
        })
    }
}

enum class HttpMethod {
    GET, POST, WEB
}