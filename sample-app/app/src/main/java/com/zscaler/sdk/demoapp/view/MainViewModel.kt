package com.zscaler.sdk.demoapp.view

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zscaler.sdk.android.ZscalerSDK
import com.zscaler.sdk.android.exception.ZscalerSDKException
import com.zscaler.sdk.android.tunnelstatus.ZscalerSDKTunnelStatus
import com.zscaler.sdk.demoapp.constants.ZDKTunnel
import com.zscaler.sdk.demoapp.networking.ApiService
import com.zscaler.sdk.demoapp.networking.ParentAppRetrofitClient
import com.zscaler.sdk.demoapp.networking.VolleyClient
import com.zscaler.sdk.demoapp.repository.SharedPrefsUserRepository
import com.zscaler.sdk.demoapp.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.BufferedInputStream
import java.io.IOException
import java.util.UUID


class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"
    var tunnelOption = mutableStateOf(ZDKTunnel.NO_SELECTION)
    var tunnelConnectionState = mutableStateOf("")
    var zdkTunnelConnectionStateLiveData: MutableLiveData<String> = MutableLiveData()
    private lateinit var zdkStatusLaunch: Job
    private val userRepository: UserRepository = SharedPrefsUserRepository(application)
    private val _responseData = MutableLiveData<String>()
    val responseData: LiveData<String>
        get() = _responseData

    fun exportLog(destination: String): String {
        val exportLogDestination =
            ZscalerSDK.exportLogs(destinationFolder = destination).toString()
        Log.d(TAG, "exportLog() called with: destination = $exportLogDestination")
        return exportLogDestination
    }

    fun clearLogs(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            ZscalerSDK.clearLogs()
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun saveUdid(username: String) {
        userRepository.saveUdid(username)
    }

    fun getUdid(defaultUdid: String): String {
        var udid = userRepository.getUdid()
        if (udid.isNullOrEmpty()) {
            udid = UUID.randomUUID().toString()
            if (udid.isNullOrEmpty()) {
                udid = defaultUdid
            }
            saveUdid(udid)
        }
        return udid
    }

    fun startPreLoginTunnel(appKey: String,
                            udid : String,
                            onErrorOccurred: (errorCode: Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ZscalerSDK.startPreLoginTunnel(appKey = appKey, deviceUdid = udid)
                tunnelConnectionState.value = ZscalerSDK.status().tunnelConnectionState
                setSelectedTunnel(ZDKTunnel.PRELOGIN)
                Log.d(TAG, "startPreLoginTunnel completed")
            } catch (e: Exception) {
                Log.e(TAG, "startPreLoginTunnel() failed with exception :: ${e.message}")
                viewModelScope.launch(Dispatchers.Main) {
                    setSelectedTunnel(ZDKTunnel.NO_SELECTION)
                    stopTunnelStatusUpdates()
                    onErrorOccurred(
                        when (e) {
                            is ZscalerSDKException -> e.errorCode
                            else -> -1
                        }
                    )
                }
            }
        }
    }

    fun startZeroTrustTunnel(
        appKey: String,
        accessToken: String,
        udid : String,
        onErrorOccurred: (errorCode: Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ZscalerSDK.startZeroTrustTunnel(appKey = appKey, deviceUdid = udid, accessToken = accessToken)
                tunnelConnectionState.value = ZscalerSDK.status().tunnelConnectionState
                setSelectedTunnel(ZDKTunnel.ZEROTRUST)
                Log.d(TAG, "startZeroTrustTunnel completed")
            } catch (e: Exception) {
            Log.e(TAG, "startZeroTrustTunnel() failed with exception ${e.message}")
                viewModelScope.launch(Dispatchers.Main) {
                    setSelectedTunnel(ZDKTunnel.NO_SELECTION)
                    stopTunnelStatusUpdates()
                    onErrorOccurred(
                        when (e) {
                            is ZscalerSDKException -> e.errorCode
                            else -> -1
                        }
                    )
                }
            }
        }
    }

    fun stopTunnel(resetStatusText:()-> String): Unit {
        Log.d(TAG, "stopTunnel() called")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val retVal = ZscalerSDK.stopTunnel()
                withContext(Dispatchers.Main) {
                    if (retVal == 0) {
                        zdkTunnelConnectionStateLiveData.value = resetStatusText()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "stopTunnel() failed with exception ${e.message}")
            }
        }
    }

    fun getStatus(): String {
        val zscalerSDKTunnelStatus = ZscalerSDK.status()
        Log.d(TAG, "getStatus() called tunnelType:${zscalerSDKTunnelStatus.tunnelType} status:${zscalerSDKTunnelStatus.tunnelConnectionState}")
        tunnelConnectionState.value = zscalerSDKTunnelStatus.tunnelConnectionState
        return tunnelConnectionState.value
    }

    fun setSelectedTunnel(tunnelOption: ZDKTunnel) {
        this.tunnelOption.value = tunnelOption
    }

    fun getSelectedTunnel(): ZDKTunnel {
        return this.tunnelOption.value
    }

    fun startTunnelStatusUpdates(): String {
        if (::zdkStatusLaunch.isInitialized) {
            zdkStatusLaunch.cancel()
        }
        zdkStatusLaunch = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val status = ZscalerSDK.status()
                tunnelConnectionState.value = status.tunnelConnectionState
                Log.d(TAG, "startPeriodicStatusUpdate() called tunnelType:${status.tunnelType} status:${status.tunnelConnectionState}")
                withContext(Dispatchers.Main) {
                    zdkTunnelConnectionStateLiveData.value = status.tunnelConnectionState
                }
                delay(2000)
            }
        }
        return tunnelConnectionState.value
    }

    fun stopTunnelStatusUpdates() {
        if (::zdkStatusLaunch.isInitialized) {
            zdkStatusLaunch.cancel()
        }
    }

    fun loadWithAutomaticConfig(url: String) {
        Log.d(TAG, "loadWithAutomaticConfig() called with: url = $url")
        val apiService = ParentAppRetrofitClient.getRetrofitClient(url)?.create(ApiService::class.java)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService?.getData(url)?.execute()
                if (response != null) {
                    parseRetrofitResponse(response)
                }
            } catch (e: IOException) {
                _responseData.postValue("Network error")
                e.printStackTrace()
            }
        }
    }

    fun loadWithSemiAutomaticConfig(url: String, method: Boolean) {
        Log.d(TAG, "loadWithSemiAutomaticConfig() called with: url = $url")
        val apiService = ZscalerSDK.setUpClient(null, url)?.create(ApiService::class.java)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = if(method) {
                    apiService?.getData(url)?.execute()
                } else {
                    apiService?.postData(url)?.execute()
                }
                if (response != null) {
                    parseRetrofitResponse(response)
                }
            } catch (e: IOException) {
                _responseData.postValue("Network error")
                e.printStackTrace()
            }
        }
    }

    fun loadPostDataWithAutomaticConfig(url: String, params: Map<String, String>) {
        Log.d(TAG, "postData() called with: url = $url, params = $params")
        val apiService = ParentAppRetrofitClient.getRetrofitClient(url)?.create(ApiService::class.java)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService?.postData(url, params)?.execute()
                if (response != null) {
                    parseRetrofitResponse(response)
                }
            } catch (e: IOException) {
                _responseData.postValue("Network error")
                e.printStackTrace()
            }
        }
    }

    /**
     * This API is used ot test the Volley Library.
     * Refer docs here for the changes to be done to ensure volley testing can be done.
     * https://confluence.corp.zscaler.com/pages/viewpage.action?pageId=714868625
     */
    fun loadGetDataWithVolley(appContext: Context, url:String) {
        //Set up the proxy first, before sending the request.
        ZscalerSDK.setUpClient(null, url)
        VolleyClient.sendVolleyRequest(appContext, url,
            onResponse = { response, statusCode ->
                if (statusCode == 200) {
                    handleVolleyRegularResponse(response)
                } else {
                    handleVolleyErrorResponse(response)
                }
            },
            onError = { error ->
                Log.e(TAG,"Error loadGetDataWithVolley: $error")
                _responseData.postValue("Network error")
            }
        )
    }

    private fun parseRetrofitResponse(response: Response<ResponseBody>) {
        if (response.isSuccessful && response.body() != null) {
            val responseBody = response.body()!!
            val contentType = responseBody.contentType().toString()

            // Check if content type is application/octet-stream
            if (contentType.contains("application/octet-stream")) {
                // Handle application/octet-stream content type
                handleOctetStreamResponse(responseBody)
            } else {
                // For other content types, handle as usual
                handleRegularResponse(responseBody)
            }
        } else {
            // If response is not successful, handle error
            handleErrorResponse(response)
        }
    }

    private fun handleOctetStreamResponse(responseBody: ResponseBody) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead: Int
        var totalBytesRead: Long = 0

        val inputStream = responseBody.byteStream()
        val bufferedInputStream = BufferedInputStream(inputStream)

        try {
            while (bufferedInputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
                _responseData.postValue("Total Bytes Read: $totalBytesRead")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            _responseData.postValue("Error reading octet-stream response: ${e.message}")
        } finally {
            bufferedInputStream.close()
            inputStream.close()
        }
    }
    private fun handleRegularResponse(responseBody: ResponseBody) {
        try {
            val responseData = responseBody.string()
            _responseData.postValue(responseData)
        } catch (e: IOException) {
            e.printStackTrace()
            _responseData.postValue("Error reading response: ${e.message}")
        }
    }

    private fun handleVolleyRegularResponse(response: String) {
        try {
            _responseData.postValue(response)
        } catch (e: IOException) {
            e.printStackTrace()
            _responseData.postValue("Error reading response: ${e.message}")
        }
    }

    private fun handleErrorResponse(response: Response<ResponseBody>) {
        _responseData.postValue(response.errorBody()?.string() ?: "Unknown error")
    }

    private fun handleVolleyErrorResponse(response: String?) {
        _responseData.postValue(response ?: "Unknown error")
    }
}


fun String.ensureEndsWithSlash(): String {
    return if (this.endsWith('/')) {
        this
    } else {
        "$this/"
    }
}