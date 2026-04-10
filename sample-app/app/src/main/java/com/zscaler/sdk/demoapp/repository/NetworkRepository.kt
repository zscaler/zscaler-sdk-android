package com.zscaler.sdk.demoapp.repository

import android.content.Context
import android.util.Log
import com.zscaler.sdk.android.ZscalerSDK
import com.zscaler.sdk.demoapp.networking.ApiService
import com.zscaler.sdk.demoapp.networking.ParentAppRetrofitClient
import com.zscaler.sdk.demoapp.networking.VolleyClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedInputStream
import java.io.IOException

/**
 * Repository for handling all network operations
 */
class NetworkRepository {
    private val TAG = "NetworkRepository"

    /**
     * Load data using automatic configuration
     */
    suspend fun loadWithAutomaticConfig(url: String): Result<String> {
        return try {
            Log.d(TAG, "loadWithAutomaticConfig() called with: url = $url")
            val apiService = ParentAppRetrofitClient.getRetrofitClient(url)?.create(ApiService::class.java)
            val response = apiService?.getData(url)?.execute()
            
            if (response != null) {
                parseRetrofitResponse(response)
            } else {
                Result.failure(IOException("API service is null"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error in loadWithAutomaticConfig", e)
            Result.failure(e)
        }
    }

    /**
     * Load data using semi-automatic configuration
     */
    suspend fun loadWithSemiAutomaticConfig(url: String, isGetMethod: Boolean): Result<String> {
        return try {
            Log.d(TAG, "loadWithSemiAutomaticConfig() called with: url = $url, method = ${if(isGetMethod) "GET" else "POST"}")
            val apiService = Retrofit.Builder()
                .baseUrl(url)
                .client(ZscalerSDK.setUpOKHttpClientBuilder(null).build())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService::class.java)
            
            val response = if (isGetMethod) {
                apiService?.getData(url)?.execute()
            } else {
                apiService?.postData(url)?.execute()
            }
            
            if (response != null) {
                parseRetrofitResponse(response)
            } else {
                Result.failure(IOException("API service is null"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadWithSemiAutomaticConfig", e)
            Result.failure(e)
        }
    }

    /**
     * Post data with parameters using automatic configuration
     */
    suspend fun loadPostDataWithAutomaticConfig(url: String, params: Map<String, String>): Result<String> {
        return try {
            Log.d(TAG, "postData() called with: url = $url, params = $params")
            val apiService = ParentAppRetrofitClient.getRetrofitClient(url)?.create(ApiService::class.java)
            val response = apiService?.postData(url, params)?.execute()
            
            if (response != null) {
                parseRetrofitResponse(response)
            } else {
                Result.failure(IOException("API service is null"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error in loadPostDataWithAutomaticConfig", e)
            Result.failure(e)
        }
    }

    /**
     * Load data using Volley library
     */
    fun loadGetDataWithVolley(
        appContext: Context,
        url: String,
        onResponse: (Result<String>) -> Unit
    ) {
        // Set up the proxy first, before sending the request.
        ZscalerSDK.setUpOKHttpClientBuilder(null)
        VolleyClient.sendVolleyRequest(
            appContext,
            url,
            onResponse = { response, statusCode ->
                if (statusCode == 200) {
                    onResponse(Result.success(response))
                } else {
                    onResponse(Result.failure(IOException("Error: $statusCode - $response")))
                }
            },
            onError = { error ->
                Log.e(TAG, "Error loadGetDataWithVolley: $error")
                onResponse(Result.failure(IOException("Network error: $error")))
            }
        )
    }

    private fun parseRetrofitResponse(response: Response<ResponseBody>): Result<String> {
        return if (response.isSuccessful && response.body() != null) {
            val responseBody = response.body()!!
            val contentType = responseBody.contentType().toString()

            // Check if content type is application/octet-stream
            if (contentType.contains("application/octet-stream")) {
                handleOctetStreamResponse(responseBody)
            } else {
                handleRegularResponse(responseBody)
            }
        } else {
            handleErrorResponse(response)
        }
    }

    private fun handleOctetStreamResponse(responseBody: ResponseBody): Result<String> {
        return try {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead: Int
            var totalBytesRead: Long = 0

            val inputStream = responseBody.byteStream()
            val bufferedInputStream = BufferedInputStream(inputStream)

            while (bufferedInputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
            }
            
            bufferedInputStream.close()
            inputStream.close()
            
            Result.success("Total Bytes Read: $totalBytesRead")
        } catch (e: IOException) {
            Log.e(TAG, "Error reading octet-stream response", e)
            Result.failure(e)
        }
    }

    private fun handleRegularResponse(responseBody: ResponseBody): Result<String> {
        return try {
            val responseData = responseBody.string()
            Result.success(responseData)
        } catch (e: IOException) {
            Log.e(TAG, "Error reading response", e)
            Result.failure(e)
        }
    }

    private fun handleErrorResponse(response: Response<ResponseBody>): Result<String> {
        val errorBody = response.errorBody()?.string() ?: "Unknown error"
        return Result.failure(IOException("Error: ${response.code()} - $errorBody"))
    }
}
