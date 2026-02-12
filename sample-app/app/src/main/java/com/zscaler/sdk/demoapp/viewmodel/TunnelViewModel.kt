package com.zscaler.sdk.demoapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zscaler.sdk.android.ZscalerSDK
import com.zscaler.sdk.android.exception.ZscalerSDKException
import com.zscaler.sdk.demoapp.constants.ZDKTunnel
import com.zscaler.sdk.demoapp.repository.SharedPrefsUserRepository
import com.zscaler.sdk.demoapp.repository.TunnelRepository
import com.zscaler.sdk.demoapp.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TunnelViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "TunnelViewModel"
    
    var tunnelOption = mutableStateOf(ZDKTunnel.NO_SELECTION)
    var tunnelConnectionState = mutableStateOf("")
    var zdkTunnelConnectionStateLiveData: MutableLiveData<String> = MutableLiveData()
    
    // Persisted credentials
    var accessKey = mutableStateOf(application.getString(com.zscaler.sdk.demoapp.R.string.zscaler_id))
    var accessToken = mutableStateOf(application.getString(com.zscaler.sdk.demoapp.R.string.zscaler_access_token))
    
    private lateinit var zdkStatusLaunch: Job
    private val userRepository: UserRepository = SharedPrefsUserRepository(application)
    private val tunnelRepository: TunnelRepository = TunnelRepository()

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

    fun startPreLoginTunnel(
        appKey: String,
        udid: String,
        onErrorOccurred: (errorCode: Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tunnelRepository.startPreLoginTunnel(appKey = appKey, deviceUdid = udid)
                val status = tunnelRepository.getTunnelStatus()
                tunnelConnectionState.value = status.tunnelConnectionState
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
        udid: String,
        onErrorOccurred: (errorCode: Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tunnelRepository.startZeroTrustTunnel(
                    appKey = appKey,
                    deviceUdid = udid,
                    accessToken = accessToken
                )
                val status = tunnelRepository.getTunnelStatus()
                tunnelConnectionState.value = status.tunnelConnectionState
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

    fun stopTunnel(resetStatusText: () -> String) {
        Log.d(TAG, "stopTunnel() called")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tunnelRepository.stopTunnel()
                withContext(Dispatchers.Main) {
                    zdkTunnelConnectionStateLiveData.value = resetStatusText()
                }
            } catch (e: Exception) {
                Log.e(TAG, "stopTunnel() failed with exception ${e.message}")
            }
        }
    }

    fun getStatus(): String {
        val zscalerSDKTunnelStatus = tunnelRepository.getTunnelStatus()
        Log.d(
            TAG,
            "getStatus() called tunnelType:${zscalerSDKTunnelStatus.tunnelType} status:${zscalerSDKTunnelStatus.tunnelConnectionState}"
        )
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
                val status = tunnelRepository.getTunnelStatus()
                tunnelConnectionState.value = status.tunnelConnectionState
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
}
