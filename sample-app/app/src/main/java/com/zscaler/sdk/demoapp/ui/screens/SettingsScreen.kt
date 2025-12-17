package com.zscaler.sdk.demoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zscaler.sdk.demoapp.configuration.SettingType
import com.zscaler.sdk.demoapp.viewmodel.SettingsViewModel

data class SettingItem(
    val type: SettingType,
    val title: String,
    val description: String,
    val isChecked: Boolean
)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings = remember {
        mutableStateListOf(
            SettingItem(
                type = SettingType.URL_SESSIONS,
                title = "URLSessions",
                description = "Should automatically configure URLSessions",
                isChecked = viewModel.getSettingValue(SettingType.URL_SESSIONS)
            ),
            SettingItem(
                type = SettingType.WEB_VIEWS,
                title = "WebViews",
                description = "Should automatically configure WebViews",
                isChecked = viewModel.getSettingValue(SettingType.WEB_VIEWS)
            ),
            SettingItem(
                type = SettingType.ENABLE_DEBUG_LOGS,
                title = "Enable logs in console",
                description = "Enable and change different log levels",
                isChecked = viewModel.getSettingValue(SettingType.ENABLE_DEBUG_LOGS)
            ),
            SettingItem(
                type = SettingType.PROXY_AUTHENTICATION,
                title = "Proxy Authentication",
                description = "Use proxy authentication",
                isChecked = viewModel.getSettingValue(SettingType.PROXY_AUTHENTICATION)
            ),
            SettingItem(
                type = SettingType.BLOCK_ROOT_TRAFFIC,
                title = "Block JB Traffic",
                description = "Block traffic if device is Jail Broken",
                isChecked = viewModel.getSettingValue(SettingType.BLOCK_ROOT_TRAFFIC)
            ),
            SettingItem(
                type = SettingType.BLOCK_ZPA_CONNECTION,
                title = "Block Connection",
                description = "Block connection on failure",
                isChecked = viewModel.getSettingValue(SettingType.BLOCK_ZPA_CONNECTION)
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(settings) { setting ->
            SettingItemCard(
                setting = setting,
                onCheckedChange = { isChecked ->
                    viewModel.updateSetting(setting.type, isChecked)
                    val index = settings.indexOfFirst { it.type == setting.type }
                    if (index != -1) {
                        settings[index] = setting.copy(isChecked = isChecked)
                    }
                }
            )
        }
    }
}

@Composable
fun SettingItemCard(
    setting: SettingItem,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = setting.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = setting.description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Switch(
                checked = setting.isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF34C759),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }
    }
}
