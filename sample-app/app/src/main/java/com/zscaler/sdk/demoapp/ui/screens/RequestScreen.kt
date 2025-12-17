package com.zscaler.sdk.demoapp.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zscaler.sdk.android.ZscalerSDK
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
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        containerColor = Color(0xFF007AFF),
                        contentColor = Color.White
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                WebViewClientWithProxyAuthSupport(com.zscaler.sdk.android.networking.ZscalerSDKProxyInfo("", 0, "", ""))
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
                        color = if (isError) Color(0xFFFF3B30) else Color.Black,
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
                            color = Color.Gray,
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
            containerColor = if (isSelected) Color(0xFFE0E0E0) else Color.White,
            contentColor = Color.Black
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
