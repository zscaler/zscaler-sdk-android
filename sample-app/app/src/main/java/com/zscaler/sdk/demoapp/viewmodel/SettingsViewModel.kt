package com.zscaler.sdk.demoapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zscaler.sdk.demoapp.configuration.SettingType
import com.zscaler.sdk.demoapp.configuration.ZscalerSDKSetting

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    fun getSettingValue(settingType: SettingType): Boolean {
        return ZscalerSDKSetting.zscalerSDKConfigurationMap.getOrDefault(settingType, false)
    }
    
    fun updateSetting(settingType: SettingType, value: Boolean) {
        ZscalerSDKSetting.zscalerSDKConfigurationMap[settingType] = value
    }
}
