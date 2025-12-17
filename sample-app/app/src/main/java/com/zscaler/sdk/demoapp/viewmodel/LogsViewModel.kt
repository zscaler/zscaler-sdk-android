package com.zscaler.sdk.demoapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zscaler.sdk.demoapp.repository.LogsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "LogsViewModel"
    private val logsRepository = LogsRepository(application.applicationContext)

    private val _eventLogs = MutableStateFlow<List<String>>(emptyList())
    val eventLogs: StateFlow<List<String>> = _eventLogs

    fun exportLog(destination: String): String {
        val exportLogDestination = logsRepository.exportLogs(destination)
        Log.d(TAG, "exportLog() called with: destination = $exportLogDestination")
        return exportLogDestination
    }

    fun clearLogs(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            logsRepository.clearLogs()
            withContext(Dispatchers.Main) {
                onSuccess()
                loadEventLogs() // Refresh after clearing
            }
        }
    }

    fun loadEventLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val logs = logsRepository.readEventLogs()
            _eventLogs.value = logs
        }
    }
}
