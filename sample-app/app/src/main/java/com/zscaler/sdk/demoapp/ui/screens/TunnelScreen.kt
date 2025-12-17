package com.zscaler.sdk.demoapp.ui.screens

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zscaler.sdk.android.ZscalerSDK
import com.zscaler.sdk.android.notification.ZscalerSDKNotificationEnum
import com.zscaler.sdk.demoapp.viewmodel.TunnelViewModel

data class NotificationItem(val message: String, val timestamp: Long)

@SuppressLint("UnspecifiedRegisterReceiverFlag")
@Composable
fun TunnelScreen(viewModel: TunnelViewModel) {
    val context = LocalContext.current
    var accessKey by remember { mutableStateOf(context.getString(com.zscaler.sdk.demoapp.R.string.zscaler_id)) }
    var accessToken by remember { mutableStateOf(context.getString(com.zscaler.sdk.demoapp.R.string.zscaler_access_token)) }
    var tunnelState by remember { mutableStateOf("OFF") }
    var tunnelType by remember { mutableStateOf("") }
    val tunnelStatus by viewModel.zdkTunnelConnectionStateLiveData.observeAsState("OFF")
    val notifications = remember { mutableStateListOf<NotificationItem>() }

    // Update tunnel state when status changes
    LaunchedEffect(tunnelStatus) {
        tunnelState = tunnelStatus
        val status = ZscalerSDK.status()
        tunnelType = status.tunnelType.toString()
    }
    
    // Continuously poll tunnel type to ensure it stays updated
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000) // Poll every second
            val status = ZscalerSDK.status()
            tunnelType = status.tunnelType.toString()
        }
    }

    // Register broadcast receiver for notifications
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val notificationCode = intent?.getIntExtra(ZscalerSDK.NOTIFICATION_CODE, -1)
                val notificationMessage = intent?.getStringExtra(ZscalerSDK.NOTIFICATION_MESSAGE)
                
                if (notificationCode != null && notificationCode > -1) {
                    val enumNotification = ZscalerSDKNotificationEnum.values()[notificationCode]
                    notifications.add(0, NotificationItem(
                        "${enumNotification.name}\n$notificationMessage",
                        System.currentTimeMillis()
                    ))
                    
                    // Update tunnel type when tunnel state changes
                    if (notificationCode == ZscalerSDKNotificationEnum.ZSCALERSDK_TUNNEL_CONNECTED.ordinal ||
                        notificationCode == ZscalerSDKNotificationEnum.ZSCALERSDK_TUNNEL_DISCONNECTED.ordinal) {
                        val status = ZscalerSDK.status()
                        tunnelType = status.tunnelType.toString()
                    }
                }
            }
        }
        
        val filter = IntentFilter(ZscalerSDK.ZSCALER_RECEIVER_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {

            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Credentials Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "CREDENTIALS",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    
                    OutlinedTextField(
                        value = accessKey,
                        onValueChange = { accessKey = it },
                        label = { Text("Access Key") },
                        modifier = Modifier.fillMaxWidth().testTag("zdk_id_text_field"),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = accessToken,
                        onValueChange = { accessToken = it },
                        label = { Text("Access Token") },
                        modifier = Modifier.fillMaxWidth().testTag("access_token_text_field"),
                        singleLine = true
                    )
                }
            }
        }

        // Tunnel Control Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "TUNNEL CONTROL",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Button(
                        onClick = {
                            if (accessKey.isNotBlank()) {
                                viewModel.startPreLoginTunnel(
                                    appKey = accessKey,
                                    udid = viewModel.getUdid("random_udid"),
                                    onErrorOccurred = { errorCode ->
                                        tunnelState = "ERROR: $errorCode"
                                        viewModel.stopTunnelStatusUpdates()
                                    }
                                )
                                viewModel.startTunnelStatusUpdates()
                                // Immediately update tunnel type
                                val status = ZscalerSDK.status()
                                tunnelType = status.tunnelType.toString()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("pre_login_toggle"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Start Pre-Login Tunnel",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (accessKey.isNotBlank() && accessToken.isNotBlank()) {
                                viewModel.startZeroTrustTunnel(
                                    appKey = accessKey,
                                    accessToken = accessToken,
                                    udid = viewModel.getUdid("random_udid"),
                                    onErrorOccurred = { errorCode ->
                                        tunnelState = "ERROR: $errorCode"
                                        viewModel.stopTunnelStatusUpdates()
                                    }
                                )
                                viewModel.startTunnelStatusUpdates()
                                // Immediately update tunnel type
                                val status = ZscalerSDK.status()
                                tunnelType = status.tunnelType.toString()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("zero_trust_toggle"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Start Zero-Trust Tunnel",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    Button(
                        onClick = {
                            viewModel.stopTunnel { "OFF" }
                            viewModel.stopTunnelStatusUpdates()
                            tunnelState = "OFF"
                            tunnelType = ""
                        },
                        modifier = Modifier.fillMaxWidth().testTag("stop_tunnel_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3B30)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Stop Tunnel",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Status Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "STATUS",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "State:",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = tunnelState,
                            color = if (tunnelState == "ON") Color(0xFF34C759) else Color.Black,
                            modifier = Modifier.testTag("tv_tunnel_status")
                        )
                    }
                    
                    if (tunnelType.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Type:",
                                fontWeight = FontWeight.Medium
                            )
                            Text(text = tunnelType)
                        }
                    }
                }
            }
        }

        // Notifications Section
        if (notifications.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "NOTIFICATIONS",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            items(notifications.take(5)) { notification ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = notification.message,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date(notification.timestamp)),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
