package com.zscaler.sdk.demoapp.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.zscaler.sdk.demoapp.constants.NOTIFICATION_ID


/**
 * A Service that handles notification cancellation-related tasks.
 */
class NotificationCancellationService : Service() {

    private val TAG = "NotificationCancellationService"

    private fun showNotification() {
        val notificationManager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        val existingNotification = notificationManager.activeNotifications.find {
            it.id == NOTIFICATION_ID
        }?.notification

        if (existingNotification != null) {
            Log.d(TAG, "showNotification() called with existingNotification : not null, show notification")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    existingNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, existingNotification)
            }
        } else {
            Log.d(TAG, "showNotification() called with existingNotification : null, do cleanup")
            notificationManager.cancelAll()
            stopSelf()
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
        showNotification()
        return START_NOT_STICKY
    }
}