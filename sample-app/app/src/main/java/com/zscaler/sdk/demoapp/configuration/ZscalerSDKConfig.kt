package com.zscaler.sdk.demoapp.configuration

import com.zscaler.sdk.android.configuration.ZscalerSDKConfiguration

enum class SettingType {
    URL_SESSIONS,
    WEB_VIEWS,
    PROXY_AUTHENTICATION,
    BLOCK_ROOT_TRAFFIC,
    BLOCK_ZPA_CONNECTION,
    ENABLE_DEBUG_LOGS,
    LOG_LEVEL,
}

data class SettingItem(
    val type: SettingType,
    val title: String,
    val description: String,
    var isChecked: Boolean,
    var hasToggle: Boolean
)

object ZscalerSDKSetting {
    val zscalerSDKConfigurationMap: MutableMap<SettingType, Boolean> = mutableMapOf()
    var logLevel: ZscalerSDKConfiguration.LogLevel = ZscalerSDKConfiguration.LogLevel.debug

    fun getZscalerSDKConfiguration(): ZscalerSDKConfiguration {
        return ZscalerSDKConfiguration(
            useProxyAuthentication = zscalerSDKConfigurationMap.getOrDefault(
                SettingType.PROXY_AUTHENTICATION,
                false
            ),
            failIfDeviceCompromised = zscalerSDKConfigurationMap.getOrDefault(
                SettingType.BLOCK_ROOT_TRAFFIC,
                false
            ),
            blockZPAConnectionsOnTunnelFailure = zscalerSDKConfigurationMap.getOrDefault(
                SettingType.BLOCK_ZPA_CONNECTION,
                false
            ),
            enableDebugLogsInConsole = zscalerSDKConfigurationMap.getOrDefault(
                SettingType.ENABLE_DEBUG_LOGS,
                false
            ),
            logLevel = logLevel
        )
    }

    fun defaultZscalerSDKConfiguration(): ZscalerSDKConfiguration {
        zscalerSDKConfigurationMap[SettingType.URL_SESSIONS] = false
        zscalerSDKConfigurationMap[SettingType.WEB_VIEWS] = false
        zscalerSDKConfigurationMap[SettingType.BLOCK_ROOT_TRAFFIC] = true
        zscalerSDKConfigurationMap[SettingType.ENABLE_DEBUG_LOGS] = true
        logLevel = ZscalerSDKConfiguration.LogLevel.debug
        return ZscalerSDKConfiguration(
            failIfDeviceCompromised = true,
            enableDebugLogsInConsole = true,
            logLevel = ZscalerSDKConfiguration.LogLevel.debug
        )
    }
}