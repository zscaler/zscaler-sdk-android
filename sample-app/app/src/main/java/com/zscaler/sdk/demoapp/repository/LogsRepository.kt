package com.zscaler.sdk.demoapp.repository

import android.content.Context
import android.util.Log
import com.zscaler.sdk.android.ZscalerSDK
import java.io.BufferedReader
import java.io.File

/**
 * Repository for handling log-related operations
 */
class LogsRepository(private val context: Context) {
    private val TAG = "LogsRepository"

    /**
     * Export logs to the specified destination folder
     * @param destinationFolder The folder path where logs will be exported
     * @return The path where logs were exported
     */
    fun exportLogs(destinationFolder: String): String {
        val exportLogDestination = ZscalerSDK.exportLogs(destinationFolder = destinationFolder).toString()
        Log.d(TAG, "exportLogs() called with: destination = $exportLogDestination")
        return exportLogDestination
    }

    /**
     * Clear all SDK logs
     */
    suspend fun clearLogs() {
        ZscalerSDK.clearLogs()
        Log.d(TAG, "clearLogs() called - all logs cleared")
    }

    /**
     * Read event logs from internal storage
     * @return List of event log entries
     */
    fun readEventLogs(): List<String> {
        val eventFile = File(context.filesDir.absolutePath + "/zdk/events.json")
        val content = mutableListOf<String>()

        if (eventFile.exists()) {
            try {
                val bufferedReader = BufferedReader(eventFile.reader())
                bufferedReader.forEachLine { line ->
                    if (line.isNotBlank()) {
                        content.add(line)
                    }
                }
                bufferedReader.close()
                Log.d(TAG, "Read ${content.size} event log entries")
            } catch (e: Exception) {
                Log.e(TAG, "Error reading event logs", e)
                content.add("Error reading logs: ${e.message}")
            }
        } else {
            Log.d(TAG, "Event log file not found")
            content.add("No event logs available yet")
        }
        return content
    }
}
