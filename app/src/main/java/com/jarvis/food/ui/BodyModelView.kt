package com.jarvis.food.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jarvis.food.data.BodyMeasurements
import java.io.ByteArrayInputStream

/** The bridge is available only to our bundled, network-free model, never to arbitrary pages. */
class BodyGuideBridge(private val onPart: (String) -> Unit, private val onRender: (Boolean) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    @JavascriptInterface fun selectPart(key: String) {
        if (BodyMeasurements.guides.any { it.key == key }) main.post { onPart(key) }
    }
    @JavascriptInterface fun renderState(ready: Boolean) { main.post { onRender(ready) } }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BodyModelView(selected: String, male: Boolean, onSelect: (String) -> Unit) {
    val selectedNow by rememberUpdatedState(selected)
    val maleNow by rememberUpdatedState(male)
    val selectNow by rememberUpdatedState(onSelect)
    var view by remember { mutableStateOf<WebView?>(null) }
    var status by remember { mutableStateOf("Caricamento modello") }
    Box(Modifier.fillMaxWidth().height(340.dp).testTag("body-model-3d").semantics { stateDescription = status }) {
        AndroidView(modifier = Modifier.fillMaxWidth().height(340.dp), factory = { context ->
            WebView(context).apply {
                tag = "body-model-webview"
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                settings.setSupportMultipleWindows(false)
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                addJavascriptInterface(BodyGuideBridge({ selectNow(it) }, { ok -> status = if (ok) "Modello 3D pronto" else "Guida testuale disponibile" }), "AndroidBody")
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse =
                        WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    override fun onPageFinished(view: WebView, url: String) {
                        view.evaluateJavascript("window.setGender(${maleNow});window.setSelection('${selectedNow}');window.dispatchEvent(new Event('resize'));", null)
                    }
                }
                val html = context.assets.open("body-guide/index.html").bufferedReader().use { it.readText() }
                loadDataWithBaseURL("https://body.jarvis.invalid/", html, "text/html", "UTF-8", null)
                view = this
            }
        }, update = { web ->
            web.evaluateJavascript("if(window.setSelection){window.setSelection('$selected');window.setGender($male);}", null)
        })
    }
    DisposableEffect(Unit) {
        onDispose { view?.apply { stopLoading(); removeJavascriptInterface("AndroidBody"); destroy() }; view = null }
    }
}
