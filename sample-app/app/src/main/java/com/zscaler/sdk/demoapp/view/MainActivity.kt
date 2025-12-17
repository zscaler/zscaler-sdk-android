package com.zscaler.sdk.demoapp.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.zscaler.sdk.android.ZscalerSDK
import com.zscaler.sdk.android.exception.ZscalerSDKException
import com.zscaler.sdk.demoapp.BuildConfig
import com.zscaler.sdk.demoapp.configuration.ZscalerSDKSetting
import com.zscaler.sdk.demoapp.ui.screens.LogsScreen
import com.zscaler.sdk.demoapp.ui.screens.RequestScreen
import com.zscaler.sdk.demoapp.ui.screens.SettingsScreen
import com.zscaler.sdk.demoapp.ui.screens.TunnelScreen
import com.zscaler.sdk.demoapp.ui.theme.ZDKTestAppTheme
import com.zscaler.sdk.demoapp.viewmodel.LogsViewModel
import com.zscaler.sdk.demoapp.viewmodel.RequestViewModel
import com.zscaler.sdk.demoapp.viewmodel.SettingsViewModel
import com.zscaler.sdk.demoapp.viewmodel.TunnelViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Tunnel : Screen("tunnel", "Tunnel", Icons.Default.Lock)
    object Request : Screen("request", "Request", Icons.Default.Language)
    object Logs : Screen("logs", "Logs", Icons.Default.Description)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private lateinit var tunnelViewModel: TunnelViewModel
    private lateinit var requestViewModel: RequestViewModel
    private lateinit var logsViewModel: LogsViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private val requestNotificationCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initCrashlytics()

        try {
            Log.d(TAG, "STAT:: ZscalerSDK init Start")
            ZscalerSDK.init(application, ZscalerSDKSetting.defaultZscalerSDKConfiguration())
            Log.d(TAG, "STAT:: ZscalerSDK init Done")
        } catch (e: ZscalerSDKException) {
            Log.e(TAG, "Got exception while initializing ZscalerSDK = $e")
            return
        }

        val viewModelProvider = ViewModelProvider(this)
        tunnelViewModel = viewModelProvider[TunnelViewModel::class.java]
        requestViewModel = viewModelProvider[RequestViewModel::class.java]
        logsViewModel = viewModelProvider[LogsViewModel::class.java]
        settingsViewModel = viewModelProvider[SettingsViewModel::class.java]

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission()
            }
        }

        setContent {
            ZDKTestAppTheme {
                MainScreen(
                    tunnelViewModel = tunnelViewModel,
                    requestViewModel = requestViewModel,
                    logsViewModel = logsViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    /**
     * Enabling Firebase crashlytics only build variant other than debug.
     */
    private fun initCrashlytics() {
        Firebase.initialize(this)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestNotificationCode)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    tunnelViewModel: TunnelViewModel,
    requestViewModel: RequestViewModel,
    logsViewModel: LogsViewModel,
    settingsViewModel: SettingsViewModel
) {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Tunnel) }
    val screens = listOf(
        Screen.Tunnel,
        Screen.Request,
        Screen.Logs,
        Screen.Settings
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedScreen.title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF007AFF),
                            selectedTextColor = Color(0xFF007AFF),
                            indicatorColor = Color(0xFFE3F2FD),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedScreen) {
                is Screen.Tunnel -> TunnelScreen(viewModel = tunnelViewModel)
                is Screen.Request -> RequestScreen(viewModel = requestViewModel)
                is Screen.Logs -> LogsScreen(viewModel = logsViewModel)
                is Screen.Settings -> SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
