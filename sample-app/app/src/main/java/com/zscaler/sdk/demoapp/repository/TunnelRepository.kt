package com.zscaler.sdk.demoapp.repository

import android.util.Log
import com.zscaler.sdk.android.ZscalerSDK

/**
 * Repository for handling tunnel-related operations
 */
class TunnelRepository {
    private val TAG = "TunnelRepository"

    /**
     * Start Pre-Login tunnel
     */
    suspend fun startPreLoginTunnel(appKey: String, deviceUdid: String) {
        Log.d(TAG, "Starting Pre-Login tunnel")
        ZscalerSDK.startPreLoginTunnel(appKey = appKey, deviceUdid = deviceUdid)
    }

    /**
     * Start Zero Trust tunnel
     */
    suspend fun startZeroTrustTunnel(appKey: String, deviceUdid: String, accessToken: String) {
        Log.d(TAG, "Starting Zero Trust tunnel")
        ZscalerSDK.startZeroTrustTunnel(
            appKey = appKey,
            deviceUdid = deviceUdid,
            accessToken = accessToken
        )
    }

    /**
     * Stop the active tunnel
     */
    suspend fun stopTunnel() {
        Log.d(TAG, "Stopping tunnel")
        val status = ZscalerSDK.status()
        if (status.tunnelConnectionState != "OFF") {
            ZscalerSDK.stopTunnel()
        } else {
            Log.d(TAG, "Tunnel is already stopped, skipping stopTunnel call")
        }
    }

    /**
     * Get current tunnel status
     */
    fun getTunnelStatus() = ZscalerSDK.status()
}
