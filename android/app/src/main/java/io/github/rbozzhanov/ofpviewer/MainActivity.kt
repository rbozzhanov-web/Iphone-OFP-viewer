package io.github.rbozzhanov.ofpviewer

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

/**
 * The whole app: one WebView showing the same reader as the web version
 * (index.html, copied in at build time), and the few things a web page cannot
 * do for itself on Android — being chosen in "Open with" and "Share", drawing a
 * PDF page as a picture, and colouring the system bars to match the page.
 *
 * Folding and unfolding the phone does not restart anything (see the manifest's
 * configChanges): the WebView is simply given the new size and the page lays
 * itself out again, in the same place.
 */
class MainActivity : ComponentActivity() {

    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private val inbox = Inbox()
    private lateinit var pages: PageRenderer
    private var pageLoaded = false
    private var chooser: ValueCallback<Array<Uri>>? = null

    private val pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        chooser?.onReceiveValue(uri?.let { arrayOf(it) })
        chooser = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pages = PageRenderer(cacheDir)

        // Drawn edge to edge; the bars are kept clear by padding, below, and take
        // the page's own background colour so the reader runs right up to them.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        root = FrameLayout(this)
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        applyTheme(systemIsDark())

        if (0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true         // the reader keeps the open plan and settings
            allowFileAccess = false
            allowContentAccess = true        // a picked PDF arrives as a content:// address
            setSupportZoom(false)            // the reader handles its own pinch on charts
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100
            mediaPlaybackRequiresUserGesture = true
        }
        // The page has a dark theme of its own; never let the WebView invert it.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(web.settings, false)
        }

        // Served from an https address that is the app's own, so the page keeps
        // everything a web page can — saved settings, the stored plan — and no
        // request ever leaves the phone.
        val assets = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                val url = request.url
                if (url.host != HOST) return refused()
                val path = url.path ?: return notFound()
                if (path == "/assets/web/inbox.pdf") {
                    val b = inbox.take() ?: return notFound()
                    return WebResourceResponse("application/pdf", null, 200, "OK",
                        mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(b))
                }
                PAGE.matchEntire(path)?.let { m ->
                    val png = pages.pagePng(m.groupValues[1].toInt()) ?: return notFound()
                    return WebResourceResponse("image/png", null, 200, "OK",
                        mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(png))
                }
                return assets.shouldInterceptRequest(url) ?: notFound()
            }

            // A link out of the reader — Trip Info, a reference — goes to the browser.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.host == HOST) return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this@MainActivity, "No app can open this link", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                pageLoaded = true
                deliver()
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                chooser?.onReceiveValue(null)
                chooser = callback
                try {
                    pickPdf.launch(arrayOf("application/pdf"))
                } catch (_: ActivityNotFoundException) {
                    chooser = null
                    callback.onReceiveValue(null)
                    Toast.makeText(this@MainActivity, "No app here can pick a file", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        }

        web.addJavascriptInterface(Bridge(), "OFPAndroid")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Back closes an open chart first; otherwise the app steps aside
                // without being closed, so the plan is still there on return.
                web.evaluateJavascript(
                    "(function(){var v=document.getElementById('cView');" +
                        "if(v&&!v.classList.contains('hide')&&typeof closeViewer==='function'){closeViewer();return true}" +
                        "return false})()"
                ) { closed -> if (closed != "true") moveTaskToBack(true) }
            }
        })

        web.loadUrl("https://$HOST/assets/web/index.html?shell=android")

        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Folding, unfolding and turning need nothing here: the WebView takes the
        // new size and the page reflows. Only an untouched page follows a change
        // of system theme; once the page has said which theme it shows, it wins.
        if (!themeFromPage) applyTheme(systemIsDark())
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onDestroy() {
        chooser?.onReceiveValue(null)
        chooser = null
        root.removeView(web)
        web.destroy()
        pages.close()
        super.onDestroy()
    }

    /* ---- documents arriving from other apps ---- */

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        // Reopened from Recents: the document it was started with has been read already.
        if (0 != (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)) return
        val uri: Uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        } ?: return
        thread(name = "inbox") {
            val result = runCatching { read(uri) }
            runOnUiThread {
                result.onSuccess { (name, bytes) ->
                    inbox.offer(name, bytes)
                    deliver()
                }.onFailure { e ->
                    Toast.makeText(this, "Could not open the document: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun read(uri: Uri): Pair<String, ByteArray> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "plan.pdf"
        if (uri.scheme == "content") {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) name = c.getString(0)
            }
        }
        val out = ByteArrayOutputStream()
        val input = contentResolver.openInputStream(uri) ?: error("it could not be read")
        input.use {
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = it.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                if (out.size() > MAX_BYTES) error("it is larger than any flight plan")
            }
        }
        if (out.size() == 0) error("it is empty")
        return name to out.toByteArray()
    }

    /** Tells the page a document is waiting, once it is there to be told. */
    private fun deliver() {
        if (!pageLoaded) return
        val name = inbox.announce() ?: return
        web.evaluateJavascript("window.__ofpOpen && window.__ofpOpen(${JSONObject.quote(name)})", null)
    }

    /* ---- the system bars follow the page's theme ---- */

    private var themeFromPage = false

    private fun systemIsDark() =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private fun applyTheme(dark: Boolean) {
        val bg = if (dark) DARK_BG else LIGHT_BG
        root.setBackgroundColor(bg)
        web.setBackgroundColor(bg)
        window.decorView.setBackgroundColor(bg)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    /** What the page may ask of the app. Called on the WebView's own thread, not the UI thread. */
    private inner class Bridge {
        /** The plan now open, for drawing its pages. Base64, as a bridge carries only text. */
        @JavascriptInterface
        fun setPdf(b64: String): Boolean =
            try { pages.setDocument(Base64.decode(b64, Base64.DEFAULT)) } catch (_: Exception) { false }

        @JavascriptInterface
        fun setTheme(dark: Boolean) {
            runOnUiThread {
                themeFromPage = true
                applyTheme(dark)
            }
        }
    }

    private fun refused() = WebResourceResponse("text/plain", "utf-8", 403, "Forbidden", emptyMap(), ByteArrayInputStream(ByteArray(0)))
    private fun notFound() = WebResourceResponse("text/plain", "utf-8", 404, "Not Found", emptyMap(), ByteArrayInputStream(ByteArray(0)))

    private companion object {
        const val HOST = WebViewAssetLoader.DEFAULT_DOMAIN
        val PAGE = Regex("/native/page/(\\d{1,5})\\.png")
        const val MAX_BYTES = 64 * 1024 * 1024
        val DARK_BG = Color.parseColor("#0f1419")
        val LIGHT_BG = Color.parseColor("#f2f5f8")
    }
}
