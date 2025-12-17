package com.zscaler.sdk.demoapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.zscaler.sdk.android.ZscalerSDK
import com.zscaler.sdk.demoapp.constants.NOTIFICATION_ID

/**
 * A Service that handles notification cancellation-related tasks.
 */
class NotificationCancellationService : Service() {

    private val TAG = "NotificationCancellationService"
    private val CHANNEL_ID = "TunnelStatusChannel"
    private val CHANNEL_NAME = "Tunnel Status Notifications"

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val notificationCode = intent?.getIntExtra(ZscalerSDK.NOTIFICATION_CODE, -1)
            val notificationMessage = intent?.getStringExtra(ZscalerSDK.NOTIFICATION_MESSAGE)
                ?: "No message"

            if (notificationCode != null && notificationCode > -1) {
                showNotification(notificationMessage)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Register broadcast receiver for tunnel status updates from Zscaler SDK
        // Use RECEIVER_EXPORTED because we're receiving broadcasts from the SDK (external component)
        val filter = IntentFilter(ZscalerSDK.ZSCALER_RECEIVER_ID)
        ContextCompat.registerReceiver(
            this,
            notificationReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows tunnel connection status messages"
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(message: String) {
        Log.d(TAG, "Showing notification with message: $message")
        
        // Create a notification with the tunnel status message
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tunnel Status")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Called when the service's task is removed from the recent apps list.
     * Cancels all notifications and stops the service.
     *
     * @param rootIntent The intent that was used to start the task that is being removed.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        stopSelf()
    }

    /**
     * Called when the service is started with startService().
     * This service will not restart automatically if it is killed by the system.
     *
     * @param intent The Intent that was used to start the service.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The start mode for this service, which is START_NOT_STICKY.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra("message") ?: "Tunnel service running"
        showNotification(message)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Receiver not registered")
        }
    }
}