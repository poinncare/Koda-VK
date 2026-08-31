package com.ivor.ivormusic.ui.vk

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VkAuthDialog(
    onDismiss: () -> Unit,
    onSession: (cookieP: String, remixSid: String) -> Unit,
) {
    var cookies by remember { mutableStateOf<Pair<String, String>?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(top = 24.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                Text(
                    text = "Sign in to VK Music",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(20.dp, 18.dp, 20.dp, 8.dp),
                )
                Text(
                    text = "Koda VK stores the resulting session encrypted on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp),
                    factory = { context ->
                        WebView(context).apply webViewSetup@{
                            webView = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/127.0 Mobile Safari/537.36"
                            CookieManager.getInstance().apply {
                                setAcceptCookie(true)
                                setAcceptThirdPartyCookies(this@webViewSetup, true)
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, url: String) {
                                    super.onPageFinished(view, url)
                                    cookies = readVkCookies()
                                }
                            }
                            loadUrl("https://vk.com/audio")
                        }
                    },
                )
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    Button(
                        enabled = cookies != null,
                        onClick = {
                            val value = cookies ?: return@Button
                            onSession(value.first, value.second)
                            onDismiss()
                        },
                    ) { Text(if (cookies == null) "Complete sign-in above" else "Continue") }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }
}

private fun readVkCookies(): Pair<String, String>? {
    val manager = CookieManager.getInstance()
    val all = listOf("https://vk.com", "https://vk.ru", "https://login.vk.ru")
        .mapNotNull(manager::getCookie)
        .flatMap { it.split(';') }
        .map { it.trim() }
    fun cookie(name: String) = all.firstOrNull { it.startsWith("$name=") }?.substringAfter('=')
    val p = cookie("p") ?: return null
    val sid = cookie("remixsid") ?: return null
    return p to sid
}
