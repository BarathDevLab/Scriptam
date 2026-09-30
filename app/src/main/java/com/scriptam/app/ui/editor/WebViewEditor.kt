package com.scriptam.app.ui.editor

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Compose wrapper around a WebView hosting CodeMirror for rich code editing.
 *
 * Communication:
 *  - Android -> JS: `setCodeBase64(b64)` via [WebView.evaluateJavascript]
 *  - JS -> Android: `ScriptamBridge.getInitialCode()` / `ScriptamBridge.onCodeChange(code)`
 *
 * @param initialCode  Code to load when the editor page finishes loading.
 * @param onCodeChange Called (debounced 200ms) when the user edits code in the editor.
 * @param isReadOnly   When true, the editor dims and disables input (e.g., during execution).
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun WebViewEditor(
    initialCode: String,
    onCodeChange: (String) -> Unit,
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currentInitialCode by rememberUpdatedState(initialCode)
    val currentOnCodeChange by rememberUpdatedState(onCodeChange)

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Allow focus and soft keyboard input inside Compose AndroidView
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocusFromTouch()

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = true
                settings.setSupportZoom(false)
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                // Transparent background to match our dark theme during load
                setBackgroundColor(android.graphics.Color.parseColor("#0D0D14"))

                webChromeClient = WebChromeClient()

                addJavascriptInterface(
                    EditorJsBridge(
                        webView = this,
                        codeProvider = { currentInitialCode },
                        onCodeChange = { currentOnCodeChange(it) }
                    ),
                    "ScriptamBridge"
                )

                // Delegate focus to WebView on touch so virtual keyboard triggers reliably
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_UP -> {
                            if (!v.hasFocus()) {
                                v.requestFocus()
                            }
                        }
                    }
                    false
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val encoded = Base64.encodeToString(
                            currentInitialCode.toByteArray(Charsets.UTF_8),
                            Base64.NO_WRAP
                        )
                        view.evaluateJavascript("setCodeBase64('$encoded')", null)
                    }
                }

                loadUrl("file:///android_asset/editor/index.html")
            }
        },
        update = { webView ->
            webView.evaluateJavascript(
                "if (typeof setReadOnly === 'function') { setReadOnly($isReadOnly); }", null
            )
        },
        modifier = modifier,
        onRelease = { it.destroy() }
    )
}

/**
 * JavascriptInterface bridge for receiving events from the CodeMirror editor.
 * Runs on a WebView background thread — callers must be thread-safe.
 */
private class EditorJsBridge(
    private val webView: WebView,
    private val codeProvider: () -> String,
    private val onCodeChange: (String) -> Unit
) {
    @JavascriptInterface
    fun getInitialCode(): String {
        val rawCode = codeProvider()
        return Base64.encodeToString(
            rawCode.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
    }

    @JavascriptInterface
    fun onCodeChange(code: String) {
        onCodeChange.invoke(code)
    }

    @JavascriptInterface
    fun onEditorReady() {
        // Editor loaded; initial code can be injected or pulled.
    }

    @JavascriptInterface
    fun showKeyboard() {
        webView.post {
            webView.requestFocus()
            val imm = webView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}
