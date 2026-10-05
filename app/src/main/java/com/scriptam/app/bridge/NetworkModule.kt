package com.scriptam.app.bridge

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.function
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Exposes a synchronous `Native.fetch(url)` function to JavaScript.
 *
 * Because QuickJS is single-threaded and our engine dispatcher already runs
 * off the main thread, we perform a **blocking** OkHttp call here.
 * This is safe: the engine dispatcher has `limitedParallelism(1)`,
 * so we never block the UI thread or spawn uncontrolled threads.
 *
 * Returns the response body as a string, or an error JSON on failure.
 */
class NetworkModule(
    private val client: OkHttpClient = sharedClient
) {

    companion object {
        val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    fun install(quickJs: QuickJs) {
        quickJs.function<String>("__native_fetch") { args ->
            val url = args.firstOrNull()?.toString()
            if (url.isNullOrBlank()) {
                return@function "{\"error\": \"fetch() requires a URL argument\"}"
            }

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Scriptam/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        body
                    } else {
                        "{\"error\": \"HTTP ${response.code}\", \"body\": ${body.take(500)}}"
                    }
                }
            } catch (e: Exception) {
                "{\"error\": \"${e.message?.replace("\"", "\\\"")}\"}"
            }
        }
    }
}
