package io.github.hanxmoneai.sunmibridge

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.hanxmoneai.sunmibridge.bridge.PosBridge
import io.github.hanxmoneai.sunmibridge.config.AppConfig
import io.github.hanxmoneai.sunmibridge.config.UrlNormalizer
import io.github.hanxmoneai.sunmibridge.printer.SunmiPrinter
import io.github.hanxmoneai.sunmibridge.scanner.ScanOutcome
import io.github.hanxmoneai.sunmibridge.scanner.ScanResultParser
import io.github.hanxmoneai.sunmibridge.scanner.SunmiScanner

/**
 * Hosts a store's web app in a WebView and lets it use the Sunmi scanner and printer
 * through [PosBridge]. The setup screen stores the URL; later launches open it directly.
 */
class MainActivity : AppCompatActivity(), PosBridge.Host {

    private lateinit var config: AppConfig
    private lateinit var printer: SunmiPrinter
    private lateinit var bridge: PosBridge

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var navBar: View
    private lateinit var setupPanel: View
    private lateinit var offlinePanel: View
    private lateinit var startUrlLayout: TextInputLayout
    private lateinit var startUrlInput: TextInputEditText
    private lateinit var homeUrlLayout: TextInputLayout
    private lateinit var homeUrlInput: TextInputEditText
    private lateinit var deviceStatus: TextView

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentUrl: String? = null
    private var lastBackPress = 0L

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val raw = SunmiScanner.readRawResult(result.data)
        val results = ScanResultParser.parse(raw)
        bridge.deliverScan(ScanOutcome(results, cancelled = results.isEmpty()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        config = AppConfig(this)
        printer = SunmiPrinter(this)
        printer.listener = SunmiPrinter.Listener { runOnMain { updateDeviceStatus() } }
        bridge = PosBridge(this, printer)

        setupWebView()
        setupActions()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleBack()
        })

        startUrlInput.setText(config.startUrl)
        homeUrlInput.setText(config.homeUrl)
        updateDeviceStatus()

        val saved = UrlNormalizer.normalize(config.startUrl)
        if (saved != null) openUrl(saved) else showSetup()
    }

    override fun onDestroy() {
        printer.listener = null
        mainHandler.removeCallbacksAndMessages(null)
        webView.removeJavascriptInterface(JS_NAME)
        webView.removeJavascriptInterface(JS_LEGACY_NAME)
        webView.destroy()
        super.onDestroy()
    }

    // ---- PosBridge.Host ---------------------------------------------------------

    override fun launchScanner(): Boolean = try {
        scanLauncher.launch(SunmiScanner.createIntent())
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    override fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
    }

    override fun evaluate(script: String) {
        if (!isDestroyed) webView.evaluateJavascript(script, null)
    }

    // ---- Setup ------------------------------------------------------------------

    private fun bindViews() {
        webView = findViewById(R.id.web_view)
        progress = findViewById(R.id.progress)
        navBar = findViewById(R.id.nav_bar)
        setupPanel = findViewById(R.id.setup_panel)
        offlinePanel = findViewById(R.id.offline_panel)
        startUrlLayout = findViewById(R.id.start_url_layout)
        startUrlInput = findViewById(R.id.start_url_input)
        homeUrlLayout = findViewById(R.id.home_url_layout)
        homeUrlInput = findViewById(R.id.home_url_input)
        deviceStatus = findViewById(R.id.device_status)
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun setupWebView() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(false)
        }
        webView.addJavascriptInterface(bridge, JS_NAME)
        webView.addJavascriptInterface(bridge, JS_LEGACY_NAME)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                progress.isVisible = true
            }

            override fun onPageFinished(view: WebView, url: String?) {
                progress.isVisible = false
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && !isOnline()) showOffline()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
            }
        }
    }

    private fun setupActions() {
        findViewById<View>(R.id.btn_open).setOnClickListener { submitSetup() }
        findViewById<View>(R.id.btn_demo).setOnClickListener {
            webView.clearCache(true)
            openUrl(UrlNormalizer.DEMO_URL)
        }
        findViewById<View>(R.id.btn_home).setOnClickListener {
            val home = UrlNormalizer.normalize(config.homeUrl) ?: UrlNormalizer.normalize(config.startUrl)
            openUrl(home ?: currentUrl ?: return@setOnClickListener)
        }
        findViewById<View>(R.id.btn_settings).setOnClickListener { showSetup() }
        findViewById<View>(R.id.btn_retry).setOnClickListener { currentUrl?.let { openUrl(it) } }
        homeUrlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitSetup()
                true
            } else {
                false
            }
        }
    }

    private fun submitSetup() {
        val start = UrlNormalizer.normalize(startUrlInput.text?.toString())
        if (start == null) {
            startUrlLayout.error = getString(R.string.error_invalid_url)
            return
        }
        startUrlLayout.error = null

        val homeText = homeUrlInput.text?.toString().orEmpty()
        val home = if (homeText.isBlank()) "" else UrlNormalizer.normalize(homeText)
        if (home == null) {
            homeUrlLayout.error = getString(R.string.error_invalid_url)
            return
        }
        homeUrlLayout.error = null

        config.startUrl = start
        config.homeUrl = home
        // Store web apps are usually SPAs; a stale cached index.html causes a blank screen
        // after a deploy, so start from a clean cache whenever the URL is (re)submitted.
        webView.clearCache(true)
        openUrl(start)
    }

    // ---- Navigation -------------------------------------------------------------

    private fun openUrl(url: String) {
        currentUrl = url
        if (!url.startsWith("file:") && !isOnline()) {
            showOffline()
            return
        }
        showWeb()
        webView.loadUrl(url)
    }

    private fun handleBack() {
        when {
            setupPanel.isVisible && currentUrl != null && webView.url != null -> showWeb()
            webView.isVisible && webView.canGoBack() -> webView.goBack()
            else -> {
                val now = SystemClock.elapsedRealtime()
                if (now - lastBackPress < BACK_EXIT_WINDOW_MS) {
                    finish()
                } else {
                    lastBackPress = now
                    Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showWeb() {
        setupPanel.isVisible = false
        offlinePanel.isVisible = false
        webView.isVisible = true
        navBar.isVisible = true
    }

    private fun showSetup() {
        updateDeviceStatus()
        setupPanel.isVisible = true
        offlinePanel.isVisible = false
        navBar.isVisible = false
    }

    private fun showOffline() {
        progress.isVisible = false
        setupPanel.isVisible = false
        webView.isVisible = false
        offlinePanel.isVisible = true
        navBar.isVisible = true
    }

    private fun updateDeviceStatus() {
        val available = getString(R.string.status_available)
        val unavailable = getString(R.string.status_unavailable)
        deviceStatus.text = getString(
            R.string.device_status,
            if (printer.isAvailable) available else unavailable,
            if (SunmiScanner.isInstalled(this)) available else unavailable,
        )
    }

    private fun isOnline(): Boolean {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private companion object {
        const val JS_NAME = "PosBridge"
        const val JS_LEGACY_NAME = "android"
        const val BACK_EXIT_WINDOW_MS = 2_000L
    }
}
