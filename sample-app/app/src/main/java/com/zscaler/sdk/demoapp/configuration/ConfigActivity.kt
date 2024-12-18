package com.zscaler.sdk.demoapp.configuration

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.zscaler.sdk.android.ZscalerSDK
import com.zscaler.sdk.android.exception.ZscalerSDKException
import com.zscaler.sdk.demoapp.R
import com.zscaler.sdk.demoapp.databinding.ActivitySettingBinding
import com.zscaler.sdk.demoapp.networking.ParentAppRetrofitClient

class ConfigActivity : AppCompatActivity() {
    private val TAG = "SettingActivity"
    private lateinit var binding: ActivitySettingBinding
    private lateinit var settingsList: MutableList<SettingItem>
    private var zscalerSDKConfigurationMap = mutableMapOf<SettingType, Boolean>()
    private lateinit var settingsAdapter: ConfigAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initZscalerConfigOption()
    }

    private fun initZscalerConfigOption() {
        settingsList = mutableListOf(
            SettingItem(
                SettingType.URL_SESSIONS,
                getString(R.string.setting_title_urlsessions),
                getString(R.string.setting_desc_urlsessions),
                zscalerSDKConfigurationMap.getOrDefault(SettingType.URL_SESSIONS, false), true
            ),
            SettingItem(
                SettingType.WEB_VIEWS,
                getString(R.string.setting_title_webviews),
                getString(R.string.setting_desc_webviews),
                zscalerSDKConfigurationMap.getOrDefault(SettingType.WEB_VIEWS, false), true
            ),
            SettingItem(
                SettingType.PROXY_AUTHENTICATION,
                getString(R.string.setting_title_proxy_authentication),
                getString(R.string.setting_desc_proxy_authentication),
                zscalerSDKConfigurationMap.getOrDefault(SettingType.PROXY_AUTHENTICATION, false),
                true
            ),
            SettingItem(
                SettingType.BLOCK_ROOT_TRAFFIC,
                getString(R.string.setting_title_block_root_traffic),
                getString(R.string.setting_desc_block_root_traffic),
                zscalerSDKConfigurationMap.getOrDefault(SettingType.BLOCK_ROOT_TRAFFIC, false), true
            ),
            SettingItem(
                SettingType.BLOCK_ZPA_CONNECTION,
                getString(R.string.setting_title_block_zpa_connection),
                getString(R.string.setting_desc_block_zpa_connection),
                zscalerSDKConfigurationMap.getOrDefault(SettingType.BLOCK_ZPA_CONNECTION, false),
                true
            ),
            SettingItem(
                SettingType.ENABLE_DEBUG_LOGS,
                getString(R.string.setting_title_enable_log),
                getString(R.string.setting_desc_enable_log),
                zscalerSDKConfigurationMap.getOrDefault(SettingType.ENABLE_DEBUG_LOGS, false),
                true
            ),
            SettingItem(
                SettingType.LOG_LEVEL,
                getString(R.string.setting_title_enable_log),
                getString(R.string.setting_desc_enable_log),
                zscalerSDKConfigurationMap.getOrDefault(SettingType.LOG_LEVEL, false),
                false
            )
        )
        settingsAdapter = ConfigAdapter(this, settingsList)
        binding.settingsRecycleView.layoutManager = LinearLayoutManager(this)
        binding.settingsRecycleView.adapter = settingsAdapter
        binding.tvSettingDone.setOnClickListener {
            ParentAppRetrofitClient.clearRetroFitInstance()
            try {
                ZscalerSDK.setConfiguration(ZscalerSDKSetting.getZscalerSDKConfiguration())
            } catch (exception: ZscalerSDKException) {
                Log.e(TAG, "Got exception while setting configuration = $exception")
            }
            onBackPressedDispatcher.onBackPressed()
        }
    }
}