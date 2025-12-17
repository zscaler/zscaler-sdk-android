package com.zscaler.sdk.demoapp.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zscaler.sdk.demoapp.constants.RequestMethod
import com.zscaler.sdk.demoapp.repository.NetworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RequestViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RequestViewModel"
    
    private val _responseData = MutableLiveData<String>()
    val responseData: LiveData<String>
        get() = _responseData

    private val _url = MutableLiveData<String>("https://google.com")
    val url: LiveData<String>
        get() = _url
    
    private val _selectedMethod = MutableLiveData<RequestMethod>(RequestMethod.GET)
    val selectedMethod: LiveData<RequestMethod>
        get() = _selectedMethod
    
    private val _isWebView = MutableLiveData<Boolean>(false)
    val isWebView: LiveData<Boolean>
        get() = _isWebView
    
    private val networkRepository = NetworkRepository()
    
    fun updateUrl(newUrl: String) {
        _url.value = newUrl
    }
    
    fun updateSelectedMethod(method: RequestMethod) {
        _selectedMethod.value = method
    }
    
    fun updateIsWebView(isWebView: Boolean) {
        _isWebView.value = isWebView
    }

    fun loadWithAutomaticConfig(url: String) {
        Log.d(TAG, "loadWithAutomaticConfig() called with: url = $url")
        viewModelScope.launch(Dispatchers.IO) {
            val result = networkRepository.loadWithAutomaticConfig(url)
            result.onSuccess { response ->
                _responseData.postValue(response)
            }.onFailure { error ->
                _responseData.postValue("Network error: ${error.message}")
            }
        }
    }

    fun loadWithSemiAutomaticConfig(url: String, isGetMethod: Boolean) {
        Log.d(TAG, "loadWithSemiAutomaticConfig() called with: url = $url, method = ${if(isGetMethod) "GET" else "POST"}")
        viewModelScope.launch(Dispatchers.IO) {
            val result = networkRepository.loadWithSemiAutomaticConfig(url, isGetMethod)
            result.onSuccess { response ->
                _responseData.postValue(response)
            }.onFailure { error ->
                _responseData.postValue("Network error: ${error.message}")
            }
        }
    }

    fun loadPostDataWithAutomaticConfig(url: String, params: Map<String, String>) {
        Log.d(TAG, "postData() called with: url = $url, params = $params")
        viewModelScope.launch(Dispatchers.IO) {
            val result = networkRepository.loadPostDataWithAutomaticConfig(url, params)
            result.onSuccess { response ->
                _responseData.postValue(response)
            }.onFailure { error ->
                _responseData.postValue("Network error: ${error.message}")
            }
        }
    }

    /**
     * This API is used to test the Volley Library.
     * Refer docs here for the changes to be done to ensure volley testing can be done.
     * https://confluence.corp.zscaler.com/pages/viewpage.action?pageId=714868625
     */
    fun loadGetDataWithVolley(appContext: Context, url: String) {
        networkRepository.loadGetDataWithVolley(appContext, url) { result ->
            result.onSuccess { response ->
                _responseData.postValue(response)
            }.onFailure { error ->
                _responseData.postValue("Network error: ${error.message}")
            }
        }
    }
}
