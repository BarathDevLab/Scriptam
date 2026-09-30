package com.scriptam.app.bridge

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.function

/**
 * Exposes device information to JavaScript:
 * - `Native.device()` — Returns a JSON string with device details.
 *
 * Returned fields:
 * ```json
 * {
 *   "model": "Pixel 8",
 *   "brand": "google",
 *   "sdk": 34,
 *   "androidVersion": "14",
 *   "screenWidth": 1080,
 *   "screenHeight": 2400,
 *   "density": 2.625,
 *   "battery": 85,
 *   "isCharging": false,
 *   "isConnected": true,
 *   "connectionType": "wifi"
 * }
 * ```
 */
class DeviceInfoModule(private val context: Context) {

    fun install(quickJs: QuickJs) {
        quickJs.function<String>("__native_device") { _ ->
            val metrics = context.resources.displayMetrics
            val battery = getBatteryInfo()
            val network = getNetworkInfo()

            buildString {
                append('{')
                append(""""model":"${Build.MODEL}",""")
                append(""""brand":"${Build.BRAND}",""")
                append(""""sdk":${Build.VERSION.SDK_INT},""")
                append(""""androidVersion":"${Build.VERSION.RELEASE}",""")
                append(""""screenWidth":${metrics.widthPixels},""")
                append(""""screenHeight":${metrics.heightPixels},""")
                append(""""density":${metrics.density},""")
                append(""""battery":${battery.first},""")
                append(""""isCharging":${battery.second},""")
                append(""""isConnected":${network.first},""")
                append(""""connectionType":"${network.second}"""")
                append('}')
            }
        }
    }

    /** Returns (batteryLevel 0-100, isCharging). */
    private fun getBatteryInfo(): Pair<Int, Boolean> {
        val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val plugged = status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        return pct to (plugged != 0)
    }

    /** Returns (isConnected, type: "wifi"|"cellular"|"none"). */
    private fun getNetworkInfo(): Pair<Boolean, String> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val active = cm.activeNetwork ?: return false to "none"
        val caps = cm.getNetworkCapabilities(active) ?: return false to "none"
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
        return true to type
    }
}
