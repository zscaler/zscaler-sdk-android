package com.zscaler.sdk.demoapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.zscaler.sdk.android.ZscalerSDK
import com.zscaler.sdk.android.exception.ZscalerSDKException
import com.zscaler.sdk.demoapp.configuration.SettingType
import com.zscaler.sdk.demoapp.configuration.ZscalerSDKSetting

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SettingsViewModel"
    
    fun getSettingValue(settingType: SettingType): Boolean {
        return ZscalerSDKSetting.zscalerSDKConfigurationMap.getOrDefault(settingType, false)
    }
    
    fun updateSetting(settingType: SettingType, value: Boolean) {
        ZscalerSDKSetting.zscalerSDKConfigurationMap[settingType] = value
        
        try {
            ZscalerSDK.setConfiguration(ZscalerSDKSetting.getZscalerSDKConfiguration())
            Log.d(TAG, "Configuration updated successfully for $settingType = $value")
        } catch (exception: ZscalerSDKException) {
            Log.e(TAG, "Got exception while setting configuration = $exception")
        }
    }
}
