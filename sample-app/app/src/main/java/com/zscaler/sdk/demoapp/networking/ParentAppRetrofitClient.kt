package com.zscaler.sdk.demoapp.networking

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url


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
object ParentAppRetrofitClient {
    private var retrofit: Retrofit? = null
    private var baseUrl: String = ""

    fun getRetrofitClient(baseUrl: String): Retrofit? {
        if (retrofit == null || ParentAppRetrofitClient.baseUrl != baseUrl) {
            ParentAppRetrofitClient.baseUrl = baseUrl
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit
    }

    fun clearRetroFitInstance() {
        retrofit = null
    }
}