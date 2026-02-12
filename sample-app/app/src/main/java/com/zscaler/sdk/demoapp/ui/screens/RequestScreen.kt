package com.zscaler.sdk.demoapp.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zscaler.sdk.android.ZscalerSDK
import com.zscaler.sdk.android.networking.ZscalerSDKProxyInfo
import com.zscaler.sdk.demoapp.constants.RequestMethod
import com.zscaler.sdk.demoapp.util.ProxyUtility
import com.zscaler.sdk.demoapp.util.WebViewClientWithProxyAuthSupport
import com.zscaler.sdk.demoapp.viewmodel.RequestViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RequestScreen(viewModel: RequestViewModel) {
    val selectedMethod by viewModel.selectedMethod.observeAsState(RequestMethod.GET)
    val url by viewModel.url.observeAsState("https://google.com")
    val isWebView by viewModel.isWebView.observeAsState(false)
    val responseData by viewModel.responseData.observeAsState("")
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabButton(
                    text = "GET",
                    isSelected = selectedMethod == RequestMethod.GET && !isWebView,
                    onClick = {
                        viewModel.updateSelectedMethod(RequestMethod.GET)
                        viewModel.updateIsWebView(false)
                    }
                )
                TabButton(
                    text = "POST",
                    isSelected = selectedMethod == RequestMethod.POST && !isWebView,
                    onClick = {
                        viewModel.updateSelectedMethod(RequestMethod.POST)
                        viewModel.updateIsWebView(false)
                    }
                )
                TabButton(
                    text = "WEB",
                    isSelected = isWebView,
                    onClick = {
                        viewModel.updateIsWebView(true)
                    },
                    modifier = Modifier.testTag("web_tab_button")
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { viewModel.updateUrl(it) },
                    modifier = Modifier.weight(1f).testTag("browser_url_text_field"),
                    singleLine = true,
                    placeholder = { Text("Enter URL") }
                )
                
                IconButton(
                    onClick = {
                        if (url.isNotBlank()) {
                            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                "https://$url"
                            } else {
                                url
                            }
                            
                            if (isWebView) {
                                webView?.loadUrl(formattedUrl)
                            } else {
                                when (selectedMethod) {
                                    RequestMethod.GET -> {
                                        viewModel.loadWithSemiAutomaticConfig(formattedUrl, true)
                                    }
                                    RequestMethod.POST -> {
                                        viewModel.loadWithSemiAutomaticConfig(formattedUrl, false)
                                    }
                                    else -> {}
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("go_button"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Request"
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (isWebView) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            
                            // Get proxy info from ZscalerSDK
                            val proxyInfo = ZscalerSDK.proxyInfo()
                            
                            // Set WebView proxy settings for manual mode
                            // This routes WebView traffic through the tunnel
                            proxyInfo?.let {
                                ProxyUtility.setWebViewProxy(it)
                            }
                            
                            // Set custom WebViewClient to handle proxy auth and prevent external browser redirects
                            webViewClient = if (proxyInfo != null) {
                                WebViewClientWithProxyAuthSupport(proxyInfo)
                            } else {
                                WebViewClientWithProxyAuthSupport(
                                    ZscalerSDKProxyInfo(
                                        "",
                                        0,
                                        "",
                                        ""
                                    )
                                )
                            }
                            
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                if (responseData.isNotEmpty()) {
                    val scrollState = rememberScrollState()
                    val isError = responseData.contains("error", ignoreCase = true) ||
                                 responseData.contains("failed", ignoreCase = true)
                    
                    Text(
                        text = responseData,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Enter URL and tap Send",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
            else 
                MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        modifier = modifier
            .height(40.dp)
            .widthIn(min = 80.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}
