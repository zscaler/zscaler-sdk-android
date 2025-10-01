package com.zscaler.sdk.demoapp.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.zscaler.sdk.android.ZscalerSDK
import com.zscaler.sdk.android.configuration.ZscalerSDKConfiguration
import com.zscaler.sdk.android.exception.ZscalerSDKException
import com.zscaler.sdk.android.notification.ZscalerSDKNotificationEnum
import com.zscaler.sdk.android.tunnelstatus.ZscalerSDKTunnelType
import com.zscaler.sdk.demoapp.BuildConfig
import com.zscaler.sdk.demoapp.R
import com.zscaler.sdk.demoapp.configuration.ConfigActivity
import com.zscaler.sdk.demoapp.configuration.ZscalerSDKSetting
import com.zscaler.sdk.demoapp.constants.NOTIFICATION_CHANNEL_ID
import com.zscaler.sdk.demoapp.constants.NOTIFICATION_ID
import com.zscaler.sdk.demoapp.constants.RequestMethod
import com.zscaler.sdk.demoapp.constants.ZDKTunnel
import com.zscaler.sdk.demoapp.constants.zpaEmptyHtml
import com.zscaler.sdk.demoapp.constants.zpaNotConnectedHtml
import com.zscaler.sdk.demoapp.databinding.ActivityMainBinding
import com.zscaler.sdk.demoapp.networking.ParentAppRetrofitClient
import com.zscaler.sdk.demoapp.service.NotificationCancellationService
import com.zscaler.sdk.demoapp.util.ProxyUtility
import com.zscaler.sdk.demoapp.util.WebViewClientWithProxyAuthSupport
import com.zscaler.sdk.demoapp.util.ZipUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var logsZipFileName: String
    private lateinit var mainViewModel: MainViewModel
    private var isRetrofitClientNeeded: Boolean = false
    private var requestMethod: RequestMethod = RequestMethod.WEB
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var notificationManager: NotificationManager
    private val requestNotificationCode = 101
    private lateinit var sdkConfiguration: ZscalerSDKConfiguration
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initCrashlytics()
        binding = ActivityMainBinding.inflate(layoutInflater)
        try {
            ZscalerSDK.init(application, ZscalerSDKSetting.defaultZscalerSDKConfiguration())
        } catch (e: ZscalerSDKException) {
            Log.e(TAG, "Got exception while initializing ZscalerSDK = $e")
            // App won't work after this
            return
        }
        val viewModelProvider = ViewModelProvider(this)
        mainViewModel = viewModelProvider[MainViewModel::class.java]
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        setContentView(binding.root)
        sdkConfiguration = ZscalerSDKSetting.getZscalerSDKConfiguration()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.preLoginToggle.contentDescription = "OFF"
        binding.zeroTrustToggle.contentDescription = "OFF"
        addLoggerFunction()
        configureTunnelToggleButtons()
        enableBrowsing()
        spinnerListener()
        createNotificationChannel()
        initZdkConfiguration()
        addEventLogsToUi()
    }

    override fun onStart() {
        sdkConfiguration = ZscalerSDKSetting.getZscalerSDKConfiguration()
        // check and apply manual web-view setting if coming from config screen, when tunnel is on
        if (ZscalerSDK.status().tunnelConnectionState == "ON") {
            applyWebViewProxySettings()
        }
        super.onStart()
    }

    private fun initZdkConfiguration() {
        binding.llZscalerConfig.setOnClickListener { startActivity(Intent(this@MainActivity, ConfigActivity::class.java)) }
    }

    private fun addEventLogsToUi() {
        binding.llEventLogs.setOnClickListener { startActivity(Intent(this@MainActivity, EventLogViewActivity::class.java)) }
    }

    /**
     * Enabling Firebase crashlytics only build variant other than debug.
     */
    private fun initCrashlytics() {
        Firebase.initialize(this)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    private fun unregisterZDKReceiver() {
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: IllegalArgumentException) {
            //do nothing if receiver is not already registered
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerZDKReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission()
        }
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val notificationCode = intent?.getIntExtra(ZscalerSDK.NOTIFICATION_CODE, -1)
                val notificationMessage = intent?.getStringExtra(ZscalerSDK.NOTIFICATION_MESSAGE)
                Log.d(TAG, "onReceive() called with: notificationCode = $notificationCode, notificationMessage = $notificationMessage")
                notificationCode?.takeIf() {it > -1
                    if (notificationCode == ZscalerSDKNotificationEnum.ZSCALERSDK_TUNNEL_CONNECTED.ordinal ||
                        notificationCode == ZscalerSDKNotificationEnum.ZSCALERSDK_TUNNEL_DISCONNECTED.ordinal) {
                        ParentAppRetrofitClient.clearRetroFitInstance()
                    }
                    if (notificationCode == ZscalerSDKNotificationEnum.ZSCALERSDK_TUNNEL_DISCONNECTED.ordinal) {
                        unregisterZDKReceiver()
                    }
                    createNotification(ZscalerSDKNotificationEnum.values()[notificationCode].message, ZscalerSDKNotificationEnum.values()[notificationCode].message)
                }
            }
        }
        val filter = IntentFilter(ZscalerSDK.ZSCALER_RECEIVER_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, filter)
        }
    }

    private fun spinnerListener() {
        binding.httpMethodButton.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>?,
                    selectedItemView: View?,
                    position: Int,
                    id: Long
                ) {
                    isRetrofitClientNeeded = when (position) {
                        1, 2 -> {
                            requestMethod = RequestMethod.entries.toTypedArray()[position.minus(1)]
                            true
                        }

                        else -> {
                            requestMethod = RequestMethod.WEB
                            false
                        }
                    }
                }

                override fun onNothingSelected(parentView: AdapterView<*>?) {
                    // Do nothing here
                }
            }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        if (mainViewModel.getSelectedTunnel() == ZDKTunnel.PRELOGIN) {
            mainViewModel.stopTunnel(resetStatusText = { getString(R.string.status_off) })
        } else if (mainViewModel.getSelectedTunnel() == ZDKTunnel.ZEROTRUST) {
            mainViewModel.stopTunnel(resetStatusText = { getString(R.string.status_off) })
        }
        ParentAppRetrofitClient.clearRetroFitInstance()
        try {
            unregisterZDKReceiver()
        } catch (e: RuntimeException) {
            Log.e(TAG, "onDestroy() :: exception raised while unregistering broadcastReceiver : ", e)
        }
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun enableBrowsing() {
        binding.webview.settings.javaScriptEnabled = true
        binding.webview.webViewClient = DefaultWebViewClient()
        addMessageOnWebView(zpaNotConnectedHtml)
        binding.goButton.setOnClickListener {
            var url = binding.browserUrlTextField.text.toString()
            val urlRegex =
                "^((http|https)://)?(www\\.)?([a-zA-Z0-9\\-\\.]+\\.)+[a-zA-Z]{2,}(/?.*)?\$"
            if (url.isBlank() || !url.matches(urlRegex.toRegex())) {
                ZdkDialog.showMessageDialog(this, getString(R.string.enter_valid_url))
            } else {
                url = addHttpsIfNeeded(url)
                when (requestMethod) {
                    RequestMethod.GET ->
                        mainViewModel.loadWithSemiAutomaticConfig(url.ensureEndsWithSlash(), true)
                    RequestMethod.POST ->
                        mainViewModel.loadWithSemiAutomaticConfig(url.ensureEndsWithSlash(), false)
                    RequestMethod.WEB ->
                        loadURLInWebView(url)
                }
            }
        }
        mainViewModel.responseData.observe(this) { responseData ->
            binding.webview.loadUrl("about:blank")
            binding.webview.clearCache(true)
            if (requestMethod != RequestMethod.WEB) {
                binding.webview.visibility = View.GONE
                binding.tvResponse.visibility = View.VISIBLE
                if (responseData != null) {
                    if (responseData.toString()
                            .startsWith("<!DOCTYPE html>") || responseData.toString()
                            .startsWith("<!doctype html>")
                    ) {
                        binding.tvResponse.text = HtmlCompat.fromHtml(responseData, 0)
                    } else {
                        binding.tvResponse.text = responseData
                    }
                    Log.d(TAG, "API data: responseData = $responseData")
                } else {
                    binding.tvResponse.text = getString(R.string.error_loading_data)
                    Log.d(TAG, "API response error")
                }
            }
        }
    }

    private fun loadURLInWebView(url: String) {
        binding.tvResponse.visibility = View.GONE
        binding.webview.visibility = View.VISIBLE
        var formattedUrl = url.trim()
        if (!formattedUrl.startsWith("http://")
            && !formattedUrl.startsWith("https://")
            && !formattedUrl.startsWith("www.")
        ) {
            formattedUrl = "https://$formattedUrl"
        }

        // Clear caches for testing purpose before loading a url
        val webSettings: WebSettings = binding.webview.settings
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.domStorageEnabled = false
        binding.webview.clearCache(true)

        binding.webview.loadUrl(formattedUrl)
    }

    private fun addMessageOnWebView(htmlText: String) {
        if (mainViewModel.getSelectedTunnel() == ZDKTunnel.NO_SELECTION) {
            val encodedHtml = Base64.encodeToString(htmlText.toByteArray(), Base64.NO_PADDING)
            binding.webview.loadData(encodedHtml, "text/html", "base64")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun configureTunnelToggleButtons() {
        binding.preLoginToggle.setOnClickListener { buttonView->
            val toggleButton = buttonView as SwitchCompat
            if (toggleButton.isChecked) {
                // everytime clear any previously manually created retrofit when tunnel status changes.
                ParentAppRetrofitClient.clearRetroFitInstance()
                registerZDKReceiver()
                binding.tvPreStatus.text = getString(R.string.status_format, "CONNECTING")

                binding.tvZeroStatus.visibility = View.INVISIBLE

                val appKey = binding.zdkIdTextField.text.toString()
                if (appKey.isBlank()) {
                    binding.zdkIdTextField.error = getString(R.string.zdk_id_is_empty)
                    binding.preLoginToggle.isChecked = false
                    binding.preLoginToggle.contentDescription = "OFF"
                    return@setOnClickListener

                } else {
                    binding.tvPreStatus.visibility = View.VISIBLE
                    binding.zdkIdTextField.error = null
                    binding.zeroTrustToggle.isChecked = false
                    binding.preLoginToggle.isChecked = true
                    mainViewModel.startPreLoginTunnel(appKey = appKey,
                        udid = mainViewModel.getUdid(getString(R.string.random_udid)),
                        onErrorOccurred = {
                            clearWebViewProxySettings()
                            binding.preLoginToggle.isChecked = false
                            binding.preLoginToggle.contentDescription = "OFF"
                            binding.tvPreStatus.visibility = View.VISIBLE
                            binding.tvPreStatus.text =
                                getString(R.string.error_status_format, it.toString())
                            mainViewModel.zdkTunnelConnectionStateLiveData.removeObservers(this)
                        })
                    applyWebViewProxySettings()
                    binding.preLoginToggle.contentDescription = "ON"
                    binding.zeroTrustToggle.contentDescription = "OFF"
                    mainViewModel.startTunnelStatusUpdates()
                    binding.tvPreStatus.text =
                        getString(R.string.status_format, mainViewModel.getStatus())
                    binding.tvZeroStatus.text = getString(R.string.status_format, "OFF")
                    mainViewModel.zdkTunnelConnectionStateLiveData.observe(this) {
                        binding.tvPreStatus.text = getString(R.string.status_format, it)
                    }
                    addMessageOnWebView(zpaEmptyHtml)
                }
            } else {
                clearWebViewProxySettings()
                unregisterZDKReceiver()
                mainViewModel.stopTunnel(resetStatusText = { getString(R.string.status_off) })
                binding.preLoginToggle.contentDescription = "OFF"
                binding.zeroTrustToggle.contentDescription = "OFF"
                mainViewModel.stopTunnelStatusUpdates()
                binding.tvPreStatus.text = getString(R.string.status_format, "OFF")
                binding.tvPreStatus.visibility = View.INVISIBLE
                addMessageOnWebView(zpaNotConnectedHtml)
            }
        }

        binding.zeroTrustToggle.setOnClickListener { buttonView->
            val toggleButton = buttonView as SwitchCompat
            if (toggleButton.isChecked) {
                // everytime clear any previously created retrofit when tunnel status changes.
                ParentAppRetrofitClient.clearRetroFitInstance()
                registerZDKReceiver()
                binding.tvZeroStatus.text = getString(R.string.status_format, "CONNECTING")
                binding.tvPreStatus.visibility = View.INVISIBLE

                val appKey = binding.zdkIdTextField.text.toString()
                val accessToken = binding.accessTokenTextField.text.toString()
                if (appKey.isBlank()) {
                    binding.zdkIdTextField.error = getString(R.string.zdk_id_is_empty)
                    binding.zeroTrustToggle.isChecked = false
                    binding.zeroTrustToggle.contentDescription = "OFF"
                    return@setOnClickListener
                } else if (accessToken.isBlank()) {
                    binding.accessTokenTextField.error = getString(R.string.access_token_is_empty)
                    binding.zeroTrustToggle.isChecked = false
                    binding.zeroTrustToggle.contentDescription = "OFF"
                    return@setOnClickListener
                } else {
                    binding.tvZeroStatus.visibility = View.VISIBLE
                    binding.zdkIdTextField.error = null
                    binding.accessTokenTextField.error = null
                    mainViewModel.startZeroTrustTunnel(
                        appKey = appKey,
                        udid = mainViewModel.getUdid(getString(R.string.random_udid)),
                        accessToken = accessToken,
                        onErrorOccurred = {
                            binding.zeroTrustToggle.isChecked = false
                            binding.zeroTrustToggle.contentDescription = "OFF"
                            binding.tvZeroStatus.visibility = View.VISIBLE
                            binding.tvZeroStatus.text =
                                getString(R.string.error_status_format, it.toString())
                            mainViewModel.zdkTunnelConnectionStateLiveData.removeObservers(this)
                            val zscalerSDKTunnelStatus = ZscalerSDK.status()
                            val isPreLoginTunnelStillActive = (zscalerSDKTunnelStatus.tunnelType == ZscalerSDKTunnelType.PRELOGIN && zscalerSDKTunnelStatus.tunnelConnectionState == "ON")
                            if (it == ZscalerSDKException.ErrorCode.TUNNEL_UPGRADE_FAILED || isPreLoginTunnelStillActive) {
                                // special case - tunnel upgrade failed
                                // i.e. tunnel upgrade from PLT to ZTT failed but PLT is still running
                                mainViewModel.setSelectedTunnel(ZDKTunnel.PRELOGIN)
                                mainViewModel.startTunnelStatusUpdates()
                                binding.tvPreStatus.visibility = View.VISIBLE
                                mainViewModel.zdkTunnelConnectionStateLiveData.observe(this) {
                                    binding.tvPreStatus.text = getString(R.string.status_format, it)
                                }
                                binding.preLoginToggle.isChecked = true
                                binding.preLoginToggle.contentDescription = "ON"

                                binding.tvZeroStatus.visibility = View.VISIBLE
                                binding.tvZeroStatus.text =
                                    getString(R.string.error_status_format, it.toString())

                            } else {
                                clearWebViewProxySettings()
                            }
                        })
                    applyWebViewProxySettings()
                    binding.zeroTrustToggle.contentDescription = "ON"
                    binding.preLoginToggle.isChecked = false
                    binding.preLoginToggle.contentDescription = "OFF"
                    mainViewModel.startTunnelStatusUpdates()
                    binding.tvZeroStatus.text =
                        getString(R.string.status_format, mainViewModel.getStatus())
                    binding.tvPreStatus.text = getString(R.string.status_format, "OFF")
                    mainViewModel.zdkTunnelConnectionStateLiveData.observe(this) {
                        binding.tvZeroStatus.text = getString(R.string.status_format, it)
                    }
                    addMessageOnWebView(zpaEmptyHtml)
                }
            } else {
                clearWebViewProxySettings()
                unregisterZDKReceiver()
                mainViewModel.stopTunnel(resetStatusText = { getString(R.string.status_off) })
                binding.zeroTrustToggle.contentDescription = "OFF"
                binding.preLoginToggle.contentDescription = "OFF"
                binding.preLoginToggle.isChecked = false
                mainViewModel.stopTunnelStatusUpdates()
                binding.tvZeroStatus.text = getString(R.string.status_format, "OFF")
                binding.tvZeroStatus.visibility = View.INVISIBLE
                addMessageOnWebView(zpaNotConnectedHtml)
            }
        }
    }

    private fun applyWebViewProxySettings() {
        ZscalerSDK.proxyInfo()?.let { proxyInfo ->
            ProxyUtility.setWebViewProxy(proxyInfo)
            if (sdkConfiguration.useProxyAuthentication) {
                Log.d(TAG, "applyWebViewProxySettings: using WebViewClientWithProxyAuthSupport")
                // only change web view client if previous is not the same which we want
                if (binding.webview.webViewClient !is WebViewClientWithProxyAuthSupport) {
                    binding.webview.webViewClient = WebViewClientWithProxyAuthSupport(proxyInfo)
                }
            } else {
                Log.d(TAG, "applyWebViewProxySettings: using Default WebViewClient")
                // only change web view client if previous is not the same which we want
                if (binding.webview.webViewClient is WebViewClientWithProxyAuthSupport) {
                    binding.webview.webViewClient = DefaultWebViewClient()
                }
            }
        }
    }

    private fun clearWebViewProxySettings() {
        ProxyUtility.clearWebViewProxy()
        Log.d(TAG, "clearWebViewProxySettings: using Default WebViewClient")
        if (binding.webview.webViewClient is WebViewClientWithProxyAuthSupport) {
            binding.webview.webViewClient = DefaultWebViewClient()
        }
    }

    private fun addLoggerFunction() {
        binding.exportLogsButton.setOnClickListener {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    createZipFileAndShare()
                } else {
                    requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
                }
            } else {
                createZipFileAndShare()
            }
        }

        binding.clearLogsButton.setOnClickListener {
            mainViewModel.clearLogs(onSuccess = {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.zdk_log_cleared), Snackbar.LENGTH_LONG).show()
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createZipFileAndShare()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == requestNotificationCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // TODO: add notification if permission granted
        }
    }

    private fun createZipFileAndShare() {
        val currentTimeStamp = System.currentTimeMillis()
        logsZipFileName = "ZscalerSDK_${currentTimeStamp/1000}.${currentTimeStamp%1000}.zip"
        ZipUtility.createEmptyZipFile(this, logsZipFileName)
            ?.let {
                launchShareIntentLogZip(it)
            }
    }

    private fun launchShareIntentLogZip(it: String) {
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.saved_to_download), Snackbar.LENGTH_LONG).show()
        lifecycleScope.launch(Dispatchers.IO) {
            mainViewModel.exportLog(it)
            val logZipFile = File(this@MainActivity.filesDir, logsZipFileName)
            val downloadFolder = this@MainActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadFolder != null) {
                val stat = StatFs(downloadFolder.path)
                val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
                if (bytesAvailable > logZipFile.length()) {
                    val fileUri = FileProvider.getUriForFile(
                        this@MainActivity,
                        this@MainActivity.packageName + ".file-provider",
                        logZipFile
                    )
                    saveFileUsingMediaStore(this@MainActivity, logZipFile)
                    withContext(Dispatchers.Main) {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "application/zip"
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        val chooser = Intent.createChooser(
                            shareIntent,
                            getString(R.string.share_zdk_log_zip_file)
                        )
                        val resInfoList: List<ResolveInfo> =
                            this@MainActivity.packageManager
                                .queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)

                        if (resInfoList.isNotEmpty()) {
                            for (resolveInfo in resInfoList) {
                                val packageName = resolveInfo.activityInfo.packageName
                                this@MainActivity.grantUriPermission(
                                    packageName,
                                    fileUri,
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }
                            startActivity(chooser)
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, getString(R.string.no_share_activity_present), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, getString(R.string.not_enough_space), Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, getString(R.string.download_folder_not_found), Toast.LENGTH_LONG).show()
                }
            }
        }


    }

    private fun saveFileUsingMediaStore(context: Context, logZipFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveLogToDownloadAboveAndEqualQ(context, logZipFile)
        } else {
            saveLogToDownloadBelowQ(context, logsZipFileName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveLogToDownloadAboveAndEqualQ(context: Context, logZipFile: File) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, logZipFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        var outputStream: OutputStream? = null

        try {
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                outputStream = resolver.openOutputStream(uri)
                logZipFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream!!)
                }
                Log.d(TAG, "saveLogToDownloadAboveAndEqualQ :: saveLogToDownloadAboveAndEqualQ: File saved successfully")
            } else {
                Log.d(TAG, "saveLogToDownloadAboveAndEqualQ :: Failed to save file")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "saveLogToDownloadAboveAndEqualQ :: exception raised", e)
        } finally {
            outputStream?.close()
        }
    }

    private fun saveLogToDownloadBelowQ(context: Context, zipFileName: String): Boolean {
        val sourceFile = File(context.filesDir, zipFileName)

        if (!sourceFile.exists()) {
            Log.e(TAG, "saveLogToDownloadBelowQ :: Source file does not exist: ${sourceFile.absolutePath}")
            return false
        }
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        if (!downloadsDir.mkdirs() && !downloadsDir.canWrite()) {
            Log.e(TAG, "saveLogToDownloadBelowQ :: Could not create or write to Downloads directory: ${downloadsDir.absolutePath}")
            return false
        }
        val destinationFile = File(downloadsDir, zipFileName)

        try {
            val inStream = FileInputStream(sourceFile)
            val outStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(1024)
            var readBytes: Int
            while (inStream.read(buffer).also { readBytes = it } != -1) {
                outStream.write(buffer, 0, readBytes)
            }

            inStream.close()
            outStream.close()
            Log.i(TAG, "saveLogToDownloadBelowQ :: File copied successfully.")
            return true
        } catch (e: IOException) {
            Log.e(TAG, "saveLogToDownloadBelowQ :: Error copying file: ${e.message}")
            return false
        }
    }

    fun addHttpsIfNeeded(url: String): String {
        return if (url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }
    }

    private fun createNotificationChannel() {
        val name = "ZDK Notification"
        val descriptionText = "Tunnel status from the ZDK lib"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(title: String, message: String): Boolean {

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntentForActivity())

        notificationManager.notify(NOTIFICATION_ID, builder.build())
        startForegroundService(Intent(this, NotificationCancellationService::class.java))
        return true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestNotificationCode)
    }

    private fun pendingIntentForActivity(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}

open class DefaultWebViewClient() : WebViewClient() {

    // override to block redirection to chrome or default browser i.e. url should open in web-view only
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return false
    }

}