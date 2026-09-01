package com.ivor.ivormusic.ui.vk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat

/**
 * VK authentication deliberately lives in a regular Activity instead of a Compose Dialog.
 * A WebView inside a Compose dialog can lose its input connection while the VK ID page
 * replaces the phone/password step, which used to break the SMS field and reverse typing.
 */
class VkAuthActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var completeButton: Button
    private var session: VkAuthSession? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        content.addView(
            TextView(this).apply {
                text = "Вход во ВКонтакте"
                textSize = 22f
                setTextColor(Color.rgb(28, 28, 30))
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(20), 0, dp(20), 0)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)),
        )

        webView = WebView(this).apply webViewSetup@{
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textDirection = View.TEXT_DIRECTION_LTR
            isFocusable = true
            isFocusableInTouchMode = true
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = false
            settings.loadWithOverviewMode = false
            settings.setSupportMultipleWindows(true)
            settings.saveFormData = false
            clearFormData()
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(this@webViewSetup, true)
            }
            val navigationClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    openExternalLogin(request.url.toString(), view)

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    openExternalLogin(url, view)

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    view.evaluateJavascript(FORCE_LTR_INPUTS_SCRIPT, null)
                    updateSession()
                }

                override fun onLoadResource(view: WebView, url: String) {
                    super.onLoadResource(view, url)
                    updateSession()
                }
            }
            webViewClient = navigationClient
            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message,
                ): Boolean {
                    val popup = WebView(this@VkAuthActivity).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                popupView: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val url = request.url.toString()
                                if (openExternalLogin(url, this@webViewSetup)) return true
                                this@webViewSetup.loadUrl(url)
                                return true
                            }

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(popupView: WebView, url: String): Boolean {
                                if (openExternalLogin(url, this@webViewSetup)) return true
                                this@webViewSetup.loadUrl(url)
                                return true
                            }
                        }
                    }
                    (resultMsg.obj as? WebView.WebViewTransport)?.let { transport ->
                        transport.webView = popup
                        resultMsg.sendToTarget()
                        return true
                    }
                    popup.destroy()
                    return false
                }
            }
        }
        content.addView(
            webView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )

        completeButton = Button(this).apply {
            text = "Завершите вход выше"
            isEnabled = false
            setOnClickListener { returnSession() }
        }
        content.addView(
            completeButton,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply {
                setMargins(dp(16), dp(8), dp(16), dp(12))
            },
        )
        setContentView(content)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) webView.goBack() else finish()
                }
            },
        )

        if (savedInstanceState == null) {
            clearOldVkSession { webView.loadUrl(LOGIN_URL) }
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            updateSession()
            webView.evaluateJavascript(FORCE_LTR_INPUTS_SCRIPT, null)
        }
    }

    private fun updateSession() {
        val manager = CookieManager.getInstance()
        val found = parseVkAuthCookies(VK_COOKIE_URLS.mapNotNull(manager::getCookie))
        if (found != null && found != session) {
            session = found
            completeButton.isEnabled = true
            completeButton.text = "Продолжить в Koda VK"
        }
    }

    private fun openExternalLogin(url: String, fallbackView: WebView): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        if (uri.scheme == "http" || uri.scheme == "https" || uri.scheme == "about") return false

        val intent = if (uri.scheme == "intent") {
            runCatching { Intent.parseUri(url, Intent.URI_INTENT_SCHEME) }.getOrNull()
        } else {
            Intent(Intent.ACTION_VIEW, uri)
        }?.apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            component = null
        } ?: return false

        return try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            val fallback = intent.getStringExtra("browser_fallback_url")
            if (!fallback.isNullOrBlank()) fallbackView.loadUrl(fallback)
            true
        } catch (_: SecurityException) {
            val fallback = intent.getStringExtra("browser_fallback_url")
            if (!fallback.isNullOrBlank()) fallbackView.loadUrl(fallback)
            true
        }
    }

    private fun returnSession() {
        val value = session ?: return
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(EXTRA_COOKIE_P, value.cookieP)
                .putExtra(EXTRA_REMIX_SID, value.remixSid),
        )
        finish()
    }

    private fun clearOldVkSession(onComplete: () -> Unit) {
        val manager = CookieManager.getInstance()
        val writes = buildList {
            VK_COOKIE_URLS.forEach { url ->
                VK_AUTH_COOKIE_NAMES.forEach { name ->
                    add(url to "$name=; Max-Age=0; Path=/; Secure")
                    add(url to "$name=; Max-Age=0; Domain=.${url.substringAfter("://")}; Path=/; Secure")
                }
            }
        }
        var remaining = writes.size
        writes.forEach { (url, value) ->
            manager.setCookie(url, value) {
                remaining -= 1
                if (remaining == 0) {
                    manager.flush()
                    webView.post(onComplete)
                }
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val LOGIN_URL = "https://vk.ru/login"
        private const val EXTRA_COOKIE_P = "vk_cookie_p"
        private const val EXTRA_REMIX_SID = "vk_remix_sid"

        private val VK_COOKIE_URLS = listOf(
            "https://vk.ru",
            "https://vk.com",
            "https://login.vk.ru",
            "https://login.vk.com",
            "https://id.vk.ru",
            "https://id.vk.com",
        )
        private val VK_AUTH_COOKIE_NAMES = listOf(
            "p",
            "remixsid",
            "remixsid6",
            "remixnsid",
            "remixdsid",
            "sui",
        )

        // VK ID renders several login steps dynamically. Apply LTR both now and to
        // subsequently inserted inputs so password characters keep their typed order.
        private val FORCE_LTR_INPUTS_SCRIPT = """
            (() => {
              const fix = root => root.querySelectorAll?.('input, textarea').forEach(input => {
                input.dir = 'ltr';
                input.style.direction = 'ltr';
                input.style.textAlign = 'left';
              });
              fix(document);
              if (!window.__kodaVkInputObserver) {
                window.__kodaVkInputObserver = new MutationObserver(records =>
                  records.forEach(record => record.addedNodes.forEach(node => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                      if (node.matches?.('input, textarea')) {
                        node.dir = 'ltr';
                        node.style.direction = 'ltr';
                        node.style.textAlign = 'left';
                      }
                      fix(node);
                    }
                  }))
                );
                window.__kodaVkInputObserver.observe(document.documentElement, {
                  childList: true,
                  subtree: true
                });
              }
            })();
        """.trimIndent()

        fun createIntent(context: Context): Intent = Intent(context, VkAuthActivity::class.java)

        fun sessionFrom(data: Intent?): VkAuthSession? {
            val cookieP = data?.getStringExtra(EXTRA_COOKIE_P)?.takeIf(String::isNotBlank) ?: return null
            val remixSid = data.getStringExtra(EXTRA_REMIX_SID)?.takeIf(String::isNotBlank) ?: return null
            return VkAuthSession(cookieP, remixSid)
        }
    }
}

data class VkAuthSession(val cookieP: String, val remixSid: String)

internal fun parseVkAuthCookies(headers: List<String>): VkAuthSession? {
    val cookies = headers
        .flatMap { it.split(';') }
        .map(String::trim)
        .mapNotNull { value ->
            val separator = value.indexOf('=')
            if (separator <= 0) null else value.substring(0, separator) to value.substring(separator + 1)
        }
    fun cookie(name: String) = cookies.lastOrNull { it.first == name }?.second?.takeIf(String::isNotBlank)
    val p = cookie("p") ?: return null
    val remixSid = cookie("remixsid") ?: return null
    return VkAuthSession(p, remixSid)
}
