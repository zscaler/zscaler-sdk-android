package com.zscaler.sdk.demoapp.networking

import android.content.Context
import com.android.volley.NetworkResponse
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

object VolleyClient {

    fun sendVolleyRequest(appContext: Context, url: String, onResponse: (String, Int) -> Unit, onError: (String) -> Unit) {
        val proxyHurlStack = ProxyHurlStack()
        val requestQueue: RequestQueue = Volley.newRequestQueue(appContext, proxyHurlStack)

        val stringRequest = object: StringRequest(
            Method.GET, url,
            {
            },
            { error ->
                onError(error.message ?: "Unknown error")
            }
        ){
            override fun parseNetworkResponse(response: NetworkResponse?): com.android.volley.Response<String> {
                val statusCode = response?.statusCode ?: -1
                return super.parseNetworkResponse(response).also {
                    it.result?.let { res ->
                        onResponse(res, statusCode)
                    }
                }
            }
        }

        requestQueue.add(stringRequest)
    }

}