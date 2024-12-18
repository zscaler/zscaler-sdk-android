package com.zscaler.sdk.demoapp.configuration

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.zscaler.sdk.android.configuration.ZscalerSDKConfiguration.*
import com.zscaler.sdk.demoapp.databinding.ItemSettingBinding
import com.zscaler.sdk.demoapp.databinding.ItemSettingRadioBinding

class ConfigAdapter(
    private val context: Context,
    private val settingsList: MutableList<SettingItem>,
) : RecyclerView.Adapter<ConfigAdapter.ConfigViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TOGGLE = 1
        private const val VIEW_TYPE_RADIO_BUTTONS = 2
    }

    class ConfigViewHolder(val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfigViewHolder {
        return when (viewType) {
            VIEW_TYPE_TOGGLE -> {
                val binding =
                    ItemSettingBinding.inflate(LayoutInflater.from(context), parent, false)
                ConfigViewHolder(binding)
            }

            VIEW_TYPE_RADIO_BUTTONS -> {
                val binding =
                    ItemSettingRadioBinding.inflate(LayoutInflater.from(context), parent, false)
                ConfigViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val settingItem = settingsList[position]
        return if (settingItem.hasToggle) {
            VIEW_TYPE_TOGGLE
        } else {
            VIEW_TYPE_RADIO_BUTTONS
        }
    }

    override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
        val settingItem = settingsList[position]

        when (getItemViewType(position)) {
            VIEW_TYPE_TOGGLE -> {
                val binding = holder.binding as ItemSettingBinding
                binding.title.text = settingItem.title
                binding.description.text = settingItem.description
                binding.toggle.isChecked =
                    ZscalerSDKSetting.zscalerSDKConfigurationMap.getOrDefault(
                        settingItem.type,
                        false
                    )
                binding.toggle.setOnClickListener {
                    ZscalerSDKSetting.zscalerSDKConfigurationMap[settingItem.type] =
                        binding.toggle.isChecked
                }
                binding.toggle.text = settingItem.type.name
            }

            VIEW_TYPE_RADIO_BUTTONS -> {
                val binding = holder.binding as ItemSettingRadioBinding
                when (ZscalerSDKSetting.logLevel) {
                    LogLevel.error -> binding.rbLogLevelError.isChecked = true
                    LogLevel.info -> binding.rbLogLevelInfo.isChecked = true
                    LogLevel.debug -> binding.rbLogLevelDebug.isChecked = true
                }
                binding.rbLogLevelInfo.setOnClickListener { ZscalerSDKSetting.logLevel = LogLevel.info }
                binding.rbLogLevelDebug.setOnClickListener { ZscalerSDKSetting.logLevel = LogLevel.debug }
                binding.rbLogLevelError.setOnClickListener { ZscalerSDKSetting.logLevel = LogLevel.error }
            }
        }
    }

    override fun getItemCount(): Int {
        return settingsList.size
    }
}

